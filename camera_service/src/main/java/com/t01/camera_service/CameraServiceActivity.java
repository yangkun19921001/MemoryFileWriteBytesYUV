package com.t01.camera_service;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.t01.camera_service.utils.MemoryFileServiceManager;

public class CameraServiceActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * 相机默认打开的情况下
         */
        findViewById(R.id.btn_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MemoryFileServiceManager.getInsta(getApplicationContext()).openCamera();
            }
        });
    }

}
