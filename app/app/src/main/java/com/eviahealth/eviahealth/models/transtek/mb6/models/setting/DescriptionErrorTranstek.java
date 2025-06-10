package com.eviahealth.eviahealth.models.transtek.mb6.models.setting;

public class DescriptionErrorTranstek {
    private String error;
    private String solucion;

    private DescriptionErrorTranstek() {}

    public DescriptionErrorTranstek(String error, String solucion) {
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
