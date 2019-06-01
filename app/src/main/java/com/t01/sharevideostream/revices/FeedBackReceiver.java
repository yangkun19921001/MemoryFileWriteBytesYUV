package com.t01.sharevideostream.revices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.t01.camera_common.Constants;

public class FeedBackReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction().equals(Constants.ACTION_FEEDBACK)) {
            if (intent.getExtras().getString(Constants.ACTION_FEEDBACK_CONTENT) != null) {
                Toast.makeText(context, intent.getExtras().getString(Constants.ACTION_FEEDBACK_CONTENT), Toast.LENGTH_SHORT).show();
            }
        }

    }
}
