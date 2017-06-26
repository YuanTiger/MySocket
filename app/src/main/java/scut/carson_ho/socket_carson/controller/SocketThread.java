package scut.carson_ho.socket_carson.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Author：mengyuan
 * Date  : 2017/6/2下午4:31
 * E-Mail:mengyuanzz@126.com
 * Desc  :
 */

public class SocketThread {
    private static SocketThread ourInstance = new SocketThread();

    private ExecutorService socketThread;

    public static SocketThread getInstance() {
        if (ourInstance == null) {
            synchronized (new Object()) {
                if (ourInstance == null) {
                    ourInstance = new SocketThread();
                }
            }
        }
        return ourInstance;
    }

    private SocketThread() {
        socketThread = Executors.newSingleThreadExecutor();
    }

    public void startThread(Runnable runnable){
        socketThread.execute(runnable);
    }

    public void stop() {
        if(socketThread == null){
            return;
        }
        socketThread.shutdown();
    }
}
