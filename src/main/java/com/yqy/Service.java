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

package com.yqy;

import com.yqy.config.Configuration;
import com.yqy.loop.EventLoop;
import com.yqy.loop.EventLoopGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author yqy
 * @date 2022/7/27 13:33
 */
public class Service {

    private static final Logger LOGGER = LogManager.getLogger();

    private EventLoop bossLoop;

    private final EventLoopGroup pool;

    public Service(Configuration config) {
        int bosses= config.getInt("bosses", Runtime.getRuntime().availableProcessors());
        int workers = config.getInt("workers", -1);
        this.pool = new EventLoopGroup(bosses, workers);
        this.bossLoop = pool.newUnsubmitChild();
        LOGGER.info("bosses: {}, workers: {}, boss=worker: {}", bosses, workers, workers == -1);
    }

    public void bind(EventLoop loop) {
        this.bossLoop = loop;
    }

    public void startService() {
        if (bossLoop == null) {
            LOGGER.error("fail to start service, boosLoop is null, you must to bind a event loop service before start service ");
        } else {
            pool.submitIOTask(bossLoop);
            LOGGER.info("the service has ben started successfully!");
        }
    }

    public void stopService() {
        pool.shutdown();
        LOGGER.info("the server has been shutdown");
    }

    public void forceStopService() {
        pool.shutdownNow();
        LOGGER.info("the server has been shutdown now");
    }

    public EventLoopGroup getEventLoopGroup() {
        return pool;
    }

    public EventLoop getBossLoop() {
        return bossLoop;
    }
}
