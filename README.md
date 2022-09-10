
## Introduction
**ShadowsocksR-nio**is a secured SOCKS5 proxy for encrypted communications. It's developed using **Java NIO**.

## prerequisite
basic:
- linux
- jdk1.8+

if you want to use salsa/chacha series encryption method , you also need install followed dependency library on your linux
- glib2.14

## Encryption method

| method                                     |                     |
| ------------------------------------------ | ------------------- |
| `none`                                     |                     |
| `rc4, rc4-md5, rc4-md5-6`                  |                     |
| `aes-128/192/256-ctr, aes-128/192/256-cfb` |                     |
| `chacha20xoric`                            | ie.  `chacha20`     |
| `chacha20ietfxoric`                        | ie. `chacha20-ietf` |
| `salsa20xoric`                             | ie. `salsa20`       |
| `xchacha20xoric`                           | ie. `xchacha20`     |

## Support Protocol

- `origin`
- `auth_chain_a`

## Support Obfs

- `plain`

## Build from source
```shell
mvn clean package -DskipTest
```
## Installation
```shell
tar -zxvf shadowsocksR-nio-beta-1.0-beta-bin.tar.gz
```
Set `JAVA_HOME` in `bin/env.sh` , if you don't set `JAVA_HOME` in your system-wide environment variable.

>提示：在使用jdk11以上jdk时，如果开启zgc垃圾收集器，可以带来性能提升

## Start Server

Before you start server,  you should check up the configuration file in `conf/config.properties`.

```shell
cd shadowsocksR-nio-beta-1.0-beta/bin
./start-server.sh
```
**Note**

**This project do not provide the client side software, so you need to install corresponding shadowsocksr client software.**

> 本项目没有提供客户端，所以你需要安装对应的**shadowsocksr**客户端软件才可使用，

**Note**: **this project should be just for study**.
> 注意：该项目目前应该仅用于学习研究。

Ok! That is it.

## 项目背景
该项目基于[shadowsocksr](https://github.com/shadowsocksr-backup/shadowsocksr.git)

本人在学习java nio以及netty网络编程框架之后，想要检验一下学习成果。毕竟书上的代码只能提供基本入门，想要深入的进一步掌握nio和netty框架，还得是实践出真知。但是苦于没有合适的项目，最终选择了shadowsocksr这个项目，因为在查看了源码时发现，作者eventloop的设计和nio很是相配。  
 在写项目的时候，踩了大量的坑，有些甚至是很难发现的天坑，不过也因此更加深入的掌握了nio，对nio的了解也更深了。当然远不止这些收获，比如数据加密，尤其是流式数据加密方式。

由于只是处于学习研究的目的写了本项目，所以只提供了基本的功能，也仅支持部分加密及协议混淆操作。

## License
Copyright 2022 yqy

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
```
    http://www.apache.org/licenses/LICENSE-2.0
```
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

Apache License, Version 2.0

Copyright © yqy 2022. Fork from Shadowsocksr by clowwindy