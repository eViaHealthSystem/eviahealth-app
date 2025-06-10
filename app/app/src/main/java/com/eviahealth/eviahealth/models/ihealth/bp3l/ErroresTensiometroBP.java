package com.eviahealth.eviahealth.models.ihealth.bp3l;

import java.util.HashMap;
import java.util.Map;

public class ErroresTensiometroBP {
    private ErroresTensiometroBP() {}

    private static final Map<Integer, DescripcionErrorBP> MAP = new HashMap<>();

    // devuelve null si le pasas un Integer que no esta en el MAP
    public static DescripcionErrorBP getDescripcionErrorBP(Integer i) {
        return MAP.get(i);
    }

    static {
        // POR EL TENSIOMETRO
        MAP.put(0, new DescripcionErrorBP("No se ha encontrado un valor adecuado.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(1, new DescripcionErrorBP("No se ha encontrado presión alta.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(2, new DescripcionErrorBP("No se ha encontrado presión baja o la presión alta es menor que la baja.","\r\n\nCompruebe que tiene colocado correctamente el tensiómetro.\r\nUna vez comprobado, pulse Reintentar."));
        MAP.put(3, new DescripcionErrorBP("Presurización demasiado rápida.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(4, new DescripcionErrorBP("Presurización demasiado lenta.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(5, new DescripcionErrorBP("El valor de la presión supera los 300mmHg.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(6, new DescripcionErrorBP("El tiempo de presión superior a 15 mmHg supera los 160s.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(7, new DescripcionErrorBP("Error interno del tensiómetro.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(8, new DescripcionErrorBP("Error interno del tensiómetro.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(9, new DescripcionErrorBP("Retención!!!.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(10, new DescripcionErrorBP("Error interno del tensiómetro.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(11, new DescripcionErrorBP("Error interno del tensiómetro.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(12, new DescripcionErrorBP("Error de conexión con el tensiómetro.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(13, new DescripcionErrorBP("Batería baja.","Compruebe que el tensiómetro esté conectado a una fuente de alimentación.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(14, new DescripcionErrorBP("El valor de la presión alta o baja supera el límite máximo.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(16, new DescripcionErrorBP("El valor de la presión alta o baja está por debajo del mínimo.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(17, new DescripcionErrorBP("Movimiento del brazo durante la medición.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(101, new DescripcionErrorBP("Error interno del tensiómetro.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(400, new DescripcionErrorBP("Error interno del tensiómetro.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(402, new DescripcionErrorBP("Error interno del tensiómetro.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));

        // PROPIOS
        MAP.put(800, new DescripcionErrorBP("No detectado dispositivo. (gatt)","Compruebe que el dispositivo está conectado.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(801, new DescripcionErrorBP("No detectado dispositivo. (iHealth)","Compruebe que el dispositivo está conectado.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(802, new DescripcionErrorBP("Tensiómetro: se ha desconectado inesperadamente.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(803, new DescripcionErrorBP("Superado los reintentos de conexión.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
        MAP.put(804, new DescripcionErrorBP("Recibida una acción no contemplada en el dispositivo.","Ponerse en contacto con el Servicio Técnico."));
        MAP.put(805, new DescripcionErrorBP("Pulsado el botón de STOP del dispositivo.","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));

        // poner mas descripciones aqui

        MAP.put(-1, new DescripcionErrorBP("FAIL RESULT","Compruebe que tiene colocado correctamente el tensiómetro.\r\n\nUna vez comprobado, pulse Reintentar."));
    }

}
