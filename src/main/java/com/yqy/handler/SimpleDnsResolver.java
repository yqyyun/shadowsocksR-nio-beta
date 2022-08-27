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

package com.yqy.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yqy
 * @date 2022/7/30 10:25
 */
public class SimpleDnsResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int CACHE_SIZE = 300;

    private static final int BLACKLIST_SIZE = 300;

    static final Map<String, InetAddress> cache = new ConcurrentHashMap<>();

    static final Map<String, Object> blackList = new ConcurrentHashMap<>();


    public static InetAddress resolve(String hostname) {
        if (hostname == null || blackList.containsKey(hostname)) {
            return null;
        }
        InetAddress inetAddress = cache.get(hostname);
        if (inetAddress == null) {
            try {
                inetAddress = InetAddress.getByName(hostname);
                cache.put(hostname, inetAddress);
                LOGGER.debug("look up name service for hostname: {}", hostname);
            } catch (UnknownHostException e) {
                LOGGER.warn("failed to get the ip address of hostname {}", hostname);
                blackList.put(hostname, new Object());
            } finally {
                clearCache();
            }
        }
        return inetAddress;
    }

    public static void clearCache() {
        if (cache.size() > CACHE_SIZE) {
            cache.clear();
            LOGGER.debug("ip cache is too large; clear ip cache");
        }
        if (blackList.size() > BLACKLIST_SIZE) {
            blackList.clear();
            LOGGER.debug("blacklist is too large; clear blacklist");
        }
    }
}
