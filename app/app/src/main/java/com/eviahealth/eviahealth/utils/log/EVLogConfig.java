package com.eviahealth.eviahealth.utils.log;

import android.util.Log;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.utils.log.models.LogApp;
import com.eviahealth.eviahealth.utils.Fecha.Fecha;

import java.io.IOException;

public class EVLogConfig {

    private EVLogConfig() {}

    private static String FECHA = null;

    public static void setFechaActual() {
        FECHA = Fecha.getFechaParaFile();
    }
    public static String getFechaActual() {
        return FECHA;
    }

    public static void log(String tag, String msg) {
        try {
            Log.e(tag, msg);
            FileAccess.appendToLogFile(FileAccess.getPATH(), FilePath.REGISTROS_LOG_CONFIG, FECHA, new LogApp(tag, msg));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
