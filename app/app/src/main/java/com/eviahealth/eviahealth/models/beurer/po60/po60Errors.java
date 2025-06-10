package com.eviahealth.eviahealth.models.beurer.po60;

import com.eviahealth.eviahealth.models.errors.DescripcionErrorGeneric;

import java.util.HashMap;
import java.util.Map;

public class po60Errors {
    private po60Errors() {}

    private static final Map<Integer, DescripcionErrorGeneric> MAP = new HashMap<>();

    // devuelve null si le pasas un Integer que no esta en el MAP
    public static DescripcionErrorGeneric getDescripcionErrorPO(Integer i) {
            return MAP.get(i);
    }

    static {
        MAP.put(101, new DescripcionErrorGeneric("Error interno del Dispositivo.","Compruebe que tiene colocado correctamente el dispositivo.\r\n\nUna vez comprobado, pulse Reintentar."));

        MAP.put(700, new DescripcionErrorGeneric("Superado el tiempo de espera, para iniciar la medición.","Recuerde de Introducir el dedo en el dispositivo y PULSAR EL BOTÓN START.\r\n\nPulse Reintentar."));

        MAP.put(800, new DescripcionErrorGeneric("No detectado Dispositivo. (gatt)","Compruebe que el dispositivo tiene batería. En caso contrario cambien las baterías.\r\n\nUna vez cargado Pulse Reintentar.\r\n\nRecuerde PULSAR EL BOTÓN START."));
        MAP.put(801, new DescripcionErrorGeneric("No detectado Dispositivo. (Beurer)",
                "Compruebe que el dispositivo tiene batería. En caso contrario cambien las baterías.\r\n\nUna vez cargado Pulse Reintentar.\r\n\nRecuerde PULSAR EL BOTÓN START."));
        MAP.put(802, new DescripcionErrorGeneric("Ha fallado la descarga de datos con el dispositivo.","Compruebe que el dispositivo tiene batería. En caso contrario cambien las baterías.\r\n\nUna vez cargado Pulse Reintentar.\r\n\nRecuerde PULSAR EL BOTÓN START."));
        MAP.put(803, new DescripcionErrorGeneric("Detectado un fallo de comunicación con el dispositivo.","Compruebe que el dispositivo tiene batería. En caso contrario cambien las baterías..\r\n\nUna vez cargado Pulse Reintentar\r\n\nRecuerde PULSAR EL BOTÓN START."));
        MAP.put(804, new DescripcionErrorGeneric("Recibida una acción no contemplada en el dispositivo.","Ponerse en contacto con el Servicio Técnico."));
        MAP.put(805, new DescripcionErrorGeneric("Transcurrido tiempo máximo para realizarse la medición.","Compruebe que el dispositivo tiene batería. En caso contrario cambien las baterías..\r\n\nUna vez cargado Pulse Reintentar\r\n\nRecuerde PULSAR EL BOTÓN START."));

        MAP.put(-1, new DescripcionErrorGeneric("FAIL RESULT","Compruebe que tiene colocado correctamente el dispositivo.\r\n\nUna vez comprobado, pulse Reintentar."));
    }

}
