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

package com.yqy.data;

import com.yqy.bean.ObfsServerDecodedMessage;
import com.yqy.bean.ProtocolDecryptedMessage;
import com.yqy.bean.ServerInfo;
import com.yqy.bean.TargetAddress;
import com.yqy.buffer.LinkedByteBuffer;
import com.yqy.config.Configuration;
import com.yqy.encrypto.*;
import com.yqy.handler.ClosedHandlerException;
import com.yqy.handler.SimpleDnsResolver;
import com.yqy.handler.SocketHandler;
import com.yqy.loop.EventLoop;
import com.yqy.loop.EventLoopGroup;
import com.yqy.obfs.NoSupportedObfsMethodException;
import com.yqy.obfs.ObfsCodec;
import com.yqy.obfs.UnknownObfsMethodException;
import com.yqy.protocol.NosupportedProtocolMethodException;
import com.yqy.protocol.Protocol;
import com.yqy.protocol.UnknownProtocolMethodException;
import com.yqy.util.ByteBufferCommon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yqy
 * @date 2022/8/3 12:48
 */
public class Channel {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final AtomicInteger idGenerator = new AtomicInteger(0);
    public static final ByteBuffer ZERO_BUFFER = ByteBuffer.allocate(0);

    private final int id = idGenerator.getAndIncrement();

    private SocketHandler leftHandler;
    private SocketHandler rightHandler;

    private EventLoop eventLoop;

    private EventLoopGroup group;

    private final Configuration config;

    private final StreamCipher.Method methodInfo;

    private final byte[] key;

    private Encryptor encryptor;

    private Decryptor decryptor;

    private Protocol protocol;

    private ObfsCodec obfsCodec;

    private TargetAddress targetAddress;

    private TargetAddress requestAddress;

    private boolean open = true;

    private final Object closeLock = new Object();

    private final LinkedByteBuffer leftToRightBuffer = new LinkedByteBuffer();

    private final LinkedByteBuffer rightToLeftBuffer = new LinkedByteBuffer();

    public Channel(SocketHandler leftHandler, EventLoop eventLoop, EventLoopGroup group, Configuration config) {
        this(leftHandler, null, eventLoop, group, config);
    }

    public Channel(EventLoop eventLoop, EventLoopGroup group, SocketHandler rightHandler, Configuration config) {
        this(null, rightHandler, eventLoop, group, config);
    }

    public Channel(SocketHandler leftHandler, SocketHandler rightHandler, EventLoop eventLoop,
                   EventLoopGroup group, Configuration config) {
        this.leftHandler = leftHandler;
        this.rightHandler = rightHandler;
        this.eventLoop = eventLoop;
        this.group = group;
        this.config = config;
        this.key = config.get("password", "").getBytes(StandardCharsets.UTF_8);
        String method = config.get("method", null);
        // TODO MethodInfo
        StreamCipher.Method methodInfo = null;
        try {
            methodInfo = StreamCipher.Method.getMethodInfo(method);
        } catch (UnknownCipherMethodException e) {
        }
        this.methodInfo = methodInfo;
        try {
            this.encryptor = new Encryptor(method, key, null, false);
        } catch (EncryptorException e) {
            e.printStackTrace();
        }
        try {
            this.protocol = Protocol.getInstance(Protocol.Method.getMethodInfo(config.get("protocol", null)));
            ServerInfo serverInfo = protocol.serverInfo();
            serverInfo.setHost(config.get("server", null));
            serverInfo.setPort(config.getInt("server_port", 0));
//            serverInfo.setUpdateUserFunc();
            serverInfo.setHeadLen(30);
            serverInfo.setTcpMss(1500);
            serverInfo.setOverhead((short) 8);
            serverInfo.setKey(this.encryptor.getKey());
            serverInfo.setIv(encryptor.getIv());
            serverInfo.setProtocolParam("");
        } catch (NosupportedProtocolMethodException | UnknownProtocolMethodException e) {
            e.printStackTrace();
        }
        try {
            this.obfsCodec = ObfsCodec.getInstance(ObfsCodec.Method.getMethodInfo(config.get("obfs", null)));
        } catch (NoSupportedObfsMethodException | UnknownObfsMethodException e) {
            e.printStackTrace();
        }
//        begin();
    }

    public void setLeftHandler(SocketHandler leftHandler) {
        this.leftHandler = leftHandler;
    }

    public void setRightHandler(SocketHandler rightHandler) {
        this.rightHandler = rightHandler;
    }

    public void begin() {
        SocketHandler leftHandler = this.leftHandler;
        if (open) {
            leftHandler.onReadComplete(() -> leftInitProcess());
            leftHandler.startRead();
        }
    }

    public void leftInitProcess() {
//        LOGGER.debug("left init Process id({})", id);
        if (leftHandler.isClosed()) {
            close();
            return;
        }
        if (leftHandler.getTotalRecvBytes() < methodInfo.getIvLen()) {
            return;
        }
        ByteBuffer input = leftHandler.input();
        if (decryptor == null) {
            byte[] iv = new byte[methodInfo.getIvLen()];
            input.get(iv);
            try {
                this.decryptor = new Decryptor(methodInfo.getName(), key, iv, false);
                this.protocol.getServerInfo().setRecv_iv(iv);
            } catch (DecryptorException e) {
                e.printStackTrace();
            }

        }
        input = processLeft(input);
        input = ByteBufferCommon.preParseHeader(input);
        if (input == null) {
//            close();
            return;
        }
        TargetAddress targetAddress = null;
        try {
            targetAddress = ByteBufferCommon.parseHeader(input);
//            LOGGER.debug("id({}) target Address: {}", id, targetAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if (targetAddress == null) {
            close();
            return;
        }
        this.targetAddress = targetAddress;
        String hostname = new String(targetAddress.getDstAddr(), StandardCharsets.UTF_8);
        if (IpBlackList.contains(hostname)) {
            close();
            return;
        }
        if (targetAddress.isHost()) {
            InetAddress resolve = SimpleDnsResolver.resolve(hostname);
            if (resolve == null) {
                LOGGER.error("Failed to get the ip address of hostname: {}", hostname);
                close();
                return;
            }
            targetAddress.setResolvedAddress(resolve);
        } else {
            try {
                targetAddress.setResolvedAddress(InetAddress.getByAddress(targetAddress.getDstAddr()));
            } catch (UnknownHostException ignore) {
            }
        }
        try {
            SocketChannel target = SocketChannel.open();
            String bind = config.get("bind", "0.0.0.0");
            target.bind(new InetSocketAddress(bind, 0));
            target.configureBlocking(false);
            target.connect(new InetSocketAddress(targetAddress.getResolvedAddress(), targetAddress.getDstPort()));
            this.rightHandler = new SocketHandler(eventLoop, target, group);

            eventLoop.attachTask(target, SelectionKey.OP_CONNECT, () -> {
                try {
//                    target.socket().setTcpNoDelay(true);
                    target.setOption(StandardSocketOptions.TCP_NODELAY, true);
                } catch (IOException ignore) {
                }
                if (!target.isConnected()) {
                    LOGGER.debug("connected is failed!, put hostname({}) into the ip blacklist", hostname);
                    IpBlackList.add(hostname);
                    leftHandler.getEventLoop().detachTask(leftHandler.getSocketChannel(), SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    rightHandler.getEventLoop().detachTask(rightHandler.getSocketChannel(), SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    close();
                }

            });
            if (input.hasRemaining()) {
                rightHandler.getWriter().write(input);
                if (input.hasRemaining()) {
                    leftToRightBuffer.add(input);
                }
                rightHandler.startWrite();
            }
            leftHandler.onReadComplete(() -> leftStreamProcess());
            rightHandler.onReadComplete(() -> rightStreamProcess());
            leftHandler.startRead();
            rightHandler.startRead();

            writeToSocket(leftHandler, rightToLeftBuffer);
            writeToSocket(rightHandler, leftToRightBuffer);
        } catch (IOException | ClosedHandlerException e) {
            LOGGER.error("something wrong in the id({}) channel left init process;get closed anyway", id, e);
            close();
        }
    }

    private void writeToSocket(SocketHandler to, LinkedByteBuffer fromToBuffer) {
        to.onWriteComplete(() -> {
            if (fromToBuffer.getSize() > 0) {
                ByteBuffer buffer = fromToBuffer.removeFirst();
                to.getWriter().write(buffer);
                if (buffer.hasRemaining()) {
                    fromToBuffer.addFirst(buffer);
                }
                to.startWrite();
            }
        });
        to.startWrite();
    }

    public void leftStreamProcess() {
//        LOGGER.debug("id({}) {} left stream process", id, targetAddress);
        if (!open) {
            return;
        }
        try {
            if (leftHandler.isClosed()) {
                close();
                return;
            }
            ByteBuffer input = leftHandler.getReader().readAll();
            if (input == null) {
                return;
            }
            input = processLeft(input);
            leftToRightBuffer.add(input);
            input = leftToRightBuffer.removeFirst();
            rightHandler.getWriter().write(input);
            if (input.hasRemaining()) {
                leftToRightBuffer.addFirst(input);
            }
            rightHandler.startWrite();
        } catch (Throwable e) {
            LOGGER.error("id({}) something wrong in the channel left Stream process;get closed anyway", id, e);
            close();
        }
    }

    public void rightStreamProcess() {
//        LOGGER.debug("id({}) {} right stream process", id, targetAddress);
        if (!open) {
            return;
        }
        try {
            if (rightHandler.isClosed()) {
                close();
                return;
            }
            ByteBuffer input = rightHandler.getReader().readAll();
            if (input == null) {
                return;
            }
            input = processRight(input);
            // BUG fixed; bug: data may be lost, so that some web page can't be open normally
            rightToLeftBuffer.add(input);
            input = rightToLeftBuffer.removeFirst();
            leftHandler.getWriter().write(input);
            if (input.hasRemaining()) {
                rightToLeftBuffer.addFirst(input);
            }
            leftHandler.startWrite();
        } catch (Throwable e) {
            LOGGER.error("id({}) something wrong in the channel right Stream process;get closed anyway", id, e);
            close();
        }
    }

    private ByteBuffer processLeft(ByteBuffer input) {
        ObfsServerDecodedMessage obfsServerDecodedMessage = obfsCodec.serverDecode(input);
        if (obfsServerDecodedMessage.needDecrypt) {
            input = decryptor.decrypt(obfsServerDecodedMessage.data);
        } else {
            input = obfsServerDecodedMessage.data;
        }
        ProtocolDecryptedMessage protocolDecryptedMessage = protocol.serverPostDecrypt(input);
        if (protocolDecryptedMessage.sendBack) {
            ByteBuffer buffer = protocol.serverPreEncrypt(ZERO_BUFFER);
            ByteBuffer encrypt = encryptor.encrypt(buffer);
            ByteBuffer sendback = obfsCodec.serverEncode(encrypt);
            leftHandler.getWriter().write(sendback);
            leftHandler.startWrite();
        }
        input = protocolDecryptedMessage.data;
        return input;
    }

    private ByteBuffer processRight(ByteBuffer input) {
        input = protocol.serverPreEncrypt(input);
        input = encryptor.encrypt(input);
        input = obfsCodec.serverEncode(input);
        return input;
    }

    public int getId() {
        return id;
    }

    public void cleanup() {

    }

    public void close() {
        synchronized (closeLock) {
            if (!open) {
                return;
            }
            open = false;
            if (leftHandler != null) {
                leftHandler.close();
            }
            if (rightHandler != null) {
                rightHandler.close();
            }
            cleanup();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress("127.0.0.1", 12345));
        server.configureBlocking(false);
        EventLoopGroup group = new EventLoopGroup(3);
        EventLoop eventLoop = group.newChild();
        eventLoop.registerWithAttachment(server, SelectionKey.OP_ACCEPT, () -> {
            try {
                LOGGER.info("accept event");
                SocketChannel socket = server.accept();
                if (socket != null) {
                    EventLoop newLoop = group.newChild();
                    SocketHandler left = new SocketHandler(newLoop, socket, group);
                    Channel channel = new Channel(left, newLoop, group, null);
                    channel.begin();
                    LOGGER.debug("channel id({}) has begined! eventLoop index({}); leftHandler id({}) eventloop index({})",
                            channel.getId(), newLoop.getIndex(), left.getId(), newLoop.getIndex());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
