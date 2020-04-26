package com.zhupeng.location;

import android.app.Application;

import androidx.multidex.MultiDex;

public class LocationApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        MultiDex.install(this);
    }
}
