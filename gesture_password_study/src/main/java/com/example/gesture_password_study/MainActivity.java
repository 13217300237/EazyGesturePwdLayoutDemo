package com.example.gesture_password_study;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.example.gesture_password_study.gesture_pwd.GesturePwdCheckActivity;
import com.example.gesture_password_study.gesture_pwd.GesturePwdResetActivity;
import com.example.gesture_password_study.gesture_pwd.GesturePwdSettingActivity;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);

        findViewById(R.id.tv_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GesturePwdSettingActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.tv_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GesturePwdCheckActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.tv_3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GesturePwdResetActivity.class);
                startActivity(intent);
            }
        });
    }


}
