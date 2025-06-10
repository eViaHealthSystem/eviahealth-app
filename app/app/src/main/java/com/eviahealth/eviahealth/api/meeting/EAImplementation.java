package com.eviahealth.eviahealth.api.meeting;

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

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class EAImplementation implements Serializable {

    static final String TAG = EAImplementation.class.getSimpleName();

    public static JSONObject postAPI(String pURL, JSONObject pBody) {

        HttpsURLConnection conn = null;
        JSONObject vOb = null;
        String vRespuesta = "";
        int responseCode = 0;
        String pToken = ApiConnector.getToken();

        try {
            URL url = new URL(pURL);
            Log.e(TAG,"url -> " + url.toString());
            conn = (HttpsURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", pToken);
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

            try {
                if (vRespuesta == "" || vRespuesta == null) {
                    vRespuesta = "{ \"httpCode\": -1 }";
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

}
