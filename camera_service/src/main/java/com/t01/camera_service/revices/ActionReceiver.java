package com.t01.camera_service.revices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.t01.camera_common.Constants;
import com.t01.camera_service.utils.MemoryFileServiceManager;

public class ActionReceiver extends BroadcastReceiver {
    private String TAG = this.getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "ActionReceiver ------- action : " + intent.getAction());
        if (intent != null && !TextUtils.isEmpty(intent.getAction())) {
            onHandleAction(context, intent);
        } else {
            Log.e(TAG, "ActionReceiver ------- intent == null");
        }
    }

    private void onHandleAction(Context context, Intent intent) {
        switch (intent.getAction()) {
            /**
             * 需要子码流
             */
            case Constants.ACTION_CAMERE_CORE_SHOW:
                //如果正在发送视频流，就不需要执行后面代码了
                if (!MemoryFileServiceManager.getInsta(context).isSendVideoFrame())
                    MemoryFileServiceManager.getInsta(context).setSendVideoFrame(true, intent);
                break;

            /**
             * 不需要子码流
             */
            case Constants.ACTION_CAMERE_CORE_HIDE:
                    MemoryFileServiceManager.getInsta(context).setSendVideoFrame(false, intent);
                break;
        }
    }
}