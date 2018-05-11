package com.github.zsh.exception;

import org.apache.zookeeper.KeeperException;

public class ZkNoNodeException extends ZkException{
    private static final long serialVersionUID = 1L;
    public ZkNoNodeException() {
        super();
    }
    public ZkNoNodeException(String message) {
        super(message);
    }
    public ZkNoNodeException(KeeperException cause) {
        super(cause);
    }
    public ZkNoNodeException(String message, KeeperException cause) {
        super(message, cause);
    }
}
