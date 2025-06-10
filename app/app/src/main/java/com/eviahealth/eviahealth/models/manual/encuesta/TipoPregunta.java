package com.eviahealth.eviahealth.models.manual.encuesta;

import java.util.HashMap;
import java.util.Map;

public enum TipoPregunta {
    DOS_RESPUESTAS(0), TRES_RESPUESTAS(1), CINCO_RESPUESTAS(2), CUATRO_RESPUESTAS(3);

    private static final Map<Integer, TipoPregunta> enum_map = new HashMap<>();
    static {
        for(TipoPregunta tp: TipoPregunta.values()) {
            enum_map.put(tp.id, tp);
        }
    }

    public static TipoPregunta fromInt(int tipo) {
        return enum_map.get(tipo);
    }

    private int id;
    TipoPregunta(int tipo) {
        this.id = tipo;
    }

    public int getInt() {
        return this.id;
    }

}
