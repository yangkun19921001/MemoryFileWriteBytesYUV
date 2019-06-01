package com.t01.camera_common.manager;

import android.content.Context;
import android.os.MemoryFile;

import com.t01.camera_common.Constants;
import com.t01.camera_common.MemoryFileHelper;

/**
 * 服务端与客服端管理内存的帮组类
 */
public class MemoryFileManager {

    private static Context context;
    private static MemoryFileManager insta;

    private String TAG = "MemoryFileManager";

    public static MemoryFileManager getInsta(Context context) {
        MemoryFileManager.context = context.getApplicationContext();
        if (insta == null)
            insta = new MemoryFileManager();
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
    public void setSendVideoFrame(boolean b) {
        Constants.IS_SEND_VIDEO_FRAME = b;
        //如果为 true ,那么服务端初始化内存
        if (b){
            sendVideoFrame();
        }else {
            Constants.IS_SEND_VIDEO_FRAME = false;
        }
    }

    private void sendVideoFrame() {
        //初始化内存空间
        initMemoryFile(Constants.MEMORY_FILE_NAME,Constants.MEMORY_SIZE);
    }


    /**
     *
     * @return 是否给客服端发送数据
     */
    public boolean isSendVideoFrame() {
        return Constants.IS_SEND_VIDEO_FRAME;
    }

    /**
     * 收到客服端需要视频流的初始化
     * 需要绑定客服端服务，并解析传递过来的数据
     */
    public void revicesLocalService(){

    }



}
