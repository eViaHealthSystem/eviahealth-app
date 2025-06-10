package com.eviahealth.eviahealth.bluetooth.beurer.ft95;

import com.eviahealth.eviahealth.utils.util;

import java.util.Arrays;

public class ft95manager {

    private final static String TAG = "ft95manager";

    // Posicion      0    1    2    3    4    5    6    7    8    9    0    1    2
    // Receiving: [0x07 0x10 0x03 0x00 0xFF 0xE1 0x07 0x04 0x0D 0x0E 0x07 0x00 0xFF]
    //Meaning: 0x07 – The flags set for the FT95 | 0xFF 0x00 0x03 0x10 – Temperature value of 78.4 | 0x07 0xE1 – Year
    //2017 | 0x04 – Month April | 0x0D – Day 13 | 0x0E – Hour 14 | 0x07 – Minute 07 | 0x00 Second 0 | 0xFF Object Measurement
    public static String msgTemperature(byte[] values){
        String message = "{ ";
//        Log.e(TAG,"OBJ: " + util.getContentData(values));

        byte flags = values[0];
        byte[] tmp = getSubArray(values, 1, 5);
//        Log.e(TAG,"tmp: " + util.getContentData(tmp));
        byte[] timestamp = getSubArray(values, 5, 12);
        byte btype = values[12];

        byte[] atemperature = rotateArray(tmp);
//        Log.e(TAG,"atemperature: " + util.getContentData(atemperature));
        float temperature = getTemperature(atemperature);

//        Log.e(TAG,"timestamp: " + util.getContentData(timestamp));
        Integer year = util.BytesToInt(new byte[]{timestamp[1],timestamp[0]});
        Integer month = util.toUnsignedInt(timestamp[2]);
        Integer day = util.toUnsignedInt(timestamp[3]);
        Integer hours = util.toUnsignedInt(timestamp[4]);
        Integer minutes = util.toUnsignedInt(timestamp[5]);
        Integer seconds = util.toUnsignedInt(timestamp[6]);

        String type = (btype == 0x02) ? "frente": "objeto";
        String medida = (util.verificarBit(util.toUnsignedInt(flags),0)) ? "Fahrenheit" : "Celsius";

        // yyyy-MM-dd HH:mm:ss
        String time = "" + year.toString() + "-" + util.dosDigitos(month) + "-" + util.dosDigitos(day);
        time += " " + util.dosDigitos(hours) + ":" + util.dosDigitos(minutes) + ":" + util.dosDigitos(seconds);

        message += "\"temperature\":" + temperature + ",";
        message += "\"type\":\"" + type + "\",";
        message += "\"unidad\":\"" + medida + "\",";
        message += "\"time\":\"" + time + "\"";
        return message + " }";
    }

    public static float getTemperature(byte[] bytes) {
        int result = 0;
        for (int i = 1; i < 4; i++) {
            result <<= 8;
            result |= (bytes[i] & 0xFF);
        }
        return (float)(result/10.0);
    }

    /**
     *
     * @param original: array de origen
     * @param start: posición de inicio del array de origen
     * @param end: posición final del array de origen + 1
     * @return
     * <p>Devuelbe un byte[] que contine los valores entre la posición start a la
     * la posición end del array original. Posición strat incluida</p>
     */
    public static byte[] getSubArray(byte[] original, int start, int end) {
        return Arrays.copyOfRange(original, start, end);
    }

    public static byte[] rotateArray(byte[] original) {
        if (original == null) {
            return original;
        }
        byte[] rotado = new byte[original.length];

        int p = 0;
        for (int i = original.length;i!=0;i--)  {
            rotado[p] = original[i-1];
            p += 1;
        }
        return rotado;
    }
}
