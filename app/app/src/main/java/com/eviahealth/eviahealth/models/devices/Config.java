package com.eviahealth.eviahealth.models.devices;

import com.eviahealth.eviahealth.devices.NombresDispositivo;

import java.util.EnumMap;
import java.util.Map;

public class Config {

    // Dispositivos
    private Map<NombresDispositivo, Dispositivo> dispositivos;
    // idpaciente
    private String idPacienteTablet = null;
    private Boolean multipaciente = false;
    private String idPacienteEnsayo = null;

    // SINGLETON
    private static Config instance = null;
    public static Config getInstance() {
        if(instance == null) {
            instance = new Config();
        }
        return instance;
    }

    private Config() {
        this.dispositivos = new EnumMap<>(NombresDispositivo.class);
    }

    public Map<NombresDispositivo, Dispositivo> getDispositivos() {
        return dispositivos;
    }

    public String getIdentificador(NombresDispositivo ndisp) {
        Dispositivo disp = this.dispositivos.get(ndisp);
        if(disp != null) {
            return disp.getIdentificador();
        } else {
            return "";
        }
    }

    public String getIdPacienteTablet() {
        return idPacienteTablet;
    }

    public void setIdPacienteTablet(String idPacienteTablet) {
        this.idPacienteTablet = idPacienteTablet;
    }

    public Boolean getMultipaciente() { return this.multipaciente; }
    public void setMultipaciente(Boolean multipaciente) { this.multipaciente = multipaciente; }

    public String getIdPacienteEnsayo() {
        return idPacienteEnsayo;
    }
    public void setIdPacienteEnsayo(String idPacienteEnsayo) {
        this.idPacienteEnsayo = idPacienteEnsayo;
    }

    public String getExtra(NombresDispositivo ndisp) {
        Dispositivo disp = this.dispositivos.get(ndisp);
        if(disp != null) {
            return disp.getExtra();
        } else {
            return "{}";
        }
    }
}
