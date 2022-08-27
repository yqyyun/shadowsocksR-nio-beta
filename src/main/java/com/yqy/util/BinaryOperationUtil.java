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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * @author yqy
 * @date 2022/8/11 11:04
 */
public class BinaryOperationUtil {


    public static long reverseByteOrder(long i) {
        i = Long.rotateLeft(i, 8);
        i = Long.rotateLeft(i, 8);
        i = Long.rotateLeft(i, 8);
        i = Long.rotateLeft(i, 8);
        i = Long.rotateLeft(i, 8);
        i = Long.rotateLeft(i, 8);
        i = Long.rotateLeft(i, 8);
        return i;
    }

    public static long reverseBytes(long i) {
        return Long.reverseBytes(i);
    }

    public static int reverseBytes(int i) {
        return Integer.reverseBytes(i);
    }

    /**
     * 将对应的Int打包成字节数组
     *
     * @return
     */
    public static byte[] pack(int i, boolean bigEndian) {
        byte[] res = new byte[4];
        ByteBuffer.wrap(res)
                .order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
                .putInt(i);
        return res;
    }

    /**
     * 将对应的long打包成字节数组
     *
     * @param i
     * @param bigEndian
     * @return
     */
    public static byte[] pack(long i, boolean bigEndian) {
        byte[] res = new byte[8];
        ByteBuffer.wrap(res)
                .order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
                .putLong(i);
        return res;
    }

    public static byte[] pack(short s, boolean bigEndian) {
        byte[] res = new byte[2];
        ByteBuffer.wrap(res)
                .order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
                .putShort(s);
        return res;
    }

    public static int unpackInt(byte[] buf, boolean bigEndian) {
        if (buf.length != 4) {
            throw new IllegalArgumentException();
        }
        return (int) unpack(buf, 0, 4, bigEndian);
    }

    public static long unpackLong(byte[] buf, boolean bigEndian) {
        return unpack(buf, 0, 8, bigEndian);
    }

    public static short unpackShort(byte[] buf, boolean bigEndian) {
        if (buf.length != 2) {
            throw new IllegalArgumentException();
        }
        return (short) unpack(buf, 0, 2, bigEndian);
    }

    public static long unpack(byte[] buf, int offset, int length, boolean bigEndian) {
        if (length != 2 && length != 4 && length != 8) {
            throw new IllegalArgumentException("length should only be 2 4 8");
        }
        if (offset < 0 || offset + length > buf.length) {
            throw new IndexOutOfBoundsException();
        }
        ByteBuffer buffer = ByteBuffer.wrap(buf, offset, length)
                .order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        if (length == 2) {
            return buffer.getShort();
        }
        if (length == 4) {
            return buffer.getInt();
        }
        return buffer.getLong();
    }

    public static byte[] slice(byte[] data, int from, int to) {
        return Arrays.copyOfRange(data, from, to);
    }

}
