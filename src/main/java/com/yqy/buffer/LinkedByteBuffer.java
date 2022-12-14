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

package com.yqy.buffer;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author yqy
 * @date 2022/8/12 09:55
 */
public class LinkedByteBuffer {

    class Node {
        byte[] buffer;
        Node next;
    }

    private Node head;
    private Node tail;

    private int size;

    private int byteSize;

    public LinkedByteBuffer() {
        head = tail = new Node();
    }

    public void insert(int index, byte[] buf) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException();
        }
        if (index == 0) {
            addFirst(buf);
        } else if (index == size) {
            add(buf);
        } else {
            // (0, size)
            Objects.requireNonNull(buf);
            size++;
            byteSize += buf.length;
            int i = 1;
            for (Node prev = head.next; prev != null; prev = prev.next, i++) {
                if (i == index) {
                    Node node = new Node();
                    node.buffer = buf;
                    node.next = prev.next;
                    prev.next = node;
                    break;
                }
            }
        }

    }

    public void add(byte[] buf) {
        Objects.requireNonNull(buf);
        size++;
        byteSize += buf.length;
        Node node = new Node();
        node.buffer = buf;
        tail.next = node;
        tail = node;
    }

    public void add(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        add(Arrays.copyOfRange(buffer.array(), buffer.position(), buffer.limit()));
    }

    public void addFirst(byte[] buf) {
        Objects.requireNonNull(buf);
        size++;
        byteSize += buf.length;
        Node node = new Node();
        node.buffer = buf;
        node.next = head.next;
        head.next =node;
        // bug fixed; bug: removeFirst throw NullPointerException
        if (tail == head) {
            tail = node;
        }
    }

    public void addFirst(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        addFirst(Arrays.copyOfRange(buffer.array(), buffer.position(), buffer.limit()));
    }

    public byte[] getBytes(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        int i = 0;
        for (Node p = head.next; p != null; p = p.next, i++) {
            if (i == index) {
                return p.buffer;
            }
        }
        return null;
    }

    public byte[] getBytes() {
        byte[] ret = new byte[byteSize];
        int offset = 0;
        for (Node p = head.next; p != null; p = p.next) {
            byte[] buf = p.buffer;
            int len = buf.length;
            System.arraycopy(buf, 0, ret, offset, len);
            offset += len;
        }
        return ret;
    }

    public ByteBuffer getByteBuffer(int index) {
        assert getBytes(index) != null;
        return ByteBuffer.wrap(getBytes(index));
    }

    public ByteBuffer[] getByteBuffers() {
        ByteBuffer[] ret = new ByteBuffer[size];
        int i = 0;
        for (Node p = head.next; p != null; p = p.next,i++) {
            ret[i] = ByteBuffer.wrap(p.buffer);
        }
        return ret;
    }

    public ByteBuffer removeFirst() {
        ByteBuffer res;
            if (size > 0) {
                size--;
                byteSize -= head.next.buffer.length;
                res = ByteBuffer.wrap(head.next.buffer);
                head.next = head.next.next;
                if (size == 0) {
                    tail = head;
                }
                return res;
            }
        return null;
    }

    public void clear() {
        head.next = null;
        tail = head;
        size = 0;
        byteSize = 0;
    }

    public int getSize() {
        return size;
    }

    public int getByteSize() {
        return byteSize;
    }

    public static void main(String[] args) {
        LinkedByteBuffer lst = new LinkedByteBuffer();
        lst.addFirst(new byte[]{0});
        System.out.println(lst.removeFirst().get());
        lst.add(new byte[]{1});
        lst.add(new byte[]{2});
        lst.add(new byte[]{3});
        System.out.println(lst.removeFirst().get());
        System.out.println(lst.removeFirst().get());
        System.out.println(Arrays.toString(lst.getBytes()));
        lst.addFirst(new byte[]{4});
        System.out.println(Arrays.toString(lst.getBytes()));
        lst.insert(0, new byte[]{5});
        lst.insert(3, new byte[]{6});
        lst.insert(6, new byte[]{7});
        System.out.println(Arrays.toString(lst.getBytes()));
        System.out.println(Arrays.toString(lst.getByteBuffers()));
        System.out.println(lst.getSize());
        System.out.println(lst.getByteSize());
        System.out.println(lst.getBytes(0)[0]);
        System.out.println(lst.getBytes(3)[0]);
//        System.out.println(lst.getBytes(7)[0]);

    }

}
