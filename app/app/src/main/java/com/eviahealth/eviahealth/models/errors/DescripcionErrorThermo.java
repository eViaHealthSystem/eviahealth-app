package com.eviahealth.eviahealth.models.errors;

public class DescripcionErrorThermo {

    private String error;
    private String solucion;

    private DescripcionErrorThermo() {}

    public DescripcionErrorThermo(String error, String solucion) {
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
