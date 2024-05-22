package com.example.myapplication;

public class HardCtrl {
    public static native void ledOpen();
    public static native int ledCtrl(int which, int status);

    static{
        System.loadLibrary("myapplication");
        System.loadLibrary("gpiod");
    }
}
