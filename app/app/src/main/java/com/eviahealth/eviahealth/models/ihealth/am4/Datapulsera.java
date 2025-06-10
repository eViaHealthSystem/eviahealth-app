package com.eviahealth.eviahealth.models.ihealth.am4;

import android.util.Log;

import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.ihealth.communication.control.AmProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Datapulsera {

    // >> "jsonactividadtotal.json"
    // >> "jsonactividaddiaria.json"
    // >> "jsonsuenyo.json"

    private Boolean _status_descarga;
    //  AM4 ------------------------------------------
    private String _bateria;
    private String _datos_actuales;
    private String _datos_actividad;
    private String _datos_sleep;
    private String _timeout_act;
    private String _stagereport;
    private Integer _error_num;

    private int battery;
    private String pasos_total;
    private String calorias_total;
    private Integer totalcalories;
    private String pasos_ultimo;
    private String calorias_ultimo;
    // ----------------------------------------------

    // SINGLETON
    private static Datapulsera instance = null;
    public static Datapulsera getInstance() {
        if(instance == null) {
            instance = new Datapulsera(false,"{}","{}","{}","{}","{}","{}");
        }
        return instance;
    }

    // Constructor
    private Datapulsera(Boolean status_descarga,String bateria, String datos_actuales, String datos_actividad, String datos_sleep,String report, String timeout_act){
        this._status_descarga = status_descarga;
        this._bateria = bateria;
        this._datos_actuales = datos_actuales;
        this._datos_actividad = datos_actividad;
        this._datos_sleep = datos_sleep;
        this._timeout_act = timeout_act;
        this._stagereport = report;
        this.battery = 0;
        this._error_num= -1;
    }

    public void clear() {
        this._status_descarga = false;
        this._bateria = "{}";
        this._datos_actuales = "{}";
        this._datos_actividad = "{}";
        this._datos_sleep = "{}";
        this._timeout_act = "{}";
        this.battery = 0;
        this._error_num= -1;
    }

    public Boolean get_status_descarga() {
        return _status_descarga;
    }

    public void set_status_descarga(Boolean _status_descarga) {
        this._status_descarga = _status_descarga;
    }

    public String get_bateria_json() {
        return _bateria;
    }

    public void set_bateria_json(String _bateria) {
        this._bateria = _bateria;
    }

    public String get_datos_actuales() {
        return _datos_actuales;
    }

    public void set_datos_actuales(String _datos_actuales) {

        this._datos_actuales = _datos_actuales;

        try {
            JSONTokener jsonTokener = new JSONTokener(get_datos_actuales());
            JSONObject jsonObject;
            jsonObject = (JSONObject) jsonTokener.nextValue();

            this.pasos_total = jsonObject.getString(AmProfile.SYNC_REAL_STEP_AM);
            this.calorias_total = jsonObject.getString(AmProfile.SYNC_REAL_CALORIE_AM);
            this.totalcalories = jsonObject.getInt(AmProfile.SYNC_REAL_TOTALCALORIE_AM);

        }catch (Exception e){
            Log.e("DATA_ACT","EXCEPTION set_datos_actuales()");
            this.pasos_total = "-";
            this.calorias_total = "-";
            this.totalcalories = 0;
        }
    }

    public String get_datos_actividad() {
        return _datos_actividad;
    }

    public void calculate_datos_actividad() {
        try {
            Integer pasos = 0; ;
            Integer calories = 0;
            JSONObject actividad = new JSONObject(this._datos_actividad);
            // bloques "activity"
            JSONArray array = actividad.getJSONArray("activity");
            int array_length = array.length();

            for(int i = 0; i < array_length; i++) {
                JSONObject each_data = array.getJSONObject(i);
                JSONArray each_data_array = each_data.getJSONArray("activity_each_data");
                int each_data_array_length = each_data_array.length();
//                Log.e("DATA-ACT", "activity_each_data[ " + i + "]");
                int pasos_anterior = 0;
                int calorias_anterior = 0;

                if (each_data_array_length > 1){
                    for(int j = 0; j < each_data_array_length; j++) {

                        JSONObject dato_actividad = each_data_array.getJSONObject(j);

                        int pasos_actual = dato_actividad.getInt("step");
                        int calorias_actual = dato_actividad.getInt("calorie");
//                        Log.e("DATA-ACT", "reg[" + j + "] step: " + pasos_actual + "  calorie: " + calorias_actual);

                        int pasos_restado = pasos_actual - pasos_anterior;
                        int calorias_restado = calorias_actual - calorias_anterior;
//                        Log.e("DATA-ACT", "restados step: " + pasos_restado + "  calorie: " + calorias_restado);

                        pasos_anterior = pasos_actual;
                        calorias_anterior = calorias_actual;

                       if(j != 0){
                           // Acumulamos pasos
                           pasos += pasos_restado;
                           calories += calorias_restado;
//                           Log.e("DATA-ACT", "Acumulados step: " + pasos + "  calorie: " + calories);
                       }
                    }
                }
            }

            Log.e("DATA-ACT","PASOS ACTUALES: " + pasos + " CALORIAS ACTUALES: " + calories);

            this.pasos_ultimo = Integer.toString(pasos);
            this.calorias_ultimo = Integer.toString(calories);

        }catch (Exception e){
            this.pasos_ultimo = this.pasos_total;
            this.calorias_ultimo = this.calorias_total;
        }
    }

    public void set_datos_actividad(String _datos_actividad) {
        this._datos_actividad = _datos_actividad;
    }

    public String get_datos_sleep() {
        return _datos_sleep;
    }

    public void set_datos_sleep(String _datos_sleep) {
        this._datos_sleep = _datos_sleep;
    }

    public String get_timeout_act() {
        return _timeout_act;
    }

    public void set_timeout_act(String _timeout_act) {
        this._timeout_act = _timeout_act;
    }

    public int getBattery() {
        try {
            JSONTokener jsonTokener = new JSONTokener(this._bateria);
            JSONObject jsonobject1;
            jsonobject1 = (JSONObject) jsonTokener.nextValue();
            this.battery = jsonobject1.getInt(AmProfile.QUERY_BATTERY_PERCENT_AM);

        }catch (Exception e){
            Log.e("DATA_ACT","EXCEPTION getBattery()");
            this.battery = -1;
        }

        return this.battery;
    }

    public String getPasos_total() {
        return pasos_total;
    }

    public String getCalorias_total() {
        return calorias_total;
    }

    public String getPasos_ultimo() {
        return pasos_ultimo;
    }

    public String getCalorias_ultimo() {
        return calorias_ultimo;
    }

    public void setFilesActivity(){
        try{

            //this._datos_actuales

            String pattern = "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            String fecha = simpleDateFormat.format(new Date());

            if (!this._datos_actuales.equals("{}")){
                JSONObject json_act = new JSONObject();
                json_act.put("idpaciente", Config.getInstance().getIdPacienteEnsayo());
                json_act.put("fecha", fecha);
                json_act.put("step", Integer.parseInt(this.pasos_total));
                json_act.put("calorie", Integer.parseInt(this.calorias_total));
                json_act.put("totalcalories", this.totalcalories);
                FileAccess.escribirJSON(FilePath.REGISTROS_ACTIVIDAD_TOTAL_AM4, json_act);
                EVLog.log("DATA_ACT","GENERADO FICHERO >> " + FilePath.REGISTROS_ACTIVIDAD_TOTAL_AM4);
            }

            if (!this._datos_actividad.equals("{}")){
                FileAccess.escribir(FilePath.REGISTROS_ACTIVIDAD_DIARIA_AM4, this._datos_actividad);
                EVLog.log("DATA_ACT","GENERADO FICHERO >> " + FilePath.REGISTROS_ACTIVIDAD_DIARIA_AM4);
            }
            if (!this._datos_sleep.equals("{}")) {
                FileAccess.escribir(FilePath.REGISTROS_ACTIVIDAD_SLEEP_AM4, this._datos_sleep);
                EVLog.log("DATA_ACT","GENERADO FICHERO >> " + FilePath.REGISTROS_ACTIVIDAD_SLEEP_AM4);
            }

//            FileAccess.escribir(FilePath.REGISTROS_ACTIVIDAD_REPORT, this._stagereport);
        } catch (JSONException | IOException err) {
            Log.d("DATA ACT", "EXCEPTION setFilesActivity() >> " + err.toString());
        }
    }

    public String get_stagereport() {
        return _stagereport;
    }

    public void set_stagereport(String _stagereport) {
        this._stagereport = _stagereport;
    }

    public void setERROR_ACT(String msg) {
        try {
            JSONObject info = new JSONObject(msg);
            this._error_num= info.getInt(AmProfile.ERROR_NUM_AM);

        }catch (Exception e){
            Log.e("DATA_TEN","EXCEPTION getERROR_ACT()");
            this._error_num= -1;
        }
    }

    public Integer get_error_num() {
        return _error_num;
    }

}
