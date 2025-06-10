package com.eviahealth.eviahealth.bluetooth.beurer.po60;

import com.eviahealth.eviahealth.utils.util;

public class po60manager {
    private final static String TAG = "po60manager";

    //region .. Funtions

    // response type: { "HardwareVersion":"00.02.01","SoftwareVersion":"00.03.01" }
    public static String msgDeviceVersion(byte[] values){
        String message = "{ ";

        int A = util.toUnsignedInt(values[1]) & 0x7f;
        int B = util.toUnsignedInt(values[2]) & 0x7f;
        int C = util.toUnsignedInt(values[3]) & 0x7f;
        String HardwareVersion = "" + util.dosDigitos(A) + "." + util.dosDigitos(B) + "." + util.dosDigitos(C);

        A = util.toUnsignedInt(values[4]) & 0x7f;
        B = util.toUnsignedInt(values[5]) & 0x7f;
        C = util.toUnsignedInt(values[6]) & 0x7f;
        String SoftwareVersion = "" + util.dosDigitos(A) + "." + util.dosDigitos(B) + "." + util.dosDigitos(C);

        message += "\"HardwareVersion\":\"" + HardwareVersion + "\",";
        message += "\"SoftwareVersion\":\"" + SoftwareVersion + "\"";
        return message + " }";
    }

    // response type: { "date":"2016-10-08 16:04:09","year":2016,"month":10,"day":8,"hour":16,"minute":4,"second":9 }
    public static String msgGetTime(byte[] values){
        String message = "{ ";

        int year = util.toUnsignedInt(values[1]) + 2000;
        int month = util.toUnsignedInt(values[2]);
        int day = util.toUnsignedInt(values[3]);
        int hour = util.toUnsignedInt(values[4]);
        int minute = util.toUnsignedInt(values[5]);
        int second = util.toUnsignedInt(values[6]);

        String sYear = "" + String.format("%04d", year);
        String sMonth = "" + String.format("%02d", month);
        String sDay = "" + String.format("%02d", day);
        String sHour = "" + String.format("%02d", hour);
        String sMinute = "" + String.format("%02d", minute);
        String sSecond = "" + String.format("%02d", second);

        String tdate = "" + sYear + "-" + sMonth + "-" + sDay + " " + sHour+ ":" + sMinute+ ":" + sSecond;

        message += "\"date\":\"" + tdate + "\",";
        message += "\"year\":" + year + ",";
        message += "\"month\":" + month + ",";
        message += "\"day\":" + day  + ",";
        message += "\"hour\":" + hour + ",";
        message += "\"minute\":" + minute + ",";
        message += "\"second\":" + second + "";
        return message + " }";
    }

    //response type: { "type":"SpO2 and PR","records":10 }
    public static String msgGetRecords(byte[] values){
        String message = "{ ";

        Integer records = util.BytesToInt(new byte[]{values[3], values[2]});
        String type = util.toUnsignedInt(values[1]) == 5 ? "SpO2 and PR" : "unknown";

        message += "\"type\":\"" + type + "\",";
        message += "\"records\":" + records + "";

        return message + " }";
    }

    // response type: { "index":0,"startingDate":"2019-01-01 00:00:19","endDate":"2019-01-01 00:00:22","storageTimePeriod":1024,"SpO2Max":98,"SpO2Min":98,"SpO2Avg":98,"PRMax":59,"PRMin":58,"PRAvg":58 }
    public static String msgGetRecordData(byte[] values){
        String message = "{ ";

        Integer index = util.toUnsignedInt(values[1]);
        Boolean ultimo = (index & 0x20) == 0x20 ? true : false;
        index = index & 0x0f;

        //region :: starting date
        int i = 2;
        int year = util.toUnsignedInt(values[i]) + 2000; i++;
        int month = util.toUnsignedInt(values[i]); i++;
        int day = util.toUnsignedInt(values[i]); i++;
        int hour = util.toUnsignedInt(values[i]); i++;
        int minute = util.toUnsignedInt(values[i]);  i++;
        int second = util.toUnsignedInt(values[i]); i++;

        String sYear = "" + String.format("%04d", year);
        String sMonth = "" + String.format("%02d", month);
        String sDay = "" + String.format("%02d", day);
        String sHour = "" + String.format("%02d", hour);
        String sMinute = "" + String.format("%02d", minute);
        String sSecond = "" + String.format("%02d", second);

        String startingDate = "" + sYear + "-" + sMonth + "-" + sDay + " " + sHour+ ":" + sMinute+ ":" + sSecond;
        //endregion

        //region :: end date
        i = 8;
        year = util.toUnsignedInt(values[i]) + 2000; i++;
        month = util.toUnsignedInt(values[i]); i++;
        day = util.toUnsignedInt(values[i]); i++;
        hour = util.toUnsignedInt(values[i]); i++;
        minute = util.toUnsignedInt(values[i]);  i++;
        second = util.toUnsignedInt(values[i]); i++;

        sYear = "" + String.format("%04d", year);
        sMonth = "" + String.format("%02d", month);
        sDay = "" + String.format("%02d", day);
        sHour = "" + String.format("%02d", hour);
        sMinute = "" + String.format("%02d", minute);
        sSecond = "" + String.format("%02d", second);

        String endDate = "" + sYear + "-" + sMonth + "-" + sDay + " " + sHour+ ":" + sMinute+ ":" + sSecond;
        //endregion

        Integer tmp = util.BytesToInt(new byte[]{values[16], values[15]});
        Integer storageTimePeriod = tmp & 0x3fff;

        int SpO2Max = util.toUnsignedInt(values[17]) & 0x7f;
        int SpO2Min = util.toUnsignedInt(values[18]) & 0x7f;
        int SpO2Avg = util.toUnsignedInt(values[19]) & 0x7f;

        Integer MSBPR = util.toUnsignedInt(values[14]); // Se encuentra el bit7 de los PR

        int PRMax = util.toUnsignedInt(values[20]) & 0x7f;
        int PRMin = util.toUnsignedInt(values[21]) & 0x7f;
        int PRAvg = util.toUnsignedInt(values[22]) & 0x7f;

        if (util.verificarBit(MSBPR,5)) { PRMax += 128; }
        if (util.verificarBit(MSBPR,4)) { PRMin += 128; }
        if (util.verificarBit(MSBPR,3)) { PRAvg += 128; }

        message += "\"index\":" + index + ",";
        message += "\"startingDate\":\"" + startingDate + "\",";
        message += "\"endDate\":\"" + endDate + "\",";
        message += "\"storageTimePeriod\":" + storageTimePeriod + ",";

        message += "\"SpO2Max\":" + SpO2Max + ",";
        message += "\"SpO2Min\":" + SpO2Min  + ",";
        message += "\"SpO2Avg\":" + SpO2Avg + ",";

        message += "\"PRMax\":" + PRMax + ",";
        message += "\"PRMin\":" + PRMin  + ",";
        message += "\"PRAvg\":" + PRAvg + "";

        return message + " }";
    }

    //endregion
}
