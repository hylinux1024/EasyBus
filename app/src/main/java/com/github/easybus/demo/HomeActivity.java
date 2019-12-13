package com.github.easybus.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.github.easybus.MyEventBusIndex;
import com.github.easybus.R;

import com.gitlab.annotation.EasySubscribe;
import com.gitlab.easybuslib.EasyBus;
import com.google.android.material.snackbar.Snackbar;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 将索引添加到 EasyBus 全局中
        EasyBus.getInstance().addIndex(new MyEventBusIndex());
        findViewById(R.id.registerBtn).setOnClickListener(v -> {
            EasyBus.getInstance().register(this);
        });

        findViewById(R.id.unregisterBtn).setOnClickListener(v -> {
            EasyBus.getInstance().unregister(this);
        });

        findViewById(R.id.easyPostBtn).setOnClickListener(v -> {
            EasyBus.getInstance().post(new MessageEvent("message from easybus!"));
        });
    }

    @EasySubscribe
    public void onUpdateMessage(MessageEvent event) {
        Snackbar.make(findViewById(R.id.fab), "onEventUpdateMessage : " + event.message, Snackbar.LENGTH_SHORT).show();
    }

    @EasySubscribe
    public void onEventNotify(MessageEvent event) {
        Toast.makeText(this, "onEventNotify :" + event.message, Toast.LENGTH_SHORT).show();
    }

}
