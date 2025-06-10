package com.eviahealth.eviahealth.models.devices;

import java.util.ArrayList;
import java.util.List;

public class FichaPacienteSpinnerValues {
    private FichaPacienteSpinnerValues(){}

    public static List<String> values_spinner_termometro;
    public static List<String> values_spinner_scale;
    public static List<String> values_spinner_peakflow;
    public static List<String> values_spinner_cat;
    public static List<String> values_spinner_cpap;
    public static List<String> values_spinner_log;

    public static final String HABILITADO = "Habilitado";
    public static final String DESHABILITADO = "Deshabilitado";
    public static final String CLASICO = "Clasico";

    static {
        values_spinner_termometro = new ArrayList<>();
        values_spinner_termometro.add(CLASICO);
        values_spinner_termometro.add(DESHABILITADO);

        values_spinner_scale = new ArrayList<>();
        values_spinner_scale.add(CLASICO);
        values_spinner_scale.add(DESHABILITADO);

        values_spinner_peakflow = new ArrayList<>();
        values_spinner_peakflow.add(HABILITADO);
        values_spinner_peakflow.add(DESHABILITADO);

        values_spinner_cat = new ArrayList<>();
        values_spinner_cat.add(HABILITADO);
        values_spinner_cat.add(DESHABILITADO);

        values_spinner_log = new ArrayList<>();
        values_spinner_log.add("Sin logs");
        values_spinner_log.add("Logs fichero");
        values_spinner_log.add("Logs Desarrollo");

    }

}
