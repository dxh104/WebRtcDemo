package com.dxh.webrtcdemo;

import android.app.Application;

/**
 * Created by XHD on 2022/06/30
 */
public class MainApplication extends Application {
    public static MainApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
