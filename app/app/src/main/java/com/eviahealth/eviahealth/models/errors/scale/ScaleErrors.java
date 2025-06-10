package com.eviahealth.eviahealth.models.errors.scale;

import java.util.HashMap;
import java.util.Map;


public class ScaleErrors {

    private ScaleErrors() {}

    private static final Map<Integer, DescriptionErrorScale> MAP = new HashMap<>();

    // devuelve null si le pasas un Integer que no esta en el MAP
    public static DescriptionErrorScale getErrorDescription(Integer i) {
        return MAP.get(i);
    }

    static {


        // GENERIC
        MAP.put(800, new DescriptionErrorScale("No se ha detectado su báscula.",
                "Compruebe que la báscula está cerca de la Tablet, en caso contrario acérquela.\n" +
                        "Si su báscula está cerca compruebe si tiene batería, en caso contrario cambien las baterías por unas nuevas.",
                "Pulse Reintentar para realizar de nuevo la medición o Saltar para continuar con el ensayo."));
        MAP.put(801, new DescriptionErrorScale("Detectado problema con la conexión Bluetooth.",
                "Si su báscula está cerca compruebe si tiene batería, en caso contrario cambien las baterías por unas nuevas.",
                "Pulse Reintentar para realizar de nuevo la medición o Saltar para continuar con el ensayo."));
        MAP.put(802, new DescriptionErrorScale("Se ha producido un error inesperado con la conexión con la báscula.",
                "Compruebe que el dispositivo tiene batería, en caso contrario cambien las baterías por unas nuevas.",
                "Pulse Reintentar para realizar de nuevo la medición o Saltar para continuar con el ensayo."));
        MAP.put(803, new DescriptionErrorScale("Dispositivo: se ha desconectado inesperadamente.",
                "Compruebe que el dispositivo tiene batería, en caso contrario cambien las baterías por unas nuevas.",
                "Pulse Reintentar para realizar de nuevo la medición o Saltar para continuar con el ensayo."));
        MAP.put(804, new DescriptionErrorScale("Superado tiempo de descarga.",
                "Compruebe que el dispositivo tiene batería, en caso contrario cambien las baterías por unas nuevas.",
                "Pulse Reintentar para realizar de nuevo la medición o Saltar para continuar con el ensayo."));

        MAP.put(805, new DescriptionErrorScale("Se ha detectado un error mientras se realizaba la medída.",
                "Una vez iniciada la medición no se baje de la báscula hasta que esta no finalice.\r\n" +
                        "Baje de la báscula.\r\n\nPulse Reintentar, para volverse a realizar la medición",
                ""));

        MAP.put(806, new DescriptionErrorScale("Se ha detectado un proceso de vinculación con el dispositivo.",
                "La vincuación con el dispositivo ha fallado.\r\r\n\n" +
                        "Pulse Reintentar, para volverse a realizar la medición",
                ""));

        MAP.put(807, new DescriptionErrorScale("Se ha detectado un proceso de vinculación con el dispositivo.",
                "La vincuación con el dispositivo se ha realizado correctamente.\r\r\n\n" +
                        "Pulse Reintentar, para volverse a realizar la medición",
                ""));
        MAP.put(808, new DescriptionErrorScale("Error al realizar comunicación con el servidor EVIAHEALTH.",
                "Pulse Reintentar, para volverse a realizar la medición\r\r\n\n" +
                        "Si persiste el error CONTINUE con el ensayo.",
                ""));

        // OTHER ERROR
        MAP.put(-1, new DescriptionErrorScale("FAIL RESULT",
                "Compruebe que tiene colocado correctamente el dispositivo.\r\n\nUna vez comprobado, pulse Reintentar.",
                ""));
    }
}