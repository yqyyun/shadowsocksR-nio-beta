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

import com.goterl.lazysodium.Sodium;
import com.goterl.lazysodium.SodiumJava;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.util.*;

/**
 * @author yqy
 * @date 2022/7/13 20:48
 */
public class Util {

    private static final Sodium sodium;

    static {
//        sodium = NaCl.sodium();
        sodium = new SodiumJava();
    }


    public static void runCipher(Openssl cipher, Openssl decipher) throws BadPaddingException, IllegalBlockSizeException {
        int blockSize = 16384;
        int rounds = 1 * 1024;
        byte[] plain = new byte[blockSize * rounds];
        sodium.randombytes_buf(plain, blockSize * rounds);

        System.out.println("test start");
        Random random = new Random();
        int pos = 0;
//        byte[] results = new byte[cipher.getOutputLen(plain.length)];
        ArrayList<Byte> results = new ArrayList<>(cipher.getOutputLen(plain.length));
        long start = System.currentTimeMillis();
        while (pos < plain.length) {
            // generate random number in [100, 32768] max - min + 1
            // encrypt random bytes of data every time
            int l = random.nextInt(32669) + 100;
            if (pos + l > plain.length) {
                l = plain.length - pos;
            }
            byte[] c = cipher.update(Arrays.copyOfRange(plain, pos, pos + l));
            for (int i = 0; i < c.length; i++) {
                results.add(c[i]);
            }
            pos += l;
        }
        pos = 0;
        ArrayList<Byte> results2 = new ArrayList<>(decipher.getOutputLen(results.size()));
        while (pos < results.size()) {
            int l = random.nextInt(32669) + 100;
            if (pos + l > results.size()) {
                l = results.size() - pos;
            }
            byte[] bs = new byte[l];
            for (int i = pos, j = 0; i < pos + l; i++, j++) {
                bs[j] = results.get(i);
            }
            byte[] c = decipher.update(bs);
            for (int i = 0; i < c.length; i++) {
                results2.add(c[i]);
            }
            pos += l;
        }
        long end = System.currentTimeMillis();
        System.out.printf("speed: %.0f bytes/s", ((double) (blockSize * rounds) / (end - start)) * 1000);
        for (int i = 0; i < plain.length; i++) {
            assert plain[i] == results2.get(i);
        }
    }

    public static void runCipher(StreamCipher cipher, StreamCipher decipher) throws BadPaddingException, IllegalBlockSizeException {
        int blockSize = 16384;
        int rounds = 1 * 1024;
        byte[] plain = new byte[blockSize * rounds];
        sodium.randombytes_buf(plain, blockSize * rounds);

        System.out.println("test start");
        Random random = new Random();
        int pos = 0;
        int i = 0;
        byte[] res1 = new byte[plain.length];
        long start = System.currentTimeMillis();
        while (pos < plain.length) {
            // generate random number in [100, 32768] max - min + 1
            // encrypt random bytes of data every time
            int l = random.nextInt(32669) + 100;
            if (pos + l > plain.length) {
                l = plain.length - pos;
            }
            byte[] c = cipher.update(Arrays.copyOfRange(plain, pos, pos + l));
            System.arraycopy(c, 0, res1, i, c.length);
            i += c.length;
            pos += l;
        }
        assert i == plain.length;
        i = 0;
        pos = 0;
        byte[] res2 = new byte[plain.length];
        while (pos < plain.length) {
            int l = random.nextInt(32669) + 100;
            if (pos + l > plain.length) {
                l = plain.length - pos;
            }
            byte[] bs = new byte[l];
            System.arraycopy(res1, pos, bs, 0, l);
            pos += l;
            byte[] c = decipher.update(bs);
            System.arraycopy(c, 0, res2, i, c.length);
            i += c.length;
        }
        long end = System.currentTimeMillis();
        System.out.printf("speed: %.0f bytes/s \n", ((double) (blockSize * rounds) / (end - start)) * 1000);
        System.out.println();
        assert Arrays.equals(plain, res2);
    }

    public static void runCipherInPlace(StreamCipher cipher, StreamCipher decipher) throws BadPaddingException, IllegalBlockSizeException {
        int blockSize = 16384;
        int rounds = 1 * 1024;
        byte[] plain = new byte[blockSize * rounds];
        sodium.randombytes_buf(plain, blockSize * rounds);

        System.out.println("test start");
        Random random = new Random();
        int pos = 0;
        byte[] res1 = new byte[plain.length];
        long start = System.currentTimeMillis();
        while (pos < plain.length) {
            // generate random number in [100, 32768] max - min + 1
            // encrypt random bytes of data every time
            int l = random.nextInt(32669) + 100;
            if (pos + l > plain.length) {
                l = plain.length - pos;
            }
            cipher.update(plain, pos, l, res1, pos);
            pos += l;
        }
        pos = 0;
        byte[] res2 = new byte[plain.length];
        while (pos < plain.length) {
            int l = random.nextInt(32669) + 100;
            if (pos + l > plain.length) {
                l = plain.length - pos;
            }
            decipher.update(res1, pos, l, res1, pos);
            pos += l;
        }
        long end = System.currentTimeMillis();
        System.out.printf("speed: %.0f bytes/s \n",   ((double) (blockSize * rounds) / (end - start)) * 1000);
        assert Arrays.equals(plain, res1);
    }

    public static void main(String[] args) throws Throwable {
        List<String> methoeds = new ArrayList<>();
//        methoeds.add("aes-128-cbc");
//        methoeds.add("aes-192-cbc");
//        methoeds.add("aes-256-cbc");
//        methoeds.add("chacha20xoric");
        methoeds.add("rc4");
        methoeds.add("rc4-md5");
        methoeds.add("rc4-md5-6");
        methoeds.add("aes-128-ctr");
        methoeds.add("aes-192-ctr");
        methoeds.add("aes-256-ctr");
        methoeds.add("aes-128-cfb");
        methoeds.add("aes-192-cfb");
        methoeds.add("aes-256-cfb");
//        methoeds.add("aes-128-cfb8");
//        methoeds.add("aes-192-cfb8");
//        methoeds.add("aes-256-cfb8");
//        methoeds.add("aes-128-cfb1");
//        methoeds.add("aes-192-cfb1");
//        methoeds.add("aes-256-cfb1");
        methoeds.add("aes-128-ofb");
        methoeds.add("aes-192-ofb");
        methoeds.add("aes-256-ofb");
        for (int i = 0; i < 100; i++) {
            testCipher("rc4");
        }
        for (String method : methoeds) {
            testCipher(method);
        }
//        testCipher("aes-128-ctr");

    }

    public static void testCipher(String name) throws Throwable {
        System.out.println("test " + name + " ==================== ");
        StreamCipher.Method method = StreamCipher.Method.getMethodInfo(name);
        byte[] key = new byte[method.getKeyLen()];
        byte[] iv = new byte[method.getIvLen()];
        Arrays.fill(key, ((byte) 'k'));
        Arrays.fill(iv, ((byte) 'i'));
        StreamCipher cipher = StreamCipher.getInstance(method, key, iv);
        cipher.init(Cipher.ENCRYPT_MODE);
        StreamCipher decipher = StreamCipher.getInstance(method, key, iv);
        decipher.init(Cipher.DECRYPT_MODE);
        //68200065
//        runCipher(cipher, decipher);
        //79512872
        runCipherInPlace(cipher,decipher);
    }
}
