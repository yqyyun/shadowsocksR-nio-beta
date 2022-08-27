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

package com.yqy.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * @author yqy
 * @date 2022/7/28 20:52
 */
public class EchoServer {

    private static final Logger LOGGER = LogManager.getLogger();

    private ServerSocket server;

    private String ip;

    private  int port;

    public EchoServer(String ip, int port) {
        try {
            this.server = new ServerSocket();
            this.server.setReuseAddress(true);
        } catch (IOException e) {

        }
        this.ip = ip;
        this.port = port;
    }

    public void startServer() {
        try {
            if (ip == null) {
                server.bind(new InetSocketAddress(port));
            } else {
                server.bind(new InetSocketAddress(ip, port));
            }
            run();
        } catch (IOException e) {
            LOGGER.error("fail to bind server to address ({}, {})", ip, port);
        }
    }

    public void run() {
        try {
            Socket client = server.accept();
            client.setTcpNoDelay(true);
            InputStream input = client.getInputStream();
            OutputStream output = client.getOutputStream();
            byte[] buf = new byte[1024];
            while (true) {
                int reads = input.read(buf);
                String recv = new String(buf, 0, reads, StandardCharsets.UTF_8);
                System.out.println("recv : " + recv);
                output.write(buf, 0, reads);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new EchoServer("127.0.0.1", 12131).startServer();
//        new EchoServer(null, 12131).startServer();
    }
}
