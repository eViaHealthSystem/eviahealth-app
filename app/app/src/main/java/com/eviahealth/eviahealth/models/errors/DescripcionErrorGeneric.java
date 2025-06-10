package com.eviahealth.eviahealth.models.errors;

public class DescripcionErrorGeneric {

    private String error;
    private String solucion;

    private DescripcionErrorGeneric() {}

    public DescripcionErrorGeneric(String error, String solucion) {
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
