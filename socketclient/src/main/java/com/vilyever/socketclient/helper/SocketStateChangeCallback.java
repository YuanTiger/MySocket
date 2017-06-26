package com.vilyever.socketclient.helper;

import android.support.annotation.NonNull;

import com.vilyever.socketclient.SocketClient;

/**
 * SocketStateChangeCallback
 * Created by vilyever on 2016/5/30.
 * Feature: Socket状态监听
 */
public interface SocketStateChangeCallback {
    void onConnected(SocketClient client);
    void onDisconnected(SocketClient client);
}
