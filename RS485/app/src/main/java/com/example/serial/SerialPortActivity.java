package com.example.serial;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

public abstract class SerialPortActivity extends AppCompatActivity {
//    protected SerialControl mSerialControl;
//    protected OutputStream mOutputStream;
//    protected InputStream mInputStream;
//    private ReadThread mReadThread;
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        try {
//            mSerialControl = getSerialPort();
//            mInputStream = mSerialControl.getInputStream();
//            mOutputStream = mSerialControl.getOutputStream();
//
//            mReadThread = new ReadThread(); // 创建接收线程
//            mReadThread.start();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    @Override
//    protected void onDestroy() {
//        if (mReadThread != null)
//            mReadThread.interrupt();
////        mSerialControl.closeSerialPort();
//        this.closeSerial();
//        mSerialControl = null;
//        super.onDestroy();
//    }
//
//    public void closeSerial(){
//        if (mSerialControl != null){
//            mSerialControl.closeSerialPort();
//            mSerialControl = null;
//        }
//    }
//
//    public SerialControl getSerialPort() throws SecurityException, IOException, InvalidParameterException{
//        if (mSerialControl == null){
//            SerialControl serialControl = new SerialControl(new File("dev/ttyS9"), 115200, 8, 2, 2, 0);
//            mSerialControl = serialControl;
//        }
//        return mSerialControl;
//    }
//
//
//    private class ReadThread extends Thread{
//        @Override
//        public void run(){
//            super.run();
//            while (!isInterrupted()){
//                int size;
//                try {
//                    byte[] buffer = new byte[64];
//                    if(mInputStream == null)
//                        return;
//                    size = mInputStream.read(buffer);
//                    if (size > 0){
//                        onDataReceived(buffer, size);
//                    }
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }
//    }
//
//    protected abstract void onDataReceived(final byte[] buffer, final int size);
}