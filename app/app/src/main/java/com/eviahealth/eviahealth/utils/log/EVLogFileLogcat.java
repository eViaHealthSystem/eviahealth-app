package com.eviahealth.eviahealth.utils.log;

import android.util.Log;

import com.eviahealth.eviahealth.utils.log.models.LogApp;

public class EVLogFileLogcat extends EVLogFile implements IEVLog {

    @Override
    public void log(LogApp logApp) {
        super.log(logApp);
        Log.e( "EVLogApp_" + logApp.getTag(), logApp.getMsg());
    }

}
