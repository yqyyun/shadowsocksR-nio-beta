package com.yqy.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author yqy
 * @date 2022/8/12 15:41
 */
public class BinaryOperationUtilTest {

    @Test
    public void reverseByteOrder() {
        long l = BinaryOperationUtil.reverseByteOrder(1);
        assertEquals(0x0100000000000000L, l);
    }

    @Test
    public void reverseBytes() {
        long l = BinaryOperationUtil.reverseBytes(1L);
        assertEquals(0x0100000000000000L, l);
    }

    @Test
    public void testReverseBytes() {
        int i = BinaryOperationUtil.reverseBytes(1);
        assertEquals(0x01000000L, i);

    }

    @Test
    public void pack() {
        byte[] p1 = BinaryOperationUtil.pack(1, true);
        byte[] p2 = BinaryOperationUtil.pack(1, false);
        assertArrayEquals(new byte[]{0, 0,0,1}, p1);
        assertArrayEquals(new byte[]{1, 0,0,0}, p2);
    }

    @Test
    public void testPack() {
        byte[] p1 = BinaryOperationUtil.pack(1L, true);
        byte[] p2 = BinaryOperationUtil.pack(1L, false);
        assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 1}, p1);
        assertArrayEquals(new byte[]{1, 0, 0, 0, 0, 0, 0, 0}, p2);
    }

    @Test
    public void unpackInt() {
        byte[] b = new byte[]{0, 0, 0, 1};
        int i1 = BinaryOperationUtil.unpackInt(b, true);
        int i2 = BinaryOperationUtil.unpackInt(b, false);
        assertEquals(1, i1);
        assertEquals(0x01000000, i2);
    }

    @Test
    public void unpackLong() {
        byte[] b = new byte[]{0, 0, 0, 0, 0, 0, 0, 1};
        long l1 = BinaryOperationUtil.unpackLong(b, true);
        long l2 = BinaryOperationUtil.unpackLong(b, false);
        assertEquals(1, l1);
        assertEquals(0x0100000000000000L, l2);
    }

    @Test
    public void unpackShort() {
        byte[] b = new byte[]{0, 1};
        short s1 = BinaryOperationUtil.unpackShort(b, true);
        short s2 = BinaryOperationUtil.unpackShort(b, false);
        assertEquals(1, s1);
        assertEquals(0x0100, s2);
    }

    @Test
    public void unpack() {
        assertTrue(true);
    }
}