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

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * @author yqy
 * @date 2022/7/24 16:35
 */
public class Common {

    public static byte[] generateAESRandomBytes() {
        KeyGenerator aes = null;
        try {
            aes = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        SecretKey secretKey = aes.generateKey();
        byte[] encoded = secretKey.getEncoded();
        return encoded;
    }

    public static byte[] generateRandomBytes(int len) {
        return generateRandomBytes1(len);
    }

    // have to load native library
    private static byte[] generateRandomBytes2(int len) {
        SodiumJava sodiumJava = new SodiumJava();
        byte[] ret = new byte[len];
        sodiumJava.randombytes_buf(ret, len);
        return ret;
    }

    public static byte[] generateRandomBytes1(int len) {
        byte[] ret = new byte[len];
        new Random().nextBytes(ret);
        return ret;
    }

    public static void main(String[] args) {
        SodiumJava sodiumJava = new SodiumJava();
        long t1 = System.currentTimeMillis();
        int count = 10000000;
        for (int i = 0; i < count; i++) {
            generateRandomBytes1(16);
        }
        long t2 = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            generateRandomBytes1(16);
        }
        long t3 = System.currentTimeMillis();
        System.out.println("1 cost: "+(t2 - t1));
        System.out.println("2 cost: "+(t3 - t2));
    }
}
