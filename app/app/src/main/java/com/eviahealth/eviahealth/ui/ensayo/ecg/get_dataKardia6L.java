package com.eviahealth.eviahealth.ui.ensayo.ecg;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.alivecor.api.AliveCorDetermination;
import com.alivecor.api.AliveCorDevice;
import com.alivecor.api.AliveCorEcg;
import com.alivecor.api.AliveCorKitLite;
import com.alivecor.api.EcgEvaluation;
import com.alivecor.api.FilterType;
import com.alivecor.api.InitListener;
import com.alivecor.api.LeadConfiguration;
import com.alivecor.api.RecordingConfiguration;
import com.alivecor.api.RecordingDeviceInfo;
import com.alivecor.ecg.record.AliveCorPdfHelper;
import com.alivecor.ecg.record.InvalidArgumentException;
import com.alivecor.ecg.record.RecordActivityResult;
import com.alivecor.ecg.record.RecordEkgConstants;
import com.alivecor.ecg.record.RecordEkgFragment;
import com.alivecor.ecg.record.RecordEkgListener;
import com.alivecor.ecg.record.RecordingError;
import com.alivecor.universal_monitor.Filter;
import com.alivecor.universal_monitor.LeadValues;
import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.alivecor.AuthAlivecor;
import com.eviahealth.eviahealth.models.alivecor.data.KardiaData;
import com.eviahealth.eviahealth.api.alivecor.MethodsAlivecor;
import com.eviahealth.eviahealth.models.alivecor.models.EDeterminationKAIv1;
import com.eviahealth.eviahealth.models.alivecor.models.EDeterminationKAIv2;
import com.eviahealth.eviahealth.models.alivecor.models.EMethodUI;
import com.eviahealth.eviahealth.models.alivecor.models.EStatusK6L;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.models.devices.DeviceID;
import com.eviahealth.eviahealth.devices.NombresDispositivo;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.utils.PermissionUtils;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;
import com.eviahealth.eviahealth.models.vitalograph.Patient;

import android.bluetooth.BluetoothAdapter;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class get_dataKardia6L extends BaseActivity implements View.OnClickListener, RecordEkgListener {

    private static final String TAG = "GET_DATA-KARDIA6L";
    TextToSpeechHelper textToSpeech;
    TextView txtStatus, txtContador, txtBPM, txtMsg;
    ImageView imgCorG, imgCorP;
    Button btEmpezarECG;
    ProgressBar pbarContador;
    CountDownTimer cTimerSearch = null;       // TIMEOUT >> Busqueda de dispositivo
    CountDownTimer cTimerMeassure = null;     // TIMEOUT >> Tiempo de prueba superado
    CountDownTimer cTimerCounter = null;     // TIMER CONTADOR 30 SEG RECOGIDA DE REGISTROS
    CountDownTimer cTimerStart = null;       // PARA USO EN EMethodUI.IntentUI, para lazar interfaz
    CountDownTimer cTimer = null;            // Obtener TASK

    private static final int MEASURE_INTERVAL = 60000;
    EStatusK6L status_k6l = EStatusK6L.None;
    String idpaciente = null;
    Boolean viewdata = false;
    private boolean togle = false;
    private Integer counter = 30;
    private int progressStatus = 0;

    KardiaData datos;
    Context mContext;
    // Selecciona el método de visualización para obtener el ecg queremos realizar
    EMethodUI methodUI = EMethodUI.RecordEkgFragment;  // METODO DE INTERFAZ PARA LA GRABACION

    //region -- Kardia variables
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int REQUEST_RECORD_EKG = 101;
    private boolean mAliveCorKitLiteInitialized = false;
    private BluetoothAdapter bluetoothAdapter;

    private AliveCorDevice selectedDevice;          // Dispositivo Kardia seleccionado
    private RecordEkgFragment recordEkgFragment = null;
    private com.alivecor.ecg.record.EcgMonitorViewModel ecgMonitorViewModel = null;
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_data_kardia6_l);

        EVLog.log(TAG, "onCreate()");
        PermissionUtils.requestAll(this);

        mContext = get_dataKardia6L.this;

        //region :: Views
        if (methodUI == EMethodUI.IntentUI) {
            findViewById(R.id.idfondoK6L).setBackground(ContextCompat.getDrawable(this, R.drawable.fondo1_k6l));
        }
        else {
            findViewById(R.id.idfondoK6L).setBackground(ContextCompat.getDrawable(this, R.drawable.fondo2_k6l));
        }
        txtStatus = findViewById(R.id.txtStatusK6L);
        txtStatus.setTextSize(14);
        txtStatus.setVisibility(View.VISIBLE);
        setVisibleView(findViewById(R.id.cdescarga_k6l), View.INVISIBLE);

        btEmpezarECG = findViewById(R.id.btEmpezarECG);
        if (methodUI == EMethodUI.IntentUI) { setVisibleView(btEmpezarECG, View.VISIBLE); }
        else { setVisibleView(btEmpezarECG, View.INVISIBLE); }


        txtMsg = findViewById(R.id.txt_msg);
        txtMsg.setVisibility(View.INVISIBLE);

        setVisibleView(findViewById(R.id.image_k6l), View.INVISIBLE);
        setVisibleView(findViewById(R.id.txtmessage_k6l), View.INVISIBLE);

        txtContador = findViewById(R.id.timerText);
        txtBPM = findViewById(R.id.txtbpm_k6l);

        pbarContador = findViewById(R.id.progressBar);
        pbarContador.setMax(30);
        pbarContador.setMin(0);
        pbarContador.setProgress(progressStatus);

        imgCorG = findViewById(R.id.image_cor);
        imgCorP = findViewById(R.id.image_cor_peque);

        setVisibleView(txtContador, View.INVISIBLE);
        setVisibleView(pbarContador, View.INVISIBLE);
        setVisibleView(txtBPM, View.INVISIBLE);
        setVisibleView(imgCorG, View.INVISIBLE);
        setVisibleView(imgCorP, View.INVISIBLE);

        //endregion

        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = KardiaData.getInstance();
        datos.setMethodUI(methodUI);

        //region :: Carga MAC Dispositivo
        String DEVICE_MAC_ADDRESS = Config.getInstance().getIdentificador(NombresDispositivo.ECG);
        EVLog.log(TAG,"MAC DISPOSITIVO: "+ DEVICE_MAC_ADDRESS);
        if (DEVICE_MAC_ADDRESS.contains("K6L")){
            DEVICE_MAC_ADDRESS = DEVICE_MAC_ADDRESS.replace("K6L-", "");
            String mDeviceAddress = util.MontarMAC(DEVICE_MAC_ADDRESS);
            EVLog.log(TAG,"MAC mDeviceAddress: "+ mDeviceAddress);
        }
        //endregion

        //region >> Carga extras desde la db
        try {
            // Cargamos datos de la configuracion del lung desde la db
            idpaciente = Config.getInstance().getIdPacienteEnsayo();
            String response = ApiMethods.getExtrasDevice(idpaciente, DeviceID.Ecg.getValue());

            JSONObject obj = new JSONObject(response);
            if (obj.has("extra")) {
                JSONObject jextras = new JSONObject(obj.getString("extra"));
                EVLog.log(TAG, "JEXTRAS ECG: " + jextras.toString());
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        //endregion

        // Obtenemos si se ha inciado previmente el sdk de ALiveCor
        SharedPreferences prefs = mContext.getSharedPreferences("VariableEntorno", Context.MODE_PRIVATE);
        mAliveCorKitLiteInitialized = prefs.getBoolean("AliveCor", false);
        EVLog.log(TAG,"onCreate() mAliveCorKitLiteInitialized: " + mAliveCorKitLiteInitialized);

        Integer timeCuenta = 33 * 1000;  // 31seg
        cTimerCounter = new CountDownTimer(timeCuenta, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                counter--;
                progressStatus++;

                if (counter < 0) counter = 0;
                updateCounter();
            }

            @Override
            public void onFinish() {
                cTimerCounter.cancel();
                progressStatus = 30;
                counter = 0;
                updateCounter();
                cTimerCounter = null;
                EVLog.log(TAG,"TEMPORIZACIÓN TERMINADA");

                if (status_k6l == EStatusK6L.Recording) {
                    datos.setStatusDescarga(false);
                    datos.setMsgfail(800,"RecordTimeOut","Se ha superado el tiempo de grabación y no se ha finalizado correctamente");
                    ViewResult();
                }

            }
        };

        Integer timeout = this.MEASURE_INTERVAL * 2;  // 60seg x 2 = 120seg
        cTimerMeassure = new CountDownTimer(timeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                cTimerMeassure.cancel();
                cTimerMeassure = null;
                EVLog.log(TAG,"Tiempo para la rezalización de la prueba superado");

                datos.setStatusDescarga(false);

                if (status_k6l == EStatusK6L.Scanning) {
                    datos.setMsgfail(801, "Error", "No detectado dispositivo ECG");
                }
                else if (status_k6l == EStatusK6L.Recording) {
                    datos.setMsgfail(802, "Error", "La grabación del dispositivo ECG no ha finalizado.");
                }
                else {
                    datos.setMsgfail(803, "Error", "TimeOut dipositico ECG: " + status_k6l.toString());
                }
                ViewResult();
            }
        };
        if (methodUI == EMethodUI.RecordEkgFragment) {
            cTimerMeassure.start();
            startAliveCor();
        }
        else {
            Integer t = 10 * 1000;  // 31seg
            cTimerStart = new CountDownTimer(t, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {}

                @Override
                public void onFinish() {
                    cTimerStart.cancel();
                    cTimerStart = null;
                    startAliveCor();
                }
            }.start();
        }

    }

    private void initializeTimer() {
        if (cTimer == null) {
            Integer timeout = this.MEASURE_INTERVAL * 2;
            cTimer = new CountDownTimer(timeout, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    LiveData d = ecgMonitorViewModel.state();
                    Log.e(TAG,"@@@@ ecgMonitorViewModel.state(): " + d.getValue().toString());

                    if (d.getValue().toString().equals("ERROR")) {
                        cTimer.cancel();
                        EVLog.log(TAG,"ERROR: Se ha producido un error. No detectado ningún dispositivo.");
                        datos.setStatusDescarga(false);
                        datos.setMsgfail(804,"Device","Se ha producido un error.\nNo detectado ningún dispositivo");
                        ViewResult();
                    }
                    else if (d.getValue().toString().equals("EVALUATION_NEED_CONFIRM")) {
                        cTimer.cancel();
                        EVLog.log(TAG,"EVALUATION_NEED_CONFIRM: Se ha producido un error. Grabación incompleta o errónea.");
                        datos.setStatusDescarga(false);
                        datos.setMsgfail(819,"Device","Se ha producido un error.\nGrabación incompleta o errónea.");
                        ViewResult();
                    }

                    /*
                    enum State (IDLE, STARTING, LISTENING, PRE_CAPTURING, CAPTURING, EVALUATING,
                    SAVING_DISK, EVALUATION_NEED_CONFIRM, EVALUATION_READY, ERROR;
                     */
                }

                @Override
                public void onFinish() {
                    cTimer.cancel();
                    cTimer = null;
                }
            }.start();
        }
    }

    //region :: KARDIA
    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.RECORD_AUDIO
        };

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }
    @SuppressLint("MissingPermission")
    private void checkAndEnableBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            EVLog.log(TAG, "Bluetooth no está disponible en este dispositivo.");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void startAliveCor() {
        if (mAliveCorKitLiteInitialized == false) {
            JSONObject jwt = MethodsAlivecor.getJWT_Alivecor();
            if (jwt != null) {
                EVLog.log(TAG, "inicializarSDKAliveCor()");
                inicializarSDKAliveCor(jwt);
            } else {
                status_k6l = EStatusK6L.ErrorJWT;
                EVLog.log(TAG, "JWT NO OBTENIDO");
                datos.setStatusDescarga(false);
                datos.setMsgfail(805, "ErrorJWT", "ERROR al obtener JWT");
                ViewResult();
            }
        }
        else {
            EVLog.log(TAG, "SDK Inicializado anteriormente.");
            status_k6l = EStatusK6L.SdkInitialized;
            mAliveCorKitLiteInitialized = true;

            methodRecorgingUI();
        }
    }

    private void methodRecorgingUI() {
        if (methodUI == EMethodUI.IntentUI) {
            setRecording();         // utilizando intent
        }
        else if (methodUI == EMethodUI.RecordEkgFragment) {
            startCustomRecording(); // utilizando RecordEkgFragment invisible en ui custom
        }
        else {
            setRecording();         // utilizando intent por defecto
        }
    }

    // Inicialización del SDK de AliveCor
    public void inicializarSDKAliveCor(JSONObject jwt) {
        try {
            // Obtén la clave API desde el JWT

            if (jwt.has("httpCode")) {
                Integer httpCode = jwt.getInt("httpCode");
                if (httpCode == 500) {
                    datos.setStatusDescarga(false);
                    datos.setMsgfail(806,"ErrorJWT","Parámetros de obtención del JWT erróneos. httpCode(500)");
                    ViewResult();
                    return;
                }
                else if (httpCode == 404) {
                    datos.setStatusDescarga(false);
                    datos.setMsgfail(807,"ErrorJWT","URL del DOCKER no válida. httpCode(404)");
                    ViewResult();
                    return;
                }
                else if (httpCode == -999) {
                    datos.setStatusDescarga(false);
                    datos.setMsgfail(808,"ErrorJWT","URL del DOCKER no responde. Compruebe que el DOCKER esté on-line.");
                    ViewResult();
                    return;
                }
            }

            String apiKey = jwt.optString("jwt");
            try { EVLog.log(TAG,"apiKey: " + apiKey); }
            catch (Exception e) { EVLog.log(TAG,"apiKey: fail"); }

            if (apiKey.isEmpty()) {
                EVLog.log(TAG, "JWT no contiene una clave API válida.");
                datos.setStatusDescarga(false);
                datos.setMsgfail(809,"ErrorJWT","JWT no contiene una clave API válida.");
                ViewResult();
                return;
            }

//            apiKey = apiKey + "22";

            // Inicializa el SDK AliveCorKitLite
            AliveCorKitLite.initialize(
                    getApplicationContext(),  // Contexto de la aplicación
                    apiKey,
                    new InitListener() {
                        @Override
                        public void onInitComplete() {
                            EVLog.log(TAG, "SDK AliveCorKitLite inicializado correctamente. Version(" + AliveCorKitLite.getVersion() + ")");
                            List<AliveCorDevice> devices = AliveCorKitLite.get().getSupportedDevices();
                            EVLog.log(TAG, "LISTADO DE DISPOSITIVOS PERMITIDOS: " + devices.toString());

                            status_k6l = EStatusK6L.SdkInitialized;
                            mAliveCorKitLiteInitialized = true;

                            SharedPreferences prefs = mContext.getSharedPreferences("VariableEntorno", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("AliveCor", mAliveCorKitLiteInitialized);
                            editor.commit();

                            methodRecorgingUI();
                        }

                        @Override
                        public void onInitError(Throwable throwable) {
                            status_k6l = EStatusK6L.SdkFail;
                            mAliveCorKitLiteInitialized = false;
                            EVLog.log(TAG, "******  ERROR al inicializar el SDK AliveCorKitLite.");
                            EVLog.log(TAG, "onInitError(): " + throwable.getMessage());
                            datos.setStatusDescarga(false);
                            datos.setMsgfail(810,"SdkFail","ERROR al inicializar el SDK AliveCorKitLite.");
                            ViewResult();
                        }
                    },
                    AuthAlivecor.TYPE_SERVER_ALIVECOR,
                    "eViaHealth APP",  // Nombre de la aplicación
                    AuthAlivecor.BUNDLEID,
                    AuthAlivecor.PARTNERID,
                    true                        // Habilitar logs en consola
            );
        } catch (Exception e) {
            status_k6l = EStatusK6L.SdkFail;
            Log.e(TAG, "Error durante la inicialización del SDK: " + e.getMessage(), e);
            datos.setStatusDescarga(false);
            datos.setMsgfail(811,"SdkFail","Exception AliveCorKitLite.initialize");
            ViewResult();
        }
    }

    //region ::  RecordEkgListener: Listener de eventos de la grabación de datos
    @Override
    public void onRecordCompleted(AliveCorEcg result) {
        EVLog.log(TAG, "****** onRecordCompleted(): Grabación completada con éxito");
        cTimerCounter.cancel();
        counter = 0;

        status_k6l = EStatusK6L.RecordComplete;
        setTextView(txtStatus, status_k6l.toString());

        analizerEcg(result);
    }

    @Override
    public void onRecordError(RecordingError error) {
        EVLog.log(TAG, "****** onRecordError()");
        String message = null;
        Integer nerr = null;

        // Manejar errores específicos durante la grabación
        switch (error) {
            case BLUETOOTH_OFF:
                nerr = 1;
                message = "Bluetooth está apagado. Solicita al usuario activarlo. (BLUETOOTH_OFF)";
                Log.e(TAG,"Bluetooth está apagado. Solicita al usuario activarlo.");
                break;
            case NFC_ON:
                nerr = 2;
                message = "NFC está activado. Solicita al usuario desactivarlo. (NFC_ON)";
                Log.e(TAG,"NFC está activado. Solicita al usuario desactivarlo.");
                break;
            case TRIANGLE_BATTERY:
                nerr = 3;
                message = "Batería baja en el dispositivo KardiaMobile 6L. (TRIANGLE_BATTERY)";
                Log.e(TAG, "Batería baja en el dispositivo KardiaMobile 6L.");
                break;
            case MIC_PERMISSION_EKG:
                nerr = 4;
                message = "Permiso de micrófono requerido. Solicítalo al usuario. (MIC_PERMISSION_EKG)";
                Log.e(TAG, "Permiso de micrófono requerido. Solicítalo al usuario.");
                break;
            default:
                nerr = 5;
                message = error.name();
                Log.e(TAG, "Error durante la grabación: " + error.name());
        }

        datos.setStatusDescarga(false);
        datos.setMsgfail(nerr,"RecordError",message);
        ViewResult();
    }

    @Override
    public void onUserCancel() {
        EVLog.log(TAG, "****** onUserCancel()");
        // este evento solo saltaría si el fragment fuera visible
    }

    @Override
    public void onBTPairingCancel() {
        EVLog.log(TAG, "****** onBTPairingCancel()");
//        Log.i(TAG,"El usuario canceló el emparejamiento Bluetooth.");
    }

    @Override
    public void onChangeDevice() {
        EVLog.log(TAG, "****** onChangeDevice()");
//        Log.i(TAG,"El usuario seleccionó cambiar de dispositivo.");
        // Aquí puedes iniciar un nuevo flujo para seleccionar otro dispositivo
    }

    @Override
    public void onLeadConfigUpdated(LeadConfiguration config) {
        EVLog.log(TAG, "****** onLeadConfigUpdated(): Configuración de derivaciones actualizada" + config.name());
//        Log.i(TAG,"Configuración de derivaciones actualizada: " + config.name());
    }

    @Override
    public void onRecordSettings() {
        EVLog.log(TAG, "****** onRecordSettings()");
//        Log.i(TAG,"El usuario abrió la configuración de grabación.");
    }

    @Override
    public void onEcgFrame(LeadValues latest) {

        if (latest != null) {
            EVLog.log(TAG, "****** onEcgFrame(): " + latest.toString());
        }
        else { EVLog.log(TAG, "****** onEcgFrame(): null"); }

//      Log.i(TAG,"Valores actuales del ECG: " + latest.toString());
    }

    @Override
    public void onBPMUpdated(String bpm) {
        // Actualización de la frecuencia cardíaca
        EVLog.log(TAG, "onBPMUpdated(): status_k6l(" + status_k6l.toString() + "), bpm(" + bpm + ")");
//        Log.e(TAG,"in status_k6l: " + status_k6l.toString());
//        Log.i(TAG,"Frecuencia cardíaca actual: " + bpm + " BPM");
        if (status_k6l == EStatusK6L.Detected) {
            status_k6l = EStatusK6L.WaitBPM;
//            Log.e(TAG,"status_k6l: " + status_k6l.toString());
            setTextView(txtStatus, status_k6l.toString());
        }
        else if (status_k6l == EStatusK6L.WaitBPM) {
            if (bpm.contains("---") == false) {
                status_k6l = EStatusK6L.Recording;
//                Log.e(TAG,"status_k6l: " + status_k6l.toString());
                setTextView(txtStatus, status_k6l.toString());
                cTimerCounter.start();
            }
        }
        else if (status_k6l == EStatusK6L.Recording) {
            setTextView(txtBPM,"" + bpm + " BPM");
            imagetogle();

            if (bpm.contains("---")) {
                status_k6l = EStatusK6L.WaitBPM;
//                Log.e(TAG,"status_k6l: " + status_k6l.toString());
                setTextView(txtStatus, status_k6l.toString());

                cTimerCounter.cancel();
                EVLog.log(TAG,"DETECTADO MAL CONTACTO CON LOS ELECTRODOS");

                if (counter <= 21) {
                    EVLog.log(TAG, "Dispositivo ya no engancha de nuevo, tiempo de reenganche excedido.");
                    datos.setStatusDescarga(false);
                    datos.setMsgfail(812,"RecordError","Usuario no ha hecho contacto de forma correcta con los electrodos. (bmpUpdated)");
                    ViewResult();
                }
                else {
                    counter = 30;
                    progressStatus = 0;
                    updateCounter();
                    setVisibleView(txtMsg, View.VISIBLE);
                }

            }

        }

    }

    @Override
    public void onDeviceId(String deviceId) {
        EVLog.log(TAG, "****** onDeviceId(): dispositivo conectado(" + deviceId + ")");
//        Log.i(TAG,"ID del dispositivo conectado: " + deviceId);

        status_k6l = EStatusK6L.Detected;
        setTextView(txtStatus, status_k6l.toString());

        changeFondoRecording();

    }
    //endregion

    //region :: Métodos Recording
    /**
     <p>Interfaz UI predefinido por Kardia, abre una actividad completa.</p>
     <p>Hay que controlar un Listener de Activity</p>
     */
    private void setRecording() {

        LeadConfiguration leadsConfig = LeadConfiguration.SINGLE;
//        LeadConfiguration leadsConfig = LeadConfiguration.SIX;
        final Filter filterType = Filter.ENHANCED;
//        final Filter filterType = Filter.ORIGINAL;
        final int mainsFilter = 50;
        final int recordingMaxDurationSec = 30;

        Intent intent = AliveCorKitLite.get().getRecordIntent(AliveCorDevice.TRIANGLE);

        LeadConfiguration finalLeadsConfig = leadsConfig;
        intent.putExtra(RecordEkgConstants.EXTRA_LEADS_CONFIG, finalLeadsConfig.name());
        intent.putExtra(RecordEkgConstants.EXTRA_MAX_DURATION, recordingMaxDurationSec);
        intent.putExtra(RecordEkgConstants.EXTRA_MIN_DURATION, 10);
        intent.putExtra(RecordEkgConstants.EXTRA_FILTER_TYPE, filterType.name());
        intent.putExtra(RecordEkgConstants.EXTRA_REC_FREQUENCY, mainsFilter);

        // Activación o desactivación de los botones de cambio de derivaciones en dispositivos Kardia 6L
        intent.putExtra(RecordEkgConstants.EXTRA_ENABLE_LEADS_BUTTONS, true);

        // NO FUNCIONAN
//        intent.putExtra(RecordEkgConstants.EXTRA_SHOW_RECORDING_RESULT, true);
//        intent.putExtra(RecordEkgConstants.EXTRA_REC_DISPLAY_BPM, true);
//        intent.putExtra(RecordEkgConstants.EXTRA_HIDE_TRACE, false);
//        intent.putExtra(RecordEkgConstants.EXTRA_HELP_URLS, createHelpUrls());

        startActivityForResult(intent, REQUEST_RECORD_EKG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_RECORD_EKG) {
            Log.e(TAG, "----@ resultCode == REQUEST_RECORD_EKG");
            EVLog.log(TAG, String.format("---- onActivityResult() from EKG Recording. Result: %d/%s", resultCode, AliveCorKitLite.get().getRecordActivityResult(data)));

            if (resultCode == Activity.RESULT_CANCELED) {
                EVLog.log(TAG, "RESULT_CANCELED: Operación cancelada por el paciente.");
                datos.setStatusDescarga(false);
                datos.setMsgfail(813,"Activity","Operación cancelada por el paciente.");
                ViewResult();
                return;
            }
            else if (resultCode == RecordEkgConstants.RESULT_CHANGE_DEVICE) {
                EVLog.log(TAG, "RESULT_CHANGE_DEVICE: Paciente a seleccionado otro dispositivo");
                datos.setStatusDescarga(false);
                datos.setMsgfail(814,"Activity","Paciente a seleccionado otro dispositivo");
                ViewResult();
                return;
            }

            // RESULT_OK
            RecordActivityResult result = AliveCorKitLite.get().getRecordActivityResult(data);
            if (result != null && result.getSuccessfulResult() != null) {

                List<RecordingError> errors = result.getErrors();
                for (RecordingError error: errors) {
                    EVLog.log(TAG,"onActivityResult() RecordingError: " + error.toString());
                }

                AliveCorEcg ecg = result.getSuccessfulResult();
                EVLog.log(TAG, "OBTENIDO ECG: " + result.toString());

                cTimerCounter.cancel();
                status_k6l = EStatusK6L.RecordComplete;
                setTextView(txtStatus, status_k6l.toString());

                analizerEcg(ecg);
            }
            else {
                EVLog.log(TAG, "onActivityResult() RESULT FAIL ECG");
                analizerEcg(null);
            }
        }
        else {
            Log.e(TAG, "----@ resultCode == " + resultCode);
        }
    }

    /**
     <p>Fragment predefinido por Kardia,.</p>
     <p>---</p>
     */
    private void startCustomRecording() {

        receiverBle(); // MI RECEIVER

        runOnUiThread(() -> {
            try {
                // Configuración personalizada del registro
                RecordingConfiguration configuration = new RecordingConfiguration();
                configuration.setDevice(AliveCorDevice.TRIANGLE); // Configura el dispositivo KardiaMobile 6L
                configuration.setLeads(LeadConfiguration.SINGLE);   // Configuración de una sola derivación
//                configuration.setLeads(LeadConfiguration.SIX);      // Configuración de 6 derivaciones
                configuration.setFilterType(FilterType.ENHANCED);   // Aplica filtro mejorado para ruido
                configuration.setMaxDurationSeconds(30);            // Duración máxima: 30 segundos
                configuration.setResetDurationSeconds(10);          // Tiempo de reinicio si pierde contacto
                configuration.setMainsFrequency(RecordingConfiguration.MAINS_FREQUENCY_50Hz); // Frecuencia de red

                // Crear instancia del fragmento de grabación
                recordEkgFragment = RecordEkgFragment.newInstance(configuration);

                // Establece listener de grabación
                recordEkgFragment.addListener(this);

                status_k6l = EStatusK6L.Scanning;
                setTextView(txtStatus, status_k6l.toString());
                setVisibleView(findViewById(R.id.cdescarga_k6l), View.VISIBLE);   // PROGRESSBAR PRINCIPAL

                // Mostrar el fragmento en el contenedor de la UI
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, recordEkgFragment)
                        .commit();

                ecgMonitorViewModel = new ViewModelProvider(this).get(com.alivecor.ecg.record.EcgMonitorViewModel.class);
//                observeTasks();
                initializeTimer();

            } catch (InvalidArgumentException e) {
                EVLog.log("CustomUI", "Error al establecer RecordEkgFragment: " + e.getMessage());

                datos.setStatusDescarga(false);
                datos.setMsgfail(815,"RecordingEx","Error al configurar RecordEkgFragment: " + e.toString());
                ViewResult();
            }
        });
    }
    //endregion

    //region: ANALISIS, INFORMACION Y PDF DEL ECG
    /**
     * <p>Información del decive que ha realizado el ecg.</p>
     */
    private void getDeviceInfo(AliveCorEcg result) {
        if (result != null) {
            RecordingDeviceInfo deviceInfo = result.getDeviceInfo();

            Log.e(TAG, "BatteryLevel: " + deviceInfo.getBatteryLevel());
            Log.e(TAG, "deviceInfo: " + deviceInfo.toString());


            Log.e(TAG, "DurationMs: " + result.getDurationMs());
            Log.e(TAG, "RawAtcPath: " + result.getRawAtcPath());
            Log.e(TAG, "EnhancedAtcPath: " + result.getEnhancedAtcPath());
            Log.e(TAG, "FilesDirectory: " + result.getFilesDirectory().toString());
            Log.e(TAG, "RecordedAtMs: " + result.getRecordedAtMs());
            Log.e(TAG, "Uuid: " + result.getUuid());

            datos.setBatteryLevel(deviceInfo.getBatteryLevel());
            try { datos.setDeviceType(deviceInfo.getDevice().toString()); }
            catch (Exception e) {
                e.printStackTrace();
                datos.setDeviceType(null);
            }
            datos.setDeviceMac(deviceInfo.getDeviceUuid());
            datos.setLeadConfiguration(deviceInfo.getLeadConfiguration().name());
            datos.setNamePdf(result.getUuid());
        }
    }

    /**
     * <p>Resolución de la grabación del ecg</p>
     * @param recordedEcg
     * @return
     */
    private String validateRecording(AliveCorEcg recordedEcg) {
        try {

            if (recordedEcg != null) {
                EcgEvaluation evaluation = recordedEcg.getEcgEvaluation();

                // Determinación principal
                AliveCorDetermination determination = evaluation.getDetermination();
                String kaiResult = evaluation.getKaiResult();
                // Resultado de la grabación
                EVLog.log(TAG, "----- kaiResult: " + kaiResult);
                return kaiResult;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * <p>Obtiene el resultado obtenido del ecg.</p>
     * <p>Tiene resultado y detalles del ecg</p>
     * @param recordedEcg
     */
    private void diagnostico(AliveCorEcg recordedEcg) {
        if (recordedEcg != null) {
            EcgEvaluation evaluation = recordedEcg.getEcgEvaluation();
            EVLog.log(TAG, "#### EcgEvaluation: " + evaluation.toString());

            // Determinación principal
            AliveCorDetermination determination = evaluation.getDetermination();
            EVLog.log(TAG, "#### AliveCorDetermination: " + determination.toString());

            String kaiResult = evaluation.getKaiResult();
            String kardiaAiVersion = evaluation.getKardiaAiVersion();
            String algorithmPackage = evaluation.getAlgorithmPackage();
            String resultText = evaluation.getAlgorithmResultText().toString();
            String resultDescription = evaluation.getAlgorithmResultDescription().toString();

            // Frecuencia cardíaca promedio
            Float averageHeartRate = evaluation.getAverageHeartRate();
            String heartRateDisplay = (averageHeartRate != null)
                    ? String.format("Frecuencia cardíaca: %.1f BPM", averageHeartRate)
                    : "Frecuencia cardíaca no disponible";

            // Manejo de inversiones
            boolean isInverted = evaluation.isInverted();
            String inversionText = isInverted
                    ? "Nota: La grabación parece estar invertida. Verifica la posición de los electrodos."
                    : "Grabación correcta.";

            CharSequence charSequence = evaluation.getResultDisclaimerText();

            // Mostrar al usuario
            Log.e(TAG, "----- AliveCorDetermination: " + determination.getSerializedName());
            Log.e(TAG, "----- kaiResult: " + kaiResult);
            Log.e(TAG, "----- kardiaAiVersion: " + kardiaAiVersion);
            Log.e(TAG, "----- Resultado: " + resultText);
            Log.e(TAG, "----- Detalles: " + resultDescription);
            Log.e(TAG, "----- " + heartRateDisplay);
            Log.e(TAG, "----- " + inversionText);
            try { Log.e(TAG, "----- charSequence: " + charSequence.toString()); }
            catch (Exception e) { e.printStackTrace(); }

            // datos
            datos.setKaiResult(kaiResult);
            datos.setKardiaVersion(kardiaAiVersion);
            datos.setResultECG(resultText);
            datos.setDetailsECG(resultDescription);
            datos.setBpm(String.format("%.1f", averageHeartRate));
            datos.setInverter(isInverted);
            datos.setAlgorithmPackage(algorithmPackage); // "KAIv1" o "KAIv2"

            Log.e(TAG,"XX X algorithmPackage: " + algorithmPackage);
            if (algorithmPackage.equals("KAIv1")) {
                Log.e(TAG,"kaiResult: " + kaiResult);
                Log.e(TAG, "XX KAiv1: " + EDeterminationKAIv1.getMessageByCode(kaiResult));
                datos.setResultScreenDeterminationInfo(EDeterminationKAIv1.getMessageByCode(kaiResult));
            } else {
                Log.e(TAG,"kaiResult: " + kaiResult);
                Log.e(TAG, "XX KAiv2: " + EDeterminationKAIv2.getMessageByCode(kaiResult));
                datos.setResultScreenDeterminationInfo(EDeterminationKAIv2.getMessageByCode(kaiResult));
            }

            EVLog.log(TAG,"analizerEcg: " + datos.toString());
        }
    }

    /**
     * <p>Genera un informe en pdf con los datos obtenidos del ecg</p>
     * @param pdfLanguage
     * @param ecg
     */
    private void generatePdf(String pdfLanguage, AliveCorEcg ecg) {

        // Obtener valores individuales para PatientInfo
        String firstName = "Nombre";
        String lastName = "Apellido";

        boolean isPatinet = false;
        try {
            String name = ApiMethods.getNameIdPaciente(idpaciente);
            JSONObject data = new JSONObject(name);
            if (data.has("nombre") && data.has("apellidos")) {
                // Mostrar cuadro de confirmación de id nombre paciente
                firstName = data.getString("nombre");
                lastName = data.getString("apellidos");
                isPatinet = true;
            }
            else {
                Toast.makeText(this, "No ha sido posible obtener la información del paciente.", Toast.LENGTH_LONG).show();
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "No ha sido posible obtener la información del paciente.", Toast.LENGTH_LONG).show();
        }


        long age = 50;
        String patientId = idpaciente;    // ID del paciente
        String gender = "M";              // Género: M/F
        boolean isCharacteristics = true;
        Patient paciente = ApiMethods.loadCharacteristics(idpaciente);
        if (paciente.getBydefault()) {
            EVLog.log(TAG,"Datos de paciente cargados con valores por defecto");
            isCharacteristics = false;
        }

        age = (long)paciente.getAge();
        long dateOfBirth = System.currentTimeMillis() - (age * 365 * 24 * 60 * 60 * 1000); // Fecha de nacimiento aproximada
        gender = (paciente.getGender().equals("Hombre"))? "M" : "F";

//        AliveCorPdfHelper.PatientInfo patientInfo = new AliveCorPdfHelper.PatientInfo(firstName, lastName, DateTime.now().minusYears(50).getMillis(), patientId, gender);/
        AliveCorPdfHelper.PatientInfo patientInfo = new AliveCorPdfHelper.PatientInfo(firstName, lastName, dateOfBirth, patientId, gender);

        // PDF Reports can be generated into any file.  The SDK will overwrite any data in this file
        File outputFile = new File(FileAccess.getPATH_FILES(), "pdfs/" + ecg.getUuid() + ".pdf");

        AliveCorPdfHelper pdfHelper = AliveCorKitLite.get().getPdfHelper();
        pdfHelper.setForceInvert(datos.getInverter());

        // nuevo método para seleccionar cualquier idioma soportado en SDK para informe PDF
        // establecer en pdfHelper.setLanguage(null); para restablecer la configuración regional predeterminada del dispositivo
        if (pdfLanguage != null) {
            pdfHelper.setLanguage(pdfLanguage);
        }

        String tags = "---";             // Etiquetas (opcional)
        //region :: Notas (opcional)
        String notes = (datos.getDetailsECG() != null) ? datos.getDetailsECG() : "";
        String notes2 = (datos.getResultScreenDeterminationInfo() != null) ? datos.getResultScreenDeterminationInfo() : "";
        notes = notes + "\n\n" + notes2;

        if ((!isPatinet) || (!isCharacteristics)) {
            notes = notes + "\n\n" + "No ha sido posible acceder a la información del paciente en el momento de la generación de este documento. Por este motivo, se han utilizado valores por defecto para completar los datos del paciente en el documento.";
        }
        //endregion

        EcgEvaluation evaluation = ecg.getEcgEvaluation();
        String algorithmVersion = evaluation.getKardiaAiVersion(); // Versión del algoritmo (ej. "KAIv2"

        // Datos adicionales (opcional) >> beatsData, avgBeatData, rrIntervalData
        List<List<Integer>> beatsData = new ArrayList<>();
        List<Short> avgBeatData = new ArrayList<>();
        List<List<Integer>> rrIntervalData = new ArrayList<>();

        boolean enablePvcNotation = true;      // Anotaciones PVC (opcional)

        pdfHelper.createPdfReport(
                ecg,                            // Grabación de ECG
                AliveCorPdfHelper.Size.A_4, // Tamaño del PDF (LETTER o A4)
                outputFile,                    // Archivo de salida
                null,                          // Ruta al logo (opcional) "path/to/logo.png"
                patientInfo,                   // Información del paciente
                null,                          // Etiquetas (opcional) tags
                notes,                         // Notas (opcional)
                algorithmVersion,              // Versión del algoritmo (ej. "KAIv2")
                beatsData, avgBeatData, rrIntervalData, // Datos adicionales (opcional)
                enablePvcNotation,             // Anotaciones PVC (opcional)
                true                          // Bandera sobre si el usuario grabó el ECG
        );
    }

    /**
     * <p>Analiza si el ecg es válido</p>
     * <p>Obtene la información del resultado del ecg.</p>
     * <p>Genera el archivo pdf</p>
     * @param recordedEcg
     */
    private void analizerEcg(AliveCorEcg recordedEcg) {

        if (recordedEcg == null) {
            datos.setStatusDescarga(false);
            datos.setMsgfail(816,"ErrorECG","Resultado del ECG = null.");
            ViewResult();
            return;
        }

        //region :: Comprobación de resultado del ecg
        String kaiResult = validateRecording(recordedEcg);
        if (kaiResult == null) {
            datos.setStatusDescarga(false);
            datos.setMsgfail(817,"ErrorECG","Resultado del ECG no definido.");
            ViewResult();
            return;
        }
        //endregion

        getDeviceInfo(recordedEcg);
        diagnostico(recordedEcg);

        generatePdf("es", recordedEcg);
        datos.setStatusDescarga(true);
        ViewResult();
    }
    //endregion

    //endregion

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.imageButtonAudio) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            String frase = "";

            if (methodUI == EMethodUI.IntentUI) {
                frase = "Sostenga el dispositivo con los pulgares en contacto con los dos electrodos superiores, y sobre la piel de la pierna izquierda, " +
                        "ya sea en la rodilla o en la parte interna del tobillo.";
                texto.add(frase);

                frase = "Asegurándose que el símbolo esté orientado correctamente.";
                texto.add(frase);
            }
            else {
                frase = "Sostenga el dispositivo con los pulgares en contacto con los dos electrodos superiores, y sobre la piel de la pierna izquierda, " +
                        "ya sea en la rodilla o en la parte interna del tobillo.";
                texto.add(frase);

                frase = "Asegurándose que el símbolo esté orientado correctamente.";
                texto.add(frase);

                frase = "El registro comenzará en cuanto todos los electrodos detecten un buen contacto en la piel.";
                texto.add(frase);
            }

            textToSpeech.speak(texto);
            //endregion
        }
        else if (viewId == R.id.btEmpezarECG) {
            if (cTimerStart != null) {
                cTimerStart.cancel();
                cTimerStart = null;
                startAliveCor();
            }
        }
    }

    private void changeFondoRecording() {
        runOnUiThread(() -> {
                    findViewById(R.id.idfondoK6L).setBackground(ContextCompat.getDrawable(this, R.drawable.fondo_base_logo));
                });

        setVisibleView(findViewById(R.id.cdescarga_k6l), View.INVISIBLE);

        setVisibleView(findViewById(R.id.image_k6l), View.VISIBLE);
        setVisibleView(findViewById(R.id.txtmessage_k6l), View.VISIBLE);

        setVisibleView(txtContador, View.VISIBLE);
        setVisibleView(pbarContador, View.VISIBLE);
        setVisibleView(txtBPM, View.VISIBLE);
        setVisibleView(imgCorG, View.INVISIBLE);
        setVisibleView(imgCorP, View.VISIBLE);

    }
    private void imagetogle() {
        if (togle) {
            togle = false;
            setVisibleView(imgCorG, View.VISIBLE);
            setVisibleView(imgCorP, View.INVISIBLE);
        }
        else {
            togle = true;
            setVisibleView(imgCorG, View.INVISIBLE);
            setVisibleView(imgCorP, View.VISIBLE);
        }
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
            }
        });
    }

    //region :: MI RECEIVER
    private BluetoothReceiver bluetoothReceiver = null;
    private void receiverBle() {
        // Crear y registrar el BroadcastReceiver dinámicamente
        bluetoothReceiver = new BluetoothReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);

        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(bluetoothReceiver, filter);
    }

    // Definición del BroadcastReceiver
    private class BluetoothReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            Log.d(TAG, "=================>> Bluetooth apagado");
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            Log.d(TAG, "=================>> Bluetooth apagándose");
                            break;
                        case BluetoothAdapter.STATE_ON:
                            Log.d(TAG, "=================>> Bluetooth encendido");
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            Log.d(TAG, "=================>> Bluetooth encendiéndose");
                            break;
                    }
                    break;

                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    BluetoothDevice connectedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(TAG, "=================>> Dispositivo conectado");
                    break;

                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    BluetoothDevice disconnectedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(TAG, "=================>> Dispositivo desconectado");

                    Log.e(TAG, "STATUS K6L: " + status_k6l.toString());
                    Log.e(TAG, "Counter: " + counter.toString());
                    Log.e(TAG, "ProgressValue: " + progressStatus);

                    if (status_k6l == EStatusK6L.Recording) {
                        if (counter > 1) {
                            if (counter <= 21) {
                                EVLog.log(TAG, "ACTION_ACL_DISCONNECTED: Dispositivo ya no engancha de nuevo, pasado los 10 segundos primeros");
                                datos.setStatusDescarga(false);
                                datos.setMsgfail(818, "RecordError", "Usuario no ha hecho contacto de forma correcta con los electrodos. (disconnect)");
                                ViewResult();
                            }
                        }
                    }

                    break;

                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(TAG, "=================>> Dispositivo encontrado: " + foundDevice.getAddress());
                    break;

                default:
                    Log.d(TAG, "=================>> ACCION NO TRATADA: " + action);
                    break;
            }
        }
    }
    //endregion

    private void updateCounter() {
        runOnUiThread(new Runnable() {
            public void run() {
                setTextView(txtContador,counter.toString());
                pbarContador.setProgress(progressStatus);
            }
        });
    }

    public void ViewResult() {
        if (viewdata == false) {
            viewdata = true;

            Intent intent;
            if (datos.getStatusDescarga()) {
                EVLog.log(TAG, " ViewResult(view_dataKardia6L.class)");
                intent = new Intent(this, view_dataKardia6L.class);
            }
            else {
                EVLog.log(TAG, " ViewResult(view_failKardia6L.class)");

                try {
                    JSONObject msgfail = new JSONObject(datos.getMsgfail());
                    if (msgfail.has("details")) {
                        EVLog.log(TAG, "ERROR: " + msgfail.getString("details"));
                    }
                } catch (JSONException e) {}

                intent = new Intent(this, view_failKardia6L.class);
            }
            startActivity(intent);
            finish();
        }
        finish();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {

        EVLog.log(TAG, "onDestroy()");

        if (cTimerSearch != null) {
            cTimerSearch.cancel();
            cTimerSearch = null;
        }
        if (cTimerMeassure != null) {
            cTimerMeassure.cancel();
            cTimerMeassure = null;
        }

        if (cTimerStart != null) {
            cTimerStart.cancel();
            cTimerStart = null;
        }

        if (cTimer != null) {
            cTimer.cancel();
            cTimer = null;
        }

        textToSpeech.shutdown();

        if (recordEkgFragment != null) {

            if (ecgMonitorViewModel != null) {
                ecgMonitorViewModel = null;
            }

            recordEkgFragment.removeListener(this);
            recordEkgFragment = null;
        }


        if (bluetoothReceiver != null) {
            unregisterReceiver(bluetoothReceiver);
        }

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause()");
//        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        EVLog.log(TAG,"onResume() mAliveCorKitLiteInitialized: " + mAliveCorKitLiteInitialized);

        //region :: Comprobación: si la fecha que se inició el ensayo en la actual
        try {
            String contenido = FileAccess.leer(FilePath.DATE_TEST);
            String dateTestFile = util.getStringValueJSON(contenido,"datetest");
            String dateTest = FileAccess.DATE_FORMAT_TEST.format(new Date());
            EVLog.log(TAG, "FILEACCESS READ: dateTestFile: " + dateTestFile + ", dateTest: " + dateTest);

            if (dateTestFile.equals(dateTest) == false) {
                EnsayoLog.log(TAG,"", "ENSAYO ANTERIOR NO FINALIZADO");
                EVLog.log(TAG, "ENSAYO ANTERIOR NO FINALIZADO");
                startActivity(new Intent(this, Inicio.class));
                finish();
            }
        }
        catch (IOException ex) {
            EVLog.log("FILEACCESS READ", "IOException: " + ex.toString());
        }
        //endregion
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){
    }

    private void observeTasks() {
        ecgMonitorViewModel.task.observe(this, tasks -> {
            if (tasks != null) {
                Log.e("TASKS", "@@@@ TASKS: " + tasks.toString());
                /*
                enum Tasks (RECORDING_INITIALIZED, SHOW_UPLOAD_PROGRESS, POST_EVALUATION_RESULT,
                ON_RECORDING_ERROR, ON_USER_CANCELLED, ON_BT_PAIRING_CANCELLED, ON_CHANGE_DEVICE,
                ON_CURRENT_BPM_UPDATED;
                */
            }
        });
    }

}