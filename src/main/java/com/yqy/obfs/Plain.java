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

package com.yqy.obfs;

import com.yqy.bean.ObfsClientDecodedMessage;
import com.yqy.bean.ObfsServerDecodedMessage;

import java.nio.ByteBuffer;

/**
 * @author yqy
 * @date 2022/8/6 07:44
 */
public class Plain implements ObfsCodec {

    @Override
    public ByteBuffer clientEncode(ByteBuffer data) {
        return data;
    }

    @Override
    public ObfsClientDecodedMessage clientDecode(ByteBuffer data) {
        return new ObfsClientDecodedMessage(data, false);
    }

    @Override
    public ByteBuffer serverEncode(ByteBuffer data) {
        return data;
    }

    @Override
    public ObfsServerDecodedMessage serverDecode(ByteBuffer data) {
        return new ObfsServerDecodedMessage(data, true, false);
    }
}
