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

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author yqy
 * @date 2022/8/13 10:46
 */
public class AES extends StreamCipher {

    private final Cipher cipher;

    private final String cipherAlgorithm;

    private final byte[] key;

    private final byte[] iv;

    private static final String PADDING = "PKCS7Padding";
//    private static final String PADDING = "PKCS5Padding";
//    private static final String PADDING = "NoPadding";

    public AES(Method method, byte[] key, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException {
        super(method.getName());
        String[] split = method.getName().split("-");
        cipherAlgorithm = split[0] + "/" + split[2] + "/" + PADDING;
        cipher = Cipher.getInstance(cipherAlgorithm, new BouncyCastleProvider());
        this.key = key;
        this.iv = iv;
    }

    @Override
    protected void doInit(boolean encryption) throws InvalidAlgorithmParameterException, InvalidKeyException {
        int mode= encryption ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
        cipher.init(mode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
    }

    @Override
    public byte[] update(byte[] data) {
        int len = data.length;
        byte[] buf = new byte[len];
        try {
            cipher.update(data,0, len, buf, 0);
        } catch (ShortBufferException e) {
            e.printStackTrace();
        }
        return buf;
    }

    @Override
    public int update(byte[] in, int inOff, int len, byte[] out, int outOff) {
        try {
            return cipher.update(in, inOff, len, out, outOff);
        } catch (ShortBufferException e) {
        }
        return 0;
    }

    @Override
    public int updateInPlace(byte[] in) {
        try {
            return cipher.update(in, 0, in.length, in, 0);
        } catch (ShortBufferException e) {

        }
        return 0;
    }

    @Override
    public int updateInPlace(byte[] in, int inOff, int len) {
        try {
            return cipher.update(in, inOff, len, in, inOff);
        } catch (ShortBufferException e) {

        }
        return 0;
    }
}
