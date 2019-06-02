package com.t01.camera_service.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;

import com.t01.camera_common.Constants;
import com.t01.camera_common.FastYUVtoRGB;
import com.t01.camera_common.MemoryFileHelper;
import com.t01.camera_common.Utils;
import com.t01.camera_common.bean.BufferBean;
import com.t01.cameracore.ICameraCoreService;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 服务端与客服端管理内存的帮组类
 */
public class MemoryFileServiceManager {

    private static Context context;
    private static MemoryFileServiceManager insta;

    private ICameraCoreService mCameraService = null;
    private ParcelFileDescriptor mParcelFileDescriptor;
    private String TAG = "MemoryFileServiceManager";
    private MemoryFile mMemoryFile;
    /**
     * 定义一个装 YUV 的队列
     */
    public ArrayBlockingQueue<byte[]> mYUVQueue = new ArrayBlockingQueue<byte[]>(Constants.YUV_QUEUE_SIZE);
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private WindowManager mWindowManager;
    private ImageView imageView;
    private FastYUVtoRGB fastYUVtoRGB;

    public static MemoryFileServiceManager getInsta(Context context) {
        MemoryFileServiceManager.context = context.getApplicationContext();
        if (insta == null)
            insta = new MemoryFileServiceManager();
        return insta;
    }

    /**
     * 初始化内存块
     */
    public MemoryFile initMemoryFile(String name, int size) {
        return MemoryFileHelper.createMemoryFile(name, size);
    }

    /**
     * 设置是否给客服端发送数据
     *
     * @param b
     */
    public void setSendVideoFrame(boolean b, Intent intent) {
        Constants.IS_SEND_VIDEO_FRAME = b;
        //如果为 true ,那么服务端初始化一系列操作
        if (b) {
            sendVideoFrame(intent);
        } else {
            stopSend();
        }
    }

    /**
     * 释放摄像头资源
     */
    private void stopSend() {
        Constants.IS_SEND_VIDEO_FRAME = false;
        try {
            context.unbindService(mCameraServiceConnection);
            sendBroadcast(Constants.ACTION_FEEDBACK, "断开连接");
            mSurfaceView.getHolder().removeCallback(null);
/*            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.lock();
            mCamera.release();
            mCamera = null;*/
        } catch (Exception e) {

        }
    }

    private void sendVideoFrame(Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            Bundle extras = intent.getExtras();
            //获取需要预览的宽
            Constants.PREVIEWHEIGHT = extras.getInt(Constants.Config.PREVIEW_WIDTH, 1280);
            //获取需要预览的高
            Constants.PREVIEWHEIGHT = extras.getInt(Constants.Config.PREVIEW_HEIGHT, 720);
            //需要绑定对方服务的进程
            Constants.BIND_OTHER_SERVICE_PCK = extras.getString(Constants.Config.BIND_OTHER_SERVICE_PCK, "");
            //需要绑定对方服务的全路径
            Constants.BIND_OTHER_SERVICE_CLASS = extras.getString(Constants.Config.BIND_OTHER_SERVICE_CLASS, "");
            //需要开启 Camera ID 的前置还是后置 0：后置 1：前置
            Constants.CAMERA_ID = extras.getInt(Constants.Config.CAMERA_ID, 0);
        }
        //开启摄像头
        if (mCamera == null)
            openCamera();
        //初始化内存空间
        mMemoryFile = initMemoryFile(Constants.MEMORY_FILE_NAME, Constants.MEMORY_SIZE);
        //绑定本地服务端 通过 AIDL 把文件描述提供给 第三方软件
        bindOtherService();
    }


    /**
     * 绑定对方服务，提供 文件描述符
     */
    private void bindOtherService() {
        try {
            if (TextUtils.isEmpty(Constants.BIND_OTHER_SERVICE_PCK) || TextUtils.isEmpty(Constants.BIND_OTHER_SERVICE_CLASS))
                throw new NullPointerException("PCK or CLSS is null ?");
            Intent intent = new Intent();
            ComponentName cmp = new ComponentName(Constants.BIND_OTHER_SERVICE_PCK, Constants.BIND_OTHER_SERVICE_CLASS);
            intent.setComponent(cmp);
            context.bindService(intent, mCameraServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }


    /**
     * @return 是否给客服端发送数据
     */
    public boolean isSendVideoFrame() {
        return Constants.IS_SEND_VIDEO_FRAME;
    }


    private ServiceConnection mCameraServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            Log.d(TAG, "-------------------- onServiceConnected------------------");
            mCameraService = ICameraCoreService.Stub.asInterface(binder);
            if (mMemoryFile != null) {
                try {
                    //反射拿到文件描述符号
                    mParcelFileDescriptor = MemoryFileHelper.getParcelFileDescriptor(mMemoryFile);
                    if (mParcelFileDescriptor != null) {
                        mCameraService.addExportMemoryFile(mParcelFileDescriptor, Constants.PREVIEWWIDTH, Constants.PREVIEWHEIGHT, Constants.MEMORY_SIZE);
                    }
                    //开始写入数据
                    Thread thread = new Thread(new SendVideo(mMemoryFile));
                    thread.start();
                    sendBroadcast(Constants.ACTION_FEEDBACK, "连接成功");
                } catch (RemoteException ioe) {
                    Log.d(TAG, "handleMessage ------------------ MSG_ADD_EXPORT_MEMORY_FILE - IOException : "
                            + ioe.getMessage());
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "-------------------- onServiceDisconnected------------------");
            mCameraService = null;
            //连接断开停止发送视频流
            stopSend();
        }
    };

    private class SendVideo implements Runnable {
        private MemoryFile memoryFile;

        public SendVideo(MemoryFile memoryFile) {
            this.memoryFile = memoryFile;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (!Constants.IS_SEND_VIDEO_FRAME) {
                        Thread.sleep(200);
                        continue;
                    }
                    writeBytes(memoryFile);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 读标志位 写入视频流
     *
     * @param memoryFile
     */
    public void writeBytes(MemoryFile memoryFile) {
        try {
            if (mYUVQueue.size() > 0) {
                BufferBean mBufferBean = new BufferBean(Constants.BUFFER_SIZE);
                //读取标志符号
                memoryFile.readBytes(mBufferBean.isCanRead, 0, 0, 1);
                //当第一位为 0 的时候，代表客服端已经读取了，可以正常将视频流写入内存中
                if (mBufferBean.isCanRead[0] == 0) {
                    //拿到视频流
                    byte[] video = mYUVQueue.poll();
                    if (video != null)
                        //将视频流写入内存中
                        memoryFile.writeBytes(video, 0, 0, video.length);
                    //标志位复位，等待客服端读取视频流
                    mBufferBean.isCanRead[0] = 1;
                    memoryFile.writeBytes(mBufferBean.isCanRead, 0, 0, 1);
                } else {
                    Log.d(TAG, "readShareBufferMsg isCanRead:" + mBufferBean.isCanRead[0] + ";length:"
                            + mBufferBean.mBuffer.length);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            sendBroadcast(Constants.ACTION_FEEDBACK, e.getMessage());
        }
    }

    public void openCamera() {
        //权限判断 是否有悬浮权限
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(context)) {
                //启动Activity让用户授权
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                context.startActivity(intent);
                return;
            } else {
                //执行6.0以上绘制代码
                /**
                 *  后台悬浮采集视频流
                 */
                onBackgroup();
            }
        } else {
            //执行6.0以下绘制代码
            /**
             *  后台悬浮采集视频流
             */
            onBackgroup();
        }

        fastYUVtoRGB = new FastYUVtoRGB(context);
    }

    /**
     * 后台采集视频流  模拟执法仪相机录像功能
     */
    public void onBackgroup() {
        try {
            //开启悬浮窗
            mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            imageView = new ImageView(context);
            mSurfaceView = new SurfaceView(context);
            int LAYOUT_FLAG;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
            }
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    400,
                    400,
                    LAYOUT_FLAG,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, //给屏幕焦点
                    PixelFormat.TRANSLUCENT
            );

            layoutParams.gravity = Gravity.LEFT | Gravity.TOP;

            mWindowManager.addView(mSurfaceView, layoutParams);
            mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    initCamera();
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {

                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    private void initCamera() {
        try {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        if (null == mCamera)
            mCamera = Camera.open(Constants.CAMERA_ID);
        Log.i(TAG, "onCreate: open");
        Camera.Parameters parameters = mCamera.getParameters();
        //指定 NV21 格式
        parameters.setPreviewFormat(ImageFormat.NV21);
        parameters.setPreviewSize(Constants.PREVIEWWIDTH, Constants.PREVIEWHEIGHT);
        mCamera.setParameters(parameters);

            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(mSurfaceView.getHolder());
        } catch (IOException e) {
            Log.i(TAG, "错误--" + e.getMessage());
        }
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                try {
                    imageView.setImageBitmap(fastYUVtoRGB.convertYUVtoRGB(data, Constants.PREVIEWWIDTH, Constants.PREVIEWHEIGHT));
                    if (Constants.IS_SEND_VIDEO_FRAME) {
                        putYUVData(data);
                    }
                    camera.addCallbackBuffer(data);
                    Log.e(TAG, "采集 YUV 数据大小 " + Utils.getVideoFrameSize(data.length));
                } catch (Exception e) {
                    Log.d(TAG, "onPreviewFrame--Exception--" + e.getMessage());
                }
            }
        });
        mCamera.startPreview();
    }

    public void putYUVData(byte[] buffer) {

        if (mYUVQueue.size() >= Constants.YUV_QUEUE_SIZE) {
            mYUVQueue.poll();
        }
        mYUVQueue.add(buffer);
    }

    public void sendBroadcast(String action, String content) {
        Intent intent = new Intent();
        intent.setAction(action);
        ComponentName componentName = new ComponentName("com.t01.sharevideostream",
                "com.t01.sharevideostream.revices.FeedBackReceiver");
        intent.setComponent(componentName);
        Bundle extras = new Bundle();
        //设置绑定本地服务的全路径
        extras.putString(Constants.ACTION_FEEDBACK_CONTENT, content);
        intent.putExtras(extras);
        context.sendBroadcast(intent);
    }

}
