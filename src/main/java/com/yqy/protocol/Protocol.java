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
 * @date 2022/8/5 12:28
 */
public interface Protocol<T> {

    enum Method {
        ORIGIN("origin"),
        AUTH_CHAIN_A("auth_chain_a")
        ;

        private final String name;

        Method(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Method getMethodInfo(String name) throws UnknownProtocolMethodException {
            switch (name) {
                case "origin":
                    return ORIGIN;
                case "auth_chain_a":
                    return AUTH_CHAIN_A;
                default:
                    throw new UnknownProtocolMethodException();
            }
        }
    }

    public static Protocol getInstance(Method name) throws NosupportedProtocolMethodException {
        switch (name) {
            case ORIGIN:
                return new Origin();
            case AUTH_CHAIN_A:
                return new AuthChainA(name.name);
            default:
                throw new NosupportedProtocolMethodException();
        }
    }

    T initData();

    ServerInfo<T> serverInfo();

    ServerInfo<T> getServerInfo();

    ByteBuffer clientPreEncrypt(ByteBuffer data);

    ByteBuffer clientPostDecrypt(ByteBuffer data);

    ByteBuffer serverPreEncrypt(ByteBuffer data);

    ProtocolDecryptedMessage serverPostDecrypt(ByteBuffer data);

    ByteBuffer clientUdpPreEncrypt(ByteBuffer data);

    ByteBuffer clientUdpPostDecrypt(ByteBuffer data);

    ByteBuffer serverUdpPreEncrypt(ByteBuffer data);

    ProtocolDecryptedMessage serverUdpPostDecrypt(ByteBuffer data);

}
