package scut.carson_ho.socket_carson.controller;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Author：mengyuan
 * Date  : 2017/6/2下午4:14
 * E-Mail:mengyuanzz@126.com
 * Desc  :
 */

public class KLSocketController {
    private Socket socket;


    private static KLSocketController ourInstance = new KLSocketController();

    public static KLSocketController getInstance() {
        if (ourInstance == null) {
            synchronized (new Object()) {
                if (ourInstance == null) {
                    ourInstance = new KLSocketController();
                }
            }
        }
        return ourInstance;
    }

    private KLSocketController() {
    }

    public void socketConnection(){
        // 利用线程池直接开启一个线程 & 执行该线程
        SocketThread.getInstance().startThread(new Runnable() {
            @Override
            public void run() {

                try {

                    // 创建Socket对象 & 指定服务端的IP 及 端口号
                    socket = new Socket("192.168.199.138", 9999);

                    // 判断客户端和服务器是否连接成功
                    System.out.println(socket.isConnected());

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    /**
     * 发送登录请求
     *
     * @param token 用户id
     * @param key   根据用户id生成的key {#link MyTestActivity}
     */
    public void socketLogin(String token, String key) {
        SocketThread.getInstance().startThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    /**
     * 发送心跳帧请求
     */
    public void socketHeart() {

    }

    /**
     * 停止socket连接
     */
    public void stopSocket() {
        if (socket == null || !socket.isConnected()) {
            socket = null;
            SocketThread.getInstance().stop();
            return;
        }
        try {
            socket.close();
            socket = null;
        } catch (IOException e) {
            socket = null;
        }
        SocketThread.getInstance().stop();
    }

    /**
     * 判断socket是否处于连接状态
     *
     * @return true为连接
     */
    public boolean socketIsConnection() {
        return socket != null && socket.isConnected();
    }
}
