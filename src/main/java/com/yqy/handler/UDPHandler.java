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
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 不缓存历史数据，每次读写都会覆盖缓冲区的内容。
 * 采用read/write方式完成读写
 * @author yqy
 * @date 2022/8/6 12:21
 */
public class UDPHandler implements Handler, Runnable {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    private final int id = idGenerator.getAndIncrement();

    private final EventLoop eventLoop;

    private final EventLoopGroup group;

    private final DatagramChannel socket;

    private SelectionKey key;

    private static final int BUF_SIZE = 65536;

    private InetSocketAddress localAddr;

    private InetSocketAddress remoteAddr;

    private volatile boolean isConnected = false;

    private volatile boolean open = true;

    private final Object closeLock = new Object();

    private final ByteBuffer input = ByteBuffer.allocate(BUF_SIZE);

    private final ByteBuffer output = ByteBuffer.allocate(BUF_SIZE);

    public UDPHandler(EventLoop eventLoop, EventLoopGroup group, DatagramChannel socket) throws IOException {
        this.eventLoop = eventLoop;
        this.group = group;
        this.socket = socket;
        key = eventLoop.register(socket);
        isConnected = socket.isConnected();
        if (isConnected) {
            localAddr = (InetSocketAddress) socket.getLocalAddress();
            remoteAddr = (InetSocketAddress) socket.getRemoteAddress();
        }
    }

    private void checkState() {
        if (!open) {
            throw new ClosedHandlerException("Handler had been closed already;id=" + id);
        }
    }

    public void addReadOps() {
        checkState();
        SelectionKey k = getValidKey();
        if (k == null) return;
        int i = k.interestOps();
        if ((i & SelectionKey.OP_READ) == 0) {
            k.interestOps(SelectionKey.OP_READ | i);
        }
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

    public void read(){
        checkState();
        try {
            input.clear();
            if (!isConnected) {
                SocketAddress rAddr = socket.receive(input);
                socket.connect(rAddr);
                remoteAddr = (InetSocketAddress) rAddr;
                localAddr = (InetSocketAddress) socket.getLocalAddress();
                isConnected = socket.isConnected();
            } else {
                socket.read(input);
            }
        } catch (IOException e) {
            LOGGER.error("error occurs on read");
        }
    }

    public void write() {
        checkState();
        try {
            if (!socket.isConnected()) {
                return;
            }
            socket.write(output);
        } catch (IOException e) {
            LOGGER.error("error occurs on write");
        } finally {
            removeWriteOps();
            output.clear();
        }
    }

    public void sendTo(InetSocketAddress address) {
        checkState();
        try {
            socket.send(output, address);
        } catch (IOException e) {
            LOGGER.error("error occurs on write");
        } finally {
            removeWriteOps();
            output.clear();
        }
    }

    public ByteBuffer[] input() {
        input.flip();
        if (!input.hasRemaining()) {
            return null;
        }
        ByteBuffer[] data = new ByteBuffer[1];
        data[0] = ByteBuffer.allocate(input.remaining());
        data[0].put(input);
        data[0].position(0);
        return data;
    }

    public void output(ByteBuffer data) {
        output.clear();
        output.put(data);
    }

    public void onReadComplete(ReadCompleteTask task) {
        checkState();
        eventLoop.attachTask(socket, SelectionKey.OP_READ, () -> {
            read();
            ByteBuffer[] input = input();
            if (input == null) {
                return;
            }
            task.read(input[0], this);
        });
    }

    public void onWriteComplete(Runnable task) {
        checkState();
        eventLoop.attachTask(socket, SelectionKey.OP_WRITE, () -> {
            write();
            task.run();
        });
    }

    public InetSocketAddress getLocalAddr() {
        return localAddr;
    }

    public InetSocketAddress getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public void close() {
        synchronized (closeLock) {
            if (!open) {
                return;
            }
            open = true;
            try {
                socket.close();
                input.position(0);
                input.limit(0);
                output.position(0);
                output.position(0);
            } catch (IOException e) {
                LOGGER.error("failed to close socket!");
            }
        }
    }

    @Override
    public void run() {

    }
}
