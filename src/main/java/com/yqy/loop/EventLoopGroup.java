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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yqy
 * @date 2022/7/27 08:13
 */
public class EventLoopGroup {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ThreadPoolExecutor boss;

    private final ThreadPoolExecutor worker;

    private final ThreadPoolExecutor ioPool;

    private final EventLoop[] eventLoops;

    private final AtomicInteger next = new AtomicInteger(0);

    private final int maxSize;

    private final SelectorProvider provider;


    public EventLoopGroup(int boss, int worker) {
        if (boss < 1) {
            throw new IllegalArgumentException("boss must large than 0");
        }
        this.maxSize = boss;
        this.boss = (ThreadPoolExecutor) Executors.newFixedThreadPool(boss);
        if (worker > 0) {
            this.worker = (ThreadPoolExecutor) Executors.newFixedThreadPool(worker);
        } else {
            this.worker = this.boss;
        }
        this.ioPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(boss << 1);
        this.eventLoops = new EventLoop[boss];
        this.provider = SelectorProvider.provider();
    }

    public EventLoopGroup(int boss) {
        this(boss, -1);
    }

    public EventLoopGroup() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public void submitIOTask(Runnable r) {
        boss.execute(r);
    }

    /**
     * ???????????????IO??????
     * attached???True?????????r???????????????IO?????????????????????????????????????????????????????????????????????????????????????????????
     * ?????????????????????????????????????????????????????????EventLoop????????????????????????????????????EventLoop???????????????????????????
     * attached???False????????????????????????????????????????????????IO?????????
     * @param r
     * @param attached ????????????attach???????????????EventLoop
     */
    public void submitIOTask(Runnable r, boolean attached) {
    }

    public void submitComputeTask(Runnable r) {
        worker.execute(r);
    }

    public void asyncSubmitIoTask(Runnable task, Runnable onComplete) {
        asyncSubmit(task, onComplete, boss);
    }

    public void asyncSubmitComputeTask(Runnable task, Runnable onComplete) {
        asyncSubmit(task, onComplete, worker);
    }

    private void asyncSubmit(Runnable task, Runnable onComplete, ThreadPoolExecutor worker) {
        Future<?> f = worker.submit(task);
        worker.execute(() ->{
            try {
                f.get();
                onComplete.run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
    }


    public void shutdown() {
        boss.shutdown();
        worker.shutdown();
    }

    public void shutdownNow() {
        boss.shutdownNow();
        worker.shutdownNow();
    }

    public boolean isShutdown() {
        return boss.isShutdown() && worker.isShutdown();
    }

    public boolean isShuttingDown() {
        return boss.isTerminating() && worker.isTerminating();
    }
    public boolean isTerminated() {
        return boss.isTerminated() && worker.isTerminated();
    }

    public ThreadPoolExecutor getIoThreadPool() {
        return boss;
    }

    public ThreadPoolExecutor getComputeThreadPool() {
        return worker;
    }

    /**
     *
     * @return
     */
    public EventLoop newChild() {
        EventLoop child = newUnsubmitChild();
        submitIOTask(child);
        return child;
    }

    public EventLoop newUnsubmitChild() {
        final int n = next.getAndIncrement();
        EventLoop child = eventLoops[n];

        if (child == null) {
            child = new EventLoop(this, provider, n);
            eventLoops[n] = child;
        }
        if (next.get() == maxSize) {
            next.compareAndSet(maxSize, 0);
        }
        return child;

    }

    public void removeChild(EventLoop eventLoop) {
        int index = eventLoop.getIndex();
        eventLoop.cleanup();
        eventLoops[index] = null;
    }
}
