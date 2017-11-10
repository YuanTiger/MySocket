package com.vilyever.socketclient.helper;

import com.vilyever.socketclient.SocketClient;

/**
 * PackageSendCallback
 * Created by vilyever on 2016/5/30.
 * Feature:
 */
public interface PackageSendCallback {
    void onSendPacketBegin(SocketClient client, SocketPacket packet);
    void onSendPacketEnd(SocketClient client, SocketPacket packet);
    void onSendPacketCancel(SocketClient client, SocketPacket packet);

}
