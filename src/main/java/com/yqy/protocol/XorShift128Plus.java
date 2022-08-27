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

package com.yqy.protocol;

import java.math.BigInteger;

import static com.yqy.util.BinaryOperationUtil.pack;
import static com.yqy.util.BinaryOperationUtil.slice;
import static com.yqy.util.BinaryOperationUtil.unpackLong;

/**
 * @author yqy
 * @date 2022/8/11 09:39
 */
public class XorShift128Plus {

    // (1<<64) - 1
    private static final BigInteger MAX_LONG = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);

    // (1 << (64 - 23)) - 1 41个1 0x1ffffffffffL
//    private static final BigInteger MOV_MASK = BigInteger.valueOf(0x1ffffffffffL);

    private static final long MOV_MASK = 0x1ffffffffffL;


    private long v0;

    private long v1;

    public XorShift128Plus() {
        this.v0 = 0;
        this.v1 = 0;
    }

    /**
     * 根据函数获得一个64-bit的数
     * 该函数类似于固定种子的随机数生成器
     *
     * @return
     */
    public BigInteger next() {
        long x = v0;
        long y = v1;
        v0 = y;
        x ^= ((x & MOV_MASK) << 23);
        x ^= (y ^ (x >>> 17) ^ (y >>> 26));
        v1 = x;
        BigInteger xb = new BigInteger(Long.toBinaryString(x), 2);
        BigInteger yb = new BigInteger(Long.toBinaryString(y), 2);
        return xb.add(yb).and(MAX_LONG);
    }

    /**
     * ensure bin is positive,ie. the highest bit should be zero.
     *
     * @param bin
     */
    public void initFromBin(byte[] bin) {
        if (bin.length < 16) {
            byte[] bytes = new byte[16 + bin.length];
            System.arraycopy(bin, 0, bytes, 0, bin.length);
            bin = bytes;
        }
        long x = unpackLong(slice(bin, 0, 8), false);
        long y = unpackLong(slice(bin, 8, 16), false);
        v0 = x;
        v1 = y;
    }

    public void initFromBinLen(byte[] bin, short length) {
        if (bin.length < 16) {
            byte[] bytes = new byte[16 + bin.length];
            System.arraycopy(bin, 0, bytes, 0, bin.length);
            bin = bytes;
        }
        byte[] bytes = new byte[8];
        byte[] s = pack(length, false);
        bytes[0] = s[0];
        bytes[1] = s[1];
        bytes[2] = bin[2];
        bytes[3] = bin[3];
        bytes[4] = bin[4];
        bytes[5] = bin[5];
        bytes[6] = bin[6];
        bytes[7] = bin[7];
        long x = unpackLong(bytes, false);
        long y = unpackLong(slice(bin, 8, 16), false);
        v0 = x;
        v1 = y;
        next();
        next();
        next();
        next();
    }

}
