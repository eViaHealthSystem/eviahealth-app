package com.eviahealth.eviahealth.models.errors.scale;

public class DescriptionErrorScale {
    private String error;
    private String solucion;
    private String  continuar;

    private DescriptionErrorScale() {}

    public DescriptionErrorScale(String error, String solucion, String continuar) {
        this.error = error;
        this.solucion = solucion;
        this.continuar = continuar;
    }

    public String getError() {
        return this.error;
    }

    public String getSolucion() {
        return this.solucion;
    }

    public String getContinuar() {
        return this.continuar;
    }
}