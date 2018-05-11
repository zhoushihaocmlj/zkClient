package com.github.zsh.exception;

import org.apache.zookeeper.KeeperException;

public class ZkNodeExistsException extends ZkException {

    private static final long serialVersionUID = 1L;
    public ZkNodeExistsException() {
        super();
    }
    public ZkNodeExistsException(String message) {
        super(message);
    }
    public ZkNodeExistsException(KeeperException cause) {
        super(cause);
    }
    public ZkNodeExistsException(String message, KeeperException cause) {
        super(message, cause);
    }
}
