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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

/**
 * @author yqy
 * @date 2022/7/24 10:06
 */
public class None extends StreamCipher {
    protected None(Method method) {
        super(method.getName());
    }

    @Override
    protected void doInit(boolean encryption) throws InvalidAlgorithmParameterException, InvalidKeyException {

    }

    @Override
    public byte[] update(byte[] data) {
        return data;
    }

    @Override
    public int update(byte[] in, int inOff, int len, byte[] out, int outOff) {
        System.arraycopy(in, inOff, out, outOff, len);
        return len;
    }
}
