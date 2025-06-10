package com.eviahealth.eviahealth.utils.log;

import android.util.Log;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ConfigLog {

    private ConfigLog() {}

    public static void configurar() {
        try {
            JSONObject json_log = FileAccess.leerJSON(FilePath.CONFIG_LOG);
            String tipo_log = json_log.getString("log");
            if(tipo_log.equals("no_log")) {
                EVLog.setLogger(new EVLogNull());
            } else if(tipo_log.equals("file_log")) {
                EVLog.setLogger(new EVLogFile());
            } else {
                // dev_log u otras opciones
                EVLog.setLogger(new EVLogFileLogcat());
            }
            EVLog.setFechaActual();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.e("ConfigLog", "Exception: " + e.toString());
            Log.e("Inicio", "Configurado logger por defecto (File y Logcat)");

            EVLog.setLogger(new EVLogFileLogcat());
        }
    }
}
