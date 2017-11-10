package com.vilyever.socketclient.helper;

import android.support.annotation.NonNull;

import com.vilyever.socketclient.SocketClient;

/**
 * SocketStateChangeCallback
 * Created by vilyever on 2016/5/30.
 * Feature: Socket状态监听
 */
public interface SocketStateChangeCallback {
    //连接成功
    void onConnected(SocketClient client);
    //连接断开
    void onDisconnect(SocketClient client);
    //连接手动断开
    void onDisconnected(SocketClient client);
}
