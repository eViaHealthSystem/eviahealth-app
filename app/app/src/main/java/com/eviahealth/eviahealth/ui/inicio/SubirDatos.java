package com.eviahealth.eviahealth.ui.inicio;

import android.annotation.SuppressLint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eviahealth.eviahealth.api.BackendConector.ApiConnector;
import com.eviahealth.eviahealth.api.BackendConector.ApiException;
import com.eviahealth.eviahealth.api.BackendConector.ApiUrl;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FileFuntions;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.api.alivecor.MethodsAlivecor;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.models.devices.Dispositivo;
import com.eviahealth.eviahealth.models.devices.EquipoPaciente;
import com.eviahealth.eviahealth.devices.NombresDispositivo;
import com.eviahealth.eviahealth.utils.Fecha.Fecha;
import com.eviahealth.eviahealth.utils.PermissionUtils;
import com.eviahealth.eviahealth.utils.log.EVLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SubirDatos extends BaseActivity {

    final String TAG = "SUBIR_DATOS";
    // -- elementos interfaz --
    ProgressBar progress_bar, progress_bar_sec;
    TextView text_progress, text_info;
    // ------------------------
    String idpaciente;
    Thread hilo;

    Map<NombresDispositivo, Dispositivo> MAP_DISP_ID;
    Map<Integer, NombresDispositivo> MAP_ID_DISP;

    int progreso;
    static JSONObject response;
    static Iterator<String> post;

    static {
        try {
            response = new JSONObject(String.valueOf(FileAccess.leerJSON((FilePath.CONFIG_DISPOSITIVOS))));
            post = response.keys();
            while (post.hasNext()) {
                Log.e("******", "post: " + post.next());
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    static int number = response.length();

    private final static int NUM_FILES_UPLOAD_CARPETA = 12; // cuando se añade la descarga de un nuevo device añadir 1 más
    private int tope = 100;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subirdatos);
        EVLog.log(TAG, "onCreate()");

        PermissionUtils.requestAll(this);

        try {
            Log.e(TAG,"Download API Dispositivos Paciente");

            // Genera un nuevo fichero con la configuración actual de la db para este paciente
            getRemoteData();
        }
        catch (IOException | ApiException | JSONException e) {
            e.printStackTrace();
        }

        //region :: Views
        this.progress_bar = findViewById(R.id.activity_subirdatos_prograssbar);
        this.progress_bar.setMax(100);
        this.progress_bar.setProgress(0);

        this.progress_bar_sec = findViewById(R.id.activity_subirdatos_prograssbar_sec);
        this.progress_bar_sec.setMax(100);
        this.progress_bar_sec.setVisibility(View.INVISIBLE);

        this.text_progress = findViewById(R.id.activity_subirdatos_txt_porcentaje);
        this.text_progress.setText("");
        this.text_info = findViewById(R.id.activity_subirdatos_txt_info);
        this.text_info.setText("");
        //endregion

//        this.idpaciente = Config.getInstance().getIdPacienteTablet();
        this.idpaciente = Config.getInstance().getIdPacienteEnsayo();

        // genera un hilo para subir los datos
        hilo = new Thread() {
            @Override
            public void run() {
                subirTodo();
            }
        };
        hilo.start();

    }

    private void setText(final TextView text_view, final String text) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text_view.setText(text);
            }
        });
    }

    private void setProgressBar(final ProgressBar bar, final int progress) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bar.setProgress(progress);
            }
        });
    }
    private void setCurrentProgress() {

        float m = 100f / (float)tope;
        float y = m * this.progreso;
        int int_progress = (int) y;
        if (int_progress > 100) int_progress = 100;

        Log.e(TAG,"setCurrentProgress() --->> PROGRESO: " + this.progreso + ", int_progress: " + int_progress);
        this.setText(this.text_progress, int_progress + "%");
        this.setProgressBar(this.progress_bar, int_progress);

        this.progreso += 1;
    }
    private void setCurrentProgressBarSec(int progress) {
//        Log.v("SEC PROGRESS BAR", "PR: " + progress);
        int progress_sec = progress;
        if (progress > 100) progress_sec = 100;
        this.setProgressBar(this.progress_bar_sec, progress_sec);
    }
    private void setProgressBarSecVisibility(final int visibility) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progress_bar_sec.setVisibility(visibility);
                progress_bar_sec.setProgress(0);
            }
        });
    }

    private boolean isNetDisponible() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo actNetInfo = connectivityManager.getActiveNetworkInfo();

        return (actNetInfo != null && actNetInfo.isConnected());
    }

    public Boolean isOnlineNet() {

        try {
            Process p = java.lang.Runtime.getRuntime().exec("ping -c 1 www.google.es");

            int val = p.waitFor();
            boolean reachable = (val == 0);
            return reachable;

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Maneja la barra de progreso principal y los textos. Ordena subir cada tipo de registros.
     * La barra de progreso secundaria se maneja dentro de cada metodo SubirDatos si es necesario
     * (primero la tienen que hacer visible y al acabar, invisible)
     */
    private void subirTodo() {

        try {
            File[] carpetas_ensayo = FileAccess.getFileCarpetaEnsayo();
            Log.e(TAG,"carpetas_ensayo length: " + carpetas_ensayo.length);

            // Nº ficheros logs general + (número de dispositivos * carpetas ensayos)
            tope = getFilesCount() + (NUM_FILES_UPLOAD_CARPETA * carpetas_ensayo.length);

            Log.e(TAG,"tope: " + tope);
            this.progreso = 0;

            for(File carpeta_ensayo: carpetas_ensayo) {
                EVLog.log(TAG,"***** Subir carpeta: " + carpeta_ensayo.toString());
                subirCarpetaEnsayo(carpeta_ensayo);
            }

            //region :: LOGS
            EVLog.log(TAG,"Upload file logs general");
            this.setText(this.text_info, "Ficheros log general ...");

            EVLog.log(TAG,"FILE REGISTROS_LOG_CONFIG(\"ev_config.log\")");
            this.subirLogsConfig();              // FILES REGISTROS_LOG_CONFIG("ev_config.log")

            EVLog.log(TAG,"FILEs iHealth \"_sdk.log\"");
            this.subirLogsSDK();                 // FILES "_sdk.log"

            EVLog.log(TAG,"FILE EVLOG(\"ev_app.log\")");
            this.subirLogs();                   // FILES EVLOG("ev_app.log")
            setCurrentProgress();
            //endregion

            this.setText(this.text_info, "FIN");
            setFilesBackup();

//            Intent intent = new Intent(SubirDatos.this, Inicio.class);
//            startActivity(intent);
//            finish();
        }
        catch (Exception e) {
            e.printStackTrace();
            EVLog.log(TAG, "EXCEPTION >> subirTodo(): " + e.toString());

            if (isNetDisponible() || isOnlineNet()) {
                this.setText(this.text_info, "ERROR");
            } else {
                this.setText(this.text_info, "SIN ACCESO A INTERNET");
            }
            SystemClock.sleep(4000);
//            finish();
        }
        finally {
            finishAffinity();
        }
    }

    /**
     * Sube todos los ficheros de un ensayo
     * @param carpeta_ensayo Carpeta actual de ficheros a subir
     */
    private void subirCarpetaEnsayo(File carpeta_ensayo){
        try {

            this.setCurrentProgress();

            EVLog.log(TAG,"Registro de ensayo.");
            this.subirEnsayo(carpeta_ensayo);

            //region :: OXIMETRO
            EVLog.log(TAG,"Upload data PULXIOXÍMETRO");
            this.setText(this.text_info, "Subiendo datos pulsioxímetro...");
            this.subirOximetro(carpeta_ensayo);
            this.setCurrentProgress();
            //endregion

            //region :: TENSIOMETRO
            EVLog.log(TAG,"Upload data TENSIOMETRO");
            this.setText(this.text_info, "Subiendo datos tensiómetro...");
            this.subirTensiometro(carpeta_ensayo);
            this.setCurrentProgress();
            //endregion

            //region :: ACTIVIDAD - MAMBO 6
            EVLog.log(TAG,"Upload data STEP & SLEEP");

            EVLog.log(TAG, "MAMBO 6 >> Actividad Total");
            this.setText(this.text_info, "Subiendo datos actividad total...");
            this.subirActividadTotal_MAMBO6(carpeta_ensayo);
            this.setCurrentProgress();

//                EVLog.log(TAG, "MAMBO 6 >> Actividad Diaria");
//                this.setText(this.text_info, "Subiendo datos actividad diaria...");
            this.subirActividadDiaria_MAMBO6(carpeta_ensayo);

            EVLog.log(TAG, "MAMBO 6 >> Actividad Diaria Bloque");
            this.setText(this.text_info, "Subiendo datos actividad diaria...");
            this.subirDataSteps_MAMBO6(carpeta_ensayo);

//                EVLog.log(TAG, "MAMBO 6 >> Sueño");
//                this.setText(this.text_info, "Subiendo datos sueño...");
            this.subirSuenyo_MAMBO6(carpeta_ensayo);

            EVLog.log(TAG, "MAMBO 6 >> Sueño Bloque");
            this.setText(this.text_info, "Subiendo datos sueño...");
            this.subirDataSleep_MAMBO6(carpeta_ensayo);

            EVLog.log(TAG, "MAMBO 6 >> Heart Rate");
            this.setText(this.text_info, "Subiendo datos de Frecuencia Cardíaca...");
            this.subirHeartRate_MAMBO6(carpeta_ensayo);

            EVLog.log(TAG, "MAMBO 6 >> Blood Oxygen");
            this.setText(this.text_info, "Subiendo datos de Oxígeno en sangre...");
            this.subirBloodOxygen_MAMBO6(carpeta_ensayo);
            //endregion

            //region :: TERMOMETRO
            EVLog.log(TAG,"Upload data TEMPERATURA");
            this.setText(this.text_info, "Subiendo datos termómetro...");
            this.subirTermometro(carpeta_ensayo);
            this.setCurrentProgress();
            //endregion

            //region :: PEAK FLOW
            EVLog.log(TAG,"Upload data PEAKFLOW");
            this.setText(this.text_info, "Subiendo datos peakflow...");
            this.subirPeakFlow(carpeta_ensayo);
            this.setCurrentProgress();
            //endregion

            //region :: BÁSCULA
            EVLog.log(TAG,"Upload data SCALE");
            this.setText(this.text_info, "Subiendo datos de báscula...");
            this.subirDataScale(carpeta_ensayo);
            this.setCurrentProgress();
            //endregion

            //region :: MONITOR PULMONAR
            EVLog.log(TAG,"Upload data LUNG");
            this.setText(this.text_info, "Subiendo datos del Monitor Pulmonar ...");
            this.subirDataMonitorPulmonar(carpeta_ensayo);
            this.setCurrentProgress();
            //endregion

            //region :: ENCUESTAS Y CAT
            EVLog.log(TAG,"Upload data SUVRVEYS");
            this.setText(this.text_info, "Subiendo datos encuesta...");
            this.subirDataSurveys(carpeta_ensayo);
            this.setCurrentProgress();

            EVLog.log(TAG,"Upload data CAT");
            this.setText(this.text_info, "Subiendo datos CAT...");
            this.subirCat(carpeta_ensayo);
            this.setCurrentProgress();
            //endregion

            //region :: ECG
            EVLog.log(TAG,"Upload data ECG");
            this.setText(this.text_info, "Subiendo datos del ECG ...");
            this.subirDataECG(carpeta_ensayo);
            this.setCurrentProgress();
            //endregion


            EVLog.log(TAG,"Set procesar incidencias.");
            this.setText(this.text_info, "Incidencias...");
            this.subirIncidencias();

            //region :: LOGS
            EVLog.log(TAG,"Upload file logs test");
            this.setText(this.text_info, "Ficheros log test ...");

            EVLog.log(TAG,"FILE LOGS TO DB");
            this.subirLogEnsayo(carpeta_ensayo); // DB
            setCurrentProgress();
            //endregion

            /*
            Elimina el archivo o directorio indicado por esta ruta abstracta. Si este nombre de ruta
            denota un directorio, entonces el directorio debe estar vacío para poder ser eliminado.
             */
            Boolean st = carpeta_ensayo.delete();
//            Boolean st = deleteDirectory(carpeta_ensayo);   // elimina directorio aunque no esté vacio
            EVLog.log(TAG,"carpeta_ensayo.delete(): " + st);

            this.setText(this.text_info, "FIN");
        }
        catch (IOException | JSONException | ApiException e) {
            e.printStackTrace();
            EVLog.log(TAG, "EXCEPTION >> subirCarpetaEnsayo(): " + e.toString());

            if (isNetDisponible() || isOnlineNet()) {
                this.setText(this.text_info, "ERROR");
            } else {
                this.setText(this.text_info, "SIN ACCESO A INTERNET");
            }
        }
    }

    private int getFilesCount() {
        int count = 0;
        File[] files = FileAccess.getFileRegistros(FileAccess.getPATH(), FilePath.REGISTROS_LOG_CONFIG);
        count += files.length;
        Log.e(TAG,"files LOG_CONFIG: " + files.length);
//        for(File file: files) {
//            Log.e(TAG, "name file: " + file.getName());
//        }

        files = FileAccess.getFilesSDKLog();
        count += files.length;
        Log.e(TAG,"files SDK_LOGS: " + files.length);
//        for(File file: files) {
//            Log.e(TAG, "name file: " + file.getName());
//        }

        files = FileAccess.getFileRegistros(FileAccess.getPATH(), FilePath.EVLOG);
        count += files.length;
        Log.e(TAG,"files EVLOG: " + files.length);
//        for(File file: files) {
//            Log.e(TAG, "name file: " + file.getName());
//        }

        Log.e(TAG,"getFilesCount(): " + count);
        return count;
    }
    private Boolean deleteDirectory(File f){

        if(f.isDirectory()){
            for(File f1 : f.listFiles()){
                if (f1.delete() == false) return false;
            }
        }
        return f.delete();
    }

    private void subirEnsayo(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTRO_ENSAYO);
        if(jsons.length > 0) {
            String fecha = jsons[0].getString("fecha");
            // se añade para que envíe idpaciente que está dentro del fichero de xxx_enayo.json
            if (jsons[0].has("idpaciente")) {
                this.idpaciente = jsons[0].getString("idpaciente");
                Log.e(TAG,"idpaciente desde fichero ensayo.json: " + this.idpaciente);
            }
            // ---------------------------------------------------------------------------------

            JSONObject params = new JSONObject();
            params.put("fecha", fecha);
            params.put("idpaciente", this.idpaciente);
            EVLog.log(TAG,"Ensayo (" + ApiUrl.CREAR_ENSAYO + "): " + params.toString());
            ApiConnector.peticion(ApiUrl.CREAR_ENSAYO, params);
        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTRO_ENSAYO);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo, FilePath.REGISTRO_ENSAYO);
    }

    private void subirLogEnsayo(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        File[] files = FileAccess.getFileRegistros(carpeta_ensayo, FilePath.REGISTROS_LOG);
        for(File file: files) {
            String contenido = FileAccess.leer(file);
            JSONObject params = new JSONObject();
            params.put("idpaciente", this.idpaciente);
            params.put("contenido", contenido);
            EVLog.log(TAG,"Log ENSAYO (" + ApiUrl.SUBIR_LOG_ENSAYO + "): " + params.toString());
            ApiConnector.peticion(ApiUrl.SUBIR_LOG_ENSAYO, params);
        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_LOG);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo, FilePath.REGISTROS_LOG);
    }

    private void subirTermometro(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_TERMOMETRO);
        int id_dispositivo = Config.getInstance().getDispositivos().get(NombresDispositivo.TERMOMETRO).getId();
        for(JSONObject json: jsons) {
            JSONObject params = new JSONObject();
            params.put("idpaciente", this.idpaciente);
            params.put("identificadorpaciente", this.idpaciente);
            params.put("dispositivo", id_dispositivo);
            params.put("temperatura", json.getDouble("temperatura"));
            params.put("fecha", json.getString("fecha"));
            EVLog.log(TAG,"Termómetro (" + ApiUrl.SUBIR_REGISTRO_TERMOMETRO + "): " + params.toString());
            ApiConnector.peticion(ApiUrl.SUBIR_REGISTRO_TERMOMETRO, params);
        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_TERMOMETRO);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo, FilePath.REGISTROS_TERMOMETRO);
    }

    private void subirPeakFlow(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_PEAKFLOW);
        int id_dispositivo = Config.getInstance().getDispositivos().get(NombresDispositivo.PEAKFLOW).getId();
        for(JSONObject json: jsons) {
            JSONObject params = new JSONObject();
            params.put("idpaciente", this.idpaciente);
            params.put("identificadorpaciente", this.idpaciente);
            params.put("dispositivo", id_dispositivo);
            params.put("flujo_resp", json.getInt("rango"));
            params.put("fecha", json.getString("fecha"));
            EVLog.log(TAG,"PeakFlow (" + ApiUrl.SUBIR_REGISTRO_PEAKFLOW + "): " + params.toString());
            ApiConnector.peticion(ApiUrl.SUBIR_REGISTRO_PEAKFLOW, params);
        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_PEAKFLOW);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo, FilePath.REGISTROS_PEAKFLOW);
    }

    //region :: ENCUESTAS
    private Integer getSurveyNumber(){

        Integer nEncuesta = 0;
        try {
            JSONObject jsonDispositivos = FileAccess.leerJSON(FilePath.CONFIG_DISPOSITIVOS);
            JSONObject json = jsonDispositivos.getJSONObject("Encuesta");

            Log.e(TAG,"jsonEncuesta: " + json.toString());

            String desc = json.getString("desc");
            if (desc.equals("Deshabilitado") == false) {
                nEncuesta = json.getInt("desc");
                Log.e(TAG,"getSurveyNumber(): " + nEncuesta);
            }
        }
        catch (IOException | JSONException e) {
            // no existe fichero configuracion
            EVLog.log(TAG, "EXCEPTION >> IOException o JSONException (dispositivos.json)");
            e.printStackTrace();
            return 0;
        }
        return nEncuesta;
    }

    private void subirDataSurveys(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_ENCUESTA);
        int id_dispositivo = Config.getInstance().getDispositivos().get(NombresDispositivo.ENCUESTA).getId();

        for(JSONObject json: jsons) {
            Log.e(TAG,"JSON SURVEYS: " + json.toString());
        }

        // Obtiene el número de la encuesta que se ha hecho el ensayo
        Integer nEncuesta = getSurveyNumber();

        for(JSONObject json: jsons) {

            JSONObject bloque = new JSONObject();
            bloque.put("idpaciente", this.idpaciente);
            bloque.put("identificadorpaciente", this.idpaciente);
            bloque.put("dispositivo", id_dispositivo);
            bloque.put("nEncuesta", nEncuesta);
            bloque.put("fecha",Fecha.getFechaYHoraActual());
            JSONArray data = new JSONArray();

            JSONArray array = json.getJSONArray("resultado_encuesta");
            int length = array.length();
            for(int i = 0; i < length; i++) {
                JSONObject resultados_pregunta = array.getJSONObject(i);
                data.put(new JSONObject().put("id_pregunta", resultados_pregunta.getInt("id_pregunta")).put("valor_respuesta", resultados_pregunta.getInt("valor_respuesta")));
            }
            bloque.put("data", data);

            EVLog.log(TAG,"SURVEYS: (" + ApiUrl.SUBIR_SURVEYS + "): " + bloque.toString());
            ApiConnector.peticion(ApiUrl.SUBIR_SURVEYS, bloque);
        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_ENCUESTA);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo, FilePath.REGISTROS_ENCUESTA);
    }

    private void subirCat(File carpeta_ensayo) throws JSONException, IOException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_CAT);
        int id_dispositivo = Config.getInstance().getDispositivos().get(NombresDispositivo.CAT).getId();
        for(JSONObject json: jsons) {
            JSONArray array = json.getJSONArray("resultado_cat");
            int length = array.length();
            for(int i = 0; i < length; i++) {
                JSONObject resultados_pregunta = array.getJSONObject(i);
                JSONObject params = new JSONObject();
                params.put("idpaciente", this.idpaciente);
                params.put("identificadorpaciente", this.idpaciente);
                params.put("dispositivo", id_dispositivo);
                params.put("fecha", resultados_pregunta.getString("fecha"));
                params.put("id_cat", resultados_pregunta.getInt("id_cat"));
                params.put("valor_cat", resultados_pregunta.getInt("suma"));
                EVLog.log(TAG,"CAT (" + ApiUrl.SUBIR_REGISTRO_CAT + "): " + params.toString());
                ApiConnector.peticion(ApiUrl.SUBIR_REGISTRO_CAT, params);
            }
        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_CAT);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo, FilePath.REGISTROS_CAT);
    }
    //endregion

    //region :: AM4
    private void subirActividadTotal(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_TOTAL_AM4);
        int id_dispositivo = Config.getInstance().getDispositivos().get(NombresDispositivo.ACTIVIDAD).getId();
        for(JSONObject json: jsons) {
            JSONObject params = new JSONObject();
            params.put("idpaciente", this.idpaciente);
            params.put("identificadorpaciente", this.idpaciente);
            params.put("dispositivo", id_dispositivo);
            params.put("fecha", json.getString("fecha"));
            params.put("step", json.getInt("step"));
            params.put("calorie", json.getInt("calorie"));
            // en el fichero hay un campo "totalcalorie", alberto no lo usaba
            ApiConnector.peticion(ApiUrl.SUBIR_REGISTRO_ACTIVIDAD, params);
        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_TOTAL_AM4);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_TOTAL_AM4);
    }

    private void subirActividadDiaria(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_DIARIA_AM4);
        int id_dispositivo = Config.getInstance().getDispositivos().get(NombresDispositivo.ACTIVIDAD).getId() + 1;
        this.setProgressBarSecVisibility(View.VISIBLE);
        for(int k = 0; k < jsons.length; k ++) {
            JSONObject json = jsons[k];
            // loop cada archivo
            JSONArray array = json.getJSONArray("activity");
            int array_length = array.length();
            int progress_archivos = k * 100 / jsons.length;
            int progress_archivos_slot = 100 / jsons.length;
            for(int i = 0; i < array_length; i++) {
                JSONObject each_data = array.getJSONObject(i);
                JSONArray each_data_array = each_data.getJSONArray("activity_each_data");
                int each_data_array_length = each_data_array.length();

                int pasos_anterior = 0;
                int calorias_anterior = 0;
                int progress_array = i * progress_archivos_slot / array_length;
                int progress_array_slot = progress_archivos_slot / array_length;
                for(int j = 0; j < each_data_array_length; j++) {
                    JSONObject dato_actividad = each_data_array.getJSONObject(j);

                    int pasos_actual = dato_actividad.getInt("step");
                    int calorias_actual = dato_actividad.getInt("calorie");
                    int pasos_restado = pasos_actual - pasos_anterior;
                    int calorias_restado = calorias_actual - calorias_anterior;
                    pasos_anterior = pasos_actual;
                    calorias_anterior = calorias_actual;

                    if (pasos_restado >= 0) {
                        JSONObject params = new JSONObject();
                        params.put("idpaciente", this.idpaciente);
                        params.put("identificadorpaciente", this.idpaciente);
                        params.put("dispositivo", id_dispositivo);
                        params.put("fecha", dato_actividad.getString("time"));
                        params.put("step", pasos_restado);
                        params.put("calorie", calorias_restado);
                        if (j != 0)
                            ApiConnector.peticion(ApiUrl.SUBIR_REGISTRO_ACTIVIDAD, params);
                    }

                    int progress_data = j * progress_array_slot / each_data_array_length;
                    this.setCurrentProgressBarSec(progress_data + progress_array + progress_archivos);
                }
            }
        }
        this.setProgressBarSecVisibility(View.INVISIBLE);

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_DIARIA_AM4);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo,FilePath.REGISTROS_ACTIVIDAD_DIARIA_AM4);
    }

    private void subirSuenyo(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_SLEEP_AM4);
        int id_dispositivo = Config.getInstance().getDispositivos().get(NombresDispositivo.ACTIVIDAD).getId() + 2;
        this.setProgressBarSecVisibility(View.VISIBLE);
        for(int k = 0; k < jsons.length; k ++) {
            JSONObject json = jsons[k];
            // loop cada archivo
            JSONArray array = json.getJSONArray("sleep");
            int array_length = array.length();
            int progress_archivos = k * 100 / jsons.length;
            int progress_archivos_slot = 100 / jsons.length;
            for(int i = 0; i < array_length; i++) {
                JSONObject each_data = array.getJSONObject(i);
                JSONArray each_data_array = each_data.getJSONArray("sleep_each_data");
                int each_data_array_length = each_data_array.length();
                int progress_array = i * progress_archivos_slot / array_length;
                int progress_array_slot = progress_archivos_slot / array_length;
                for(int j = 0; j < each_data_array_length; j++) {
                    JSONObject dato_suenyo = each_data_array.getJSONObject(j);
                    JSONObject params = new JSONObject();
                    params.put("idpaciente", this.idpaciente);
                    params.put("identificadorpaciente", this.idpaciente);
                    params.put("dispositivo", id_dispositivo);
                    params.put("fecha", dato_suenyo.getString("time"));
                    params.put("nivel", dato_suenyo.getInt("level"));
                    ApiConnector.peticion(ApiUrl.SUBIR_REGISTRO_SUENYO, params);

                    int progress_data = j * progress_array_slot / each_data_array_length;
                    this.setCurrentProgressBarSec(progress_data + progress_array + progress_archivos);
                }
            }
        }
        this.setProgressBarSecVisibility(View.INVISIBLE);

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_SLEEP_AM4);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo,FilePath.REGISTROS_ACTIVIDAD_SLEEP_AM4);
    }
    //endregion

    //region :: MAMBO6
    private void subirActividadTotal_MAMBO6(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_TOTAL_MAMBO6);

        if (jsons.length <= 0) {
            Log.e(TAG,"MAMBO6: NO EXISTE DATOS ACTIVIDAD TOTAL");
        }

//        for (JSONObject i: jsons) {
//            // {"fecha":"2023-02-02 13:32:02","step":2339,"calorie":70,"totalcalories":70}
//            Log.e(TAG,"MAMBO6: " + i.toString());
//        }

//        int id_dispositivo = Config.getInstance().getDispositivos().get(NombresDispositivo.ACTIVIDAD).getId();
        int id_dispositivo = 3; // Para que sea compatible en la web como la AM4
        for(JSONObject json: jsons) {
            JSONObject params = new JSONObject();
            params.put("idpaciente", this.idpaciente);
            params.put("identificadorpaciente", this.idpaciente);
            params.put("dispositivo", id_dispositivo);
            params.put("fecha", json.getString("fecha"));
            params.put("step", json.getInt("step"));
            params.put("calorie", json.getInt("calorie"));
            EVLog.log(TAG,"Actividad Total (" + ApiUrl.SUBIR_REGISTRO_ACTIVIDAD + "): " + params.toString());
            ApiConnector.peticion(ApiUrl.SUBIR_REGISTRO_ACTIVIDAD, params);
        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_TOTAL_MAMBO6);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_TOTAL_MAMBO6);
    }

    // stepsOfHour
    private void subirActividadDiaria_MAMBO6(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_DIARIA_MAMBO6);

        if (jsons.length <= 0) {
            Log.e(TAG,"MAMBO6: NO EXISTE DATOS ACTIVIDAD DIARIA");
        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_DIARIA_MAMBO6);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo,FilePath.REGISTROS_ACTIVIDAD_DIARIA_MAMBO6);
    }

    private void subirDataSteps_MAMBO6(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_DATA_STEPS_MAMBO6);
        if (jsons.length <= 0) {
            Log.e(TAG,"MAMBO6: NO EXISTE DATOS ACTIVIDAD DIARIA(BLOQUES)");
        }
        for(JSONObject params: jsons) {
//            Log.e(TAG,"MAMBO6 STEPS BLOCK: " + params.toString());
            params.put("idpaciente", this.idpaciente);
            EVLog.log(TAG,"Actividad Steps Block (" + ApiUrl.SUBIR_STEPS + "): " + params.toString());
            ApiConnector.peticion(ApiUrl.SUBIR_STEPS, params);
        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_DATA_STEPS_MAMBO6);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo,FilePath.REGISTROS_ACTIVIDAD_DATA_STEPS_MAMBO6);
    }

    private void subirSuenyo_MAMBO6(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_SLEEP_MAMBO6);
        if (jsons.length <= 0) {
            Log.e(TAG,"MAMBO6: NO EXISTE DATOS DE SUEÑO");
        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_SLEEP_MAMBO6);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo,FilePath.REGISTROS_ACTIVIDAD_SLEEP_MAMBO6);
    }

    private void subirDataSleep_MAMBO6(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_DATA_SLEEP_MAMBO6);
        if (jsons.length <= 0) {
            Log.e(TAG,"MAMBO6: NO EXISTE DATOS DE SUEÑO (BLOQUES)");
        }
        for(JSONObject params: jsons) {
//            Log.e(TAG,"MAMBO6 SLEEP BLOCK: " + params.toString());
            params.put("idpaciente", this.idpaciente);
            EVLog.log(TAG,"Actividad Sleep Block (" + ApiUrl.SUBIR_SLEEP + "): " + params.toString());
            ApiConnector.peticion(ApiUrl.SUBIR_SLEEP, params);
        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_DATA_SLEEP_MAMBO6);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo,FilePath.REGISTROS_ACTIVIDAD_DATA_SLEEP_MAMBO6);
    }

    private void subirHeartRate_MAMBO6(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_HEART_RATE_MAMBO6);
        if (jsons.length <= 0) {
            Log.e(TAG,"MAMBO6: NO EXISTE DATOS HEART RATE");
        }

        // Pendiente de adaptar los datos para que se envien como arrays de measure
//        for(JSONObject params: jsons) {
//            params.put("idpaciente", this.idpaciente);
//            ApiConnector.peticion(ApiUrl.SUBIR_REGISTRO_MEASSURE, params);
//        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_HEART_RATE_MAMBO6);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo,FilePath.REGISTROS_ACTIVIDAD_HEART_RATE_MAMBO6);
    }

    private void subirBloodOxygen_MAMBO6(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_BLODD_OXYGEN_MAMBO6);
        if (jsons.length <= 0) {
            Log.e(TAG,"MAMBO6: NO EXISTE DATOS BLOOD OXYGEN");
        }
        // Pendiente de adaptar los datos para que se envien como arrays de measure
//        for(JSONObject params: jsons) {
//            params.put("idpaciente", this.idpaciente);
//            ApiConnector.peticion(ApiUrl.SUBIR_REGISTRO_MEASSURE, params);
//        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_ACTIVIDAD_BLODD_OXYGEN_MAMBO6);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo,FilePath.REGISTROS_ACTIVIDAD_BLODD_OXYGEN_MAMBO6);
    }

    //endregion

    private void subirDataScale(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_BASCULA);
        if (jsons.length <= 0) {
            Log.e(TAG,"SCALE: NO EXISTE DATOS DE BÁSCULA");
        }
        for(JSONObject params: jsons) {
//            Log.e(TAG,"MAMBO6 STEPS BLOCK: " + params.toString());
            EVLog.log(TAG,"Medidas báscula (" + ApiUrl.SUBIR_REGISTRO_MEASSURE + "): " + params.toString());
            ApiConnector.peticion(ApiUrl.SUBIR_REGISTRO_MEASSURE, params);
        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_BASCULA);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo,FilePath.REGISTROS_BASCULA);
    }

    private void subirTensiometro(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_TENSIOMETRO);
        int id_dispositivo = Config.getInstance().getDispositivos().get(NombresDispositivo.TENSIOMETRO).getId();
        if (jsons.length <= 0) {
            Log.e(TAG,"DESCARGA: NO EXISTE DATOS TENSIÓMETRO");
        }
        for(JSONObject json: jsons) {
            JSONObject params = new JSONObject();
            params.put("idpaciente", this.idpaciente);
            params.put("identificadorpaciente", this.idpaciente);
            params.put("dispositivo", id_dispositivo);
            params.put("fecha", json.getString("fecha"));
            params.put("sys", json.getInt("sys"));
            params.put("dia", json.getInt("dia"));
            params.put("heartrate_ten", json.getInt("heartRate"));
            EVLog.log(TAG,"Tensiómetro (" + ApiUrl.SUBIR_REGISTRO_TENSIOMETRO + "): " + params.toString());
            ApiConnector.peticion(ApiUrl.SUBIR_REGISTRO_TENSIOMETRO, params);
        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_TENSIOMETRO);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo, FilePath.REGISTROS_TENSIOMETRO);
    }

    private void subirOximetro(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo,FilePath.REGISTROS_OXIMETRO);
        int id_dispositivo = Config.getInstance().getDispositivos().get(NombresDispositivo.OXIMETRO).getId();
        if (jsons.length <= 0) {
            Log.e(TAG,"DESCARGA: NO EXISTE DATOS OXÍMETRO");
        }
        for(JSONObject json: jsons) {
            JSONObject params = new JSONObject();
            params.put("idpaciente", this.idpaciente);
            params.put("identificadorpaciente", this.idpaciente);
            params.put("dispositivo", id_dispositivo);
            params.put("fecha", json.getString("fecha"));
            params.put("bloodoxygen", json.getInt("bloodoxygen"));
            params.put("heartrate", json.getInt("heartrate"));
            EVLog.log(TAG,"Oximetro (" + ApiUrl.SUBIR_REGISTRO_OXIMETRO + "): " + params.toString());
            ApiConnector.peticion(ApiUrl.SUBIR_REGISTRO_OXIMETRO, params);
        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_OXIMETRO);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo,FilePath.REGISTROS_OXIMETRO);
    }

    private void subirDataMonitorPulmonar(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_MONITOR_PULMONAR);
        if (jsons.length <= 0) {
            Log.e(TAG,"LUNG: NO EXISTE DATOS");
        }
        for(JSONObject params: jsons) {
            EVLog.log(TAG,"Medida monitor pulmonar (" + ApiUrl.SUBIR_REGISTRO_MEASSURE + "): " + params.toString());
            ApiConnector.peticion(ApiUrl.SUBIR_REGISTRO_MEASSURE, params);
        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_MONITOR_PULMONAR);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo,FilePath.REGISTROS_MONITOR_PULMONAR);
    }

    private void subirDataECG(File carpeta_ensayo) throws IOException, JSONException, ApiException {
        JSONObject[] jsons = FileAccess.leerFicherosRegistros(carpeta_ensayo, FilePath.REGISTROS_ECG);
        if (jsons.length <= 0) {
            Log.e(TAG,"ECG: NO EXISTE DATOS");
        }
        for(JSONObject params: jsons) {
            EVLog.log(TAG,"Recording ECG (" + ApiUrl.SUBIR_REGISTRO_MEASSURE + "): " + params.toString());
            ApiConnector.peticion(ApiUrl.SUBIR_REGISTRO_MEASSURE, params);

            String namePdf = MethodsAlivecor.getNamePdf(params.toString());
            enviarPdfKardia(namePdf);
        }

        FileAccess.deleteFile(carpeta_ensayo, FilePath.REGISTROS_ECG);
//        FileMoveUtil.mover_a_subido(carpeta_ensayo,FilePath.REGISTROS_ECG);
    }

    private void subirIncidencias() throws JSONException, IOException, ApiException {
        JSONObject params_incidencia = new JSONObject();
        params_incidencia.put("idpaciente", this.idpaciente);
        EVLog.log(TAG,"Incidencias (" + ApiUrl.PROCESAR_INCIDENCIAS + "): " + params_incidencia.toString());
        ApiConnector.peticion(ApiUrl.PROCESAR_INCIDENCIAS, params_incidencia);
    }

    private void subirLogs() throws IOException, ApiException {
        File[] files = FileAccess.getFileRegistros(FileAccess.getPATH(), FilePath.EVLOG);
        subirLogsTroceado(files);

        FileAccess.deleteFile(files);
//        FileMoveUtil.mover_a_subido(FileAccess.getPATH(), FilePath.EVLOG);
    }

    private void subirLogsConfig() throws IOException, ApiException {
        File[] files = FileAccess.getFileRegistros(FileAccess.getPATH(), FilePath.REGISTROS_LOG_CONFIG);
        subirLogsTroceado(files);

        FileAccess.deleteFile(files);
//        FileMoveUtil.mover_a_subido(FileAccess.getPATH(), FilePath.REGISTROS_LOG_CONFIG);
    }

    private void subirLogsSDK() throws IOException, ApiException {
        File[] files = FileAccess.getFilesSDKLog();
        subirLogsTroceado(files);

        FileAccess.deleteFile(files);
//        FileMoveUtil.mover_a_subido(files);
    }

    private void subirLogsTroceado(File[] files) throws IOException, ApiException {
        for (File file : files) {
            Log.e(TAG, "name file: " + file.getName());
            try (FileInputStream fis = new FileInputStream(file)) {
                // nginx buffer defecto 1MB
//                byte[] buffer = new byte[1024 * 1024]; // 1MB buffer nginx lo corta todavía
                byte[] buffer = new byte[1000 * 1024]; // <1MB buffer para evitar que el proxy te corte.
                int bytesRead;
                int partNumber = 0;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    String partFileName = file.getName();
                    Log.e(TAG, "partFileName: " + file.getName() + ".part" + partNumber + " bytes" + bytesRead);
                    partNumber++;
                    try (Scanner sc = new Scanner(new ByteArrayInputStream(buffer, 0, bytesRead))) {
//                        ApiConnector.subirLog(this.idpaciente, partFileName, sc);
                        // logs se envía al paciente de la tablet
                        ApiConnector.subirLog(Config.getInstance().getIdPacienteTablet(), partFileName, sc);
                    }
                }
            }
            setCurrentProgress();
        }
    }

    /**
     * Solicitud api: Obtiene listado de dispositivos de la instalación >> "SELECT id, nombre, params, display FROM device"
     * @throws IOException
     * @throws ApiException
     * @throws JSONException
     */
    private void getRemoteData() throws IOException, ApiException, JSONException {

        JSONObject params = new JSONObject();
        params.put("idpaciente", this.idpaciente);
//        JSONObject respuesta = ApiConnector.peticionJSONObject(ApiUrl.DISPOSITIVOS_GET, new JSONObject());
        JSONObject respuesta = ApiConnector.peticionJSONObject(ApiUrl.DISPOSITIVOS_GET, params);
        JSONArray dispositivos = respuesta.getJSONArray("devices");

        Map<NombresDispositivo, Dispositivo> map = new HashMap<>();
        Map<Integer, NombresDispositivo> id_disp_map = new HashMap<>();

        for(int i = 0; i < dispositivos.length(); i++) {
            JSONObject dispositivo = dispositivos.getJSONObject(i);

            int id = dispositivo.getInt("id");
            String nombre = dispositivo.getString("nombre");
            NombresDispositivo cual = NombresDispositivo.fromName(nombre);

            if(cual != null) {
                map.put(cual, new Dispositivo(id));
                id_disp_map.put(id, cual);
            }
        }

        MAP_DISP_ID = map;              // <NombreDispositivo(enum), Dispositivo(Class)>
        MAP_ID_DISP = id_disp_map;      // <Integer, NombreDispositivo(enum)>

        Log.e(TAG,"Genera el fichero de \"dispositivos.json\" y \"encuesta.json\"");
        escribirFicheros(this.idpaciente, getEquiposPaciente());
    }

    /**
     * Solicitud API: Obtener todos los dispositivos de un paciente >> "FROM EquiposEnsayoPaciente"
     * @return Map<NombresDispositivo, EquipoPaciente>
     * @throws JSONException
     * @throws ApiException
     * @throws IOException
     */
    private Map<NombresDispositivo, EquipoPaciente> getEquiposPaciente() throws JSONException, ApiException, IOException {
        JSONObject params = new JSONObject();
        params.put("idpaciente", this.idpaciente);
        JSONArray dispositivos = ApiConnector.peticionJSONArray(ApiUrl.PACIENTE_EQUIPO_GET, params);

        Map<NombresDispositivo, EquipoPaciente> equipos_paciente = new HashMap<>();

        for(int i = 0; i < dispositivos.length(); i++) {
            JSONObject dispositivo = dispositivos.getJSONObject(i);
            int id = dispositivo.getInt("id_device");
            boolean enable = dispositivo.getInt("enable") == 1;
            String mac = dispositivo.getString("desc");
            String extra = dispositivo.getString("extra");

            NombresDispositivo cual = MAP_ID_DISP.get(id);
            if (cual != null) {
                equipos_paciente.put(cual, new EquipoPaciente(new Dispositivo(id), enable, mac, extra));
            }
        }
        return equipos_paciente;
    }

    /**
     * Genera el fichero de "dispositivos.json" y "encuesta.json"
     * @param idpaciente
     * @param equipos_paciente
     * @throws IOException
     * @throws JSONException
     */
    private void escribirFicheros(String idpaciente, Map<NombresDispositivo, EquipoPaciente> equipos_paciente) throws IOException, JSONException {
        JSONObject jsonequipos = new JSONObject();
        for(NombresDispositivo ndisp: equipos_paciente.keySet()) {
            if (ndisp.getNombre() != "ConcentradorO2" && ndisp.getNombre() != "CPAP") {
                EquipoPaciente equipo = equipos_paciente.get(ndisp);

                JSONObject jsoneq = new JSONObject();
                jsoneq.put("id", equipo.getDispositivo().getId());
                jsoneq.put("enable", equipo.isEnabled());
                jsoneq.put("desc", equipo.getDesc());
                jsoneq.put("extra", equipo.getExtra());
                jsonequipos.put(ndisp.getNombre(), jsoneq);

                //region >> WRITE Encuestas
                if (ndisp.getNombre() == "Encuesta" && !equipo.isEnabled()){
                    JSONObject jsonencuesta = new JSONObject();
                    jsoneq.put("desc", "Deshabilitado");
                    jsonencuesta.put("id_encuesta", 0);
                    jsonencuesta.put("arrayencuesta", new JSONArray());
                    FileAccess.escribirJSON(FilePath.CONFIG_ENCUESTA, jsonencuesta);

                    String contenido = FileFuntions.readfile("encuesta.json");
                    Log.e("ENCUESTAS","" + contenido);
                }
                //endregion
            }
        }

//        Log.e(TAG,"dispositivos.json: " + jsonequipos.toString());
        FileAccess.escribirJSON(FilePath.CONFIG_DISPOSITIVOS, jsonequipos);

// COPY       FileAccess.copyFiles(FilePath.CONFIG_DISPOSITIVOS, true);
        String contenido = FileFuntions.readfile("dispositivos.json");
        Log.e("DISPOSITIVOS.JSON","" + contenido);
    }

    @Override
    protected void onDestroy() {
        EVLog.log(TAG, "onDestroy()");
        try {
            //        hilo.interrupt();
            hilo.join(); // Espera a que el hilo termine.
        } catch (InterruptedException e) {
            e.printStackTrace();
            EVLog.log(TAG, "InterruptedException onDestroy(): " + e.toString());
        }

//        Intent intent = new Intent(SubirDatos.this, Inicio.class);
//        startActivity(intent);
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}

    @Override
    protected void onResume() {
        super.onResume();
        EVLog.log(TAG,"onResume()");
    }

    @Override
    protected  void onPause() {
        super.onPause();
        EVLog.log(TAG,"onPause()");
    }

    public void setFilesBackup(){
        Log.e(TAG,"setFilesBackup()");
    }

    //region :: KARDIA
    private void enviarPdfKardia(String namePdf) {

        Log.e(TAG, "enviarPdfKardia()");
        try {
            final File name = new File(FileAccess.getPATH_FILES(), "pdfs/" + namePdf + ".pdf");
            Log.e(TAG,"FILE: " + name.toString());

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<String> future = executor.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return ApiConnector.subirArchivoPDF(ApiUrl.ECG_PACIENTE, name, idpaciente);
                }
            });

            try {
                String result = future.get();  // Espera la ejecución
                Log.e(TAG, "API: " + result);
                // FICHERO SUBIDO CORRECTAMENTE", Toast.LENGTH_LONG).show();

//                finalizar(); ***************** MIRARRRRRRRRRRRRRRRRRRRRRRRRR

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "EXCEPCION: Error al subir el archivo", Toast.LENGTH_LONG).show();
            }

            executor.shutdown();  // No olvides cerrar el executor

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "EXCEPCION: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    //endregion
}