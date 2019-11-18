package com.github.easybus;

import android.util.Log;

public class Logger {
    private static final String TAG = "EasyBus";

    public static void v(String text) {
        Log.v(TAG, text);
    }

    public static void i(String text) {
        Log.i(TAG, text);
    }
}
