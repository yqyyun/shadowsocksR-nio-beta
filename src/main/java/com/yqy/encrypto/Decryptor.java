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

import javax.crypto.Cipher;
import java.nio.ByteBuffer;

/**
 * @author yqy
 * @date 2022/7/28 12:40
 */
public class Decryptor extends KeyGenerateable{

    private StreamCipher cipher;

    private final String method;

    private StreamCipher.Method methodInfo;

    private byte[] key;

    private byte[] iv;

    private boolean isCached = false;

    public Decryptor(String method, byte[] key, byte[] iv, boolean isCached) throws DecryptorException {
        this.method = method;
        try {
            methodInfo = StreamCipher.Method.getMethodInfo(method);
            int keyLen = methodInfo.getKeyLen();
            int ivLen = methodInfo.getIvLen();
            key = generateKey(key, keyLen, ivLen);
            if (ivLen != 0 && (iv == null || iv.length != ivLen)) {
                throw new IllegalArgumentException("iv length must be " + ivLen);
            }
            this.cipher = StreamCipher.getInstance(methodInfo, key, iv);
            this.key = key;
            this.iv = iv;
            cipher.init(Cipher.DECRYPT_MODE);
        } catch (Throwable e) {
            throw new DecryptorException(e);
        }
    }

    public byte[] decrypt(byte[] data) {
        if (data.length == 0) {
            return data;
        }
        return cipher.update(data);
    }

    public ByteBuffer decrypt(ByteBuffer data) {
        byte[] bs = new byte[data.remaining()];
        data.get(bs);
        byte[] rees = decrypt(bs);
        return ByteBuffer.wrap(rees);
    }


    public String getMethod() {
        return method;
    }

    public byte[] getKey() {
        return key;
    }

    public byte[] getIv() {
        return iv;
    }
}
