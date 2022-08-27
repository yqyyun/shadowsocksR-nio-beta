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

import org.bouncycastle.crypto.*;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.*;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.bouncycastle.jcajce.provider.symmetric.util.ClassUtil;

import javax.crypto.*;
import java.lang.reflect.Constructor;
import java.security.NoSuchAlgorithmException;

/**
 * @author yqy
 * @date 2022/8/13 10:46
 */
public class AESImp extends StreamCipher {

    private final String cipherAlgorithm;

    private AESEngine baseEngine = new AESEngine();

    private GenericStreamCipher engine;

    private final String mode;

    private final byte[] key;

    private final byte[] iv;

    private final int blockSize;
    //    private static final String PADDING = "PKCS7Padding";
    //    private static final String PADDING = "PKCS5Padding";
    private static final String NOPADDING = "NoPadding";

    public AESImp(Method method, byte[] key, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this(method, key, iv, NOPADDING);
    }

    public AESImp(Method method, byte[] key, byte[] iv, String padding) throws NoSuchAlgorithmException, NoSuchPaddingException {
        super(method.getName());
        String[] split = method.getName().split("-");
        mode = split[2];
        cipherAlgorithm = split[0] + "/" + mode + "/" + padding;
        this.key = key;
        this.iv = iv;
        modeSet(mode);
        paddingSet(padding);
        this.blockSize = engine.getUnderlyingCipher().getBlockSize();
    }

    @Override
    protected void doInit(boolean encryption) {
        KeyParameter keyP = new KeyParameter(key);
        ParametersWithIV parameters = new ParametersWithIV(keyP, iv);
        engine.init(encryption, parameters);
    }

    private void paddingSet(String padding) throws NoSuchPaddingException {
        if (engine == null) {
            throw new NoSuchPaddingException("no padding supported for this algorithm");
        }
        padding = padding.toUpperCase();
        if ("NOPADDING".equals(padding)) {
            if (!(engine instanceof WrapperStreamCipher)) {
                engine = new BufferedGenericBlockCipher(new BufferedBlockCipher(engine.getUnderlyingCipher()));
            }
        }
        else if ("PKCS5PADDING".equals(padding) || "PKCS7PADDING".equals(padding)) {
            engine = new BufferedGenericBlockCipher(new PaddedBufferedBlockCipher(engine.getUnderlyingCipher(), new PKCS7Padding()));
        }
        else {
           throw new NoSuchPaddingException("Padding " + padding + " unknown.");
        }
    }

    private void modeSet(String mode) throws NoSuchAlgorithmException {
        if (baseEngine == null) {
            throw new NoSuchAlgorithmException("no mode supported for this algorithm");
        }
        switch (mode.toUpperCase()) {
            case "ECB":
                this.engine = new BufferedGenericBlockCipher(baseEngine);
                break;
            case "CBC":
                this.engine = new BufferedGenericBlockCipher(new CBCBlockCipher(baseEngine));
                break;
            case "CFB":
                this.engine = new WrapperStreamCipher(new CFBBlockCipher(baseEngine, 128));
                break;
            case "OFB":
                this.engine = new WrapperStreamCipher(new OFBBlockCipher(baseEngine, 128));
                break;
            case "SIC":
            case "CTR":
                this.engine = new WrapperStreamCipher(new SICBlockCipher(baseEngine));
                break;
            case "GCM":
                this.engine = new AEADGenericBlockCipher(new GCMBlockCipher(baseEngine));
                break;
            default:
                throw new NoSuchAlgorithmException("can't support mode " + mode);
        }
    }

    @Override
    public byte[] update(byte[] data) {
        int len = data.length;
        byte[] res = new byte[(len / blockSize + 1) * blockSize];
        int rl = update(data, 0, len, res, 0);
        if (rl != res.length) {
            byte[] newRes = new byte[rl];
            System.arraycopy(res, 0, newRes, 0, rl);
            return newRes;
        }
        return res;
    }

    @Override
    public int update(byte[] in, int inOff, int len, byte[] out, int outOff) {
        int off = engine.processBytes(in, inOff, len, out, outOff);
        try {
            off += engine.doFinal(out, outOff + off);
        } catch (BadPaddingException e) {
        }
        return off;
    }

    @Override
    public int doFinal(byte[] out, int outOff) throws BadPaddingException {
        return engine.doFinal(out, outOff);
    }

    @Override
    public int updateInPlace(byte[] in) {
        return update(in, 0, in.length, in, 0);
    }

    @Override
    public int updateInPlace(byte[] in, int inOff, int len) {
        return update(in, inOff, len, in, inOff);
    }

    static private interface GenericStreamCipher {
        public void init(boolean forEncryption, CipherParameters params)
                throws IllegalArgumentException;

        public boolean wrapOnNoPadding();

        public String getAlgorithmName();

        public org.bouncycastle.crypto.BlockCipher getUnderlyingCipher();

        public int getOutputSize(int len);

        public int getUpdateOutputSize(int len);

        public void updateAAD(byte[] input, int offset, int length);

        public int processByte(byte in, byte[] out, int outOff)
                throws DataLengthException;

        public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff)
                throws DataLengthException;

        public int doFinal(byte[] out, int outOff)
                throws IllegalStateException,
                BadPaddingException;
    }

    private static class WrapperStreamCipher
            implements GenericStreamCipher {
        private org.bouncycastle.crypto.StreamBlockCipher cipher;

        WrapperStreamCipher(org.bouncycastle.crypto.StreamBlockCipher cipher) {
            this.cipher = cipher;
        }

        public void init(boolean forEncryption, CipherParameters params)
                throws IllegalArgumentException {
            cipher.init(forEncryption, params);
        }

        public boolean wrapOnNoPadding() {
            return true;
        }

        public String getAlgorithmName() {
            return cipher.getAlgorithmName();
        }

        public org.bouncycastle.crypto.BlockCipher getUnderlyingCipher() {
            return cipher.getUnderlyingCipher();
        }

        public int getOutputSize(int len) {
            return len;
        }

        public int getUpdateOutputSize(int len) {
            return len;
        }

        public void updateAAD(byte[] input, int offset, int length) {
            throw new UnsupportedOperationException("AAD is not supported in the current mode.");
        }

        public int processByte(byte in, byte[] out, int outOff)
                throws DataLengthException {
            out[outOff] = cipher.returnByte(in);
            return 1;
        }

        public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff)
                throws DataLengthException {
            return cipher.processBytes(in, inOff, len, out, outOff);
        }

        public int doFinal(byte[] out, int outOff)
                throws IllegalStateException, BadPaddingException {
                return 0;
        }
    }

    private static class BufferedGenericBlockCipher
            implements GenericStreamCipher {
        private BufferedBlockCipher cipher;

        BufferedGenericBlockCipher(BufferedBlockCipher cipher) {
            this.cipher = cipher;
        }

        BufferedGenericBlockCipher(org.bouncycastle.crypto.BlockCipher cipher) {
            this.cipher = new PaddedBufferedBlockCipher(cipher);
        }

        BufferedGenericBlockCipher(org.bouncycastle.crypto.BlockCipher cipher, BlockCipherPadding padding) {
            this.cipher = new PaddedBufferedBlockCipher(cipher, padding);
        }

        public void init(boolean forEncryption, CipherParameters params)
                throws IllegalArgumentException {
            cipher.init(forEncryption, params);
        }

        public boolean wrapOnNoPadding() {
            return !(cipher instanceof CTSBlockCipher);
        }

        public String getAlgorithmName() {
            return cipher.getUnderlyingCipher().getAlgorithmName();
        }

        public org.bouncycastle.crypto.BlockCipher getUnderlyingCipher() {
            return cipher.getUnderlyingCipher();
        }

        public int getOutputSize(int len) {
            return cipher.getOutputSize(len);
        }

        public int getUpdateOutputSize(int len) {
            return cipher.getUpdateOutputSize(len);
        }

        public void updateAAD(byte[] input, int offset, int length) {
            throw new UnsupportedOperationException("AAD is not supported in the current mode.");
        }

        public int processByte(byte in, byte[] out, int outOff)
                throws DataLengthException {
            return cipher.processByte(in, out, outOff);
        }

        public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff)
                throws DataLengthException {
            return cipher.processBytes(in, inOff, len, out, outOff);
        }

        public int doFinal(byte[] out, int outOff)
                throws IllegalStateException, BadPaddingException {
            try {
                return cipher.doFinal(out, outOff);
            } catch (InvalidCipherTextException e) {
                throw new BadPaddingException(e.getMessage());
            }
        }
    }

    private static class AEADGenericBlockCipher
            implements GenericStreamCipher {
        private static final Constructor aeadBadTagConstructor;

        static {
            Class aeadBadTagClass = ClassUtil.loadClass(BaseBlockCipher.class, "javax.crypto.AEADBadTagException");
            if (aeadBadTagClass != null) {
                aeadBadTagConstructor = findExceptionConstructor(aeadBadTagClass);
            } else {
                aeadBadTagConstructor = null;
            }
        }

        private static Constructor findExceptionConstructor(Class clazz) {
            try {
                return clazz.getConstructor(new Class[]{String.class});
            } catch (Exception e) {
                return null;
            }
        }

        private AEADCipher cipher;

        AEADGenericBlockCipher(AEADCipher cipher) {
            this.cipher = cipher;
        }

        public void init(boolean forEncryption, CipherParameters params)
                throws IllegalArgumentException {
            cipher.init(forEncryption, params);
        }

        public String getAlgorithmName() {
            if (cipher instanceof AEADBlockCipher) {
                return ((AEADBlockCipher) cipher).getUnderlyingCipher().getAlgorithmName();
            }

            return cipher.getAlgorithmName();
        }

        public boolean wrapOnNoPadding() {
            return false;
        }

        public org.bouncycastle.crypto.BlockCipher getUnderlyingCipher() {
            if (cipher instanceof AEADBlockCipher) {
                return ((AEADBlockCipher) cipher).getUnderlyingCipher();
            }

            return null;
        }

        public int getOutputSize(int len) {
            return cipher.getOutputSize(len);
        }

        public int getUpdateOutputSize(int len) {
            return cipher.getUpdateOutputSize(len);
        }

        public void updateAAD(byte[] input, int offset, int length) {
            cipher.processAADBytes(input, offset, length);
        }

        public int processByte(byte in, byte[] out, int outOff)
                throws DataLengthException {
            return cipher.processByte(in, out, outOff);
        }

        public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff)
                throws DataLengthException {
            return cipher.processBytes(in, inOff, len, out, outOff);
        }

        public int doFinal(byte[] out, int outOff)
                throws IllegalStateException, BadPaddingException {
            try {
                return cipher.doFinal(out, outOff);
            } catch (InvalidCipherTextException e) {
                if (aeadBadTagConstructor != null) {
                    BadPaddingException aeadBadTag = null;
                    try {
                        aeadBadTag = (BadPaddingException) aeadBadTagConstructor
                                .newInstance(new Object[]{e.getMessage()});
                    } catch (Exception i) {
                        // Shouldn't happen, but fall through to BadPaddingException
                    }
                    if (aeadBadTag != null) {
                        throw aeadBadTag;
                    }
                }
                throw new BadPaddingException(e.getMessage());
            }
        }
    }

}
