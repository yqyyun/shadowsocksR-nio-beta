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

package com.yqy.encrypto;

import com.goterl.lazysodium.SodiumJava;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author yqy
 * @date 2022/7/15 14:46
 */
public class LazyLibsodiumJava {

    private final SodiumJava sodiumJava;

    private static final int BLOCK_SIZE = 64;

    private byte[] buf;

    private int bufSize;
    
    private final byte[] key ;
    
    private final byte[] nonce;

    private int counter;

    public LazyLibsodiumJava(String cipherName, byte[] key, byte[] nonce) {
        this.sodiumJava = new SodiumJava();
        this.key = key;
        this.nonce = nonce;
        this.bufSize = 2048;
        this.buf = new byte[bufSize];
        counter = 0;
    }

    public byte[] update(byte[] data) {
        int l = data.length;
        int padding = counter % BLOCK_SIZE;
//        if (bufSize < padding + l) {
//            bufSize  = (padding + l) * 2;
//            buf = new byte[bufSize];
//        }
        bufSize = padding + l;
        buf = new byte[bufSize];
        if (padding > 0) {
            byte[] newData = new byte[bufSize];
            Arrays.fill(newData,0, padding, ((byte) '\0'));
            System.arraycopy(data, 0, newData, padding, l);
            data = newData;
        }
        sodiumJava.crypto_stream_salsa20_xor_ic(buf, data, data.length, nonce, counter / BLOCK_SIZE, key);
        counter += l;
        return Arrays.copyOfRange(buf, padding, padding + l);
    }

    public ByteBuffer update(ByteBuffer data) {
        byte[] bytes = new byte[data.remaining()];
        data.get(bytes);
        byte[] res = update(bytes);
        return ByteBuffer.wrap(res);
    }

    public boolean updateInPlace(ByteBuffer data) {
        return false;
    }
    
}
