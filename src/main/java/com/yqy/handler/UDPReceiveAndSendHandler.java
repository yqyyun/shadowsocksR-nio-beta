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
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

/**
 * 不缓存历史数据，每次读写都会覆盖缓冲区的内容。
 * 采用read/write方式完成读写
 * @author yqy
 * @date 2022/8/6 12:21
 */
public class UDPReceiveAndSendHandler implements Handler, Runnable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final EventLoop eventLoop;

    private final EventLoopGroup group;

    private final DatagramChannel socket;

    private SelectionKey key;

    private static final int BUF_SIZE = 65536;

    private final InetSocketAddress localAddr;

    private final InetSocketAddress remoteAddr;

    private final ByteBuffer input = ByteBuffer.allocate(BUF_SIZE);

    private final ByteBuffer output = ByteBuffer.allocate(BUF_SIZE);

    public UDPReceiveAndSendHandler(EventLoop eventLoop, EventLoopGroup group, DatagramChannel socket) throws IOException {
        this.eventLoop = eventLoop;
        this.group = group;
        this.socket = socket;
        localAddr = (InetSocketAddress) socket.getLocalAddress();
        remoteAddr = (InetSocketAddress) socket.getRemoteAddress();
    }

    public void addReadOps() {
        SelectionKey k = getValidKey();
        if (k == null) return;
        int i = k.interestOps();
        if ((i & SelectionKey.OP_READ) == 0) {
            k.interestOps(SelectionKey.OP_READ | i);
        }
    }

    public void addWriteOps() {
        SelectionKey k = getValidKey();
        if (k == null) return;
        int i = k.interestOps();
        if ((i & SelectionKey.OP_WRITE) == 0) {
            k.interestOps(SelectionKey.OP_WRITE | i);
        }
    }

    public void removeReadOps() {
        SelectionKey k = getValidKey();
        if (k == null) return;
        int i = k.interestOps();
        if ((i & SelectionKey.OP_READ) != 0) {
            k.interestOps(i & ~SelectionKey.OP_READ);
        }
    }

    public void removeWriteOps() {
        SelectionKey k = getValidKey();
        if (k == null) return;
        int i = k.interestOps();
        if ((i & SelectionKey.OP_WRITE) != 0) {
            k.interestOps(i & ~SelectionKey.OP_WRITE);
        }
    }

    public SelectionKey getValidKey() {
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
        try {
            input.clear();
            socket.receive(input);
        } catch (IOException e) {
            LOGGER.error("error occurs on read");
        }
    }

    public void write() {
        try {
            socket.write(output);
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
        ByteBuffer[] data = new ByteBuffer[0];
        data[0] = ByteBuffer.allocate(input.remaining());
        data[0].put(input);
        return data;
    }

    public void output(ByteBuffer data) {
        output.clear();
        output.put(data);
    }

    public void onReadComplete(Runnable task) {
        eventLoop.attachTask(socket, SelectionKey.OP_READ, () -> {
            read();
            task.run();
        });
    }

    public void onWriteComplete(Runnable task) {
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
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.error("failed to close socket!");
        }
    }

    @Override
    public void run() {

    }
}
