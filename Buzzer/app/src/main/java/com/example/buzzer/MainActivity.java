package com.example.buzzer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.buzzer.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'buzzer' library on application startup.
    static {
        System.loadLibrary("buzzer");
    }

    private ActivityMainBinding binding;
    private Button play;
    private Button stop;
    private EditText period;
    private EditText duty_cycle;

    private String periodBuf;
    private String dutyBuf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        play = findViewById(R.id.play);
        period = findViewById(R.id.period);
        duty_cycle = findViewById(R.id.duty_cycle);
        stop = findViewById(R.id.stop);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                periodBuf = period.getText().toString();
                dutyBuf = duty_cycle.getText().toString();

                if( pwmOpen("2") < 0)
                    Toast.makeText(MainActivity.this, "打开PWM失败", Toast.LENGTH_SHORT).show();
                else {
                    if( pwmConfig("period", periodBuf) < 0 )
                        Toast.makeText(MainActivity.this, "设置周期失败", Toast.LENGTH_SHORT).show();
                    if( pwmConfig("duty_cycle", dutyBuf) < 0 )
                        Toast.makeText(MainActivity.this, "设置占空比失败", Toast.LENGTH_SHORT).show();
                    if( pwmConfig("enable", "1") < 0 )
                        Toast.makeText(MainActivity.this, "使能PWM失败", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(MainActivity.this, "打开PWM, 正在播放", Toast.LENGTH_SHORT).show();
                }
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( pwmConfig("enable", "0") < 0 )
                    Toast.makeText(MainActivity.this, "禁止PWM失败", Toast.LENGTH_SHORT).show();
                else {
                    Toast.makeText(MainActivity.this, "禁止PWM，暂停播放", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    native static int pwmOpen(String id);
    native static int pwmConfig(String attr, String val);
}