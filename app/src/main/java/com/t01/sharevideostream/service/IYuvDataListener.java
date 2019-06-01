package com.t01.sharevideostream.service;

public interface IYuvDataListener {
    void onYUVData(byte[] mBuffer, int width, int height);
}
