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

import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * @author yqy
 * @date 2022/8/15 08:40
 */
public class HmacMD5 {

    public static byte[] digest(byte[] key, byte[] data, int off, int len) {
        Mac hmac = new HMac(new MD5Digest());
        hmac.init(new KeyParameter(key));
        hmac.update(data, off, len);
        byte[] bytes = new byte[16];
        hmac.doFinal(bytes, 0);
        return bytes;
    }

    public static byte[] digest(byte[] key, byte[] data) {
        return digest(key, data, 0, data.length);
    }
}
