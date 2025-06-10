package com.eviahealth.eviahealth.devices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TranstekDevice {
    // Listado de los dispositivos aceptados
    public static List<String> device = new ArrayList<>(Arrays.asList("MAMBO6","GBS2012B"));

    // dispositivos que necesitan configuración inicial -------------------------------------------
    public static List<String> deviceOXI_CNF = new ArrayList<>();
    public static List<String> deviceTEN_CNF = new ArrayList<>();
    public static List<String> deviceACT_CNF = new ArrayList<>(Arrays.asList("MAMBO6"));
    public static List<String> deviceSCALE_CNF = new ArrayList<>();
    // --------------------------------------------------------------------------------------------
}
