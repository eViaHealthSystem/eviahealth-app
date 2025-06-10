package com.eviahealth.eviahealth.bluetooth.vitalograph;

import android.app.Application;
import android.util.Log;

import com.telit.terminalio.TIOManager;

public class TIOSample extends Application {

    public static final String PERIPHERAL_ID_NAME = "com.telit.tiosample.peripheralId";

    @Override
    public void onCreate() {
        Log.e("TIOSample", "************************************** TIOSample.onCreate()");
        TIOManager.initialize(this.getApplicationContext());
    }

    @Override
    public void onTerminate() {
        Log.e("TIOSample", "************************************** TIOSample.onTerminate()");
        TIOManager.getInstance().done();
    }

}