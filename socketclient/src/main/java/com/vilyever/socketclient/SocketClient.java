package com.vilyever.socketclient;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import com.vilyever.socketclient.helper.PackageReciveCallback;
import com.vilyever.socketclient.helper.PackageSendCallback;
import com.vilyever.socketclient.helper.SocketClientAddress;
import com.vilyever.socketclient.helper.SocketStateChangeCallback;
import com.vilyever.socketclient.helper.SocketHeartBeatHelper;
import com.vilyever.socketclient.helper.SocketInputReader;
import com.vilyever.socketclient.helper.SocketPacket;
import com.vilyever.socketclient.helper.SocketPacketHelper;
import com.vilyever.socketclient.helper.SocketResponsePacket;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * SocketClient
 * AndroidSocketClient <com.vilyever.socketclient>
 * <p>
 * Created by vilyever on 2016/3/18.
 * <p>
 * Update My:2017/06/20 针对口粮App需求，删除无用代码
 */
public class SocketClient {
    final SocketClient self = this;

    public static final String TAG = SocketClient.class.getSimpleName();

    /* Constructors */
    public SocketClient() {
        this(new SocketClientAddress());
    }

    public SocketClient(SocketClientAddress address) {
        this.address = address;
    }

    /**
     * 开始连接
     * 连接前必须设置合理ip以及一系列参数
     * 如果需要回调也需要在连接之前设置
     */
    public void connect() {
        if (!isDisconnected()) {
            return;
        }

        if (getAddress() == null) {
            throw new IllegalArgumentException("we need a SocketClientAddress to connect");
        }
        //检查地址是否合理，利用正则
        getAddress().checkValidation();
        //检查包内容的合理性
        getSocketPacketHelper().checkValidation();
        //改变状态为：连接中
        setState(ConnectState.Connecting);
        //启动连接线程
        getConnectionThread().start();
    }

    public void disconnect() {
        if (isDisconnected() || isDisconnecting()) {
            return;
        }

        setDisconnecting(true);

        getDisconnectionThread().start();
    }

    public boolean isConnected() {
        return getState() == ConnectState.Connected;
    }


    public boolean isDisconnected() {
        return getState() == ConnectState.Disconnect;
    }

    public boolean isConnecting() {
        return getState() == ConnectState.Connecting;
    }


    /**
     * 发送byte数组
     *
     * @param data
     * @return 打包后的数据包
     */
    public SocketPacket sendData(byte[] data) {
        if (!isConnected()) {
            return null;
        }
        SocketPacket packet = new SocketPacket(data);
        sendPacket(packet);
        return packet;
    }

    /**
     * 发送字符串，将以{@link #charsetName}编码为byte数组
     *
     * @param message
     * @return 打包后的数据包
     */
    public SocketPacket sendString(String message) {
        if (!isConnected()) {
            return null;
        }
        SocketPacket packet = new SocketPacket(message);
        sendPacket(packet);
        return packet;
    }

    public SocketPacket sendPacket(final SocketPacket packet) {
        if (!isConnected()) {
            return null;
        }

        if (packet == null) {
            return null;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                self.__i__enqueueNewPacket(packet);
            }
        }).start();

        return packet;
    }

    public void cancelSend(final SocketPacket packet) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (getSendingPacketQueue()) {
                    if (self.getSendingPacketQueue().contains(packet)) {
                        self.getSendingPacketQueue().remove(packet);

                        self.__i__onSendPacketCancel(packet);
                    }
                }
            }
        }).start();
    }


    public SocketResponsePacket loopReadData() {
        if (!isConnected()) {
            return null;
        }

        if (getSocketPacketHelper().getReadStrategy() != SocketPacketHelper.ReadStrategy.Manually) {
            return null;
        }

        setReceivingResponsePacket(new SocketResponsePacket());

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (; ; ) {
                    if (!self.isConnected()) {
                        return;
                    }
                    self.__i__onReceivePacketBegin(self.getReceivingResponsePacket());
                    try {
                        //读取数据
                        byte[] data = self.getSocketInputReader().readData();
                        if (data == null) {
                            Thread.sleep(3 * 1000);
                        }
                        //设置回调
                        self.getReceivingResponsePacket().setData(data);
                        if (getCharsetName() != null) {
                            self.getReceivingResponsePacket().buildStringWithCharsetName(getCharsetName());
                        }
                        self.__i__onReceivePacketEnd(self.getReceivingResponsePacket());
                    } catch (IOException e) {
                        e.printStackTrace();

                        if (self.getReceivingResponsePacket() != null) {
                            self.__i__onReceivePacketCancel(self.getReceivingResponsePacket());
                            self.setReceivingResponsePacket(null);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        return getReceivingResponsePacket();
    }

    public SocketResponsePacket readDataToData(final byte[] data) {
        return readDataToData(data, true);
    }

    /**
     * 读取到与指定字节相同的字节序列后回调数据包
     *
     * @param data        指定字节序列
     * @param includeData 是否在回调的数据包中包含指定的字节序列
     * @return 将要读取的数据包实例
     */
    public SocketResponsePacket readDataToData(final byte[] data, final boolean includeData) {
        if (!isConnected()) {
            return null;
        }

        if (getSocketPacketHelper().getReadStrategy() != SocketPacketHelper.ReadStrategy.Manually) {
            return null;
        }

        if (getReceivingResponsePacket() != null) {
            return null;
        }

        setReceivingResponsePacket(new SocketResponsePacket());

        new Thread(new Runnable() {
            @Override
            public void run() {
                self.__i__onReceivePacketBegin(self.getReceivingResponsePacket());
                try {
                    byte[] result = self.getSocketInputReader().readToData(data, includeData);
                    self.getReceivingResponsePacket().setData(result);
                    if (getCharsetName() != null) {
                        self.getReceivingResponsePacket().buildStringWithCharsetName(getCharsetName());
                    }
                    self.__i__onReceivePacketEnd(self.getReceivingResponsePacket());
                } catch (IOException e) {
                    e.printStackTrace();

                    if (self.getReceivingResponsePacket() != null) {
                        self.__i__onReceivePacketCancel(self.getReceivingResponsePacket());
                        self.setReceivingResponsePacket(null);
                    }
                }
            }
        }).start();

        return getReceivingResponsePacket();
    }

    //Socket状态监听
    private SocketStateChangeCallback socketClientDelegate;

    //注册Socket状态监听
    public SocketClient registerSocketStateChangeCallback(SocketStateChangeCallback delegate) {
        if (socketClientDelegate == null) {
            socketClientDelegate = delegate;
        }
        return this;
    }

    //包的发送监听
    private PackageSendCallback packageSendCallback;

    //注册信息发送回调
    public SocketClient registerPackageSendCallback(PackageSendCallback delegate) {
        if (packageSendCallback == null) {
            packageSendCallback = delegate;
        }
        return this;
    }

    private PackageReciveCallback packageRecieveCallback;

    //注册信息接收回调
    public SocketClient registerPackageReceiveCallback(PackageReciveCallback delegate) {
        if (packageRecieveCallback == null) {
            packageRecieveCallback = delegate;
        }
        return this;
    }


    /* Properties */
    private SocketClientAddress address;

    public SocketClient setAddress(SocketClientAddress address) {
        this.address = address;
        return this;
    }

    public SocketClientAddress getAddress() {
        if (this.address == null) {
            this.address = new SocketClientAddress();
        }
        return this.address;
    }

    /**
     * 设置默认的编码格式
     * 为null表示不自动转换data到string
     */
    private String charsetName;

    public SocketClient setCharsetName(String charsetName) {
        this.charsetName = charsetName;
        return this;
    }

    public String getCharsetName() {
        return this.charsetName;
    }

    /**
     * 发送接收时对信息的处理
     * 发送添加尾部信息
     * 接收使用尾部信息截断消息
     */
    private SocketPacketHelper socketPacketHelper;

    public SocketClient setSocketPacketHelper(SocketPacketHelper socketPacketHelper) {
        this.socketPacketHelper = socketPacketHelper;
        return this;
    }

    public SocketPacketHelper getSocketPacketHelper() {
        if (this.socketPacketHelper == null) {
            this.socketPacketHelper = new SocketPacketHelper();
        }
        return this.socketPacketHelper;
    }

    /**
     * 心跳包信息
     */
    private SocketHeartBeatHelper heartBeatHelper;

    public SocketClient setHeartBeatHelper(SocketHeartBeatHelper heartBeatHelper) {
        this.heartBeatHelper = heartBeatHelper;
        return this;
    }

    public SocketHeartBeatHelper getHeartBeatHelper() {
        if (this.heartBeatHelper == null) {
            this.heartBeatHelper = new SocketHeartBeatHelper();
        }
        return this.heartBeatHelper;
    }

    private Socket runningSocket;

    public Socket getRunningSocket() {
        if (this.runningSocket == null) {
            this.runningSocket = new Socket();
        }
        return this.runningSocket;
    }

    protected SocketClient setRunningSocket(Socket socket) {
        this.runningSocket = socket;
        return this;
    }

    private SocketInputReader socketInputReader;

    protected SocketClient setSocketInputReader(SocketInputReader socketInputReader) {
        this.socketInputReader = socketInputReader;
        return this;
    }

    protected SocketInputReader getSocketInputReader() throws IOException {
        if (this.socketInputReader == null) {
            this.socketInputReader = new SocketInputReader(getRunningSocket().getInputStream());
        }
        return this.socketInputReader;
    }

    /**
     * 当前连接状态
     * 当设置状态为{@link ConnectState#Connected}, 收发线程等初始操作均未启动
     * 此状态仅为一个标识
     */
    private ConnectState state;

    protected SocketClient setState(ConnectState state) {
        this.state = state;
        return this;
    }

    public ConnectState getState() {
        if (this.state == null) {
            return ConnectState.Disconnect;
        }
        return this.state;
    }

    /**
     * 连接状态
     */
    public enum ConnectState {
        //断开
        Disconnect,
        //手动断开
        Disconnected,
        //连接中
        Connecting,
        //连接成功
        Connected
    }

    private boolean disconnecting;

    protected SocketClient setDisconnecting(boolean disconnecting) {
        this.disconnecting = disconnecting;
        return this;
    }

    public boolean isDisconnecting() {
        return this.disconnecting;
    }

    private LinkedBlockingQueue<SocketPacket> sendingPacketQueue;

    protected LinkedBlockingQueue<SocketPacket> getSendingPacketQueue() {
        if (sendingPacketQueue == null) {
            sendingPacketQueue = new LinkedBlockingQueue<SocketPacket>();
        }
        return sendingPacketQueue;
    }

    /**
     * 计时器
     */
    private CountDownTimer hearBeatCountDownTimer;

    protected CountDownTimer getHearBeatCountDownTimer() {
        if (this.hearBeatCountDownTimer == null) {
            this.hearBeatCountDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000L) {
                @Override
                public void onTick(long millisUntilFinished) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            self.__i__onTimeTick();
                        }
                    }).start();
                }

                @Override
                public void onFinish() {
                    if (self.isConnected()) {
                        this.start();
                    }
                }
            };

        }
        return this.hearBeatCountDownTimer;
    }

    /**
     * 记录上次发送心跳包的时间
     */
    private long lastSendHeartBeatMessageTime;

    protected SocketClient setLastSendHeartBeatMessageTime(long lastSendHeartBeatMessageTime) {
        this.lastSendHeartBeatMessageTime = lastSendHeartBeatMessageTime;
        return this;
    }

    protected long getLastSendHeartBeatMessageTime() {
        return this.lastSendHeartBeatMessageTime;
    }

    /**
     * 记录上次接收到消息的时间
     */
    private long lastReceiveMessageTime;

    protected SocketClient setLastReceiveMessageTime(long lastReceiveMessageTime) {
        this.lastReceiveMessageTime = lastReceiveMessageTime;
        return this;
    }

    protected long getLastReceiveMessageTime() {
        return this.lastReceiveMessageTime;
    }

    /**
     * 记录上次发送数据片段的时间
     * 仅在每个发送包开始发送时计时，结束后重置计时
     * NoSendingTime 表示当前没有在发送数据
     */
    private final static long NoSendingTime = -1;
    private long lastSendMessageTime = NoSendingTime;

    protected SocketClient setLastSendMessageTime(long lastSendMessageTime) {
        this.lastSendMessageTime = lastSendMessageTime;
        return this;
    }

    protected long getLastSendMessageTime() {
        return this.lastSendMessageTime;
    }

    private SocketPacket sendingPacket;

    protected SocketClient setSendingPacket(SocketPacket sendingPacket) {
        this.sendingPacket = sendingPacket;
        return this;
    }

    protected SocketPacket getSendingPacket() {
        return this.sendingPacket;
    }

    private SocketResponsePacket receivingResponsePacket;

    protected SocketClient setReceivingResponsePacket(SocketResponsePacket receivingResponsePacket) {
        this.receivingResponsePacket = receivingResponsePacket;
        return this;
    }

    protected SocketResponsePacket getReceivingResponsePacket() {
        return this.receivingResponsePacket;
    }

    private long lastReceiveProgressCallbackTime;

    protected SocketClient setLastReceiveProgressCallbackTime(long lastReceiveProgressCallbackTime) {
        this.lastReceiveProgressCallbackTime = lastReceiveProgressCallbackTime;
        return this;
    }

    protected long getLastReceiveProgressCallbackTime() {
        return this.lastReceiveProgressCallbackTime;
    }

    private ConnectionThread connectionThread;

    protected SocketClient setConnectionThread(ConnectionThread connectionThread) {
        this.connectionThread = connectionThread;
        return this;
    }

    protected ConnectionThread getConnectionThread() {
        if (this.connectionThread == null) {
            this.connectionThread = new ConnectionThread();
        }
        return this.connectionThread;
    }

    private DisconnectionThread disconnectionThread;

    protected SocketClient setDisconnectionThread(DisconnectionThread disconnectionThread) {
        this.disconnectionThread = disconnectionThread;
        return this;
    }

    protected DisconnectionThread getDisconnectionThread() {
        if (this.disconnectionThread == null) {
            this.disconnectionThread = new DisconnectionThread();
        }
        return this.disconnectionThread;
    }

    private SendThread sendThread;

    protected SocketClient setSendThread(SendThread sendThread) {
        this.sendThread = sendThread;
        return this;
    }

    protected SendThread getSendThread() {
        if (this.sendThread == null) {
            this.sendThread = new SendThread();
        }
        return this.sendThread;
    }

    private ReceiveThread receiveThread;

    protected SocketClient setReceiveThread(ReceiveThread receiveThread) {
        this.receiveThread = receiveThread;
        return this;
    }

    protected ReceiveThread getReceiveThread() {
        if (this.receiveThread == null) {
            this.receiveThread = new ReceiveThread();
        }
        return this.receiveThread;
    }


    private UIHandler uiHandler;

    protected UIHandler getUiHandler() {
        if (this.uiHandler == null) {
            this.uiHandler = new UIHandler(this);
        }
        return this.uiHandler;
    }

    private static class UIHandler extends Handler {
        private WeakReference<SocketClient> referenceSocketClient;

        public UIHandler(@NonNull SocketClient referenceSocketClient) {
            super(Looper.getMainLooper());

            this.referenceSocketClient = new WeakReference<SocketClient>(referenceSocketClient);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    /* Overrides */


    /* Delegates */


    /* Protected Methods */
    @CallSuper
    protected void internalOnConnected() {
        setState(ConnectState.Connected);

        setLastSendHeartBeatMessageTime(System.currentTimeMillis());
        setLastReceiveMessageTime(System.currentTimeMillis());
        setLastSendMessageTime(NoSendingTime);

        setSendingPacket(null);
        setReceivingResponsePacket(null);

        __i__onConnected();
    }

    /* Private Methods */
    private void __i__enqueueNewPacket(final SocketPacket packet) {
        if (!isConnected()) {
            return;
        }

        synchronized (getSendingPacketQueue()) {
            try {
                getSendingPacketQueue().put(packet);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void __i__sendHeartBeat() {
        if (!isConnected()) {
            return;
        }

        if (getHeartBeatHelper() == null
                || !getHeartBeatHelper().isSendHeartBeatEnabled()) {
            return;
        }

        final SocketPacket packet = new SocketPacket(getHeartBeatHelper().getSendData(), true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                self.__i__enqueueNewPacket(packet);
            }
        }).start();
    }


    /**
     * 当Socket连接成功时自动调用此方法
     */
    private void __i__onConnected() {
        //如果该线程不是主线程
        if (Looper.myLooper() != Looper.getMainLooper()) {
            //切换到主线程执行该方法
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onConnected();
                }
            });
            return;
        }
        //触发连接成功回调-这绝对是在主线程
        if (socketClientDelegate != null) {
            socketClientDelegate.onConnected(this);
        }

        //启动发送线程
        getSendThread().start();
        //启动接收线程
        getReceiveThread().start();
        //启动心跳线程
        getHearBeatCountDownTimer().start();
    }

    /**
     * 当Socket断开时自动调用此方法
     */
    private void __i__onDisconnected() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onDisconnected();
                }
            });
            return;
        }
        //触发断开连接的回调-这绝对是在主线程
        if (socketClientDelegate != null) {
            socketClientDelegate.onDisconnected(this);
        }
    }

    /**
     * 开始发送数据时调用此方法
     *
     * @param packet
     */
    private void __i__onSendPacketBegin(final SocketPacket packet) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onSendPacketBegin(packet);
                }
            });
            return;
        }
        //触发发包开始的回调-这绝对是在主线程
        if (packageSendCallback != null) {
            packageSendCallback.onSendPacketBegin(this, packet);
        }
    }

    /**
     * 发送数据结束调用此方法
     * @param packet
     */
    private void __i__onSendPacketEnd(final SocketPacket packet) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onSendPacketEnd(packet);
                }
            });
            return;
        }
        //触发发送结束的回调-这绝对是在主线程
        if (packageSendCallback != null) {
            packageSendCallback.onSendPacketEnd(this, packet);
        }
    }

    /**
     * 取消发送调用此方法
     * @param packet
     */
    private void __i__onSendPacketCancel(final SocketPacket packet) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onSendPacketCancel(packet);
                }
            });
            return;
        }
        //触发发送取消的回调-这绝对是在主线程
        if (packageSendCallback != null) {
            packageSendCallback.onSendPacketCancel(this, packet);
        }
    }

    /**
     * 发送进度
     * @param packet
     * @param sendedLength
     * @param headerLength
     * @param packetLengthDataLength
     * @param dataLength
     * @param trailerLength
     */
    private void __i__onSendingPacketInProgress(final SocketPacket packet, final int sendedLength, final int headerLength, final int packetLengthDataLength, final int dataLength, final int trailerLength) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onSendingPacketInProgress(packet, sendedLength, headerLength, packetLengthDataLength, dataLength, trailerLength);
                }
            });
            return;
        }
        float progress = sendedLength / (float) (headerLength + packetLengthDataLength + dataLength + trailerLength);
        //触发发送进度的回调-这绝对是在主线程
        if (packageSendCallback != null) {
            packageSendCallback.onSendingPacketInProgress(this, packet, progress, sendedLength);
        }

    }

    /**
     * 开始接收时调用此方法
     * @param packet
     */
    private void __i__onReceivePacketBegin(final SocketResponsePacket packet) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onReceivePacketBegin(packet);
                }
            });
            return;
        }
        //触发接收包的开始回调-这绝对是在主线程
        if (packageRecieveCallback != null) {
            packageRecieveCallback.onReceivePacketBegin(this, packet);
        }
    }

    /**
     * 接收数据完成时调用此方法
     * @param packet
     */
    private void __i__onReceivePacketEnd(final SocketResponsePacket packet) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onReceivePacketEnd(packet);
                }
            });
            return;
        }
        //触发接收包的结束回调-这绝对是在主线程
        if (packageRecieveCallback != null) {
            packageRecieveCallback.onReceivePacketEnd(this, packet);
        }

    }

    /**
     * 取消接收时调用此方法
     * @param packet
     */
    private void __i__onReceivePacketCancel(final SocketResponsePacket packet) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onReceivePacketCancel(packet);
                }
            });
            return;
        }
        //触发接收包取消回调-这绝对是在主线程
        if (packageRecieveCallback != null) {
            packageRecieveCallback.onReceivePacketCancel(this, packet);
        }
    }

    /**
     * 接收进度
     * @param packet
     * @param receivedLength
     * @param headerLength
     * @param packetLengthDataLength
     * @param dataLength
     * @param trailerLength
     */
    private void __i__onReceivingPacketInProgress(final SocketResponsePacket packet, final int receivedLength, final int headerLength, final int packetLengthDataLength, final int dataLength, final int trailerLength) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - getLastReceiveProgressCallbackTime() < (1000 / 24)) {
            return;
        }

        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onReceivingPacketInProgress(packet, receivedLength, headerLength, packetLengthDataLength, dataLength, trailerLength);
                }
            });
            return;
        }
        float progress = receivedLength / (float) (headerLength + packetLengthDataLength + dataLength + trailerLength);
        //触发接收包进度回调-这绝对是在主线程
        if (packageRecieveCallback != null) {
            packageRecieveCallback.onReceivingPacketInProgress(this, packet, progress, receivedLength);
        }
        setLastReceiveProgressCallbackTime(System.currentTimeMillis());
    }

    private void __i__onTimeTick() {
        if (!isConnected()) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (getHeartBeatHelper().isSendHeartBeatEnabled()) {
            if (currentTime - getLastSendHeartBeatMessageTime() >= getHeartBeatHelper().getHeartBeatInterval()) {
                __i__sendHeartBeat();
                setLastSendHeartBeatMessageTime(currentTime);
            }
        }

        if (getSocketPacketHelper().isReceiveTimeoutEnabled()) {
            if (currentTime - getLastReceiveMessageTime() >= getSocketPacketHelper().getReceiveTimeout()) {
                disconnect();
            }
        }

        if (getSocketPacketHelper().isSendTimeoutEnabled()
                && getLastSendMessageTime() != NoSendingTime) {
            if (currentTime - getLastSendMessageTime() >= getSocketPacketHelper().getSendTimeout()) {
                disconnect();
            }
        }
    }


    /**
     * Socket连接线程
     */
    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            super.run();

            try {
                if (Thread.interrupted()) {
                    return;
                }
                //获取地址参数
                SocketClientAddress address = self.getAddress();

                //实例化Sokcet对象，并连接
                self.getRunningSocket().connect(address.getInetSocketAddress(), address.getConnectionTimeout());

                if (Thread.interrupted()) {
                    return;
                }
                //改变连接状态为连接成功
                self.setState(ConnectState.Connected);
                //上次发送心跳包的时间
                self.setLastSendHeartBeatMessageTime(System.currentTimeMillis());
                //上次接收消息的时间
                self.setLastReceiveMessageTime(System.currentTimeMillis());
                //上次发送消息的时间
                self.setLastSendMessageTime(NoSendingTime);
                //发送内容
                self.setSendingPacket(null);
                //接收内容
                self.setReceivingResponsePacket(null);
                //线程启动成功，将线程对象清空
                self.setConnectionThread(null);

                self.__i__onConnected();
            } catch (IOException e) {
                e.printStackTrace();

                self.disconnect();
            }
        }
    }

    private class DisconnectionThread extends Thread {
        @Override
        public void run() {
            super.run();

            if (self.connectionThread != null) {
                self.getConnectionThread().interrupt();
                self.setConnectionThread(null);
            }

            if (!self.getRunningSocket().isClosed()
                    || self.isConnecting()) {
                try {
                    self.getRunningSocket().getOutputStream().close();
                    self.getRunningSocket().getInputStream().close();
                } catch (IOException e) {
//                e.printStackTrace();
                } finally {
                    try {
                        self.getRunningSocket().close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    self.setRunningSocket(null);
                }
            }

            if (self.sendThread != null) {
                self.getSendThread().interrupt();
                self.setSendThread(null);
            }
            if (self.receiveThread != null) {
                self.getReceiveThread().interrupt();
                self.setReceiveThread(null);
            }

            self.setDisconnecting(false);
            self.setState(ConnectState.Disconnect);
            self.setSocketInputReader(null);

            if (self.hearBeatCountDownTimer != null) {
                self.hearBeatCountDownTimer.cancel();
            }

            if (self.getSendingPacket() != null) {
                self.__i__onSendPacketCancel(self.getSendingPacket());
                self.setSendingPacket(null);
            }

            SocketPacket packet;
            while ((packet = self.getSendingPacketQueue().poll()) != null) {
                self.__i__onSendPacketCancel(packet);
            }

            if (self.getReceivingResponsePacket() != null) {
                self.__i__onReceivePacketCancel(self.getReceivingResponsePacket());
                self.setReceivingResponsePacket(null);
            }

            self.setDisconnectionThread(null);

            self.__i__onDisconnected();
        }
    }

    private class SendThread extends Thread {
        public SendThread() {
        }

        @Override
        public void run() {
            super.run();

            SocketPacket packet;
            try {
                while (self.isConnected()
                        && !Thread.interrupted()
                        && (packet = self.getSendingPacketQueue().take()) != null) {
                    self.setSendingPacket(packet);
                    self.setLastSendMessageTime(System.currentTimeMillis());

                    if (packet.getData() == null
                            && packet.getMessage() != null) {
                        if (self.getCharsetName() == null) {
                            throw new IllegalArgumentException("we need string charset to send string type message");
                        } else {
                            packet.buildDataWithCharsetName(self.getCharsetName());
                        }
                    }

                    if (packet.getData() == null) {
                        self.__i__onSendPacketCancel(packet);
                        self.setSendingPacket(null);
                        continue;
                    }

                    byte[] headerData = self.getSocketPacketHelper().getSendHeaderData();
                    int headerDataLength = headerData == null ? 0 : headerData.length;

                    byte[] trailerData = self.getSocketPacketHelper().getSendTrailerData();
                    int trailerDataLength = trailerData == null ? 0 : trailerData.length;

                    byte[] packetLengthData = self.getSocketPacketHelper().getSendPacketLengthData(packet.getData().length + trailerDataLength);
                    int packetLengthDataLength = packetLengthData == null ? 0 : packetLengthData.length;

                    int sendedPacketLength = 0;

                    packet.setHeaderData(headerData);
                    packet.setTrailerData(trailerData);
                    packet.setPacketLengthData(packetLengthData);

                    if (headerDataLength + packetLengthDataLength + packet.getData().length + trailerDataLength <= 0) {
                        self.__i__onSendPacketCancel(packet);
                        self.setSendingPacket(null);
                        continue;
                    }

                    self.__i__onSendPacketBegin(packet);
                    self.__i__onSendingPacketInProgress(packet, sendedPacketLength, headerDataLength, packetLengthDataLength, packet.getData().length, trailerDataLength);

                    try {
                        if (headerDataLength > 0) {
                            self.getRunningSocket().getOutputStream().write(headerData);
                            self.getRunningSocket().getOutputStream().flush();
                            self.setLastSendMessageTime(System.currentTimeMillis());

                            sendedPacketLength += headerDataLength;
                            self.__i__onSendingPacketInProgress(packet, sendedPacketLength, headerDataLength, packetLengthDataLength, packet.getData().length, trailerDataLength);
                        }

                        if (packetLengthDataLength > 0) {
                            self.getRunningSocket().getOutputStream().write(packetLengthData);
                            self.getRunningSocket().getOutputStream().flush();
                            self.setLastSendMessageTime(System.currentTimeMillis());

                            sendedPacketLength += packetLengthDataLength;
                            self.__i__onSendingPacketInProgress(packet, sendedPacketLength, headerDataLength, packetLengthDataLength, packet.getData().length, trailerDataLength);
                        }

                        if (packet.getData().length > 0) {
                            int segmentLength = self.getRunningSocket().getSendBufferSize();
                            if (self.getSocketPacketHelper().isSendSegmentEnabled()) {
                                segmentLength = Math.min(segmentLength, self.getSocketPacketHelper().getSendSegmentLength());
                            }

                            int offset = 0;

                            while (offset < packet.getData().length) {
                                int end = offset + segmentLength;
                                end = Math.min(end, packet.getData().length);
                                self.getRunningSocket().getOutputStream().write(packet.getData(), offset, end - offset);
                                self.getRunningSocket().getOutputStream().flush();
                                self.setLastSendMessageTime(System.currentTimeMillis());

                                sendedPacketLength += end - offset;

                                self.__i__onSendingPacketInProgress(packet, sendedPacketLength, headerDataLength, packetLengthDataLength, packet.getData().length, trailerDataLength);

                                offset = end;
                            }
                        }

                        if (trailerDataLength > 0) {
                            self.getRunningSocket().getOutputStream().write(trailerData);
                            self.getRunningSocket().getOutputStream().flush();
                            self.setLastSendMessageTime(System.currentTimeMillis());

                            sendedPacketLength += trailerDataLength;

                            self.__i__onSendingPacketInProgress(packet, sendedPacketLength, headerDataLength, packetLengthDataLength, packet.getData().length, trailerDataLength);
                        }

                        self.__i__onSendPacketEnd(packet);
                        self.setSendingPacket(null);

                        self.setLastSendMessageTime(NoSendingTime);
                    } catch (IOException e) {
                        e.printStackTrace();

                        if (self.getSendingPacket() != null) {
                            self.__i__onSendPacketCancel(self.getSendingPacket());
                            self.setSendingPacket(null);
                        }
                    }
                }
            } catch (InterruptedException e) {
//                e.printStackTrace();
                if (self.getSendingPacket() != null) {
                    self.__i__onSendPacketCancel(self.getSendingPacket());
                    self.setSendingPacket(null);
                }
            }
        }
    }

    /**
     * 接收消息线程
     */
    private class ReceiveThread extends Thread {
        @Override
        public void run() {
            super.run();

            try {
                //连接成功 && socket.getInputStream()不为空 && 线程没有中断
                while (self.isConnected() && self.getSocketInputReader() != null && !Thread.interrupted()) {
                    //创建接收包对象
                    SocketResponsePacket packet = new SocketResponsePacket();
                    self.setReceivingResponsePacket(packet);
                    //触发开始回调
                    self.__i__onReceivePacketBegin(packet);
                    //开始读取数据
                    byte[] data = self.getSocketInputReader().readData();
                    //读取完成后赋值本次读取时间
                    self.setLastReceiveMessageTime(System.currentTimeMillis());
                    //将数据赋值到接收包对象中
                    packet.setData(data);
                    //结束回调的触发
                    self.__i__onReceivePacketEnd(packet);

//                    self.setReceivingResponsePacket(null);
                }
            } catch (Exception e) {
//                e.printStackTrace();
                self.disconnect();

                if (self.getReceivingResponsePacket() != null) {
                    self.__i__onReceivePacketCancel(self.getReceivingResponsePacket());
                    self.setReceivingResponsePacket(null);
                }
            }
        }
    }

}