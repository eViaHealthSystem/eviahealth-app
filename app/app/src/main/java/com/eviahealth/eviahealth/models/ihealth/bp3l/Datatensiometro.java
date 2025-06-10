package com.eviahealth.eviahealth.models.ihealth.bp3l;

import android.util.Log;

import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.ihealth.communication.control.BpProfile;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Datatensiometro {

    private Boolean _status_descarga;
    private String _messure;

    private String _bateria;

    private Integer _highPressure;
    private Integer _lowPressure;
    private Integer _pulso;
    private Boolean _ahr;
    private Boolean _hsd;

    private Integer _error_num;
    private String _error_description;

    private int battery;


    // SINGLETON
    private static Datatensiometro instance = null;
    public static Datatensiometro getInstance() {
        if(instance == null) {
            instance = new Datatensiometro(false,"{}","{}");
        }
        return instance;
    }

    // Constructor
    private Datatensiometro(Boolean status_descarga,String bateria, String messure){
        this._status_descarga = status_descarga;
        this._bateria = bateria;
        this._messure = messure;
    }

    public void clear() {
        this._status_descarga = false;
        this._bateria = "{}";
        this._messure = "{}";
        this._highPressure = null;
        this._lowPressure = null;
        this._ahr = false;
        this._hsd = false;
        this._pulso = null;
        this.battery = 0;
        this._error_num= -1;
        this._error_description = null;
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

    public String get_messure() {
        return _messure;
    }

    public void set_messure(String medida) {
        this._messure = medida;
        try {

            JSONTokener jsonTokener = new JSONTokener(this._messure);
            JSONObject info = (JSONObject) jsonTokener.nextValue();

            this._highPressure = info.getInt(BpProfile.HIGH_BLOOD_PRESSURE_BP);
            this._lowPressure = info.getInt(BpProfile.LOW_BLOOD_PRESSURE_BP);
            this._pulso = info.getInt(BpProfile.PULSE_BP);

            this._ahr = info.getBoolean(BpProfile.MEASUREMENT_AHR_BP);
            this._hsd = info.getBoolean(BpProfile.MEASUREMENT_HSD_BP);

        }catch (Exception e){
            Log.e("DATA_TEN","EXCEPTION set_messure()");
            this.battery = -1;
            this._highPressure = null;
            this._lowPressure = null;
            this._ahr = false;
            this._hsd = false;
            this._pulso = null;
        }
    }

    public Integer get_highPressure() {
        return _highPressure;
    }

    public Integer get_lowPressure() {
        return _lowPressure;
    }

    public Boolean get_arrhythmia() {
        return _ahr;
    }

    public Integer get_pulsaciones() {
        return _pulso;
    }

    public int getBattery() {
        try {
            JSONTokener jsonTokener = new JSONTokener(this._bateria);
            JSONObject jsonobject1;
            jsonobject1 = (JSONObject) jsonTokener.nextValue();
            this.battery = jsonobject1.getInt(BpProfile.BATTERY_BP);

        }catch (Exception e){
            Log.e("DATA_TEN","EXCEPTION getBattery()");
            this.battery = -1;
        }

        return this.battery;
    }

    public void setERROR_BP(String msg) {
        try {

            JSONObject info = new JSONObject(msg);
            this._error_num= info.getInt(BpProfile.ERROR_NUM_BP);
            EVLog.log("DATA_TEN","setERROR_BP() >> error: " + this._error_num);

            this._error_description = info.getString(BpProfile.ERROR_DESCRIPTION_BP);
        }catch (Exception e){
            Log.e("DATA_TEN","EXCEPTION setERROR_BP()");
            this._error_num= -1;
            this._error_description = "exception";
        }
    }

    public Integer get_error_num() {
        return _error_num;
    }

    public String get_error_description() {
        return _error_description;
    }

    public void setFilesActivity(){
        try{

            String pattern = "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            String fecha = simpleDateFormat.format(new Date());

            JSONObject json_ten = new JSONObject();
            json_ten.put("idpaciente", Config.getInstance().getIdPacienteEnsayo());
            json_ten.put("fecha", fecha);
            json_ten.put("sys", this._highPressure);
            json_ten.put("dia", this._lowPressure);
            json_ten.put("heartRate", this._pulso);
            FileAccess.escribirJSON(FilePath.REGISTROS_TENSIOMETRO, json_ten);

        } catch (JSONException | IOException err) {
            Log.e("DATA TEN CLASS", "EXCEPTION setFilesActivity() >> " + err.toString());
        }
    }


}
