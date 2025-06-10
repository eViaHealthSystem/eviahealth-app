package com.eviahealth.eviahealth.utils.log.models;

import com.eviahealth.eviahealth.utils.Fecha.Fecha;

public class LogApp {

    public static final String DELIMITER = ";";

    private String fecha;
    private String tag;
    private String txt;

    private LogApp() {}

    public LogApp(String tag, String txt) {
        this.fecha = Fecha.getFechaYHoraActual();
        this.tag = tag;
        this.txt = txt;
    }

    public String getTag() {
        return this.tag;
    }

    public String getMsg() {
        return this.txt;
    }

    @Override
    public String toString() {
        return this.fecha + DELIMITER + this.tag + DELIMITER + this.txt + DELIMITER + "\n";
    }
}
