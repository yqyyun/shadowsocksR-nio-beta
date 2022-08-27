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

import org.bouncycastle.crypto.digests.MD5Digest;

import javax.crypto.BadPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

/**
 * @author yqy
 * @date 2022/8/19 08:56
 */
public class RC4MD5 extends StreamCipher {

    private final RC4 rc4;

    public RC4MD5(Method method, byte[] key, byte[] iv) {
        super(method.getName());
        MD5Digest md5 = new MD5Digest();
        byte[] rc4Key = new byte[16];
        md5.update(key, 0, key.length);
        md5.update(iv, 0, iv.length);
        md5.doFinal(rc4Key, 0);
        rc4 = new RC4(method, rc4Key);
    }

    @Override
    protected void doInit(boolean encryption) throws InvalidAlgorithmParameterException, InvalidKeyException {
        rc4.doInit(encryption);
    }

    @Override
    public byte[] update(byte[] in) {
        return rc4.update(in);
    }

    @Override
    public int update(byte[] in, int inOff, int len, byte[] out, int outOff) {
        return rc4.update(in, inOff, len, out, outOff);
    }

    @Override
    public int updateInPlace(byte[] in) {
        return rc4.updateInPlace(in);
    }

    @Override
    public int updateInPlace(byte[] in, int inOff, int len) {
        return rc4.updateInPlace(in, inOff, len);
    }

    @Override
    public int doFinal(byte[] out, int outOff) throws BadPaddingException {
        return rc4.doFinal(out, outOff);
    }
}
