package com.vilyever.socketclient.helper;

import com.vilyever.socketclient.util.CharsetUtil;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SocketPacket
 * AndroidSocketClient <com.vilyever.vdsocketclient>
 * Created by vilyever on 2015/9/15.
 * Feature:
 */
public class SocketPacket {
    private final SocketPacket self = this;

    private static final AtomicInteger IDAtomic = new AtomicInteger();

    /* Constructors */
    public SocketPacket(byte[] data) {
        this(data, false);
    }

    public SocketPacket(byte[] data, boolean isHeartBeat) {
        this.ID = IDAtomic.getAndIncrement();
        this.data = Arrays.copyOf(data, data.length);
        this.heartBeat = isHeartBeat;
    }
    /* Properties */
    /**
     * ID, unique
     */
    private final int ID;
    public int getID() {
        return this.ID;
    }

    /**
     * bytes data
     */
    private byte[] data;
    public byte[] getData() {
        return this.data;
    }

    private boolean heartBeat;
    public boolean isHeartBeat() {
        return this.heartBeat;
    }

    private byte[] headerData;
    public SocketPacket setHeaderData(byte[] headerData) {
        this.headerData = headerData;
        return this;
    }
    public byte[] getHeaderData() {
        return this.headerData;
    }

    private byte[] packetLengthData;
    public SocketPacket setPacketLengthData(byte[] packetLengthData) {
        this.packetLengthData = packetLengthData;
        return this;
    }
    public byte[] getPacketLengthData() {
        return this.packetLengthData;
    }

    private byte[] trailerData;
    public SocketPacket setTrailerData(byte[] trailerData) {
        this.trailerData = trailerData;
        return this;
    }
    public byte[] getTrailerData() {
        return this.trailerData;
    }
}