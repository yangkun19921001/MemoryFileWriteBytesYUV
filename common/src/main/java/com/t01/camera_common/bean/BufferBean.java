package com.t01.camera_common.bean;


import android.util.Log;

public class BufferBean {
	
    public byte[] isCanRead; // 1 can 0 no
    public byte[] mBuffer; // adas buffer

    public BufferBean(int bufferSize) {
       Log.d("BufferBean ", "bufferSize:" + bufferSize);
        // init data
        isCanRead = new byte[1];

        if (bufferSize > 0) {
            mBuffer = new byte[bufferSize];
        }

        for (int i = 0; i < mBuffer.length; i++) {
            mBuffer[i] = 0;
        }
    }

}
