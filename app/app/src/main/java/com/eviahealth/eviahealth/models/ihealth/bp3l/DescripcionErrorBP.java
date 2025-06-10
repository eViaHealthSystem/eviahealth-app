package com.eviahealth.eviahealth.models.ihealth.bp3l;

public class DescripcionErrorBP {

    private String error;
    private String solucion;

    private DescripcionErrorBP() {}

    public DescripcionErrorBP(String error, String solucion) {
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
