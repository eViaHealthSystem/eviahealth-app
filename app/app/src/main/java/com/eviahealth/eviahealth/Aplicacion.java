package com.eviahealth.eviahealth;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.log.ConfigLog;
import com.ihealth.communication.manager.iHealthDevicesManager;
import org.conscrypt.Conscrypt;

import java.security.Security;

public class Aplicacion extends Application {
    private static final String TAG = "EviaHealth";

    @Override
    public void onCreate() {
        super.onCreate();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Log.e(TAG,"onCreate Aplicacion");
        Security.insertProviderAt(Conscrypt.newProvider(),1);

        Log.e(TAG,"getExternalFilesDir(null): " + getExternalFilesDir(null));
        FileAccess.init(getApplicationContext()); // Inicializar PATH para toda la APP

        initHealthDevices();
        ConfigLog.configurar();
    }

    private void initHealthDevices() {
        Log.e(TAG, "initHealthDevices()");
        iHealthDevicesManager.getInstance().init(this,  Log.VERBOSE, Log.VERBOSE);
    }

}
