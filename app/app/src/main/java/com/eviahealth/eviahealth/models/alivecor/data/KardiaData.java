package com.eviahealth.eviahealth.models.alivecor.data;

import android.util.Log;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.models.alivecor.models.EMethodUI;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class KardiaData {
    private String TAG = "KardiaData";
    private Boolean _status_descarga;
    private Integer _error_num;
    private String msgfail;

    private Float batteryLevel;
    private String deviceType;
    private String deviceMac;
    private String leadConfiguration;
    private String namePdf;
    private String kaiResult;
    private String kardiaVersion;
    private String resultECG;
    private String detailsECG;
    private String bpm;
    private Boolean inverter;
    private String algorithmPackage;
    private String resultScreenDeterminationInfo;
    private EMethodUI methodUI = EMethodUI.None;

    private static KardiaData instance = null;
    public static KardiaData getInstance() {
        if(instance == null) {
            instance = new KardiaData();
        }
        return instance;
    }

    // Constructor
    private KardiaData(){
        clear();
    }

    public void clear() {
        this._status_descarga = false;
        this._error_num= -1;
        this.msgfail = null;
        this.batteryLevel = null;
        this.deviceType = null;
        this.deviceMac = null;
        this.leadConfiguration = null;
        this.namePdf = null;
        this.kaiResult = null;
        this.kardiaVersion = null;
        this.resultECG = null;
        this.detailsECG = null;
        this.bpm = null;
        this.inverter = null;
        this.algorithmPackage = null;
        this.resultScreenDeterminationInfo = null;
        this.methodUI = EMethodUI.None;
    }

    public Boolean getStatusDescarga() {
        return _status_descarga;
    }

    public void setStatusDescarga(Boolean _status_descarga) { this._status_descarga = _status_descarga; }

    public Float getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Float batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceMac() {
        return deviceMac;
    }

    public void setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
    }

    public String getLeadConfiguration() {
        return leadConfiguration;
    }

    public void setLeadConfiguration(String leadConfiguration) {
        this.leadConfiguration = leadConfiguration;
    }

    public String getNamePdf() {
        return namePdf;
    }

    public void setNamePdf(String namePdf) {
        this.namePdf = namePdf;
    }

    public String getKaiResult() {
        return kaiResult;
    }

    public void setKaiResult(String kaiResult) {
        this.kaiResult = kaiResult;
    }

    public String getKardiaVersion() {
        return kardiaVersion;
    }

    public void setKardiaVersion(String kardiaVersion) {
        this.kardiaVersion = kardiaVersion;
    }

    public String getResultECG() {
        return resultECG;
    }

    public void setResultECG(String resultECG) {
        this.resultECG = resultECG;
    }

    public String getDetailsECG() {
        return detailsECG;
    }

    public void setDetailsECG(String detailsECG) {
        this.detailsECG = detailsECG;
    }

    public String getBpm() {
        return bpm;
    }

    public void setBpm(String bpm) {
        this.bpm = bpm;
    }

    public Boolean getInverter() {
        return inverter;
    }
    public void setInverter(Boolean inverter) {
        this.inverter = inverter;
    }

    public void setInstance(KardiaData instance) {
        KardiaData.instance = instance;
    }

    public void setMethodUI(EMethodUI methodUI) {
        this.methodUI = methodUI;
    }
    public String getMethodUI() { return  methodUI.toString(); }

    public String getAlgorithmPackage() {
        return algorithmPackage;
    }

    public void setAlgorithmPackage(String algorithmPackage) {
        this.algorithmPackage = algorithmPackage;
    }

    public String getResultScreenDeterminationInfo() {
        return resultScreenDeterminationInfo;
    }

    public void setResultScreenDeterminationInfo(String resultScreenDeterminationInfo) {
        this.resultScreenDeterminationInfo = resultScreenDeterminationInfo;
    }

    public JSONObject createJSON() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("batteryLevel", this.batteryLevel);
            obj.put("deviceType", this.deviceType);
            obj.put("deviceMac", this.deviceMac);
            obj.put("leadConfiguration", this.leadConfiguration);
            obj.put("namePdf", this.namePdf);
            obj.put("kaiResult", this.kaiResult);
            obj.put("kardiaVersion", this.kardiaVersion);
            obj.put("resultECG", this.resultECG);
            obj.put("detailsECG", this.detailsECG);
            obj.put("bpm", this.bpm);
            obj.put("inverter", this.inverter);
            obj.put("ui", getMethodUI());
            obj.put("algorithmPackage", this.getAlgorithmPackage());
            obj.put("resultScreenDeterminationInfo", this.resultScreenDeterminationInfo);
            return obj;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        JSONObject obj = createJSON();
        if (obj != null) { return obj.toString(); }
        return "null value";
    }

    public void setFilesActivity(){

        JSONObject params = createJSON();
        if (params == null) {
            return;
        }

        try{
            JSONObject obj = new JSONObject();
            obj.put("idpaciente", Config.getInstance().getIdPacienteEnsayo());
            obj.put("identificadorpaciente", Config.getInstance().getIdPacienteEnsayo());
            obj.put("typeMeassure", "ecg");

            // Array meassures
            JSONArray meassures = new JSONArray();

            JSONObject registro = new JSONObject();
            registro.put("fecha", util.getDateNow());
            registro.put("params", params);

            // Añadimos el registro a meassures
            meassures.put(registro);

            // -------------
            obj.put("meassures", meassures);

            FileAccess.escribirJSON(FilePath.REGISTROS_ECG, obj);

            EVLog.log(TAG,"GENERADO FICHERO >> " + FilePath.REGISTROS_ECG);
            EVLog.log(TAG,"DATA >> " + obj.toString());

        } catch (JSONException | IOException e) {
            Log.d(TAG, "EXCEPTION setFilesActivity() >> " + e.toString());
        }
    }

    public void setERROR2(String msg) {
        try {
            this._error_num= util.getIntValueJSON(msg,"error");
        }catch (Exception e){
            Log.e("DATA_TEN","EXCEPTION getERROR_PO()");
            this._error_num= -1;
        }
    }

    public Integer getERROR() {
        return _error_num;
    }

    public String getMsgfail() {
        return this.msgfail;
    }

    public void setMsgfail(Integer error, String msgfail, String details){

        try {
            JSONObject obj = new JSONObject();
            obj.put("error", error);
            obj.put("fail", msgfail);
            obj.put("details", details);

            this.msgfail = obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            this.msgfail = null;
        }
    }

}
