package com.eviahealth.eviahealth.utils.Fecha;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Fecha {
    private Fecha() {}

    private static final SimpleDateFormat DATE_FORMAT_FECHA_HORA = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat DATE_FORMAT_FECHA_HORA_FILE = new SimpleDateFormat("yyyyMMddHHmmss_");

    public static String getFechaYHoraActual() {

        return DATE_FORMAT_FECHA_HORA.format(new Date());

    }

    public static String getFechaParaFile() {
        return DATE_FORMAT_FECHA_HORA_FILE.format(new Date());
    }
}
