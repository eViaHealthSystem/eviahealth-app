package com.eviahealth.eviahealth.models.vitalograph;

public class DescripcionErrorLung {
    private String error;
    private String solucion;

    private DescripcionErrorLung() {}

    public DescripcionErrorLung(String error, String solucion) {
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
