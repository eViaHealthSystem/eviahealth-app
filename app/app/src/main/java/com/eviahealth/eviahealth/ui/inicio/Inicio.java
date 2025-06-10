package com.eviahealth.eviahealth.ui.inicio;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.Manifest;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.eviahealth.eviahealth.api.BackendConector.ApiConnector;
import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.ui.formacion.ViewPdfWeb;
import com.eviahealth.eviahealth.utils.FileAccess.CarpetaEnsayo;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.api.meeting.APIServiceImpl;
import com.eviahealth.eviahealth.meeting.models.InfoMeeting;
import com.eviahealth.eviahealth.meeting.models.zutils;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.admin.tecnico.LoginOption;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.models.SecuenciaActivity;
import com.eviahealth.eviahealth.devices.ClassDispositivo;
import com.eviahealth.eviahealth.models.devices.Dispositivo;
import com.eviahealth.eviahealth.devices.NombresDispositivo;
import com.eviahealth.eviahealth.ui.ensayo.encuesta.EncuestaCat;
import com.eviahealth.eviahealth.ui.formacion.EducationTraining;
import com.eviahealth.eviahealth.ui.formacion.ViewPDF;
import com.eviahealth.eviahealth.ui.meeting.ConfirmMeetingPatient;
import com.eviahealth.eviahealth.ui.meeting.WaitMeetingPatient;
import com.eviahealth.eviahealth.utils.log.EVLogConfig;
import com.eviahealth.eviahealth.utils.PermissionUtils;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.util;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.ihealth.communication.manager.iHealthDevicesManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class Inicio extends BaseActivity implements View.OnClickListener, Consentimiento.OnDialogInteractionListener {

    final String TAG = "INICIO";
    private String versionName = "--";
    CountDownTimer cTimerSendTest = null;       // Comprobación SubirDatos >> 30 segundos
    CountDownTimer cTimerCheckMeeting = null;   // Gestion de VideoLLamada (50 seg) httpCode
    CountDownTimer cTimerResumeMeeting = null;  // Gestion de VideoLLamada Inicial onResume(5 seg)
    CountDownTimer cTimer = null;               // Timeout de pulsación de botones para acceder a tecnico

    Button buttonEnsayo;
    TextView txtStatus;

    Boolean fgtoken = false;
    Boolean on_ensayo = false;
    boolean server_on = false;
    Boolean activePatient = false;      // Tiene configurado un paciente
    Boolean waitCheckMeeting = false;   // Flag para determinar si han pasado los 5 segundos y se ha gestionado si hay videollamada al iniciar
    int secuencia = 0;
    Boolean multipaciente = false;

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);
        Log.e(TAG, "onCreate()");

        versionName = getVersionNameBuild();

        PermissionUtils.requestAll(this);

        if (checkPermission()) {
            Log.e(TAG, "Si los permisos ya están concedidos");
        } else {
            requestPermission();
        }

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.e(TAG, "Device does not support Bluetooth");
        } else if (!mBluetoothAdapter.isEnabled()) {
            // Bluetooth is not enabled
            mBluetoothAdapter.enable();
        }

        //region :: Views
        txtStatus = findViewById(R.id.txtVersion);
        txtStatus.setTextSize(14);
        txtStatus.setText(versionName); // BuildConfig.VERSION_NAME

        setVisibleView(findViewById(R.id.txt_status_error), View.INVISIBLE);
        setVisibleView(findViewById(R.id.img_warning), View.INVISIBLE);

        buttonEnsayo = findViewById(R.id.activity_inicio_boton_inicio);
        buttonEnsayo.setBackgroundResource(R.drawable.iniciarchequeo_disable);
        //endregion

        int batteryLevel = util.getBatteryLevel(this);
        Log.e(TAG, "Nivel de batería: " + batteryLevel + "%");

        fgtoken = false;
        boolean isiHealthActivate = AutorizarHealthDevices();

        // directorios para los ficheros del ecg
        util.createDirectoryIfNotExists(this, "pdfs");

        SharedPreferences prefs = this.getSharedPreferences("VariableEntorno", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        // Creamos variable de entorno
        editor.putString("url_web", "");
        editor.putString("token", "");
        editor.putString("idpaciente", "");
        editor.putBoolean("AliveCor", false); // Añadido para el sdk de Kardia > AliveCor

        editor.commit();

        //region :: Comprueba si existe host y token asignado
        boolean f = FileAccess.checkIfFileExist(FilePath.CONFIG_TOKEN, true);
        boolean h = FileAccess.checkIfFileExist(FilePath.CONFIG_ADMIN, true);
        try {
            String token, host;
            if (f) {
                token = FileAccess.leerJSON(FilePath.CONFIG_TOKEN).getString("token");
                ApiConnector.setToken(token);
                // Guarda en variables de entorno
                editor.putString("token", token);
                editor.commit();
                fgtoken = true;
            }
            if (h) {
                host = FileAccess.leerJSON(FilePath.CONFIG_ADMIN).getString("URL");
                // Guarda en variables de entorno
                editor.putString("url_web", "https://" + host + "/api/");
                editor.commit();
                ApiConnector.setHost(host);
                fgtoken = true;
            }
        } catch (IOException e) {
            Log.e(TAG, "EXCEPTION >> NO existe fichero conftoken.json");
            SetDisplaySinConfiguracion();
            e.printStackTrace();
        } catch (JSONException e) {
            Log.e(TAG, "EXCEPTION >> NO existe fichero conftoken.json");
            SetDisplaySinConfiguracion();
            e.printStackTrace();
        }
        //endregion

        //region :: Carga URL API EVIHEALTH
        try {
            Log.e("INICIO","Comprobación de URL");
            JSONObject json_admin = FileAccess.leerJSON(FilePath.CONFIG_ADMIN);
            String url = json_admin.getString("URL");
            ApiConnector.setHost(url);
        }
        catch (IOException  | JSONException e) {
            Log.e(TAG, "DIRECCION API INCORRECTA O NO HAY INTERNET.");
        }
        //endregion

        //region :: Timer Comprobación SubirDatos >> 30 segundos
        startTimerSendTest();
        //endregion

        //region :: Google
        // obteniendo la cuenta de Google del último inicio de sesión y almacenándola en la variable acct.
        // Si no hay ninguna cuenta de inicio de sesión, acct será null
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {
            String personName = acct.getDisplayName();
            String personGivenName = acct.getGivenName();
            String personFamilyName = acct.getFamilyName();
            String personEmail = acct.getEmail();
            String personId = acct.getId();
            Log.e("INFO", personName + "PERSON NAME");
            Log.e("INFO", personGivenName + "personGivenName");
            Log.e("INFO", personFamilyName + "personFamilyName");
            Log.e("INFO", personEmail + "personEmail");
            Log.e("INFO", personId + "personId");
        }
        //endregion

        //region :: TimerOut Planificación VideoLlamada primera entrada (5 segudos)
        cTimerResumeMeeting = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimerResumeMeeting.cancel();
//                setEnableView(buttonEnsayo,true);
                buttonEnsayo.setBackgroundResource(R.drawable.iniciarchequeo);
                plannedMeeting(); // >> cTimer.start();
                waitCheckMeeting = true;
            }
        };
        //endregion

        //region :: TimerOut Planificación VideoLlamada (50 segundos)
        cTimerCheckMeeting = new CountDownTimer(50000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimerCheckMeeting.cancel();
                plannedMeeting();
            }
        };
        //endregion

        Log.e(TAG,"VE >> TOKEN: " + prefs.getString("token", ""));
        Log.e(TAG,"VE >> URL_WEB: " + prefs.getString("url_web", ""));

    }

    private String getVersionNameBuild() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            Log.e("Name package", getPackageName());
            Log.e("Version Name: ", versionName);
            return versionName;
        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
            Log.e(TAG, "getVersionNameBuild() >> EXCEPTION: " + e.toString());
            return "";
        }
    }

    private void CargaConfiguracion() {
        try {
            Log.e(TAG, "Carga Configuracion DISPOSITIVOS --------------------------------------");
            // Carga idpaciente del fichero >> "paciente.json"
            JSONObject json_paciente = FileAccess.leerJSON(FilePath.CONFIG_PACIENTE);
            String idpaciente = json_paciente.getString("idpaciente");
            if (json_paciente.has("multipaciente")) {
                multipaciente = json_paciente.getBoolean("multipaciente");
            }
            Config.getInstance().setIdPacienteTablet(idpaciente);
            Config.getInstance().setIdPacienteEnsayo(idpaciente);
            Config.getInstance().setMultipaciente(multipaciente);

            SharedPreferences prefs = this.getSharedPreferences("VariableEntorno", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("idpaciente", idpaciente);
            editor.commit();
            Log.e(TAG,"VE >> IDPACIENTE: " + prefs.getString("idpaciente", ""));

            // configuracion dispositivos >> "dispositivos.json"
            JSONObject json_dispositivos = FileAccess.leerJSON(FilePath.CONFIG_DISPOSITIVOS);
//            EVLog.log(TAG, "DISPOSITIVOS: " + json_dispositivos.toString());

            // NOTA la variable dispositivos es "Config", todo lo que se le haga le afecta.
            Map<NombresDispositivo, Dispositivo> dispositivos = Config.getInstance().getDispositivos();
            SecuenciaActivity secuenciaActivity = SecuenciaActivity.getInstance();
            secuenciaActivity.clear();

            // Carga las actividades de los dispositivos que están habilitados
            for (NombresDispositivo disp : NombresDispositivo.values()) {
                Log.e(TAG, "******** Dispositivos: " + disp.getNombre());

                // config disp enable
                if (!disp.getNombre().equals("ConcentradorO2") && !disp.getNombre().equals("CPAP")) {
                    // Comprueba que el nombre esté en el json de dispositivos
                    if (json_dispositivos.has(disp.getNombre())) {
                        JSONObject json_disp = json_dispositivos.getJSONObject(disp.getNombre());

                        // id, enable device
                        int id_int = json_disp.getInt("id");
                        boolean enabled = json_disp.getBoolean("enable");
//                        Log.e(TAG, "Dispositivo >> id: " + id_int + ", enable: " + enabled);

                        //region: desc = model-mac device
                        String model = "None";
                        String _id = json_disp.getString("desc");
                        if (_id.contains("-")) {
                            String[] dev = _id.split("-");
                            model = dev[0];
//                    String mac = dev[1];
                        }
                        //endregion

                        String extra = "{}";
                        if (json_disp.has("extra")) { extra = json_disp.getString("extra"); }

                        dispositivos.put(disp, new Dispositivo(enabled, _id, disp.getNombre(), id_int, extra));

                        Log.e(TAG, "Dispositivo >> id: " + id_int + ", model: " + model + ", enable: " + enabled + ", extra: " + extra);

                        //region >> Secuencia de actividades

                        // secuencia de actividades, se añaden solo las relativas a dispositivos que enable == 1
                        // dispositivos.put(disp, new Dispositivo(enable_int == 1));

                        Class disp_activity = disp.getActivity();

                        if (enabled) {
                            if (disp == NombresDispositivo.OXIMETRO || disp == NombresDispositivo.TENSIOMETRO ||
                                    disp == NombresDispositivo.ACTIVIDAD || disp == NombresDispositivo.MONITORPULMONAR ||
                                    disp == NombresDispositivo.ECG) {
                                disp_activity = ClassDispositivo.ActivityDevice.get(model);
//                            Log.e(TAG, "model: " + model + ", " + disp_activity.toString());
                            } else if (disp == NombresDispositivo.TERMOMETRO) {
                                Log.e(TAG, "--------------------------------------------MODELO: " + _id + ", " + model);
                                if (!_id.equals("Clasico")) {
                                    disp_activity = ClassDispositivo.ActivityDevice.get(model);
                                }
                            } else if (disp == NombresDispositivo.BASCULA) {
                                Log.e(TAG, "--------------------------------------------MODELO: " + _id + ", " + model);
                                if (!_id.equals("Clasico")) {
                                    disp_activity = ClassDispositivo.ActivityDevice.get(model);
                                }
                            }
                            else if (disp == NombresDispositivo.CAT) {
                                disp_activity = EncuestaCat.class;
                            }
                        }

                        if (disp_activity != null && enabled) {
                            Log.e(TAG, "ADD ACTIVITY LIST: " + disp_activity.getSimpleName());
                            secuenciaActivity.addActivity(disp_activity, extra);
                        }
                        //endregion
                    }
                    else {
                        Log.e(TAG,"******** Dispositivo " + disp.getNombre() + " NO se encuentra en la fichero dispositivos.json");
                    }
                }
            }
            Log.e(TAG,secuenciaActivity.toString());
            Log.e(TAG,"--------------------------------------------------");
            // config idpaciente

            activePatient = true; // se han cargado los fichero correctamente
        }
        catch (IOException | JSONException e) {
            // no existe fichero configuracion
            Log.e(TAG, "EXCEPTION >> IOException o JSONException (dispositivos.json o paciente.json)");
            e.printStackTrace();
            SetDisplaySinConfiguracion();
        }
        finally {
//            EVLog.log("INICIO", "ConfigurarBotonesIrATecnico()");
            ConfigurarBotonesIrATecnico();
        }
    }

    // Timer SUbirDatos
    private void startTimerSendTest() {
       if (cTimerSendTest != null) {
           cTimerSendTest.cancel();
           cTimerSendTest = null;
       }

       // Timeout 30 seg
        cTimerSendTest = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimerSendTest.cancel();

                Log.e(TAG, "Comprobación >> existen datos de ensayos por subir ??");
                // Hay fichero que subir
                if (getFiles_previous()) {
                    Log.e(TAG ,"EXISTE DATOS PENDIENTES DE SUBIR EN LA CARPETA DE ENSAYOS");

                    // disable boton de inicio de ensayo miestras se gestiona subir datos
                    setEnableView(buttonEnsayo,false);

                    if (isNetDisponible() || isOnlineNet()){
                        Log.e(TAG, "EXISTE ACCESO A INTERNET");
                        if (fgtoken && on_ensayo == false) {

                            cTimerSendTest = null;
                            Log.e(TAG, "SEND DATA TEST >> DESCARGAMOS DATOS PENDIENTES.");
                            startActivity(new Intent(Inicio.this, SubirDatos.class));
                            finish();
                        }
                        else {
                            Log.e(TAG, "SEND DATA TEST  >> SIN TOKEN REINICIAMOS APP");
                            cTimerSendTest.start();
                        }
                    } else {
                        Log.e(TAG, "SEND DATA TEST >> SIN ACCESO A INTERNET.");
                        cTimerSendTest.start();
                    }
                    setEnableView(buttonEnsayo,true);
                }
                else {
                    Log.e(TAG,"cTimerSendTest.start() NOT DATA TEST");
                    cTimerSendTest.start();
                }
            }
        };

       Log.e(TAG,"cTimerSendTest.start()");
       cTimerSendTest.start();
    }

    private boolean isNetDisponible() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    public Boolean isOnlineNet() {

        try {
            Process p = java.lang.Runtime.getRuntime().exec("ping -c 1 www.google.es");

            int val = p.waitFor();
            boolean reachable = (val == 0);
            Log.e(TAG,"reachable: " + reachable);

            return reachable;

        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.e(TAG,"Exception reachable: " + e);
            e.printStackTrace();
        }
        return false;
    }

    private void ConfigurarBotonesIrATecnico() {
        //region :: TimerOut Pulsaciones (5 seg x tecla)
        cTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimer.cancel();
                secuencia = 0;
            }
        };
        //endregion
    }

    private void IrALoginAdmin() {

        EVLogConfig.setFechaActual();
        EVLogConfig.log(TAG, "IrALoginAdmin()");

        // Existe? serial.txt y confadmin.json
//        if (FileAccess.checkIfFileExist(FilePath.CONFIG_SERIAL, true) &&
//            FileAccess.checkIfFileExist(FilePath.CONFIG_ADMIN, true)) {
//            Intent intent = new Intent(this, LoginTecnico.class);
//            startActivity(intent);
//        }
//        else {
//            // Si alguno no existe no tiendría imei ni url de instalación establecida
//            Intent intent = new Intent(this, LoginOption.class);
//            startActivity(intent);
//        }

        startActivity(new Intent(this, LoginOption.class));
        finish();
    }

    // Layout Sin Configuración (invisible btInicio)
    private void SetDisplaySinConfiguracion() {
        visibleWarnigsViews();

        TextView statusError = findViewById(R.id.txt_status_error);
        statusError.setTextSize(48);
        setTextView(statusError,"Sistema sin configuración.");
    }

    private void SetDisplayModoAvion() {
        visibleWarnigsViews();
        TextView statusError = findViewById(R.id.txt_status_error);
        statusError.setTextSize(24);
        setTextView(statusError,"Su tablet está en 'Modo Avión'. Es necesario que un técnico restablezca su  configuración para que vuelva a funcionar correctamente. Por favor, póngase en contacto con el servicio técnico.");
    }

    private void SetDisplayServerDown() {
        server_on = false;
        visibleWarnigsViews();
        TextView statusError = findViewById(R.id.txt_status_error);
        statusError.setTextSize(24);
        setTextView(statusError,"Servidor EVIAHEALTH temporalmente inactivo");
    }

    private void SetDisplayServerUp() {
        // Cerramos app para que reinice por estar en modo kiosk
        finish();
    }

    private void visibleWarnigsViews() {
        setVisibleView(findViewById(R.id.txt_status_error), View.VISIBLE);
        setVisibleView(findViewById(R.id.img_warning), View.VISIBLE);

        Button btn_inicio = findViewById(R.id.activity_inicio_boton_inicio);
        btn_inicio.setVisibility(View.INVISIBLE);

        ImageButton imageButtonPoliticaPrivacidad = findViewById(R.id.imageButtonPoliticaPrivacidad);
        imageButtonPoliticaPrivacidad.setVisibility(View.INVISIBLE);

        TextView textTitulo2 = findViewById(R.id.textTitulo2);
        textTitulo2.setVisibility(View.INVISIBLE);

        ImageButton imageButtonFormacion = findViewById(R.id.imageButtonFormacion);
        imageButtonFormacion.setVisibility(View.INVISIBLE);

        TextView textTitulo3 = findViewById(R.id.textTitulo3);
        textTitulo3.setVisibility(View.INVISIBLE);

        activePatient = false;
    }

    private boolean AutorizarHealthDevices(){
        Log.e(TAG, "AutorizarHealthDevices()");
        try {
            InputStream is = this.getAssets().open("com_eviahealth_eviahealth_android.pem");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            boolean isPass = iHealthDevicesManager.getInstance().sdkAuthWithLicense(buffer);
            if (isPass) {
                //cLog("REPOSO", "Autorizar()", "ACCESO PERMITIDO");
                Log.e(TAG, "AutorizarHealthDevices() >> ACCESO PERMITIDO");
            } else {
                Log.e(TAG, "AutorizarHealthDevices() >> ACCESO DENEGADO");
            }
            return isPass;
        } catch (IOException e) {
            Log.e(TAG, "AutorizarHealthDevices() >> EXCEPTION: " + e.toString());
            e.printStackTrace();
            return false;
        }
    }

    public Boolean getFiles_previous() {
        return FileAccess.getFileCarpetaEnsayo().length > 0;
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        Log.e(TAG,"onStart()");
//    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG,"onResume()");

        buttonEnsayo.setEnabled(true);
        buttonEnsayo.setBackgroundResource(R.drawable.iniciarchequeo_disable);
//        EquiposEnsayoPaciente equipos = new EquiposEnsayoPaciente("88888888");
//        equipos.downloadDevices();

        // Carga de configuración de los dispositivos y activa botón para entrar a técnico
        CargaConfiguracion();

        // Comprobamos si la tablet se encuentra en modo avión
        if (isAirplaneModeOn(this)) {
            Log.e(TAG,"Tablet en isAirplaneModeOn(true)");
            SetDisplayModoAvion();
        }

        // Hay paciente configurado en la tablet
        if (activePatient) {
            if (cTimerResumeMeeting != null) {
                cTimerResumeMeeting.cancel();
                cTimerResumeMeeting.start();
                waitCheckMeeting = false;
                Log.e(TAG,"Start Timer VideoLlamada");
            }
        }
    }

    @Override
    protected  void onPause() {
        super.onPause();
        Log.e(TAG,"onPause()");
        waitCheckMeeting = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy()");

        if (cTimerSendTest != null) {
            cTimerSendTest.cancel();
            cTimerSendTest = null;
        }

        if (cTimerResumeMeeting != null) {
            cTimerResumeMeeting.cancel();
            cTimerResumeMeeting = null;
        }

        if (cTimerCheckMeeting != null) {
            cTimerCheckMeeting.cancel();
            cTimerCheckMeeting = null;
        }

        if (cTimer != null) {
            cTimer.cancel();
            cTimer = null;
        }

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.imageButtonPoliticaPrivacidad) {
            //region :: BOTON POLITICA DE PRIVACIDAD

            // Politica de Privacidad del servidor de formacion
            int regFormacion = 0;
            try {
                JSONObject vOb = ApiMethods.getFormacion(getApplicationContext());
                Log.e(TAG, "vOb: " + vOb.toString());

                if (vOb.getInt("httpCode") != HttpsURLConnection.HTTP_OK) {
                    vOb = new JSONObject();
                    // Crear el JSONArray "formacion"
                    JSONArray formacionArray = new JSONArray();
                    // Agregar el JSONArray al JSON principal
                    vOb.put("formacion", formacionArray);
                }

                JSONArray arrayFormacion = vOb.getJSONArray("formacion");
                regFormacion = arrayFormacion.length();
                Log.e(TAG, "len: " + regFormacion);
                List<JSONObject> listado = new ArrayList<>();

                if (regFormacion != 0) {
                    for (int i = 0; i < arrayFormacion.length(); i++) {
                        JSONObject objeto = arrayFormacion.getJSONObject(i);

                        Log.e(TAG, "type: " + objeto.getString("type") + ", title: " + objeto.getString("title"));
                        // Añadimos al listado  todos los tipos type="politica"
                        if (objeto.getString("type").equals("politica")) {
                            listado.add(objeto);
                        }
                    }
                } else {
                    lanzaPoliticaAssets();
                    return;
                }

                regFormacion = listado.size();
                Log.e(TAG, "regFormacion: " + regFormacion);
                if (regFormacion != 0) {
                    Log.e(TAG, "url: " + listado.get(0).getString("url"));
                    Intent i = new Intent(this, ViewPdfWeb.class);
                    i.putExtra("pdf_url", listado.get(0).getString("url"));
                    startActivity(i);
                    finish();
                } else {
                    lanzaPoliticaAssets();
                }

            } catch (JSONException e) {
                Log.e(TAG, "EXCEPTION >> JSONException queryFormacion(): " + e.toString());
                e.printStackTrace();
                lanzaPoliticaAssets();
            }

            //endregion
        }
        else if (viewId == R.id.imageButtonFormacion) {
            //region :: BOTON FORMACIÓN
//                Inicio.this.startActivity(new Intent(Inicio.this, Navegador.class));
            Inicio.this.startActivity(new Intent(Inicio.this, EducationTraining.class));
            finish();
            //endregion
        }
        else if (viewId == R.id.activity_inicio_boton_inicio) {
            // region :: BOTÓN ENSAYO

            if (waitCheckMeeting == false) return; // espera primera consulta de meeting planificada

            on_ensayo = true;

            if (cTimerSendTest != null) {
                // Detiene Timer SubirDatos pendientes
                cTimerSendTest.cancel();
//                cTimerSendTest = null;
            }

            buttonEnsayo.setEnabled(false);

            String paciente = Config.getInstance().getIdPacienteTablet();
            Boolean consentimiento = ApiMethods.getConsentimientoPaciente(paciente);
            Log.e(TAG, "getConsentimiento: " + consentimiento);

            if (consentimiento == false) {
                // Falta consentimiento
                Consentimiento dialog = new Consentimiento();
                dialog.show(getSupportFragmentManager(), "navegadorDialog"); // NO bloquea
            }
            else {
                onEnayo();
            }
            //endregion
        }
        else if (viewId == R.id.buttonConfig1) {
            cTimer.cancel();
            if (secuencia == 0) {
                secuencia = 1;
                cTimer.start();
            }
        }
        else if (viewId == R.id.buttonConfig2) {
            cTimer.cancel();
            if (secuencia == 1) {
                secuencia = 2;
                cTimer.start();
            } else secuencia = 0;
        }
        else if (viewId == R.id.buttonConfig3) {
            cTimer.cancel();
            if (secuencia == 2) {
                secuencia = 3;
                cTimer.start();
            } else secuencia = 0;
        }
        else if (viewId == R.id.buttonConfig4) {
            cTimer.cancel();
            if (secuencia == 3) {
                // Ir a login
                IrALoginAdmin();
            }
            secuencia = 0;
        }
    }

    private void onEnayo() {
        if (multipaciente) {
            Inicio.this.startActivity(new Intent(Inicio.this, PacienteEnsayo.class));
            finish();
        }
        else {
            String idpaciente = Config.getInstance().getIdPacienteTablet();
            Config.getInstance().setIdPacienteEnsayo(idpaciente);

            CarpetaEnsayo.generarCarpetaEnsayo();
            EVLog.setFechaActual();
            EVLog.log(TAG, "onClick() >> Inicio Ensayo " + versionName); // BuildConfig.VERSION_NAME
            EVLog.log(TAG, SecuenciaActivity.getInstance().toString());

            EnsayoLog.setFechaActual();
            EnsayoLog.log(TAG, "", "Inicio de Ensayo");

            FileAccess.writeDateEnsayo(FilePath.DATE_TEST);

            if (!isNetDisponible() || !isOnlineNet()) {
                EVLog.log(TAG, "onClick() >> SIN ACCESO A INTERNET AL INICIAR ENSAYO.");
                EnsayoLog.log(TAG, "", "onClick() >> SIN ACCESO A INTERNET AL INICIAR ENSAYO.");
                Toast.makeText(this, "SIN ACCESO A INTERNET", Toast.LENGTH_LONG).show();
            }

            SecuenciaActivity.getInstance().next(Inicio.this);
        }
    }

    private void lanzaPoliticaAssets() {
        Intent i = new Intent(this, ViewPDF.class);
        i.putExtra("document", "POLITICADEPRIVACIDAD.pdf");
        startActivity(i);
        finish();
    }

    private void plannedMeeting() {

        Log.e(TAG,"---------------------------------------------------------------------------- plannedMeeting()");

        APIServiceImpl serviceZoom = new APIServiceImpl();

        SharedPreferences prefs = this.getSharedPreferences("VariableEntorno", Context.MODE_PRIVATE);
        String idpaciente = prefs.getString("idpaciente", "");

        try {
            JSONObject isActive = serviceZoom.isActivePatientMeeting(getApplicationContext(),idpaciente);
            Log.e(TAG, "isActive: " + isActive);

//            if (isActive.has("httpCode")) {
//                int httpcode = isActive.getInt("httpCode");
//                if (httpcode < 0) {
//                    Log.e(TAG,"SERVIDOR EVIAHEALTH TEMPORALMENTE INACTIVO");
//                    cTimerCheckMeeting.start();
//                    SetDisplayServerDown();
//                    return;
//                }
//            }

            if (isActive.has("status")) {
                int status = isActive.getInt("status");
                if (status == 0) {
                    Log.e(TAG,"Paciente no tiene ninguna videollamada planificada");
                    visibleFrameMeeting(false);
                    cTimerCheckMeeting.start();
                    return;
                }
            }

            // Paciente tiene una videollamada programada
            JSONObject consulta = serviceZoom.getPatientMeeting(getApplicationContext(),idpaciente);
            if (consulta.has("consulta")){
                JSONObject vOb = consulta.getJSONObject("consulta");
                InfoMeeting meeting = new InfoMeeting(vOb);
                Log.e(TAG, "meeting: " + meeting.toString());

                String sdk_appkey = "none";
                String sdk_appsecret = "none";

                try {
                    JSONObject password = new JSONObject(meeting.getPassword());
                    sdk_appkey = password.getString("sdk_appkey");
                    sdk_appsecret = password.getString("sdk_appsecret");
                }catch (JSONException e) {
                    Log.e(TAG,"JSONException: " + e);
                }
                Log.e(TAG,"sdk_appkey: "  + sdk_appkey + ", sdk_appsecret: " + sdk_appsecret);

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("idprescriptor", meeting.getIdprescriptor());
                editor.putString("meeting", meeting.getMeeting());
                editor.putString("fechaMeeting", meeting.getFechaVideoCall());
                editor.putString("sdk_appkey", sdk_appkey);
                editor.putString("sdk_appsecret", sdk_appsecret);
                editor.commit();

                Date ahora = zutils.toDate(zutils.getDateNow());

                Log.e(TAG, "meeting.getFechaVideoCall(): " + meeting.getFechaVideoCall().toString());
//                if (meeting.getFechaVideoCall().equals("null")) {
//                    Log.e(TAG, "Error fecha en null");
//                    cTimerCheckMeeting.start();
//                    return;
//                }

                Date fechaEntradaMeeting = zutils.addMinutesFecha(meeting.getFechaVideoCall(),-10);
                Date fechaFinishMeeting = zutils.addMinutesFecha(meeting.getFechaVideoCall(),40);

                if (meeting.getConfirmado() == 1) {
                    // PENDIENTE DE CONFIRMAR
                    // Abrir nueva actividad para que el paciente confirme o rechace metting
                    // ahora >= fechaEntradaMeeting
                    if (zutils.compareTo(ahora,fechaFinishMeeting) >= 0) {

                        // ahora >= fechaFinishMeeting
                        Log.e(TAG,"ahora >= fechaFinishMeeting FECHA A PASADO SIN CONFIRMAR");

                        // Ocultamos Frame
                        visibleFrameMeeting(false);
                        cTimerCheckMeeting.start();
                        return;
                    }

                    Intent intent = new Intent(this, ConfirmMeetingPatient.class);
                    startActivity(intent);

                    finish();
                }
                else if (meeting.getConfirmado() == 2) {
                    // CONFIRMADO

                    // Comprovamos si la fecha es fechaActual - 10 mint
                    // Fecha no ha llegado a la fecha requerida
                    /*
                     * <p>Devuelve 0  si (date1== date2)</p>
                     * <p>Devuelve >0 si (date1 > date2)</p>
                     * <p>Devuelve <0 si (date1 < date2)</p>
                     */
                    if (zutils.compareTo(ahora,fechaEntradaMeeting) < 0 ) {
                        // ahora < fechaEntradaMeeting
                        Log.e(TAG,"ahora < fechaEntradaMeeting FECHA HA PASADO");

                        // Ponemos visible el frame y establecemos la fecha de la videollamada
                        visibleFrameMeeting(true);
                        setTextMeeting(zutils.clearSecond(meeting.getFechaVideoCall()));

                        cTimerCheckMeeting.start();
                        return;
                    }
                    else {
                        // ahora >= fechaEntradaMeeting
                        if (zutils.compareTo(ahora,fechaFinishMeeting) >= 0) {

                            // ahora >= fechaFinishMeeting
                            Log.e(TAG,"ahora >= fechaFinishMeeting");

                            // Ocultamos Frame
                            visibleFrameMeeting(false);
                            cTimerCheckMeeting.start();
                            return;
                        }
                        else {
                            // fechaEntradaMeeting >= ahora <= fechaFinishMeeting
                            // Ocultamos Frame
                            visibleFrameMeeting(false);

                            // Lanzamos actividad para Esperar a que el paciente entre a la videollamada
                            Intent intent = new Intent(this, WaitMeetingPatient.class);
                            startActivity(intent);
                            finish();
                        }

                    }
                }
                else {
                    // NO deberia producirse nunca
                    visibleFrameMeeting(false);
                    cTimerCheckMeeting.start();
                }

            }
            else {
                Log.e(TAG,"Error de comunicación solicitando API ZOOM");
                cTimerCheckMeeting.start();
            }

        }
        catch (JSONException  e) {
            Log.e(TAG, "EXCEPTION >> JSONException plannedMeeting(): " + e.toString());
            e.printStackTrace();
            cTimerCheckMeeting.start();
        }
        catch (NullPointerException  e){
            Log.e(TAG, "EXCEPTION >> NullPointerException  plannedMeeting(): " + e.toString());
            e.printStackTrace();
            cTimerCheckMeeting.start();
        }
    }

    private void visibleFrameMeeting(boolean visible) {
        runOnUiThread(new Runnable() {
            public void run() {
                FrameLayout layout = (FrameLayout) findViewById(R.id.FramePlanificadaMeeting2);
                if (visible) {
                    if (layout.getVisibility() == View.INVISIBLE) { layout.setVisibility(View.VISIBLE); }
                }
                else {
                    if (layout.getVisibility() == View.VISIBLE) { layout.setVisibility(View.INVISIBLE); }
                }
            }
        });
    }

    private void setTextMeeting(String fecha) {
        runOnUiThread(new Runnable() {
            public void run() {
                TextView textFechaMeeting = (TextView) findViewById(R.id.textFechaMeeting);
                textFechaMeeting.setText(fecha);
            }
        });
    }

    //region :: Permisos READ/WRITE
    private static final int PERMISSION_REQUEST_CODE = 200;
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean writeAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean readAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (writeAccepted && readAccepted) {
                        // Si los permisos se conceden, puedes continuar con tu lógica aquí
                        Log.e(TAG,"PERMISOS CONSEDIDOS *************************");
                    } else {
                        // Si los permisos no se conceden, puedes mostrar un mensaje al usuario explicando por qué necesitas los permisos
                        Log.e(TAG,"PERMISOS NO CONSEDIDOS *************************");
                    }
                }
                break;
        }
    }
    //endregion

    public boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void setTextView(View view, String texto) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (view instanceof TextView) {
                    TextView obj = (TextView) view;
                    obj.setText(texto);
                }
                else if (view instanceof EditText) {
                    EditText obj = (EditText) view;
                    obj.setText(texto);
                }
                else if (view instanceof Button) {
                    Button obj = (Button) view;
                    obj.setText(texto);
                }
            }
        });
    }
    private void setVisibleView(View view, int state) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (view instanceof TextView) {
                    TextView obj = (TextView) view;
                    obj.setVisibility(state);
                }
                else if (view instanceof EditText) {
                    EditText obj = (EditText) view;
                    obj.setVisibility(state);
                }
                else if (view instanceof Button) {
                    Button obj = (Button) view;
                    obj.setVisibility(state);
                }
                else if (view instanceof ImageView) {
                    ImageView obj = (ImageView) view;
                    obj.setVisibility(state);
                }
                else if (view instanceof ProgressBar) {
                    ProgressBar obj = (ProgressBar) view;
                    obj.setVisibility(state);
                }
                else if (view instanceof FrameLayout) {
                    FrameLayout obj = (FrameLayout) view;
                    obj.setVisibility(state);
                }
            }
        });
    }
    private void setEnableView(View view, boolean enable) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (view instanceof Button) {
                    Button bview = (Button) view;
                    if (enable) { bview.setVisibility(View.VISIBLE); }
                    else { bview.setVisibility(View.INVISIBLE); }
                }
            }
        });
    }

    //region :: Consentimiento.onDialogInteractionListener()
    @Override
    public void onAceptar() {
        // Código al aceptar
        String idpaciente = Config.getInstance().getIdPacienteTablet();
        Boolean action = ApiMethods.setConsentimientoPaciente((idpaciente));
        Log.e(TAG, "set: " + action);
        if (action) { onEnayo(); }
        else {
            Toast.makeText(this, "NO se ha podido registrar la acción de consentimiento", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onCerrar() {
        // Código al cerrar activamos de nuevo el botón de ensayo
//        Toast.makeText(this, "Cancelado", Toast.LENGTH_SHORT).show();

        on_ensayo = false;
        if (cTimerSendTest != null) {
            // Activa de nuevo Timer SubirDatos pendientes
            cTimerSendTest.start();
        }
        buttonEnsayo.setEnabled(true);
    }

    @Override
    public void onTecnico() {
        // Código pulsado ensayo técnico
//        Toast.makeText(this, "Técnico", Toast.LENGTH_SHORT).show();
        onEnayo();
    }
    //endregion
}
