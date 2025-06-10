package com.eviahealth.eviahealth.ui.ensayo.peakflow;

public class PeakFlowPickerValues {
    private PeakFlowPickerValues() {}

    public static int[] values;
    public static String[] display_values;

    static {
        // crear values del picker
        int[] values = new int[18];
        String[] display_values = new String[18];
        values[17] = 0;
        values[16] = 60;
        values[0] = 900;
        display_values[17] = "0";
        display_values[16] = "60";
        display_values[0] = "900";

        int value_iter = 100;
        for(int i = 15; i >= 1; i--) {
            values[i] = value_iter;
            display_values[i] = Integer.toString(value_iter);
            value_iter += 50;
        }
        PeakFlowPickerValues.values = values;
        PeakFlowPickerValues.display_values = display_values;
    }
}
