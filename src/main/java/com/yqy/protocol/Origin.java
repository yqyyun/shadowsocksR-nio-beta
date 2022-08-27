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

package com.yqy.protocol;

import com.yqy.bean.ProtocolDecryptedMessage;
import com.yqy.bean.ServerInfo;

import java.nio.ByteBuffer;

/**
 * @author yqy
 * @date 2022/8/6 07:42
 */
public class Origin implements Protocol {

    protected ServerInfo serverInfo;

    @Override
    public ByteBuffer initData() {
        return ByteBuffer.allocate(0);
    }

    @Override
    public ServerInfo serverInfo() {
        serverInfo = new ServerInfo();
        return serverInfo;
    }

    @Override
    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    @Override
    public ByteBuffer clientPreEncrypt(ByteBuffer data) {
        return data;
    }

    @Override
    public ByteBuffer clientPostDecrypt(ByteBuffer data) {
        return data;
    }

    @Override
    public ByteBuffer serverPreEncrypt(ByteBuffer data) {
        return data;
    }

    @Override
    public ProtocolDecryptedMessage serverPostDecrypt(ByteBuffer data) {
        return new ProtocolDecryptedMessage(data, false);
    }

    @Override
    public ByteBuffer clientUdpPreEncrypt(ByteBuffer data) {
        return data;
    }

    @Override
    public ByteBuffer clientUdpPostDecrypt(ByteBuffer data) {
        return data;
    }

    @Override
    public ByteBuffer serverUdpPreEncrypt(ByteBuffer data) {
        return data;
    }

    @Override
    public ProtocolDecryptedMessage serverUdpPostDecrypt(ByteBuffer data) {
        return new ProtocolDecryptedMessage(data, false);
    }
}
