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

import com.yqy.bean.Address;
import com.yqy.bean.TargetAddress;
import com.yqy.encrypto.Decryptor;
import com.yqy.encrypto.Encryptor;
import com.yqy.handler.SimpleDnsResolver;
import com.yqy.handler.UDPHandler;
import com.yqy.loop.EventLoop;
import com.yqy.loop.EventLoopGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.yqy.util.ByteBufferCommon;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * data channel as one local
 *
 * @author yqy
 * @date 2022/8/7 07:26
 */
public class UDPChannel {

    private static final Logger LOGGER = LogManager.getLogger();

    // TODO lru timeout
    private final Map<Address, UDPHandler> map = new ConcurrentHashMap<>();

    private final UDPHandler server;

    private final EventLoop eventLoop;

    private final EventLoopGroup group;

    private final Encryptor encryptor = null;

    private final Decryptor decryptor = null;

    public UDPChannel(UDPHandler server, EventLoop eventLoop, EventLoopGroup group) {
        this.server = server;
        this.eventLoop = eventLoop;
        this.group = group;
    }

    public void init() {
        server.onReadComplete((data, handler) -> {
            handleServer(data, handler);
        });
    }

    public void handleServer(ByteBuffer data, UDPHandler handler) {
        LOGGER.debug("handler server");
        LOGGER.debug("read data: {}", new String(data.array(), data.position(), data.limit()));
        data = ByteBufferCommon.preParseHeader(data);
        if (data == null) {
            return;
        }
        TargetAddress targetAddress = null;
        try {
            targetAddress = ByteBufferCommon.parseHeader(data);
        } catch (UnknownHostException e) {
            return;
        }
        if (targetAddress == null) {
//            return;
            targetAddress = new TargetAddress();
            InetSocketAddress remoteAddr = handler.getRemoteAddr();
            LOGGER.debug("remoteAddr: {}", remoteAddr);
            targetAddress.setDstAddr(remoteAddr.getAddress().getAddress());
            targetAddress.setDstPort(remoteAddr.getPort());
            targetAddress.setAtyp(1);
        }
        Address address;
        if (targetAddress.isHost()) {
            InetAddress resolve = SimpleDnsResolver.resolve(new String(targetAddress.getDstAddr()));
            address = new Address(resolve.getAddress(), targetAddress.getDstPort(), targetAddress.getAtyp());
        } else {
            address = new Address(targetAddress.getDstAddr(), targetAddress.getDstPort(), targetAddress.getAtyp());
        }
        UDPHandler rightHandler = map.get(address);
        if (rightHandler == null) {
            // new connection
            try {
                DatagramChannel ch = DatagramChannel.open();
                ch.connect(new InetSocketAddress(InetAddress.getByAddress(address.address), address.port));
                ch.configureBlocking(false);
                rightHandler = new UDPHandler(eventLoop, group, ch);
                map.put(address, rightHandler);
            } catch (IOException ignore) {
                return;
            }
        }
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LOGGER.debug("send data: {}", new String(data.array(), data.position(), data.remaining()));
        rightHandler.output(data);
        rightHandler.onWriteComplete(() -> {
            LOGGER.debug("send data is done!");
        });
        rightHandler.onReadComplete((data1, handler1) -> {
            handleClient(data1, handler1);
        });
    }

    public void handleClient(ByteBuffer data, UDPHandler handler) {
        LOGGER.debug("handle client");
        LOGGER.debug("read data: {}", new String(data.array(), data.position(), data.limit()));
        InetSocketAddress remoteAddr = handler.getRemoteAddr();
        InetAddress address = remoteAddr.getAddress();
        int port = remoteAddr.getPort();
        int datalen = data.remaining();
        ByteBuffer response;
        if (address instanceof Inet6Address) {
            //ipv6 19
            response = ByteBuffer.allocate(datalen + 19);
            response.order(ByteOrder.BIG_ENDIAN);
            response.put((byte) 0x04);
        } else {
            // ipv4 7
            response = ByteBuffer.allocate(datalen + 7);
            response.order(ByteOrder.BIG_ENDIAN);
            response.put((byte) 0x01);
        }
        response.put(address.getAddress());
        response.putShort((short) port);
        response.put(data);
        server.output(response);
        server.onWriteComplete(() -> {
            LOGGER.debug("send is done");
        });
    }

}
