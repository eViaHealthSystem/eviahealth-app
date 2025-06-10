package com.eviahealth.eviahealth.utils.log;

import com.eviahealth.eviahealth.utils.log.models.LogApp;
import com.eviahealth.eviahealth.utils.Fecha.Fecha;

/*
Para hacer logs de debug, desarrollo (no los que se insertan en db)
Se genera una fecha al hacer .setFechaActual()
    En el archivo con esa fecha se guardaran los logs hasta hacer un nuevo .setFechaActual()

Se llama a .setFechaActual() en el onCreate de la aplicacion y al empezar el ensayo (click inicio ensayo)
tambien en la parte de tecnico al guardar.
 */

public class EVLog {
    private EVLog() {}

    private static IEVLog LOGGER = null;

    public static void setLogger(IEVLog logger) {
        LOGGER = logger;
    }

    public static void setFechaActual() {
        LOGGER.setFecha(Fecha.getFechaParaFile());
    }

    public static void log(String tag, String msg) {
        LOGGER.log(new LogApp(tag, msg));
    }
}
