package com.jiangdg.usb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "MyBroadcastReceiver";
    private MyCallback callback;

    public MyBroadcastReceiver(MyCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Call a method on the callback
        if (callback != null) {
            callback.onEventReceived(intent);
        }
    }

    public interface MyCallback {
        void onEventReceived(Intent intent);
    }
}