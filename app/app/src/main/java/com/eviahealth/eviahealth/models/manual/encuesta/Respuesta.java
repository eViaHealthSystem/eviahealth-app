package com.eviahealth.eviahealth.models.manual.encuesta;

public class Respuesta {
    private int valor;
    private String texto;

    public Respuesta(String text, int valor) {
        this.texto = text;
        this.valor = valor;
    }

    public int getValor() {
        return this.valor;
    }

    public String getTexto() {
        return this.texto;
    }
}
