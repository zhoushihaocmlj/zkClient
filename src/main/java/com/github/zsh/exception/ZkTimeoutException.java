package com.github.zsh.exception;

public class ZkTimeoutException extends ZkException {

    private static final long serialVersionUID = 1L;
    public ZkTimeoutException() {
        super();
    }
    public ZkTimeoutException(String message) {
        super(message);
    }
    public ZkTimeoutException(Throwable cause) {
        super(cause);
    }
    public ZkTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
