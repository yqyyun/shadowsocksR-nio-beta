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
import java.nio.ByteBuffer;
import java.security.*;
import java.util.Arrays;

/**
 * @author yqy
 * @date 2022/7/18 10:13
 */
public class Openssl extends StreamCipher {

    private final byte[] key;
    private final byte[] iv;
    private Cipher cipher;

    private String cipherName;

    public Openssl(Method method, byte[] key, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException {
        super(method.getName());
//        Security.addProvider(new BouncyCastleProvider());
        String[] split = method.getName().split("-");
        String mode = split[2];
        String cipherAlgorithm = split[0] + "/" + mode + "/" + "NoPadding";
        this.cipherName = cipherAlgorithm;
        this.key = key;
        this.iv = iv;
        cipher = Cipher.getInstance(cipherName, new BouncyCastleProvider());

    }

    @Override
    protected void doInit(boolean encryption) throws InvalidAlgorithmParameterException, InvalidKeyException {
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
            if (encryption) {
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            }
    }

    public byte[] update(byte[] input) {
        byte[] plainBytes = input;
        int plainLen = plainBytes.length;
        int blockSize = cipher.getBlockSize();
        int paddingLen = plainLen % blockSize;
        byte[] dataBytes = plainBytes;
//        if (paddingLen > 0) {
//            plainLen += blockSize - paddingLen;
//            dataBytes = new byte[plainLen];
//            System.arraycopy(plainBytes, 0, dataBytes, 0, plainBytes.length);
//        }
        return cipher.update(dataBytes);
    }

    @Override
    public int update(byte[] in, int inOff, int len, byte[] out, int outOff) {
        try {
            return cipher.update(in, inOff,len, out, outOff);
        } catch (ShortBufferException e) {

        }
        return 0;
    }

    public ByteBuffer update(ByteBuffer input) throws BadPaddingException, ShortBufferException, IllegalBlockSizeException {
        int outputlen = cipher.getOutputSize(input.remaining());
        ByteBuffer output = ByteBuffer.allocate(outputlen);
        cipher.update(input, output);
        return output;
    }

    public int getOutputLen(int inputLen) {
        int blockSize = cipher.getBlockSize();
        int padding = inputLen % blockSize;
        if (padding > 0) {
            inputLen += blockSize - padding;
        }
        return cipher.getOutputSize(inputLen);
    }

    public int updateInPlace(byte[] data) {

        return 0;
    }

    public void updateInPlace(ByteBuffer data) {

    }

    public static void main(String[] args) throws Exception, UnknownCipherMethodException, NoSupportedCipherMethodException {
        Method method = Method.getMethodInfo("aes-128-cfb");
        byte[] key = new byte[16];
        byte[] iv = new byte[16];
        Arrays.fill(key, ((byte) 'k'));
        Arrays.fill(iv, ((byte) 'i'));
        StreamCipher cipher = StreamCipher.getInstance(method, key, iv);
        cipher.init(Cipher.ENCRYPT_MODE);
        StreamCipher decipher = StreamCipher.getInstance(method, key, iv);
        decipher.init(Cipher.DECRYPT_MODE);
        Util.runCipher(cipher, decipher);
    }
}
