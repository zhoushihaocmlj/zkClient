package com.github.zsh;

import com.github.zsh.exception.ZkException;
import com.github.zsh.exception.ZkInterruptedException;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;


public class ZkServer {
    private static final Logger LOG = LoggerFactory.getLogger(ZkServer.class);

    public static final int DEFAULT_PORT = 2181;

    public static final int DEFAULT_TICK_TIME = 5000;

    public static final int DEFAULT_MIN_SESSION_TIMEOUT = 2 * DEFAULT_TICK_TIME;

    private final String _dataDir;

    private final String _logDir;

    private ZooKeeperServer _zk;

    private ServerCnxnFactory _nioFactory;

    private ZkClient _zkClient;

    private final int _port;

    private final int _tickTime;

    private final int _minSessionTimeout;

    public ZkServer(String dataDir, String logDir) {
        this(dataDir, logDir, DEFAULT_PORT);
    }

    public ZkServer(String dataDir, String logDir, int port) {
        this(dataDir, logDir, port, DEFAULT_TICK_TIME);
    }

    public ZkServer(String dataDir, String logDir, int port, int tickTime) {
        this(dataDir, logDir, port, tickTime, DEFAULT_MIN_SESSION_TIMEOUT);
    }

    public ZkServer(String dataDir, String logDir, int port, int tickTime, int minSessionTimeout) {
        _dataDir = dataDir;
        _logDir = logDir;
        _port = port;
        _tickTime = tickTime;
        _minSessionTimeout = minSessionTimeout;
    }

    public int getPort() {
        return _port;
    }

    @PostConstruct
    public void start() {
        startZkServer();
        _zkClient = new ZkClient("localhost:" + _port, 10000);
    }

    private void startZkServer() {
        final int port = _port;
        if (ZkClientUtils.isPortFree(port)) {
            final File dataDir = new File(_dataDir);
            final File dataLogDir = new File(_logDir);
            dataDir.mkdirs();
            dataLogDir.mkdirs();

            LOG.info("启动单机zookeeper服务");
            LOG.info("数据目录: " + dataDir.getAbsolutePath());
            LOG.info("数据日志目录: " + dataLogDir.getAbsolutePath());
            startSingleZkServer(_tickTime, dataDir, dataLogDir, port);
        } else {
            throw new IllegalStateException("Zookeeper port " + port + " 被占用");
        }
    }

    private void startSingleZkServer(final int tickTime, final File dataDir, final File dataLogDir, final int port) {
        try {
            _zk = new ZooKeeperServer(dataDir, dataLogDir, tickTime);
            _zk.setMinSessionTimeout(_minSessionTimeout);
            _nioFactory = ServerCnxnFactory.createFactory(port, 60);
            _nioFactory.startup(_zk);
        } catch (IOException e) {
            throw new ZkException("启动单机ZooKeeper服务失败.", e);
        } catch (InterruptedException e) {
            throw new ZkInterruptedException(e);
        }
    }

    @PreDestroy
    public void shutdown() {
        ZooKeeperServer zk = _zk;
        if (zk == null) {
            LOG.warn("重复关闭");
            return;
        }else {
            _zk = null;
        }
        LOG.info("关闭ZkServer...");
        try {
            _zkClient.close();
        } catch (ZkException e) {
            LOG.warn("关闭zkClient: " + e.getClass().getName()+" 失败");
        }
        if (_nioFactory != null) {
            _nioFactory.shutdown();
            try {
                _nioFactory.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            _nioFactory = null;
        }
        zk.shutdown();
        if (zk.getZKDatabase() != null) {
            try {
                zk.getZKDatabase().close();
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        LOG.info("关闭ZkServer成功");
    }

    public ZkClient getZkClient() {
        return _zkClient;
    }
}
