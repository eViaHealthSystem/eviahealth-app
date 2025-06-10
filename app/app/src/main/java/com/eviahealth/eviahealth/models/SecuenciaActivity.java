package com.eviahealth.eviahealth.models;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.eviahealth.eviahealth.ui.inicio.SubirDatos;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SecuenciaActivity {

    final String TAG = "SecuenciaActivity";

    // SINGLETON
    private static SecuenciaActivity instance = null;

    public static SecuenciaActivity getInstance() {
        if(instance == null) {
            instance = new SecuenciaActivity();
        }
        return instance;
    }

    private List<Class> lista;
    private List<String> extra;
    private int index;

    private SecuenciaActivity() {
        this.clear();
    }

    public void clear() {
        this.lista = new ArrayList<Class>();
        this.extra = new ArrayList<>();
        this.index = 0;
    }

    public void addActivity(Class c, String extra) {
        this.lista.add(c);
        this.extra.add(extra);
    }

    public void next(Activity ctx) {

        Boolean busca = false;

        while (busca == false) {
            if(index < this.lista.size()) {
                Class nextclass = this.lista.get(index);
                String nextra = this.extra.get(index);
                index++;

                try {
                    JSONObject jsonObject = new JSONObject(nextra);
                    if (jsonObject.has("dayOfweek")) {
                        JSONArray jsonArray = jsonObject.getJSONArray("dayOfweek");

                        List<String> dayOfweek = new ArrayList<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            dayOfweek.add(jsonArray.getString(i));
                        }

                        String diaDeSemana = util.getCurrentDayOfWeek();

                        Log.e(TAG, "dayOfweek: " + dayOfweek.toString());
                        Log.e(TAG, "Hoy: " + diaDeSemana);

                        if (dayOfweek.contains(diaDeSemana)) {
                            Log.e(TAG, "----------- SI");
                            busca = true;
                        }
                        else {
                            Log.e(TAG, "----------- NO: " + nextclass.toString());
                        }
                    }
                    else {
                        // NO tiene campo 'dayOfweek' establecido, se consideran que debe hacerse todo los días
                        // "{}"
                        Log.e(TAG, "----------- EXTRA: {}");
                        busca = true;
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG,"JSONException: " + e);
                    // Extra no se puede convertir en un JSON, se consideran que debe hacerse todo los días
                    busca = true;
                }

                if (busca) {
                    EVLog.log(TAG, "Current activity: " + ctx.toString() + ", Next activity: " + nextclass.toString());
                    Intent intent = new Intent(ctx, nextclass);
                    ctx.startActivity(intent);
                    ctx.finish();
                }
            }
            else {
                // se ha acabado el ensayo, ir a subir datos.
                busca = true;
                EVLog.log(TAG,"Current activity: " + ctx.toString() + ", Next activity: SubirDatos.class");
                EnsayoLog.log("FIN","API","Finalización de Ensayo");
                index = 0;
                Intent intent = new Intent(ctx, SubirDatos.class);
                ctx.startActivity(intent);
                ctx.finish();
            }
        }
    }

    @Override
    public String toString() {
        String res = "SecuenciaActivity: ";
        for(int i = 0; i < this.lista.size(); i++) {
            res += this.lista.get(i).getSimpleName() + ", ";
        }
        return res;
    }

    public List getlist(){
        return lista;
    }
}
