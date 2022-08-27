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

import com.yqy.bean.Socks5Packet;
import com.yqy.encrypto.StreamCipher;
import com.yqy.encrypto.UnknownCipherMethodException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * @author yqy
 * @date 2022/7/27 10:35
 */
public class Common {

    private static final Logger LOGGER = LogManager.getLogger();

    public static boolean checkAddress(String address) {
        return false;
    }

    public static boolean checkPort(String p) {
        try {
            int port = Integer.parseInt(p);
            return checkPort(port);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean checkPort(int port) {
        return port >= 0 && port <= 65535;
    }

    public static boolean checkMethod(String method) {
        try {
            return StreamCipher.Method.getMethodInfo(method) != null;
        } catch (UnknownCipherMethodException e) {
            return false;
        }
    }

    public static boolean checkProtocol(String arg) {
        //TODO
        return true;
    }

    public static boolean checkObfs(String arg) {
        // TODO
        return true;
    }

    public static boolean checkProtocolParams(String arg) {
        // TODO
        return true;
    }

    public static boolean checkObfsParams(String arg) {
        // TODO
        return true;
    }

    public static boolean isNumber(String arg) {
        // TODO
        return true;
    }

    public static boolean checkPath(String arg) {
        return new File(arg).exists();
    }

    public static byte[] preParseHeader(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        int len = data.length;
        int dataType = char2Int((char) data[0]);
        if (dataType == 0x80) {
            if (len <= 2)
                return null;
            int randDataSize = char2Int((char) data[1]);
            if (randDataSize + 2 >= len) {
                LOGGER.warn("head too short, maybe wrong password or encryption method");
                return null;
            }
            data = Arrays.copyOfRange(data, randDataSize + 2, len);
        } else if (dataType == 0x81) {
            data = Arrays.copyOfRange(data, 1, len);
        } else if (dataType == 0x82) {
            if (len <= 3)
                return null;
            int randDataSize = toShort(data[1], data[2]);
            if (randDataSize + 3 >= len) {
                LOGGER.warn("header too short, maybe wrong password or encryption method");
                return null;
            }
            data = Arrays.copyOfRange(data, randDataSize + 3, len);
        } else if (dataType == 0x88 || (~dataType & 0xff) == 0x88) {
            if (len < 7 + 7)
                return null;
            int dataSize = toShort(data[1], data[2]);
            byte[] ornData = data;
            data = Arrays.copyOfRange(data, 0, dataSize);
            long crc = crc32(data);
            if (crc != 0xffffffff) {
                LOGGER.warn("incorrect CRC32,maybe wrong password or encryption method");
                return null;
            }
            int startPos = 3 + char2Int((char) data[3]);
            data = Arrays.copyOfRange(data, startPos, len - 4);
            if (dataSize < len) {
                byte[] newData = new byte[data.length + (len - dataSize)];
                System.arraycopy(data, 0, newData, 0, data.length);
                System.arraycopy(ornData, dataSize, newData, data.length, len - dataSize);
                data = newData;
            }
        }
        return data;
    }

    public static Socks5Packet parseHeader(byte[] data) throws UnknownHostException {
        int atyp = char2Int((char) data[0]);
        int connectType = (atyp & 0x8) != 0 ? 1 : 0;
        byte[] dstAddr = null;
        int dstPort = -1;
        InetAddress dstAddress = null;
        int headLen = 0;
        atyp &= ~0x8;
        if (atyp == Socks5Packet.ATYP_IPV4) {
            if (data.length >= 7) {
                dstAddr = Arrays.copyOfRange(data, 1, 5);
                dstAddress = InetAddress.getByAddress(dstAddr);
                dstPort = toShort(data[5], data[6]);
                headLen = 7;
            } else {
                LOGGER.warn("header is too short");
            }
        } else if (atyp == Socks5Packet.ATYP_HOST) {
            if (data.length > 2) {
                int addrLen = char2Int((char) data[1]);
                if (data.length >= 4 + addrLen) {
                    dstAddr = Arrays.copyOfRange(data, 2, 2 + addrLen);
                    dstAddress = InetAddress.getByName(new String(dstAddr, StandardCharsets.UTF_8));
                    dstPort = toShort(data[2 + addrLen], data[3 + addrLen]);
                    headLen = 4 + addrLen;
                } else
                    LOGGER.warn("header is too short");
            } else
                LOGGER.warn("header is too short");

        } else if (atyp == Socks5Packet.ATYP_IPV6) {
            if (data.length >= 19) {
                dstAddr = Arrays.copyOfRange(data, 1, 17);
                dstPort = toShort(data[17], data[18]);
                dstAddress = InetAddress.getByAddress(dstAddr);
                headLen = 19;
            } else
                LOGGER.warn("header is too short");
        } else {
            LOGGER.warn("unsupported atyp {}, maybe wrong password or encryption method ", atyp);
        }

        if (dstAddress != null) {
            Socks5Packet socks5Packet = new Socks5Packet();
            socks5Packet.setAtyp(atyp);
            socks5Packet.setDstAddr(dstAddr);
            socks5Packet.setDstPort(dstPort);
            socks5Packet.setDstAddress(dstAddress);
            socks5Packet.setConnectType(connectType);
            socks5Packet.setHeaderLen(headLen);
            return socks5Packet;
        }
        return null;
    }

    /**
     * 如果是是字符串表示的数字，就将其转为对应的int类型的数字
     * 如果不是数字，就返回对应的ascii码值
     * '1' -> 1
     * 'a' -> 97
     *
     * @param ch
     * @return
     */
    public static int char2Int(char ch) {
        if (ch >= 0x30 && ch <= 0x39) {
            return ch - 0x30;
        } else {
            return ch;
        }
    }

    public static short toShort(byte b1, byte b2) {
        return (short) (((b1 & 0xff) << 8) |
                (b2 & 0xff));
    }

    public static long crc32(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }

    public static void main(String[] args) throws IOException {
        byte b = -1;
        System.out.println(Integer.toBinaryString(((byte)Short.parseShort("128")) & 0xff));
        System.out.println(Integer.toBinaryString(128));
        System.out.println((byte)130);
    }
}
