package com.eviahealth.eviahealth.models.devices;

import org.json.JSONException;
import org.json.JSONObject;

public class Dispositivo {

    private Dispositivo() {}

    private boolean enabled;
    private String identificador;
    private String nombre;
    private Integer id;
    private String extra;
    private Class<?> actividad;

    public Dispositivo(boolean enabled, String identificador, String nombre, int id, String extra) {
        this.identificador = identificador;
        this.enabled = enabled;
        this.nombre = nombre;
        this.id = id;
        this.extra = extra;
        this.actividad = null;
    }

    public Dispositivo(boolean enabled, String identificador, String nombre, int id, String extra, Class<?> actividad) {
        this.identificador = identificador;
        this.enabled = enabled;
        this.nombre = nombre;
        this.id = id;
        this.extra = extra;
        this.actividad = actividad;
    }

    public Dispositivo(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return this.nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getIdentificador() {
        return this.identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setExtra(String value) { this.extra = value; }

    public String getExtra() { return this.extra; }

    public Class getActividad() {
        return actividad;
    }

    public void setActividad(Class actividad) {
        this.actividad = actividad;
    }

    public String toString() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("nombre", nombre);

            if (actividad != null)
                obj.put("class", actividad.getName());
            else {
                obj.put("class", null);
            }

            obj.put("id", id);
            obj.put("enabled", enabled);
            obj.put("identificador", identificador);
            obj.put("extra", extra);
            return obj.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

}
