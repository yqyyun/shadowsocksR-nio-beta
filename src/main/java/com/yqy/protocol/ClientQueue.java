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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yqy
 * @date 2022/8/11 11:42
 */
public class ClientQueue {

    private static final Logger LOGGER = LogManager.getLogger();

    private int front;

    private int back;

    private Map<Integer, Integer> alloc;

    private boolean enable;

    private long lastUpdate;

    private int ref = 0;

    public ClientQueue(int beginId) {
        this.front = beginId - 64;
        this.back = beginId + 1;
        this.alloc = new HashMap<>();
        this.lastUpdate = System.currentTimeMillis() / 1000;
        this.enable = true;
    }

    public void update() {
        lastUpdate = System.currentTimeMillis() / 1000;
    }

    public void addRef() {
        ref += 1;
    }

    public void delRef() {
        if (ref > 0) {
            ref -= 1;
        }
    }

    public boolean isActive() {
        long now = System.currentTimeMillis() / 1000;
        return (ref > 0) && (now - lastUpdate < 60 * 10);
    }

    public void  reEnable(int connectionId) {
        enable = true;
        front = connectionId - 64;
        back = connectionId + 1;
        // TODO
//        alloc.clear();
        alloc = new HashMap<>();
    }

    public boolean insert(int connectionId) {
        if (!enable) {
            LOGGER.warn("obfs auth: not enable");
            return false;
        }
        if (!isActive()) {
            reEnable(connectionId);
        }
        update();
        int front = this.front;
        if (connectionId < front) {
            LOGGER.warn("obfs auth: deprecated id, someone replay attack");
            return false;
        }
        if (connectionId > front + 0x4000) {
            LOGGER.warn("obfs auth: wrong id");
            return false;
        }
        if (alloc.containsKey(connectionId)) {
            LOGGER.warn("obfs auth: duplicated id, someone replay attack");
            return false;
        }
        if (back <= connectionId) {
            back = connectionId + 1;
        }
        alloc.put(connectionId, 1);
        while (alloc.containsKey(front) || front + 0x1000 < back) {
            if (alloc.containsKey(front)) {
                alloc.remove(front);
            }
            front++;
        }
        this.front = front;
        addRef();
        return true;
    }
}
