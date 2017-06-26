package scut.carson_ho.socket_carson;

/**
 * Author：mengyuan
 * Date  : 2017/6/2下午3:57
 * E-Mail:mengyuanzz@126.com
 * Desc  :长连接操作类型
 */

public interface OperationType {

    //登录类型
    int TYPE_LOGIN = 7;
    //登录类型😄😄😄
    int TYPE_LOGIN_SERVER = 8;

    //心跳类型
    //客户端向服务器发送请求时的值
    int TYPE_HEART_APP = 2;
    //服务器返回的值
    int TYPE_HEART_SERVER = 3;

    //支付
    //客户端向服务器发送请求时的值
    int TYPE_PAY_APP = 16;
    //服务器返回的值
    int TYPE_PAY_SERVER = 17;
}
