package scut.carson_ho.socket_carson.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import scut.carson_ho.socket_carson.KLSocketBean;
import scut.carson_ho.socket_carson.R;

public class MyTestActivity extends AppCompatActivity {

    /**
     * 主 变量
     */

    // 主线程Handler
    // 用于将从服务器获取的消息显示出来
    private Handler mMainHandler;

    // Socket变量
    private Socket socket;

    // 线程池
    // 为了方便展示,此处直接采用线程池进行线程管理,而没有一个个开线程
    private ExecutorService mThreadPool;

    /**
     * 接收服务器消息 变量
     */
    // 输入流对象
    InputStream is;

    // 输入流读取器对象
    InputStreamReader isr;
    BufferedReader br;

    // 接收服务器发送过来的消息
    String response;

    /**
     * 发送消息到服务器 变量
     */
    // 输出流对象
    OutputStream outputStream;


    // 连接 断开连接 发送数据到服务器 的按钮变量
    private Button btnConnect, btPay, btLogin;

    // 显示接收服务器消息 按钮
    private TextView tv_server_message;


    private int tag = 0;


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
        tv_server_message = (TextView) findViewById(R.id.tv_server_message);
        // 初始化线程池
        mThreadPool = Executors.newCachedThreadPool();


        // 实例化主线程,用于更新接收过来的消息
        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        tv_server_message.setText(response);
                        break;
                }
            }
        };


        /**
         * 创建客户端 & 服务器的连接
         */
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 利用线程池直接开启一个线程 & 执行该线程
                mThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {

                        try {

                            // 创建Socket对象 & 指定服务端的IP 及 端口号
                            socket = new Socket("192.168.199.248", 9912);

                            // 判断客户端和服务器是否连接成功
                            System.out.println(socket.isConnected());

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });

        /**
         * 接收 服务器消息
         */
        btPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 利用线程池直接开启一个线程 & 执行该线程
                mThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            // 步骤1：创建输入流对象InputStream
                            is = socket.getInputStream();

                            // 步骤2：创建输入流读取器对象 并传入输入流对象
                            // 该对象作用：获取服务器返回的数据
                            isr = new InputStreamReader(is);
                            br = new BufferedReader(isr);

                            // 步骤3：通过输入流读取器对象 接收服务器发送过来的数据
                            response = br.readLine();
                            Log.i("mengyuan123", response);

                            // 步骤4:通知主线程,将接收的消息显示到界面
                            Message msg = Message.obtain();
                            msg.what = 0;
                            mMainHandler.sendMessage(msg);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });

            }
        });


        /**
         * 发送消息 给 服务器
         */
        btLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 利用线程池直接开启一个线程 & 执行该线程
                mThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            // 步骤1：从Socket 获得输出流对象OutputStream
                            // 该对象作用：发送数据
                            outputStream = socket.getOutputStream();
                            KLSocketBean bean = new KLSocketBean();
                            bean.headLenght = KLSocketBean.HEADER_LEN;
                            bean.version = 1;
                            bean.operationType = 7;
                            bean.tag = tag;
                            bean.body = "{\"Token\":\"zhongjin\",\"Key\":\"test\"}";
                            bean.totalLenght = bean.getTotalLenght();
                            // 步骤2：写入需要发送的数据到输出流对象中
                            outputStream.write(bean.parseString());
                            // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞
                            System.out.println(Arrays.toString(bean.parseString()));
                            // 步骤3：发送数据到服务端
                            outputStream.flush();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });

            }
        });


//        /**
//         * 断开客户端 & 服务器的连接
//         */
//        btPay.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                try {
//                    // 断开 客户端发送到服务器 的连接，即关闭输出流对象OutputStream
//                    outputStream.close();
//
//                    // 断开 服务器发送到客户端 的连接，即关闭输入流读取器对象BufferedReader
//                    br.close();
//
//                    // 最终关闭整个Socket连接
//                    socket.close();
//
//                    // 判断客户端和服务器是否已经断开连接
//                    System.out.println(socket.isConnected());
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//            }
//        });


    }
}
