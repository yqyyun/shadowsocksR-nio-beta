package com.yqy.util;

import com.yqy.bean.Socks5Packet;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * @author yqy
 * @date 2022/7/28 09:27
 */
public class CommonTest {

    @Test
    public void testCrc32() {
        assertEquals(Common.crc32("123".getBytes(StandardCharsets.UTF_8)), Long.valueOf("884863d2", 16).longValue());
    }

    @Test
    public void testPreParseHeader() {

    }

    @Test
    public void testParseHeader() throws UnknownHostException {
//        b'\x03\x0ewww.google.com\x00\x50')
//        (0, ADDRTYPE_HOST, b'www.google.com', 80, 18)
        ByteBuffer ipv4 = ByteBuffer.allocate(20);
        ipv4.put((byte) 0x03);
        ipv4.put((byte) 0x0e);
        ipv4.put("www.google.com".getBytes(StandardCharsets.UTF_8));
        ipv4.put((byte) 0x00);
        ipv4.put((byte) 0x50);
        ipv4.flip();
        byte[] ipv4Data = new byte[ipv4.remaining()];
        ipv4.get(ipv4Data);
        Socks5Packet ipv4test = new Socks5Packet();
        ipv4test.setHeaderLen(18);
        ipv4test.setConnectType(0);
        ipv4test.setDstPort(80);
        ipv4test.setAtyp(Socks5Packet.ATYP_HOST);
        ipv4test.setDstAddr("www.google.com".getBytes(StandardCharsets.UTF_8));
        ipv4test.setDstAddress(InetAddress.getByName("www.google.com"));
        Socks5Packet expected = Common.parseHeader(ipv4Data);
        assertEquals(expected, ipv4test);
//        b'\x01\x08\x08\x08\x08\x00\x35')
//       (0, ADDRTYPE_IPV4, b'8.8.8.8', 53, 7)
        ipv4.clear();
        ipv4Data = new byte[]{1, 8, 8, 8, 8, 0, 0x35};
        ipv4test.setAtyp(Socks5Packet.ATYP_IPV4);
        ipv4test.setConnectType(0);
        ipv4test.setDstAddr(new byte[]{8, 8, 8, 8});
        ipv4test.setDstPort(53);
        ipv4test.setDstAddress(InetAddress.getByName("8.8.8.8"));
        ipv4test.setHeaderLen(7);
        expected = Common.parseHeader(ipv4Data);
        assertEquals(expected, ipv4test);
//        b'\x04$\x04h\x00@\x05\x08\x05\x00\x00\x00\x00\x00\x00\x10\x11\x00\x50'
//        (0, ADDRTYPE_IPV6, b'2404:6800:4005:805::1011', 80, 19)
        ipv4Data = new byte[]{4, '$', 4, 'h', 0, '@', 5, 8, 5, 0, 0, 0, 0, 0, 0, 0x10, 0x11, 0x00, 0x50};
        ipv4test.setAtyp(Socks5Packet.ATYP_IPV6);
        ipv4test.setConnectType(0);
        ipv4test.setDstAddr(new byte[]{'$', 4, 'h', 0, '@', 5, 8, 5, 0, 0, 0, 0, 0, 0, 0x10, 0x11});
        ipv4test.setDstPort(80);
        ipv4test.setDstAddress(InetAddress.getByName("2404:6800:4005:805::1011"));
        ipv4test.setHeaderLen(19);
        expected = Common.parseHeader(ipv4Data);
        assertEquals(expected, ipv4test);
    }
}
