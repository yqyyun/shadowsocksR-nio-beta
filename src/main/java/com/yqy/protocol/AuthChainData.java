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

import java.util.HashMap;
import java.util.Map;

/**
 * @author yqy
 * @date 2022/8/11 12:06
 */
public class AuthChainData {

    private String name;

    private Map<Integer, Integer> userIds;

    private int connectionId;

    private byte[] localClientId;

    private Map<Integer, ClientQueue> localClientIds;

    private int maxClient;

    private int maxBuffer;

    public AuthChainData(String name) {
        this.name = name;
        userIds = new HashMap<>();
        connectionId = 0;
        setMaxClient(64);
    }

    public void setMaxClient(int maxClient) {
        this.maxClient = maxClient;
        this.maxBuffer = Math.max(maxClient * 2, 1024);
    }

    public void update(byte[] userId, int clientId, int connectionId) {
//        if (!userIds.containsKey(userId)) {
            //TODO LRUcache
//            userIds.put(userId, 1);
//        }
//        localClientId = userIds.get(userId);
//        if (clientId in localClientID)
//        localClientId.update()
    }

    public boolean insert(byte[] userId, int clientId, int connectionId) {
        if (userId == null) {

        }
        return true;
    }

    public void remove(int userId, int clientId) {

    }

    public int getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public byte[] getLocalClientId() {
        return localClientId;
    }

    public void setLocalClientId(byte[] localClientId) {
        this.localClientId = localClientId;
    }
}
