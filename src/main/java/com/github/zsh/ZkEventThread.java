package com.github.zsh;

import com.github.zsh.exception.ZkInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 当事件发生时,所有的监听被通知且注册在{@link ZkClient}.防止死锁的发生. {@link ZkClient} 从{@link org.apache.zookeeper.ZooKeeper}
 * 拉取信息,以通知 {@link ZkLock} 条件. 重复使用{@link org.apache.zookeeper.ZooKeeper}
 * 事件线程用来通知{@link ZkClient}监听, 当监听停止的时候,zkClient停止从 {@link org.apache.zookeeper.ZooKeeper}接收信息
 * (因为在等待其他通知). {@link ZkClient}的连接状态并非一直不变.
 */
public class ZkEventThread extends Thread{
    private static final Logger LOG = LoggerFactory.getLogger(ZkEventThread.class);

    private final BlockingQueue<ZkEvent> _events = new LinkedBlockingQueue<>();

    private static final AtomicInteger _eventId = new AtomicInteger(0);

    private volatile boolean shutdown = false;
    ZkEventThread(String name) {
        setDaemon(true);
        setName("ZkClient-EventThread-" + getId() + "-" + name);
    }

    @Override
    public void run(){
        LOG.info("启动ZkClient事件线程.");
        try {
            while (!isShutdown()) {
                ZkEvent zkEvent = _events.take();
                int eventId = _eventId.incrementAndGet();
                LOG.debug("发送事件# " + eventId + " " + zkEvent);
                try {
                    zkEvent.run();
                } catch (InterruptedException | ZkInterruptedException e) {
                    shutdown();
                } catch (Throwable e) {
                    LOG.error("执行事件#{}异常", zkEvent, e);
                }
                LOG.debug("发送事件# " + eventId + " 成功");
            }

        }catch(InterruptedException e){
            LOG.info("ZkClient线程终止");
        }
    }

    public boolean isShutdown() {
        return shutdown || isInterrupted();
    }

    public void shutdown() {
        this.shutdown = true;
        this.interrupt();
    }

    public void send(ZkEvent event) {
        if (!isShutdown()) {
            LOG.debug("New event: " + event);
            _events.add(event);
        }
    }
}
