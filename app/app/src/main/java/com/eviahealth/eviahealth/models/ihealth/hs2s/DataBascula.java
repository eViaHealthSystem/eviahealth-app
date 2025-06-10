package com.eviahealth.eviahealth.models.ihealth.hs2s;

import android.util.Log;

import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.util;
import com.ihealth.communication.control.Hs2sProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class DataBascula {

    private String TAG = "DataBascula";
    private Boolean statusDescarga;
    private Integer error_num;

    private String onlineResul;     // tiene json
    private String historyData;
    private String bodyFatResult;   // tiene json
    private String battery;
    private String status;
    private Integer height;

    // SINGLETON
    private static DataBascula instance = null;
    public static DataBascula getInstance() {
        if(instance == null) {
            instance = new DataBascula(false,null,null, null,null,0);
        }
        return instance;
    }

    // Constructor
    private DataBascula(Boolean statusDescarga,String bateria, String onlineResul, String historyData, String bodyFatResult, Integer height){
        this.statusDescarga = statusDescarga;
        this.error_num= -1;
        this.battery = bateria;
        this.onlineResul = onlineResul;
        this.historyData = historyData;
        this.bodyFatResult = bodyFatResult;
        this.height = height;
        this.status = "{ \"history\": false, \"online\": false, \"bodyfat\": false } ";
    }

    public void clear() {
        this.statusDescarga = false;
        this.error_num= -1;
        this.battery = null;        // "{}";
        this.onlineResul = null;    // "{}";
        this.historyData = null;    // "[]";
        this.bodyFatResult = null;  // "{}";
        this.status = "{ \"history\": false, \"online\": false, \"bodyfat\": false } ";
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Boolean getStatusDescarga() {
        return statusDescarga;
    }

    public void setStatusDescarga(Boolean statusDescarga) {
        this.statusDescarga = statusDescarga;
    }

    public void setStatusDataHistory(Boolean status) {
        try {
            JSONObject obj = new JSONObject(this.status);
            obj.put("history",status);
            this.status = obj.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void setStatusOnline(Boolean status) {
        try {
            JSONObject obj = new JSONObject(this.status);
            obj.put("online",status);
            this.status = obj.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void setStatusBodyFat(Boolean status) {
        try {
            JSONObject obj = new JSONObject(this.status);
            obj.put("bodyfat",status);
            this.status = obj.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Boolean getStatusDataHistory() {
        return util.getBoolValueJSON(this.status,"history");
    }

    public Boolean getStatusOnline() {
        return util.getBoolValueJSON(this.status,"online");
    }

    public Boolean getStatusBodyFat() {
        return util.getBoolValueJSON(this.status,"bodyfat");
    }

    public String getStatusFlags() { return this.status; }

    // {"dataID":"004D3212FD3E17013645330600000000","status":0,"weight":99.06}
    public Double getWeigth() { return util.getDoubleValueJSON(this.onlineResul,Hs2sProfile.DATA_WEIGHT); }

//  message: {"dataID":"004D3212FD3E17013645330600000000","status":0,"weight":99.06}
    public void setMessageWeight(String onlineResul) {
        this.onlineResul = onlineResul;
    }

//  message: {"status":0,"describe":"Measure Successful","data_body_fat_result":{"dataID":"004D3212FD3E17014246269800000000","weight":97.94999694824219,"impedance":[{"impedance":515},{"impedance":445},{"impedance":419},{"impedance":395},{"impedance":1},{"impedance":18348},{"impedance":25961},{"impedance":44530}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701424626,"right_time":1,"body_building":0,"instruction_type":1,"body_fit_percentage":"33.2","muscle_mas":"61.4","bone_salt_content":"4.1","body_water_rate":"48.9","protein_rate":"13.8","visceral_fat_grade":"20","physical_age":"68"}}
    public void setBodyFatResult(String message) {
        this.bodyFatResult = message;
    }

    public String getBodyFatResult() {
        return this.bodyFatResult;
    }

    public String getOnlineResul() {
        return this.onlineResul;
    }

    public Boolean getInstructionType() {
        try {
            JSONObject obj = new JSONObject(this.bodyFatResult);

            if (obj.has(Hs2sProfile.DATA_BODY_FAT_RESULT)) {
                JSONObject dataBodyFat = obj.getJSONObject(Hs2sProfile.DATA_BODY_FAT_RESULT);
                Integer instructionType = dataBodyFat.getInt(Hs2sProfile.DATA_INSTRUCTION_TYPE);

                if (instructionType == 1) { return true; }
            }

            return false;
        }
        catch (Exception e) {
            Log.e("DATA_ACT","EXCEPTION getInstructionType()");
            return false;
        }
    }

    public String getMeasureTime() {
        Integer utc = util.getIntValueJSON(this.bodyFatResult, Hs2sProfile.DATA_MEASURE_TIME);
        if (utc == null) return null;
        return util.getDate(utc);
    }

//  message: [] sin datos
//  message: [{"dataID":"004D3212FD3E17014273539800000000","weight":"97.83","impedance":[{"impedance":515},{"impedance":444},{"impedance":420},{"impedance":396},{"impedance":1},{"impedance":18348},{"impedance":25961},{"impedance":47257}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701427353,"right_time":1,"body_building":0,"instruction_type":1,"body_fit_percentage":"33.1","muscle_mas":"61.4","bone_salt_content":"4.0","body_water_rate":"49.0","protein_rate":"13.8","visceral_fat_grade":"20","physical_age":"68"},{"dataID":"004D3212FD3E17014273829800000000","weight":"97.83","impedance":[{"impedance":515},{"impedance":444},{"impedance":1243},{"impedance":0},{"impedance":1},{"impedance":18348},{"impedance":25961},{"impedance":47286}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701427382,"right_time":1,"body_building":0,"instruction_type":0},{"dataID":"004D3212FD3E17014273989800000000","weight":"97.81","impedance":[{"impedance":513},{"impedance":442},{"impedance":416},{"impedance":1295},{"impedance":1},{"impedance":18348},{"impedance":25961},{"impedance":47302}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701427398,"right_time":1,"body_building":0,"instruction_type":0},{"dataID":"004D3212FD3E17014274199800000000","weight":"97.79","impedance":[{"impedance":512},{"impedance":440},{"impedance":415},{"impedance":391},{"impedance":1},{"impedance":18348},{"impedance":25961},{"impedance":47323}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701427419,"right_time":1,"body_building":0,"instruction_type":1,"body_fit_percentage":"33.1","muscle_mas":"61.4","bone_salt_content":"4.0","body_water_rate":"49.0","protein_rate":"13.8","visceral_fat_grade":"20","physical_age":"68"},{"dataID":"004D3212FD3E17014274479800000000","weight":"97.8","impedance":[{"impedance":516},{"impedance":443},{"impedance":418},{"impedance":394},{"impedance":1},{"impedance":18348},{"impedance":25961},{"impedance":47351}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701427447,"right_time":1,"body_building":0,"instruction_type":1,"body_fit_percentage":"33.1","muscle_mas":"61.4","bone_salt_content":"4.0","body_water_rate":"49.0","protein_rate":"13.8","visceral_fat_grade":"20","physical_age":"68"},{"dataID":"004D3212FD3E17014274799800000000","weight":"98.02","impedance":[{"impedance":518},{"impedance":445},{"impedance":420},{"impedance":395},{"impedance":1},{"impedance":18348},{"impedance":25961},{"impedance":47383}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701427479,"right_time":1,"body_building":0,"instruction_type":1,"body_fit_percentage":"33.2","muscle_mas":"61.4","bone_salt_content":"4.1","body_water_rate":"48.9","protein_rate":"13.8","visceral_fat_grade":"20","physical_age":"68"}]
//  message: [{"dataID":"004D3212FD3E17013646579900000000","weight":"99.02","impedance":[{"impedance":65535},{"impedance":65535},{"impedance":65535},{"impedance":65535},{"impedance":1},{"impedance":18348},{"impedance":25960},{"impedance":50097}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701364657,"right_time":1,"body_building":0,"instruction_type":0}]
    public void setHistoryData(String historyData) {
        this.historyData = historyData;
    }

    public String getHistoryData() { return  this.historyData; }

    // message{"user_count":1,"unit_current":1,"battery":100}
    public void setBattery(String value) {
        this.battery = value;
    }

    public Integer getBattery() {
        try {
            JSONObject obj = new JSONObject(this.battery);
            return obj.getInt(Hs2sProfile.BATTERY_HS);
        }catch (JSONException e){
            Log.e("DATA_ACT","EXCEPTION getBattery()");
            return null;
        }
    }

    public String generateResult(String msg) {

        try {
            JSONObject obj = new JSONObject(msg);
            JSONObject params = new JSONObject();

            // Comprobamos si solo se ha medido peso
            if (obj.has(Hs2sProfile.DATA_WEIGHT)) {
                Double weight = roundDouble(obj.getDouble(Hs2sProfile.DATA_WEIGHT));
                params.put("weight", weight);
                params.put("imc",getIMC(weight,this.height));
            }

            if (obj.has(Hs2sProfile.DATA_BODY_FAT_RESULT)) {
                JSONObject dataBodyFat = obj.getJSONObject(Hs2sProfile.DATA_BODY_FAT_RESULT);
                Integer instructionType = dataBodyFat.getInt(Hs2sProfile.DATA_INSTRUCTION_TYPE);

                Double weight = roundDouble(dataBodyFat.getDouble(Hs2sProfile.DATA_WEIGHT));
                params.put("weight", weight);
                params.put("imc",getIMC(weight,this.height));

                if (instructionType == 1) {
                    // hay medida de grasa corporal
                    Double data_body_fit_percentage = dataBodyFat.getDouble(Hs2sProfile.DATA_BODY_FIT_PERCENTAGE);
                    Double masaMagra = weight * (1 - (data_body_fit_percentage / 100));

                    Integer BMR = getBMR(masaMagra.floatValue());
                    Double bodyWaterContent = weight * (dataBodyFat.getDouble(Hs2sProfile.DATA_BODY_WATER_RATE) / 100);
                    Integer CCI = getCCI(weight, dataBodyFat.getDouble(Hs2sProfile.DATA_MUSCLE_MASS), dataBodyFat.getDouble(Hs2sProfile.DATA_BONE_SALT_CONTENT), bodyWaterContent);

                    params.put("grasa_corporal", roundDouble(dataBodyFat.getDouble(Hs2sProfile.DATA_BODY_FIT_PERCENTAGE))); // %
                    params.put("masa_magra", roundDouble(masaMagra));  // kg
                    params.put("agua_corporal", roundDouble(dataBodyFat.getDouble(Hs2sProfile.DATA_BODY_WATER_RATE))); // %
                    params.put("bmr", BMR);
                    params.put("masa_muscular", roundDouble(dataBodyFat.getDouble(Hs2sProfile.DATA_MUSCLE_MASS))); // kg
                    params.put("tgv", roundDouble(dataBodyFat.getDouble(Hs2sProfile.DATA_VISCERAL_FAT_GRADE)));
//                    params.put("cci", CCI); // kcal
                    params.put("masa_osea", roundDouble(dataBodyFat.getDouble(Hs2sProfile.DATA_BONE_SALT_CONTENT))); // kg
                }
            }

            return params.toString();
        }
        catch (Exception e) {
            Log.e("DATA_ACT","EXCEPTION generateResult()");
        }

        return "";
    }

    public String generateResultHistorico() {

        try {
            JSONArray arrayHistory = new JSONArray(getHistoryData());
            Log.e(TAG,"DATA HISTORY: " + arrayHistory.toString());

            if (arrayHistory.length() > 0) {

                JSONObject paramHistorial = new JSONObject();
                JSONObject item = arrayHistory.getJSONObject(arrayHistory.length()-1); // el último registro es el histórico más actual

                Double weight = 0.0;
                if (item.has(Hs2sProfile.DATA_WEIGHT)) {
                    weight = roundDouble(item.getDouble(Hs2sProfile.DATA_WEIGHT));
                    paramHistorial.put("weight", weight);
                    paramHistorial.put("imc",getIMC(weight,this.height));
                }

                Integer instructionType = item.getInt(Hs2sProfile.DATA_INSTRUCTION_TYPE);
                if (instructionType == 1) {
                    Double data_body_fit_percentage = item.getDouble(Hs2sProfile.DATA_BODY_FIT_PERCENTAGE);
                    Double masaMagra = weight * (1 - (data_body_fit_percentage / 100));
                    Integer BMR = getBMR(masaMagra.floatValue());

                    paramHistorial.put("grasa_corporal", roundDouble(item.getDouble(Hs2sProfile.DATA_BODY_FIT_PERCENTAGE))); // %
                    paramHistorial.put("masa_magra", roundDouble(masaMagra));  // kg
                    paramHistorial.put("agua_corporal", roundDouble(item.getDouble(Hs2sProfile.DATA_BODY_WATER_RATE))); // %
                    paramHistorial.put("bmr", BMR);
                    paramHistorial.put("masa_muscular", roundDouble(item.getDouble(Hs2sProfile.DATA_MUSCLE_MASS))); // kg
                    paramHistorial.put("tgv", roundDouble(item.getDouble(Hs2sProfile.DATA_VISCERAL_FAT_GRADE)));
                    paramHistorial.put("masa_osea", roundDouble(item.getDouble(Hs2sProfile.DATA_BONE_SALT_CONTENT))); // kg
                }

                Long measureTime = item.getLong(Hs2sProfile.DATA_MEASURE_TIME);
                paramHistorial.put("fecha", util.toStrDateTime(measureTime));

                return paramHistorial.toString();
            }
        }
        catch (JSONException e) {
            Log.e("DATA_ACT","EXCEPTION generateResultHistorico()");
        }
        return "";
    }

    /***
     * Calcula el IMC "Índice de Masa Corporal"
     * @param weight
     * @param height
     * @return
     */
    public Double getIMC(double weight, Integer height){
        float fheight = (float)height / (float)100.0;
        Double imc = weight / (fheight * fheight);
        Double result = roundDouble(imc);
        Log.e(TAG,"IMC >> " + result);
        return result;
    }

    public Integer getBMR(float masaMagra) {
        return 370 + (int)(21.6 * masaMagra);
    }

    // Total de Grasa Visceral
    public float getTGV(double visceral_fat_grade, double bone_salt_content) {
        return (float) (visceral_fat_grade * bone_salt_content);
    }

    // Indice de Composición Corporal (CCI) (kcal)
    public Integer getCCI(double weight, double muscle_mas, double bone_salt_content, Double bodyWaterContent) {
        Double cci = (muscle_mas + bone_salt_content + bodyWaterContent) / weight;
        return cci.intValue();
    }

    @Override
    public String toString() {

        String json = "{ \"sleep\": [{ ";
        json += "\"sleep_each_data\": [ ";
//        for (ATSleepReportItem item: itemSleep) {
//            json += getJsonItem(item) + ", ";
//        }
        json += "]} ";
        json += "]}";

        return  json;
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
            if (getStatusBodyFat() || getStatusOnline()) {
                JSONObject registro = new JSONObject();
                registro.put("fecha", util.getDateNow());

                String data = "";
                if (getStatusBodyFat()) {
                    data = generateResult(getBodyFatResult());
                } else {
                    data = generateResult(getOnlineResul());
                }

                JSONObject params = new JSONObject(data);
                registro.put("params", params);

                // Añadir arrayRegistros al array
                arrayRegistros.put(registro);
            }
            //endregion

            //region :: Registros de historial
            if (getStatusDataHistory()) {
                JSONArray arrayHistory = new JSONArray(getHistoryData());
                Log.e(TAG,"DATA HISTORY: " + arrayHistory.toString());
                for (int i = 0; i < arrayHistory.length(); i++) {
                    JSONObject paramHistorial = new JSONObject();
                    JSONObject item = arrayHistory.getJSONObject(i);

                    Double weight = 0.0;
                    if (item.has(Hs2sProfile.DATA_WEIGHT)) {
                        weight = roundDouble(item.getDouble(Hs2sProfile.DATA_WEIGHT));
                        paramHistorial.put("weight", weight);

                        if (Config.getInstance().getMultipaciente() == false) {
                            paramHistorial.put("imc", getIMC(weight, this.height));
                        }
                    }

                    Integer instructionType = item.getInt(Hs2sProfile.DATA_INSTRUCTION_TYPE);
                    if (instructionType == 1) {
                        Double data_body_fit_percentage = item.getDouble(Hs2sProfile.DATA_BODY_FIT_PERCENTAGE);
                        Double masaMagra = weight * (1 - (data_body_fit_percentage / 100));
                        Integer BMR = getBMR(masaMagra.floatValue());

                        paramHistorial.put("grasa_corporal", roundDouble(item.getDouble(Hs2sProfile.DATA_BODY_FIT_PERCENTAGE))); // %
                        paramHistorial.put("masa_magra", roundDouble(masaMagra));  // kg
                        paramHistorial.put("agua_corporal", roundDouble(item.getDouble(Hs2sProfile.DATA_BODY_WATER_RATE))); // %
                        paramHistorial.put("bmr", BMR);
                        paramHistorial.put("masa_muscular", roundDouble(item.getDouble(Hs2sProfile.DATA_MUSCLE_MASS))); // kg
                        paramHistorial.put("tgv", roundDouble(item.getDouble(Hs2sProfile.DATA_VISCERAL_FAT_GRADE)));
                        paramHistorial.put("masa_osea", roundDouble(item.getDouble(Hs2sProfile.DATA_BONE_SALT_CONTENT))); // kg
                    }

                    JSONObject registroHistorial = new JSONObject();

                    Long measureTime = item.getLong(Hs2sProfile.DATA_MEASURE_TIME);
                    registroHistorial.put("fecha", util.toStrDateTime(measureTime));
                    registroHistorial.put("params", paramHistorial);
                    arrayRegistros.put(registroHistorial);  // Añadimos al array
                }
            }
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
