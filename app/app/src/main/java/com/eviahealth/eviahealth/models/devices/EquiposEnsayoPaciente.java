package com.eviahealth.eviahealth.models.devices;

import android.util.Log;

import com.eviahealth.eviahealth.api.BackendConector.ApiConnector;
import com.eviahealth.eviahealth.api.BackendConector.ApiDeviceSystem;
import com.eviahealth.eviahealth.api.BackendConector.ApiException;
import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.api.BackendConector.ApiUrl;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.log.EVLogConfig;
import com.eviahealth.eviahealth.utils.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class EquiposEnsayoPaciente {

    private static final String TAG = "EquiposEnsayoPaciente";
    private Map<Integer, String> devices = new HashMap<>();
    private Map<Integer, Dispositivo> dispositivos = new HashMap<>();
    private String id_paciente;
    ApiDeviceSystem apiDeviceSystem = null;

    public EquiposEnsayoPaciente(String id_paciente) {
        devices.clear();
        dispositivos.clear();
        this.id_paciente = id_paciente;

        this.apiDeviceSystem = new ApiDeviceSystem();

    }

    public String getIdPaciente() { return id_paciente; }

    /**
     * Carga mapa de dispositivos del fichero dispositivos.json
     */
    public void loadDispositivos() {

        dispositivos.clear();

        try {
            JSONObject jsonObject = util.readFileJSON(new File(FileAccess.getPATH_FILES() + "dispositivos.json"));
            Log.e(TAG, "loadDispositivos(): " + jsonObject.toString());

            int elementCount = jsonObject.length();

            Integer key = 0;
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String etiqueta = keys.next();

                JSONObject obj = jsonObject.optJSONObject(etiqueta);

                boolean enable = obj.getBoolean("enable");
                String identificador = obj.getString("desc");
                String nombre = etiqueta;
                Integer id  = obj.getInt("id");
                String extra = obj.getString("extra");
                Log.e(TAG, "loadDispositivos(): " + enable + ", " + identificador + ", " + nombre + ", " + id + ", " + extra);

                dispositivos.put(key, new Dispositivo(enable, identificador, nombre,  id,  extra));
                key +=1;
            }

        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Carga nº de encusta del fichero encuesta.json
     * @return idEncuesta
     */
    public Integer loadEncuesta() {

        int id_encuesta = 0;

        try {
            JSONObject jsonObject = util.readFileJSON(new File(FileAccess.getPATH_FILES() + "encuesta.json"));
            Log.e(TAG, "loadEncuesta(): " + jsonObject.toString());

            if (jsonObject.has("id_encuesta")) {
                id_encuesta = jsonObject.getInt("id_encuesta");
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return id_encuesta;
    }

    public void updateDispositivos() {

        // Carga mapa de dispositivos del fichero dispositivos.json
        loadDispositivos();
        // Carga nº de encusta del fichero encuesta.json
        int id_encuesta = loadEncuesta();

        Log.e(TAG,"dispositivos.size(): "+ dispositivos.size());
        try {

            for (int i = 0; i < dispositivos.size(); i++) {

                Log.e(TAG,"" +dispositivos.get(i).getId() + ", " + dispositivos.get(i).toString());

                String extras = dispositivos.get(i).getExtra();
                Log.e(TAG,"dispositivos.get(i).getExtra(): "+ extras);

                // id_device == 16 LUNG
                if (dispositivos.get(i).getId() == 16) {
                    Log.e(TAG,"id_device == 16 LUNG");
                    String response = ApiMethods.getExtrasDevice(id_paciente,16);

                    JSONObject obj = new JSONObject(response);
                    if (obj.has("extra")) {
                        Log.e(TAG,"si extra: " + obj.toString());
                        String extra = obj.getString("extra");
                        if (extra.contains("lung")) {
                            Log.e(TAG,"si lung");
                            extras = extra;
                        }
                    }
                }

                UpdateDevice(id_paciente, dispositivos.get(i).getId(), dispositivos.get(i).isEnabled() ? 1 : 0, dispositivos.get(i).getIdentificador(), extras);
            }

            UpdateEncuestaPaciente(id_paciente, id_encuesta);
        }
        catch (IOException | ApiException | JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"updateDispositivos(): " + e.getMessage());
        }
    }

    /**
     * Actualiza el registro del dispositivo de un paciente en EquiposPaciente.
     * @param idpaciente
     * @param id_dispositivo
     * @param enable
     * @param mac
     * @param extra
     * @throws IOException
     * @throws ApiException
     * @throws JSONException
     */
    private void UpdateDevice(String idpaciente, int id_dispositivo, int enable, String mac, String extra) throws IOException, ApiException, JSONException {
        EVLogConfig.log(TAG,"UpdateDevice() >> getId(): " + id_dispositivo + ", isEnabled(): " + enable + ", getDesc(): " + mac + ", getExtra(): " + extra);
        JSONObject params = new JSONObject();
        params.put("idpaciente", idpaciente);
        params.put("id_device", id_dispositivo);
        params.put("enable", enable);
        params.put("desc", mac);
        params.put("extra", extra);
        EVLogConfig.log("PARAMS", params.toString());
        ApiConnector.peticion(ApiUrl.PACIENTE_EQUIPO_UPDATE, params);
    }

    /**
     * Actualiza el id_encuesta de un paciente
     * @param idpaciente
     * @param id_encuesta
     * @throws IOException
     * @throws ApiException
     * @throws JSONException
     */
    private void UpdateEncuestaPaciente(String idpaciente, int id_encuesta) throws IOException, ApiException, JSONException  {
        Log.e(TAG,"UpdateEncuestaPaciente()");
        JSONObject params = new JSONObject();
        params.put("idpaciente", idpaciente);
        params.put("id_encuesta", id_encuesta);
        ApiConnector.peticion(ApiUrl.PACIENTE_UPDATE_ENCUESTA, params);
    }

}

