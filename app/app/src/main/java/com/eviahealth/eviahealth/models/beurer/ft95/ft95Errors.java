package com.eviahealth.eviahealth.models.beurer.ft95;

import com.eviahealth.eviahealth.models.errors.DescripcionErrorThermo;

import java.util.HashMap;
import java.util.Map;

public class ft95Errors {
    private ft95Errors() {}

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
        MAP.put(800, new DescripcionErrorThermo("No se ha detectado medición con su termómetro.",
                "Si ha pulsado el botón de ENCENDIDO y no se enciende compruebe las pilas.\r\n\n"
                        + "Compruebe en la pantalla del termómetro que no haya ningún mensaje de error.\r\n\n"
                + "Una vez comprobado, pulse Reintentar."));

        MAP.put(801, new DescripcionErrorThermo("No se pudo descargar sus datos de medición.",
                "Compruebe en la pantalla del termómetro que no haya ningún mensaje de error.\r\n\n"
                        + "Una vez comprobado. Apague el termómetro pulsando el botón de ENCENDIDO y vuelva a Reintentar.\r\n\n"
                        + "Si este error persiste cambie las pilas y vuelva a Reintentar."));

        // ERROR INDETERMINADO
        MAP.put(-1, new DescripcionErrorThermo("FAIL RESULT",
                "Compruebe que el termómetro tenga batería.\r\n\nUna vez comprobado, pulse Reintentar."));
    }

}
