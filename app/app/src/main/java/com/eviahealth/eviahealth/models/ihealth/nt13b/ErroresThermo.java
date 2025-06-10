package com.eviahealth.eviahealth.models.ihealth.nt13b;


import com.eviahealth.eviahealth.models.errors.DescripcionErrorThermo;

import java.util.HashMap;
import java.util.Map;

public class ErroresThermo {
    private ErroresThermo() {}

    private static final Map<Integer, DescripcionErrorThermo> MAP = new HashMap<>();

    // devuelve null si le pasas un Integer que no esta en el MAP
    public static DescripcionErrorThermo DescripcionErrorThermo(Integer i) {
        return MAP.get(i);
    }

    static {
        // POR EL FABRICANTE
        MAP.put(0, new DescripcionErrorThermo("No se ha encontrado un valor adecuado.",
                "Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));

        // PROPIOS
        MAP.put(800, new DescripcionErrorThermo("No detectado que haya encendido el termómetro.",
                "Si ha pulsando el botón de ENCENDIDO y no se enciende compruebas las pilas.\r\n\nUna vez comprobado, pulse Reintentar."));

        MAP.put(801, new DescripcionErrorThermo("No hemos podido conectar con el termómetro.",
                "Apague el termómetro pulsando el botón de ENCENDIDO y vuelva a Reintentar.\r\n\nSi este error persiste cambie las pilas y vuelva a Reintentar."));

        MAP.put(802, new DescripcionErrorThermo("No ha pulsado el botón MEASURE de su termómetro.",
                "Apague el termómetro pulsando el botón de ENCENDIDO y vuelva a Reintentar."));

        MAP.put(803, new DescripcionErrorThermo("Error no esperado.",
                "Apague el termómetro pulsando el botón de ENCENDIDO y vuelva a Reintentar."));

        MAP.put(804, new DescripcionErrorThermo("No se ha detectado que haya pulsado el botón de MEASURE y haya realizado correctamente la medida.",
                "Apague el termómetro pulsando el botón de ENCENDIDO y vuelva a Reintentar."));

        MAP.put(805, new DescripcionErrorThermo("No se ha realizado la medida correctamente o temperatura baja (Lo)",
                "Apague el termómetro pulsando el botón de ENCENDIDO y vuelva a Reintentar."));

        // ERROR INDETERMINADO
        MAP.put(-1, new DescripcionErrorThermo("FAIL RESULT",
                "Compruebe que el termómetro tenga batería.\r\n\nUna vez comprobado, pulse Reintentar."));
    }

}