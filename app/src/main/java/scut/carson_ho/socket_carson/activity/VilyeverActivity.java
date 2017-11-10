package scut.carson_ho.socket_carson.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import scut.carson_ho.socket_carson.KLSocketBean;
import scut.carson_ho.socket_carson.R;
import scut.carson_ho.socket_carson.controller.KLVilyeverController;

/**
 * Author：mengyuan
 * Date  : 2017/6/2下午7:04
 * E-Mail:mengyuanzz@126.com
 * Desc  :
 */

public class VilyeverActivity extends Activity implements View.OnClickListener {


    // 连接 断开连接 发送数据到服务器 的按钮变量
    private Button btnConnect, btPay, btLogin, stop, start_heart;

    // 显示接收服务器消息 按钮
    private TextView tv_server_message;



    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] bytes = (byte[]) msg.obj;
            KLSocketBean bean = KLSocketBean.toSocketBean(bytes);
            if (bean == null) {
                Log.i("mengyuansocket", "解析对象失败");
                return;
            }
            tv_server_message.setText(bean.toString());
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect://连接
                KLVilyeverController.getInstance().connectionSocket("192.168.199.125", 9999, handler);
                break;
            case R.id.pay://支付
                break;
            case R.id.start_heart://启动心跳
                KLVilyeverController.getInstance().sendHeart();
                break;
            case R.id.stop://停止
                KLVilyeverController.getInstance().stop();
                break;
            case R.id.send://登录
                KLVilyeverController.getInstance().login();
                break;
        }
    }
}
