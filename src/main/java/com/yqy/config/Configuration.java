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

package com.yqy.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * @author yqy
 * @date 2022/7/27 08:18
 */
public class Configuration {

    static final Logger LOGGER = LogManager.getLogger();

    private Properties properties;

    private Configuration(String[] args) throws InvalidCommandLineParameterException {
        Properties conf = new Properties();
        String configDir = System.getenv("SHADOW_CONF_DIR");
        if (configDir == null || configDir.isEmpty()) {
            LOGGER.warn("SHADOW_CONF_DIR was not set");
        }
        File file = new File(configDir, "config.properties");
        try {
            if (file.exists()) {
                conf.load(new FileReader(file));
            } else {
                LOGGER.warn("config.properties was not found!");
            }
        } catch (IOException e) {
            LOGGER.error("Fail to load config.properties", e);
        }
        if (args == null) {
            this.properties = conf;
            return;
        }

        // commandline options
        Properties properties = new Properties(conf);
        try {
            Properties cmdOptions = CommandLineParameter.parse(args);
            cmdOptions.forEach((key, value) -> {
                properties.setProperty(((String) key), ((String) value));
            });
        } catch (InvalidCommandLineParameterException e) {
            LOGGER.error("fail to parse commandline parameter!", e);
            throw e;
        }
        this.properties = properties;
    }

    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String strValue = properties.getProperty(key);
        int intVal = defaultValue;
        if (strValue != null) {
            try {
                intVal = Integer.parseInt(strValue);
            } catch (NumberFormatException e) {
                LOGGER.warn("The value corresponding to key is invalid: key is {}", key);
            }
        }
        return intVal;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.getBoolean(value);

        }
        return defaultValue;
    }

    public static Configuration loadConfig(String[] args) {
        try {
            return new Configuration(args);
        } catch (InvalidCommandLineParameterException e) {
            return null;
        }
    }


    @Override
    public String toString() {
        return "Configuration{" +
                "properties=" + properties +
                '}';
    }
}
