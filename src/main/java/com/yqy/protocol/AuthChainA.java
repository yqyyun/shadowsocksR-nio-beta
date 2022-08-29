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

package com.yqy.protocol;

import com.yqy.bean.ProtocolDecryptedMessage;
import com.yqy.bean.ServerInfo;
import com.yqy.buffer.LinkedByteBuffer;
import com.yqy.encrypto.*;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import com.yqy.util.BinaryOperationUtil;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

/**
 * @author yqy
 * @date 2022/8/15 08:24
 */
public class AuthChainA extends AuthBase {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    public static final BigInteger BIG_INTEGER_31 = BigInteger.valueOf(31);
    public static final BigInteger BIG_INTEGER_127 = BigInteger.valueOf(127);
    public static final BigInteger BIG_INTEGER_521 = BigInteger.valueOf(521);
    public static final BigInteger BIG_INTEGER_1021 = BigInteger.valueOf(1021);
    public static final BigInteger BIG_INTEGER_8589934609 = BigInteger.valueOf(8589934609L);

    private byte[] recvBuf = new byte[0];

    private int unitLen = 2800;

    private boolean hasSentHeader = false;

    private boolean hasRecvHeader = false;

    private int clientId = 0;

    private int connectionId = 0;

    // second
    private long maxTimeDiff = 60 * 60 * 24;

    private final String salt = "auth_chain_a";

    private final byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);

    private int packId = 1;

    private int recvId = 1;

    private byte[] userId = null;

    private int userIdNum = 0;

    public byte[] userKey = null;

    private int clientOverhead = 4;

    public byte[] lastClientHash = null;

    private byte[] lastServerHash = null;

    private final XorShift128Plus randomClient = new XorShift128Plus();

    private final XorShift128Plus randomServer = new XorShift128Plus();

    public Encryptor encryptor = null;

    private Decryptor decryptor = null;


    public AuthChainA(String method) {
        super(method);
        super.rawTrans = false;
        super.noCompatibleMethod = "auth_chain_a";
        super.overhead = 4;
    }

    @Override
    public AuthChainData initData() {
        return super.initData();
    }

    @Override
    public void setServerInfo(ServerInfo<AuthChainData> serverInfo) {
        this.serverInfo = serverInfo;
        int maxClient;
        try {
            maxClient = Integer.parseInt(
                    serverInfo.getProtocolParam().split("#")[0]);
        } catch (NumberFormatException e) {
            maxClient = 64;
        }
        serverInfo.getData().setMaxClient(maxClient);
    }

    // TODO double or float
    public double trapezoidRandomFloat(double d) {
        if (d == 0) {
            return Math.random();
        }
        double random = Math.random();
        double a = 1 - d;
        return (Math.sqrt(a * a + 4 * d * random) - a) / (2 * d);
    }

    public int trapezoidRandomInt(int maxVal, double d) {
        double v = trapezoidRandomFloat(d);
        return ((int) (v * maxVal));
    }

    public int randomDataLen(int bufSize, byte[] lastHash, XorShift128Plus random) {
        if (bufSize > 1440) {
            return 0;
        }
        random.initFromBinLen(lastHash, (short) bufSize);
        if (bufSize > 1300) {
            return (int) (random.next().mod(BIG_INTEGER_31).intValue());
        }
        if (bufSize > 900) {
            return (int) (random.next().mod(BIG_INTEGER_127).intValue());
        }
        if (bufSize > 400) {
            return (int) (random.next().mod(BIG_INTEGER_521).intValue());
        }
        return (int) (random.next().mod(BIG_INTEGER_1021).intValue());
    }

    public int udpRandomDataLen(byte[] lastHash, XorShift128Plus random) {
        random.initFromBin(lastHash);
        return (int) (random.next().mod(BIG_INTEGER_127).intValue());
    }

    public int randomStartPos(int randomlen, XorShift128Plus random) {
        if (randomlen > 0) {
            return (int) (random.next().mod(BIG_INTEGER_8589934609).mod(BigInteger.valueOf(randomlen)).intValue());
        }
        return 0;
    }

    public byte[] randomData(int bufSize, byte[] buf, byte[] lastHash, XorShift128Plus random) {
        int randomLen = randomDataLen(bufSize, lastHash, random);
        byte[] randomDataBuf = rndBytes(randomLen);
        if (bufSize == 0) {
            return randomDataBuf;
        }
        if (randomLen > 0) {
            int startPos = randomStartPos(randomLen, random);
            byte[] resBuf = new byte[randomLen + bufSize];
            System.arraycopy(randomDataBuf, 0, resBuf, 0, startPos);
            System.arraycopy(buf, 0, resBuf, startPos, bufSize);
            System.arraycopy(randomDataBuf, startPos, resBuf, startPos + bufSize, randomLen - startPos);
            return resBuf;
        }
        return buf;
    }

    public byte[] packClientData(byte[] buf) {
        LinkedByteBuffer builder = new LinkedByteBuffer();
        buf = encryptor.encrypt(buf);
        int bufSize = buf.length;
        byte[] data = randomData(bufSize, buf, lastClientHash, randomClient);
        builder.add(userKey);
        builder.add(packInt(packId));
        byte[] macKey = builder.getBytes();
        builder.clear();
        int length = bufSize ^ unpackShort(slice(lastClientHash, 14, 16));
        builder.add(packShort((short) length));
        builder.add(data);
        data = builder.getBytes();
        lastClientHash = hmac(macKey, data);
        builder.add(slice(lastClientHash, 0, 2));
        data = builder.getBytes();
        packId++;
        return data;
    }

    public byte[] packServerData(byte[] buf) {
        LinkedByteBuffer builder = new LinkedByteBuffer();
        buf = encryptor.encrypt(buf);
        int bufSize = buf.length;
        byte[] data = randomData(bufSize, buf, lastServerHash, randomServer);
        builder.add(userKey);
        builder.add(packInt(packId));
        byte[] macKey = builder.getBytes();
        builder.clear();
        int length = bufSize ^ unpackShort(Arrays.copyOfRange(lastServerHash, 14, 16));
        builder.add(packShort((short) length));
        builder.add(data);
        data = builder.getBytes();
        lastServerHash = hmac(macKey, data);
        builder.add(Arrays.copyOfRange(lastServerHash, 0, 2));
        data = builder.getBytes();
        packId++;
        return data;
    }

    public byte[] packAuthData(byte[] authData, byte[] buf) {
        LinkedByteBuffer builder = new LinkedByteBuffer();
        byte[] data;
        builder.add(authData);
        builder.add(packShort(serverInfo.getOverhead()));
        builder.add(packShort((short) 0));
        LinkedByteBuffer macKeyBuilder = new LinkedByteBuffer();
        macKeyBuilder.add(serverInfo.getIv());
        macKeyBuilder.add(serverInfo.getKey());
        byte[] macKey = macKeyBuilder.getBytes();
        LinkedByteBuffer checkHeadBuilder = new LinkedByteBuffer();
        byte[] checkHead = rndBytes(4);
        lastClientHash = hmac(macKey, checkHead);
        checkHeadBuilder.add(checkHead);
        checkHeadBuilder.add(slice(lastClientHash, 0, 8));
        checkHead = checkHeadBuilder.getBytes();
        byte[] uid = rndBytes(4);
        String protocolParam = serverInfo.getProtocolParam();
        if (protocolParam.contains(":")) {
            try {
                String[] items = protocolParam.split(":");
                userKey = items[1].getBytes(StandardCharsets.UTF_8);
                uid = packInt(Integer.parseInt(items[0]));
            } catch (NumberFormatException ignore) {
            }
        }
        if (userKey == null) {
            userKey = serverInfo.getKey();
        }
        checkHeadBuilder.clear();
        checkHeadBuilder.add(base64(userKey));
        checkHeadBuilder.add(saltBytes);
        Encryptor encryptor = null;
        try {
            encryptor = new Encryptor(
                    "aes-128-cbc",
                    checkHeadBuilder.getBytes(),
                    new byte[16],
                    false
            );
        } catch (EncryptorException ignore) {
            ignore.printStackTrace();
            System.out.println("error======================");
        }
        data = encryptor.encrypt(builder.getBytes());
        builder.clear();
        int uidInt = unpackInt(uid) ^ unpackInt(slice(lastClientHash, 8, 12));
        uid = packInt(uidInt);
        builder.add(uid);
        builder.add(data);
        lastServerHash = hmac(userKey, builder.getBytes());
        builder.addFirst(checkHead);
        builder.add(slice(lastServerHash, 0, 4));
        LinkedByteBuffer keyBuilder = new LinkedByteBuffer();
        keyBuilder.add(base64(userKey));
        keyBuilder.add(base64(lastClientHash));
        try {
            byte[] key = keyBuilder.getBytes();
            this.encryptor = new Encryptor(
                    "rc4",
                    key,
                    null,
                    false);
            this.decryptor = new Decryptor(
                    "rc4",
                    key,
                    null, false
            );
        } catch (EncryptorException | DecryptorException ignore) {
        }
        builder.add(packClientData(buf));
        return builder.getBytes();
    }


    private byte[] authData() {
        int utcTime = (int) (System.currentTimeMillis() / 1000);
        AuthChainData data = serverInfo.getData();
        if (data.getConnectionId() > 0xff000000) {
            data.setLocalClientId(null);
        }
        if (data.getLocalClientId() == null) {
            byte[] localClientId = rndBytes(4);
            data.setLocalClientId(localClientId);
            LOGGER.debug("local client id {}", hexString(localClientId));
            data.setConnectionId(unpackInt(rndBytes(4)) & 0xffffff);
        }
        int cnnId = data.getConnectionId() + 1;
        data.setConnectionId(cnnId);
        byte[] bytes = new byte[12];
        System.arraycopy(packInt(utcTime), 0, bytes, 0, 4);
        System.arraycopy(data.getLocalClientId(), 0, bytes, 4, 4);
        System.arraycopy(packInt(cnnId), 0, bytes, 8, 4);
        return bytes;
    }

    private String hexString(byte[] localClientId) {
        return Hex.encodeHexString(localClientId);
    }

    public byte[] clientPreEncrypt(byte[] buf) {
        byte[] bytes;
        LinkedByteBuffer builder = new LinkedByteBuffer();
        int bufSize = buf.length;
        int i = 0;
        if (!hasSentHeader) {
            int headSize = getHeadSize(buf, 30);
            int dataLen = min(bufSize,
                    new Random().nextInt(31) + headSize);
            builder.add(packAuthData(authData(),
                    slice(buf, 0, dataLen)));
            buf = slice(buf, dataLen, bufSize);
//            i += dataLen;
            hasSentHeader = true;
        }
//        int alignLen = (bufSize / unitLen) * unitLen + i;
//        while (i < alignLen) {
            while (buf.length > unitLen) {
                builder.add(packClientData(slice(buf, 0, unitLen)));
                buf = slice(buf, unitLen, buf.length);
            }
//            builder.add(packClientData(Arrays.copyOfRange(buf, i, i += unitLen)));
//        }
//        builder.add(packClientData(slice(buf, i, bufSize)));
        builder.add(packClientData(buf));
        return builder.getBytes();
    }

    public ProtocolDecryptedMessage serverPostDecrypt(byte[] buf) {
        if (rawTrans) {
            return new ProtocolDecryptedMessage(wrap(buf), false);
        }
        LinkedByteBuffer outBuilder = new LinkedByteBuffer();
        LinkedByteBuffer recvBuilder = new LinkedByteBuffer();
        recvBuilder.add(recvBuf);
        recvBuilder.add(buf);
        recvBuf = recvBuilder.getBytes();
        boolean sendBack = false;

        if (!hasRecvHeader) {
            byte[] md5Data = null;
            if (recvBuf.length >= 12 ||
                    recvBuf.length == 7 || recvBuf.length == 8) {
                int recvLen = min(recvBuf.length, 12);
                LinkedByteBuffer mackKeyBuilder = new LinkedByteBuffer();
                mackKeyBuilder.add(serverInfo.getRecv_iv());
                mackKeyBuilder.add(serverInfo.getKey());
                md5Data = hmac(mackKeyBuilder.getBytes(), recvBuf, 0, 4);
                if (!Arrays.equals(
                        //recvLen = 12
                        slice(md5Data, 0, recvLen - 4),
                        slice(recvBuf, 4, recvLen)
                )) {
                    return notMatchReturn(wrap(recvBuf));
                }
            }

            if (recvBuf.length < 12 + 24) {
                return new ProtocolDecryptedMessage(EMPTY_BUFFER, false);
            }

            lastClientHash = md5Data;
            int uidInt = unpackInt(slice(recvBuf, 12, 16)) ^ unpackInt(slice(md5Data, 8, 12));
            userIdNum = uidInt;
            byte[] uid = packInt(uidInt);
            if (serverInfo.getUsers().containsKey(uidInt)) {
                userId = uid;
                userKey = serverInfo.getUsers().get(uidInt).getBytes(StandardCharsets.UTF_8);
                serverInfo.getUpdateUserFunc().accept(uidInt);
            } else {
                userIdNum = 0;
                if (serverInfo.getUsers().isEmpty()) {
                    userKey = serverInfo.getKey();
                } else {
                    userKey = serverInfo.getRecv_iv();
                }
            }
            // slice(recvBuf, 12, 12 + 20)
            md5Data = hmac(userKey, recvBuf, 12, 20);
            if (!Arrays.equals(
                    slice(md5Data, 0, 4),
                    slice(recvBuf, 32, 36)
            )) {
                LOGGER.error("{} data uncorrect auth HmacMd5 from {}:{}, data {}",
                        noCompatibleMethod, serverInfo.getClient(), serverInfo.getClientPort(),
                        hexString(recvBuf));
                if (recvBuf.length < 36) {
                    return new ProtocolDecryptedMessage(EMPTY_BUFFER, false);
                }
                return notMatchReturn(wrap(recvBuf));
            }
            lastServerHash = md5Data;
            LinkedByteBuffer keyBuilder = new LinkedByteBuffer();
            keyBuilder.add(base64(userKey));
            keyBuilder.add(saltBytes);
            byte[] head = null;
            try {
                Decryptor decryptor = new Decryptor(
                        "aes-128-cbc",
                        keyBuilder.getBytes(),
                        new byte[16],
                        false);
                //TODO
                head = decryptor.decrypt(wrap(slice(recvBuf, 16, 32))).array();
            } catch (DecryptorException e) {
            }
            clientOverhead = unpackShort(Arrays.copyOfRange(head, 12, 14));
            int utcTime = unpackInt(Arrays.copyOfRange(head, 0, 4));
            int clientId = unpackInt(Arrays.copyOfRange(head, 4, 8));
            int connId = unpackInt(Arrays.copyOfRange(head, 8, 12));
            int now = (int) (System.currentTimeMillis() / 1000);
            int timeDiff = now - utcTime;
            if (timeDiff > maxTimeDiff || timeDiff < -maxTimeDiff) {
                LOGGER.info("{}: wrong timestamp, timeDiff {}, data {}",
                        noCompatibleMethod, timeDiff, hexString(head));
                return notMatchReturn(wrap(recvBuf));
                // TODO
            } else if (serverInfo.getData().insert(userId, clientId, connId)) {
                hasRecvHeader = true;
                this.clientId = clientId;
                this.connectionId = connId;
            } else {
                LOGGER.info("{}: auth fail, data {}",
                        noCompatibleMethod, hexString(outBuilder.getBytes()));
                return notMatchReturn(wrap(recvBuf));
            }
            // onRecvAuthData(utcTime)
            keyBuilder.clear();
            keyBuilder.add(base64(userKey));
            keyBuilder.add(base64(lastClientHash));
            try {
                byte[] key = keyBuilder.getBytes();
                this.decryptor = new Decryptor(
                        "rc4",
                        key,
                        null, false);
                this.encryptor = new Encryptor("rc4", key, null, false);
            } catch (DecryptorException | EncryptorException ignore) {
            }
            recvBuf = slice(recvBuf, 36, recvBuf.length);
            hasRecvHeader = true;
            sendBack = true;
        }
        while (recvBuf.length > 4) {
            byte[] macKey = toBytes(userKey, packInt(recvId));
            //TODO NO TEST
            int dataLen = byte2ShortLE(recvBuf[0], recvBuf[1]) ^
                    byte2ShortLE(lastClientHash[14], lastClientHash[15]);
            int rndLen = randomDataLen(dataLen, lastClientHash, randomClient);
            int length = dataLen + rndLen;
            if (length >= 4096) {
                rawTrans = true;
                recvBuf = new byte[0];
                if (recvId == 1) {
                    LOGGER.info("{}: over size", noCompatibleMethod);
                    byte[] res = new byte[2048];
                    Arrays.fill(res, (byte) 'E');
                    return new ProtocolDecryptedMessage(wrap(res), false);
                } else {
                    throw new IllegalStateException("server post decrypt data error");
                }
            }
            if (length + 4 > recvBuf.length) {
                break;
            }

            byte[] clientHash = hmac(macKey, recvBuf, 0, length + 2);
            if (!Arrays.equals(
                    slice(clientHash, 0, 2),
                    slice(recvBuf, length + 2, length + 4)
            )) {
                LOGGER.info("{}: checksum error, data {}",
                        noCompatibleMethod, hexString(Arrays.copyOfRange(recvBuf, 0, length)));
                rawTrans = true;
                recvBuf = new byte[0];
                if (recvId == 1) {
                    byte[] res = new byte[2048];
                    Arrays.fill(res, (byte) 'E');
                    return new ProtocolDecryptedMessage(wrap(res), false);
                } else {
                    throw new IllegalStateException("server post decrypt data error");
                }
            }
            recvId++;
            int pos = 2;
            if (dataLen > 0 && rndLen > 0) {
                pos = 2 + randomStartPos(rndLen, randomClient);
            }
            outBuilder.add(
                    decryptor.decrypt(
                            slice(recvBuf, pos, dataLen + pos)));
            lastClientHash = clientHash;
            recvBuf = slice(recvBuf, length + 4, recvBuf.length);
            if (dataLen == 0) {
                sendBack = true;
            }
        }
        if (outBuilder.getByteSize() > 0) {
            serverInfo.getData().update(userId, clientId, connectionId);
        }
        return new ProtocolDecryptedMessage(wrap(outBuilder.getBytes()), sendBack);
    }

    private byte[] toBytes(byte[] a, byte[] b) {
        LinkedByteBuffer keyBuilder = new LinkedByteBuffer();
        keyBuilder.add(a);
        keyBuilder.add(b);
        return keyBuilder.getBytes();
    }

    public byte[] clientPostDecrypt(byte[] buf) {
        if (rawTrans) {
            return buf;
        }
        LinkedByteBuffer recvBuilder = new LinkedByteBuffer();
        recvBuilder.add(recvBuf);
        recvBuilder.add(buf);
        recvBuf = recvBuilder.getBytes();
        LinkedByteBuffer outBuilder = new LinkedByteBuffer();
        while (recvBuf.length > 4) {
            byte[] macKey = toBytes(userKey, packInt(recvId));
            int dataLen = byte2ShortLE(recvBuf[0], recvBuf[1]) ^
                    byte2ShortLE(lastServerHash[14], lastServerHash[15]);
            int rndLen = randomDataLen(dataLen, lastServerHash, randomServer);
            int length = dataLen + rndLen;
            if (length > 4096) {
                rawTrans = true;
                recvBuf = new byte[0];
                throw new IllegalStateException("clientPostDecrypt data error");
            }
            if (length + 4 > recvBuf.length) {
                break;
            }
            byte[] serverHash = hmac(macKey, recvBuf, 0, length + 2);
            if (!Arrays.equals(
                    Arrays.copyOfRange(serverHash, 0, 2),
                    Arrays.copyOfRange(recvBuf, length + 2, length + 4)
            )) {
                LOGGER.info("{}: checksum error, data {}",
                        noCompatibleMethod, hexString(Arrays.copyOfRange(recvBuf, 0, length)));
                rawTrans = true;
                recvBuf = new byte[0];
                throw new IllegalStateException("clientPostDecrypt data error");
            }
            int pos = 2;
            if (dataLen > 0 && rndLen > 0) {
                pos = 2 + randomStartPos(rndLen, randomServer);
            }
            byte[] decrypt = decryptor.decrypt(
                    Arrays.copyOfRange(recvBuf, pos, pos + dataLen)
            );
            lastServerHash = serverHash;
            if (recvId == 1) {
                serverInfo.setTcpMss(byte2ShortLE(decrypt[0], decrypt[1]));
                decrypt = Arrays.copyOfRange(decrypt, 2, decrypt.length);
            }
            outBuilder.add(decrypt);
            recvId++;
            recvBuf = Arrays.copyOfRange(recvBuf, length + 4, recvBuf.length);
        }
        return outBuilder.getBytes();
    }

    public byte[] serverPreEncrypt(byte[] buf) {
        if (rawTrans) {
            return buf;
        }
        LinkedByteBuffer builder = new LinkedByteBuffer();
        if (packId == 1) {
            int tcpMss = min(serverInfo.getTcpMss(), 1500);
            if (tcpMss < 576) {
                tcpMss = 1500;
            }
            serverInfo.setTcpMss(tcpMss);
            builder.add(packShort((short) tcpMss));
            builder.add(buf);
            unitLen = tcpMss - clientOverhead;
            buf = builder.getBytes();
            builder.clear();
        }
        while (buf.length > unitLen) {
            builder.add(packServerData(slice(buf, 0, unitLen)));
            buf = slice(buf, unitLen, buf.length);
        }
        builder.add(packServerData(buf));
        return builder.getBytes();
    }

    public byte[] clientUdpPreEncrypt(byte[] buf) {
        if (userKey == null) {
            String protocolParam = serverInfo.getProtocolParam();
            if (protocolParam.contains(":")) {
                try {
                    String[] items = protocolParam.split(":");
                    userKey = md5(items[1].getBytes(StandardCharsets.UTF_8));
                    userId = packInt(Integer.parseInt(items[0]));
                } catch (NumberFormatException ignore) {
                }
            }
            if (userKey == null) {
                userId = rndBytes(4);
                userKey = serverInfo.getKey();
            }
        }
        byte[] authData = rndBytes(3);
        byte[] macKey = serverInfo.getKey();
        byte[] md5data = hmac(macKey, authData);
        int uidInt = unpackInt(userId) ^ unpackInt(slice(md5data, 0, 4));
        byte[] uid = packInt(uidInt);
        int rndLen = udpRandomDataLen(md5data, randomClient);
        LinkedByteBuffer builder = new LinkedByteBuffer();
        builder.add(base64(userKey));
        builder.add(base64(md5data));
        try {
            Encryptor encryptor = new Encryptor(
                    "rcr", builder.getBytes(),
                    null, false);
            byte[] out = encryptor.encrypt(buf);
            builder.clear();
            builder.add(out);
        } catch (EncryptorException ignore) {
        }
        builder.add(rndBytes(rndLen));
        builder.add(authData);
        builder.add(uid);
        builder.add(slice(hmac(userKey, buf), 0, 1));
        return builder.getBytes();
    }

    public byte[] clientUdpPostDecrypt(byte[] buf) {
        int bufLen = buf.length;
        if (bufLen <= 8) {
            return new byte[0];
        }
        if (hmac(userKey, buf, 0, bufLen - 1)[0] !=
                buf[bufLen - 1]) {
            return new byte[0];
        }
        byte[] macKey = serverInfo.getKey();
        // slice(bufLen - 8, bufLen -1)
        byte[] md5data = hmac(macKey, buf, bufLen - 8, 7);
        int rndLen = udpRandomDataLen(md5data, randomServer);
        LinkedByteBuffer builder = new LinkedByteBuffer();
        builder.add(base64(userKey));
        builder.add(base64(md5data));

        try {
            Decryptor decryptor = new Decryptor("rc4",
                    builder.getBytes(), null, false);
            return decryptor.decrypt(Arrays.copyOfRange(buf, 0, bufLen - 8 - rndLen));
        } catch (DecryptorException e) {
        }
        return new byte[0];
    }

    public byte[] serverUdpPreEncrypt(byte[] buf, byte[] uid) {
        if (serverInfo.getUsers().containsKey(unpackInt(uid))) {
            userKey = serverInfo.getUsers().get(unpackInt(uid)).getBytes(StandardCharsets.UTF_8);
        } else {
            if (serverInfo.getUsers().isEmpty()) {
                userKey = serverInfo.getKey();
            } else {
                userKey = serverInfo.getRecv_iv();
            }
        }
        byte[] authData = rndBytes(7);
        byte[] mackey = serverInfo.getKey();
        byte[] md5data = hmac(mackey, authData);
        int rndLen = udpRandomDataLen(md5data, randomServer);
        LinkedByteBuffer builder = new LinkedByteBuffer();
        builder.add(base64(userKey));
        builder.add(base64(md5data));
        try {
            Encryptor encryptor = new Encryptor("rc4",
                    builder.getBytes(), null, false);
            builder.clear();
            builder.add(encryptor.encrypt(buf));
        } catch (EncryptorException e) {
        }
        builder.add(rndBytes(rndLen));
        builder.add(authData);
        builder.add(slice(hmac(userKey, buf), 0, 1));
        return builder.getBytes();
    }

    public byte[] serverUdpPostDecrypt(byte[] buf) {
        byte[] mackey = serverInfo.getKey();
        int bufLen = buf.length;
        // slice(bufLen - 8, bufLen - 5)
        byte[] md5data = hmac(mackey, buf, bufLen - 8, 3);
        int uidInt = unpackInt(slice(buf, bufLen - 5, bufLen - 1));
        byte[] uid = packInt(uidInt);
        if (serverInfo.getUsers().containsKey(uidInt)) {
            userKey = serverInfo.getUsers().get(uidInt).getBytes(StandardCharsets.UTF_8);
        } else {
            uid = null;
            if (serverInfo.getUsers().isEmpty()) {
                userKey = serverInfo.getKey();
            } else {
                userKey = serverInfo.getRecv_iv();
            }
        }
        if (hmac(userKey, buf, 0, bufLen - 1)[1] !=
                buf[bufLen - 1]) {
            return new byte[0];
        }
        int rndLen = udpRandomDataLen(md5data, randomClient);
        LinkedByteBuffer builder = new LinkedByteBuffer();
        builder.add(base64(userKey));
        builder.add(base64(md5data));
        try {
            Decryptor decryptor = new Decryptor("rc4",
                    builder.getBytes(), null, false);
            builder.clear();
            builder.add(decryptor.decrypt(slice(buf, 0, bufLen - 8 - rndLen)));
        } catch (DecryptorException e) {
        }
        return builder.getBytes();
    }

    public void dispose() {
        serverInfo.getData().remove(unpackInt(userId),clientId);
    }

    private byte[] slice(byte[] buf, int from, int to) {
        return Arrays.copyOfRange(buf, from, to);
    }

    private byte[] rndBytes(int len) {
        return Common.generateRandomBytes(len);
    }

    private byte[] packInt(int i) {
        return BinaryOperationUtil.pack(i, false);
    }

    private byte[] packShort(short s) {
        return BinaryOperationUtil.pack(s, false);
    }

    private int unpackInt(byte[] b) {
        return BinaryOperationUtil.unpackInt(b, false);
    }

    private short unpackShort(byte[] b) {
        return BinaryOperationUtil.unpackShort(b, false);
    }

    private short byte2ShortLE(byte a, byte b) {
        return com.yqy.util.Common.toShort(b, a);
    }

    public byte[] hmac(byte[] macKey, byte[] data) {
        return HmacMD5.digest(macKey, data);
    }

    public byte[] hmac(byte[] macKey, byte[] data, int dataOff, int dataLen) {
        return HmacMD5.digest(macKey, data, dataOff, dataLen);
    }

    private byte[] md5(byte[] data) {
        return Md5.digest(data);
    }

    private byte[] base64(byte[] data) {
        return Base64.encode(data);
    }

    public int getHeadSize(byte[] buf, int defaultValue) {
        if (buf.length < 2) {
            return defaultValue;
        }
        int headType = com.yqy.util.Common.char2Int((char) buf[0]);
        if (headType == 1) {
            return 7;
        }
        if (headType == 4) {
            return 19;
        }
        if (headType == 3) {
            return 4 + com.yqy.util.Common.char2Int((char) buf[1]);
        }
        return defaultValue;
    }

    private int min(int a, int b) {
        return Math.min(a, b);
    }

    private ByteBuffer wrap(byte[] buf) {
        return ByteBuffer.wrap(buf);
    }
}
