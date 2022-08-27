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

package com.yqy.handler;

import com.yqy.loop.EventLoop;
import com.yqy.loop.EventLoopGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yqy
 * @date 2022/7/25 10:28
 */
public class SocketHandler implements Handler, Runnable {

    protected static final Logger LOGGER = LogManager.getLogger();

    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    private final int id = idGenerator.getAndIncrement();

    private final EventLoop eventLoop;

    private final EventLoopGroup group;

    private final SocketChannel socket;

    private volatile SelectionKey key;

    public static final int BUF_SIZE = 1024 * 1024;

    public final ByteBuffer input = ByteBuffer.allocate(BUF_SIZE);

    public final ByteBuffer output = ByteBuffer.allocate(BUF_SIZE);

    private Runnable onReadCompleteTask;

    private Runnable onWriteCompleteTask;

    private volatile boolean open = true;

    private final Object closeLock = new Object();


    public SocketHandler(EventLoop eventLoop, SocketChannel socket, EventLoopGroup group) {
        this.eventLoop = eventLoop;
        this.socket = socket;
        this.group = group;
        init();
    }

    private void init() {
        if (!socket.isRegistered()) {
            try {
                socket.configureBlocking(false);
                key = eventLoop.register(socket);
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error("failed to register channel with selector;get closed anyway");
                close();
            }
        }
    }

    /**
     * @throws ClosedHandlerException if this handler is closed
     */
    public void addReadOps() {
        checkState();
        SelectionKey k = getValidKey();
        if (k == null) return;
        int i = k.interestOps();
        if ((i & SelectionKey.OP_READ) == 0) {
            k.interestOps(SelectionKey.OP_READ | i);
        }
    }

    private void checkState() {
        if (!open) {
            throw new ClosedHandlerException("Handler had been closed already;id=" + id);
        }
    }

    public void startWrite() {
        checkState();
        eventLoop.attachTask(socket, SelectionKey.OP_WRITE, () -> {
            try {
                write();
                if (onWriteCompleteTask != null) {
                    onWriteCompleteTask.run();
                }
            } catch (ClosedHandlerException ignore) {
                LOGGER.warn("closedHandlerException");
            }
        });
    }

    public void startRead() {
        checkState();
        eventLoop.attachTask(socket, SelectionKey.OP_READ, () -> {
            read();
            if (onReadCompleteTask != null) {
                onReadCompleteTask.run();
            }
        });
    }

    public void addWriteOps() {
        checkState();
        SelectionKey k = getValidKey();
        if (k == null) return;
        int i = k.interestOps();
        if ((i & SelectionKey.OP_WRITE) == 0) {
            k.interestOps(SelectionKey.OP_WRITE | i);
        }
    }

    public void removeReadOps() {
        checkState();
        SelectionKey k = getValidKey();
        if (k == null) return;
        int i = k.interestOps();
        if ((i & SelectionKey.OP_READ) != 0) {
            k.interestOps(i & ~SelectionKey.OP_READ);
        }
    }

    public void removeWriteOps() {
        checkState();
        SelectionKey k = getValidKey();
        if (k == null) return;
        int i = k.interestOps();
        if ((i & SelectionKey.OP_WRITE) != 0) {
            k.interestOps(i & ~SelectionKey.OP_WRITE);
        }
    }

    /**
     * // fixed checkState
     *
     * @return
     */
    public SelectionKey getValidKey() {
        checkState();
        SelectionKey k = this.key;
        if (!k.isValid()) {
            k = eventLoop.getKey(socket);
            if (k == null) {
                throw new IllegalStateException("socket not registered yet!");
            }
            this.key = k;
        }
        if (!k.isValid()) {
            return null;
        }
        return k;
    }

    public void read() {
        // limt = cap , pos is read index
        checkState();
        int readBytes = 0;
        int recv = 0;
        boolean error = false;
        try {
            readBytes = socket.read(input);
            if (readBytes == -1) {
                LOGGER.warn("id({}) the channel has reached end-of-stream;get closed!", id);
                error = true;
            }
            if (readBytes == 0) {
                return;
            }
            recv += readBytes;
        } catch (IOException ignore) {
            LOGGER.error("id({}) error occurs when read socket;get closed", id, ignore);
            error = true;
        } finally {
            if (error) {
                close();
            }
        }
    }


    public void write() {
        // lim = cap, pos is wait to writing so have to change it
        checkState();
        ByteBuffer data = output;
        data.flip();
        if (!data.hasRemaining()) {
            removeWriteOps();
            return;
        }
        int writeBytes = 0;
        boolean error = false;
        try {
            writeBytes = socket.write(data);
        } catch (IOException ignore) {
            LOGGER.error("id({}) failed to write data to socket!; then closed", id);
            error = true;
        } finally {
            if (error) {
                close();
            }
            if (!data.hasRemaining()) {
                removeWriteOps();
                data.compact();
            }
        }
    }

    public ByteBuffer input() {
        //
        checkState();
        // flip
        try {
            input.flip();
            if (!input.hasRemaining()) {
                return null;
            }
            ByteBuffer data = ByteBuffer.allocate(input.remaining());
            data.put(input);
            data.position(0);
            return data;
        } finally {
            input.clear();
        }
    }

    /**
     * put data into output buffer whose data will be written into socket at a later point in time.
     * if output buffer has no enough space for data, this method will throw HandlerException.
     *
     * @param data
     * @throws HandlerException
     */
    public void output(ByteBuffer data) {
        checkState();
        int avail = output.remaining();
        int len = data.remaining();
        if (avail < len) {
            throw new BufferOverflowException();
        }
        output.put(data);
    }


    private final Reader reader = new Reader() {

        /**
         * non-blocking read
         * @param dst
         * @return
         */
        @Override
        public int raed(ByteBuffer dst) {
            outer.checkState();
            return 0;
        }

        /**
         * non-blocking read
         * @return
         */
        @Override
        public ByteBuffer readAll() {
            return outer.input();
        }

    };

    private final SocketHandler outer = this;
    private final Writer writer = new Writer() {

        /**
         * non-blocking write
         * @param src
         * @return
         */
        @Override
        public int write(ByteBuffer src) {
            outer.checkState();
            int remaining = src.remaining();
            outer.output(src);
            eventLoop.attachTask(socket, SelectionKey.OP_WRITE, () -> {
                outer.write();
                if (onReadCompleteTask != null) {
                    onReadCompleteTask.run();
                }
            });
            return remaining - src.remaining();
        }
    };

    public Reader getReader() {
        checkState();
        return reader;
    }

    public Writer getWriter() {
        checkState();
        return writer;
    }

    public void onReadComplete(Runnable r) {
        checkState();
        Runnable task = () -> {
            read();
            r.run();
        };
        eventLoop.attachTask(socket, SelectionKey.OP_READ, task);
        onReadCompleteTask = r;
    }

    public void onWriteComplete(Runnable r) {
        checkState();
        Runnable writeTask = () -> {
            write();
            r.run();
        };
        eventLoop.attachTask(socket, SelectionKey.OP_WRITE, writeTask);
        onWriteCompleteTask = r;
    }


    @Override
    public void run() {
    }

    public boolean isClosed() {
        return !open;
    }

    public int getId() {
        return id;
    }

    public EventLoop getEventLoop() {
        return eventLoop;
    }

    public SocketChannel getSocketChannel() {
        return socket;
    }

    public int getInputRemaing() {
        return input.remaining();
    }

    public int getOutputRemaing() {
        return output.remaining();
    }

    private InetSocketAddress localAddress;

    private InetSocketAddress remoteAddress;

    public String getLocalHostName() {
        if (localAddress == null) {
            try {
                localAddress = ((InetSocketAddress) socket.getLocalAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (localAddress == null) {
            return null;
        }
        return localAddress.getHostString();
    }

    public String getRemoteHostName() {
        if (remoteAddress == null) {
            try {
                remoteAddress = (InetSocketAddress) socket.getRemoteAddress();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (remoteAddress == null) {
            return null;
        }
        return remoteAddress.getHostString();
    }

    @Override
    public void close() {
        synchronized (closeLock) {
            if (!open) {
                return;
            }
            try {
                open = false;
                eventLoop.getKey(socket).attach(null);
                socket.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close the socket!", e);
            }
        }
    }
}
