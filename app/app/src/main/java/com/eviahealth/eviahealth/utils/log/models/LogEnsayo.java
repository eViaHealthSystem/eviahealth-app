package com.eviahealth.eviahealth.utils.log.models;

import com.eviahealth.eviahealth.utils.Fecha.Fecha;

public class LogEnsayo {

    public static final String DELIMITER = ";";

    private String fecha;
    private String fase;
    private String tag;
    private String msg;

    private LogEnsayo() {}

    public LogEnsayo(String fase, String tag, String msg) {
        this.fecha = Fecha.getFechaYHoraActual();
        this.fase = fase;
        this.tag = tag;
        this.msg = msg;
    }

    @Override
    public String toString() {
        return this.fecha + DELIMITER + this.fase + DELIMITER + this.tag + DELIMITER + this.msg + DELIMITER + "\n";
    }

}
