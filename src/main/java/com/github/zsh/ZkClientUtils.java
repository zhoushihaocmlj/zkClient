package com.github.zsh;

import java.io.IOException;
import java.net.*;

public class ZkClientUtils {

    private ZkClientUtils() {}

    public enum ZkVersion {
        V33, V34
    }

    public static final ZkVersion zkVersion;

    static {
        ZkVersion version = null;
        try {
            Class.forName("org.apache.zookeeper.OpResult");
            version = ZkVersion.V34;
        }
        catch (ClassNotFoundException e) {
            version = ZkVersion.V33;
        }
        finally {
            zkVersion = version;
        }
    }

    public static RuntimeException convertToRuntimeException(Throwable e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        retainInterruptFlag(e);
        return new RuntimeException(e);
    }

    /**
     * 设置一个中断标志如果捕获到{@link InterruptedException}. 捕获其他异常清除中断标志.
     *
     * @param catchedException 捕获的异常
     */
    public static void retainInterruptFlag(Throwable catchedException) {
        if (catchedException instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    public static String leadingZeros(long number, int numberOfLeadingZeros) {
        return String.format("%0" + numberOfLeadingZeros + "d", number);
    }

    public static boolean isPortFree(int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("localhost", port), 200);
            socket.close();
            return false;
        }
        catch (SocketTimeoutException e) {
            return true;
        }
        catch (ConnectException e) {
            return true;
        }
        catch (SocketException e) {
            if (e.getMessage().equals("Connection reset by peer")) {
                return true;
            }
            throw new RuntimeException(e);
        }
        catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
