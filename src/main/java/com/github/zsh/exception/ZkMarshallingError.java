package com.github.zsh.exception;

public class ZkMarshallingError extends ZkException {

    private static final long serialVersionUID = 1L;
    public ZkMarshallingError() {
        super();
    }
    public ZkMarshallingError(String message) {
        super(message);
    }
    public ZkMarshallingError(Throwable cause) {
        super(cause);
    }
    public ZkMarshallingError(String message, Throwable cause) {
        super(message, cause);
    }
}
