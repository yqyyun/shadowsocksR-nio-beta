package com.yqy.loop;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * @author yqy
 * @date 2022/8/3 16:19
 */
public class EventLoopTest {

    private static final Logger LOGGER = LogManager.getLogger();
    static final AtomicInteger port = new AtomicInteger(51234);
    InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.1", port.getAndIncrement());

    ServerSocketChannel server;
    SocketChannel client;
    SocketChannel accept;
    EventLoop eventLoop;
    EventLoopGroup group;

    @Before
    public void init() throws IOException {

        server = ServerSocketChannel.open();
        server.bind(serverAddress);
        server.socket().setReuseAddress(true);
        server.configureBlocking(false);

        client = SocketChannel.open();
        client.configureBlocking(false);

        group = new EventLoopGroup(1);
        eventLoop = group.newChild();


    }


    @Test
    public void registerWithAttachment() {
        AtomicInteger i = new AtomicInteger(0);
        eventLoop.registerWithAttachment(server, SelectionKey.OP_ACCEPT, () -> {
            try {
                LOGGER.info("accept event!");
                accept = server.accept();
                accept.configureBlocking(false);
                // 只能在主线程使用
//                assertTrue(true);
                i.incrementAndGet();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        eventLoop.registerWithAttachment(client, SelectionKey.OP_CONNECT, () -> {
            LOGGER.info("connect event!");
            SelectionKey key = eventLoop.getKey(client);
            key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
            try {
                boolean b = client.finishConnect();
                LOGGER.info("finished: {}", b);
            } catch (IOException e) {
                e.printStackTrace();
            }
            eventLoop.registerWithAttachment(client, SelectionKey.OP_READ, () -> {
                LOGGER.info("read event");
                ByteBuffer buffer = ByteBuffer.allocate(1);
                try {
                    client.read(buffer);
                    eventLoop.getKey(client).interestOps(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                i.incrementAndGet();
            });
            eventLoop.registerWithAttachment(accept, SelectionKey.OP_WRITE, () -> {
                LOGGER.info("write event");
                try {
                    accept.write(ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)));
                    eventLoop.getKey(accept).interestOps(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                i.incrementAndGet();
            });
            i.incrementAndGet();
        });
        connect();
        sleep(3000);
        assertEquals(4, i.get());

    }

    @Test
    public void registerWithIOTask() {
        final AtomicInteger wr = new AtomicInteger(0);
        final AtomicInteger ac = new AtomicInteger(0);
        final AtomicInteger att = new AtomicInteger(0);
        EventLoop.IOTask ioTask = new EventLoop.IOTask();
        ioTask.acceptTask = () -> {
            LOGGER.info("accept event");
            ac.incrementAndGet();
            try {
                accept = server.accept();
                accept.configureBlocking(false);
                EventLoop.IOTask io = new EventLoop.IOTask();
                io.readTask = () -> {
                    LOGGER.info("read event");
                    wr.incrementAndGet();
                    eventLoop.getKey(accept).interestOps(0);
                    ByteBuffer buffer = ByteBuffer.allocate(1);
                    try {
                        accept.read(buffer);
                        buffer.flip();
                        LOGGER.info("accept socket read: {}", new String(buffer.array(), buffer.position(), buffer.limit()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                };
                io.attachment = () -> {
                    LOGGER.info("any event");
                    att.incrementAndGet();
                };
                eventLoop.registerWithIOTask(accept, SelectionKey.OP_READ, io);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        eventLoop.registerWithIOTask(server, SelectionKey.OP_ACCEPT, ioTask);
        ioTask.writeTask = () -> {
            LOGGER.info("client write event");
            wr.incrementAndGet();
            ByteBuffer data = ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8));
            try {
                client.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            eventLoop.getKey(client).interestOps(0);
        };
        ioTask.connectTask = () -> {
            LOGGER.info("connect event");
            ac.incrementAndGet();
        };
        ioTask.attachment = () -> {
            LOGGER.info("any event");
            att.incrementAndGet();
        };
        eventLoop.registerWithIOTask(client, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE, ioTask);
        connect();
        sleep(3000);
        assertEquals(2, wr.get());
        assertEquals(2, ac.get());
        assertEquals(4, att.get());
    }

    @Test
    public void register() {
        assertTrue(true);
    }

    @Test
    public void attach() {
        try {
            eventLoop.attach(client, () -> {
            });
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }
        eventLoop.register(client);
        eventLoop.attach(client, () -> {
        });
        Object o = eventLoop.getKey(client).attachment();
        assertNotNull(o);
    }

    @Test
    public void attachTask() {
        try {
            eventLoop.attachTask(client,SelectionKey.OP_READ, () -> {
            });
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }
        final SelectionKey serverKey = eventLoop.register(server);
        final SelectionKey clientKey = eventLoop.register(client);
        final AtomicInteger ac = new AtomicInteger(0);
        final AtomicInteger wr = new AtomicInteger(0);
        eventLoop.attachTask(server, SelectionKey.OP_ACCEPT, () -> {
            LOGGER.info("accept event");
            ac.incrementAndGet();
            try {
                server.accept();
            } catch (IOException e) {
            }
        });
        eventLoop.attachTask(client, SelectionKey.OP_CONNECT, () -> {
            LOGGER.info("connect event");
            ac.incrementAndGet();
        });

        assertNotNull(((EventLoop.IOTask) clientKey.attachment()).connectTask);
        connect();
        sleep(3000);
        assertNotNull(serverKey.attachment());
        assertNotNull(clientKey.attachment());
        assertNotNull(((EventLoop.IOTask) serverKey.attachment()).acceptTask);
        // after connect() will be null
        assertNull(((EventLoop.IOTask) clientKey.attachment()).connectTask);
        assertNull(((EventLoop.IOTask) serverKey.attachment()).readTask);
        assertNull(((EventLoop.IOTask) serverKey.attachment()).writeTask);
        assertNull(((EventLoop.IOTask) serverKey.attachment()).connectTask);
        assertNull(((EventLoop.IOTask) serverKey.attachment()).attachment);
        assertNull(((EventLoop.IOTask) clientKey.attachment()).readTask);
        assertNull(((EventLoop.IOTask) clientKey.attachment()).writeTask);
        assertNull(((EventLoop.IOTask) clientKey.attachment()).connectTask);
        assertNull(((EventLoop.IOTask) clientKey.attachment()).attachment);
        assertEquals(2, ac.get());
    }

    /**
     * 由于eventloop是异步执行的，所以必须调用该方法等到事件触发
     * @param timeout
     */
    private void sleep(long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void connect() {
        try {
            client.connect(serverAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void detachTask() {
        final SelectionKey serverKey = eventLoop.register(server);
        final SelectionKey clientKey = eventLoop.register(client);
        final AtomicInteger ac = new AtomicInteger(0);
        final AtomicInteger wr = new AtomicInteger(0);
        eventLoop.attachTask(server, SelectionKey.OP_ACCEPT, () -> {});
        assertNotNull(serverKey.attachment());
        assertNotNull(((EventLoop.IOTask) serverKey.attachment()).acceptTask);
        try {
            eventLoop.detachTask(server, 0);
        } catch (Throwable t) {
            //
            assertEquals(IllegalArgumentException.class, t.getClass());
        }
        try {
            eventLoop.detachTask(server, SelectionKey.OP_READ);
        } catch (Throwable t) {
            assertTrue(t instanceof IllegalArgumentException);
        }
        eventLoop.detachTask(server, SelectionKey.OP_ACCEPT);
        assertNotNull(serverKey.attachment());
        assertNull(((EventLoop.IOTask) serverKey.attachment()).acceptTask);
        eventLoop.attachTask(server,SelectionKey.OP_ACCEPT, ()-> {
            LOGGER.info("accept event");
            ac.incrementAndGet();
            try {
                SocketChannel accept = server.accept();
                ByteBuffer data = ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8));
                accept.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        eventLoop.attachTask(client, SelectionKey.OP_CONNECT, ()-> {
            LOGGER.info("connect event");
            ac.incrementAndGet();
        });
        eventLoop.attachTask(client, SelectionKey.OP_READ, () -> {
            LOGGER.info("read event");
            wr.incrementAndGet();
            assertNotNull(((EventLoop.IOTask)clientKey.attachment()).readTask);
            if (wr.get() == 2) {
                eventLoop.detachTask(client, SelectionKey.OP_READ);
                assertNull(((EventLoop.IOTask) clientKey.attachment()).readTask);
            }
        });
        connect();
        sleep(3000);
        assertEquals(2, ac.get());
        assertEquals(2, wr.get());
    }
}