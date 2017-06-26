package scut.carson_ho.socket_carson;

import android.util.Log;

import scut.carson_ho.socket_carson.utils.ByteUtils;

/**
 * Author：mengyuan
 * Date  : 2017/6/2上午11:33
 * E-Mail:mengyuanzz@126.com
 * Desc  :
 */

public class KLSocketBean {

    public static final int HEADER_LEN = 16;//packet包头协议长度 字节
    public static String DELIMITER = "www.ruwe.cn";

    // 总长度
    public int totalLenght;
    // 头长度
    public short headLenght;
    // 版本号
    public short version;
    // 操作类型
    public int operationType;
    // Tag
    public int tag;
    // 内容
    public String body;


    public byte[] parseString() {
        //分隔符
        byte[] heardBytes = DELIMITER.getBytes();
        //总长度 4字节
        byte[] totalLenghtBytes = ByteUtils.intToByteArray(totalLenght);
        //头长度 2字节
        byte[] headLenghtBytes = ByteUtils.shortToByteArray(headLenght);
        //版本号 2字节
        byte[] versionBytes = ByteUtils.shortToByteArray(version);
        //操作类型 4字节
        byte[] operationTypeByte = ByteUtils.intToByteArray(operationType);
        //Tag 4字节
        byte[] tagsByte = ByteUtils.intToByteArray(tag);
        //body
        byte[] bodybytes = body.getBytes();

        return ByteUtils.mergeBytes(heardBytes, totalLenghtBytes, headLenghtBytes, versionBytes, operationTypeByte, tagsByte, bodybytes);
    }

    /**
     * 创建登录对象
     *
     * @param tag
     * @return
     */
    public static byte[] createAppLoginPackage(String token, String key, int tag) {
        KLSocketBean bean = new KLSocketBean();
        bean.headLenght = KLSocketBean.HEADER_LEN;
        bean.version = BuildConfig.VERSION_CODE;
        bean.operationType = OperationType.TYPE_LOGIN;
        bean.tag = tag;
        bean.body = "{\"Token\":\"" + token + "\",\"Key\":\"" + key + "\"}";
        bean.totalLenght = bean.getTotalLenght();
        return bean.parseString();
    }

    /**
     * 创建请求心跳帧
     *
     * @param tag
     * @return
     */
    public static byte[] createAppHeartPackage(int tag) {
        KLSocketBean bean = new KLSocketBean();
        bean.headLenght = KLSocketBean.HEADER_LEN;
        bean.version = BuildConfig.VERSION_CODE;
        bean.operationType = OperationType.TYPE_HEART_APP;
        bean.tag = tag;
        bean.body = "";
        bean.totalLenght = bean.getTotalLenght();
        return bean.parseString();
    }

    public static KLSocketBean toSocketBean(byte[] resourceBytes) {
        String newString = new String(resourceBytes);
        Log.i("mengyuan--", "最初的字符串:" + newString);
        int position = newString.indexOf(DELIMITER);
        if (position < 0) {
            return null;
        }
        Log.i("mengyuan--", "分割符的位置:" + position);
        newString = newString.substring(position, newString.length());
        resourceBytes = newString.getBytes();
        Log.i("mengyuan--", "将分隔符之前的数据去掉:" + newString);
        int currentPosition = 0;
        byte[] heardBytes = new byte[DELIMITER.length()];
        byte[] totalLengthBytes = new byte[4];
        byte[] heartLengthBytes = new byte[2];
        byte[] versionBytes = new byte[2];
        byte[] operationTypeBytes = new byte[4];
        byte[] tagBytes = new byte[4];

        //分隔符集合
        System.arraycopy(resourceBytes, 0, heardBytes, 0, DELIMITER.length());
        String delimiter = new String(heardBytes);
        Log.i("mengyuan--", "解析完成的分隔符:" + delimiter);
        currentPosition += DELIMITER.length();
        Log.i("mengyuan--", "currentPosition:" + currentPosition);

        //总长-4个字节
        System.arraycopy(resourceBytes, currentPosition, totalLengthBytes, 0, 4);
        int totalLength = ByteUtils.byteArrayToInt(totalLengthBytes);
        Log.i("mengyuan--", "总长:" + totalLength);
        //如果数据小于总长，证明被拆包，等待下个包到来
        if (totalLength + DELIMITER.length() > resourceBytes.length) {
            return null;
        }
        currentPosition += 4;
        Log.i("mengyuan--", "currentPosition:" + currentPosition);

        //头长-2个字节
        System.arraycopy(resourceBytes, currentPosition, heartLengthBytes, 0, 2);
        short headLength = ByteUtils.byteArrayToShort(heartLengthBytes);
        Log.i("mengyuan--", "头长:" + headLength);
        currentPosition += 2;
        Log.i("mengyuan--", "currentPosition:" + currentPosition);
        //初始化body数组
        byte[] bodyBytes = new byte[totalLength - headLength];

        //版本号-2个字节
        System.arraycopy(resourceBytes, currentPosition, versionBytes, 0, 2);
        short versionCode = ByteUtils.byteArrayToShort(versionBytes);
        Log.i("mengyuan--", "版本号:" + versionCode);
        currentPosition += 2;
        Log.i("mengyuan--", "currentPosition:" + currentPosition);

        //操作类型-4个字节
        System.arraycopy(resourceBytes, currentPosition, operationTypeBytes, 0, 4);
        int operationType = ByteUtils.byteArrayToInt(operationTypeBytes);
        Log.i("mengyuan--", "操作类型:" + operationType);
        currentPosition += 4;
        Log.i("mengyuan--", "currentPosition:" + currentPosition);

        //Tag-4个字节
        System.arraycopy(resourceBytes, currentPosition, tagBytes, 0, 4);
        int tag = ByteUtils.byteArrayToInt(tagBytes);
        Log.i("mengyuan--", "tag:" + tag);
        currentPosition += 4;
        Log.i("mengyuan--", "currentPosition:" + currentPosition);

        //Body-剩余部分
        System.arraycopy(resourceBytes, currentPosition, bodyBytes, 0, totalLength - headLength);
        String body = new String(bodyBytes);
        Log.i("mengyuan--", "body:" + body);
        currentPosition += resourceBytes.length - currentPosition;
        Log.i("mengyuan--", "currentPosition:" + currentPosition);
        Log.i("mengyuan--", "bytes.length:" + resourceBytes.length);


        KLSocketBean bean = new KLSocketBean();
        bean.body = body;
        bean.totalLenght = totalLength;
        bean.tag = tag;
        bean.operationType = operationType;
        bean.headLenght = headLength;
        bean.version = versionCode;
        return bean;
    }

    public int getTotalLenght() {
        return headLenght + body.length();
    }

    @Override
    public String toString() {
        return "Packet{" +
                "totalLenght=" + totalLenght +
                ", headLenght=" + headLenght +
                ", version=" + version +
                ", operationType=" + operationType +
                ", tag=" + tag +
                ", body=" + body +
                '}';
    }
}
