package com.github.zsh;


import com.github.zsh.exception.*;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Zookeeper客户端
 * <p>
 * 线程安全
 * </p>
 */
public class ZkClient implements Watcher , IZkClient{

    private final static Logger LOG = LoggerFactory.getLogger(ZkClient.class);

    protected ZkConnection _connection;

    private final Map<String, Set<IZkChildListener>> _childListener = new ConcurrentHashMap<>();

    private final Map<String, Set<IZkDataListener>> _dataListener = new ConcurrentHashMap<>();

    private final Set<IZkStateListener> _stateListener = new CopyOnWriteArraySet<>();

    private volatile Event.KeeperState _currentState;

    private final ZkLock _zkEventLock = new ZkLock();

    private volatile boolean _shutdownTriggered;

    private ZkEventThread _eventThread;

    private Thread _zookeeperEventThread;

    /**
     * 创建一个ZkClient
     *
     * @param zkConnection      真实客户端
     * @param connectionTimeout 连接超时时间
     */
    public ZkClient(ZkConnection zkConnection, int connectionTimeout) {
        _connection = zkConnection;
        connect(connectionTimeout, this);
    }
    /**
     * 创建一个ZkClient
     *
     * @param connectString 连接字符串
     *                      如:"127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002"
     *                      或者创建根路径,如:"127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002/app/a"
     *
     * @param sessionTimeout    会话超时时间(毫秒)
     * @param connectionTimeout 连接超时时间(毫秒)
     */
    public ZkClient(String connectString, int sessionTimeout, int connectionTimeout) {
        this(new ZkConnection(connectString, sessionTimeout), connectionTimeout);
    }
    /**
     * 创建一个ZkClient
     *
     * @param connectString 连接字符串
     *                      如:"127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002"
     *                      或者创建根路径,如:"127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002/app/a"
     *
     * @param connectionTimeout 连接超时时间(毫秒)
     */
    public ZkClient(String connectString, int connectionTimeout) {
        this(connectString, DEFAULT_SESSION_TIMEOUT, connectionTimeout);
    }
    /**
     * 创建一个拥有默认连接超时时间和默认会话超时时间的ZkClient
     *
     * @param connectString 连接字符串
     *                      如:"127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002"
     *                      或者创建根路径,如:"127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002/app/a"
     *
     * @see IZkClient#DEFAULT_CONNECTION_TIMEOUT
     * @see IZkClient#DEFAULT_SESSION_TIMEOUT
     */
    public ZkClient(String connectString) {
        this(connectString, DEFAULT_CONNECTION_TIMEOUT);
    }

    @Override
    public synchronized void close() throws ZkInterruptedException{
        if (_eventThread == null) {
            return;
        }
        LOG.debug("关闭ZkClient!!");
        getEventLock().lock();
        try {
            setShutdownTrigger(true);
            _currentState = null;
            _eventThread.interrupt();
            _eventThread.join(2000);
            _connection.close();
            _eventThread = null;
        } catch (InterruptedException e) {
            throw new ZkInterruptedException(e);
        } finally {
            getEventLock().unlock();
        }
        LOG.debug("关闭ZkClient成功!");
    }
    /**
     * 返回一个所有zookeeper事件依赖的互斥锁. 所以任何zookeeper事件都需要先获取互斥锁,除非线程中断.
     * 所有等待锁的线程将被另一个事件通知.
     *
     * @return 互斥锁.
     */
    public ZkLock getEventLock() {
        return _zkEventLock;
    }

    private void setShutdownTrigger(boolean triggerState) {
        _shutdownTriggered = triggerState;
    }

    @Override
    public synchronized void connect(final long timeout, Watcher watcher) {
        if (_eventThread != null) {
            return;
        }
        boolean started = false;
        try {
            getEventLock().lockInterruptibly();
            setShutdownTrigger(false);
            _eventThread = new ZkEventThread(_connection.getServers());
            _eventThread.start();
            _connection.connect(watcher);

            LOG.debug("连接Zookeeper服务器超时时间为: " + timeout);
            if (!waitUntilConnected(timeout, TimeUnit.MILLISECONDS)) {
                throw new ZkTimeoutException(String.format(
                        "无法连接 zookeeper 服务器[%s], 在超时时间 %dms 内", _connection.getServers(), timeout));
            }
            started = true;
        } catch (InterruptedException e) {
            ZooKeeper.States state = _connection.getZookeeperState();
            throw new IllegalStateException("尚未连接zookeeper,目前状态是 " + state);
        } finally {
            getEventLock().unlock();
            if (!started) {
                close();
            }
        }
    }

    @Override
    public int countChildren(String path) {
        try {
            Stat stat = new Stat();
            this.readData(path, stat);
            return stat.getNumChildren();
        } catch (ZkNoNodeException e) {
            return -1;
        }
    }

    @Override
    public String create(String path, byte[] data, CreateMode mode) {
        if (path == null) {
            throw new NullPointerException("节点路径不能为空.");
        }
        final byte[] bytes = data;

        return retryUntilConnected(() -> _connection.create(path, bytes, mode));
    }

    @Override
    public void createEphemeral(String path) {
        create(path, null, CreateMode.EPHEMERAL);
    }

    @Override
    public void createEphemeral(String path, byte[] data) {
        create(path, data, CreateMode.EPHEMERAL);
    }

    @Override
    public String createEphemeralSequential(String path, byte[] data) {
        return create(path, data, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    @Override
    public void createPersistent(String path) {
        createPersistent(path, false);
    }

    @Override
    public void createPersistent(String path, boolean createParents) {
        try{
            create(path,null,CreateMode.PERSISTENT);
        }catch (ZkNodeExistsException e){
            if (!createParents) {
                throw e;
            }
        }catch (ZkNoNodeException e){
            if (!createParents) {
                throw e;
            }
            String parentDir = path.substring(0, path.lastIndexOf('/'));
            createPersistent(parentDir, createParents);
            createPersistent(path, createParents);
        }
    }

    @Override
    public void createPersistent(String path, byte[] data) {
        create(path, data, CreateMode.PERSISTENT);
    }

    @Override
    public String createPersistentSequential(String path, byte[] data) {
        return create(path, data, CreateMode.PERSISTENT_SEQUENTIAL);
    }

    @Override
    public boolean delete(String path) {
        try {
            retryUntilConnected((Callable<byte[]>) () -> {
                _connection.delete(path);
                return null;
            });

            return true;
        } catch (ZkNoNodeException e) {
            return false;
        }
    }

    @Override
    public boolean deleteRecursive(String path) {
        List<String> children;
        try {
            children = getChildren(path, false);
        } catch (ZkNoNodeException e) {
            return true;
        }
        if (children != null){
            for (String subPath : children) {
                if (!deleteRecursive(path + "/" + subPath)) {
                    return false;
                }
            }
        }

        return delete(path);
    }

    @Override
    public boolean exists(String path) {
        return exists(path, hasListeners(path));
    }

    private boolean exists(final String path, final boolean watch) {
        return retryUntilConnected(() -> _connection.exists(path, watch));
    }

    @Override
    public List<String> getChildren(String path) {
        return getChildren(path, hasListeners(path));
    }

    private List<String> getChildren(final String path, final boolean b) {
        try {
            return retryUntilConnected(() -> _connection.getChildren(path, b));
        } catch (ZkNoNodeException e) {
            return null;
        }
    }

    @Override
    public long getCreationTime(String path) {
        try {
            getEventLock().lockInterruptibly();
            return _connection.getCreateTime(path);
        } catch (KeeperException e) {
            throw ZkException.create(e);
        } catch (InterruptedException e) {
            throw new ZkInterruptedException(e);
        } finally {
            getEventLock().unlock();
        }
    }

    @Override
    public int numberOfListeners() {
        int listeners = 0;
        for (Set<IZkChildListener> childListeners : _childListener.values()) {
            listeners += childListeners.size();
        }
        for (Set<IZkDataListener> dataListeners : _dataListener.values()) {
            listeners += dataListeners.size();
        }
        listeners += _stateListener.size();

        return listeners;
    }

    @Override
    public byte[] readData(String path) {
        return readData(path, false);
    }

    @Override
    public byte[] readData(String path, boolean returnNullIfPathNotExists) {
        byte[] data = null;
        try {
            data = readData(path, null);
        } catch (ZkNoNodeException e) {
            if (!returnNullIfPathNotExists) {
                throw e;
            }
        }
        return data;
    }

    @Override
    public byte[] readData(String path, Stat stat) {
        return readData(path, stat, hasListeners(path));
    }

    private boolean hasListeners(String path) {
        Set<IZkDataListener> dataListeners = _dataListener.get(path);
        if (dataListeners != null && dataListeners.size() > 0) {
            return true;
        }
        Set<IZkChildListener> childListeners = _childListener.get(path);
        return childListeners != null && childListeners.size() > 0;
    }
    private byte[] readData(final String path, final Stat stat, final boolean watch) {
        return retryUntilConnected(() -> _connection.readData(path, stat, watch));
    }

    /**
     * @param callable 回调对象
     * @param <E> 泛型
     * @return 回调函数的结果
     * @throws ZkInterruptedException   连接与重连接中断
     * @throws IllegalArgumentException 非zookeeper事件线程
     * @throws ZkException              zookeeper异常
     * @throws RuntimeException         运行时异常
     */
    public <E> E retryUntilConnected(Callable<E> callable) {
        if (_zookeeperEventThread != null && Thread.currentThread() == _zookeeperEventThread) {
            throw new IllegalArgumentException("不能在ZooKeeper事件线程中完成.");
        }
        while (true) {
            try {
                return callable.call();
            } catch (KeeperException.ConnectionLossException | KeeperException.SessionExpiredException e) {
                //线程让步
                Thread.yield();
                waitUntilConnected();
            } catch (KeeperException e) {
                throw ZkException.create(e);
            } catch (InterruptedException e) {
                throw new ZkInterruptedException(e);
            } catch (Exception e) {
                throw ZkClientUtils.convertToRuntimeException(e);
            }
        }
    }

    @Override
    public List<String> subscribeChildChanges(String path, IZkChildListener listener) {
        synchronized (_childListener) {
            Set<IZkChildListener> listeners = _childListener.get(path);
            if (listeners == null) {
                listeners = new CopyOnWriteArraySet<>();
                _childListener.put(path, listeners);
            }
            listeners.add(listener);
        }
        return watchForChilds(path);
    }

    private List<String> watchForChilds(final String path) {
        if (_zookeeperEventThread != null && Thread.currentThread() == _zookeeperEventThread) {
            throw new IllegalArgumentException("不能在ZooKeeper事件线程中完成.");
        }
        return retryUntilConnected(() -> {
            exists(path, true);
            try {
                return getChildren(path, true);
            } catch (ZkNoNodeException e) {
                //todo
            }
            return null;
        });
    }

    @Override
    public void subscribeDataChanges(String path, IZkDataListener listener) {
        Set<IZkDataListener> listeners;
        synchronized (_dataListener) {
            listeners = _dataListener.get(path);
            if (listeners == null) {
                listeners = new CopyOnWriteArraySet<>();
                _dataListener.put(path, listeners);
            }
            listeners.add(listener);
        }
        watchForData(path);
    }
    public void watchForData(final String path) {
        retryUntilConnected(() -> {
            _connection.exists(path, true);
            return null;
        });
    }
    @Override
    public void subscribeStateChanges(IZkStateListener listener) {
        synchronized (_stateListener) {
            _stateListener.add(listener);
        }
    }

    @Override
    public void unsubscribeAll() {
        synchronized (_childListener) {
            _childListener.clear();
        }
        synchronized (_dataListener) {
            _dataListener.clear();
        }
        synchronized (_stateListener) {
            _stateListener.clear();
        }
    }

    @Override
    public void unsubscribeChildChanges(String path, IZkChildListener childListener) {
        synchronized (_childListener) {
            final Set<IZkChildListener> listeners = _childListener.get(path);
            if (listeners != null) {
                listeners.remove(childListener);
            }
        }
    }

    @Override
    public void unsubscribeDataChanges(String path, IZkDataListener dataListener) {
        synchronized (_dataListener) {
            final Set<IZkDataListener> listeners = _dataListener.get(path);
            if (listeners != null) {
                listeners.remove(dataListener);
            }
            if (listeners == null || listeners.isEmpty()) {
                _dataListener.remove(path);
            }
        }
    }

    @Override
    public void unsubscribeStateChanges(IZkStateListener stateListener) {
        synchronized (_stateListener) {
            _stateListener.remove(stateListener);
        }
    }

    @Override
    public void cas(String path, DataUpdater updater) {
        Stat stat = new Stat();
        boolean retry;
        do {
            retry = false;
            try {
                byte[] oldData = readData(path, stat);
                byte[] newData = updater.update(oldData);
                writeData(path, newData, stat.getVersion());
            } catch (ZkBadVersionException e) {
                retry = true;
            }
        } while (retry);
    }

    @Override
    public boolean waitForKeeperState(Event.KeeperState keeperState, long time, TimeUnit timeUnit) {
        if (_zookeeperEventThread != null && Thread.currentThread() == _zookeeperEventThread) {
            throw new IllegalArgumentException("不能在ZooKeeper事件线程中完成.");
        }
        Date timeout = new Date(System.currentTimeMillis() + timeUnit.toMillis(time));
        acquireEventLock();
        try {
            boolean stillWaiting = true;
            while (_currentState != keeperState) {
                if (!stillWaiting) {
                    return false;
                }
                stillWaiting = getEventLock().getStateChangedCondition().awaitUntil(timeout);
            }
            return true;
        } catch (InterruptedException e) {
            throw new ZkInterruptedException(e);
        } finally {
            getEventLock().unlock();
        }
    }

    private void acquireEventLock() {
        try {
            getEventLock().lockInterruptibly();
        } catch (InterruptedException e) {
            throw new ZkInterruptedException(e);
        }
    }

    @Override
    public boolean waitUntilConnected() throws ZkInterruptedException {
        return waitUntilConnected(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean waitUntilConnected(long time, TimeUnit timeUnit) {
        return waitForKeeperState(Event.KeeperState.SyncConnected, time, timeUnit);
    }

    @Override
    public boolean waitUntilExists(String path, TimeUnit timeUnit, long time) {
        Date timeout = new Date(System.currentTimeMillis() + timeUnit.toMillis(time));
        if (exists(path)) {
            return true;
        }
        acquireEventLock();
        try {
            while (!exists(path, true)) {
                boolean gotSignal = getEventLock().getZNodeEventCondition().awaitUntil(timeout);
                if (!gotSignal) {
                    return false;
                }
            }
            return true;
        } catch (InterruptedException e) {
            throw new ZkInterruptedException(e);
        } finally {
            getEventLock().unlock();
        }
    }

    @Override
    public Stat writeData(String path, byte[] data) {
        return writeData(path, data, -1);
    }

    @Override
    public Stat writeData(String path, byte[] data, int expectedVersion) {
        return retryUntilConnected(() -> _connection.writeData(path, data, expectedVersion));
    }

    @Override
    public List<?> multi(Iterable<?> ops) {
        return retryUntilConnected(() -> _connection.multi(ops));
    }

    @Override
    public ZooKeeper getZooKeeper() {
        return _connection != null ? _connection.getZooKeeper() : null;
    }

    @Override
    public boolean isConnected() {
        return _currentState == Event.KeeperState.SyncConnected;
    }

    @Override
    public void process(WatchedEvent event) {
        _zookeeperEventThread = Thread.currentThread();

        boolean stateChanged = event.getPath() == null;
        boolean znodeChanged = event.getPath() != null;
        boolean dataChanged = event.getType() == Event.EventType.NodeDataChanged ||
                event.getType() == Event.EventType.NodeDeleted ||
                event.getType() == Event.EventType.NodeCreated ||
                event.getType() == Event.EventType.NodeChildrenChanged;

        getEventLock().lock();
        try {

            if (getShutdownTrigger()) {
                return;
            }
            if (stateChanged) {
                processStateChanged(event);
            }
            if (dataChanged) {
                processDataOrChildChange(event);
            }
        } finally {
            if (stateChanged) {
                getEventLock().getStateChangedCondition().signalAll();

                if (event.getState() == Event.KeeperState.Expired) {
                    getEventLock().getZNodeEventCondition().signalAll();
                    getEventLock().getDataChangedCondition().signalAll();
                    fireAllEvents();
                }
            }
            if (znodeChanged) {
                getEventLock().getZNodeEventCondition().signalAll();
            }
            if (dataChanged) {
                getEventLock().getDataChangedCondition().signalAll();
            }
            getEventLock().unlock();
        }
    }
    private void processStateChanged(WatchedEvent event) {
        setCurrentState(event.getState());
        if (getShutdownTrigger()) {
            return;
        }
        try {
            fireStateChangedEvent(event.getState());

            if (event.getState() == Event.KeeperState.Expired) {
                reconnect();
                fireNewSessionEvents();
            }
        } catch (final Exception e) {
            throw new RuntimeException("重启ZkClient异常", e);
        }
    }

    private void setCurrentState(Event.KeeperState currentState) {
        getEventLock().lock();
        try {
            _currentState = currentState;
        } finally {
            getEventLock().unlock();
        }
    }

    private boolean getShutdownTrigger() {
        return _shutdownTriggered;
    }

    private void fireStateChangedEvent(final Event.KeeperState state) {
        for (final IZkStateListener stateListener : _stateListener) {
            _eventThread.send(new ZkEvent("服务器状态变化为" + state + ", 告知 " + stateListener) {

                @Override
                public void run() throws Exception {
                    stateListener.handleStateChanged(state);
                }
            });
        }
    }

    private void reconnect() {
        getEventLock().lock();
        try {
            _connection.close();
            _connection.connect(this);
        } catch (InterruptedException e) {
            throw new ZkInterruptedException(e);
        } finally {
            getEventLock().unlock();
        }
    }

    private void fireAllEvents() {
        for (Map.Entry<String, Set<IZkChildListener>> entry : _childListener.entrySet()) {
            fireChildChangedEvents(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Set<IZkDataListener>> entry : _dataListener.entrySet()) {
            fireDataChangedEvents(entry.getKey(), entry.getValue());
        }
    }

    private void fireChildChangedEvents(final String path, Set<IZkChildListener> childListeners) {
        try {
            // reinstall the watch
            for (final IZkChildListener listener : childListeners) {
                _eventThread.send(new ZkEvent("路径" + path + " 下的节点变化告知" + listener) {

                    @Override
                    public void run() throws Exception {
                        try {
                            exists(path);
                            List<String> children = getChildren(path);
                            listener.handleChildChange(path, children);
                        } catch (ZkNoNodeException e) {
                            listener.handleChildChange(path, null);
                        }
                    }
                });
            }
        } catch (Exception e) {
            LOG.error("无法获取子节点 ", e);
        }
    }

    private void fireDataChangedEvents(final String path, Set<IZkDataListener> listeners) {
        for (final IZkDataListener listener : listeners) {
            _eventThread.send(new ZkEvent("路径 " + path + " 下节点数据变化告知 " + listener) {

                @Override
                public void run() throws Exception {
                    // reinstall watch
                    exists(path, true);
                    try {
                        byte[] data = readData(path, null, true);
                        listener.handleDataChange(path, data);
                    } catch (ZkNoNodeException e) {
                        listener.handleDataDeleted(path);
                    }
                }
            });
        }
    }


    private void processDataOrChildChange(WatchedEvent event) {
        final String path = event.getPath();

        if (event.getType() == Event.EventType.NodeChildrenChanged ||
                event.getType() == Event.EventType.NodeCreated ||
                event.getType() == Event.EventType.NodeDeleted) {
            Set<IZkChildListener> childListeners = _childListener.get(path);
            if (childListeners != null && !childListeners.isEmpty()) {
                fireChildChangedEvents(path, childListeners);
            }
        }

        if (event.getType() == Event.EventType.NodeDataChanged ||
                event.getType() == Event.EventType.NodeDeleted ||
                event.getType() == Event.EventType.NodeCreated) {
            Set<IZkDataListener> listeners = _dataListener.get(path);
            if (listeners != null && !listeners.isEmpty()) {
                fireDataChangedEvents(event.getPath(), listeners);
            }
        }
    }

    private void fireNewSessionEvents() {
        for (final IZkStateListener stateListener : _stateListener) {
            _eventThread.send(new ZkEvent("新的会话事件告知" + stateListener) {

                @Override
                public void run() throws Exception {
                    stateListener.handleNewSession();
                }
            });
        }
    }
}
