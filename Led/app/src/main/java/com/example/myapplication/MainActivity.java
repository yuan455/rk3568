package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.myapplication.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'myapplication' library on application startup.
    static {
        System.loadLibrary("myapplication");
    }

    private boolean redTag = false;
    private boolean greenTag = false;
    private boolean yellowTag = false;
    private Button ledRed = null;
    private Button ledGreen = null;
    private Button ledYellow = null;
    private ImageView imageRed = null;
    private ImageView imageGreen = null;
    private ImageView imageYellow = null;
    private HardCtrl hardCtrl;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ledRed = (Button)findViewById(R.id.red);
        ledYellow = (Button)findViewById(R.id.yellow);
        ledGreen = (Button)findViewById(R.id.green);

        imageRed = findViewById(R.id.imageRed);
        imageYellow = findViewById(R.id.imageYellow);
        imageGreen = findViewById(R.id.imageGreen);

        hardCtrl = new HardCtrl();
        hardCtrl.ledOpen();

        ledRed.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                redTag = !redTag;
                if( redTag ) {
                    imageRed.setColorFilter(getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_IN);
                    hardCtrl.ledCtrl(0, 0);
                }else{
                    imageRed.setColorFilter(getResources().getColor(android.R.color.darker_gray), PorterDuff.Mode.SRC_IN);
                    hardCtrl.ledCtrl(0, 1);
                }
            }
        });
        ledYellow.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                yellowTag = !yellowTag;
                if( yellowTag ) {
                    imageYellow.setColorFilter(getResources().getColor(android.R.color.holo_orange_light), PorterDuff.Mode.SRC_IN);
                    hardCtrl.ledCtrl(1, 0);
                }else{
                    imageYellow.setColorFilter(getResources().getColor(android.R.color.darker_gray), PorterDuff.Mode.SRC_IN);
                    hardCtrl.ledCtrl(1, 1);
                }
            }
        });

        ledGreen.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                greenTag = !greenTag;
                if( greenTag ) {
                    imageGreen.setColorFilter(getResources().getColor(android.R.color.holo_green_light), PorterDuff.Mode.SRC_IN);
                    hardCtrl.ledCtrl(2, 0);
                }else{
                    imageGreen.setColorFilter(getResources().getColor(android.R.color.darker_gray), PorterDuff.Mode.SRC_IN);
                    hardCtrl.ledCtrl(2, 1);
                }
            }
        });

        hardCtrl.ledCtrl(0, 1);
        hardCtrl.ledCtrl(1, 1);
        hardCtrl.ledCtrl(2, 1);
    }
}