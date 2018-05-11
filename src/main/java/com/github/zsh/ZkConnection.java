package com.github.zsh;

import com.github.zsh.exception.ZkException;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ZkConnection {

    private ZooKeeper _zk = null;

    private final Lock _zookeeperLock = new ReentrantLock();

    private final String _servers;

    private final int _sessionTimeOut;

    private static final Method method;

    static {
        Method[] methods = ZooKeeper.class.getDeclaredMethods();
        Method m = null;
        for (Method method : methods) {
            if (method.getName().equals("multi")) {
                m = method;
                break;
            }
        }
        method = m;
    }

    /**
     * 创建一个zookeeper连接
     * @param zkServers      服务器名称
     * @param sessionTimeOut 会话超时时间
     */
    public ZkConnection(String zkServers, int sessionTimeOut) {
        _servers = zkServers;
        _sessionTimeOut = sessionTimeOut;
    }

    public void connect(Watcher watcher) {
        _zookeeperLock.lock();
        try{
            if(_zk != null){
                throw new IllegalStateException("zookeeper 客户端未启动");
            }
            try {
                _zk = new ZooKeeper(_servers, _sessionTimeOut, watcher);
            } catch (IOException e) {
                throw new ZkException("无法连接到服务器" + _servers, e);
            }
        }finally {
            _zookeeperLock.unlock();
        }
    }

    public void close() throws InterruptedException{
        _zookeeperLock.lock();
        try {
            if (_zk != null) {
                _zk.close();
                _zk = null;
            }
        } finally {
            _zookeeperLock.unlock();
        }
    }

    public String create(String path, byte[] data, CreateMode mode) throws KeeperException , InterruptedException{
        return _zk.create(path,data, ZooDefs.Ids.OPEN_ACL_UNSAFE,mode);
    }

    public void delete(String path) throws KeeperException , InterruptedException{
        _zk.delete(path,-1);
    }

    public boolean exists(String path, boolean watch) throws KeeperException,InterruptedException{
        return _zk.exists(path, watch) != null;
    }

    public List<String> getChildren(final String path, final boolean watch)throws KeeperException,InterruptedException{
        return _zk.getChildren(path,watch);
    }

    public byte[] readData(String path, Stat stat , boolean watch) throws KeeperException, InterruptedException{
        return _zk.getData(path,watch,stat);
    }

    public Stat writeData(String path, byte[] data, int version) throws KeeperException, InterruptedException {
        return _zk.setData(path, data, version);
    }

    public ZooKeeper.States getZookeeperState() {
        return _zk != null ? _zk.getState() : null;
    }

    public long getCreateTime(String path) throws KeeperException, InterruptedException {
        Stat stat = _zk.exists(path, false);
        if (stat != null) {
            return stat.getCtime();
        }
        return -1;
    }

    public String getServers() {
        return _servers;
    }

    public ZooKeeper getZooKeeper() {
        return _zk;
    }

    /**
     * @param ops 多重操作
     * @return 结果列表
     */
    public List<?> multi(Iterable<?> ops) {
        if (method == null) throw new UnsupportedOperationException("zookeeper 版本 3.4+以上才 支持多重操作");
        try {
            return (List<?>) method.invoke(_zk, ops);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new UnsupportedOperationException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
