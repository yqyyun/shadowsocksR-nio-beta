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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;

/**
 * @author yqy
 * @date 2022/7/24 09:18
 */
public abstract class StreamCipher {

    private final String algorithmName;

    public enum Method {
        NONE("none", 16, 0),
        CHACHA20("chacha20", 32, 8),
        CHACHA20_XOR("chacha20xor", 32, 8),
        CHACHA20_XOR_IC("chacha20xoric", 32, 8),
        CHACHA20_IETF_XOR_IC("chacha20ietfxoric", 32, 12),
        XCHACHA20_XOR_IC("xchacha20xoric", 32, 24),
        SALSA20_XOR_IC("salsa20xoric", 32, 8),
        AES128CBC("aes-128-cbc", 16, 16),
        AES192CBC("aes-192-cbc", 24, 16),
        AES256CBC("aes-256-cbc", 32, 16),
        AES128CTR("aes-128-ctr", 16, 16),
        AES192CTR("aes-192-ctr", 24, 16),
        AES256CTR("aes-256-ctr", 32, 16),
        AES128CFB("aes-128-cfb", 16, 16),
        AES192CFB("aes-192-cfb", 24, 16),
        AES256CFB("aes-256-cfb", 32, 16),
        AES128CFB8("aes-128-cfb8", 16, 16),
        AES192CFB8("aes-192-cfb8", 24, 16),
        AES256CFB8("aes-256-cfb8", 32, 16),
        //        AES128CFB1("aes-128-cfb1", 16, 16),
//        AES192CFB1("aes-192-cfb1", 24, 16),
//        AES256CFB1("aes-256-cfb1", 32, 16),
        AES128OFB("aes-128-ofb", 16, 16),
        AES192OFB("aes-192-ofb", 24, 16),
        AES256OFB("aes-256-ofb", 32, 16),
        RC4("rc4", 16, 0),
        RC4MD5("rc4-md5", 16, 16),
        RC4MD5_6("rc4-md5-6", 16, 6),
        ;

        private final String name;
        // TODO 考虑key和iv
        private final int keyLen;
        private final int ivLen;

//        private final String algorithm;

        Method(String name, int keyLen, int ivLen) {
            this.name = name;
            this.keyLen = keyLen;
            this.ivLen = ivLen;
        }

        public static Method getMethodInfo(String name) throws UnknownCipherMethodException {
            switch (name) {
                case "none":
                    return NONE;
                case "chacha20":
                    return CHACHA20;
                case "chacha20xor":
                    return CHACHA20_XOR;
                case "chacha20xoric":
                    return CHACHA20_XOR_IC;
                case "chacha20ietfxoric":
                    return CHACHA20_IETF_XOR_IC;
                case "xchacha20xoric":
                    return XCHACHA20_XOR_IC;
                case "salsa20xoric":
                    return SALSA20_XOR_IC;
                case "aes-128-cbc":
                    return AES128CBC;
                case "aes-192-cbc":
                    return AES192CBC;
                case "aes-256-cbc":
                    return AES256CBC;
                case "aes-128-ctr":
                    return AES128CTR;
                case "aes-192-ctr":
                    return AES192CTR;
                case "aes-256-ctr":
                    return AES256CTR;
                case "aes-128-cfb":
                    return AES128CFB;
                case "aes-192-cfb":
                    return AES192CFB;
                case "aes-256-cfb":
                    return AES256CFB;
                case "aes-128-cfb8":
                    return AES128CFB8;
                case "aes-192-cfb8":
                    return AES192CFB8;
                case "aes-256-cfb8":
                    return AES256CFB8;
//                case "aes-128-cfb1":
//                    return AES128CFB1;
//                case "aes-192-cfb1":
//                    return AES192CFB1;
//                case "aes-256-cfb1":
//                    return AES256CFB1;
                case "aes-128-ofb":
                    return AES128OFB;
                case "aes-192-ofb":
                    return AES192OFB;
                case "aes-256-ofb":
                    return AES256OFB;
                case "rc4":
                    return RC4;
                case "rc4-md5":
                    return RC4MD5;
                case "rc4-md5-6":
                    return RC4MD5_6;
                default:
                    throw new UnknownCipherMethodException();
            }
        }

        public String getName() {
            return name;
        }

        public int getKeyLen() {
            return keyLen;
        }

        public int getIvLen() {
            return ivLen;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    protected StreamCipher(String name) {
        this.algorithmName = name;
    }

    public static StreamCipher getInstance(Method method, byte[] key, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSupportedCipherMethodException {
        try {
            switch (method) {
                case NONE:
                    return new None(method);
                case CHACHA20:
                    return new ChaCha20(method, key, iv);
                case CHACHA20_XOR:
                    return new ChaCha20XOR(method, key, iv);
                case CHACHA20_XOR_IC:
                    return new ChaCha20XORIC(method, key, iv);
                case CHACHA20_IETF_XOR_IC:
                    return new ChaCha20IETFXORIC(method, key, iv);
                case XCHACHA20_XOR_IC:
                    return new XChaCha20XORIC(method, key, iv);
                case SALSA20_XOR_IC:
                    return new SalSa20XORIC(method, key, iv);
                case AES128CBC:
                    return new AESImp(method, key, iv);
                case AES192CBC:
                    return new AESImp(method, key, iv);
                case AES256CBC:
                    return new AESImp(method, key, iv);
                case AES128CTR:
                    return new AESImp(method, key, iv);
                case AES192CTR:
                    return new AESImp(method, key, iv);
                case AES256CTR:
                    return new AESImp(method, key, iv);
                case AES128CFB:
                    return new AESImp(method, key, iv);
//                    return new Openssl(method, key, iv);
                case AES192CFB:
                    return new AESImp(method, key, iv);
                case AES256CFB:
                    return new AESImp(method, key, iv);
                case AES128CFB8:
                    return new AESImp(method, key, iv);
                case AES192CFB8:
                    return new AESImp(method, key, iv);
                case AES256CFB8:
                    return new AESImp(method, key, iv);
//                case AES128CFB1:
//                    return new AES(method, key, iv);
//                case AES192CFB1:
//                    return new AES(method, key, iv);
//                case AES256CFB1:
//                    return new AES(method, key, iv);
                case AES128OFB:
                    return new AESImp(method, key, iv);
                case AES192OFB:
                    return new AESImp(method, key, iv);
                case AES256OFB:
                    return new AESImp(method, key, iv);
                case RC4:
                    return new RC4(method, key);
                case RC4MD5:
                    return new RC4MD5(method, key, iv);
                case RC4MD5_6:
                    return new RC4MD5(method, key, iv);
                default:
                    throw new NoSupportedCipherMethodException(method);
            }
        } catch (Throwable e) {
            throw e;
        }
    }

    public final void init(int mode) throws InvalidAlgorithmParameterException, InvalidKeyException {
        switch (mode) {
            case Cipher.ENCRYPT_MODE:
            case Cipher.WRAP_MODE:
                doInit(true);
                break;
            case Cipher.DECRYPT_MODE:
            case Cipher.UNWRAP_MODE:
                doInit(false);
                break;
            default:
                throw new InvalidParameterException("unknown mode " + mode + " passed");
        }
    }

    protected abstract void doInit(boolean encryption) throws InvalidAlgorithmParameterException, InvalidKeyException;

    public abstract byte[] update(byte[] in);

    public abstract int update(byte[] in, int inOff, int len, byte[] out, int outOff);

    public int updateInPlace(byte[] in) {
        throw  new UnsupportedOperationException(algorithmName + " do not support update in place!");
    }

    public int updateInPlace(byte[] in, int inOff, int len) {
        throw  new UnsupportedOperationException(algorithmName + " do not support update in place!");
    }

    public int doFinal(byte[] out, int outOff) throws BadPaddingException {
        return 0;
    }

    public final String getAlgorithmName() {
        return algorithmName;
    }

}
