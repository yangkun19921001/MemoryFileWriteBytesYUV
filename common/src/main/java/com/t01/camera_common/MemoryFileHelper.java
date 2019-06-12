package com.t01.camera_common;

import android.os.Build;
import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;

import com.t01.camera_common.utils.ReflectUtil;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;

/**
 * 对memoryFile类的扩展 *
 * 1.从memoryFile对象中获取FileDescriptor,ParcelFileDescriptor *
 * 2.根据一个FileDescriptor和文件length实例化memoryFile对象 *
 */
public class MemoryFileHelper {

    private static final int PROT_READ = 0x1;
    private static final int PROT_WRITE = 0x2;
    public static final int OPEN_READONLY = PROT_READ;
    public static final int OPEN_READWRITE = PROT_READ | PROT_WRITE;

    /**
     *     
     * 创建共享内存对象
     *
     * @param name   描述共享内存文件名称    
     * @param length 用于指定创建多大的共享内存对象    
     * @return MemoryFile 描述共享内存对象    
     */

    public static MemoryFile createMemoryFile(String name, int length) {
        try {
            return new MemoryFile(name, length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static MemoryFile openMemoryFile(ParcelFileDescriptor pfd, int length, int mode) {
        if (pfd == null) {
            throw new IllegalArgumentException("ParcelFileDescriptor 不能为空");
        }
        FileDescriptor fd = pfd.getFileDescriptor();
        return openMemoryFile(fd, length, mode);
    }


    /**
     * 打开共享内存，一般是一个地方创建了一块共享内存
     * 另一个地方持有描述这块共享内存的文件描述符，调用
     * 此方法即可获得一个描述那块共享内存的MemoryFile
     * 对象
     *
     * @param fd     文件描述
     * @param length 共享内存的大小
     * @param mode   PROT_READ = 0x1只读方式打开,
     *               PROT_WRITE = 0x2可写方式打开，
     *               PROT_WRITE|PROT_READ可读可写方式打开
     * @return MemoryFile
     */
    private static MemoryFile openMemoryFile(FileDescriptor fd, int length, int mode) {
        if (mode != OPEN_READONLY && mode != OPEN_READWRITE)
            throw new IllegalArgumentException("invalid mode, only support OPEN_READONLY and OPEN_READWRITE");
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            return openMemoryFileV26(fd, length, mode);
        }
        return openMemoryFileHighVersion(fd, mode);
    }


    /**
     * 适配高版本
     *
     * @param fd
     * @param mode
     * @return
     */
    private static MemoryFile openMemoryFileHighVersion(FileDescriptor fd, int mode) {
        throw new NoSuchElementException("Please use less than 8.0 devices.");
    }


    private static MemoryFile openMemoryFileV26(FileDescriptor fd, int length, int mode) {
        MemoryFile memoryFile = null;
        try {
            memoryFile = new MemoryFile("temp", 1);
            memoryFile.close();
            Class<?> c = MemoryFile.class;
            Method native_mmap = null;
            Method[] ms = c.getDeclaredMethods();
            for (int i = 0; ms != null && i < ms.length; i++) {
                if (ms[i].getName().equals("native_mmap")) {
                    native_mmap = ms[i];
                }
            }
            ReflectUtil.setField("android.os.MemoryFile", memoryFile, "mFD", fd);
            ReflectUtil.setField("android.os.MemoryFile", memoryFile, "mLength", length);
            long address = (long) ReflectUtil.invokeMethod(null, native_mmap, fd, length, mode);
            ReflectUtil.setField("android.os.MemoryFile", memoryFile, "mAddress", address);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return memoryFile;
    }

    /**
     * 获取memoryFile的ParcelFileDescriptor    
     *
     * @param memoryFile 描述一块共享内存    
     * @return ParcelFileDescriptor    
     */
    public static ParcelFileDescriptor getParcelFileDescriptor(MemoryFile memoryFile) {
        if (memoryFile == null) {
            throw new IllegalArgumentException("memoryFile 不能为空");
        }
        ParcelFileDescriptor pfd;
        FileDescriptor fd = getFileDescriptor(memoryFile);
        pfd = (ParcelFileDescriptor) ReflectUtil.getInstance("android.os.ParcelFileDescriptor", fd);
        return pfd;
    }

    /**
     * 获取memoryFile的FileDescriptor   
     *
     * @param memoryFile 描述一块共享内存    
     * @return 这块共享内存对应的文件描述符    
     */

    public static FileDescriptor getFileDescriptor(MemoryFile memoryFile) {
        if (memoryFile == null) {
            throw new IllegalArgumentException("memoryFile 不能为空");
        }
        FileDescriptor fd;
        fd = (FileDescriptor) ReflectUtil.invoke("android.os.MemoryFile", memoryFile, "getFileDescriptor");
        return fd;
    }
}
