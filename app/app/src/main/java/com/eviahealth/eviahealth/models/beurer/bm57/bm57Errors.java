package com.eviahealth.eviahealth.models.beurer.bm57;

import com.eviahealth.eviahealth.models.errors.DescripcionErrorGeneric;

import java.util.HashMap;
import java.util.Map;

public class bm57Errors {

    private bm57Errors() {}

    private static final Map<Integer, DescripcionErrorGeneric> MAP = new HashMap<>();

    // devuelve null si le pasas un Integer que no esta en el MAP
    public static DescripcionErrorGeneric getDescripcionError(Integer i) {
        return MAP.get(i);
    }

    static {
        // Er1, Er2
        MAP.put(1, new DescripcionErrorGeneric(
                "Error: Se ha movido o ha hablado durante la medición.",
                "Repita la medición tras una pausa de un minuto.\r\nAsegúrese de no hablar ni moverse durante la medición.\r\n\nPulse Reintentar."));
        // Er3
        MAP.put(2, new DescripcionErrorGeneric(
                "Error: No se ha colocado correctamemnte el brazalete.",
                "Compruebe que tiene colocado correctamente el dispositivo.\r\n\nUna vez comprobado, pulse Reintentar."));
        // Er5
        MAP.put(3, new DescripcionErrorGeneric(
                "Error: La presión de inflado es superior a 300 mmHg o la presion arterial medida está fuera del rango de medición.",
                "Repita la medición tras una pausa de un minuto.\r\nAsegúrese de no hablar ni moverse durante la medición.\r\n\nPulse Reintentar."));

        MAP.put(4, new DescripcionErrorGeneric(
                "Error: El pulso detectado se encuentra por debajo del limite inferior.",
                "Compruebe que tiene colocado correctamente el dispositivo.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(5, new DescripcionErrorGeneric(
                "Error: Detectado un problema con la medición del pulso.",
                "Compruebe que tiene colocado correctamente el dispositivo.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(6, new DescripcionErrorGeneric(
                "Error: Detectada posición incorrecta del dispositivo.",
                "Compruebe que tiene colocado correctamente el dispositivo.\r\n\nUna vez comprobado, pulse Reintentar."));

        MAP.put(801, new DescripcionErrorGeneric(
                "No detectado Dispositivo. (Beurer)",
            "Compruebe que el dispositivo tiene batería. En caso contrario cambien las baterías.\n\nUna vez cargado Pulse Reintentar.\n\nRecuerde PULSAR EL BOTÓN INICIO.\n\nSi el equipo tiene batería y a pulsado el botón de incio pongase en contacto con el servicio técnico."));
        MAP.put(802, new DescripcionErrorGeneric(
                "Ha fallado la descarga de datos con el dispositivo.",
                "Compruebe que el dispositivo tiene batería.\nEn caso contrario cambien las pilas. (Er6)\n\nPulse Reintentar."));
        MAP.put(803, new DescripcionErrorGeneric(
                "Detectado un fallo de comunicación con el dispositivo.",
                "Compruebe que el dispositivo tiene batería.\nEn caso contrario cambien las pilas. (Er6)\n\nPulse Reintentar."));
        MAP.put(804, new DescripcionErrorGeneric(
                "Recibida una acción no contemplada en el dispositivo.",
                "Termine el ensayo y pogase en contacto con el Servicio Técnico."));

        MAP.put(-1, new DescripcionErrorGeneric(
                "DETECTADO ERROR.",
                "Compruebe si su aparato muestra un mensaje de error:" +"\n\n" +
                        "EE1:     No se ha podido medir la tensión.\n" +
                        "             Repita la medición tras una pausa de un minuto." +"\n" +
                        "EE2:     El brazalete está defectuoso, se ha colocado demasiado tenso o demasiado flojo.\n" +
                        "             Siga las instruciones que se muestran." +"\n" +
                        "EE8:     La presión de inflado es superior a 290 mmHg.\n" +
                        "             Repita la medición tras una pausa de un minuto." +"\n" +
                        "\nUna vez solucionado, pulse Reintentar."));
    }
}
