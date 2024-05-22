package com.example.serial;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.serial.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    // Used to load the 'serial' library on application startup.
    static {
        System.loadLibrary("serial");
    }

    ArrayAdapter<CharSequence> adapter;

    // 与串口参数相关的组件
    private ActivityMainBinding binding;
    private Spinner mSpBaud;
    private Spinner mSpSerial;
    private Spinner mSpData;
    private Spinner mSpStop;
    private Spinner mSpParity;
    private Spinner mSpFlow;

    // 和发送信息相关的控件
    private RadioGroup mRGSend;
    private CheckBox mCK;
    private Button mBtnClearSend;
    private Button mBtnSend;
    private EditText mEtSend;
    // 和接收信息相关的控件
    private RadioGroup mRGRv;
    private Button mBtnSave;
    private Button mBtnClearRev;
    private TextView mTvRev;
    private TextView mTvShow;

    // 打开串口&关闭串口
    private Button mBtnOpen;
    private Button mBtnClose;


    // 设置串口的参数
    private String serial;
    private long baudRate;
    private int dataBits;
    private double stopBits;
    private int parity;
    private int flowControl; // 0:None, 1:RTS/CTS, 2:XON/XOFF

    // 获取已经设定的值
    String[] serialArray;
    String[] baudArray;
    String[] dataArray;
    String[] stopArray;
    String[] parityArray;
    String[] flowArray;
    String sendWay;
    String revWay;
    String sendData;
    String revData;
    boolean isHexSend; // 是否以HEX发送
    boolean isHexRecv;
    int    sendOnTime;
    int    maxLen = 1024;
    private static final int MSG_SEND_DATA = 1;

    boolean turnFlag = false;
    private SerialControl serialControl;
    private HexUtils hexUtils = new HexUtils();

    private Handler mHandler = new Handler(Looper.getMainLooper()){
        public void handleMessage(Message msg){
            if (msg.what == MSG_SEND_DATA){
                serialControl.sendToPort(sendData, sendData.length());
                sendEmptyMessageDelayed(MSG_SEND_DATA, 30*1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        serialControl = new SerialControl();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 找到控件
        mSpSerial = findViewById(R.id.sp_serial);
        mSpBaud = findViewById(R.id.sp_baud);
        mSpData = findViewById(R.id.sp_data);
        mSpStop = findViewById(R.id.sp_stop);
        mSpParity = findViewById(R.id.sp_parity);
        mSpFlow = findViewById(R.id.sp_flow);

        mRGSend = findViewById(R.id.rg_send);
        mRGRv = findViewById(R.id.rg_rv);
        mCK = findViewById(R.id.cb_time_send);
        mBtnSave = findViewById(R.id.btn_save);
        mBtnOpen = findViewById(R.id.btn_open);
        mBtnClose = findViewById(R.id.btn_close);
        mBtnClearRev = findViewById(R.id.btn_clear_rev);
        mBtnClearSend = findViewById(R.id.btn_clear_send);
        mBtnSend = findViewById(R.id.btn_send);
        mEtSend = findViewById(R.id.et_send);
        mTvRev = findViewById(R.id.txt_rev);
        mTvShow = findViewById(R.id.txt_show);


        // 获取所有可用的串口。除了串口需要扫描获得数据外，其他的都是固定的值
        serialArray = new String[]{"dev/ttyS9"};

        // 还没有连接串口时，显示not connected
        serialClosed();
        // 设置并监听下拉列表
        setSpinner(mSpSerial);
        setSpinner(mSpBaud);
        setSpinner(mSpData);
        setSpinner(mSpStop);
        setSpinner(mSpParity);
        setSpinner(mSpFlow);

        // 监听所有的button
        setListener();

        // 设置发送和接收的文本格式
        mRGSend.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = group.findViewById(checkedId);
                sendWay = radioButton.getText().toString();
//                Toast.makeText(MainActivity.this, radioButton.getText(), Toast.LENGTH_SHORT).show();
            }
        });
        mRGRv.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = group.findViewById(checkedId);
                revWay = radioButton.getText().toString();
//                Toast.makeText(MainActivity.this, radioButton.getText(), Toast.LENGTH_SHORT).show();
            }
        });

        mCK.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if( isChecked )
                    sendOnTime = 1;
                else
                    sendOnTime = 0;
//                Toast.makeText(MainActivity.this, isChecked?"checked":"not checked", Toast.LENGTH_SHORT).show();
            }
        });

//        mTvShow.getPaint().setAntiAlias(true);

        // 接收串口数据
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                byte[] msg = new  byte[1024];
//                int len = 1024; // 读取数据的长度
//                int timeout = 1000; // 超时时间，单位为毫秒
//
//                while (true){
//                    int result = serialControl.recvFromPort(Arrays.toString(msg), len, timeout);
//                    if (result > 0){
//                        final String recvData = new String(msg, 0, result);
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                mTvRev.setText(recvData);
//                            }
//                        });
//                    } else if ( 0 == result ) {
//                        Log.d("MainActivity", "Timeout");
//                    }else {
//                        Log.e("MainActivity", "Read data from port falied");
//                    }
//                }
//
//            }
//        }).start();
        mTvRev.setText(HexUtils.bytesToHexString("hahaha".getBytes()));

        // 如果选中定时发送，则每隔30秒发送一次信息
        while (sendOnTime == 1){
            mHandler.sendEmptyMessageDelayed(MSG_SEND_DATA, 30*1000);
        }
    }

    private void setSpinner(Spinner spinner){
        ArrayAdapter<CharSequence> adapter;
        int id = spinner.getId();

        if (id == R.id.sp_serial){
            adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, serialArray);
        }else if (id == R.id.sp_baud) {
            adapter = ArrayAdapter.createFromResource(MainActivity.this, R.array.baud_rate, android.R.layout.simple_spinner_item);
        } else if (id == R.id.sp_data) {
            adapter = ArrayAdapter.createFromResource(MainActivity.this, R.array.data_bits, android.R.layout.simple_spinner_item);
        } else if (id == R.id.sp_stop) {
            adapter = ArrayAdapter.createFromResource(MainActivity.this, R.array.stop_bits, android.R.layout.simple_spinner_item);
        } else if (id == R.id.sp_parity) {
            adapter = ArrayAdapter.createFromResource(MainActivity.this, R.array.parity, android.R.layout.simple_spinner_item);
        } else if (id == R.id.sp_flow) {
            adapter = ArrayAdapter.createFromResource(MainActivity.this, R.array.flow_control, android.R.layout.simple_spinner_item);
        } else {
            throw new IllegalStateException("Unexpected value: " + spinner.getId());
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int spinnerId = spinner.getId();

                if (spinnerId == R.id.sp_serial) {
                    serial = serialArray[position];
//                    Toast.makeText(MainActivity.this, "串口:" + serial, Toast.LENGTH_SHORT).show();
                } else if (spinnerId == R.id.sp_baud) {
                    baudArray = getResources().getStringArray(R.array.baud_rate);
                    baudRate = Integer.parseInt(baudArray[position]);
//                    Toast.makeText(MainActivity.this, "波特率:" + baudRate, Toast.LENGTH_SHORT).show();
                } else if (spinnerId == R.id.sp_data) {
                    dataArray = getResources().getStringArray(R.array.data_bits);
                    dataBits = Integer.parseInt(dataArray[position]);
//                    Toast.makeText(MainActivity.this, "数据位:" + dataBits, Toast.LENGTH_SHORT).show();
                } else if (spinnerId == R.id.sp_stop) {
                    stopArray = getResources().getStringArray(R.array.stop_bits);
                    stopBits = Double.parseDouble(stopArray[position]);
//                    Toast.makeText(MainActivity.this, "停止位:" + stopBits, Toast.LENGTH_SHORT).show();
                } else if (spinnerId == R.id.sp_parity) {
                    parityArray = getResources().getStringArray(R.array.parity);
                    if (parityArray[position].equals("偶校验"))
                        parity = 2;
                    else if (parityArray[position].equals("奇校验"))
                        parity = 1;
                    else if (parityArray[position].equals("None"))
                        parity = 0;
//                    Toast.makeText(MainActivity.this, "校验位:" + parityArray[position], Toast.LENGTH_SHORT).show();
                } else if (spinnerId == R.id.sp_flow) {
                    flowArray = getResources().getStringArray(R.array.flow_control);
                    flowControl = position;
                } else {
                    throw new IllegalStateException("Unexpected value: " + spinner.getId());
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setListener(){
        OnClick onClick = new OnClick();
        mBtnSave.setOnClickListener(onClick);
        mBtnOpen.setOnClickListener(onClick);
        mBtnClose.setOnClickListener(onClick);
        mBtnClearRev.setOnClickListener(onClick);
        mBtnClearSend.setOnClickListener(onClick);
        mBtnSend.setOnClickListener(onClick);
    }
    private class OnClick implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.btn_save) {
                // 将接收的内容保存在文件中
//                Toast.makeText(MainActivity.this, "保存到文件中", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.btn_open) {
                int rv = serialControl.openSerialPort(serial, (int) baudRate, dataBits, parity, (int) stopBits, flowControl, maxLen);
                if (rv < 0) {
                    Toast.makeText(MainActivity.this, "打开串口失败", Toast.LENGTH_SHORT).show();
                }else {
                    turnFlag = true;
                    serialOpened();
                    Toast.makeText(MainActivity.this, "成功打开串口", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.btn_close){
                // 关闭串口
                int rv = serialControl.closeSerialPort();
//                    Toast.makeText(MainActivity.this, "close", Toast.LENGTH_SHORT).show();
                if (rv < 0) {
                    Toast.makeText(MainActivity.this, "关闭串口失败", Toast.LENGTH_SHORT).show();
                }else {
                    turnFlag = false;
                    serialClosed();
                    Toast.makeText(MainActivity.this, "关闭串口", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.btn_clear_rev) {
                // 清空接收区数据
                mTvRev.setText("");
            } else if (id == R.id.btn_clear_send) {
                // 清空发送区内的数据
                mEtSend.getText().clear();
            } else if (id == R.id.btn_send) {
                // 发送数据,发送数据后清空发送区
                // 读取发送区中的数据
                sendData = mEtSend.getText().toString();

                // 判断是否已连接串口, 如果没有连接串口则需先连接串口
                if( !turnFlag ){
                    Toast.makeText(MainActivity.this, "串口未打开，请打开串口！", Toast.LENGTH_SHORT).show();
                }else if(sendData.isEmpty()){
                    // 判断发送区是否为空,如果为空则弹出提示
                    Toast.makeText(MainActivity.this, "发送区为空，请输入数据！", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(MainActivity.this, "sendWay"+sendWay, Toast.LENGTH_SHORT).show();
                    if( sendWay == "HEX")
                    {
                        Toast.makeText(MainActivity.this, "HEX in ", Toast.LENGTH_SHORT).show();
                        mEtSend.getText().clear();
                        mEtSend.setText(HexUtils.bytesToHexString(sendData.getBytes()));
                    }
                    int rv = serialControl.sendToPort(sendData, sendData.length());
                    if( rv < 0 )
                    {
                        Toast.makeText(MainActivity.this, "发送数据失败！", Toast.LENGTH_SHORT).show();
                    }
                    Toast.makeText(MainActivity.this, "发送数据成功", Toast.LENGTH_SHORT).show();

                }
            }
        }
    }

    private void serialClosed(){
        mTvShow.setTextColor(Color.parseColor("#FF0000"));
        mTvShow.setText("NOT CONNECTED");
        mTvShow.setPaintFlags(mTvShow.getPaintFlags() | Paint.ANTI_ALIAS_FLAG);
    }

    private void serialOpened(){
        mTvShow.setTextColor(Color.parseColor("#1FA324"));
        mTvShow.setText(serial+" OPENED, "+baudRate+", "+dataBits+", "+stopBits+", "+parity);
        mTvShow.setPaintFlags(mTvShow.getPaintFlags() | Paint.ANTI_ALIAS_FLAG);
    }

}