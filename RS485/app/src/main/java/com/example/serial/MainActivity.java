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

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


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
    private Button mBtnState;


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
    String sendWay = "ASCII";
    String revWay = "ASCII";
    String sendData;
    String revData;
    String currenState = "recv";
    boolean isHexSend; // 是否以HEX发送
    boolean isHexRecv;
    int    sendOnTime;
    int    maxLen = 1024;
    int    state = 0;
    private static final int MSG_SEND_DATA = 1;

    boolean turnFlag = false;
    private SerialControl serialControl;
    private HexUtils hexUtils = new HexUtils();
    Runnable runnable;
    private volatile boolean receiving = false;
    private Thread receiveThread;


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
        mBtnState = findViewById(R.id.changeState);


        // 获取所有可用的串口。除了串口需要扫描获得数据外，其他的都是固定的值
        serialArray = new String[]{"dev/ttyS9"};

        // 还没有连接串口时，显示not connected
        mTvShow.setTextColor(Color.parseColor("#FF0000"));
        mTvShow.setText("NOT CONNECTED"+"接收模式");
        mTvShow.setPaintFlags(mTvShow.getPaintFlags() | Paint.ANTI_ALIAS_FLAG);
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

        if (sendOnTime == 1){
            startTimeSending();
        }
    }

    private void startTimeSending(){
        // 如果选中定时发送，则每隔30秒发送一次信息
        runnable = new Runnable() {
            @Override
            public void run() {
                // 模拟按钮点击
                mBtnSend.performClick();
                // 30秒后再次运行
                mHandler.postDelayed(this, 30000);
            }
        };
// 开始首次运行
        mHandler.post(runnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(runnable);
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
        mBtnState.setOnClickListener(onClick);
    }
    private class OnClick implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.btn_save) {
                String file_buf = serialControl.saveToFile(revData, revData.length(), "rev_log.txt");
                Toast.makeText(MainActivity.this, "wenjian"+file_buf, Toast.LENGTH_SHORT).show();
            } else if (id == R.id.btn_open) {
                if (turnFlag) {
                    Toast.makeText(MainActivity.this, "串口已打开, 不能重复打开", Toast.LENGTH_SHORT).show();
                }else {
                    int rv = serialControl.openSerialPort(serial, (int) baudRate, dataBits, parity, (int) stopBits, flowControl, maxLen);
                    if (rv < 0) {
                        Toast.makeText(MainActivity.this, "打开串口失败", Toast.LENGTH_SHORT).show();
                    } else {
                        turnFlag = true;
                        startReceiving(); // 串口打开后默认开始读
                        serialOpened();
                        Toast.makeText(MainActivity.this, "成功打开串口", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (id == R.id.btn_close){
                if (!turnFlag) {
                    Toast.makeText(MainActivity.this, "串口已关闭，不能重复关闭", Toast.LENGTH_SHORT).show();
                }else {
                    // 关闭串口
                    int rv = serialControl.closeSerialPort();
//                    Toast.makeText(MainActivity.this, "close", Toast.LENGTH_SHORT).show();
                    if (rv < 0) {
                        Toast.makeText(MainActivity.this, "关闭串口失败", Toast.LENGTH_SHORT).show();
                    } else {
                        turnFlag = false;
                        stopReceiving(); //关闭串口，停止接收
                        serialClosed();
                        Toast.makeText(MainActivity.this, "关闭串口", Toast.LENGTH_SHORT).show();
                    }
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
                } else if (currenState.equals("recv")) {
                    Toast.makeText(MainActivity.this, "串口未处于发送状态，不能发送数据!", Toast.LENGTH_SHORT).show();
                } else {
                    if( sendWay.equals("HEX发送"))
                    {
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
            } else if (id == R.id.changeState) {
                if (state == 1){
                    // 当前GPIO为0，按下按钮后，GPIO设置为0
                    if( serialControl.changeState(0) < 0 ){
                        Log.e("Change State for send", "Change State as send failed");
                    }else {
                        stopReceiving(); // 发送模式下不接收
                        mBtnState.setText("接收模式");
                        state = 0;
                        currenState = "send";
                        if (turnFlag)
                            serialOpened();
                        else
                            serialClosed();
                    }
                }else if (state == 0){
                    if( serialControl.changeState(1) < 0 ){
                        Log.e("Change State for recv", "Change State as recv failed");
                    }else {// 接收串口数据, 同时包含当前时间戳
                        startReceiving();
                        state = 1;
                        mBtnState.setText("发送模式");
                        currenState = "recv";
                        if (turnFlag)
                            serialOpened();
                        else
                            serialClosed();
                    }

                }
            }
        }
    }

    private void serialClosed(){
        mTvShow.setTextColor(Color.parseColor("#FF0000"));
        if (currenState.equals("recv"))
            mTvShow.setText("NOT CONNECTED"+"接收模式");
        else if (currenState.equals("send")) {
            mTvShow.setText("NOT CONNECTED"+"发送模式");
        }
        mTvShow.setPaintFlags(mTvShow.getPaintFlags() | Paint.ANTI_ALIAS_FLAG);
    }

    private void serialOpened(){
        mTvShow.setTextColor(Color.parseColor("#1FA324"));
        if (currenState.equals("recv"))
            mTvShow.setText(serial+" OPENED, "+baudRate+", "+dataBits+", "+stopBits+", "+parity+", "+"接收模式");
        else if (currenState.equals("send")) {
            mTvShow.setText(serial+" OPENED, "+baudRate+", "+dataBits+", "+stopBits+", "+parity+", "+"发送模式");
        }

        mTvShow.setPaintFlags(mTvShow.getPaintFlags() | Paint.ANTI_ALIAS_FLAG);
    }


    public void startReceiving() {
        receiving = true;
        receiveThread = new Thread(() -> {
            while (receiving) {
                int len = 1024;
                int timeout = 10;
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                df.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                String date = df.format(new Date());
                String buf = serialControl.recvFromPort(len, timeout); // 超时时间设置为 1000 ms
                if (buf != null) {
                    // 在主线程上更新 UI
                    mTvRev.post(() -> {
                        String revData = date + "\t" + buf;
                        String currentText = mTvRev.getText().toString();
                        String newText = currentText + "\n" + revData; // 追加新数据
                        mTvRev.setText(newText);
                    });
                } else if (buf == null) {
                    System.err.println("Error receiving data.");
                }
                try {
                    Thread.sleep(10); // 每 10 ms 读取一次数据
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        receiveThread.start();
    }

    public void stopReceiving() {
        receiving = false;
        if (receiveThread != null) {
            try {
                receiveThread.join(); // 等待线程结束
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}