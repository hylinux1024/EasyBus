package com.github.easybus.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.github.easybus.R;

import com.gitlab.annotation.EasySubscribe;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }

    @EasySubscribe
    public void onUpdateMessage(MessageEvent event) {

    }
}
