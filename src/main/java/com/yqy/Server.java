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

package com.yqy;

import com.yqy.config.Configuration;
import com.yqy.data.Channel;
import com.yqy.data.UDPChannel;
import com.yqy.handler.SocketHandler;
import com.yqy.handler.UDPHandler;
import com.yqy.loop.EventLoop;
import com.yqy.loop.EventLoopGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @author yqy
 * @date 2022/7/27 13:03
 */
public class Server {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        Configuration  config = Configuration.loadConfig(args);
        if (config == null) {
            LOGGER.error("fail to load configuration, system exist  with 1");
            System.exit(1);
        }
        processSystemProperty(config);
        Service service = new Service(config);
        EventLoopGroup group = service.getEventLoopGroup();
        EventLoop bossLoop = service.getBossLoop();
        try {
            startTcpRelay(config, group, bossLoop);
            if (!config.getBoolean("disableudp", false)) {
                startUdpRelay(config, group);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        service.startService();
    }

    private static void processSystemProperty(Configuration config) {
        System.setProperty(SocketHandler.BUF_SIZE_KEY, config.get(SocketHandler.BUF_SIZE_KEY, "null"));
    }

    private static void startTcpRelay(Configuration config, EventLoopGroup group, EventLoop bossLoop) throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(config.get("server", null), config.getInt("server_port", 0)));
        server.configureBlocking(false);
        bossLoop.registerWithAttachment(server, SelectionKey.OP_ACCEPT, () -> {
            try {
                SocketChannel accept = server.accept();
                if (accept != null) {
                    accept.setOption(StandardSocketOptions.TCP_NODELAY, true);
                    EventLoop eventLoop = group.newChild();
                    SocketHandler leftHandler = new SocketHandler(eventLoop, accept, group);
                    new Channel(leftHandler, eventLoop, group, config).begin();
                }
            } catch (IOException ignore) { }
        });
        LOGGER.info("tcp relay has been started!");
    }

    private static void startUdpRelay(Configuration config, EventLoopGroup group) throws IOException {
        DatagramChannel udpServer = DatagramChannel.open();
        udpServer.socket().bind(new InetSocketAddress(config.get("server", null), config.getInt("server_port", 0)));
        udpServer.configureBlocking(false);
        EventLoop udpLoop = group.newChild();
        udpLoop.register(udpServer);
        UDPHandler udpHandler = new UDPHandler(udpLoop, group, udpServer);
        new UDPChannel(udpHandler, udpLoop, group).init();
        LOGGER.info("udp relay has been started!");
    }
}
