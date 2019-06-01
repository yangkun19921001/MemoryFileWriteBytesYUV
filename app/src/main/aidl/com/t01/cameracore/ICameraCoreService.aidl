package com.t01.cameracore;

import android.os.ParcelFileDescriptor;

interface ICameraCoreService {
	void addExportMemoryFile(in ParcelFileDescriptor pfd,int w,int h,int memory);
}