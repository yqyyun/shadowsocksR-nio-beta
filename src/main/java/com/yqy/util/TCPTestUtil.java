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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

/**
 * @author yqy
 * @date 2022/7/28 20:28
 */
public class TCPTestUtil {

    private static final Logger LOGGER = LogManager.getLogger();

    private Socket socket = new Socket();

    private String ip;

    private int port;

    public TCPTestUtil(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void init() {
        try {
            socket.connect(new InetSocketAddress(ip, port));
            socket.setTcpNoDelay(true);
            socket.setReuseAddress(true);
        } catch (IOException e) {
            LOGGER.error("fail to connect to address ({}, {}), error message {}",
                    ip, port, e);
        }
    }

    public void send(String message) {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        try {
            socket.getOutputStream().write(bytes);
        } catch (IOException e) {
            LOGGER.error("error occur when send message to address ({}, {})", ip, port);
        }
    }

    public void send(byte[] data) {
        try {
            socket.getOutputStream().write(data);
        } catch (IOException e) {
            LOGGER.error("error occur when send message to address ({}, {})", ip, port);
        }
    }

    public void send(char[] chars) {
        byte[] data = new byte[chars.length * 2];
        for (int i = 0; i < chars.length; i++) {
            int j = i * 2;
            data[j] = (byte) (chars[i] >>> 8 & 0xff);
            data[j + 1] = (byte) (chars[i] & 0xff);
        }
        try {
            socket.getOutputStream().write(data);
        } catch (IOException e) {
            LOGGER.error("error occur when send message to address ({}, {})", ip, port);
        }
    }

    public void send(int[] nums) {
        byte[] data = new byte[nums.length * 4];
        for (int i = 0; i < nums.length; i++) {
            int j = i * 4;
            data[j] = (byte) (nums[i] >>> 24 & 0xff);
            data[j + 1] = (byte) (nums[i] >>> 16 & 0xff);
            data[j + 2] = (byte) (nums[i] >>> 8 & 0xff);
            data[j + 3] = (byte) (nums[i] & 0xff);
        }
        try {
            socket.getOutputStream().write(data);
        } catch (IOException e) {
            LOGGER.error("error occur when send message to address ({}, {})", ip, port);
            close();
        }
    }

    public String recv() {
        String recv = null;
        try {
            InputStream input = socket.getInputStream();
            int remaining = input.available();
            byte[] buf = new byte[remaining];
            int read = input.read(buf);
            recv = new String(buf, 0, read, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return recv;
    }

    public void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.error("fail to close socket");
            }
        }
    }

    public void send(String kind, Scanner input) {
        while (!socket.isClosed()) {
            System.out.println("type your " + kind + " data list: ");
            String line = input.nextLine();
            try {
                if ("quit".equals(line)) {
                    break;
                } else if ("byte".equals(kind)) {
                    String[] splits = line.split("\\s");
                    byte[] data = new byte[splits.length];
                    for (int i = 0; i < splits.length; i++) {
                        data[i] = (byte) (Integer.parseInt(splits[i]) & 0xff);
                    }
                    System.out.println("prepare to send data: " + Arrays.toString(data));
                    send(data);
                } else if ("char".equals(kind)) {
                    String[] splits = line.split("\\s");
                    char[] data = new char[splits.length];
                    for (int i = 0; i < splits.length; i++) {
                        data[i] = (char) (Integer.parseInt(splits[i]) & 0xffff);
                    }
                    System.out.println("prepare to send data: " + Arrays.toString(data));
                    send(data);
                } else if ("int".equals(kind)) {
                    String[] splits = line.split("\\s");
                    int[] data = new int[splits.length];
                    for (int i = 0; i < splits.length; i++) {
                        data[i] = Integer.parseInt(splits[i]);
                    }
                    System.out.println("prepare to send data: " + Arrays.toString(data));
                    send(data);
                } else if ("string".equals(kind)) {
                    System.out.println("prepare to send data: " + line);
                    send(line);
                } else {
                    System.out.println("Unknown data type: " + kind);
                    break;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        byte[] buf = new byte[1024];
        int reads;
        while (!socket.isClosed()) {
            System.out.println("you wanna to send message(send), or recv message(recv)?(send/recv)");
            String line = scanner.nextLine();
            if ("send".equals(line)) {
                System.out.println("which kind of data format do you want to send?(byte/char/int/string)");
                String kind = scanner.nextLine();
                send(kind, scanner);
            } else if ("recv".equals(line)) {
                Thread t = new Thread(() -> {
                    while (!Thread.interrupted() && !socket.isClosed()) {
                        String recv = recv();
                        if (!recv.trim().equals("")) {
                            System.out.print(recv);
                        }
                    }
                });
                t.start();
                do {
                } while (!"quit".equals(scanner.nextLine()));
                t.interrupt();
            } else if ("quit".equals(line)) {
                break;
            } else {
                System.out.println("Wrong Command!");
            }
        }
        if (socket.isClosed()) {
            System.out.println("connection is disconnected, do you want to reconnect?(y/n): ");
            if (scanner.nextLine().equals("y")) {
                socket = new Socket();
                init();
                run();
            }
        }
    }

    public static void main(String[] args) {
        TCPTestUtil client = new TCPTestUtil("127.0.0.1", 12345);
        client.init();
        client.run();

    }
}
