package com.github.zsh;

import org.apache.zookeeper.Watcher;

import java.util.List;

/**
 * 抽象监听类
 * @see IZkChildListener
 * @see IZkDataListener
 * @see IZkStateListener
 */
public abstract class AbstractListener implements IZkChildListener,IZkDataListener,IZkStateListener{
    @Override
    public void handleChildChange(String parentPath, List<String> currentChildren) {
    }

    @Override
    public void handleDataChange(String dataPath, byte[] data) {
    }

    @Override
    public void handleDataDeleted(String dataPath) {
    }

    @Override
    public void handleStateChanged(Watcher.Event.KeeperState state) {
    }

    @Override
    public void handleNewSession() {
    }

}
