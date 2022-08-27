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

/**
 * @author yqy
 * @date 2022/8/15 17:27
 */
public class Md5 {

    public static byte[] digest(byte[] data) {
        return digest(data, 0, data.length);
    }

    public static byte[] digest(byte[] data, int off, int len) {
        Digest md5 = new MD5Digest();
        md5.update(data, off, len);
        byte[] outBuf = new byte[16];
        md5.doFinal(outBuf, 0);
        return outBuf;
    }
}
