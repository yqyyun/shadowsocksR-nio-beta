/*
 * Copyright 2022 yqy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yqy.loop;

import com.yqy.handler.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 单个selector的事件循环
 *
 * @author yqy
 * @date 2022/7/25 11:38
 */
public class EventLoop implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final int index;

    private Selector selector;

    private final SelectorProvider provider;

    private final EventLoopGroup pool;

    // TODO
    private static final int SELECTOR_AUTO_REBUILD_THRESHOLD = 512;

    private final AtomicLong DEADLINE_TIME = new AtomicLong();

    private static final long AWAKE = -1;
    private static final long NONE = Long.MAX_VALUE;


    private final Queue<Runnable> onceTaskQueue = new ConcurrentLinkedQueue<>();

    // pending task
    // TODO queue or else
    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();

    private final Queue<Runnable> readTaskQueue = new ConcurrentLinkedQueue<>();

    private final Queue<Runnable> writeTaskQueue = new ConcurrentLinkedQueue<>();

    private final Queue<Runnable> connectTaskQueue = new ConcurrentLinkedQueue<>();

    private final Queue<Runnable> acceptTaskQueue = new ConcurrentLinkedQueue<>();

    // TODO 来自其它线程提交的任务, 考虑能否使用工作窃取算法
    private final Queue<Runnable> shareIOTaskQueue = new ConcurrentLinkedQueue<>();

    private static final Runnable EMPTY_TASK = () -> {
    };

    // TODO
    private boolean isAsyncIO = false;


    static class IOTask {
        // attachment will be executed when  any event happened
        // read will be executed only when read event happened;
        // write, connect, accept do so.
        // task will be executed before attachment when isSync() return true;
        Runnable readTask;
        Runnable writeTask;
        Runnable connectTask;
        Runnable acceptTask;
        Runnable attachment;
    }

    private volatile Thread runThread;

    private volatile boolean needsToSelectAgain = false;

    private static final int CLEANUP_INTERVAL = 256;

    private int cancelledKeys = 0;

    EventLoop(EventLoopGroup pool, SelectorProvider provider, int index) {
        this.pool = pool;
        this.provider = provider;
        this.index = index;
//        this.selector = openSelector();
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Selector openSelector() {
        try {
            return provider.openSelector();
        } catch (IOException e) {
            throw new EventLoopException("failed to open a new selector", e);
        }
    }


    /**
     * register an SelectableChannel with eventloop's selector;
     * att is attachment.
     * Traditionally run att.run in reactor mode. Not support async!
     *
     * @param ch
     * @param interestOps
     * @param att
     * @return
     */
    public SelectionKey registerWithAttachment(final SelectableChannel ch, final int interestOps, final Runnable att) {
        Objects.requireNonNull(att, "att");
        return register(ch, att, interestOps);
    }

    /**
     * register an SelectableChannel with eventloop's selector;
     * <p>
     * Support async! Check isAsyncIO() Method for more information.
     *
     * @param ch
     * @param interestOps
     * @param ioTask
     * @return
     */
    public SelectionKey registerWithIOTask(final SelectableChannel ch, final int interestOps, final IOTask ioTask) {
        Objects.requireNonNull(ioTask, "ioTask");
        return register(ch, ioTask, interestOps);
    }

    private SelectionKey register(final SelectableChannel ch, final Object obj, final int interestOps) {
        checkInterestOps(ch, interestOps);
        if (isShutDown()) {
            throw new IllegalStateException("event loop shut down");
        }
        // if select() blocks forever, then register operation also blocks forever;
        // selector.wakeup();
        try {
            return ch.register(selector, interestOps, obj);
        } catch (Exception e) {
            throw new EventLoopException("failed to register a channel", e);
        }
    }

    public SelectionKey register(final SelectableChannel ch) {
        try {
            return ch.register(selector, 0);
        } catch (Exception e) {
            throw new EventLoopException("failed to register a channel", e);
        }
    }

    /**
     * 添加一次性任务到队列开头，这意味着在所有一次性任务中，它会第一个被执行
     *
     * @param ch
     * @param task
     */
    public void addOnceTaskFist(SelectableChannel ch, Runnable task) {
        throw new UnsupportedOperationException("this operation  is not yet implemented");
    }

    /**
     * 添加一次性任务到队列末尾，这意味在所有一次性任务中，它会被最后一个执行
     *
     * @param ch
     * @param task
     */
    public void addOnceTaskLast(SelectableChannel ch, Runnable task) {
        throw new UnsupportedOperationException("this operation  is not yet implemented");
    }

    /**
     * 绑定任务到Channel对应的SelectionKey上
     *
     * @param ch
     * @param task
     */
    public void attach(SelectableChannel ch, Runnable task) {
        Objects.requireNonNull(ch, "ch");
        SelectionKey key = ch.keyFor(selector);
        if (key == null) {
            throw new IllegalStateException("ch is not registered yet");
        }
        key.attach(task);
    }

    /**
     * 添加并绑定对应事件触发时需要执行的任务
     *
     * @param ch
     * @param interestOps
     * @param task
     */
    public void attachTask(final SelectableChannel ch, final int interestOps, final Runnable task) {
        //
        // read operation -> raed task
        // write operation -> write task
        // accept, connect etc.
        checkInterestOps(ch, interestOps);
        Objects.requireNonNull(task, "task");

        if (isShutDown()) {
            throw new IllegalStateException("event loop shut down");
        }
        SelectionKey key = ch.keyFor(selector);
        if (key == null) {
            throw new IllegalStateException("ch is not registered yet");
        }
        if (!key.isValid()) {
            return;
        }
        IOTask ioTask;
        Object o = key.attachment();
        if (o instanceof IOTask) {
            ioTask = (IOTask) o;
        } else {
            ioTask = new IOTask();
            key.attach(ioTask);
            ioTask.attachment = (Runnable) o;
        }
        try {
            if ((interestOps & SelectionKey.OP_READ) != 0) {
                ioTask.readTask = task;
            }
            if ((interestOps & SelectionKey.OP_WRITE) != 0) {
                ioTask.writeTask = task;
            }
            // optimization; 5 = 1 | 4
            if (interestOps <= 5) {
                return;
            }
            if ((interestOps & SelectionKey.OP_CONNECT) != 0) {
                ioTask.connectTask = task;
            }
            if ((interestOps & SelectionKey.OP_ACCEPT) != 0) {
                ioTask.acceptTask = task;
            }
        } finally {
            int i = key.interestOps();
            if (i != interestOps) {
                key.interestOps(interestOps | i);
            }
        }
    }

    public void detachTask(final SelectableChannel ch, final int interestOps) {
        checkInterestOps(ch, interestOps);
        SelectionKey key = ch.keyFor(selector);
        if (key == null || !key.isValid()) {
            return;
        }
        int i = key.interestOps();
        if ((i & interestOps) != 0) {
            key.interestOps(i & ~interestOps);
        }
        Object o = key.attachment();
        if (o == null) {
            return;
        }
        if (o instanceof IOTask) {
            IOTask ioTask = ((IOTask) o);
            if ((interestOps & SelectionKey.OP_READ) != 0) {
                ioTask.readTask = null;
            }
            if ((interestOps & SelectionKey.OP_WRITE) != 0) {
                ioTask.writeTask = null;
            }
            // optimization; 5 = 1 | 4
            if (interestOps <= 5) {
                return;
            }
            if ((interestOps & SelectionKey.OP_CONNECT) != 0) {
                ioTask.connectTask = null;
            }
            if ((interestOps & SelectionKey.OP_ACCEPT) != 0) {
                ioTask.acceptTask = null;
            }
        }
    }

    private void checkInterestOps(SelectableChannel ch, int interestOps) {
        Objects.requireNonNull(ch, "ch");
        if (interestOps == 0) {
            throw new IllegalArgumentException("interestOps must be non-zero.");
        }
        if ((interestOps & ~ch.validOps()) != 0) {
            throw new IllegalArgumentException(
                    "invalid interestOps: " + interestOps + "(validOps: " + ch.validOps() + ')');
        }
    }


    public int select() throws IOException {
        return selector.select();
    }

    public int select(long deadlineTimeout) throws IOException {
        int numKeys;
        if (deadlineTimeout == AWAKE) {
            numKeys = selector.selectNow();
        } else if (deadlineTimeout == NONE) {
            numKeys = selector.select();
        } else {
            numKeys = selector.select(deadlineTimeout);
        }
        return numKeys;
    }

    public int selectNow() throws IOException {
        return selector.selectNow();
    }

    private void selectAgain() {
        needsToSelectAgain = false;
        try {
            selector.selectNow();
        } catch (Throwable t) {
            LOGGER.warn("Failed to update SelectionKeys.", t);
        }
    }

    private long nextDeadlineTimeout() {
        // todo
        return 1000;
    }

    public static final int CONTINUE = -2;
    public static final int BUSY_WAIT = -3;
    public static final int SELECT = -1;

    private int calculateStrategy() throws IOException {
        return hasTask() ? selector.selectNow() : SELECT;
    }

    private void processSelectedKeys() {
        Set<SelectionKey> keys = selector.selectedKeys();
        // creating an empty iterator still has a gc activity
        if (keys.isEmpty()) {
            return;
        }
        Iterator<SelectionKey> it = keys.iterator();
        for (; ; ) {
            final SelectionKey key = it.next();
            final Object o = key.attachment();
            it.remove();

            if (o instanceof Runnable) {
                ((Runnable) o).run();
            } else if (o instanceof IOTask) {
                processSelectedKey(key, (IOTask) o);
            }
            if (!it.hasNext()) {
                break;
            }
            // optimized gc
            if (needsToSelectAgain) {
                selectAgain();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                if (selectionKeys.isEmpty()) {
                    break;
                } else {
                    it = selectionKeys.iterator();
                }
            }
        }
    }

    private void processSelectedKey(SelectionKey key, IOTask task) {
        if (!key.isValid()) {
            return;
        }
        final int readyOps = key.readyOps();
        int i = key.interestOps();
        final int oi = i;
        // we need to do something special here to avoid CancelledKeyException
        boolean closed = false;
        if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
            i &= ~SelectionKey.OP_CONNECT;
            SocketChannel ch = (SocketChannel) key.channel();
            try {
                if (!ch.finishConnect()) {
                    throw new Error();
                }
            } catch (Throwable e) {
                LOGGER.error("Can't establish connection!");
                try {
                    closed = true;
                    LOGGER.debug("event loop close socket, targetAddress({})", ((InetSocketAddress) ch.getRemoteAddress()));
                    ch.close();
                } catch (IOException ignore) {
                }
            }
            // even through establishing connection is failed, we still have a chance to do connectTask
            if (task.connectTask != null) {
                connectTaskQueue.add(task.connectTask);
                task.connectTask = null;
            }
        } else if ((readyOps & SelectionKey.OP_ACCEPT) != 0) {
            if (task.acceptTask != null) {
                acceptTaskQueue.add(task.acceptTask);
            } else {
                i &= ~SelectionKey.OP_ACCEPT;
            }
        }
        if (closed) {
            // ensure this key is removed from this selector,so do it anyway
            key.cancel();
            return;
        }
        // then do read/write task
        if ((readyOps & SelectionKey.OP_READ) != 0) {
            if (task.readTask != null) {
                readTaskQueue.add(task.readTask);
            } else {
                i &= ~SelectionKey.OP_READ;
            }
        }
        if ((readyOps & SelectionKey.OP_WRITE) != 0) {
            if (task.writeTask != null) {
                writeTaskQueue.add(task.writeTask);
            } else {
                i &= ~SelectionKey.OP_WRITE;
            }
        }
        if (readyOps != 0 && task.attachment != null) {
            taskQueue.add(task.attachment);
        }
        if (key.isValid())
        if (i != oi) {
            key.interestOps(i);
        }
    }

    public void setAsyncIO(boolean asyncIO) {
        isAsyncIO = asyncIO;
    }

    public boolean isAsyncIO() {
        return isAsyncIO;
    }

    private boolean runAllTask() {
//        boolean runTask = false;
        //
        if (isAsyncIO) {
            return runAllAsyncIOTask();
        } else {
            return runAllSyncIOTask();
        }
//        return runTask;
    }

    private boolean runAllAsyncIOTask() {
        // read and write task is executed in another two thread
        // connected and accept task is executed in selector(maybe this) thread
        boolean runTask = false;
        if (!readTaskQueue.isEmpty()) {
            pool.submitIOTask(() ->
                    readTaskQueue.forEach(r -> {
                        // happens-before
                        if (readTaskQueue.remove(r)) {
                            r.run();
                        }
                    })
            );
            runTask |= true;
        }
        if (!writeTaskQueue.isEmpty()) {
            pool.submitIOTask(() ->
                    writeTaskQueue.forEach(r -> {
                        if (writeTaskQueue.remove(r)) {
                            r.run();
                        }
                    }));
            runTask |= true;
        }
        taskQueue.forEach(r -> {
            if (taskQueue.remove(r)) {
                r.run();
            }
        });
        // no more connection to be established because we closed event loop
        if (isShutDown()) {
            return runTask;
        }
        runTask |= !connectTaskQueue.isEmpty();
        runTask |= !acceptTaskQueue.isEmpty();
        connectTaskQueue.forEach(r -> {
            if (connectTaskQueue.remove(r)) {
                r.run();
            }
        });
        acceptTaskQueue.forEach(r -> {
            if (acceptTaskQueue.remove(r)) {
                r.run();
            }
        });
        return runTask;
    }

    private boolean runAllSyncIOTask() {
        boolean runTask = !readTaskQueue.isEmpty() ||
                !writeTaskQueue.isEmpty() ||
                !connectTaskQueue.isEmpty() ||
                !acceptTaskQueue.isEmpty();
        if (!runTask) {
            // prematurely returned , optimize gc
            return runTask;
        }
        // Connect和rw事件执行顺序
        readTaskQueue.forEach(r -> {
            readTaskQueue.remove(r);
            r.run();
        });
        writeTaskQueue.forEach(r -> {
            writeTaskQueue.remove(r);
            r.run();
        });
        taskQueue.forEach(r -> {
            taskQueue.remove(r);
            r.run();
        });
        // no more connection to be established because we closed event loop
        if (isShutDown()) {
            return runTask;
        }
        connectTaskQueue.forEach(r -> {
            connectTaskQueue.remove(r);
            r.run();
        });
        acceptTaskQueue.forEach(r -> {
            acceptTaskQueue.remove(r);
            r.run();
        });
        return runTask;
    }

    /**
     * TODO 根据是否还有正在执行的IO任务来判断是否继续select()，提升性能
     */
    @Override
    public void run() {
        runThread = Thread.currentThread();
        int selectCnt = 0;
        while (true) {
            try {
                int numKeys;
                try {
                    numKeys = calculateStrategy();
                    switch (numKeys) {
                        case CONTINUE:
                            // still has io task ,no need to do select() for more tasks; continue
                            continue;
                        case BUSY_WAIT:
                        case SELECT:
                            long deadlineTimeout = nextDeadlineTimeout();
                            try {
                                numKeys = select(deadlineTimeout);
                            } finally {
                                DEADLINE_TIME.lazySet(deadlineTimeout);
                            }
                        default:
                    }
                } catch (IOException e) {
                    rebuildSelector();
                    selectCnt = 0;
                    handleLoopException(e);
                    continue;
                }
                selectCnt++;
                cancelledKeys = 0;
                needsToSelectAgain = false;
                processSelectedKeys();
                boolean runTask = runAllTask();
                if (numKeys > 0 || runTask) {
                    selectCnt = 0;
                } else if (unexpectedWakeup(selectCnt)) {
//                        System.exit(1);
                    selectCnt = 0;
                }
                if (!open) {
                    break;
                }
                if (isAsyncIO) {
                    runSharedTask();
                }
            } catch (CancelledKeyException e) {
                // Harmless exception - log anyway
                LOGGER.debug(CancelledKeyException.class.getSimpleName() + " raised by a Selector {} - JDK bug?",
                        selector, e);
            } catch (Error e) {
                throw e;
            } catch (Throwable t) {
                handleLoopException(t);
            } finally {
                // Always handle shutdown even if the loop processing threw an exception.
                try {
                    if (isShuttingDown()) {
                        closeAll();
                        if (confirmShutdown()) {
                            // break this loop, then pool can be terminated
                            return;
                        }
                    }
                } catch (Error e) {
                    throw e;
                } catch (Throwable t) {
                    handleLoopException(t);
                }
            }
        }
    }

    private void runSharedTask() {
        if (shareIOTaskQueue.isEmpty()) {
            // average load in group
            // max load in here
            // group.rearrangeSharedTask()
            //stealTask()

        }
        shareIOTaskQueue.forEach(r -> {
            if (shareIOTaskQueue.remove(r)) {
                r.run();
            }
        });
    }

    private boolean confirmShutdown() {
        if (isTerminated()) {
            return true;
        }
        if (!isShuttingDown()) {
            return false;
        }
        // run all remaining tasks in this thread
        //no need to worry about connect event and accept event
        if (runAllSyncIOTask()) {
            // it's time to close event loop
            LOGGER.debug("all remaining has been done");
            return true;
        }
        return true;
    }

    public void dispatch(SelectionKey sk) {
        LOGGER.error("Selection key: {}", sk);
        if (sk.isValid()) {
            LOGGER.error("writable: {}, readable: {}", sk.isWritable(), sk.isReadable());
        }
        Runnable r = ((Runnable) sk.attachment());
        if (r != null) {
            r.run();
        }
    }

    private int emptyKeyCnt = 0;

    private static final int  MIN_EMPTY_KEY_THRESHOLD = 20;

    private boolean unexpectedWakeup(int selectCnt) {
        if (Thread.interrupted()) {
            LOGGER.debug("Selector.select() returned prematurely because of interruption");
            return true;
        }
        if (selector.keys().isEmpty()) {
            emptyKeyCnt++;
            LOGGER.warn("Selector keys is empty, no need to rebuild!");
            if (emptyKeyCnt >= MIN_EMPTY_KEY_THRESHOLD) {
                release();
            }
            return true;
        }
        emptyKeyCnt = 0;
        if (SELECTOR_AUTO_REBUILD_THRESHOLD > 0 &&
                selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD) {
            LOGGER.warn("Selector.select() returned prematurely {} times in a row; rebulding Selector {}", selectCnt, selector);
            rebuildSelector();
            return true;
        }
        return false;
    }

    private void rebuildSelector() {
        final Selector oldSelector = this.selector;
        final Selector newSelector;
        try {
            newSelector = Selector.open();
        } catch (IOException e) {
            LOGGER.error("failed to create a new Selector");
            return;
        }
        int nChannels = 0;
        for (SelectionKey key : oldSelector.keys()) {
            // 多线程环境下，对应的通道可能已经在newSelector上注册过
            try {
                if (!key.isValid() || key.channel().keyFor(newSelector) != null) {
                    continue;
                }
                int interestOps = key.interestOps();
                Object a = key.attachment();
                key.cancel();
                key.channel().register(newSelector, interestOps, a);
                nChannels++;
            } catch (Exception e) {
                LOGGER.warn("Failed to re-register a channel to the new selector.", e);
            }
        }
        this.selector = newSelector;
        try {
            oldSelector.close();
        } catch (Throwable e) {
            LOGGER.warn("Failed to close the old selector.", e);
        }
        LOGGER.info("Migrated {} channel(s) to the new selector.", nChannels);
    }

    public Selector getSelector() {
        return selector;
    }

    public SelectionKey getKey(SelectableChannel socket) {
        Objects.requireNonNull(socket, "socket");
        return socket.keyFor(selector);
    }

    public int getIndex() {
        return index;
    }

    public Queue<Runnable> getOnceTaskQueue() {
        return onceTaskQueue;
    }

    public Queue<Runnable> getTaskQueue() {
        return taskQueue;
    }

    public boolean hasTask() {
        return !taskQueue.isEmpty() || !readTaskQueue.isEmpty() ||
                !writeTaskQueue.isEmpty() || !connectTaskQueue.isEmpty() ||
                !acceptTaskQueue.isEmpty();
    }

    public int pendingTasks() {
        return taskQueue.size();
    }

    public Queue<Runnable> getReadTaskQueue() {
        return readTaskQueue;
    }

    public Queue<Runnable> getWriteTaskQueue() {
        return writeTaskQueue;
    }

    public Queue<Runnable> getConnectTaskQueue() {
        return connectTaskQueue;
    }

    public Queue<Runnable> getAcceptTaskQueue() {
        return acceptTaskQueue;
    }

    public void shutdown() {
        pool.shutdown();
    }

    public void shutdownNow() {
        pool.shutdownNow();
    }

    public boolean isShutDown() {
        return pool.isShutdown();
    }

    public boolean isShuttingDown() {
        return pool.isShuttingDown();
    }

    public boolean isTerminated() {
        return pool.isTerminated();
    }

    void cancel(SelectionKey key) {
        key.cancel();
        cancelledKeys++;
        if (cancelledKeys >= CLEANUP_INTERVAL) {
            cancelledKeys = 0;
            needsToSelectAgain = true;
        }
    }

    private static void handleLoopException(Throwable t) {
        LOGGER.warn("Unexpected exception in the selector loop.", t);

        // Prevent possible consecutive immediate failures that lead to
        // excessive CPU consumption.
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
            // Ignore.
//        }
    }

    public void closeAll() {
        selectAgain();
        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey key : keys) {
            Object o = key.attachment();
            if (o instanceof Handler) {
                // need to cancel key in close method
                ((Handler) o).close();
            } else {
                key.cancel();
            }
        }
    }

    private volatile boolean open = true;
    protected void cleanup() {
        open = false;
        try {
            selector.close();
        } catch (IOException e) {
            LOGGER.warn("Failed to close a selector.", e);
        }
    }

    private void release() {
        pool.removeChild(this);
    }
}
