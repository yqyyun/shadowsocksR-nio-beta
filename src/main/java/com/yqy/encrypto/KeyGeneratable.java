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

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD5Digest;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yqy
 * @date 2022/8/14 07:19
 */
abstract class KeyGenerateable {
    private static final Map<String, byte[]> keyCache = new HashMap<>();

    // test pass
    protected final byte[] generateKey(byte[] password, int keyLen, int ivLen) {
        String cacheKey = new String(password, StandardCharsets.UTF_8) + "-" + keyLen + "-" + ivLen;
        byte[] mapKey = keyCache.get(cacheKey);
        if (mapKey != null) {
            return mapKey;
        }
        int len = keyLen + ivLen;
        byte[] data = password;
        int dl = data.length;
        int rdl = dl + 16;
        // |-----|-----|data--|
        byte[] ret = new byte[(len / 16 + 1) * 16];
        Digest md5 = new MD5Digest();
        md5.update(data, 0, dl);
        md5.doFinal(ret, 0);
        // optimize erase if i==0 clause
        if (data.length < 16) {
            int i = 16;
            while (i < len) {
                md5 = new MD5Digest();
                // i>> 4 == i * 16
                System.arraycopy(data, 0, ret, i, dl);
                md5.update(ret, i - 16, rdl);
                md5.doFinal(ret, i);
                i += 16;
            }
        }
        mapKey = Arrays.copyOfRange(ret, 0, keyLen);
        keyCache.put(cacheKey, mapKey);
        return mapKey;
    }
}
