package com.example.serial;

public class SerialControl {
    static {
        System.loadLibrary("serial");
    }

//    private File device = null; // dev/ttyS9
//    private int baudRate = 0;
//    private int dataBits = 5;
//    private int parity = 0;
//    private int stopBits = 1;
//    private int flags;
//
//    private FileDescriptor mFd;
//    private FileInputStream mFileInputStream;
//    private FileOutputStream mFileOutputStream;
//
//    public SerialControl(@NonNull File device, int baudRate, int dataBits, int parity, int stopBits, int flags) throws SecurityException, IOException{
//        this.device = device;
//        this.baudRate = baudRate;
//        this.dataBits = dataBits;
//        this.parity = parity;
//        this.stopBits = stopBits;
//        this.flags = flags;
//
//        mFd = openSerialPort(device.getAbsolutePath(), baudRate, dataBits, parity, stopBits, flags);
//        if (mFd == null){
//            Log.e(TAG, "openSerialPort returns null");
//            throw new IOException();
//        }
//        mFileInputStream = new FileInputStream(mFd);
//        mFileOutputStream = new FileOutputStream(mFd);
//    }
//
//    @NonNull
//    public InputStream getInputStream(){
//        return mFileInputStream;
//    }
//
//    @NonNull
//    public OutputStream getOutputStream(){
//        return mFileOutputStream;
//    }

    public static native int openSerialPort(String path, int baudRate, int dataBits, int parity, int stopBits, int flowControl, int maxLen);
    public static native int closeSerialPort();

    public static native int sendToPort(String msg, int len);
    public static native int recvFromPort(String msg, int len, int timeout);

    public static native int saveToFile(String msg, int len, String fileName);

}
