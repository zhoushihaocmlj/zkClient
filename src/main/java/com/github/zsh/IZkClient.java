package com.github.zsh;


import com.github.zsh.exception.*;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * zk客户端封装
 */
public interface IZkClient extends Closeable{

    /**
     * 连接超时时间
     */
    int DEFAULT_CONNECTION_TIMEOUT = 10000;

    /**
     * 会话超时时间
     */
    int DEFAULT_SESSION_TIMEOUT = 30000;
    /**
     * 关闭
     *
     * @throws ZkInterruptedException 如果线程中断
     */
    void close();
    /**
     * 连接
     * @param timeout 最大等待时间
     * @param watcher 监控点
     * @throws ZkInterruptedException 连接超时由于线程中断
     * @throws ZkTimeoutException 连接超时
     * @throws IllegalStateException 连接超时由于线程中断
     */
    void connect(final long timeout, Watcher watcher);
    /**
     * 指定路径下的子节点个数
     *
     * @param path 路径
     * @return 0或者子节点个数
     */
    int countChildren(String path);
    /**
     * 创建节点
     *
     * @param path 路径
     * @param data 节点数据
     * @param mode 节点类型枚举 {@link CreateMode}
     * @return 创建路径
     * @throws IllegalArgumentException 非zookeeper调用就抛出异常
     * @throws ZkException              未知异常
     */
    String create(final String path, byte[] data, final CreateMode mode);
    /**
     * 创建一个没有数据的临时节点
     *
     * @param path 路径
     * @throws ZkInterruptedException   连接或者重连接中断
     * @throws IllegalArgumentException 非zookeeper调用就抛出异常
     * @throws ZkException              未知异常
     */
    void createEphemeral(final String path);
    /**
     * 创建一个临时节点
     *
     * @param path 节点路径
     * @param data 节点数据
     * @throws ZkInterruptedException   连接或者重连接中断
     * @throws IllegalArgumentException 非zookeeper调用就抛出异常
     * @throws ZkException              未知异常
     */
    void createEphemeral(final String path, final byte[] data);
    /**
     * 创建一个临时有序的节点
     *
     * @param path 节点路径
     * @param data 节点数据
     * @return 创建路径
     * @throws ZkInterruptedException   连接或者重连接中断
     * @throws IllegalArgumentException 非zookeeper调用就抛出异常
     * @throws ZkException              未知异常
     */
    String createEphemeralSequential(final String path, final byte[] data);
    /**
     * 创建一个没有数据的永久节点
     *
     * @param path 节点路径
     * @throws ZkNodeExistsException    如果节点已存在
     * @throws ZkNoNodeException        如果父节点不存在
     * @throws ZkInterruptedException   连接或者重连接中断
     * @throws IllegalArgumentException 非zookeeper调用就抛出异常
     * @throws ZkException              未知异常
     * @throws RuntimeException         其他异常
     * @see #createPersistent(String, boolean)
     */
    void createPersistent(String path);
    /**
     * 创建一个没有数据的永久节点
     * <p>
     * 如果创建父节点, 抛出 {@link ZkNodeExistsException} 或者 {@link com.github.zsh.exception.ZkNoNodeException}
     * </p>
     *
     * @param path          节点路径
     * @param createParents 是否创建父节点
     * @throws ZkInterruptedException   连接或者重连接中断
     * @throws IllegalArgumentException 非zookeeper调用就抛出异常
     * @throws ZkException              未知异常
     * @throws RuntimeException         其他异常
     */
    void createPersistent(String path, boolean createParents);
    /**
     * 创建一个永久节点
     *
     * @param path 节点路径
     * @param data 节点数据
     * @throws ZkInterruptedException   连接或者重连接中断
     * @throws IllegalArgumentException 非zookeeper调用就抛出异常
     * @throws ZkException              未知异常
     * @throws RuntimeException         其他异常
     */
    void createPersistent(String path, byte[] data);
    /**
     * 创建一个有序的永久节点
     *
     * @param path 节点路径
     * @param data 节点数据
     * @return 创建路径
     * @throws ZkInterruptedException   连接或者重连接中断
     * @throws IllegalArgumentException 非zookeeper调用就抛出异常
     * @throws ZkException              未知异常
     * @throws RuntimeException         其他异常
     */
    String createPersistentSequential(String path, byte[] data);
    /**
     * 删除一个节点
     *
     * @param path 节点路径
     * @return 成功或者失败
     */
    boolean delete(final String path);
    /**
     * 递归删除一个节点
     *
     * @param path 节点路径
     * @return 成功或者失败
     */
    boolean deleteRecursive(String path);
    /**
     * 检测一个节点是否存在
     *
     * @param path 节点路径
     * @return 成功或者失败
     */
    boolean exists(final String path);
    /**
     * 获取节点的子节点
     *
     * @param path 节点路径
     * @return 空List或者子节点名称
     */
    List<String> getChildren(String path);
    /**
     * 获取节点创建时间戳
     *
     * @param path 节点路口
     * @return -1或者时间戳
     */
    long getCreationTime(String path);
    /**
     * 此连接的所有监控点个数
     *
     * @return 监控点个数
     */
    int numberOfListeners();
    /**
     * 读取节点数据
     *
     * @param path 节点路径
     * @return 节点数据
     * @throws com.github.zsh.exception.ZkNoNodeException 节点不存在
     * @see #readData(String, boolean)
     */
    byte[] readData(String path);
    /**
     * 读取节点数据
     *
     * @param path 节点路径
     * @param returnNullIfPathNotExists true不抛出 {@link com.github.zsh.exception.ZkNoNodeException}
     * @return 节点数据
     */
    byte[] readData(String path, boolean returnNullIfPathNotExists);
    /**
     * 读取节点数据和元数据信息
     *
     * @param path 节点路径
     * @param stat 元数据信息
     * @return 节点数据
     * @see #readData(String, boolean)
     */
    byte[] readData(String path, Stat stat);
    /**
     * 订阅子节点的变化
     *
     * @param path     节点路径
     * @param listener 监控点
     * @return 子节点列表
     * @see IZkChildListener
     */
    List<String> subscribeChildChanges(String path, IZkChildListener listener);
    /**
     * 订阅节点的数据变化
     *
     * @param path     节点路径
     * @param listener 监控点
     * @see IZkDataListener
     */
    void subscribeDataChanges(String path, IZkDataListener listener);
    /**
     * 订阅连接状态
     *
     * @param listener 监控点
     * @see IZkStateListener
     */
    void subscribeStateChanges(IZkStateListener listener);
    /**
     * 取消所有监控点
     */
    void unsubscribeAll();
    /**
     * 取消子节点监控
     *
     * @param path          节点路径
     * @param childListener 监控点
     */
    void unsubscribeChildChanges(String path, IZkChildListener childListener);
    /**
     * 取消数据变化的监控
     *
     * @param path         节点路径
     * @param dataListener 监控点
     */
    void unsubscribeDataChanges(String path, IZkDataListener dataListener);
    /**
     * 取消连接状态监控点
     *
     * @param stateListener 监控点
     */
    void unsubscribeStateChanges(IZkStateListener stateListener);
    /**
     * 更新已存在的节点. 传送的目前节点内容为{@link DataUpdater} 并且返回新的节点内容. 如果没有服务器修改给定
     * 的节点内容,将会写回zookeeper. 在zookeeper修改成功之前不停的发送并发请求.
     *
     * @param path    节点路径
     * @param updater 更新操作
     */
    void cas(String path, DataUpdater updater);
    /**
     * 等待zookeeper状态
     *
     * @param keeperState 状态
     * @param time        时间
     * @param timeUnit    时间单位
     * @return 如果状态为keeperState则返回true
     */
    boolean waitForKeeperState(Watcher.Event.KeeperState keeperState, long time, TimeUnit timeUnit);
    /**
     * 等待连接状态
     * <pre>
     *     waitForKeeperState(KeeperState.SyncConnected, Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
     * </pre>
     *
     * @return 如果客户端连上服务器则返回true
     * @throws ZkInterruptedException 请求中断
     * @see #waitForKeeperState(org.apache.zookeeper.Watcher.Event.KeeperState, long, java.util.concurrent.TimeUnit)
     */
    boolean waitUntilConnected() throws ZkInterruptedException;
    /**
     * 等待连接状态
     *
     * @param time     超时时间
     * @param timeUnit 时间单位
     * @return 如果客户端连上服务器则返回true
     */
    boolean waitUntilConnected(long time, TimeUnit timeUnit);
    /**
     * 等待节点存在
     *
     * @param path     节点路径
     * @param timeUnit 时间单位
     * @param time     时间
     * @return 如果节点存在返回true
     */
    boolean waitUntilExists(String path, TimeUnit timeUnit, long time);
    /**
     * 向指定节点写入数据
     *
     * @param path 节点路径
     * @param data 写入数据
     * @return 节点的元数据
     */
    Stat writeData(String path, byte[] data);
    /**
     * 向指定节点写入数据
     *
     * @param path            节点路径
     * @param data            写入数据
     * @param expectedVersion 预期版本
     * @return 节点的元数据
     * @see #cas(String, com.github.zsh.IZkClient.DataUpdater)
     */
    Stat writeData(String path, byte[] data, int expectedVersion);
    /**
     * 多个操作在 zookeeper 3.4.x
     *
     * @param ops 操作
     * @return op 结果
     * @see org.apache.zookeeper.ZooKeeper#multi(Iterable)
     * @see org.apache.zookeeper.Op
     * @see org.apache.zookeeper.OpResult
     */
    List<?> multi(Iterable<?> ops);
    /**
     * 获取zookeeper
     *
     * @return zookeeper
     */
    ZooKeeper getZooKeeper();
    /**
     * 检测zookeeper连接状态
     * @return 如果连接则返回true
     */
    boolean isConnected();
    /**
     * 原子操作
     */
    interface DataUpdater {

        /**
         * 更新节点数据
         *
         * @param currentData 当前节点内容
         * @return 写回zookeeper的新的数据
         */
        public byte[] update(byte[] currentData);

    }
}
