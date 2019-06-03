package com.t01.sharevideostream;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.t01.camera_common.Constants;
import com.t01.camera_common.FastYUVtoRGB;
import com.t01.sharevideostream.service.IYuvDataListener;
import com.t01.sharevideostream.service.LocalService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = this.getClass().getSimpleName();
    private ImageView mYuvShow;
    private FastYUVtoRGB mFastYUVtoRGB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去除状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        bindService(new Intent(this, LocalService.class), mCameraServiceConnection, Context.BIND_AUTO_CREATE);

        findViewById(R.id.btn_start).setOnClickListener(this);
        findViewById(R.id.btn_stop).setOnClickListener(this);
        mYuvShow = findViewById(R.id.iv_yuv);
        mFastYUVtoRGB = new FastYUVtoRGB(getApplicationContext());
    }


    private ServiceConnection mCameraServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            Log.d(TAG, "-------------------- onServiceConnected------------------");
            LocalService.MyBinder myBinder = (LocalService.MyBinder) binder;
            myBinder.getYuvData(new IYuvDataListener() {
                @Override
                public void onYUVData(byte[] output, int width, int height) {
//                    Log.e(TAG, "收到 YUV 数据大小 " + Utils.getVideoFrameSize(output.length));
                    mYuvShow.setImageBitmap(mFastYUVtoRGB.rotaingImageView(90,mFastYUVtoRGB.convertYUVtoRGB(output, width, height)));
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "-------------------- onServiceDisconnected------------------");
        }
    };





    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_start) {
            sendBroadcast(Constants.ACTION_CAMERE_CORE_SHOW);
        } else if (v.getId() == R.id.btn_stop) {
            sendBroadcast(Constants.ACTION_CAMERE_CORE_HIDE);
        }

    }


    public void sendBroadcast(String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        ComponentName componentName = new ComponentName("com.t01.camera_service",
                "com.t01.camera_service.revices.ActionReceiver");
        intent.setComponent(componentName);
        Bundle extras = new Bundle();
        //设置需要预览的宽
        extras.putInt(Constants.Config.PREVIEW_WIDTH, 1280);
        //设置需要预览的宽
        extras.putInt(Constants.Config.CAMERA_ID, 0);
        //设置需要预览的高
        extras.putInt(Constants.Config.PREVIEW_HEIGHT, 720);
        //设置绑定本地进程
        extras.putString(Constants.Config.BIND_OTHER_SERVICE_PCK, getPackageName());
        //设置绑定本地服务的全路径
        extras.putString(Constants.Config.BIND_OTHER_SERVICE_CLASS, "com.t01.sharevideostream.service.LocalService");
        //设置绑定本地广播进程
        extras.putString(Constants.Config.BIND_OTHER_BROADCAST_PCK, "com.t01.sharevideostream");
        //设置绑定本地广播的全路径
        extras.putString(Constants.Config.BIND_OTHER_BROADCAST_CLASS, "com.t01.sharevideostream.revices.FeedBackReceiver");
        intent.putExtras(extras);
        sendBroadcast(intent);
    }
}
