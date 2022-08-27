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

import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * Test Pass
 *
 * @author yqy
 * @date 2022/8/14 07:42
 */
public class RC4 extends StreamCipher {

    private final org.bouncycastle.crypto.StreamCipher cipher;

    private final byte[] key;

    public RC4(Method method, byte[] key) {
        super(method.getName());
        cipher = new RC4Engine();
        this.key = key;
    }

    @Override
    protected void doInit(boolean encryption) {
        KeyParameter param = new KeyParameter(key);
        cipher.init(encryption, param);
    }

    @Override
    public byte[] update(byte[] data) {
        int len = data.length;
        byte[] res = new byte[len];
        cipher.processBytes(data, 0, len, res, 0);
        return res;
    }

    @Override
    public int update(byte[] in, int inOff, int len, byte[] out, int outOff) {
        return cipher.processBytes(in, inOff, len, out, outOff);
    }

    @Override
    public int updateInPlace(byte[] in) {
        return cipher.processBytes(in, 0, in.length, in, 0);
    }

    @Override
    public int updateInPlace(byte[] in, int inOff, int len) {
        return cipher.processBytes(in, inOff, len, in, inOff);
    }
}
