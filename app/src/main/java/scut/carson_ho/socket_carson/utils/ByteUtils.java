package scut.carson_ho.socket_carson.utils;

import android.util.Log;

/**
 * Author：mengyuan
 * Date  : 2017/6/2下午2:43
 * E-Mail:mengyuanzz@126.com
 * Desc  :
 */

public class ByteUtils {

    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        //由高位到低位
        result[0] = (byte) ((i >> 24) & 0xFF);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }

    public static byte[] shortToByteArray(short i) {
        byte[] result = new byte[2];
        //由高位到低位
        result[0] = (byte) ((i >> 8) & 0xFF);
        result[1] = (byte) (i & 0xFF);
        return result;
    }

    //byte 数组与 int 的相互转换
    public static int byteArrayToInt(byte[] b) {
        if (b == null || b.length < 4) {
            return -1;
        }
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    //byte 数组与 int 的相互转换
    public static short byteArrayToShort(byte[] b) {
        if (b == null || b.length < 2) {
            return -1;
        }
        return (short) (b[1] & 0xff | (b[0] & 0xFF) << 8);
    }

    public static String mergeBytestoString(byte[]... bytes) {
        //计算出总长度
        int totalLength = 0;
        for (int i = 0; i < bytes.length; i++) {
            totalLength += bytes[i].length;
        }
        byte[] bytes1 = new byte[totalLength];
        int currentLength = 0;
        for (int i = 0; i < bytes.length; i++) {
            for (int i1 = 0; i1 < bytes[i].length; i1++) {
                bytes1[currentLength] = bytes[i][i1];
                currentLength++;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < bytes1.length; i++) {
            stringBuilder.append(String.valueOf(bytes1[i]));
        }
        return stringBuilder.toString();
    }


    public static byte[] mergeBytes(byte[]... bytes) {
        //计算出总长度
        int totalLength = 0;
        for (int i = 0; i < bytes.length; i++) {
            totalLength += bytes[i].length;
        }
        byte[] bytes1 = new byte[totalLength];
        int currentLength = 0;
        for (int i = 0; i < bytes.length; i++) {
            for (int i1 = 0; i1 < bytes[i].length; i1++) {
                bytes1[currentLength] = bytes[i][i1];
                currentLength++;
            }
        }

        return bytes1;
    }
}
