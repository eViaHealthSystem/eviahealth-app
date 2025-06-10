package com.eviahealth.eviahealth.models.ihealth.am4;

import com.eviahealth.eviahealth.models.errors.DescripcionErrorGeneric;

import java.util.HashMap;
import java.util.Map;

public class ErroresPulsera {
    private ErroresPulsera() {}

    private static final Map<Integer, DescripcionErrorGeneric> MAP = new HashMap<>();

    // devuelve null si le pasas un Integer que no esta en el MAP
    public static DescripcionErrorGeneric getDescripcionErrorACT(Integer i) {
            return MAP.get(i);
    }

    static {

        MAP.put(400, new DescripcionErrorGeneric("El parámetro min de setAlarmClock() debe estar en el rango [0, 59]","Resetee el Dispositivo."));

        MAP.put(800, new DescripcionErrorGeneric("No detectado Dispositivo. (gatt)","Compruebe que el dispositivo tiene batería. En caso contrario póngalo a cargar.\r\n\nUna vez cargado Pulse Reintentar."));
        MAP.put(801, new DescripcionErrorGeneric("No detectado Dispositivo. (iHealth)","Compruebe que el dispositivo tiene batería. En caso contrario póngalo a cargar.\r\n\nUna vez cargado Pulse Reintentar."));
        MAP.put(802, new DescripcionErrorGeneric("Dispositivo: se ha desconectado inesperadamente.","Compruebe que el dispositivo tiene batería. En caso contrario póngalo a cargar.\r\n\nUna vez cargado Pulse Reintentar."));
        MAP.put(803, new DescripcionErrorGeneric("Superado los reintentos de conexión.","Compruebe que el dispositivo tiene batería. En caso contrario póngalo a cargar.\r\n\nUna vez cargado Pulse Reintentar."));
        MAP.put(804, new DescripcionErrorGeneric("Recibida una acción no contemplada del dispositivo.","Póngase en contacto con el Servicio Técnico."));

        MAP.put(-1, new DescripcionErrorGeneric("FAIL RESULT","Compruebe que tiene colocado correctamente el dispositivo.\r\n\nUna vez comprobado, pulse Reintentar."));
    }

}
