package com.eviahealth.eviahealth.models.vitalograph;

import android.util.Log;

import com.eviahealth.eviahealth.utils.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class SigleTestData {

    private byte[] data;
    private String deviceID;
    private int PEF;
    private double PEV075;
    private double FEV1;
    private double FEV6;
    private double FEV1FEV6;
    private double FEF;
    private int FEV1_PersonalBest;
    private int PEF_PersonalBest;
    private int FEV1x100;
    private int PEFx100;
    private int greenZone;
    private int yellowZone;
    private int orangeZone;
    private String fecha;
    private String hora;
    private boolean goodTest = false;
    private String swNumber;
    private Integer _id_test;
    private String color_semaforo = "none";

    public SigleTestData(byte[] data, String extras) {

        try {
            JSONObject lung = new JSONObject(extras);
            Log.e("SigleTestData", "lung: " + lung);
            if (lung.has("FEV1PB")) {
                FEV1_PersonalBest = lung.getInt("FEV1PB");
            } else {
                FEV1_PersonalBest = 300;
            }
            if (lung.has("GreenZone")) {
                greenZone = lung.getInt("GreenZone");
            } else {
                greenZone = 80;
            }
            if (lung.has("YellowZone")) {
                yellowZone = lung.getInt("YellowZone");
            } else {
                yellowZone = 50;
            }
            if (lung.has("OrangeZone")) {
                orangeZone = lung.getInt("OrangeZone");
            } else {
                orangeZone = 30;
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        this.data = new byte[data.length-3];
        for (int i = 1; i < data.length-2; i++) {
            this.data[i-1] = data[i];
        }
        Log.e("SigleTestData","data: " + Arrays.toString(this.data));
        Log.e("SigleTestData","data.length: " + this.data.length);
        parseData();

        this._id_test = 0;
    }

    @Override
    public String toString() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("deviceID", deviceID);
            obj.put("PEF", PEF);
            obj.put("PEV075", PEV075);
            obj.put("FEV1", FEV1);
            obj.put("FEV6", FEV6);
            obj.put("FEV1FEV6", FEV1FEV6);
            obj.put("FEF", FEF);
            obj.put("FEV1_PersonalBest", FEV1_PersonalBest);
            obj.put("PEF_PersonalBest", PEF_PersonalBest);
            obj.put("FEV1x100", FEV1x100);
            obj.put("PEFx100", PEFx100);
            obj.put("greenZone", greenZone);
            obj.put("yellowZone", yellowZone);
            obj.put("orangeZone", orangeZone);
            obj.put("fecha", fecha);
            obj.put("hora", hora);
            obj.put("goodTest", goodTest);
            obj.put("swNumber", swNumber);
            return obj.toString();
        }
        catch (JSONException e){
            Log.e("TAG", "Exception toString: " + e.getMessage());
            return "{}";
        }
    }

    private void parseData(){
        //region :: Parse
        try {
            deviceID = "";
            for (int i = 0; i < 10; i++) {
                deviceID += (char)data[i+3];
            }
            Log.e("TAG", "deviceID: " + deviceID);

            PEF = bytesToInt(util.splitBytes(data,13,16));
            PEV075 = bytesToInt(util.splitBytes(data,16,19)) / 100.0;
            FEV1 = bytesToInt(util.splitBytes(data,19,22)) / 100.0;
            FEV6 = bytesToInt(util.splitBytes(data,22,25)) / 100.0;
            FEV1FEV6 = bytesToInt(util.splitBytes(data,25,28)) / 100.0;
            FEF = bytesToInt(util.splitBytes(data,28,31)) / 100.0;

            // Estos datos se obtienen de la base de datos, no del dispositivo
//            FEV1_PersonalBest = bytesToInt(util.splitBytes(data,31,34)) / 100.0;
//            PEF_PersonalBest = bytesToInt(util.splitBytes(data,34,37));
//            calculatePersonalBest();

//            FEV1x100 = bytesToInt(util.splitBytes(data,37,40));
//            PEFx100 = bytesToInt(util.splitBytes(data,40,43));
//            FEV1x100 = (int)(FEV1_PersonalBest / FEV1);  // double
//            FEV1x100 *= 100;
//            PEFx100 = (PEF_PersonalBest / PEF) * 100;            // int

//            greenZone = bytesToInt(util.splitBytes(data,43,46));
//            yellowZone = bytesToInt(util.splitBytes(data,46,49));
//            orangeZone = bytesToInt(util.splitBytes(data,49,52));

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat format2 = new SimpleDateFormat("HH:mm:SS");

            Date ahora = new Date();
            fecha = format.format(ahora);
            hora = format2.format(ahora);

            // = '0' correcta
            goodTest = (util.toUnsignedInt(data[64]) == 48) ? true : false;

            swNumber = "";
            for (int i = 0; i < 3; i++) {
                swNumber += (char) data[65 + i];
            }
            Log.e("TAG", "swNumber: " + swNumber);
        }
        catch (Exception e){
            Log.e("TAG", "Exception parseData: " + e.getMessage());
        }
        //endregion
    }

    public String toStringLung() {
        try {
            JSONObject lung = new JSONObject();
            lung.put("FEV1PB", FEV1_PersonalBest);
            lung.put("GreenZone", greenZone);
            lung.put("YellowZone", yellowZone);
            lung.put("OrangeZone", orangeZone);
            return lung.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return "{}";
        }
    }

    //"test": { "PEF": 382, "FEV1":  3.28, "FEV6":  3.91, "FEV1_6":  0.84, "date": "YYYY-MM-DD HH:MM:SS", "intentos": 2, "semaforo": "green"}
    public String toStringTest() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("PEF", PEF);
            obj.put("FEV1", FEV1);
            obj.put("FEV6", FEV6);
            obj.put("FEV1FEV6", FEV1FEV6);
            obj.put("date", fecha + " " + hora);

            obj.put("intentos", this._id_test);
            obj.put("semaforo", this.color_semaforo);

            return obj.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return "{}";
        }
    }

    //region :: Getters and Setters
    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        parseData();
    }

    public int getPEF() {
        return PEF;
    }

    public double getFEV1() {
        return FEV1;
    }

    public double getFEV6() {
        return FEV6;
    }

    public int getGreenZone() {
        return greenZone;
    }

    public int getYellowZone() {
        return yellowZone;
    }

    public int getOrangeZone() {
        return orangeZone;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public boolean isGoodTest() {
        return goodTest;
    }

    public double getFEV1_PersonalBest() {
        return (FEV1_PersonalBest / 100.0);
    }

    public double getFEV1FEV6() {
        return FEV1FEV6;
    }

    public void setIdTest(Integer _id_test) {
        this._id_test = _id_test;
    }

    public void setColorSemaforo(String color_semaforo) {
        this.color_semaforo = color_semaforo;
    }
    // endregion

    private int charToInt(byte c){
        if(c >= 48 && c <= 57){
            return c - 48;
        }
        return 0;
    }

    private int bytesToInt(byte[] b){
        return (charToInt(b[0])*100) + (charToInt(b[1])*10) + charToInt(b[2]);
    }
}
