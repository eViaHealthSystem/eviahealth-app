package com.eviahealth.eviahealth.api.tecnico;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class EPService implements Serializable {
    static final String TAG = EPService.class.getSimpleName();

    /**
     * <p>Realiza lo solicitud de login de un usuario técnico.</p>
     * @param vCr1
     * @param vCr2
     * @return
     */
    public String login(String vCr1, String vCr2){
        String msg="";
        JSONObject vOb = new JSONObject();

        try {
            JSONObject token = EPImplementation.loginWS(vCr1, vCr2);
            if (token != null) {
                Log.e(TAG, "response login: " + token.toString());

                if (token.getString("token").length() != 0 && (token.has("user"))) {
                    vOb = new JSONObject(token.getString("user"));

                    if (token.getString("token") != null && vOb.getString("user") != null) {
                        /*
                        {
                            "status_code": 200,
                            "status": "ok",
                            "user": {
                                "rol": 0,
                                "user": "admin",
                                "password_date": "2024-04-05 11:53:19",
                                "password_expiration_months": 120,
                                "twofa_activated": 0,
                                "email": "email@gmail.com",
                                "activo": 1,
                                "ver_detalle_paciente": 0,
                                "notificaciones_activated": 0,
                                "acceso_pacientes_baja": 0,
                                "fecha_last_login": "2025-02-17 10:52:26"
                            },
                            "token": "eyJ1c2VyIjp7InJvbCI6MCwidXNlciI6ImFkbWluIiwicGFzc3dvcmRfZGF0ZSI6IjIwMjQtMDQtMDUgMTE6NTM6MTkiLCJwYXNzd29yZF9leHBpcmF0aW9uX21vbnRocyI6MTIwLCJ0d29mYV9hY3RpdmF0ZWQiOjAsImVtYWlsIjoiZW1haWxAZ21haWwuY29tIiwiYWN0aXZvIjoxLCJ2ZXJfZGV0YWxsZV9wYWNpZW50ZSI6MCwibm90aWZpY2FjaW9uZXNfYWN0aXZhdGVkIjowLCJhY2Nlc29fcGFjaWVudGVzX2JhamEiOjAsImZlY2hhX2xhc3RfbG9naW4iOiIyMDI1LTAyLTE3IDEwOjUyOjI2In19"
                        }
                         */

                        Log.e(TAG, "token: " + token.getString("token"));
                        msg = token.toString();
//                        msg = "OK";
                    } else {
                        if (token.has("msg")) {
                            msg = token.getString("msg");
                        }
                    }
                }
                else {
                    // {"msg":"Usuario y contraseña incorrectos","token":""}
                    if (token.has("msg")) { msg = token.toString(); }
                    else msg = null;
                }
            }
            else {
                Log.e(TAG,"Servidor eviahealth no contesta");
                JSONObject merr = new JSONObject();
                merr.put("msg","Servidor eviahealth no contesta");
                merr.put("token","");
                msg = merr.toString();
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return "\"msg\": \"JSONException\", \"token\": \"\"";
        }
        return msg;
    }

    /***
     * <p>Realiza el envío del codigo pin para realizar la doble autentificación del login del usuario técnico.</p>
     * @param token
     * @param usuario
     * @param code
     * @return
     */
    public String activateUser(String token, String usuario, String code){

        // confirm_twofa_pin
        try {
            JSONObject pBody = new JSONObject();
            pBody.put("user", usuario);
            pBody.put("pin", code);

            JSONObject vOb = EPImplementation.activateUserWS(token, pBody);
            if (vOb == null) { return null; }
            return vOb.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
