package com.eviahealth.eviahealth.api.BackendConector;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.api.meeting.EAImplementation;
import com.eviahealth.eviahealth.models.vitalograph.Patient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ApiMethods {
    private static final String TAG = "ApiMethods";

    /**
     * Obtiene las características de un paciente que dispone la DB
     * @param idpaciente
     * @return
     */
    public static Patient loadCharacteristics(String idpaciente) {
        try {
            JSONObject params = new JSONObject();
            params.put("identificador", idpaciente);
            params.put("idpaciente", idpaciente);
            JSONObject respuesta = ApiConnector.peticionJSONObject(ApiUrl.CARACTERISTICAS_PACIENTE, params);
            Log.e(TAG,"API CARACTERISTICAS PACIENTE: " + respuesta); // {"paciente":{"gender":"Hombre","age":65,"weight":70,"height":165}}

            JSONObject paciente = respuesta.getJSONObject("paciente");
            return new Patient(paciente.toString());
        }
        catch (IOException | ApiException | JSONException e) {
            Log.e(TAG, "Exception loadCharacteristics: " + e.getMessage());
            return new Patient("Hombre", "01/01/1060",65, 70, 165, true);
        }
    }

    /**
     * Actualiza las caracteristicas establecidas de un paciente en la DB
     * @param idpaciente
     * @param patient
     * @return
     */
    public static Boolean updateCharacteristics(String idpaciente, Patient patient) {
        try {
            JSONObject characteristics = new JSONObject();
            characteristics.put("paciente", patient.get());

            JSONObject params = new JSONObject();
            params.put("identificador", idpaciente);
            params.put("idpaciente", idpaciente);
            params.put("characteristics",characteristics);

            String response = ApiConnector.peticion(ApiUrl.SET_CARACTERISTICAS_PACIENTE, params);
            Log.e(TAG,"API UPDATE CARACTERISTICAS PACIENTE: " + response);

            if (response.contains("API ERROR HTTP CODE")) return false;
            else return true;
        }
        catch (IOException | ApiException | JSONException e) {
            Log.e(TAG, "Exception updateCharacteristics: " + e.getMessage());
            return false;
        }
    }

    /**
     * <p>Obtiene el FEV1 Personal Best mediante solicitud al backend</p>
     * @param idpaciente
     * @param patient
     * @return
     */
    public static Integer generateFEV1PB(String idpaciente, Patient patient) {
        try {
            /* BODY
            idpaciente:"99999999",
            age:40,
            gender:"Mujer",
            height:167,
            weight:65
             */
            JSONObject params = new JSONObject();
            params.put("idpaciente", idpaciente);
            params.put("age",patient.getAge());
            params.put("gender",patient.getGender());
            params.put("height",patient.getHeight());
            params.put("weight",patient.getHeight());

            String response = ApiConnector.peticion(ApiUrl.CALCULATE_FEV1PB, params);
            Log.e(TAG,"API CALCULATE FEV1PB: " + response);

            if (response.contains("API ERROR HTTP CODE")) return null;
            else {
                Float responseFloat = Float.parseFloat(response);
                Integer fev1pb = (int)(responseFloat * 100);
                Log.e(TAG,"FEV1PB: " + fev1pb);
                return fev1pb;
            }
        }
        catch (IOException | ApiException | JSONException e) {
            Log.e(TAG, "Exception generateFEV1PB: " + e.getMessage());
            return null;
        }
    }

    /**
     * <p>Comprueba si el identificador de un paciente existe.</p>
     * <p>Uso en modo multipaciente</p>
     * @param idpaciente
     * @return
     */
    public static Boolean existeIdPaciente(String idpaciente) {
        try {

            JSONObject params = new JSONObject();
            params.put("idpaciente", idpaciente);

            String response = ApiConnector.peticion(ApiUrl.PACIENTE_EXISTE, params);
            Log.e(TAG,"API EXISTE ID PACIENTE: " + response);

            if (response.contains("API ERROR HTTP CODE")) return false;

            JSONObject respuesta = new JSONObject(response);
            if (respuesta.has("status")) {
                if (respuesta.getInt("status") == 1) return true;
            }

            return false;
        }
        catch (IOException | ApiException | JSONException e) {
            Log.e(TAG, "Exception existeIdPaciente: " + e.getMessage());
            return false;
        }
    }

    /**
     * <p>Obtiene el nombre y apellidos de un identificador de paciente.</p>
     * <p>Uso en modo multipaciente</p>
     * @param idpaciente
     * @return
     */
    public static String getNameIdPaciente(String idpaciente) {
        try {

            JSONObject params = new JSONObject();
            params.put("idpaciente", idpaciente);

            String response = ApiConnector.peticion(ApiUrl.PACIENTE_GET_DATOS, params);
            Log.e(TAG,"API EXISTE ID PACIENTE: " + response);

            if (response.contains("API ERROR HTTP CODE")) return "";
            
            return response;
        }
        catch (IOException | ApiException | JSONException e) {
            Log.e(TAG, "Exception getNameIdPaciente: " + e.getMessage());
            return "";
        }
    }

    /**
     * <p>Solicitud de extras asignados a un dispositivo de un paciente</p>
     * <p>POST -> /api/paciente/equipos/get_extra</p>
     * <p>Body:</p>
     * <p>{ "idpaciente": "30303030", "id_device": 16 }</p>
     * @param idpaciente
     * @param id_device
     * @return
     * <p>{</p>
     * <p>   "extra": "{\"dayOfweek\":[\"Thu\",\"Sat\",\"Tue\",\"Sun\",\"Wed\",\"Mon\",\"Fri\"],\"paciente\":{\"sex\":\"Hombre\",\"age\":65,\"weight\":70,\"height\":165},\"lung\":{\"FEV1PB\":453,\"GreenZone\":60,\"YellowZone\":40,\"OrangeZone\":20}}"</p>
     * <p>}</p>
     */
    public static String getExtrasDevice(String idpaciente, int id_device) {
        try {
            JSONObject params = new JSONObject();
            params.put("idpaciente", idpaciente);
            params.put("id_device", id_device);

            String response = ApiConnector.peticion(ApiUrl.PACIENTE_EQUIPO_GET_EXTRAS, params);
            Log.e(TAG,"RESPONSE getExtrasDevice(" + idpaciente + "," + id_device + "): " + response);

            if (response.contains("API ERROR HTTP CODE")) {
                Log.e(TAG, "API ERROR HTTP CODE: " + response);
                return "{}";
            }

            return response;
        }
        catch (IOException e) {
//            e.printStackTrace();
            Throwable cause = e.getCause();
            if (cause != null) {

                String errorMsg = cause.toString();
                String motivo, description;
                if (errorMsg == null) {
                    motivo = "NULL";
                    description = "IOException ocurrió, pero el mensaje es null.";
                }
                else if (errorMsg.contains("EAI_NODATA")) {
                    motivo = "EAI_NODATA";
                    description = "No hay datos DNS para el hostname";
                }
                else if (errorMsg.contains("EAI_AGAIN")) {
                    motivo = "EAI_AGAIN";
                    description = "Error temporal en la resolución DNS";
                }
                else if (errorMsg.contains("EAI_FAIL")) {
                    motivo = "EAI_FAIL";
                    description = "alla de resolución DNS en el servidor";
                }
                else if (errorMsg.contains("EAI_NONAME")) {
                    motivo = "EAI_NONAME";
                    description = "No se puede resolver el hostname";
                }
                else if (errorMsg.contains("ECONNREFUSED")) {
                    motivo = "ECONNREFUSED";
                    description = "El servidor rechazó la conexión ";
                }
                else if (errorMsg.contains("ETIMEDOUT")) {
                    motivo = "ETIMEDOUT";
                    description = "Tiempo de espera agotado al conectar";
                }
                else if (errorMsg.contains("ENETUNREACH")) {
                    motivo = "ENETUNREACH";
                    description = "La red no está disponible o no hay conexión";
                }
                else if (errorMsg.contains("EHOSTUNREACH")) {
                    motivo = "EHOSTUNREACH";
                    description = "El servidor de destino es inalcanzable";
                }
                else if (errorMsg.contains("EBADF")) {
                    motivo = "EBADF";
                    description = "Uso incorrecto del socket";
                }
                else if (errorMsg.contains("EPIPE")) {
                    motivo = "EPIPE";
                    description = "Conexión cerrada inesperadamente";
                }
                else if (errorMsg.contains("ENOTCONN")) {
                    motivo = "ENOTCONN";
                    description = "Intento de enviar datos sin conexión activa";
                }
                else {
                    motivo = "UNKNOWN";
                    description = "Error de red desconocido: " + errorMsg;
                }
                Log.e(TAG, "IOException: Causa: (" + motivo + "), Descripción: " + description);
                return "{ \"code\": -999, \"cause\": \"" + motivo + "\", \"description\": \"" + description + "\" }";
            }
            return "{ \"code\": -999, \"cause\": \"IOException\", \"description\": \"Causa desconocida\" }";
        }
        catch (JSONException e ) {
            Log.e(TAG, "JSONException getExtrasDevice: msg(" + e.getMessage() + ")");
            String motivo = "JSONException";
            Throwable description = e.getCause();
            if (description != null) {
                Log.e("JSON_ERROR", "Causa de JSONException: " + e.getCause());
                return "{ \"code\": -998, \"cause\": \"" + motivo + "\", \"description\": \"" + description.toString() + "\" }";
            }
            return "{ \"code\": -998, \"cause\": \"" + motivo + "\", \"description\": \"" + description + "\" }";
        }
        catch (ApiException e) {
            Log.e(TAG, "ApiException getExtrasDevice: code(" + e.getCode() + "), msg(" + e.getMessage() + ")");
            return "{ \"code\": " + e.getCode() + ", \"cause\": \"none\", \"description\": \"" + e.getMessage() + "\" }";
        }
    }


    // [{"id_device":1,"enable":1,"desc":"PO60-FE2D51A59800","extra":"{\"dayOfweek\":[\"Mon\", \"Tue\", \"Wed\", \"Thu\", \"Fri\", \"Sat\", \"Sun\"]}","alertas":"[]"},{"id_device":2,"enable":1,"desc":"BP3L-10082C4FF244","extra":"{\"dayOfweek\":[\"Mon\", \"Tue\", \"Wed\", \"Thu\", \"Fri\", \"Sat\", \"Sun\"]}","alertas":"[]"},{"id_device":3,"enable":1,"desc":"MAMBO6-EB04501082E1","extra":"{\"dayOfweek\":[\"Mon\", \"Tue\", \"Wed\", \"Thu\", \"Fri\", \"Sat\", \"Sun\"]}","alertas":"[]"},{"id_device":4,"enable":0,"desc":"Deshabilitado","extra":"{\"dayOfweek\":[\"Mon\", \"Tue\", \"Wed\", \"Thu\", \"Fri\", \"Sat\", \"Sun\"]}","alertas":"[]"},{"id_device":5,"enable":0,"desc":"Deshabilitado","extra":"{\"dayOfweek\":[\"Mon\", \"Tue\", \"Wed\", \"Thu\", \"Fri\", \"Sat\", \"Sun\"]}","alertas":"[]"},{"id_device":6,"enable":0,"desc":"Deshabilitado","extra":"{\"dayOfweek\":[\"Mon\", \"Tue\", \"Wed\", \"Thu\", \"Fri\", \"Sat\", \"Sun\"]}","alertas":"[]"},{"id_device":7,"enable":0,"desc":"Deshabilitado","extra":"{\"dayOfweek\":[\"Mon\", \"Tue\", \"Wed\", \"Thu\", \"Fri\", \"Sat\", \"Sun\"]}","alertas":"[]"},{"id_device":8,"enable":0,"desc":"Deshabilitado","extra":"{\"dayOfweek\":[\"Mon\", \"Tue\", \"Wed\", \"Thu\", \"Fri\", \"Sat\", \"Sun\"]}","alertas":"[]"},{"id_device":9,"enable":1,"desc":"2","extra":"{\"dayOfweek\":[\"Mon\",\"Tue\",\"Wed\",\"Thu\",\"Fri\",\"Sat\",\"Sun\"]}","alertas":"[]"},{"id_device":10,"enable":1,"desc":"NT13B-FF470003AD20","extra":"{\"dayOfweek\":[\"Mon\", \"Tue\", \"Wed\", \"Thu\", \"Fri\", \"Sat\", \"Sun\"]}","alertas":"[]"},{"id_device":11,"enable":0,"desc":"Deshabilitado","extra":"{}","alertas":"[]"},{"id_device":12,"enable":1,"desc":"Habilitado","extra":"{\"dayOfweek\":[\"Mon\",\"Tue\",\"Wed\",\"Thu\",\"Fri\"]}","alertas":"[]"},{"id_device":13,"enable":0,"desc":"Deshabilitado","extra":"{\"dayOfweek\":[\"Mon\", \"Tue\", \"Wed\", \"Thu\", \"Fri\", \"Sat\", \"Sun\"]}","alertas":"[]"},{"id_device":14,"enable":0,"desc":"Deshabilitado","extra":"{\"dayOfweek\":[\"Mon\", \"Tue\", \"Wed\", \"Thu\", \"Fri\", \"Sat\", \"Sun\"]}","alertas":"[]"},{"id_device":15,"enable":1,"desc":"HS2S-004D3212FD3E","extra":"{\"hs2s\":{\"impedance\":false},\"dayOfweek\":[\"Mon\",\"Tue\",\"Wed\",\"Thu\",\"Fri\",\"Sat\",\"Sun\"]}","alertas":"[]"},{"id_device":16,"enable":1,"desc":"LUNG-008025D85A5B","extra":"{\"lung\":{\"FEV1PB\":340,\"GreenZone\":80,\"YellowZone\":50,\"OrangeZone\":30},\"dayOfweek\":[\"Mon\",\"Tue\",\"Wed\",\"Thu\",\"Fri\",\"Sat\",\"Sun\"]}","alertas":"[]"}]

    /**
     * Descarga los dispositivos asignados a un paciente
     * @param id_paciente
     * @return
     */
    public static String downloadDevices(String id_paciente) {
        try {
            JSONObject params = new JSONObject();
            params.put("idpaciente", id_paciente);
            JSONArray equipos = ApiConnector.peticionJSONArray(ApiUrl.PACIENTE_EQUIPO_GET, params);
            if (equipos != null) {
                Log.e(TAG, "equipos(" + id_paciente + "): " + equipos.toString());
                return equipos.toString();
            }
            return null;
        }
        catch (IOException | ApiException | JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"EXCEPTION downloadDevices(): " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtene el listado de formación actual en la instalación
     * @param pContext
     * @return (JSONObject)
     * <p>{ "formacion": {}}</p>
     * <p>"httpCode": int > if httpCode != 200 Error en la consulta</p>
     */
    public static JSONObject getFormacion(Context pContext){
        JSONObject vOb = null;
        try {
//            String pURL = url_web + pContext.getString(R.string.formacion);
            String pURL = ApiConnector.getHost() + ApiUrl.FORMACION.getURL();
            vOb = EAImplementation.postAPI(pURL, null);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(pContext, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return vOb;
    }

    //region :: CONSENTIMIENTO PACIENTE
    public static String getTextConsentimiento(String idpaciente) {
        try {

            JSONObject params = new JSONObject();
            params.put("idpaciente", idpaciente);
            // "parametro": " consentimiento_inicial_app"
            params.put("parametro", "consentimiento_inicial_app");

            String response = ApiConnector.peticion(ApiUrl.GET_TEXT_CONSENTIMIENTO, params);

            if (response != null) { Log.e(TAG,"API TEXTO CONSENTIMIENTO: " + response); }
            else { Log.e(TAG,"API TEXTO CONSENTIMIENTO: null"); }

            if (response.contains("API ERROR HTTP CODE")) return "";

            return response;
        }
        catch (IOException | ApiException | JSONException e) {
            Log.e(TAG, "Exception getTextConsentimiento: " + e.getMessage());
            return "";
        }
    }

    public static Boolean getConsentimientoPaciente(String idpaciente) {
        try {

            JSONObject params = new JSONObject();
            params.put("idpaciente", idpaciente);

            String response = ApiConnector.peticion(ApiUrl.GET_CONSENTIMIENTO, params);
            if (response != null) { Log.e(TAG,"API GET CONSENTIMIENTO: " + response); }
            else { Log.e(TAG,"API GET CONSENTIMIENTO: null"); }

            if (response.contains("API ERROR HTTP CODE")) return false;

            JSONObject respuesta = new JSONObject(response);
            if (respuesta.has("consentimiento_inicial")) {
                if (respuesta.getInt("consentimiento_inicial") == 1) return true;
            }

            return false;
        }
        catch (IOException | ApiException | JSONException e) {
            Log.e(TAG, "Exception getConsentimientoPaciente: " + e.getMessage());
            return false;
        }
    }

    public static Boolean setConsentimientoPaciente(String idpaciente) {
        try {

            JSONObject params = new JSONObject();
            params.put("idpaciente", idpaciente);

            JSONObject response = ApiConnector.postAPI_HTTPCODE(ApiUrl.CONSENTIMIENTO_OK, params);

            if (response != null) { Log.e(TAG,"API SET CONSENTIMIENTO: " + response.toString()); }
            else { Log.e(TAG,"API SET CONSENTIMIENTO: null"); }

            if (response.has("httpCode")) {
                if (response.getInt("httpCode") == 200) return true;
            }

            return false;
        }
        catch (JSONException e) {
            Log.e(TAG, "Exception setConsentimientoPaciente: " + e.getMessage());
            return false;
        }
    }

    //endregion
}
