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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * @author yqy
 * @date 2022/8/7 09:13
 */
public class UDPTestUtil {
    private static final Scanner scanner = new Scanner(System.in);

    private static DatagramChannel ch;

    public static void main(String[] args) throws IOException {
        ch = DatagramChannel.open();
        ch.connect(new InetSocketAddress("127.0.0.1", 12345));
        for (; ; ) {
            System.out.println("enter which mode (send/recv)?");
            String mode = scanner.nextLine();
            if ("send".equals(mode.trim())) {
                send();
            } else if ("recv".equals(mode.trim())) {
                recv();
            } else if ("quit".equals(mode.trim())) {
                break;
            } else {
                System.out.println("Unknown Command!");
            }
        }
    }

    private static void send() {
        System.out.println("type quit will quit this mode");
        for (; ; ) {
            System.out.println("type message: ");
            String message = scanner.nextLine();
            if ("quit".equals(message.trim())) {
                break;
            }
            ByteBuffer data = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
            try {
                ch.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void recv() {
        System.out.println("type quit will quit this mode");
        final Thread mainThread = Thread.currentThread();
        new Thread(()-> {
            String s = scanner.nextLine();
            if ("quit".equals(s.trim())) {
                mainThread.interrupt();
            }
        }).start();
        while (!Thread.interrupted()) {
            ByteBuffer data = ByteBuffer.allocate(65536);
            try {
                ch.read(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            data.flip();
            String message = new String(data.array(), data.position(), data.limit());
            System.out.println(message);
        }
    }
}
