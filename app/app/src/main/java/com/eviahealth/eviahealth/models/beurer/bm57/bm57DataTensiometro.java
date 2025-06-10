package com.eviahealth.eviahealth.models.beurer.bm57;

import android.util.Log;

import com.eviahealth.eviahealth.bluetooth.beurer.bm57.bm57control;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.utils.util;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class bm57DataTensiometro {

    private Integer status;
    private String messure;
    private String problems;

    private Integer highPressure;
    private Integer lowPressure;
    private Integer pulso;
    private Integer pulsoIrregular;

    private Boolean _status_descarga;
    private Integer _error_num;

    // SINGLETON
    private static bm57DataTensiometro instance = null;
    public static bm57DataTensiometro getInstance() {
        if(instance == null) {
            instance = new bm57DataTensiometro("{}");
        }
        return instance;
    }

    // Constructor
    private bm57DataTensiometro(String messure){
        Log.e("bm57DATA","bm57DataTensiometro(String messure)****************************************************************");
        this.status = util.getIntValueJSON(messure,"status");
        this._status_descarga = false;
        this.messure = messure;
    }

    public void clear() {

        this.messure = "{}";
        this.problems = "{}";

        this.status = null;
        this.highPressure = null;
        this.lowPressure = null;
        this.pulso = null;
        this.pulsoIrregular = null;

        this._status_descarga = false;
        this._error_num= -1;
    }

    public String getMessure() {
        return this.messure;
    }

    public void setMessure(String medida) {
        this.messure = medida;

        this.status = util.getIntValueJSON(messure,"status");
        this.highPressure = util.getIntValueJSON(this.messure,"systolic");
        this.lowPressure = util.getIntValueJSON(this.messure,"diastolic");
        this.pulso = util.getIntValueJSON(this.messure,"pulse");

        this.problems = bm57control.msgGetMeasurementStatus(this.messure);

        this.pulsoIrregular = util.getIntValueJSON(this.problems,"irregularPulse");
        this._error_num = 0;
//        setError(this.problems); // Obtiene numero de error asignado
    }

    public Integer getStatus() {
        return this.status;
    }

    public Integer getHighPressure() {
        return this.highPressure;
    }

    public Integer getLowPressure() {
        return this.lowPressure;
    }

    public Integer getPulsaciones() {
        return this.pulso;
    }

    public Integer getPulsoIrregular() {
        return this.pulsoIrregular;
    }

    public String getProblems() { return  this.problems; }

    public Boolean get_status_descarga() {
        return _status_descarga;
    }

    public void set_status_descarga(Boolean _status_descarga) {
        this._status_descarga = _status_descarga;
    }

    public void setError(String msg) {
        this._error_num = util.getIntValueJSON(msg,"error");
        if (this._error_num == null) { this._error_num = -1; }
    }

    public Integer get_error_num() {
        return _error_num;
    }

    public void setFilesActivity(){
        try{

            String pattern = "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            String fecha = simpleDateFormat.format(new Date());

            JSONObject json_ten = new JSONObject();
            json_ten.put("idpaciente", Config.getInstance().getIdPacienteEnsayo());
            json_ten.put("fecha", fecha);
            json_ten.put("sys", this.highPressure);
            json_ten.put("dia", this.lowPressure);
            json_ten.put("heartRate", this.pulso);
            FileAccess.escribirJSON(FilePath.REGISTROS_TENSIOMETRO, json_ten);

        } catch (JSONException | IOException err) {
            Log.e("DATA TEN CLASS", "EXCEPTION setFilesActivity() >> " + err.toString());
        }
    }


}
