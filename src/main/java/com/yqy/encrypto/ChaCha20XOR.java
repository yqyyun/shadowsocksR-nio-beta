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

/**
 * @author yqy
 * @date 2022/7/24 10:12
 */
public class ChaCha20XOR extends LazyLibsodium{
    public ChaCha20XOR(Method cipherMethod, byte[] key, byte[] nonce) {
        super(cipherMethod, key, nonce);
    }

    @Override
    protected void doFinal(byte[] buf, byte[] data, byte[] key, byte[] nonce, long ic) {
        sodiumJava.crypto_stream_chacha20_xor(buf, data, data.length, nonce, key);
    }

    @Override
    public int update(byte[] in, int inOff, int len, byte[] out, int outOff) {
        return 0;
    }
}
