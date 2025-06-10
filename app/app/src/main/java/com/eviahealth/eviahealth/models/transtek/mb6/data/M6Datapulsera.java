package com.eviahealth.eviahealth.models.transtek.mb6.data;

import android.util.Log;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class M6Datapulsera {

    private String TAG = "M6Datapulsera";

    private Boolean _status_descarga;
    private String batteryData;
    private String StepOfDayData;
    private String StepOfHourData;
    private String StepOfHourDataComplet;
    private String sleepData;
    private String sleepDataComplet;

    private String heartRateData;
    private String bloodOxygenData ;

    private Integer battery;
    private Integer totalStepsDay;
    private Integer totalCalories;

    private Boolean _timeout_act;
    private Integer _error_num;
    // ----------------------------------------------

    // SINGLETON
    private static M6Datapulsera instance = null;
    public static M6Datapulsera getInstance() {
        if(instance == null) {
            instance = new M6Datapulsera(false,"{}","{}","{}","{}",false, "{}", "{}", "{}","{}");
        }
        return instance;
    }

    // Constructor
    private M6Datapulsera(Boolean status_descarga,String bateria, String datos_actuales, String datos_actividad, String datos_sleep, boolean timeout_act, String heartRate, String boodOxygen, String data_actividad_complet, String data_sleep_complet){

        this.batteryData = bateria;
        this.StepOfDayData = datos_actuales;
        this.StepOfHourData = datos_actividad;
        this.StepOfHourDataComplet = data_actividad_complet;
        this.sleepDataComplet = data_sleep_complet;
        this.sleepData = datos_sleep;

        this.heartRateData = heartRate;
        this.bloodOxygenData = boodOxygen;

        this._status_descarga = status_descarga;
        this._timeout_act = timeout_act;

        this.battery = 0;
        this._error_num= -1;
    }

    public void clear() {

        this.batteryData = "{}";
        this.StepOfDayData = "{}";
        this.StepOfHourData = "{}";
        this.StepOfHourDataComplet = "{}";
        this.sleepData = "{}";
        this.sleepDataComplet = "{}";

        this.battery = 0;
        this._timeout_act = false;
        this._status_descarga = false;
        this._error_num= -1;
    }

    public Boolean getDownloadState() {
        return _status_descarga;
    }

    public void setDownloadState(Boolean _status_descarga) { this._status_descarga = _status_descarga; }


    public String getBatteryData() {
        return batteryData;
    }

    public void setBatteryData(String _bateria) {
        this.batteryData = _bateria;
    }

    public Integer getBattery() {
        try {
            this.battery = util.getIntValueJSON(this.batteryData,"battery");
            if (this.battery == null) this.battery = -1;
        }catch (Exception e){
            Log.e("DATA_ACT","EXCEPTION getBattery()");
            this.battery = -1;
        }
        return this.battery;
    }

    public String getStepOfDayData() {
        return StepOfDayData;
    }

    public void setStepOfDayData(String stepOfDayData) {

        this.StepOfDayData = stepOfDayData;

        try {
            this.totalStepsDay  = util.getIntValueJSON(stepOfDayData,"steps");
            this.totalCalories = util.getIntValueJSON(stepOfDayData,"calories");

            if (this.totalStepsDay == null) this.totalStepsDay = 0;
            if (this.totalCalories == null) this.totalCalories = 0;

        }catch (Exception e){
            Log.e("DATA_ACT","EXCEPTION set_datos_actuales()");
            this.totalStepsDay = 0;
            this.totalCalories = 0;
        }
    }

    public String getStepOfHourData() {
        return StepOfHourData;
    }

    public void setStepOfHourData(String data) { this.StepOfHourData = data; }

    public void setStepOfHourDataComplet(String data) { this.StepOfHourDataComplet = data; }

    public String getStepOfHourDataComplet() { return this.StepOfHourDataComplet; }

    public String getSleepData() {
        return sleepData;
    }

    public void setSleepData(String sleepData) { this.sleepData = sleepData; }

    public void setSleepDataComplet(String data) { this.sleepDataComplet = data; }

    public String getSleepDataComplet() { return this.sleepDataComplet; }

    public boolean getTimeOut() {
        return _timeout_act;
    }

    public void setTimeOut(boolean _timeout_act) {
        this._timeout_act = _timeout_act;
    }

    public Integer getTotalStepsDay() {
        return this.totalStepsDay;
    }

    public int getCaloriesDay() {
        return this.totalCalories;
    }

    public String getHeartRateData() {
        return heartRateData;
    }

    public void setHeartRateData(String heartRateData) {
        this.heartRateData = heartRateData;
    }

    public String getBloodOxygenData() {
        return bloodOxygenData;
    }

    public void setBloodOxygenData(String bloodOxygenData) {
        this.bloodOxygenData = bloodOxygenData;
    }

    public void setFilesActivity(){
        try{
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String fecha = simpleDateFormat.format(new Date());

            // ACTIVIDAD TOTAL
            if (!this.StepOfDayData.equals("{}")){

                JSONObject json_act = new JSONObject();
                json_act.put("idpaciente", Config.getInstance().getIdPacienteEnsayo());
                json_act.put("fecha", fecha);
                json_act.put("step", this.totalStepsDay);
                json_act.put("calorie", this.totalCalories);
                json_act.put("totalcalories", this.totalCalories);

                FileAccess.escribirJSON(FilePath.REGISTROS_ACTIVIDAD_TOTAL_MAMBO6, json_act);
                EVLog.log(TAG,"GENERADO FICHERO >> " + FilePath.REGISTROS_ACTIVIDAD_TOTAL_MAMBO6);
                EVLog.log(TAG,"DATA >> " + json_act.toString());
            }

            // ACTIVIDAD DIARIA
            if (!this.StepOfHourData.equals("{}")){
                FileAccess.escribir(FilePath.REGISTROS_ACTIVIDAD_DIARIA_MAMBO6, getStepOfHourData());
                EVLog.log(TAG,"GENERADO FICHERO >> " + FilePath.REGISTROS_ACTIVIDAD_DIARIA_MAMBO6);
                EVLog.log(TAG,"DATA >> " + getStepOfHourData());

                // para envio en bloque
                FileAccess.escribir(FilePath.REGISTROS_ACTIVIDAD_DATA_STEPS_MAMBO6, getStepOfHourDataComplet());
                EVLog.log(TAG,"GENERADO FICHERO >> " + FilePath.REGISTROS_ACTIVIDAD_DATA_STEPS_MAMBO6);
                EVLog.log(TAG,"DATA >> " + getStepOfHourDataComplet());
            }

            // ACTIVIDAD SUEÑO
            if (!this.sleepData.equals("{}")) {
                FileAccess.escribir(FilePath.REGISTROS_ACTIVIDAD_SLEEP_MAMBO6, getSleepData());
                EVLog.log(TAG,"GENERADO FICHERO >> " + FilePath.REGISTROS_ACTIVIDAD_SLEEP_MAMBO6);
                EVLog.log(TAG,"DATA >> " +getSleepData());

                // para envio en bloque
                FileAccess.escribir(FilePath.REGISTROS_ACTIVIDAD_DATA_SLEEP_MAMBO6, getSleepDataComplet());
                EVLog.log(TAG,"GENERADO FICHERO >> " + FilePath.REGISTROS_ACTIVIDAD_DATA_SLEEP_MAMBO6);
                EVLog.log(TAG,"DATA >> " + getSleepDataComplet());
            }

            // ACTIVIDAD HEART RATE (FRECUENCIA CARDIACA)
            if (!this.heartRateData.equals("{}")) {
                FileAccess.escribir(FilePath.REGISTROS_ACTIVIDAD_HEART_RATE_MAMBO6, getHeartRateData());
                EVLog.log(TAG,"GENERADO FICHERO >> " + FilePath.REGISTROS_ACTIVIDAD_HEART_RATE_MAMBO6);
                EVLog.log(TAG,"DATA >> " + getHeartRateData());
            }

            // ACTIVIDAD BLOOD OXYDEN (OXIGENO EN SANGRE)
            if (!this.bloodOxygenData.equals("{}")) {
                FileAccess.escribir(FilePath.REGISTROS_ACTIVIDAD_BLODD_OXYGEN_MAMBO6, getBloodOxygenData());
                EVLog.log(TAG,"GENERADO FICHERO >> " + FilePath.REGISTROS_ACTIVIDAD_BLODD_OXYGEN_MAMBO6);
                EVLog.log(TAG,"DATA >> " + getBloodOxygenData());
            }

        } catch (JSONException | IOException e) {
            Log.d(TAG, "EXCEPTION setFilesActivity() >> " + e.toString());
        }
    }

    public void setERROR_ACT(String msg) {
        try {
            this._error_num= util.getIntValueJSON(msg,"error");
        }catch (Exception e){
            Log.e("DATA_TEN","EXCEPTION getERROR_ACT()");
            this._error_num= -1;
        }
    }

    public Integer get_error_num() {
        return _error_num;
    }
}
