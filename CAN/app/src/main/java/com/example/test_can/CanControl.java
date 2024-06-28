package com.example.test_can;


public class CanControl {
    public String recvData;
    public String recvID;
    public String recvDlc;
    public static native int sendMessage(String id, String dlc, String data, String ifname);
    public static native String receiveMessage(String ifname);
}
