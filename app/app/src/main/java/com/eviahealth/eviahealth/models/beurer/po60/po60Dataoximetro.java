package com.eviahealth.eviahealth.models.beurer.po60;

import android.util.Log;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.utils.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class po60Dataoximetro {
    private Boolean _status_descarga;
    private String _messure;
    private Integer _error_num;

    private Integer bloodoxygen;
    private Integer heartrate;

    // SINGLETON
    private static po60Dataoximetro instance = null;
    public static po60Dataoximetro getInstance() {
        if(instance == null) {
            instance = new po60Dataoximetro(false,"{}");
        }
        return instance;
    }

    // Constructor
    private po60Dataoximetro(Boolean status_descarga, String messure){
        this._status_descarga = status_descarga;
        this._messure = messure;
        this.bloodoxygen = 0;
        this.heartrate = 0;
        this._error_num= -1;
    }

    public void clear() {
        this._status_descarga = false;
        this._messure = "{}";
        this._error_num= -1;

        this.bloodoxygen = 0;
        this.heartrate = 0;
    }

    public Boolean get_status_descarga() {
        return _status_descarga;
    }

    public void set_status_descarga(Boolean _status_descarga) {
        this._status_descarga = _status_descarga;
    }

    public String getMessure() {
        return _messure;
    }

    public void setMessure(String medida) {
        this._messure = medida;
        try {
            this.bloodoxygen = util.getIntValueJSON(medida,"SpO2Max");
            this.heartrate = util.getIntValueJSON(medida,"PRMin");
        }catch (Exception e){
            Log.e("DATA_OXI","EXCEPTION set_messure()");
            this.bloodoxygen = 0;
            this.heartrate = 0;
        }
    }

    public void setFilesActivity(){
        try{
            // {"fecha":"2021-03-05 17:22:54","bloodoxygen":96,"heartrate":83}
            String pattern = "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            String fecha = simpleDateFormat.format(new Date());

            JSONObject json_oxi = new JSONObject();
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
            this._error_num= util.getIntValueJSON(msg,"error");
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
