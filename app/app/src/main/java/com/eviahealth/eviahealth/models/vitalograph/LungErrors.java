package com.eviahealth.eviahealth.models.vitalograph;

import java.util.HashMap;
import java.util.Map;

public class LungErrors {

    private LungErrors() {}

    private static final Map<Integer, DescripcionErrorLung> MAP = new HashMap<>();

    // devuelve null si le pasas un Integer que no esta en el MAP
    public static DescripcionErrorLung DescripcionErrorLung(Integer i) {
        return MAP.get(i);
    }

    static {
        // POR EL FABRICANTE
        MAP.put(0, new DescripcionErrorLung("No se ha encontrado un valor adecuado.",
                "Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));

        // PROPIOS
        MAP.put(800, new DescripcionErrorLung("No se ha detectado su monitor pulmonar.",
                "Si ha pulsado el botón de ENCENDIDO y no se enciende compruebe las pilas.\r\n\n"
                        + "Compruebe en la pantalla del dispositivo que no haya ningún mensaje de error.\r\n\n"
                        + "Una vez comprobado, pulse Reintentar."));

        MAP.put(801, new DescripcionErrorLung("Reintos de conexión superados.",
                "Si ha pulsado el botón de ENCENDIDO y no se enciende compruebe las pilas.\r\n\n"
                        + "Compruebe en la pantalla del dispositivo que no haya ningún mensaje de error.\r\n\n"
                        + "Una vez comprobado, pulse Reintentar."));

        MAP.put(802, new DescripcionErrorLung("Se ha superado el límite de realización del test.",
                "Compruebe que el dispositivo esté encendido y que no haya ningún mensaje de error.\r\n\n"
                        + "APAGUE el dispositivo y pulse 'Reintentar'.\r\n\n"
                        + "Si este error persiste cambie las pilas y pulse 'Reintentar'."));

        MAP.put(803, new DescripcionErrorLung("Error al realizar comunicación con el servidor EVIAHEALTH",
                "Intentelo de nuevo y si este error persiste contacte con el Servicio Técnico'."));

        // ERROR INDETERMINADO
        MAP.put(-1, new DescripcionErrorLung("FAIL RESULT",
                "Compruebe que el termómetro tenga batería.\r\n\nUna vez comprobado, pulse Reintentar."));
    }
}
