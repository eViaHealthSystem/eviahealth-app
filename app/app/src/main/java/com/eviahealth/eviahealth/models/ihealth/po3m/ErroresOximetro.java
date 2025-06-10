package com.eviahealth.eviahealth.models.ihealth.po3m;

import java.util.HashMap;
import java.util.Map;

public class ErroresOximetro {
    private ErroresOximetro() {}

    private static final Map<Integer, DescripcionErrorPO> MAP = new HashMap<>();

    // devuelve null si le pasas un Integer que no esta en el MAP
    public static DescripcionErrorPO getDescripcionErrorPO(Integer i) {
            return MAP.get(i);
    }

    static {
        MAP.put(101, new DescripcionErrorPO("Error interno del Dispositivo.","Compruebe que tiene colocado correctamente el dispositivo.\r\n\nUna vez comprobado, pulse Reintentar."));

        MAP.put(700, new DescripcionErrorPO("Superado el tiempo de espera, para iniciar la medición.","Recuerde de Introducir el dedo en el dispositivo y PULSAR EL BOTÓN START.\r\n\nPulse Reintentar."));

        MAP.put(800, new DescripcionErrorPO("No detectado Dispositivo. (gatt)","Compruebe que el dispositivo tiene batería. En caso contrario pongalo a cargar.\r\n\nUna vez cargado Pulse Reintentar.\r\n\nRecuerde PULSAR EL BOTÓN START."));
        MAP.put(801, new DescripcionErrorPO("No detectado Dispositivo. (iHealth)","Compruebe que el dispositivo tiene batería. En caso contrario pongalo a cargar.\r\n\nUna vez cargado Pulse Reintentar.\r\n\nRecuerde PULSAR EL BOTÓN START."));
        MAP.put(802, new DescripcionErrorPO("Dispositivo: se ha desconectado inesperadamente.","Compruebe que el dispositivo tiene batería. En caso contrario pongalo a cargar.\r\n\nUna vez cargado Pulse Reintentar.\r\n\nRecuerde PULSAR EL BOTÓN START."));
        MAP.put(803, new DescripcionErrorPO("Superado los reintentos de conexión.","Compruebe que el dispositivo tiene batería. En caso contrario pongalo a cargar.\r\n\nUna vez cargado Pulse Reintentar\r\n\nRecuerde PULSAR EL BOTÓN START."));
        MAP.put(804, new DescripcionErrorPO("Recibida una acción no contemplada en el dispositivo.","Ponerse en contacto con el Servicio Técnico."));

        MAP.put(-1, new DescripcionErrorPO("FAIL RESULT","Compruebe que tiene colocado correctamente el dispositivo.\r\n\nUna vez comprobado, pulse Reintentar."));
    }

}
