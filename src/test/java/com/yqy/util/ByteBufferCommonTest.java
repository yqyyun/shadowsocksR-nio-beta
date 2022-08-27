package com.yqy.util;

import com.yqy.bean.TargetAddress;
import org.junit.Test;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/**
 * @author yqy
 * @date 2022/7/30 14:26
 */
public class ByteBufferCommonTest {

    @Test
    public void parseHeader() throws UnknownHostException {
        //        b'\x03\x0ewww.google.com\x00\x50')
//        (0, ADDRTYPE_HOST, b'www.google.com', 80, 18)
        ByteBuffer ipv4 = ByteBuffer.allocate(20);
        ipv4.put((byte) 0x03);
        ipv4.put((byte) 0x0e);
        ipv4.put("www.google.com".getBytes(StandardCharsets.UTF_8));
        ipv4.put((byte) 0x00);
        ipv4.put((byte) 0x50);
        TargetAddress ipv4test = new TargetAddress();
        ipv4test.setHeaderLen(18);
        ipv4test.setConnectType(0);
        ipv4test.setDstPort(80);
        ipv4test.setAtyp(TargetAddress.ATYP_HOST);
        ipv4test.setDstAddr("www.google.com".getBytes(StandardCharsets.UTF_8));
        ipv4.flip();
        TargetAddress expected = ByteBufferCommon.parseHeader(ipv4);
        System.out.println(expected);
        System.out.println(ipv4.get(0));
        assertEquals(expected, ipv4test);
//        b'\x01\x08\x08\x08\x08\x00\x35')
//       (0, ADDRTYPE_IPV4, b'8.8.8.8', 53, 7)
        ipv4.clear();
        byte[]  ipv4Data = new byte[]{1, 8, 8, 8, 8, 0, 0x35};
        ipv4.put(ipv4Data);
        ipv4test.setAtyp(TargetAddress.ATYP_IPV4);
        ipv4test.setConnectType(0);
        ipv4test.setDstAddr(new byte[]{8, 8, 8, 8});
        ipv4test.setDstPort(53);
        ipv4test.setHeaderLen(7);
        ipv4.flip();
        expected = ByteBufferCommon.parseHeader(ipv4);
        assertEquals(expected, ipv4test);
//        b'\x04$\x04h\x00@\x05\x08\x05\x00\x00\x00\x00\x00\x00\x10\x11\x00\x50'
//        (0, ADDRTYPE_IPV6, b'2404:6800:4005:805::1011', 80, 19)
        ipv4Data = new byte[]{4, '$', 4, 'h', 0, '@', 5, 8, 5, 0, 0, 0, 0, 0, 0, 0x10, 0x11, 0x00, 0x50};
        ipv4test.setAtyp(TargetAddress.ATYP_IPV6);
        ipv4test.setConnectType(0);
        ipv4test.setDstAddr(new byte[]{'$', 4, 'h', 0, '@', 5, 8, 5, 0, 0, 0, 0, 0, 0, 0x10, 0x11});
        ipv4test.setDstPort(80);
        ipv4test.setHeaderLen(19);
        ipv4.clear();
        ipv4.put(ipv4Data);
        ipv4.flip();
        expected = ByteBufferCommon.parseHeader(ipv4);
        assertEquals(expected, ipv4test);
    }
}