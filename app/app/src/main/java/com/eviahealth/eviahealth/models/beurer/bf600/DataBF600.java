package com.eviahealth.eviahealth.models.beurer.bf600;

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

public class DataBF600 {

    private String TAG = "DataBF600";
    private Boolean statusDescarga;
    private Integer error_num;
    private String onlineResul;     // tiene json
    private String bodyFatResult;   // tiene json
    private String battery;
    private String status;

    // SINGLETON
    private static DataBF600 instance = null;
    public static DataBF600 getInstance() {
        if(instance == null) {
            instance = new DataBF600(false,null, null,null);
        }
        return instance;
    }

    // Constructor
    private DataBF600(Boolean statusDescarga,String bateria, String onlineResul, String bodyFatResult){
        this.statusDescarga = statusDescarga;
        this.error_num= -1;
        this.battery = bateria;
        this.onlineResul = onlineResul;
        this.bodyFatResult = bodyFatResult;
        this.status = "{ \"online\": false, \"bodyfat\": false } ";
    }

    public void clear() {
        this.statusDescarga = false;
        this.error_num= -1;
        this.battery = null;        // "{}";
        this.onlineResul = null;    // "{}";
        this.bodyFatResult = null;  // "{}";
        this.status = "{ \"online\": false, \"bodyfat\": false } ";
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
    public void setBodyComposition(String message) {
        this.bodyFatResult = message;
    }
    public String getBodyComposition() {
        return this.bodyFatResult;
    }
    public String getWeightMeasurement() {
        return this.onlineResul;
    }


    public void setBattery(String value) { this.battery = value; }

    public Integer getBattery() {
        try {
            JSONObject obj = new JSONObject(this.battery);
            return obj.getInt("battery");
        }catch (JSONException e){
            Log.e("DATA_ACT","EXCEPTION getBattery()");
            return null;
        }
    }


    // Masa Magra
    public Double calculateMasaMagra(Integer bmr) {
        Double masaMagra = (double) (bmr - 370) / 21.6;
        return roundDouble(masaMagra);
    }

    public Integer getBMR(float masaMagra) {
        return 370 + (int)(21.6 * masaMagra);
    }

    // Total de Grasa Visceral
    public Double getTGV(double visceral_fat_grade, double bone_salt_content) {
        return roundDouble(visceral_fat_grade * bone_salt_content);
    }

    public float getVisceralFat(float weight, float height, float age, int sex) {
        float visceralFat = 0.0f;
        if (sex == 0) {
            // Hombre
            if (weight > (13.0f - (height * 0.5f)) * -1.0f) {
                float subsubcalc = ((height * 1.45f) + (height * 0.1158f) * height) - 120.0f;
                float subcalc = weight * 500.0f / subsubcalc;
                visceralFat = (subcalc - 6.0f) + (age * 0.07f);
            }
            else {
                float subcalc = 0.691f + (height * -0.0024f) + (height * -0.0024f);
                visceralFat = (((height * 0.027f) - (subcalc * weight)) * -1.0f) + (age * 0.07f) - age;
            }
        }
        else {
            if (height < weight * 1.6f) {
                float subcalc = ((height * 0.4f) - (height * (height * 0.0826f))) * -1.0f;
                visceralFat = ((weight * 305.0f) / (subcalc + 48.0f)) - 2.9f + (age * 0.15f);
            }
            else {
                float subcalc = 0.765f + height * -0.0015f;
                visceralFat = (((height * 0.143f) - (weight * subcalc)) * -1.0f) + (age * 0.15f) - 5.0f;
            }
        }

        return visceralFat;
    }

    // Indice de Composición Corporal (CCI) (kcal)
    public Integer getCCI(double weight, double muscle_mas, double bone_salt_content, Double bodyWaterContent) {
        Double cci = (muscle_mas + bone_salt_content + bodyWaterContent) / weight;
        return cci.intValue();
    }

    public String generateResult() {

        try {
            JSONObject params = new JSONObject();
            JSONObject weightMeasure = new JSONObject(getWeightMeasurement());

            if (weightMeasure.has("weight")) {
                params.put("weight", weightMeasure.getDouble("weight"));
            }

            if (weightMeasure.has("imc")) {
                params.put("imc", weightMeasure.getDouble("imc"));
            }

            JSONObject bodyComposition = new JSONObject(getBodyComposition());
            // si impendace == 0 no se ha medido grasa corporal
            if (bodyComposition.has("impedance")) {
                if (bodyComposition.getInt("impedance") != 0) {

                    // Grasa Corporal
                    if (bodyComposition.has("body_fat")) {
                        params.put("grasa_corporal", bodyComposition.getDouble("body_fat")); // %
                    }

                    // BRM y Masa Magra
                    if (bodyComposition.has("bmr")) {
                        Integer bmr = bodyComposition.getInt("bmr");
                        params.put("bmr", bmr);
                        params.put("masa_magra", calculateMasaMagra(bmr));  // kg
                    }

                    // Agua Corporal
                    if (bodyComposition.has("water")) {
                        params.put("agua_corporal", bodyComposition.getDouble("water")); // %
                    }

                    // Masa Muscular
                    if (bodyComposition.has("muscle_percentage")) {
                        params.put("masa_muscular", bodyComposition.getDouble("muscle_percentage")); // kg
                    }

                    // Masa Osea
                    if (bodyComposition.has("boneMass")) {
                        params.put("masa_osea", bodyComposition.getDouble("boneMass")); // kg
                    }

                    // TGV >> Tejido Graso Visceral o Grasa visceral
//                    Double weight = weightMeasure.getDouble("weight");
//                    Double height = (double)paciente.getHeight() / 100.0f;
//                    int sex = (paciente.getGender().equals("Hombre")) ? 0 : 1;
//                    float visceral_fat_grade = datos.getVisceralFat(weight.floatValue(),height.floatValue(),paciente.getAge(),sex);
//                    Double tgv = datos.getTGV(visceral_fat_grade, bodyComposition.getDouble("boneMass"));
//                    setTextView(valTxtTGV, "" + );
                    Double tgv = 0.0;
                    params.put("tgv", tgv.intValue());

//                    params.put("cci", CCI); // kcal

                }
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
//            principal.put("identificadorpaciente", Config.getInstance().getIdPacienteTablet());
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
