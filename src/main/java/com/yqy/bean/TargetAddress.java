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

import com.yqy.util.ByteBufferCommon;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author yqy
 * @date 2022/7/30 08:57
 */
public class TargetAddress {
    public static final int ATYP_IPV4 = 0x1;
    public static final int ATYP_IPV6 = 0x4;
    public static final int ATYP_HOST = 0x3;

    int connectType;
    int atyp;
    byte[] dstAddr;
    int dstPort;
    int headerLen;
    boolean isResolved;
    InetAddress resolvedAddress;

    public int getConnectType() {
        return connectType;
    }

    public void setConnectType(int connectType) {
        this.connectType = connectType;
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

    public int getHeaderLen() {
        return headerLen;
    }

    public void setHeaderLen(int headLen) {
        this.headerLen = headLen;
    }

    public static TargetAddress parseHeader(ByteBuffer data) throws UnknownHostException {
        return ByteBufferCommon.parseHeader(data);
    }

    public boolean isIpv4() {
        return atyp == ATYP_IPV4;
    }

    public boolean isIpv6() {
        return atyp == ATYP_IPV6;
    }

    public boolean isHost() {
        return atyp == ATYP_HOST;
    }

    public void setResolvedAddress(InetAddress resolvedAddress) {
        this.resolvedAddress = resolvedAddress;
    }

    public InetAddress getResolvedAddress() {
        return resolvedAddress;
    }

    public boolean isResolved() {
        return resolvedAddress == null;
    }

    @Override
    public String toString() {
        return "ServerSocks5{" +
                "connectType=" + connectType +
                ", atyp=" + atyp +
                ", dstAddr=" + new String(dstAddr, StandardCharsets.UTF_8) +
                ", dstPort=" + dstPort +
                ", headerLen=" + headerLen +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetAddress that = (TargetAddress) o;
        return connectType == that.connectType && atyp == that.atyp && dstPort == that.dstPort && headerLen == that.headerLen && isResolved == that.isResolved && Arrays.equals(dstAddr, that.dstAddr) && Objects.equals(resolvedAddress, that.resolvedAddress);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(connectType, atyp, dstPort, headerLen, isResolved, resolvedAddress);
        result = 31 * result + Arrays.hashCode(dstAddr);
        return result;
    }
}
