package com.eviahealth.eviahealth.bluetooth.beurer.bf600;

import android.util.Log;

import com.eviahealth.eviahealth.models.beurer.bf600.ScaleLib;
import com.eviahealth.eviahealth.utils.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

public class bf600ParseData {

    public static String ParseBatteryLevel(byte[] data){

        try {
            JSONObject params = new JSONObject();
            if (data.length == 1) {
                params.put("battery", util.toUnsignedInt(data[0]));
                params.put("status", 1);
            }
            else { params.put("status", 4); }
            return params.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static String ResponseUserControlPoint(byte[] data){

        try {
            JSONObject params = new JSONObject();

            params.put("code", data[1]);

            if (data.length > 3) {
                // esta valor solo existe si status = 1
                params.put("user_index", data[3]);
            }

            params.put("status", data[2]);
            params.put("description", StatusBF600.getTextByValue(data[2]));

            return params.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String ParseCurrentTime(byte[] data) {

        try {

            Log.e("bf600", "ParseCurrentTime: " + data.toString());

            int year = util.BytesToInt(new byte[]{data[1], data[0]});
            int month = util.toUnsignedInt(data[2]);
            int day = util.toUnsignedInt(data[3]);
            int hour = util.toUnsignedInt(data[4]);
            int minute = util.toUnsignedInt(data[5]);
            int second = util.toUnsignedInt(data[6]);

            JSONObject message = new JSONObject();
            message.put("date", year + "-" + util.dosDigitos(month) + "-" + util.dosDigitos(day));
            message.put("time", util.dosDigitos(hour) + ":" + util.dosDigitos(minute) + ":" + util.dosDigitos(second));
            return message.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static int calculateAge(byte[] birthDateBytes) {
        try {
            int year = (birthDateBytes[1] << 8) | (birthDateBytes[0] & 0xFF);
            int month = birthDateBytes[2] & 0xFF;
            int day = birthDateBytes[3] & 0xFF;

            LocalDate birthDate = LocalDate.of(year, month, day);
            LocalDate currentDate = LocalDate.now();
            Period age = Period.between(birthDate, currentDate);

//        Log.e("bf600", "Age: " + age.getYears());
            return age.getYears();
        }
        catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static String ParseGender(byte[] data) {
        try {
            if (data[0] == 0x00) {
                return "Hombre";
            }
            else if (data[0] == 0x01) {
                return "Mujer";
            }
            else if (data[0] == 0x02) {
                return "Unspacified";
            }
            return "no_selected";
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int ParseHeight(byte[] data) {
        try {
            return util.toUnsignedInt(data[0]);
        }
        catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    public static int ParseActivituLevel(byte[] data) {
        try {
            return util.toUnsignedInt(data[0]);
        }
        catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    public static String ParseUserInfo(int age, String gender, int height) {
        try {
            JSONObject message = new JSONObject();
            message.put("age", age);
            message.put("gender", gender);
            message.put("height", height);
            return message.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String ParseWeightMeasurement(byte[] data) {

        try {
//            Log.e("bf600", "ParseWeightMeasurement: " + data.toString());

            byte flags = data[0];  // debe ser 0x0e
            float weight = util.BytesToInt(new byte[]{data[2], data[1]}) * 0.005f; // {alta, baja}

            int year = util.BytesToInt(new byte[]{data[4], data[3]});
            int month = util.toUnsignedInt(data[5]);
            int day = util.toUnsignedInt(data[6]);
            int hour = util.toUnsignedInt(data[7]);
            int minute = util.toUnsignedInt(data[8]);
            int second = util.toUnsignedInt(data[9]);

            int user_id = util.toUnsignedInt(data[10]);
            float imc = util.BytesToInt(new byte[]{data[12], data[11]}) * 0.1f;
            int height = util.BytesToInt(new byte[]{data[14], data[13]}) / 10;

            JSONObject message = new JSONObject();
            message.put("date", year + "-" + util.dosDigitos(month) + "-" + util.dosDigitos(day));
            message.put("time", util.dosDigitos(hour) + ":" + util.dosDigitos(minute) + ":" + util.dosDigitos(second));

            message.put("weight", roundDouble(weight));
            message.put("user_id", user_id);
            message.put("imc", roundDouble(imc));
            message.put("height", height);

            return message.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String ParseBodyComposition(byte[] data, float weightValue) {

        try {
            int flags = util.BytesToInt(new byte[]{data[0], data[1]});  // debe ser 0x98 03
            float body_fat = util.BytesToInt(new byte[]{data[3], data[2]}) * 0.1f;

            int bmr = util.BytesToInt(new byte[]{data[5], data[4]});
            int bmrInKcal = Math.round(((bmr / 4.1868f) * 10.0f) / 10.0f);

            float muscle_percentage = util.BytesToInt(new byte[]{data[7], data[6]}) * 0.1f;
            float soft_lean_mass = util.BytesToInt(new byte[]{data[9], data[8]}) * 0.005f;     // masa magra
            float body_water_mass = util.BytesToInt(new byte[]{data[11], data[10]}) * 0.005f;
            int impedance = util.BytesToInt(new byte[]{data[13], data[12]});

            // calculate lean body mass and bone mass
            float fatMass = weightValue * body_fat / 100.0f;
            float leanBodyMass = weightValue - fatMass;
            float boneMass = leanBodyMass - soft_lean_mass;
            float water = Math.round(((body_water_mass / weightValue) * 10000.f))/100.f;

            JSONObject message = new JSONObject();
            message.put("body_fat", roundDouble(body_fat));
            message.put("bmr", bmrInKcal);
            message.put("muscle_percentage", roundDouble(muscle_percentage));
            message.put("impedance", impedance);
            message.put("water", roundDouble(water));
            message.put("boneMass", roundDouble(boneMass));

            return message.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String ParseTakeMeasurement(byte[] data) {

        try {

            Log.e("bf600", "ParseTakeMeasurement: " + data.toString());

            int status = util.toUnsignedInt(data[0]);
            String description = "";

            if (status == 1) { description = "Éxito"; }
            else if (status == 2) { description = "Tiempo de espera de toma de medición"; }
            else if (status == 3) { description = "Error en la medición"; }
            else if (status == 4) { description = "No se ha seleccionado ningún usuario"; }
            else { description = "Error desconocido"; }

            JSONObject message = new JSONObject();
            message.put("status", status);
            message.put("description", description);
            return message.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String ParseUserList(byte[] data) {

        try {

            JSONObject params = new JSONObject();
            params.put("code_status", data[data.length - 1]);
            params.put("users_mumber", data.length / 12);

            JSONArray users = new JSONArray();
            for (int i = 0; i < data.length-1; i += 12) {

                int op_code = data[i];
                int user_index = data[i + 1];
                int initials = util.BytesToInt(new byte[]{data[i + 2], data[i + 3], data[i + 4]});
                int year_of_birth = util.BytesToInt(new byte[]{data[i + 6], data[i + 5]});
                int month_of_birth = util.toUnsignedInt(data[i + 7]);
                int day_of_birth = util.toUnsignedInt(data[i + 8]);
                int height = util.toUnsignedInt(data[i + 9]);
                int gender = util.toUnsignedInt(data[i + 10]);
                int activity_level = util.toUnsignedInt(data[i + 11]);

                JSONObject message = new JSONObject();
                message.put("op_code", op_code);
                message.put("user_index", user_index);
                message.put("initials", initials);
                message.put("year_of_birth", year_of_birth);
                message.put("month_of_birth", month_of_birth);
                message.put("day_of_birth", day_of_birth);
                message.put("height", height);
                message.put("gender", gender);
                message.put("activity_level", activity_level);

                users.put(message);
            }

            params.put("users", users);
            return params.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String DataMeasureScale(String gender, int age, float height, float weight, float impedance) {

        int sex = (gender.equals("Hombre")) ? 1 : 0;
        ScaleLib scale = new ScaleLib(sex, age, height / 100.0f);

        try {
            JSONObject params = new JSONObject();
            params.put("LBM", scale.getLBM(weight,impedance));
            params.put("getMuscle", scale.getMuscle(weight,impedance));
            params.put("getWater", scale.getWater(weight,impedance));
            params.put("getBoneMass", scale.getBoneMass(weight,impedance));
            params.put("getVisceralFat", scale.getVisceralFat(weight));
            params.put("getBodyFat", scale.getBodyFat(weight,impedance));
            return params.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
        }



        return "";
    }


    // BMR cálculo teórico
    public static float getBMR(int gender,float weight, float height, int age) {
        float bmr;

        // BMR Harris-Benedict equation
        if (gender == 0) {
            // Male
            bmr = 66.4730f + (13.7516f * weight) + (5.0033f * height) - (6.7550f * age);
        } else {
            bmr = 655.0955f + (9.5634f * weight) + (1.8496f * height) - (4.6756f * age);
        }

        return bmr; // kCal / day
    }

    //region :: util convert
    public static float bytesToFloat(byte b0, byte b1) {
        int mantissa = unsignedToSigned(unsignedByteToInt(b0) + ((unsignedByteToInt(b1) & 15) << 8), 12);
        int exponent = unsignedToSigned(unsignedByteToInt(b1) >> 4, 4);
        return (float)((double)mantissa * Math.pow(10.0, (double)exponent));
    }

    public static float bytesToFloat(byte b0, byte b1, byte b2, byte b3) {
        int mantissa = unsignedToSigned(unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8) + (unsignedByteToInt(b2) << 16), 24);
        return (float)((double)mantissa * Math.pow(10.0, (double)b3));
    }

    public static int unsignedToSigned(int unsigned, int size) {
        return (unsigned & 1 << size - 1) != 0 ? -1 * ((1 << size - 1) - (unsigned & (1 << size - 1) - 1)) : unsigned;
    }

    public static int intToSignedBits(int i, int size) {
        return i < 0 ? (1 << size - 1) + (i & (1 << size - 1) - 1) : i;
    }

    public static int unsignedByteToInt(byte b) {
        return b & 255;
    }
    //endregion

    public static Double roundDouble(double number) {
        BigDecimal bd = new BigDecimal(Double.toString(number));
        bd = bd.setScale(1, RoundingMode.HALF_UP);

//        Double bdValue = bd.doubleValue();
        return bd.doubleValue();
    }
}
