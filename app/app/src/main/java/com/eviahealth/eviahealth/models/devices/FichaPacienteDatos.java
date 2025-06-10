package com.eviahealth.eviahealth.models.devices;

import com.eviahealth.eviahealth.devices.NombresDispositivo;
import com.eviahealth.eviahealth.utils.Fecha.Hora;

import java.util.Map;

public class FichaPacienteDatos {

    private Map<NombresDispositivo, EquipoPaciente> equipos;
    private int id_encuesta;
    private Hora hora_ensayo;
    private int logs;

    private FichaPacienteDatos() {}

    public
    FichaPacienteDatos(Map<NombresDispositivo, EquipoPaciente> equipos, int id_encuesta, int logs) {
        this.equipos = equipos;
        this.id_encuesta = id_encuesta;
        this.logs = logs;
    }

    public Map<NombresDispositivo, EquipoPaciente> getEquipos() {
        return equipos;
    }

    public int getId_encuesta() {
        return id_encuesta;
    }

    public Hora getHora_ensayo() {
        return hora_ensayo;
    }

    public int getLogs() {
        return logs;
    }
}
