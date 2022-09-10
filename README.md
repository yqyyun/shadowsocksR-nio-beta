
## Introduction
**ShadowsocksR-nio**is a secured SOCKS5 proxy for encrypted communications. It's developed using **Java NIO**.

## prerequisite
basic:
- linux
- jdk1.8+

if you want to use salsa/chacha series encryption method , you also need install followed dependency library on your linux
- glib2.14

> **内存大小**  
 目前该项目在本人笔记本上i5第七代，并分配200m内存的情况下工作良好，`-server -Xms200m -Xmx200m`，未见OOM异常。   
  并且在jdk11，开启zgc垃圾收集，并分配400m内存的实际使用环境中，也没有出现任何问题。

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

## Pre-build Binary Release
`/dist/`目录下可以找到预编译的构建版本

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

由于只是出于学习研究的目的写了本项目，所以只提供了基本的功能，也仅支持部分加密及协议混淆操作。  

**再次声明：该项目应该仅用于学习研究，不应用于其它任何用途，否则由于错误使用而引起的一切不良后果，均与本人无关。**

## 展望
**目前暂不考虑更新后续版本。**  

虽然目前该应用的性能已经能够满足我的要求了，但是精益求精，该项目本身还存在很多可以优化的地方。本人希望能够进一步提升并发性能以及内存性能，并且已经在这两方面的研究都已经取得突破性进展，理论上可以提升性能。  

实际上，在项目开发的早期，就已经尝试了内存性能更高方案，但是存在在数据丢失的bug，导致出现网页部分数据没有加载出来（比如图片）的情况。但为了尽快看到项目实际成果，换成了目前的简单化版本，目前版本会导致频繁的gc，所以替换成性能更优的ZGC垃圾收集器能获得性能提升。后续在研究如何提升并发性能时，发现了早期版本数据丢失的问题所在。 

本人不打算把主要精力放在该项目上了，对我来说该项目完全够用了。所以该项目是否会有后续版本，取决于我业余时间的分配。但可以肯定的是，如果有后续的版本更新，将会把主要目标放在性能提升上，包含前面提到的性能提升方案。

## License
Copyright 2022 yqy

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
```
    http://www.apache.org/licenses/LICENSE-2.0
```
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

Apache License, Version 2.0

Copyright © yqy 2022. Fork from Shadowsocksr by clowwindy