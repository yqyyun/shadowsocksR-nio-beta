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

package com.yqy.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yqy
 * @date 2022/8/9 07:27
 */
public class IpBlackList {

    public static final Map<String, Object> blackList=new ConcurrentHashMap<>();

    private static final int MAX_SIZE = 300;

    public static boolean contains(String ip) {
        return blackList.containsKey(ip);
    }

    public static void add(String ip) {
        if (blackList.size() > MAX_SIZE) {
            blackList.remove(blackList.keySet().iterator().next());
        }
        blackList.put(ip, new Object());
    }
}
