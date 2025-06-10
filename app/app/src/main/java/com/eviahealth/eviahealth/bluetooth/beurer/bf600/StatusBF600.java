package com.eviahealth.eviahealth.bluetooth.beurer.bf600;

public enum StatusBF600 {
    FUNCTION_01(0x01, "Éxito"),
    FUNCTION_02(0x02, "Código OP no compatible"),
    FUNCTION_03(0x03, "Parámetro no válido"),
    FUNCTION_04(0x04, "Error en la operación"),
    FUNCTION_05(0x05, "Usuario no autorizado");

    private final int value;
    private final String text;

    StatusBF600(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public static String getTextByValue(int value) {
        for (StatusBF600 function : StatusBF600.values()) {
            if (function.getValue() == value) {
                return function.getText();
            }
        }
        return null; // o puedes lanzar una excepción si prefieres
    }
}

/* Ejemplo de uso:
    int value = 0x01;
    String text = bf600Function.getTextByValue(value);
 */