package com.eviahealth.eviahealth.models.beurer.ft95;

import android.util.Log;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ft95Data {
    private String TAG = "ft95Data";
    final String FASE = "TERMOMETRO";

    private Boolean statusDescarga;
    private Integer error_num;

    private String message;
    private Double valor;

    // SINGLETON
    private static ft95Data instance = null;
    public static ft95Data getInstance() {
        if(instance == null) {
            instance = new ft95Data(false,"{}");
        }
        return instance;
    }

    // Constructor
    private ft95Data(Boolean status_descarga, String message){

        this.statusDescarga = status_descarga;
        this.error_num= -1;

        this.message = message;
        this.valor = null;
    }

    public void clear() {
        this.statusDescarga = false;
        this.error_num= -1;
        this.message = "{}";
        this.valor = null;
    }

    public Boolean getStatusDescarga() {
        return statusDescarga;
    }

    public void setStatusDescarga(Boolean statusDescarga) {
        this.statusDescarga = statusDescarga;
    }

    // FT95 - { "temperature":36.3,"type":"frente","unidad":"Celsius","time":"2018-02-05 08:27:21" }
    public void setResponse(String message) {
        this.message = message;
    }

    public Double getValor() {
        Double res = null;
        try {
            this.valor = util.getDoubleValueJSON(this.message,"temperature");
            if (this.valor != null) {

                if (getUnidad().equals("Fahrenheit"))  {
                    this.valor = convertirFahrenheitACelsius(this.valor);
                }

                // Aplicar el redondeo al número
                DecimalFormat formato = new DecimalFormat("##.#");
                String temperatura = formato.format(this.valor).replace(",",".");
                Double redondeado = Double.parseDouble(temperatura);
                return redondeado;
            }
        }catch (Exception e){
            Log.e(TAG,"EXCEPTION getValor(): " + e.toString());
        }
        return res;
    }

    public String getUnidad() {
        try {
            return util.getStringValueJSON(this.message,"unidad");
        }catch (Exception e){
            Log.e(TAG,"EXCEPTION getValor(): " + e.toString());
        }
        return null;
    }

    public Double convertirFahrenheitACelsius(Double temperaturaFahrenheit) {
        return (temperaturaFahrenheit - 32) * 5/9;
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
