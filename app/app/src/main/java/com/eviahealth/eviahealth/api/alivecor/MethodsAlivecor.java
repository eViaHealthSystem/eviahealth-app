package com.eviahealth.eviahealth.api.alivecor;

import android.os.StrictMode;
import android.util.Log;

import com.eviahealth.eviahealth.api.BackendConector.ApiConnector;
import com.eviahealth.eviahealth.api.BackendConector.ApiException;
import com.eviahealth.eviahealth.api.BackendConector.ApiUrl;
import com.eviahealth.eviahealth.models.alivecor.AuthAlivecor;
import com.eviahealth.eviahealth.models.vitalograph.Patient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MethodsAlivecor implements Serializable {

    static final String TAG = "MethodsAlivecor";

    /**
     * Obtiene el JWT de AliveCor Docker mediante puente de Backend
     * @return
     */
    public static JSONObject getJWT_AlivecorDocker(String idpaciente) {
        try {

            JSONObject params = new JSONObject();
            params.put("identificador", idpaciente);
            params.put("idpaciente", idpaciente);
            Log.e(TAG, "idpaciente: " + idpaciente);

            JSONObject response = postAPI_AlivecorDocker(ApiUrl.JWT_KARDIA, params);
            if (response != null) {
                Log.e(TAG, "API JWT: " + response);
            }
            else { Log.e(TAG, "API JWT: null"); }
            return response;
        }
        catch (JSONException e) {
            Log.e(TAG, "Exception getJWT_AlivecorDocker: " + e.getMessage());
            return null;
        }
    }

    public static JSONObject postAPI_AlivecorDocker(ApiUrl purl, JSONObject pBody) {

        HttpsURLConnection conn = null;
        JSONObject vOb = null;
        String vRespuesta = "";
        Integer responseCode = 0;

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            URL url = new URL(ApiConnector.buildURL(purl));
            Log.e(TAG,"url docker -> " + url.toString());
            conn = (HttpsURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Authorization", ApiConnector.getToken());

            if(pBody != null) {
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(pBody.toString());
                writer.flush();
                writer.close();
                os.flush();
                os.close();
            }
            conn.connect();

            String protocol = conn.getCipherSuite();
            Log.e("TLS_VERSION", "Protocolo en uso: " + protocol);

            responseCode = conn.getResponseCode();
            Log.e(TAG,"responseCode postAPI_AlivecorDocker -> " + responseCode);

            if (responseCode == HttpsURLConnection.HTTP_OK || responseCode == HttpsURLConnection.HTTP_CREATED) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    vRespuesta += line;
                }
            }
            else {
                vRespuesta = "";
            }

            conn.disconnect();

//            Log.e("API", "vRespuesta;: " + vRespuesta);

            try {
                if (vRespuesta == "" || vRespuesta == null) {
                    if ((responseCode == HttpsURLConnection.HTTP_INTERNAL_ERROR || responseCode == HttpsURLConnection.HTTP_NOT_FOUND)) {
                        vRespuesta = "{ \"httpCode\": " + responseCode.toString() + " }";
                    }
                    else { vRespuesta = "{ \"httpCode\": -1 }"; }
                }

                vOb = new JSONObject(vRespuesta);
                if (!vOb.has("httpCode")) {
                    vOb.put("httpCode", responseCode);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                System.out.println("postAPI_AlivecorDocker()->" + e.getMessage());

            }
        }catch (IOException e) {
            Log.e(TAG,"Exception postAPI(IOException)->" + e.getMessage());
            try {
                vOb = new JSONObject("{ \"httpCode\": -999 }");
            } catch (JSONException err) {
                err.printStackTrace();
            }
        }catch (Exception ex){
            System.out.println("postAPI_AlivecorDocker()->" + ex.getMessage());
        }
        finally {
            if(conn != null)
                conn.disconnect();
        }

        if (vOb == null) {
            try {
                vOb = new JSONObject("{ \"httpCode\": -2 }");
            }catch (JSONException e) {
            }
        }

        Log.e(TAG,"Response postAPI_AlivecorDocker: " + vOb);
        return vOb;
    }

    public static JSONObject getJWT_Alivecor() {
        try {
            /*
            "bundleId": "com.eviahealth.eviahealth",
            "partnerId": "I6KjBh3K7cU6vodYGbh5d6450c13htcp",
            "patientMrn": "12345",
            "teamId": "Kmo7ifJa6uhnXssd7MkYd6450bxfv0f7"
            */

            JSONObject params = new JSONObject();
            params.put("bundleId", AuthAlivecor.BUNDLEID);
            params.put("partnerId", AuthAlivecor.PARTNERID);
            params.put("patientMrn", AuthAlivecor.PATIENTMRN);
            params.put("teamId", AuthAlivecor.TEAMID);

            JSONObject response = postAPI(AuthAlivecor.API_ALIVECOR, params);
            Log.e(TAG,"API JWT: " + response);
            return response;
        }
        catch (JSONException e) {
            Log.e(TAG, "Exception getJWT_Alivecor: " + e.getMessage());
            return null;
        }
    }

    public static JSONObject postAPI(String pURL, JSONObject pBody) {

//        HttpURLConnection conn = null;
        HttpsURLConnection conn = null;
        JSONObject vOb = null;
        String vRespuesta = "";
        Integer responseCode = 0;

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            URL url = new URL(pURL);
            Log.e(TAG,"url docker -> " + url.toString());
            conn = (HttpsURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Authorization", "Basic c3Q4cjJqZXQ6eDZucDg4Yzc=");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            if(pBody != null) {
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(pBody.toString());
                writer.flush();
                writer.close();
                os.flush();
                os.close();
            }
            conn.connect();

            String protocol = conn.getCipherSuite();
            Log.e("TLS_VERSION", "Protocolo en uso: " + protocol);

            responseCode = conn.getResponseCode();
            Log.e(TAG,"responseCode postAPI -> " + responseCode);

            if (responseCode == HttpsURLConnection.HTTP_OK || responseCode == HttpsURLConnection.HTTP_CREATED) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    vRespuesta += line;
                }
            }
            else {
                vRespuesta = "";
            }

            conn.disconnect();

//            Log.e("API", "vRespuesta;: " + vRespuesta);

            try {
                if (vRespuesta == "" || vRespuesta == null) {
                    if ((responseCode == HttpsURLConnection.HTTP_INTERNAL_ERROR || responseCode == HttpsURLConnection.HTTP_NOT_FOUND)) {
                        vRespuesta = "{ \"httpCode\": " + responseCode.toString() + " }";
                    }
                    else { vRespuesta = "{ \"httpCode\": -1 }"; }
                }

                vOb = new JSONObject(vRespuesta);
                if (!vOb.has("httpCode")) {
                    vOb.put("httpCode", responseCode);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                System.out.println("postAPI()->" + e.getMessage());

            }
        }catch (IOException e) {
            Log.e(TAG,"Exception postAPI(IOException)->" + e.getMessage());
            try {
                vOb = new JSONObject("{ \"httpCode\": -999 }");
            } catch (JSONException err) {
                err.printStackTrace();
            }
        }catch (Exception ex){
            System.out.println("postAPI()->" + ex.getMessage());
        }
        finally {
            if(conn != null)
                conn.disconnect();
        }

        if (vOb == null) {
            try {
                vOb = new JSONObject("{ \"httpCode\": -2 }");
            }catch (JSONException e) {
            }
        }

        Log.e(TAG,"Response postAPI: " + vOb);
        return vOb;
    }

    public static String getNamePdf(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray meassuresArray = jsonObject.getJSONArray("meassures");
            if (meassuresArray.length() > 0) {
                JSONObject firstMeassure = meassuresArray.getJSONObject(0);
                JSONObject params = firstMeassure.getJSONObject("params");
                return params.getString("namePdf");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
