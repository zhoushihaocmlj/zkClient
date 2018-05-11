package com.github.zsh.exception;

import org.apache.zookeeper.KeeperException;

public class ZkBadVersionException extends ZkException {
    private static final long serialVersionUID = 1L;
    public ZkBadVersionException() {
        super();
    }
    public ZkBadVersionException(String message) {
        super(message);
    }
    public ZkBadVersionException(KeeperException cause) {
        super(cause);
    }
    public ZkBadVersionException(String message, KeeperException cause) {
        super(message, cause);
    }
}
