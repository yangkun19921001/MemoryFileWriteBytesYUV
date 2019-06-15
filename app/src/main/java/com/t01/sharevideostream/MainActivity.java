package com.t01.sharevideostream;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.t01.camera_common.Constants;
import com.t01.camera_common.FastYUVtoRGB;
import com.t01.camera_common.utils.Utils;
import com.t01.sharevideostream.service.IYuvDataListener;
import com.t01.sharevideostream.service.LocalService;

import java.util.Date;

@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = this.getClass().getSimpleName();
    private ImageView mYuvShow;
    private FastYUVtoRGB mFastYUVtoRGB;

    private SimpleDateFormat sdf = new SimpleDateFormat("ss");

    /**
     * 显示 YUV 数据通知
     */
    private final int SHOW_YUV_NOTI = 0x110;
    private final String YUV_DATA = "YUV_DATA";

    private String pre = "";

    int count = 0;

    private Handler handlerYuv = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SHOW_YUV_NOTI:
                    String next = sdf.format(new Date());
                    byte[] nv21 = msg.getData().getByteArray(YUV_DATA);
                    int width = msg.getData().getInt(Constants.Config.PREVIEW_WIDTH);
                    int height = msg.getData().getInt(Constants.Config.PREVIEW_HEIGHT);
                    Log.e(TAG, "时间：" + next + " " + "收到 YUV 数据大小 " + Utils.getVideoFrameSize(nv21.length));
                    count = ++count;
                    if (!pre.equals(next)) {
                        pre = next;
                        tvCurrentFrameCount.setText("当前收到：" + count + " fps");
                        count = 0;
                    }
                    //执法仪本身就是 横屏 不需要在旋转了
                    mYuvShow.setImageBitmap(mFastYUVtoRGB.convertYUVtoRGB(nv21, width, height));
//                    mYuvShow.setImageBitmap(mFastYUVtoRGB.rotaingImageView(90,mFastYUVtoRGB.convertYUVtoRGB(output, width, height)));
                    break;
            }
        }
    };
    private TextView tvCurrentFrameCount;

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
        tvCurrentFrameCount = findViewById(R.id.tv_frame);
        mYuvShow = findViewById(R.id.iv_yuv);
        mFastYUVtoRGB = new FastYUVtoRGB(getApplicationContext());
    }


    private ServiceConnection mCameraServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, final IBinder binder) {
            Log.d(TAG, "-------------------- onServiceConnected------------------");
            LocalService.MyBinder myBinder = (LocalService.MyBinder) binder;
            myBinder.getYuvData(new IYuvDataListener() {
                @Override
                public void onYUVData(byte[] output, int width, int height) {
                    sendMessage(output, width, height);
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
            sendBroadcast(Constants.ACTION_CAMERE_CORE_SHOW, null, null);
//            sendBroadcast(Constants.ACTION_CAMERE_CORE_SHOW, "com.example.reach.reachlive","com.t01.camera_service.revices.ActionReceiver");
        } else if (v.getId() == R.id.btn_stop) {
//            sendBroadcast(Constants.ACTION_CAMERE_CORE_HIDE, "com.example.reach.reachlive","com.t01.camera_service.revices.ActionReceiver");
            sendBroadcast(Constants.ACTION_CAMERE_CORE_HIDE, null, null);
        }
    }


    /**
     * @param action 开启或者关闭的 Action
     * @param pck    服务端广播所在进程
     * @param cls    服务端广播全路径
     */
    public void sendBroadcast(String action, String pck, String cls) {
        Intent intent = new Intent();
        intent.setAction(action);
        ComponentName componentName = null;
        if (!TextUtils.isEmpty(pck) && !TextUtils.isEmpty(cls)) {
            componentName = new ComponentName(pck,
                    cls);
        } else {
            componentName = new ComponentName("com.t01.camera_service",
                    "com.t01.camera_service.revices.ActionReceiver");
        }
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

    /**
     * 发送显示 YUV
     *
     * @param nv21
     * @param width
     * @param height
     */
    private void sendMessage(byte[] nv21, int width, int height) {
        Message obtain = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putByteArray(YUV_DATA, nv21);
        bundle.putInt(Constants.Config.PREVIEW_WIDTH, width);
        bundle.putInt(Constants.Config.PREVIEW_HEIGHT, height);
        obtain.what = SHOW_YUV_NOTI;
        obtain.setData(bundle);
        handlerYuv.sendMessage(obtain);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handlerYuv != null) {
            handlerYuv.removeCallbacksAndMessages(null);
            handlerYuv = null;
        }
    }
}
