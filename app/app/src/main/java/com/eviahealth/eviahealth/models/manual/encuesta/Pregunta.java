package com.eviahealth.eviahealth.models.manual.encuesta;


import java.util.List;

public class Pregunta {

    private String texto_pregunta;
    private Integer id_cat;
    private Integer respuesta;
    private TipoPregunta tipo_pregunta;
    private List<Respuesta> posibles_respuestas;
    private int id;

    public Pregunta(int id, String pregunta, Integer cat, TipoPregunta tipo_pregunta, List<Respuesta> lista_respuestas) {
        this.id = id;
        this.texto_pregunta = pregunta;
        this.id_cat = cat;
        this.tipo_pregunta = tipo_pregunta;
        this.respuesta = null;
        this.posibles_respuestas = lista_respuestas;
    }

    public int getId() { return this.id; }

    public void setRespuesta(int respuesta) {
        this.respuesta = respuesta;
    }

    public Integer getRespuesta() {
        return this.respuesta;
    }

    public Integer getCAT() { return this.id_cat; }

    public TipoPregunta getTipo_pregunta() { return this.tipo_pregunta; }

    public String getTexto() { return this.texto_pregunta; }

    public List<Respuesta> getPosibles_respuestas() { return this.posibles_respuestas; }
}
