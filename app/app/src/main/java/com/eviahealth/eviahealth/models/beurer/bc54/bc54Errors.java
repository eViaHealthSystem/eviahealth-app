package com.eviahealth.eviahealth.models.beurer.bc54;

import com.eviahealth.eviahealth.models.errors.DescripcionErrorGeneric;

import java.util.HashMap;
import java.util.Map;

public class bc54Errors {

    private bc54Errors() {}

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
                "Compruebe que el dispositivo tiene batería.\nEn caso contrario cambien las pilas. (Er6)\n\nPulse Reintentar."));
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
                        "Er1:     No se ha podido registrar ningún pulso.\n" +
                        "             Repita la medición tras una pausa de un minuto." +"\n" +
                        "Er2:     Se ha movido o ha hablado durante la medición.\n" +
                        "             Asegúrese de no hablar ni moverse durante la medición." +"\n" +
                        "Er3:     No se ha colocado correctamente el brazalete.\n" +
                        "             Siga las instruciones que se muestran." +"\n" +
                        "Er4:     Se ha producido un error durante la medición.\n" +
                        "             Repita la medición tras una pausa de un minuto." + "\n" +
                        "Er5:     Presión del inflador alta o presión arterial fura de rango.\n" +
                        "             Repita la medición tras una pausa de un minuto." +"\n" +
                        "Er6:     Las pilas están casi gastadas.\n" +
                        "             Introduzca nuevas pilas en el aparato." +"\n" +
                        "Er7:     Los datos no se han podido transmitir.\n" +
                        "             Pongase en contacto con el servicio técnico." +"\n" +
                        "Er8:     Se ha producido un error del aparato.\n" +
                        "             Repita la medición tras una pausa de un minuto." +"\n" +
                        "\nUna vez solucionado, pulse Reintentar."));
    }
}
