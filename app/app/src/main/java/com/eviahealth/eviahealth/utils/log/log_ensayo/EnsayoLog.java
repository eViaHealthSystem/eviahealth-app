package com.eviahealth.eviahealth.utils.log.log_ensayo;

import com.eviahealth.eviahealth.utils.FileAccess.CarpetaEnsayo;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.utils.log.models.LogEnsayo;
import com.eviahealth.eviahealth.utils.Fecha.Fecha;

import java.io.IOException;

/*
Para hacer logs del dispositivo de logs (los que se guardaran el db)
al hacer setFechaActual(), se genera una fecha y hasta que no se vuelva a ejecutar el metodo,
se guardaran los logs en un archivo con ese nombre.
Se hace .setFechaActual() al empezar el ensayo.
 */
public class EnsayoLog {
    private EnsayoLog() {}

    private static String FECHA = null;

    public static void setFechaActual() {
        FECHA = Fecha.getFechaParaFile();
    }

    public static void log(String fase, String tag, String msg) {
        try {
            FileAccess.appendToLogFile(CarpetaEnsayo.getCarpeta(), FilePath.REGISTROS_LOG, FECHA, new LogEnsayo(fase, tag, msg));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
