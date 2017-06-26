package scut.carson_ho.socket_carson.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import scut.carson_ho.socket_carson.KLSocketBean;
import scut.carson_ho.socket_carson.R;
import scut.carson_ho.socket_carson.controller.KLVilyeverController;

/**
 * Author：mengyuan
 * Date  : 2017/6/2下午7:04
 * E-Mail:mengyuanzz@126.com
 * Desc  :
 */

public class SocketIoActivity extends AppCompatActivity implements View.OnClickListener {


    // 连接 断开连接 发送数据到服务器 的按钮变量
    private Button btnConnect, btPay, btLogin, stop, start_heart;

    // 显示接收服务器消息 按钮
    private TextView tv_server_message;


    private Socket mSocket = null;
    private boolean isConnected;
    private int loginTag;
    private int heartTag;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            tv_server_message.setText((String) msg.obj);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        /**
         * 初始化操作
         */

        // 初始化所有按钮
        btnConnect = (Button) findViewById(R.id.connect);
        btPay = (Button) findViewById(R.id.pay);
        btLogin = (Button) findViewById(R.id.send);
        start_heart = (Button) findViewById(R.id.start_heart);
        stop = (Button) findViewById(R.id.stop);
        tv_server_message = (TextView) findViewById(R.id.tv_server_message);

        btnConnect.setOnClickListener(this);
        btPay.setOnClickListener(this);
        btLogin.setOnClickListener(this);
        stop.setOnClickListener(this);
        start_heart.setOnClickListener(this);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        onDestorySocket();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect://连接
                try {
//                    mSocket = IO.socket("http://192.168.199.149:9999");
                    IO.Options options = new IO.Options();
                    options.forceNew = true;
                    options.reconnection = false;
                    mSocket = IO.socket("http://192.168.199.122:9999/",options);
//                    mSocket = IO.socket("https://socketio-chat.now.sh/");

                } catch (URISyntaxException e) {
                    Log.i("mengyuanio", e.getMessage());
                    e.printStackTrace();
                }
                Log.i("mengyuanio",mSocket.toString());
                //连接成功监听
                mSocket.on(Socket.EVENT_CONNECT, onConnect);
                //连接断开监听
                mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
                //连接失败监听
                mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
                //连接超时监听
                mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
//                //自定义消息监听
                mSocket.on("login", login);
                mSocket.on("heart", heart);
                mSocket.on("pay", pay);

                mSocket.connect();
                break;
            case R.id.pay://支付
                break;
            case R.id.start_heart://启动心跳
                mSocket.emit("heart", KLSocketBean.createAppHeartPackage(++heartTag));
                break;
            case R.id.stop://停止
                onDestorySocket();
                break;
            case R.id.send://登录
                mSocket.emit("login", KLSocketBean.createAppLoginPackage("mengyuan", "test1", ++loginTag));
                break;
        }
    }

    private void onDestorySocket() {

        mSocket.disconnect();

        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("login", login);
        mSocket.off("heart", heart);
        mSocket.off("pay", pay);
    }


    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isConnected) {
                        //连接成功
                        isConnected = true;
                        Toast.makeText(getApplicationContext(),
                                R.string.connect_success, Toast.LENGTH_LONG).show();
                        //发送登录请求
                        mSocket.emit("login", KLSocketBean.createAppLoginPackage("mengyuan", "test1", ++loginTag));

                    }
                }
            });
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isConnected = false;
                    Toast.makeText(getApplicationContext(),
                            R.string.connect_disconnect, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isConnected = false;
                    Toast.makeText(getApplicationContext(),
                            R.string.connect_failed, Toast.LENGTH_LONG).show();
                }
            });
        }
    };
    private Emitter.Listener login = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    Log.i("mengyuanio", "登录回调：" + data.toString());
                }
            });
        }
    };

    private Emitter.Listener heart = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    Log.i("mengyuanio", "心跳回调：" + data.toString());
                }
            });
        }
    };

    private Emitter.Listener pay = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    Log.i("mengyuanio", "支付回调：" + data.toString());
                }
            });
        }
    };

}
