package com.eviahealth.eviahealth.models.alivecor.models;

import java.util.ArrayList;
import java.util.List;

public enum EDeterminationKAIv2 {
    UNKNOWN("Unknown", "Determinación no encontrada."),
    NORMAL("normal", "normal"),
    AFIB("afib", "Indica que se detectó fibrilación auricular, un ritmo cardíaco irregular, en el electrocardiograma. Este hallazgo se puede detectar a cualquier frecuencia cardíaca."),
    UNCLASSIFIED("unclassified", "No se detectó fibrilación auricular y el electrocardiograma no entra en las clasificaciones algorítmicas de normal, bradicardia o taquicardia. Esto puede ser causado por otras arritmias, frecuencias cardíacas inusualmente rápidas o lentas, o grabaciones de mala calidad."),
    BRADYCARDIA("bradycardia", "Su frecuencia cardíaca es inferior a 50 latidos por minuto, que es más lenta de lo normal para la mayoría de las personas. No se detectó fibrilación auricular."),
    TACHYCARDIA("tachycardia", "Su frecuencia cardíaca es más rápida que 100 latidos por minuto. Esto puede ser normal con el estrés o la actividad física. No se detectó fibrilación auricular."),
    SHORT("too_short", "El registro del electrocardiograma debe durar al menos 30 segundos para permitir que los algoritmos de análisis instantáneo realicen un análisis."),
    LONG("too_long", "too_long"),
    UNREADABLE("unreadable", "Hay demasiada interferencia en esta grabación. Vuelva a grabar el electrocardiograma. Trate de relajarse y quedarse quieto, descansar los brazos o moverse a un lugar tranquilo o lejos de los aparatos electrónicos y la maquinaria."),
    NO_ANALYSIS("no_analysis", "No hay un análisis instantáneo para este electrocardiograma."),
    SINUS_RHYTHM("sinus_rhythm", "Indica un ritmo sinusal normal sin que se hayan detectado anomalías del ritmo o de la frecuencia cardíaca; Su frecuencia cardíaca era de 50 a 100 latidos por minuto (lpm)."),
    SINUS_RHYTHM_PACS("sinus_rhythm,multiple_pacs", "El electrocardiograma muestra el ritmo sinusal con ectopia supraventricular (SVE) ocasional. Esto puede estar presente en adultos sanos y en adultos con afecciones cardíacas."),
    SINUS_RHYTHM_WIDE_QRS("sinus_rhythm,wide_qrs", "El electrocardiograma muestra el ritmo sinusal con QRS ancho. Esto puede estar presente en adultos sanos y en adultos con afecciones cardíacas."),
    SINUS_RHYTHM_PVCS("sinus_rhythm,multiple_pvcs", "El electrocardiograma muestra el ritmo sinusal con contracciones ventriculares prematuras (PVC) ocasionales. Esto puede estar presente en adultos sanos y en adultos con afecciones cardíacas.");

    private String code, msg;

    public String getCode() {
        return this.code;
    }

    public String getMsg() { return this.msg; }

    private EDeterminationKAIv2(String var3, String var4) {
        this.code = var3;
        this.msg = var4;
    }

    public static EDeterminationKAIv2 toErrorCode(String code) {
        for (EDeterminationKAIv2 determination : values()) {
            if (determination.getCode().equals(code)) {
                return determination;
            }
        }
        return UNKNOWN;
    }

    // Método para obtener todos los códigos
    public static List<String> getAllCodes() {
        List<String> codes = new ArrayList<>();
        for (EDeterminationKAIv2 determination : values()) {
            codes.add(determination.getCode());
        }
        return codes;
    }

    // Método para obtener todos los mensajes
    public static List<String> getAllMessages() {
        List<String> messages = new ArrayList<>();
        for (EDeterminationKAIv2 determination : values()) {
            messages.add(determination.getMsg());
        }
        return messages;
    }

    // Método para obtener el mensaje (msg) por código (code)
    public static String getMessageByCode(String code) {
        EDeterminationKAIv2 determination = toErrorCode(code);
        return determination.getMsg();
    }
    /*
        String message1 = EDeterminationKAIv2.getMessageByCode(code1);
        // Mostrar resultados
        System.out.println("Code: " + code1 + " -> Message: " + message1);
     */
}
