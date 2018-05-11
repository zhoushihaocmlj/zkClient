package com.github.zsh;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ZkLock extends ReentrantLock {
    private static final long serialVersionUID = 1L;

    private Condition _dataChangedCondition = newCondition();

    private Condition _stateChangedCondition = newCondition();

    private Condition _zNodeEventCondition = newCondition();

    /**
     * 当节点与子节点发生变化时产生.
     *
     * @return the condition.
     */
    public Condition getDataChangedCondition() {
        return _dataChangedCondition;
    }
    /**
     * 当连接状态变化时产生.
     *
     * @return the condition.
     */
    public Condition getStateChangedCondition() {
        return _stateChangedCondition;
    }
    /**
     * 当收到zk事件时产生.
     *
     * @return the condition.
     */
    public Condition getZNodeEventCondition() {
        return _zNodeEventCondition;
    }
}
