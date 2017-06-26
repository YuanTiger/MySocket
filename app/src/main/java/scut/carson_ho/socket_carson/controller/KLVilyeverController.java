package scut.carson_ho.socket_carson.controller;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.helper.SocketClientDelegate;
import com.vilyever.socketclient.helper.SocketClientReceivingDelegate;
import com.vilyever.socketclient.helper.SocketClientSendingDelegate;
import com.vilyever.socketclient.helper.SocketHeartBeatHelper;
import com.vilyever.socketclient.helper.SocketPacket;
import com.vilyever.socketclient.helper.SocketPacketHelper;
import com.vilyever.socketclient.helper.SocketResponsePacket;
import com.vilyever.socketclient.util.CharsetUtil;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import scut.carson_ho.socket_carson.KLSocketBean;
import scut.carson_ho.socket_carson.OperationType;
import scut.carson_ho.socket_carson.SocketConfig;

/**
 * Author：mengyuan
 * Date  : 2017/6/5上午11:07
 * E-Mail:mengyuanzz@126.com
 * Desc  :
 */

public class KLVilyeverController {
    private static KLVilyeverController ourInstance = new KLVilyeverController();

    private SocketClient localSocketClient;

    private int heartTag;
    private int loginTag;


    private boolean isLogin = false;

    private String ip;
    private int port;
    //从Activity传来的Handler
    private Handler mainHandler;

    public static KLVilyeverController getInstance() {
        if (ourInstance == null) {
            synchronized (new Object()) {
                if (ourInstance == null) {
                    ourInstance = new KLVilyeverController();
                }
            }
        }
        return ourInstance;
    }

    private KLVilyeverController() {
    }

    /**
     * PUBLIC
     * 连接服务器
     *
     * @param ip
     * @param port
     */
    public synchronized void connectionSocket(final String ip, final int port, final Handler mainHandler) {
        //如果服务器已经连接
        if (localSocketClient != null && localSocketClient.isConnected()) {
            localSocketClient.disconnect();
            localSocketClient = null;
        }
        if (localSocketClient == null) {
            localSocketClient = new SocketClient();
        }
        isLogin = false;
        this.mainHandler = mainHandler;
        this.ip = ip;
        this.port = port;
        //设置远端地址
        setSocketAddress(ip, port + "");
        //设置编码格式
        localSocketClient.setCharsetName(CharsetUtil.UTF_8); // 设置编码为UTF-8
        //设置包头
//        localSocketClient.getSocketPacketHelper().setReceivePacketLengthDataLength(KLSocketBean.HEADER_LEN);

        //设置固定的心跳包内容
//        setDefaultHeartContent();
        //设置动态的心跳包内容
        setAutoHeartContent();

        //设置读取模式：手动读取模式
        setModelDefaultSend();
        setModelDefaultReceive();


        this.localSocketClient.registerSocketClientDelegate(new SocketClientDelegate() {
            /**
             * 连接上远程端时的回调
             */
            @Override
            public void onConnected(SocketClient client) {
                Log.i("mengyuansocket", "成功连接上服务器：" + client.getAddress().toString());
                //去登录
                login();
            }

            /**
             * 与远程端断开连接时的回调
             */
            @Override
            public void onDisconnected(final SocketClient client) {
                if (client.getState() == SocketClient.State.Disconnected) {
                    Log.i("mengyuansocket", "手动断开了连接");
                    return;
                }
                Log.i("mengyuansocket", "与服务器断开了连接，3秒后重新连接");
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            Thread.sleep(3 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        connectionSocket(ip, port, mainHandler);

                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);

                    }
                }.execute();
            }

            /**
             * 接收到数据包时的回调
             */
            @Override
            public void onResponse(final SocketClient client, @NonNull SocketResponsePacket responsePacket) {
                Log.i("mengyuansocket", "onResponse: " + responsePacket.hashCode() + " 【" + responsePacket.getMessage() + "】 " + " isHeartBeat: " + responsePacket.isHeartBeat() + " " + Arrays.toString(responsePacket.getData()));
                if (responsePacket.getData() == null) {
                    return;
                }
                KLSocketBean bean = KLSocketBean.toSocketBean(responsePacket.getData());
                if (bean == null) {
                    return;
                }
                switch (bean.operationType) {
                    case OperationType.TYPE_LOGIN_SERVER://登录成功
                        Log.i("mengyuansocket", "登录成功");
                        localSocketClient.getHeartBeatHelper().setSendHeartBeatEnabled(true); // 设置允许自动发送心跳包，此值默认为false
//                            localSocketClient.getSocketConfigure().getHeartBeatHelper().setSendHeartBeatEnabled(isLogin = true); // 设置允许自动发送心跳包，此值默认为false
                        break;
                    case OperationType.TYPE_HEART_SERVER://心跳成功
                        Log.i("mengyuansocket", "心跳成功");
                        break;
                }

            }

        });
        this.localSocketClient.registerSocketClientSendingDelegate(new SocketClientSendingDelegate() {
            /**
             * 数据包开始发送时的回调
             */
            @Override
            public void onSendPacketBegin(SocketClient client, SocketPacket packet) {
                Log.i("mengyuansocket", "数据包开始发送时的回调: " + packet.hashCode() + "   " + Arrays.toString(packet.getData()));
            }

            /**
             * 数据包取消发送时的回调
             * 取消发送回调有以下情况：
             * 1. 手动cancel仍在排队，还未发送过的packet
             * 2. 断开连接时，正在发送的packet和所有在排队的packet都会被取消
             */
            @Override
            public void onSendPacketCancel(SocketClient client, SocketPacket packet) {
                Log.i("mengyuansocket", "数据包取消发送时的回调: " + packet.hashCode());
            }

            /**
             * 数据包发送的进度回调
             * progress值为[0.0f, 1.0f]
             * 通常配合分段发送使用
             * 可用于显示文件等大数据的发送进度
             */
            @Override
            public void onSendingPacketInProgress(SocketClient client, SocketPacket packet, float progress, int sendedLength) {
                Log.i("mengyuansocket", "数据包发送的进度回调: " + packet.hashCode() + " : " + progress + " : " + sendedLength);
            }

            /**
             * 数据包完成发送时的回调
             */
            @Override
            public void onSendPacketEnd(SocketClient client, SocketPacket packet) {
                Log.i("mengyuansocket", "数据包完成发送时的回调: " + packet.hashCode());
            }
        });
        this.localSocketClient.registerSocketClientReceiveDelegate(new SocketClientReceivingDelegate() {
            @Override
            public void onReceivePacketBegin(SocketClient client, SocketResponsePacket packet) {
                Log.i("mengyuansocket", "接收数据包开始: " + packet.hashCode());
            }

            @Override
            public void onReceivePacketEnd(SocketClient client, SocketResponsePacket packet) {
                Log.i("mengyuansocket", "接收数据包完成: " + packet.hashCode());
            }

            @Override
            public void onReceivePacketCancel(SocketClient client, SocketResponsePacket packet) {
                Log.i("mengyuansocket", "接收数据包已取消: " + packet.hashCode());
            }

            @Override
            public void onReceivingPacketInProgress(SocketClient client, SocketResponsePacket packet, float progress, int receivedLength) {
                Log.i("mengyuansocket", "接收数据包进度: " + packet.hashCode() + " : " + progress + " : " + receivedLength);
            }
        });


        localSocketClient.connect();
    }


    public void login() {
        if (localSocketClient == null) {
            return;
        }
        //发送登录请求
        SocketPacket packet = new SocketPacket(KLSocketBean.createAppLoginPackage("zhongjin", "test", ++loginTag));
        localSocketClient.sendPacket(packet);
    }

    public void stop() {
        if (localSocketClient == null) {
            return;
        }
        localSocketClient.disconnect();
        localSocketClient = null;
    }

    public void sendHeart() {
        if (localSocketClient == null) {
            return;
        }
        //发送心跳包请求
        SocketPacket packet = new SocketPacket(KLSocketBean.createAppHeartPackage(++heartTag));
        localSocketClient.sendPacket(packet);
    }

    /**
     * 设置远程端地址信息
     */
    private void setSocketAddress(String ip, String port) {
        localSocketClient.getAddress().setRemoteIP(ip); // 远程端IP地址
        localSocketClient.getAddress().setRemotePort(port); // 远程端端口号
        localSocketClient.getAddress().setConnectionTimeout(SocketConfig.CONNECTION_TIME); // 连接超时时长，单位毫秒
    }


    /**
     * 设置固定的心跳包内容
     */
    private void setDefaultHeartContent() {
        /**
         * 设置自动发送的心跳包信息
         */
        localSocketClient.getHeartBeatHelper().setDefaultSendData(KLSocketBean.createAppHeartPackage(heartTag++));

        /**
         * 设置远程端发送到本地的心跳包信息内容，用于判断接收到的数据包是否是心跳包
         * 通过{@link SocketResponsePacket#isHeartBeat()} 查看数据包是否是心跳包
         */
//        localSocketClient.getHeartBeatHelper().setDefaultReceiveData(KLSocketBean.createServerHeartPackage(heartTag));
        localSocketClient.getHeartBeatHelper().setHeartBeatInterval(SocketConfig.HEART_SEND_TIME); // 设置自动发送心跳包的间隔时长，单位毫秒
//        localSocketClient.getHeartBeatHelper().setSendHeartBeatEnabled(true); // 设置允许自动发送心跳包，此值默认为false
    }


    /**
     * 设置自动生成的心跳包内容
     * 这里的例子是使用不同的日期
     */
    private void setAutoHeartContent() {
        /**
         * 设置自动发送的心跳包信息
         * 此信息动态生成
         *
         * 每次发送心跳包时自动调用
         */
        localSocketClient.getHeartBeatHelper().setSendDataBuilder(new SocketHeartBeatHelper.SendDataBuilder() {
            @Override
            public byte[] obtainSendHeartBeatData(SocketHeartBeatHelper helper) {

                return KLSocketBean.createAppHeartPackage(++heartTag);
            }
        });

        /**
         * 设置远程端发送到本地的心跳包信息的检测器，用于判断接收到的数据包是否是心跳包
         * 通过{@link SocketResponsePacket#isHeartBeat()} 查看数据包是否是心跳包
         */
        localSocketClient.getHeartBeatHelper().setReceiveHeartBeatPacketChecker(new SocketHeartBeatHelper.ReceiveHeartBeatPacketChecker() {
            @Override
            public boolean isReceiveHeartBeatPacket(SocketHeartBeatHelper helper, SocketResponsePacket responsePacket) {
                /**
                 * 判断数据包信息是否含有指定的心跳包前缀和后缀
                 */
                Log.i("mengyuansocket", "isReceiveHeartBeatPacket返回的数据: " + responsePacket.hashCode() + " 【" + responsePacket.getMessage() + "】 " + " isHeartBeat: " + responsePacket.isHeartBeat() + " " + Arrays.toString(responsePacket.getData()));

                if (responsePacket.getData() == null) {
                    Log.i("mengyuansocket", "isReceiveHeartBeatPacket-数据为空");
                    return false;
                }
                KLSocketBean bean = KLSocketBean.toSocketBean(responsePacket.getData());
                if (bean == null) {
                    Log.i("mengyuansocket", "isReceiveHeartBeatPacket-解析失败");
                    return false;
                }

                if (bean.operationType == OperationType.TYPE_HEART_SERVER) {
                    Log.i("mengyuansocket", "isReceiveHeartBeatPacket-为心跳包");
                    return true;
                }
                Log.i("mengyuansocket", "isReceiveHeartBeatPacket-不为为心跳包");

                return false;
            }
        });

        localSocketClient.getHeartBeatHelper().setHeartBeatInterval(SocketConfig.HEART_SEND_TIME); // 设置自动发送心跳包的间隔时长，单位毫秒
        localSocketClient.getHeartBeatHelper().setSendHeartBeatEnabled(false); // 设置允许自动发送心跳包，此值默认为false
    }


    /**
     * 设置发送为手动模式
     */
    private void setModelDefaultSend() {
        /**
         * 设置分段发送数据长度
         * 即在发送指定长度后通过 {@link SocketClientSendingDelegate#onSendingPacketInProgress(SocketClient, SocketPacket, float, int)}回调当前发送进度
         * 注意：回调过于频繁可能导致设置UI过于频繁从而导致主线程卡顿
         *
         * 若无需进度回调可删除此二行，删除后仍有【发送开始】【发送结束】的回调
         */
//        socketClient.getSocketPacketHelper().setSendSegmentLength(8); // 设置发送分段长度，单位byte
//        socketClient.getSocketPacketHelper().setSendSegmentEnabled(true); // 设置允许使用分段发送，此值默认为false

        /**
         * 设置发送超时时长
         * 在发送每个数据包时，发送每段数据的最长时间，超过后自动断开socket连接
         * 通过设置分段发送{@link SocketPacketHelper#setSendSegmentEnabled(boolean)} 可避免发送大数据包时因超时断开，
         *
         * 若无需限制发送时长可删除此二行
         */
        localSocketClient.getSocketPacketHelper().setSendTimeout(SocketConfig.HEART_SEND_TIME); // 设置发送超时时长，单位毫秒
        localSocketClient.getSocketPacketHelper().setSendTimeoutEnabled(true); // 设置允许使用发送超时时长，此值默认为false
    }

    /**
     * 设置接收为手动模式
     */
    private void setModelDefaultReceive() {
        /**
         * 设置读取策略为手动读取
         * 手动读取有两种方法
         * 1. {@link SocketClient#readDataToData(byte[], boolean)} )} 读取到与指定字节相同的字节序列后回调数据包
         * 2. {@link SocketClient#readDataToLength(int)} 读取指定长度的字节后回调数据包
         *
         * 此时SocketPacketHelper中其他读取相关设置将会无效化
         */
        localSocketClient.getSocketPacketHelper().setReadStrategy(SocketPacketHelper.ReadStrategy.Manually);
    }

}
