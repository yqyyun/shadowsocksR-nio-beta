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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.Arrays;

/**
 * @author yqy
 * @date 2022/7/24 09:15
 */
public abstract class LazyLibsodium extends StreamCipher {

    protected final SodiumJava sodiumJava;

    private static final int BLOCK_SIZE = 64;

    private byte[] buf;

    private int bufSize;

    private final byte[] key ;

    private final byte[] nonce;

    private int counter;

    public LazyLibsodium(Method method, byte[] key, byte[] nonce) {
        super(method.getName());
        this.sodiumJava = new SodiumJava();
        this.key = key;
        this.nonce = nonce;
        this.bufSize = 2048;
        this.buf = new byte[bufSize];
        counter = 0;
    }

    @Override
    protected void doInit(boolean encryption) throws InvalidAlgorithmParameterException, InvalidKeyException {

    }

    @Override
    public final byte[] update(byte[] data) {
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
//        sodiumJava.crypto_stream_salsa20_xor_ic(buf, data, data.length, nonce, counter / BLOCK_SIZE, key);
        doFinal(buf,data,key,nonce,counter/BLOCK_SIZE);
        counter += l;
        return Arrays.copyOfRange(buf, padding, padding + l);
    }

    protected abstract void doFinal(byte[] buf, byte[] data, byte[] key, byte[] nonce, long ic);

}
