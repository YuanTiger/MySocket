package com.vilyever.socketclient.helper;

import com.vilyever.socketclient.SocketClient;

/**
 * PackageReciveCallback
 * Created by vilyever on 2016/5/30.
 * Feature:
 */
public interface PackageReciveCallback {
    void onReceivePacketBegin(SocketClient client, SocketResponsePacket responsePacket);
    void onReceivePacketEnd(SocketClient client, SocketResponsePacket responsePacket);
    void onReceivePacketCancel(SocketClient client, SocketResponsePacket responsePacket);
    void onReceivingPacketInProgress(SocketClient client, SocketResponsePacket packet, float progress, int receivedLength);

}
