package scut.carson_ho.socket_carson;

/**
 * Author：mengyuan
 * Date  : 2017/6/2下午7:00
 * E-Mail:mengyuanzz@126.com
 * Desc  :
 */

public interface SocketConfig {

    //发送心跳帧间隔
    int HEART_SEND_TIME = 8*1000;
    //连接超时时间
    int CONNECTION_TIME = 15*1000;
    //读取超时时间
    int READT_TIME = 15*1000;
}
