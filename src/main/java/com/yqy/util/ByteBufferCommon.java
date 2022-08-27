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

import com.yqy.bean.TargetAddress;
import com.yqy.encrypto.StreamCipher;
import com.yqy.encrypto.UnknownCipherMethodException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * @author yqy
 * @date 2022/7/27 10:35
 */
public class ByteBufferCommon {

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

    public static ByteBuffer preParseHeader(final ByteBuffer data) {
        if (data == null || data.remaining() == 0) {
            return null;
        }
        int len = data.remaining();
        int offset = data.position();
        // data[0]
        int dataType = char2Int((char) data.get());
        if (dataType == 0x80) {
            LOGGER.debug("parse dataType = 0x80 ...");
            if (len <= 2)
                return null;
            // data[1]
            int randDataSize = char2Int((char) data.get());
            if (randDataSize + 2 >= len) {
                LOGGER.warn("head too short, maybe wrong password or encryption method");
                return null;
            }
            data.position(offset + randDataSize + 2);
            return data;
        } else if (dataType == 0x81) {
            LOGGER.debug("parse dataType = 0x81 ...");
            data.position(offset + 1);
            return data;
        } else if (dataType == 0x82) {
            LOGGER.debug("parse dataType = 0x82 ...");
            if (len <= 3)
                return null;
            // data[1] data[2]
            int randDataSize = toShort(data.get(), data.get());
            if (randDataSize + 3 >= len) {
                LOGGER.warn("header too short, maybe wrong password or encryption method");
                return null;
            }
            data.position(offset + randDataSize + 3);
            return data;
        } else if (dataType == 0x88 || (~dataType & 0xff) == 0x88) {
            LOGGER.debug("parse dataType = 0x88 ...");
            if (len < 7 + 7)
                return null;
            // data[1] data[2]
            int dataSize = toShort(data.get(), data.get());
            int ornPos = data.position();
            int ornLim = data.limit();
            data.position(offset);
            data.limit(dataSize);
            long crc = crc32(data);
            data.position(ornPos);
            data.limit(ornLim);
            if (crc != 0xffffffff) {
                LOGGER.warn("incorrect CRC32,maybe wrong password or encryption method");
                return null;
            }
            // data[3]
            int startPos = offset + 3 + char2Int((char) data.get());
            data.position(startPos);
            data.limit(data.limit() - 4);
            if (dataSize < len) {
                ByteBuffer buf = ByteBuffer.allocate(len - 4 - startPos + (len - dataSize));
                buf.put(data);
                data.position(offset + dataSize);
                data.limit(ornLim - dataSize);
                buf.put(data);
                return buf;
            }
            return data;
        }
        data.position(offset);
        LOGGER.debug("Nothing need to be parsed, just return origin data!");
        return data;
    }

    public static TargetAddress parseHeader(ByteBuffer data) throws UnknownHostException {
        int len = data.remaining();
        int atyp = char2Int((char) data.get());
        int connectType = (atyp & 0x8) != 0 ? 1 : 0;
        byte[] dstAddr = null;
        int dstPort = -1;
        int headLen = 0;
        atyp &= ~0x8;
        if (atyp == TargetAddress.ATYP_IPV4) {
            if (len >= 7) {
                dstAddr = new byte[4];
                data.get(dstAddr);
                dstPort = toShort(data.get(), data.get());
                headLen = 7;
            } else {
                LOGGER.warn("header is too short");
            }
        } else if (atyp == TargetAddress.ATYP_HOST) {
            if (len > 2) {
                int addrLen = char2Int((char) data.get());
                if (len >= 4 + addrLen) {
                    dstAddr = new byte[addrLen];
                    data.get(dstAddr);
                    dstPort = toShort(data.get(), data.get());
                    headLen = 4 + addrLen;
                } else
                    LOGGER.warn("header is too short");
            } else
                LOGGER.warn("header is too short");

        } else if (atyp == TargetAddress.ATYP_IPV6) {
            if (len >= 19) {
                dstAddr = new byte[16];
                data.get(dstAddr);
                dstPort = toShort(data.get(), data.get());
                headLen = 19;
            } else
                LOGGER.warn("header is too short");
        } else {
            LOGGER.warn("unsupported atyp {}, maybe wrong password or encryption method ", atyp);
        }

        if (dstAddr != null) {
            TargetAddress targetAddress = new TargetAddress();
            targetAddress.setAtyp(atyp);
            targetAddress.setDstAddr(dstAddr);
            targetAddress.setDstPort(dstPort);
            targetAddress.setConnectType(connectType);
            targetAddress.setHeaderLen(headLen);
            return targetAddress;
        }
        return null;
    }

    /**
     * 如果是是字符串表示的数字，就将其转为对应的int类型的数字
     * 如果不是数字，就返回对应的ascii码值
     * '1' -> 1
     * 'a' -> 97
     *  TODO 只有当值范围在0-127时，结果才正确，超过128，需要考虑占位符为1时，数字自动转换的问题
     *  byte b = (byte)128
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

    public static long crc32(ByteBuffer data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }

    public static void main(String[] args) throws IOException {
        byte b = 3;
        System.out.println(char2Int((char) b));
    }
}
