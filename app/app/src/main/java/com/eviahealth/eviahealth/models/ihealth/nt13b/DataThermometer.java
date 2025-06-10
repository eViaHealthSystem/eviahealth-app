package com.eviahealth.eviahealth.models.ihealth.nt13b;

import android.util.Log;

import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DataThermometer {

    private String TAG = "DataThermometer";
    final String FASE = "TERMOMETRO";

    private Boolean statusDescarga;
    private Integer error_num;

    private String message;
    private Double valor;

    private int unit_flag;

    // SINGLETON
    private static DataThermometer instance = null;
    public static DataThermometer getInstance() {
        if(instance == null) {
            instance = new DataThermometer(false,"{}");
        }
        return instance;
    }

    // Constructor
    private DataThermometer(Boolean status_descarga, String message){

        this.statusDescarga = status_descarga;
        this.error_num= -1;

        this.message = message;
        this.valor = null;
        unit_flag = 0;
    }

    public void clear() {
        this.statusDescarga = false;
        this.error_num= -1;
        this.message = "{}";
        this.valor = null;
        unit_flag = 0;
    }

    public Boolean getStatusDescarga() {
        return statusDescarga;
    }

    public void setStatusDescarga(Boolean statusDescarga) {
        this.statusDescarga = statusDescarga;
    }

    // NT13B - action_measurement_result - {"unit_flag":0,"result":35.900001525878906,"ts_flag":0,"thermometer_type_flag":1,"thermometer_type":2}
    public void setResponse(String message) {
        this.message = message;

        try {
            JSONObject json = new JSONObject(message);
            if (json.has("unit_flag")) {unit_flag = json.getInt("unit_flag"); }

            Double temperatura = util.getDoubleValueJSON(this.message,"result");

            if (unit_flag == 1) {
                double fahrenheit = Double.parseDouble(temperatura.toString());
                double celsius = (fahrenheit - 32) * 5/9;
                Log.e(TAG, "fahrenheit: " + fahrenheit + ", celsius: " + celsius);
                temperatura = celsius;
            }

            if (temperatura != null) {
                this.valor = redondear(temperatura);
            }

        }
        catch (JSONException e) {
            Log.e(TAG,"EXCEPTION setResponse(): " + e.toString());
        }
    }

    private Double redondear(Double valor) {
        DecimalFormat formato = new DecimalFormat("##.#");
        // Aplicar el redondeo al número
        String temperatura = formato.format(valor).replace(",",".");
        return Double.parseDouble(temperatura);
    }

    public Double getValor() {
        return this.valor;
    }

    public void setFilesActivity(){
        try{
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String fecha = simpleDateFormat.format(new Date());

            // ACTIVIDAD TOTAL
            if (!this.message.equals("{}")){

                JSONObject json = new JSONObject();
                json.put("idpaciente", Config.getInstance().getIdPacienteEnsayo());
                json.put("fecha", fecha);
                json.put("temperatura", getValor());
                FileAccess.escribirJSON(FilePath.REGISTROS_TERMOMETRO, json);

                EVLog.log(TAG,"GENERADO FICHERO >> " + FilePath.REGISTROS_TERMOMETRO);
                EVLog.log(TAG,"DATA >> " + json.toString());
            }

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
}
