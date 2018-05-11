package com.github.zsh;

public abstract class ZkEvent {

    private final String _description;

    public ZkEvent(String description) {
        _description = description;
    }

    public abstract void run() throws Exception;

    @Override
    public String toString() {
        return "ZkEvent[" + _description + "]";
    }
}
