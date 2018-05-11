package com.github.zsh;


import java.util.List;

/**
 * {@link IZkChildListener} 用于监听{@link ZkClient} 指定路径下的节点变化.
 * <p>
 * 定时订阅,不能保证事件的丢失(详见 http://zookeeper.apache.org/doc/current/zookeeperProgrammers.html#ch_zkWatches).
 */
public interface IZkChildListener {


    /**
     * 当指定路径的节点产生变化时的回调函数
     * <p>
     * 注意: 当父节点创建时,监听将被触发.
     * </p>
     *
     * @param parentPath      父路径
     * @param currentChildren 节点列表
     * @throws Exception      其他异常
     */
    void handleChildChange(String parentPath, List<String> currentChildren) throws Exception;
}
