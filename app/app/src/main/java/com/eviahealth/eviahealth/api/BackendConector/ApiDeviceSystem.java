package com.eviahealth.eviahealth.api.BackendConector;

import android.util.Log;

import com.eviahealth.eviahealth.models.devices.DeviceType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ApiDeviceSystem {

    private static final String TAG = "ApiDeviceSystem";
    private Map<Integer, DeviceType> devices = new HashMap<>();

    /**
     * Carga el listado de dispositivos registrados en la DB em esta clase.
     */
    public ApiDeviceSystem() {
        getApiDeviceSystem(); // Petición al servidor del listado de dispositivos del sistema
    }

    /**
     * Mapa de dispositivos cargados
     * @return
     */
    public Map<Integer, DeviceType> getDevices() { return devices; }

    /**
     * Devuelve el número de elmentos que tiene el MAP devices
     * @return
     */
    public Integer getSizeDevices() { return devices.size(); }

    // {"devices":[{"id":1,"nombre":"Oximetro","params":"[{\"col\":\"bloodoxygen\",\"name\":\"Saturación de oxígeno\"},{\"col\":\"heartrate\",\"name\":\"Ritmo cardíaco\"}]","display":"Oxímetro"},{"id":2,"nombre":"Tensiometro","params":"[{\"col\":\"sys\",\"name\":\"Presión sistólica\"},{\"col\":\"dia\",\"name\":\"Presión diastólica\"},{\"col\":\"heartRate_ten\",\"name\":\"Ritmo cardíaco\"}]","display":"Tensiómetro"},{"id":3,"nombre":"Actividad Total","params":"[]","display":"Actividad Total"},{"id":4,"nombre":"Actividad Diaria","params":"[]","display":"Actividad Diaria"},{"id":5,"nombre":"Sueño","params":"[]","display":""},{"id":6,"nombre":"Viadhox FC","params":"[]","display":""},{"id":7,"nombre":"Viadhox AD","params":"[]","display":""},{"id":8,"nombre":"CPAP","params":"[]","display":""},{"id":9,"nombre":"Encuesta","params":"[]","display":""},{"id":10,"nombre":"Termometro","params":"[{\"col\":\"temperatura\",\"name\":\"Temperatura\"}]","display":"Termómetro"},{"id":11,"nombre":"PeakFlow","params":"[{\"col\":\"flujo_resp\",\"name\":\"Flujo espiratorio máximo\"}]","display":"PeakFlow"},{"id":12,"nombre":"CAT","params":"[]","display":""},{"id":13,"nombre":"ConcentradorO2","params":"[]","display":"Con. O2"},{"id":14,"nombre":"Pulsera AM5","params":"[]","display":" "},{"id":15,"nombre":"Bascula","params":"[]","display":"Bascula"},{"id":16,"nombre":"Monitor Pulmonar","params":"[]","display":"Monitor Pulmonar"}]}

    /**
     * <p>Petición al servidor del listado de dispositivos del sistema</p>
     * <p>Descarga el contenido de la tabla Devices de la DB >> MAP devices</p>
     * <p>se obtiene MAP mediante getDevices()</p>
     */
    private void getApiDeviceSystem() {
        devices.clear();
        try {
            JSONObject respuesta = ApiConnector.peticionJSONObject(ApiUrl.DISPOSITIVOS_GET, new JSONObject());
            Log.e(TAG, "getApiDeviceSystem(): " + respuesta.toString());

            if (respuesta.has("devices")) {
                JSONArray dispositivos = respuesta.getJSONArray("devices");  // ArrayJSON
                for (int i = 0; i < dispositivos.length(); i++) {

                    JSONObject dispositivo = dispositivos.getJSONObject(i);

                    int id = dispositivo.getInt("id");
                    String nombre = dispositivo.getString("nombre");
                    String params = dispositivo.getString("params");

                    devices.put(id, new DeviceType(id, nombre, params));
                }
            }

        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        catch (ApiException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Obtiene los datos de "devices" a partir de su id, id=id_device
     * @param id
     * @return
     */
    public DeviceType getDeviceType(int id) {
        return devices.get(id);
    }

}
