package com.eviahealth.eviahealth.devices;

import com.eviahealth.eviahealth.ui.ensayo.bascula.clasico.get_dataClassicScale;
import com.eviahealth.eviahealth.ui.ensayo.encuesta.Encuesta;
import com.eviahealth.eviahealth.ui.ensayo.peakflow.PeakFlow;
import com.eviahealth.eviahealth.ui.ensayo.termometro.clasico.Termometro;

import java.util.HashMap;
import java.util.Map;

public enum NombresDispositivo {

    OXIMETRO("Oximetro","OXI", null),              // Se utiliza la clase ClassDispositivo para asignar la activity
    TENSIOMETRO("Tensiometro", "TEN", null),       // Se utiliza la clase ClassDispositivo para asignar la activity
    ACTIVIDAD("Actividad Total","ACT", null),      // Se utiliza la clase ClassDispositivo para asignar la activity
    BASCULA("Bascula","BAS", get_dataClassicScale.class),  // Se utiliza la clase ClassDispositivo para asignar la activity si es clasico
    MONITORPULMONAR("Monitor Pulmonar","MON", null), // Se utiliza la clase ClassDispositivo para asignar la activity
    ECG("ECG","ECG", null),                         // Se utiliza la clase ClassDispositivo para asignar la activity
    TERMOMETRO("Termometro","TER", Termometro.class),     // Para el termometro clasico
    PEAKFLOW("PeakFlow","PEAK", PeakFlow.class),
    ENCUESTA("Encuesta","ENC", Encuesta.class),
    CAT("CAT","CAT", null);

    private String nombre;
    private String diminutivo;
    private Class activity;

    private static Map<String, NombresDispositivo> enum_map = new HashMap<>();
    static {
        for(NombresDispositivo ndisp: NombresDispositivo.values()) {
            enum_map.put(ndisp.nombre, ndisp);
        }
    }
    public static NombresDispositivo fromName(String nombre) {
        return enum_map.get(nombre);
    }

    NombresDispositivo(String nombre, String diminutivo, Class activity) {
        this.nombre = nombre;
        this.diminutivo = diminutivo;
        this.activity = activity;
    }

    public String getNombre() { return this.nombre; }

    public void setNombre(String name) { this.nombre = name; }

    public String getDiminutivo() { return diminutivo; }

    public Class getActivity() { return activity; }

}
