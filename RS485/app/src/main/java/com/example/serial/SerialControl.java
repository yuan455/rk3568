package com.example.serial;

public class SerialControl {
    static {
        System.loadLibrary("serial");
    }
    public String receivedMessage;

    public static native int openSerialPort(String path, long baudRate, int dataBits, int parity, int stopBits, int flowControl, int maxLen);
    public static native int closeSerialPort();

    public static native int sendToPort(String msg, int len);
    public static native String recvFromPort(int len, int timeout);

    public static native int changeState(int state); //

    public static native String saveToFile(String msg, int len, String fileName);

}
