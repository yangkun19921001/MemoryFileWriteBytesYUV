package com.t01.camera_common;

public class Constants {

    /***
     *  需要 camera 视频流数据
     */
    public static final String ACTION_CAMERE_CORE_SHOW = "com.t01.action.CAMERE_CORE_SHOW";

    /**
     * 停止接收 camera 视频数据流
     */
    public static final String ACTION_CAMERE_CORE_HIDE = "com.t01.action.CAMERA_CORE_HIDE";
    /**
     * 接收来至服务端的反馈 action
     */
    public static final String ACTION_FEEDBACK = "com.t01.action.CAMERE_FEEDBACK";
    /**
     * 接收来至服务端的反馈内容
     */
    public static final String ACTION_FEEDBACK_CONTENT = "ACTION_FEEDBACK_CONTENT";
    /**
     * 当前摄像头前后置
     */
    public static int CAMERA_ID = 0;
    /**
     * 对方服务的进程
     */
    public static String BIND_OTHER_SERVICE_PCK = "";
    /**
     * 对方服务的全路径
     */
    public static String BIND_OTHER_SERVICE_CLASS = "";

    /**
     * 接收反馈广播的进程
     */
    public static String BIND_OTHER_BROADCAST_PCK = "";

    /**
     * 接收反馈广播的全路径
     */
    public static String BIND_OTHER_BROADCAST_CLASS = "";

    /**
     * YUV 缓存 5 帧
     */
    public static final int YUV_QUEUE_SIZE = 2;
    /**
     * 客服端发过来的心跳包
     */
    public static final String ACTION_HEARTBEAT = "ACTION_HEARTBEAT";

    /**
     * 是否发送视频流
     */
    public static boolean IS_SEND_VIDEO_FRAME = false;

    /**
     * 预览的宽度
     */
    public static int PREVIEWWIDTH = 1280;
    /**
     * 预览的高度
     */
    public static int PREVIEWHEIGHT = 720;

    /**
     * 默认 720 一帧的大小
     */
    public static int BUFFER_SIZE = PREVIEWWIDTH * PREVIEWHEIGHT * 3 / 2;// 1382400;
    /**
     * 内存文件中的缓存的大小
     */
    public static final int MEMORY_SIZE = BUFFER_SIZE * YUV_QUEUE_SIZE;
    /**
     * 内存文件的名称
     */
    public static String MEMORY_FILE_NAME = "CAMERA_SERVICE";


    /**
     * 发送过来的视频信息
     */
    public interface Config {
        /**
         * 0-后置，1-前置
         * 相机方向
         */
        String CAMERA_ID = "CAMERA_ID";

        /**
         * 开启相机的参数 预览宽度值
         */
        String PREVIEW_WIDTH = "PREVIEW_WIDTH";

        /**
         * 开启相机的参数 预览高度值
         */
        String PREVIEW_HEIGHT = "PREVIEW_HEIGHT";

        /**
         * 绑定对方服务的进程
         */
        String BIND_OTHER_SERVICE_PCK = "BIND_OTHER_SERVICE_PCK";
        /**
         * 绑定对方服务的全路径
         */
        String BIND_OTHER_SERVICE_CLASS = "BIND_OTHER_SERVICE_CLASS";
        /**
         * 绑定反馈广播的进程
         */
        String BIND_OTHER_BROADCAST_PCK = "BIND_OTHER_FEEDBACK_BROADCAST_PCK";
        /**
         * 绑定反馈广播的全路径
         */
        String BIND_OTHER_BROADCAST_CLASS = "BIND_OTHER_FEEDBACK_BROADCAST_CLASS";

    }

}
