package com.eviahealth.eviahealth.models.manual;

import android.util.Log;

import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class DataClassicScale {

    private String TAG = "DataClassicScale";
    private Boolean statusDescarga;
    private Integer error_num;
    private String onlineResul;     // tiene json
    private String status;

    // SINGLETON
    private static DataClassicScale instance = null;
    public static DataClassicScale getInstance() {
        if(instance == null) {
            instance = new DataClassicScale(false, null);
        }
        return instance;
    }

    // Constructor
    private DataClassicScale(Boolean statusDescarga, String onlineResul){
        this.statusDescarga = statusDescarga;
        this.error_num= -1;
        this.onlineResul = onlineResul;
        this.status = "{ \"online\": false } ";
    }

    public void clear() {
        this.statusDescarga = false;
        this.error_num= -1;
        this.onlineResul = null;    // "{}";
        this.status = "{ \"online\": false } ";
    }

    public Boolean getStatusDescarga() {
        return statusDescarga;
    }
    public void setStatusDescarga(Boolean statusDescarga) {
        this.statusDescarga = statusDescarga;
    }

    public void setWeightMeasurement(String onlineResul) {
        this.onlineResul = onlineResul;
    }
    public String getWeightMeasurement() {
        return this.onlineResul;
    }

    public String generateResult() {

        try {
//            Double imc = 0.0;
            JSONObject params = new JSONObject();
            JSONObject weightMeasure = new JSONObject(getWeightMeasurement());

            if (weightMeasure.has("weight")) {
                params.put("weight", weightMeasure.getDouble("weight"));
//                imc = weightMeasure.getDouble("weight") / ((heigth/100f) * (heigth/100f));
//                params.put("imc", imc);
            }

            if (weightMeasure.has("imc")) {
                params.put("imc", weightMeasure.getDouble("imc"));
            }

            return params.toString();
        }
        catch (Exception e) {
            Log.e("DATA_ACT","EXCEPTION generateResult()");
        }
        return "";
    }

    public void setFilesActivity(){
        try{
            JSONObject principal = new JSONObject();
            principal.put("idpaciente", Config.getInstance().getIdPacienteEnsayo());
            principal.put("identificadorpaciente", Config.getInstance().getIdPacienteEnsayo());
            principal.put("typeMeassure", "scale");

            // Crear array de medidas
            JSONArray arrayRegistros = new JSONArray();

            //region :: primer elemento en el array ** onLineResult o BodyFatResult
            JSONObject registro = new JSONObject();
            registro.put("fecha", util.getDateNow());

            String data = generateResult();
            JSONObject params = new JSONObject(data);
            registro.put("params", params);

            // Añadir arrayRegistros al array
            arrayRegistros.put(registro);

            //endregion

            // Añadir array de medidas al objeto principal
            principal.put("meassures", arrayRegistros);

            // generando fichero de los datos de la báscula
            FileAccess.escribirJSON(FilePath.REGISTROS_BASCULA, principal);
            EVLog.log(TAG,"GENERADO FICHERO >> " + FilePath.REGISTROS_BASCULA);
            EVLog.log(TAG,"DATA >> " + principal.toString());

        } catch (JSONException | IOException e) {
            Log.d(TAG, "EXCEPTION setFilesActivity() >> " + e.toString());
        }

    }

    public void setERROR(String msg) {
        try {
            this.error_num= util.getIntValueJSON(msg,"error");
        }catch (Exception e){
            Log.e(TAG,"EXCEPTION getERROR()");
            this.error_num= -1;
        }
    }

    public Integer get_error_num() {
        return error_num;
    }

    private Double roundDouble(double number) {
        BigDecimal bd = new BigDecimal(Double.toString(number));
        bd = bd.setScale(1, RoundingMode.HALF_UP);

        Log.e(TAG,"roundDouble >> " + bd.doubleValue());

        return bd.doubleValue();
    }
}
