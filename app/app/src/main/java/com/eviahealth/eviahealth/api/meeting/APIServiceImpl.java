package com.eviahealth.eviahealth.api.meeting;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.api.BackendConector.ApiConnector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class APIServiceImpl implements Serializable{

    static final String TAG = APIServiceImpl.class.getSimpleName();

    /**
     * Obtener si un paciente tiene una videollamada planificada
     * @param pContext
     * @param idPaciente (String)
     * @return (JSONObject)
     * <p>"status": 0 > Paciente sin videollamada planificada</p>
     * <p>"status": 1 > Paciente con videollamada planificada</p>
     * <p>"httpCode": int > if httpCode != 200 Error en la consulta</p>
     */
    public JSONObject isActivePatientMeeting(Context pContext, String idPaciente){

        JSONObject vOb = null;

        try {
            JSONObject params = new JSONObject();
            params.put("idpaciente", idPaciente);

            String pURL = ApiConnector.getHost() + MeetingUrl.CONSULTA.getURL(); //pContext.getString(R.string.consulta);
            vOb = EAImplementation.postAPI(pURL, params);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(pContext, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return vOb;
    }

    /**
     * Descargas las videollamadas planificadas de un paciente
     * @param pContext
     * @param idPaciente (String)
     * @return (JSONObject)
     * <p>{ "consulta": {}}</p>
     * <p>"httpCode": int > if httpCode != 200 Error en la consulta</p>
     */
    public JSONObject getPatientMeeting(Context pContext, String idPaciente){

        JSONObject vOb = null;

        try {
            JSONObject params = new JSONObject();
            params.put("idpaciente", idPaciente);

            String pURL = ApiConnector.getHost() + MeetingUrl.PLANIFICADA.getURL(); //pContext.getString(R.string.planificada);
            vOb = EAImplementation.postAPI(pURL, params);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(pContext, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return vOb;
    }

    /**
     * Confirmación de videollamda por paciente.
     * @param pContext
     * @param idPaciente (String)
     * @param idPrescriptor (String)
     * @return (JSONObject)
     * <p>"httpCode": int > if httpCode != 200 Error en la consulta</p>
     */
    public JSONObject confirmarMeeting(Context pContext, String idPaciente, String idPrescriptor){

        JSONObject vOb = null;
        String fecha = getDateNow();

        try {
            JSONObject params = new JSONObject();
            params.put("idprescriptor", idPrescriptor);
            params.put("idpaciente", idPaciente);
            params.put("fechaConfirmado", fecha);
            Log.e(TAG,"Param: " + params);

            String pURL = ApiConnector.getHost() + MeetingUrl.CONFIRMAR.getURL(); //pContext.getString(R.string.confirmar);
            vOb = EAImplementation.postAPI(pURL, params);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(pContext, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return vOb;
    }

    /**
     * Rechazar videollamda por paciente.
     * @param pContext
     * @param idPaciente (String)
     * @param idPrescriptor (String)
     * @param motivo (String)
     * @return (JSONObject)
     * <p>"httpCode": int > if httpCode != 200 Error en la consulta</p>
     */
    public JSONObject refusedMeeting(Context pContext, String idPaciente, String idPrescriptor, String motivo){

        JSONObject vOb = null;
        String fecha = getDateNow();

        try {
            JSONObject params = new JSONObject();
            params.put("idprescriptor", idPrescriptor);
            params.put("idpaciente", idPaciente);
            params.put("motivo", motivo);
            params.put("fechaConfirmado", fecha);
            Log.e(TAG,"Param: " + params);

            String pURL = ApiConnector.getHost() + MeetingUrl.RECHAZAR.getURL(); //pContext.getString(R.string.rechazar);
            vOb = EAImplementation.postAPI(pURL, params);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(pContext, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return vOb;
    }

    private String getDateNow(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        return dateFormat.format(calendar.getTime());
    }
}
