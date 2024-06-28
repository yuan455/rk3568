package com.example.test_can;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.test_can.databinding.ActivityMainBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'test_can' library on application startup.
    static {
        System.loadLibrary("test_can");
    }

    private ActivityMainBinding binding;

    private volatile boolean receiving = false;
    private Thread receiveThread;

    private String data; // 存放发送的数据
    private String canId;
    private String dlc;
    private String canName;
    private String status = "send";
    private final Object lock = new Object();

    private Button mBtnSend;
    private Button mBtnRecv;
    private Button mBtnSendClear;
    private Button mBtnRecvClear;
    private TextView mTvSend;
    private TextView mTvRecv;
    private EditText mEdId;
    private EditText mEdDlc;
    private EditText mEdData;
    private Spinner mSpCan;

    private CanControl canControl = new CanControl();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mBtnSend = findViewById(R.id.btn_send);
        mBtnRecv = findViewById(R.id.btn_recv);
        mBtnSendClear = findViewById(R.id.btn_send_clear);
        mBtnRecvClear = findViewById(R.id.btn_recv_clear);
        mTvSend = findViewById(R.id.tv_send);
        mTvRecv = findViewById(R.id.tv_recv);
        mEdId = findViewById(R.id.ed_id);
        mEdDlc = findViewById(R.id.ed_dlc);
        mEdData = findViewById(R.id.ed_data);
        mSpCan = findViewById(R.id.sp_can);

        ArrayAdapter<CharSequence> adapter;
        adapter = ArrayAdapter.createFromResource(MainActivity.this, R.array.can_name, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        mSpCan.setAdapter(adapter);
        mSpCan.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                canName = getResources().getStringArray(R.array.can_name)[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        setListener();

    }

    private void setListener() {
        OnClick onClick = new OnClick();
        mBtnRecv.setOnClickListener(onClick);
        mBtnRecvClear.setOnClickListener(onClick);
        mBtnSend.setOnClickListener(onClick);
        mBtnSendClear.setOnClickListener(onClick);
    }

    private class OnClick implements View.OnClickListener {
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.btn_send) {
                status = "send";
                if (canName == null){
                    Toast.makeText(MainActivity.this, "请选择CAN！", Toast.LENGTH_SHORT).show();
                }
                canId = mEdId.getText().toString();
                dlc = mEdDlc.getText().toString();
                data = mEdData.getText().toString();
                if (Integer.parseInt(canId) > 0xFFF || Integer.parseInt(canId) < 0x000) {
                    Toast.makeText(MainActivity.this, "请输入正确的ID！", Toast.LENGTH_SHORT).show();
                }else if (Integer.parseInt(dlc) > 7 || Integer.parseInt(canId) < 0){
                    Toast.makeText(MainActivity.this, "请输入正确的DLC！", Toast.LENGTH_SHORT).show();
                }else {
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    df.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                    String date = df.format(new Date());
                    int rv = canControl.sendMessage(canId, dlc, data, canName);
                    if (rv < 0) {
                        Log.e("Send", "Send failed");
                    }else {
                        mTvSend.post(() -> {
                            String sendData = date + "\t" + "ID=" + canId + " DLC=" + dlc + " Data=" + data;
                            String currentText = mTvSend.getText().toString();
                            String newText = currentText + "\n" + sendData; // 追加新数据
                            mTvSend.setText(newText);
                        });
                    }
                }
            } else if (id == R.id.btn_send_clear) {
                mTvSend.setText("");
            } else if (id == R.id.btn_recv) {
                if (canName == null){
                    Toast.makeText(MainActivity.this, "请选择CAN！", Toast.LENGTH_SHORT).show();
                }
                status = "recv";
                startReceiving();
            } else if (id == R.id.btn_recv_clear) {
                status = "stopRecv";
                mTvRecv.setText("");
            }
        }
    }
    public void startReceiving() {
            receiving = true;
        Log.d("TAG", "startReceiving: "+status);
            receiveThread = new Thread(() -> {
                Log.d("TAG", "startReceiving: "+status);
                while (status.equals("recv")) {
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    df.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                    String date = df.format(new Date());
                    String buf = canControl.receiveMessage(canName);
                    if (buf == null) {
                        Log.e("Recv", "Receive failed");
                    }
                    Log.d("Recv", buf);
                    mTvRecv.post(() -> {
                        String revData = date + "\t" + buf;
                        String currentText = mTvRecv.getText().toString();
                        String newText = currentText + "\n" + revData; // 追加新数据
                        mTvRecv.setText(newText);
                    });
                    try {
                        Thread.sleep(10); // 每 10 ms 读取一次数据
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            receiveThread.start();
//        }
    }
    public void stopReceiving() {
//        synchronized (lock) {
            receiving = false;
            if (receiveThread != null) {
                try {
                    receiveThread.join(); // 等待线程结束
                    receiveThread = null; // 防止重复调用时的错误
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
//        }
    }
//    public void stopReceiving() {
//        receiving = false;
//        if (receiveThread != null) {
//            try {
//                receiveThread.join(); // 等待线程结束
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }

}