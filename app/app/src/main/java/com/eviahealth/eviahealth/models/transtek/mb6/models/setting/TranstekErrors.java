package com.eviahealth.eviahealth.models.transtek.mb6.models.setting;

import java.util.HashMap;
import java.util.Map;

public class TranstekErrors {

    private TranstekErrors() {}

    private static final Map<Integer, DescriptionErrorTranstek> MAP = new HashMap<>();

    // devuelve null si le pasas un Integer que no esta en el MAP
    public static DescriptionErrorTranstek getErrorDescription(Integer i) {
        return MAP.get(i);
    }

    static {

        // MAMBO 6
        MAP.put(100, new DescriptionErrorTranstek("Error descargando estado de la batería.","Compruebe que el dispositivo tiene batería. En caso contrario pongalo a cargar.\r\n\nUna vez cargado Pulse Reintentar."));

        // GENERIC
        MAP.put(800, new DescriptionErrorTranstek("No detectado Dispositivo.","Compruebe que el dispositivo tiene batería. En caso contrario pongalo a cargar.\r\n\nUna vez cargado Pulse Reintentar."));
        MAP.put(801, new DescriptionErrorTranstek("Detectado problema con la conexión Bluetooth.","Pongase en contacto con el Servicio Técnico."));
        MAP.put(802, new DescriptionErrorTranstek("No se ha podido establecer conexión con el dispositivo.","Compruebe que el dispositivo tiene batería. En caso contrario pongalo a cargar.\r\n\nUna vez cargado Pulse Reintentar."));
        MAP.put(803, new DescriptionErrorTranstek("Dispositivo: se ha desconectado inesperadamente.","Compruebe que el dispositivo tiene batería. En caso contrario pongalo a cargar.\r\n\nUna vez cargado Pulse Reintentar."));
        MAP.put(804, new DescriptionErrorTranstek("Superado tiempo de descarga.","Compruebe que el dispositivo tiene batería. En caso contrario pongalo a cargar.\r\n\nUna vez cargado Pulse Reintentar."));




        MAP.put(814, new DescriptionErrorTranstek("Superado los reintentos de conexión.","Compruebe que el dispositivo tiene batería. En caso contrario pongalo a cargar.\r\n\nUna vez cargado Pulse Reintentar."));
        MAP.put(815, new DescriptionErrorTranstek("Recibida una acción no contemplada en el dispositivo.","Ponerse en contacto con el Servicio Técnico."));

        // OTHER ERROR
        MAP.put(-1, new DescriptionErrorTranstek("FAIL RESULT","Compruebe que tiene colocado correctamente el dispositivo.\r\n\nUna vez comprobado, pulse Reintentar."));
    }
}
