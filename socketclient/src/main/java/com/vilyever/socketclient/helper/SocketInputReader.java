package com.vilyever.socketclient.helper;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * SocketInputReader
 * AndroidSocketClient <com.vilyever.socketclient.util>
 * Created by vilyever on 2016/4/11.
 * Feature:
 */
public class SocketInputReader extends Reader {

    private InputStream inputStream;


    /* Constructors */
    public SocketInputReader(InputStream inputStream) {
        super(inputStream);
        this.inputStream = inputStream;
    }

    /* Public Methods */
    
    
    /* Properties */

    /* Overrides */
    @Override
    public void close() throws IOException {
        synchronized (lock) {
            if (this.inputStream != null) {
                this.inputStream.close();
                this.inputStream = null;
            }
        }
    }

    @Override
    public int read(char[] buffer, int offset, int count) throws IOException {
        throw new IOException("read() is not support for SocketInputReader, try readBytes().");
    }

    public byte[] readData() throws IOException {
        synchronized (lock) {
            if (!__i__isOpen()) {
                throw new IOException("InputStream is closed");
            }
            try {
                Log.i("mengyuansocket", "--------开始读取数据--------");
                int count = 0;
                while (count == 0) {
                    count = inputStream.available();
                }
                byte[] b = new byte[count];
                inputStream.read(b);
                Log.i("mengyuansocket", "读取数据成功----------------" + b.toString());
                return b;
            } catch (IOException e) {
                return null;
            }
        }
    }



    @Override
    public boolean ready() throws IOException {
        synchronized (lock) {
            if (this.inputStream == null) {
                throw new IOException("InputStream is closed");
            }
            try {
                return this.inputStream.available() > 0;
            } catch (IOException e) {
                return false;
            }
        }
    }

    /* Delegates */


    /* Private Methods */
    public static void __i__checkOffsetAndCount(int arrayLength, int offset, int count) {
        if ((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count) {
            throw new ArrayIndexOutOfBoundsException("arrayLength=" + arrayLength + "; offset=" + offset
                    + "; count=" + count);
        }
    }

    private boolean __i__isOpen() {
        return this.inputStream != null;
    }
}