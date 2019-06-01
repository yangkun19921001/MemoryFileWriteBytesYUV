package com.t01.sharevideostream.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.t01.camera_common.Constants;
import com.t01.camera_common.MemoryFileHelper;
import com.t01.camera_common.bean.BufferBean;
import com.t01.cameracore.ICameraCoreService;

import java.io.IOException;

public class LocalService extends Service {

    private MyBinder mMyBinder = null;
    private MemoryFile mMemoryFileService;
    private MyHandler mHandler;

    private static final int MSG_ADD_EXPORT_MEMORY_FILE = 1;
    private static final int MEG_READ_BUF = 2;

    private String TAG = this.getClass().getSimpleName();

    private IYuvDataListener mIYuvDataListener;
    private BufferBean mBufferBean;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMyBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMyBinder = new MyBinder();
        mHandler = new MyHandler();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }

    public class MyBinder extends ICameraCoreService.Stub {

        @Override
        public void addExportMemoryFile(ParcelFileDescriptor pfd, int w, int h, int memorySize) throws RemoteException {
            //拿到服务端创建的内存块 ，客服端去读写
           Log.i(TAG,"收到服务端返回过来的内存块信息-->" + "预览宽：" +w + " 预览高：" + h + " 内存大小：" + memorySize);
            mMemoryFileService = MemoryFileHelper.openMemoryFile(pfd, memorySize, MemoryFileHelper.OPEN_READWRITE);
            mBufferBean = new BufferBean(memorySize);
            sendMessage(w,h);
        }

        public  void getYuvData(IYuvDataListener iYuvDataListener){
            mIYuvDataListener = iYuvDataListener;
        }
    }

    public void sendMessage(int w, int h) {
        Message obtain = Message.obtain();
        Bundle bundle = new Bundle();
        obtain.what = MEG_READ_BUF;
        bundle.putInt(Constants.Config.PREVIEW_HEIGHT,h);
        bundle.putInt(Constants.Config.PREVIEW_WIDTH,w);
        obtain.setData(bundle);
        mHandler.sendMessage(obtain);
    }

    public void sendMessageDelayed(int width, int height, int delay) {
        Message obtain = Message.obtain();
        obtain.what = MEG_READ_BUF;
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.Config.PREVIEW_HEIGHT,height);
        bundle.putInt(Constants.Config.PREVIEW_WIDTH,width);
        obtain.setData(bundle);
        mHandler.sendMessageDelayed(obtain, delay);
    }


    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MEG_READ_BUF:
                    if (msg.getData() != null){
                        int width = msg.getData().getInt(Constants.Config.PREVIEW_WIDTH);
                        int height = msg.getData().getInt(Constants.Config.PREVIEW_HEIGHT);
                        readShareBufferMsg(width,height);
                    }
                    break;
            }
        }
    }

    private void readShareBufferMsg(int width, int height) {
        try {
            if (mMemoryFileService != null) {
                mMemoryFileService.readBytes(mBufferBean.isCanRead, 0, 0, 1);
                Log.d(TAG, "readShareBufferMsg isCanRead:" + mBufferBean.isCanRead[0] + ";length:"
                        + mBufferBean.mBuffer.length + ":测试数据 ：" +  mBufferBean.mBuffer[2]);
                if (mBufferBean.isCanRead[0] == 1) {
                    mMemoryFileService.readBytes(mBufferBean.mBuffer, 0, 0, mBufferBean.mBuffer.length);
                    // 显示
                    showCameraBuff(mBufferBean.mBuffer,width,height);
                    mBufferBean.isCanRead[0] = 0;
                    mMemoryFileService.writeBytes(mBufferBean.isCanRead, 0, 0, 1);
                } else {
                    Log.d(TAG, "readShareBufferMsg isCanRead:" + mBufferBean.isCanRead[0] + ";length:"
                            + mBufferBean.mBuffer.length);
                }
                sendMessageDelayed(width,height,20);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showCameraBuff(byte[] mBuffer, int width, int height) {
        if (mIYuvDataListener != null)
            mIYuvDataListener.onYUVData(mBuffer,width,height);
    }
}
