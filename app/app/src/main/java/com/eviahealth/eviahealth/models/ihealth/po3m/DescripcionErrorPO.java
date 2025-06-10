package com.eviahealth.eviahealth.models.ihealth.po3m;

public class DescripcionErrorPO {

    private String error;
    private String solucion;

    private DescripcionErrorPO() {}

    public DescripcionErrorPO(String error, String solucion) {
        this.error = error;
        this.solucion = solucion;
    }

    public String getError() {
        return error;
    }

    public String getSolucion() {
        return solucion;
    }
}
