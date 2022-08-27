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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author yqy
 * @date 2022/7/28 07:52
 */
public class Socks5Packet {

    public static final int ATYP_IPV4 = 0x1;
    public static final int ATYP_IPV6 = 0x4;
    public static final int ATYP_HOST = 0x3;

    int ver;
    int nmethods;
    byte[] methods;
    int cmd;
    int rsv;
    int atyp;
    byte[] dstAddr;
    InetAddress dstAddress;
    int dstPort;
    int rep;
    byte[] bndAddr;
    InetAddress bndAddress;
    int bndPort;
    int headerLen;
    int connectType;


    public int getHeaderLen() {
        return headerLen;
    }

    public void setHeaderLen(int headerLen) {
        this.headerLen = headerLen;
    }

    public InetAddress getDstAddress() {
        return dstAddress;
    }

    public void setDstAddress(InetAddress dstAddress) {
        this.dstAddress = dstAddress;
    }

    public InetAddress getBndAddress() {
        return bndAddress;
    }

    public void setBndAddress(InetAddress bndAddress) {
        this.bndAddress = bndAddress;
    }

    public int getConnectType() {
        return connectType;
    }

    public void setConnectType(int connectType) {
        this.connectType = connectType;
    }

    public int getVer() {
        return ver;
    }

    public void setVer(int ver) {
        this.ver = ver;
    }

    public int getNmethods() {
        return nmethods;
    }

    public void setNmethods(int nmethods) {
        this.nmethods = nmethods;
    }

    public byte[] getMethods() {
        return methods;
    }

    public void setMethods(byte[] methods) {
        this.methods = methods;
    }

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public int getRsv() {
        return rsv;
    }

    public void setRsv(int rsv) {
        this.rsv = rsv;
    }

    public int getAtyp() {
        return atyp;
    }

    public void setAtyp(int atyp) {
        this.atyp = atyp;
    }

    public byte[] getDstAddr() {
        return dstAddr;
    }

    public void setDstAddr(byte[] dstAddr) {
        this.dstAddr = dstAddr;
    }

    public int getDstPort() {
        return dstPort;
    }

    public void setDstPort(int dstPort) {
        this.dstPort = dstPort;
    }

    public int getRep() {
        return rep;
    }

    public void setRep(int rep) {
        this.rep = rep;
    }

    public byte[] getBndAddr() {
        return bndAddr;
    }

    public void setBndAddr(byte[] bndAddr) {
        this.bndAddr = bndAddr;
    }

    public int getBndPort() {
        return bndPort;
    }

    public void setBndPort(int bndPort) {
        this.bndPort = bndPort;
    }

    public static Socks5Packet parse(byte[] data) throws UnknownHostException {
        return com.yqy.util.Common.parseHeader(data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Socks5Packet that = (Socks5Packet) o;
        return ver == that.ver && nmethods == that.nmethods && cmd == that.cmd && rsv == that.rsv && atyp == that.atyp && dstPort == that.dstPort && rep == that.rep && headerLen == that.headerLen && bndPort == that.bndPort && connectType == that.connectType && Arrays.equals(methods, that.methods) && Arrays.equals(dstAddr, that.dstAddr) && Objects.equals(dstAddress, that.dstAddress) && Arrays.equals(bndAddr, that.bndAddr) && Objects.equals(bndAddress, that.bndAddress);
    }

    @Override
    public String toString() {
        return "Socks5Packet{" +
                "ver=" + ver +
                ", nmethods=" + nmethods +
                ", methods=" + Arrays.toString(methods) +
                ", cmd=" + cmd +
                ", rsv=" + rsv +
                ", atyp=" + atyp +
                ", dstAddr=" + Arrays.toString(dstAddr) +
                ", dstAddress=" + dstAddress +
                ", dstPort=" + dstPort +
                ", rep=" + rep +
                ", bndAddr=" + Arrays.toString(bndAddr) +
                ", bndAddress=" + bndAddress +
                ", headerLen=" + headerLen +
                ", bndPort=" + bndPort +
                ", connectType=" + connectType +
                '}';
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(ver, nmethods, cmd, rsv, atyp, dstAddress, dstPort, rep, bndAddress, headerLen, bndPort, connectType);
        result = 31 * result + Arrays.hashCode(methods);
        result = 31 * result + Arrays.hashCode(dstAddr);
        result = 31 * result + Arrays.hashCode(bndAddr);
        return result;
    }
}
