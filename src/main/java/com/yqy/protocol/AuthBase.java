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
import java.util.Arrays;

/**
 * @author yqy
 * @date 2022/8/11 11:26
 */
public abstract class AuthBase implements Protocol<AuthChainData> {

    protected String method;

    protected String noCompatibleMethod;

    protected short overhead;

    protected ServerInfo<AuthChainData> serverInfo;

    protected boolean rawTrans = false;

    public AuthBase(String method) {
        this.method = method;
        this.noCompatibleMethod = "";
        this.overhead = 4;
    }

    @Override
    public AuthChainData initData() {
        return new AuthChainData(method);
    }

    @Override
    public final ServerInfo<AuthChainData> serverInfo() {
        ServerInfo<AuthChainData> serverInfo = new ServerInfo<>();
        serverInfo.setData(initData());
        setServerInfo(serverInfo);
        return serverInfo;
    }

    public int getOverhead() {
        return overhead;
    }

    public void setServerInfo(ServerInfo<AuthChainData> serverInfo) {
        this.serverInfo = serverInfo;
    }

    public ServerInfo<AuthChainData> getServerInfo() {
        return serverInfo;
    }

    public ProtocolDecryptedMessage notMatchReturn(ByteBuffer buffer) {
        rawTrans = true;
        overhead = 0;
        if (method.equals(noCompatibleMethod)) {
            ByteBuffer res = ByteBuffer.allocate(2048);
            Arrays.fill(res.array(), (byte) 'E');
            return new ProtocolDecryptedMessage(res, false);
        }
        return new ProtocolDecryptedMessage(buffer, false);
    }

    protected abstract byte[] clientPreEncrypt(byte[] data);
    protected abstract ProtocolDecryptedMessage serverPostDecrypt(byte[] data);
    protected abstract byte[] serverPreEncrypt(byte[] data);
    protected abstract byte[] clientPostDecrypt(byte[] data);

    @Override
    public ByteBuffer clientPreEncrypt(ByteBuffer data) {
        int pos = data.position();
        int limit = data.limit();
        return ByteBuffer.wrap(clientPreEncrypt(
                Arrays.copyOfRange(data.array(), pos, limit)));
    }

    @Override
    public ByteBuffer clientPostDecrypt(ByteBuffer data) {
        int pos = data.position();
        int limit = data.limit();
        return ByteBuffer.wrap(clientPostDecrypt(
                Arrays.copyOfRange(data.array(), pos, limit)));
    }

    @Override
    public ByteBuffer serverPreEncrypt(ByteBuffer data) {
        int pos = data.position();
        int limit = data.limit();
        return ByteBuffer.wrap(serverPreEncrypt(
                Arrays.copyOfRange(data.array(), pos, limit)));
    }

    @Override
    public ProtocolDecryptedMessage serverPostDecrypt(ByteBuffer data) {
        int pos = data.position();
        int limit = data.limit();
        return serverPostDecrypt(
                Arrays.copyOfRange(data.array(), pos, limit));
    }

    @Override
    public ByteBuffer clientUdpPreEncrypt(ByteBuffer data) {
        return null;
    }

    @Override
    public ByteBuffer clientUdpPostDecrypt(ByteBuffer data) {
        return null;
    }

    @Override
    public ByteBuffer serverUdpPreEncrypt(ByteBuffer data) {
        return null;
    }

    @Override
    public ProtocolDecryptedMessage serverUdpPostDecrypt(ByteBuffer data) {
        return null;
    }
}
