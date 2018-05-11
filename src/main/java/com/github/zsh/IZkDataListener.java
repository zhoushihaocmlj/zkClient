package com.github.zsh;


/**
 * {@link IZkDataListener} 用于监听 {@link ZkClient} 指定路径下的节点数据变化.
 * <p>
 * 定时订阅,不能保证事件的丢失(详见 http://zookeeper.apache.org/doc/current/zookeeperProgrammers.html#ch_zkWatches).
 */
public interface IZkDataListener {
    void handleDataChange(String dataPath, byte[] data) throws Exception;

    void handleDataDeleted(String dataPath) throws Exception;
}
