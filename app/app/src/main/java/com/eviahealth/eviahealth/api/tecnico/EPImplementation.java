package com.eviahealth.eviahealth.api.tecnico;

import android.os.StrictMode;
import android.util.Log;

import com.eviahealth.eviahealth.api.BackendConector.ApiConnector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class EPImplementation implements Serializable {
    static final String TAG = EPImplementation.class.getSimpleName();

    /**
     * Realiza el login con el sistema, enviando las credenciales y capturando la cookie "viadhox_session".
     *
     * @param pUsuario Usuario.
     * @param pPass    Contraseña.
     * @return JSONObject con la respuesta y el token extraído.
     */
    public static JSONObject loginWS(String pUsuario, String pPass) {
        JSONObject vOb = null;
        HttpsURLConnection conn = null;
        String vRespuesta = "";
        String vToken = "";

        try {
            // Configurar el CookieManager
            CookieManager cookieManager = new CookieManager();
            CookieHandler.setDefault(cookieManager);

            String host = ApiConnector.getHost();

            // IMPORTANTE: Construir la URL para login (removiendo "api-" y añadiendo "login")
            URL url = new URL(host.replace("api-", "") + "/api/login");
            Log.e(TAG, "url web: " + url.toString());

            conn = (HttpsURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Requested-With", "aaa");
            conn.connect();

            // Construir el cuerpo de la petición
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("user", pUsuario);
            jsonParam.put("password", pPass);

            // Escribir el cuerpo utilizando try-with-resources
            writeRequestBody(conn, jsonParam);

            // Recorrer las cookies para extraer "viadhox_session"
            Map<String, List<String>> headerFields = conn.getHeaderFields();
            List<String> cookiesHeader = headerFields.get("Set-Cookie");
            if (cookiesHeader != null) {
                for (String cookie : cookiesHeader) {
                    if (cookie.contains("viadhox_session")) {
                        cookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                        vToken = cookie.split(";")[0].replace("viadhox_session=", "");
                        System.out.println("Token->" + vToken);
                        break;
                    }
                    System.out.println(cookie);
                }
            }

            int responseCode = conn.getResponseCode();
            System.out.println("responseCode -> " + responseCode);
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                vRespuesta = readResponse(conn);
            } else {
                vRespuesta = "";
            }
            conn.disconnect();

            try {
                vOb = new JSONObject(vRespuesta);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception LoginWs(IOException)->" + e.getMessage());
            try {
                vOb = new JSONObject();
                vOb.put("token", vToken);
                vOb.put("msg", "No se puede acceder a la url configurada !!!");
            } catch (JSONException err) {
                err.printStackTrace();
            }
        } catch (Exception ex) {
            System.out.println("LoginWs()->" + ex.getMessage());
            Log.e(TAG, "Exception LoginWs()->" + ex.getMessage());
            try {
                vOb = new JSONObject();
                vOb.put("token", vToken);
                vOb.put("msg", "Exception accediendo a eviahealth !!!");
            } catch (JSONException err) {
                err.printStackTrace();
            }
        } finally {
            if (conn != null)
                conn.disconnect();
        }

        if (vOb != null) {
            try {
                vOb.put("token", vToken);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return vOb;
    }

    /**
     * Activa el usuario enviando un PIN de dos factores.
     *
     * @param pToken Token de autorización.
     * @param pBody  Cuerpo de la petición en formato JSONObject.
     * @return JSONObject con la respuesta.
     */
    public static JSONObject activateUserWS(String pToken, JSONObject pBody) {
        HttpsURLConnection conn = null;
        JSONObject vOb = null;
        String vRespuesta = "";
        int responseCode = 0;

        // Permitir operaciones de red en el hilo actual
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            // Construir la URL para activar el usuario
            String host = ApiConnector.getHost();
            URL url = new URL(host.replace("api-", "") + "/api/users/confirm_twofa_pin");
            Log.e(TAG, "url -> " + url.toString());

            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + pToken);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Requested-With", "aaa");

            if (pBody != null) {
                writeRequestBody(conn, pBody);
            }
            conn.connect();
            responseCode = conn.getResponseCode();
            Log.e(TAG, "responseCode -> " + responseCode);
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                vRespuesta = readResponse(conn);
            } else if (responseCode == HttpsURLConnection.HTTP_UNAUTHORIZED) {
                vRespuesta = "{ \"httpcode\": 401 }";
            } else {
                vRespuesta = "";
            }
            conn.disconnect();

            try {
                vOb = new JSONObject(vRespuesta);
            } catch (JSONException e) {
                e.printStackTrace();
                System.out.println("activateUserWS()->" + e.getMessage());
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception activateUserWS(IOException)->" + e.getMessage());
        } catch (Exception ex) {
            System.out.println("activateUserWS()->" + ex.getMessage());
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        return vOb;
    }

    /**
     * Escribe el cuerpo de la petición en la conexión utilizando try-with-resources.
     *
     * @param conn Conexión HttpsURLConnection.
     * @param body Objeto JSONObject que se enviará.
     * @throws IOException Si ocurre un error al escribir el cuerpo.
     */
    private static void writeRequestBody(HttpsURLConnection conn, JSONObject body) throws IOException {
        try (OutputStream os = conn.getOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            writer.write(body.toString());
            writer.flush();
        }
    }

    /**
     * Lee la respuesta de la conexión utilizando try-with-resources y StandardCharsets.UTF_8.
     *
     * @param conn Conexión HttpsURLConnection.
     * @return Respuesta del servidor en formato String.
     * @throws IOException Si ocurre un error al leer la respuesta.
     */
    private static String readResponse(HttpsURLConnection conn) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
}
