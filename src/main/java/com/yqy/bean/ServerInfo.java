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

package com.yqy.bean;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * @author yqy
 * @date 2022/8/11 11:30
 */
public class ServerInfo<T> {

    private T data;


    private String host;

    private int port;

    private String client;

    private int clientPort;

    private String protocolParam = "";

    private String obfsParam = "";

    private byte[] iv;

    private byte[] recv_iv;

    private String keyStr;

    private byte[] key;

    private int headLen = 30;

    private int tcpMss;

    private int bufSize;

    private short overhead;

    // protocol special

    // users{userId, password}
    private Map<Integer, String> users = new HashMap<>();

    private IntConsumer updateUserFunc;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public String getProtocolParam() {
        return protocolParam;
    }

    public void setProtocolParam(String protocolParam) {
        this.protocolParam = protocolParam;
    }

    public String getObfsParam() {
        return obfsParam;
    }

    public void setObfsParam(String obfsParam) {
        this.obfsParam = obfsParam;
    }

    public byte[] getIv() {
        return iv;
    }

    public void setIv(byte[] iv) {
        this.iv = iv;
    }

    public byte[] getRecv_iv() {
        return recv_iv;
    }

    public void setRecv_iv(byte[] recv_iv) {
        this.recv_iv = recv_iv;
    }

    public String getKeyStr() {
        return keyStr;
    }

    public void setKeyStr(String keyStr) {
        this.keyStr = keyStr;
    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    public int getHeadLen() {
        return headLen;
    }

    public void setHeadLen(int headLen) {
        this.headLen = headLen;
    }

    public int getTcpMss() {
        return tcpMss;
    }

    public void setTcpMss(int tcpMss) {
        this.tcpMss = tcpMss;
    }

    public int getBufSize() {
        return bufSize;
    }

    public void setBufSize(int bufSize) {
        this.bufSize = bufSize;
    }

    public short getOverhead() {
        return overhead;
    }

    public void setOverhead(short overhead) {
        this.overhead = overhead;
    }

    public Map<Integer, String> getUsers() {
        return users;
    }

    public void setUsers(Map<Integer, String> users) {
        this.users = users;
    }

    public IntConsumer getUpdateUserFunc() {
        return updateUserFunc;
    }

    public void setUpdateUserFunc(IntConsumer func) {
        this.updateUserFunc = func;
    }
}
