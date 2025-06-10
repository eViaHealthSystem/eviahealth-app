package com.eviahealth.eviahealth.models.vitalograph;

import android.util.Log;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class LungData {
    private String TAG = "LungData";
    private Boolean _status_descarga;
    private Integer _error_num;
    private Integer _id_test;
    private String color_semaforo = "none";
    private SigleTestData singleTestData = null;
    private Patient patient = null;

    private static LungData instance = null;
    public static LungData getInstance() {
        if(instance == null) {
            instance = new LungData(null, null,false);
        }
        return instance;
    }

    // Constructor
    private LungData(Patient patient,SigleTestData singleTestData, Boolean status_descarga){
        this.patient = patient;
        this.singleTestData = singleTestData;
        this._status_descarga = status_descarga;
        this._error_num= -1;
        this._id_test = 0;
        this.color_semaforo = "none";
    }

    public void clear() {
        this.singleTestData = null;
        this._status_descarga = false;
        this._error_num= -1;
        this._id_test = 0;
    }

    public void setSingleTestData(SigleTestData singleTestData) {
        this.singleTestData = singleTestData;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Patient getPatient() {
        return patient;
    }

    @Override
    public String toString() {
        return this.singleTestData.toString();
    }

    public Boolean getStatusDescarga() {
        return _status_descarga;
    }

    public void setStatusDescarga(Boolean _status_descarga) {
        this._status_descarga = _status_descarga;
    }

    public SigleTestData getSingleTestData() {
        return singleTestData;
    }

    public void setFilesActivity(){

/*
DATOS ENVIADOS DE MONITOR PULMONAR

{
    "typeMeassure": "lung_monitor",
    "meassures": [{"fecha":"<la fecha>", "params":"<el json>"},{"fecha":"<la fecha>", "params":"<el json>"},...]
}
params:
{
    "test": { "PEF": 382, "FEV1":  3.28, "FEV6":  3.91, "FEV1_6":  0.84, "date": "YYYY-MM-DD HH:MM:SS", "intentos": 2, "semaforo": "green"}
    "paciente": { "sex": "Hombre", "age": 65, "weight": 70, "height": 165  },
    "lung": { "FEV1PB": 350, "GreenZone": 50, "YellowZone": 35, "OrangeZone": 35 }
}
*/
        if (singleTestData == null) {
            return;
        }

        try{
            JSONObject obj = new JSONObject();
            obj.put("idpaciente", Config.getInstance().getIdPacienteEnsayo());
//            obj.put("identificadorpaciente", Config.getInstance().getIdPacienteTablet());
            obj.put("identificadorpaciente", Config.getInstance().getIdPacienteEnsayo());
            obj.put("typeMeassure", "lung_monitor");

            // Array meassures
            JSONArray meassures = new JSONArray();

            JSONObject registro = new JSONObject();
            registro.put("fecha", util.getDateNow());

            //region :: measure test
            JSONObject params = new JSONObject();
            JSONObject test = new JSONObject(singleTestData.toStringTest());
            JSONObject paciente = new JSONObject(this.patient.toString());
            JSONObject lung = new JSONObject(singleTestData.toStringLung());

            params.put("test", test);
//            params.put("paciente", paciente);
            params.put("lung", lung);
            //endregion
            registro.put("params", params);

            // Añadimos el registro a meassures
            meassures.put(registro);

            // -------------
            obj.put("meassures", meassures);

            FileAccess.escribirJSON(FilePath.REGISTROS_MONITOR_PULMONAR, obj);

            EVLog.log(TAG,"GENERADO FICHERO >> " + FilePath.REGISTROS_MONITOR_PULMONAR);
            EVLog.log(TAG,"DATA >> " + obj.toString());

        } catch (JSONException | IOException e) {
            Log.d(TAG, "EXCEPTION setFilesActivity() >> " + e.toString());
        }
    }

    public void setERROR(String msg) {
        try {
            this._error_num= util.getIntValueJSON(msg,"error");
        }catch (Exception e){
            Log.e("DATA_TEN","EXCEPTION getERROR_PO()");
            this._error_num= -1;
        }
    }

    public Integer getERROR() {
        return _error_num;
    }

    public Integer getIdTest() {
        return _id_test;
    }
    public void setIdTest(Integer _id_test) {
        this._id_test = _id_test;
        if (singleTestData != null) { singleTestData.setIdTest(_id_test); }
    }

    public void setColorSemaforo(String color_semaforo) {
        this.color_semaforo = color_semaforo;
        if (singleTestData != null) { singleTestData.setColorSemaforo(color_semaforo); }
    }

    public String getColorSemaforo() {
        return color_semaforo;
    }
}
