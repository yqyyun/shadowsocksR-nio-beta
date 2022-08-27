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

import com.yqy.util.Common;

import java.util.Objects;
import java.util.Properties;

/**
 * @author yqy
 * @date 2022/7/27 09:04
 */
public class CommandLineParameter {

    // local  hd:s:b:p:k:l:m:O:o:G:g:c:t:vq
    // server hd:s:p:k:m:O:o:G:g:c:t:vq
    // -p --server_port=
    // -k --password=
    // -l --local_port=
    // -s --server=
    // -m --method=
    // -O --protocol=
    // -o --obfs=
    // -G --protocol_param=
    // -g --obfs_param=
    // -b --local_address=
    // -v --verbose
    // -t --timeout=
    // --fast-open
    // --workers=
    // --manager_address=
    // --user=
    // --forbidden_ip
    // -d --daemon=
    // --pid_file
    // --log_file --log_dir
    // -q --quiet
    public static Properties parse(String[] args) throws InvalidCommandLineParameterException {
        Objects.requireNonNull(args, "commandline parameter is null!");
        Properties properties = new Properties();
        int len = args.length;
        String error = null;
        loop:
        for (int i = 0; i < len; i++) {
            switch (args[i]) {
                case "-p":
                case "--server_port":
                    if (++i < len && Common.checkPort(args[i])) {
                        properties.setProperty("server_port", args[i]);
                    } else {
                        error = "server_port is not a valid value!";
                        break loop;
                    }
                    break;
                case "-k":
                case "--password":
                    if (++i < len) {
                        properties.setProperty("password", args[i]);
                    } else {
                        error = "password not found!";
                        break loop;
                    }
                    break;
                case "-l":
                case "--local_port":
                    if (++i < len && Common.checkPort(args[i])) {
                        properties.setProperty("local_port", args[i]);
                    } else {
                        error = "local_port is not a valid value!";
                        break loop;
                    }
                    break;
                case "-s":
                case "--server":
                    if (++i < len && Common.checkAddress(args[i])) {
                        properties.setProperty("server", args[i]);
                    } else {
                        error = "the format of the given server is invalid!";
                        break loop;
                    }
                    break;
                case "-m":
                case "--method":
                    if (++i < len && Common.checkMethod(args[i])) {
                        properties.setProperty("method", args[i]);
                    } else {
                        error = "the given method is not supported!";
                        break loop;
                    }
                    break;
                case "-O":
                case "--protocol":
                    if (++i < len && Common.checkProtocol(args[i])) {
                        properties.setProperty("protocol", args[i]);
                    } else {
                        error = "the given protocol is not supported!";
                        break loop;
                    }
                    break;
                case "-o":
                case "--obfs":
                    if (++i < len && Common.checkObfs(args[i])) {
                        properties.setProperty("obfs", args[i]);
                    } else {
                        error = "the given obfs is not supported!";
                        break loop;
                    }
                    break;
                case "-G":
                case "--protocol_params":
                    if (++i < len && Common.checkProtocolParams(args[i])) {
                        properties.setProperty("protocol_params", args[i]);
                    } else {
                        error = "the given protocol_params is not supported!";
                        break loop;
                    }
                    break;
                case "-g":
                case "--obfs_param":
                    if (++i < len && Common.checkObfsParams(args[i])) {
                        properties.setProperty("obfs_params", args[i]);
                    } else {
                        error = "the given obfs_params is not supported!";
                        break loop;
                    }
                    break;
                case "-b":
                case "--local_address":
                    if (++i < len && Common.checkAddress(args[i])) {
                        properties.setProperty("local_address", args[i]);
                    } else {
                        error = "the format of the  given local_address is invalid!";
                        break loop;
                    }
                    break;
                case "-v":
                case "--verbose":
                    properties.setProperty("log_level", "debug");
                    break;
                case "-t":
                case "--timeout":
                    if (++i < len && Common.isNumber(args[i])) {
                        properties.setProperty("timeout", args[i]);
                    } else {
                        error = "the given timeout is not a number!";
                        break loop;
                    }
                    break;
                case "--fast_open":
                    properties.setProperty("fast_open", "true");
                    break;
                case "--bosses":
                    if (++i < len && Common.isNumber(args[i])) {
                        properties.setProperty("bosses", args[i]);
                    } else {
                        error = "the given bosses is not a number!";
                        break loop;
                    }
                    break;
                case "--workers":
                    if (++i < len && Common.isNumber(args[i])) {
                        properties.setProperty("workers", args[i]);
                    } else {
                        error = "the given workers is not a number!";
                        break loop;
                    }
                    break;
                case "--manager_address":
                    if (++i < len && Common.checkAddress(args[i])) {
                        properties.setProperty("manager_address", args[i]);
                    } else {
                        error = "the format of the given manager_address is invalid!";
                        break loop;
                    }
                    break;
                case "--user":
                    if (++i < len) {
                        properties.setProperty("user", args[i]);
                    } else {
                        error = "the value of the  given user is not found!";
                        break loop;
                    }
                    break;
                case "--forbidden_ip":
                    if (++i < len) {
                        properties.setProperty("forbidden_ip", args[i]);
                    }
                    break;
                case "-d":
                case "--daemon":
                    if (++i < len) {
                        properties.setProperty("daemon", args[i]);
                    }
                    break;
                case "--log_dir":
                    if (++i < len && Common.checkPath(args[i])) {
                        properties.setProperty("log_dir", args[i]);
                    } else {
                        error = "the given log_dir is not exists!";
                        break loop;
                    }
                    break;
                case "-q":
                case "--quiet":
                    properties.setProperty("log_level", "error");
                    break;
                default:
                    error = "unknown commandline parameter";
                    break loop;
            }
        }
        if (error != null) {
            throw new InvalidCommandLineParameterException(error);
        }
        return properties;
    }
}
