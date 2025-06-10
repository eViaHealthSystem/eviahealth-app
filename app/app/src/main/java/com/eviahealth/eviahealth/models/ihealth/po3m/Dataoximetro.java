package com.eviahealth.eviahealth.models.ihealth.po3m;

import android.util.Log;

import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.ihealth.communication.control.PoProfile;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Dataoximetro {
    private Boolean _status_descarga;
    private String _messure;
    private String _bateria;

    private Integer _error_num;
    private Integer battery;
    private Integer bloodoxygen;
    private Integer heartrate;

    // SINGLETON
    private static Dataoximetro instance = null;
    public static Dataoximetro getInstance() {
        if(instance == null) {
            instance = new Dataoximetro(false,"{}","{}");
        }
        return instance;
    }

    // Constructor
    private Dataoximetro(Boolean status_descarga,String bateria, String messure){
        this._status_descarga = status_descarga;
        this._bateria = bateria;
        this._messure = messure;
        this.battery = 0;
        this.bloodoxygen = 0;
        this.heartrate = 0;
        this._error_num= -1;
    }

    public void clear() {
        this._status_descarga = false;
        this._bateria = "{}";
        this._messure = "{}";
        this.battery = 0;
        this.bloodoxygen = 0;
        this.heartrate = 0;
        this._error_num= -1;
    }

    public Boolean get_status_descarga() {
        return _status_descarga;
    }

    public void set_status_descarga(Boolean _status_descarga) {
        this._status_descarga = _status_descarga;
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

            this.bloodoxygen = info.getInt(PoProfile.BLOOD_OXYGEN_PO);
            this.heartrate = info.getInt(PoProfile.PULSE_RATE_PO);

        }catch (Exception e){
            Log.e("DATA_OXI","EXCEPTION set_messure()");
            this.battery = -1;
            this.bloodoxygen = 0;
            this.heartrate = 0;
        }
    }

    public int getBattery() {
        try {
            JSONTokener jsonTokener = new JSONTokener(this._bateria);
            JSONObject jsonobject1;
            jsonobject1 = (JSONObject) jsonTokener.nextValue();
            this.battery = jsonobject1.getInt(PoProfile.BATTERY_PO);

        }catch (Exception e){
            Log.e("DATA_OXI","EXCEPTION getBattery()");
            this.battery = -1;
        }

        return this.battery;
    }

    public void setFilesActivity(){
        try{
            // {"fecha":"2021-03-05 17:22:54","bloodoxygen":96,"heartrate":83}
//            Log.e("DataOximetro","setFilesActivity()");
            String pattern = "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            String fecha = simpleDateFormat.format(new Date());

            JSONObject json_oxi = new JSONObject();
            json_oxi.put("idpaciente", Config.getInstance().getIdPacienteEnsayo());
            json_oxi.put("fecha", fecha);
            json_oxi.put("bloodoxygen", this.bloodoxygen);
            json_oxi.put("heartrate", this.heartrate);
            FileAccess.escribirJSON(FilePath.REGISTROS_OXIMETRO, json_oxi);

        } catch (JSONException | IOException err) {
            Log.e("DATA OXI CLASS", "EXCEPTION setFilesActivity() >> " + err.toString());
        }
    }

    public void setERROR_PO(String msg) {
        try {
            JSONObject info = new JSONObject(msg);
            this._error_num= info.getInt(PoProfile.ERROR_NUM_PO);

        }catch (Exception e){
            Log.e("DATA_TEN","EXCEPTION getERROR_PO()");
            this._error_num= -1;
        }
    }

    public Integer get_error_num() {
        return _error_num;
    }

    public Integer get_bloodoxygen() {
        return bloodoxygen;
    }

    public Integer get_heartrate() {
        return heartrate;
    }
}
