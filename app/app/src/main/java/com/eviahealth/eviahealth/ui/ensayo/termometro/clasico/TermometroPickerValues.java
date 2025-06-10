package com.eviahealth.eviahealth.ui.ensayo.termometro.clasico;

public class TermometroPickerValues {
    private TermometroPickerValues() {}

    public static double[] values;
    public static String[] display_values;

    // crear values del picker. Desde 34.0 hasta 45.0 con pasos de 0.1 ... 111 valores
    // vamos desde 340 hasta 450 y dividomos cada vez entre 10.
    static {
        double[] num_values = new double[111];
        String[] string_values = new String[111];
        int temp = 340;
        for(int i = 110; i >= 0; i--) {
            double true_temp = temp / 10.0; // tiene que ser 10.0, y no 10, si no se ejecuta una division entera en vez de division decimal.
            num_values[i] = true_temp;
            string_values[i] = Double.toString(true_temp);
            temp += 1;
        }
        TermometroPickerValues.values = num_values;
        TermometroPickerValues.display_values = string_values;
    }
}
