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
 * @date 2022/7/24 10:35
 */
public class Encryptor extends KeyGenerateable {

    private StreamCipher cipher;

    private final String method;

    private StreamCipher decipher;

    private byte[] key;

    private byte[] iv;

    private boolean ivSent = false;

    private boolean isCached = false;

    public Encryptor(String method, byte[] key, byte[] iv, boolean isCached) throws EncryptorException {
        this.method = method;
        try {
            StreamCipher.Method methodInfo = StreamCipher.Method.getMethodInfo(method);
            int keLen = methodInfo.getKeyLen();
            int ivLen = methodInfo.getIvLen();
            key = generateKey(key, keLen, ivLen);
            if (ivLen != 0 && (iv == null || iv.length != ivLen)) {
                iv = Common.generateRandomBytes(ivLen);
            }
            this.key = key;
            this.iv = iv;
            this.cipher = StreamCipher.getInstance(methodInfo, key, iv);
            cipher.init(Cipher.ENCRYPT_MODE);
        } catch (Throwable e) {
            throw new EncryptorException(e);
        }
    }



    public byte[] encrypt(byte[] data) {
        //TODO 可优化
        if (!ivSent && iv != null) {
            data = cipher.update(data);
            byte[] res = new byte[iv.length + data.length];
            System.arraycopy(iv, 0, res, 0, iv.length);
            System.arraycopy(data,0, res, iv.length, data.length);
            ivSent = true;
            return res;
        }
        return cipher.update(data);
    }

    public ByteBuffer encrypt(ByteBuffer data) {
        byte[] bs = new byte[data.remaining()];
        data.get(bs);
        byte[] rees = encrypt(bs);
        return ByteBuffer.wrap(rees);
    }

    public byte[] getKey() {
        return key;
    }

    public byte[] getIv() {
        return iv;
    }

    public String getMethod() {
        return method;
    }
}
