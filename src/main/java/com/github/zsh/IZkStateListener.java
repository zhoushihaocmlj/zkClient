package com.github.zsh;

import org.apache.zookeeper.Watcher;

public interface IZkStateListener {

    /**
     * 当zk连接状态变化时的回调函数
     *
     * @param state 新状态
     * @throws Exception 其他异常
     */
    void handleStateChanged(Watcher.Event.KeeperState state) throws Exception;

    /**
     * 当会话失效且新的会话创建时的回调函数. 必须重新创建临时节点.
     *
     * @throws Exception 其他异常
     */
    void handleNewSession() throws Exception;
}
