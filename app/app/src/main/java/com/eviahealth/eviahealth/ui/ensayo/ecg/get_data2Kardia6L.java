package com.eviahealth.eviahealth.ui.ensayo.ecg;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.alivecor.api.AliveCorDetermination;
import com.alivecor.api.AliveCorDevice;
import com.alivecor.api.AliveCorEcg;
import com.alivecor.api.AliveCorKitLite;
import com.alivecor.api.EcgEvaluation;
import com.alivecor.api.InitListener;
import com.alivecor.api.LeadConfiguration;
import com.alivecor.api.RecordingConfiguration;
import com.alivecor.api.RecordingDeviceInfo;
import com.alivecor.ecg.record.AliveCorPdfHelper;
import com.alivecor.ecg.record.InvalidArgumentException;
import com.alivecor.ecg.record.RecordEkgFragment;
import com.alivecor.ecg.record.RecordEkgListener;
import com.alivecor.ecg.record.RecordingError;
import com.alivecor.universal_monitor.LeadValues;
import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.alivecor.AuthAlivecor;
import com.eviahealth.eviahealth.models.alivecor.data.KardiaData;
import com.eviahealth.eviahealth.api.alivecor.MethodsAlivecor;
import com.eviahealth.eviahealth.models.alivecor.models.ConfigurationK6L;
import com.eviahealth.eviahealth.models.alivecor.models.EDeterminationKAIv1;
import com.eviahealth.eviahealth.models.alivecor.models.EDeterminationKAIv2;
import com.eviahealth.eviahealth.models.alivecor.models.EMethodUI;
import com.eviahealth.eviahealth.models.alivecor.models.EStateK6L;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class get_data2Kardia6L extends BaseActivity implements View.OnClickListener, RecordEkgListener {

    //region :: Variables Activity
    private static final String TAG = "GET_DATA2-KARDIA6L";
    Context mContext;
    TextToSpeechHelper textToSpeech;
    TextView txtStatus;
    Button btEmpezarECG;

    CountDownTimer cTimerMaxTest = null;      // TIMEOUT >> Duración máxima establecida para la realización del test
    CountDownTimer cTimerSearch = null;       // TIMEOUT >> Espera inicialización del sdk
    CountDownTimer cTimerStart = null;       // PARA USO EN EMethodUI.IntentUI, para lazar interfaz
    CountDownTimer cTimer = null;            // Obtener TASK

    private static int TIMEOUT_MAX_TEST= 1000 * 60 * 6;  // 6 minutos
    private static final int MEASURE_INTERVAL = 60000;
    private static final int INICIALIZER_INTERVAL = 20000;
    private static final int WAIT_INTERVAL = 10000;
    EStatusK6L status_k6l = EStatusK6L.None;
    String idpaciente = null;
    Boolean viewdata = false;
    KardiaData datos;

    ConfigurationK6L configK6L = null;
    //endregion

    //region -- Kardia variables
    private boolean mAliveCorKitLiteInitialized = false;
    private RecordEkgFragment recordEkgFragment = null;
    private com.alivecor.ecg.record.EcgMonitorViewModel ecgMonitorViewModel = null;
    //endregion

    EStateK6L state = EStateK6L.IDLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_data2_kardia6_l);

        EVLog.log(TAG, "onCreate()");
        PermissionUtils.requestAll(this);

        mContext = get_data2Kardia6L.this;

        //region :: Views
        txtStatus = findViewById(R.id.txtStatusK6L);
        txtStatus.setTextSize(14);
        txtStatus.setVisibility(View.VISIBLE);

        setVisibleView(findViewById(R.id.fragment_container_ecg), View.INVISIBLE);

        btEmpezarECG = findViewById(R.id.btEmpezarECG);
        setVisibleView(btEmpezarECG, View.VISIBLE);
        //endregion

        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = KardiaData.getInstance();
        datos.setMethodUI(EMethodUI.RecordEkgFragment);

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
            if (obj.has("code")) {
                Log.e(TAG,"ERROR: " + obj.toString());
                datos.setStatusDescarga(false);
                String description = "Error al realizar comunicación con el servidor.\n" +
                        "    Causa: " + obj.getString("cause") + ".\n" +
                        "    Descripción: " + obj.getString("description");
                datos.setMsgfail(820,"API_EVIAHEALTH",description);
                ViewResult();
                return;
            }
            else if (obj.has("extra")) {
                JSONObject jextras = new JSONObject(obj.getString("extra"));
                EVLog.log(TAG, "JEXTRAS ECG: " + jextras.toString());
                configK6L = (jextras.has("k6l")) ? new ConfigurationK6L(jextras.getString("k6l")) : new ConfigurationK6L(null);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        //endregion

        Log.e(TAG,"Configuration KardiaMobile 6L: " + configK6L.toString());

        // Obtenemos si se ha inciado previmente el sdk de ALiveCor
        SharedPreferences prefs = mContext.getSharedPreferences("VariableEntorno", Context.MODE_PRIVATE);
        mAliveCorKitLiteInitialized = prefs.getBoolean("AliveCor", false);
        EVLog.log(TAG,"onCreate() mAliveCorKitLiteInitialized: " + mAliveCorKitLiteInitialized);

        // Espera de visualización de la pantalla inicial para que sepan como se debe de usar.-
        cTimerStart = new CountDownTimer(WAIT_INTERVAL, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                if (cTimerStart != null) {
                    cTimerStart.cancel();
                    cTimerStart = null;
                }
                EVLog.log(TAG,"TimeOut Timer > EMPEZAR");
                startAliveCor();
            }
        }.start();

        // Timer para que la inicialización del sdk responda.
        cTimerSearch = new CountDownTimer(INICIALIZER_INTERVAL, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                if (cTimerSearch != null) {
                    cTimerSearch.cancel();
                    cTimerSearch = null;
                }
                EVLog.log(TAG,"cTimerSearch TimeOut > SDK NO INICIADO, ERROR INESPERADO");
                datos.setStatusDescarga(false);
                datos.setMsgfail(823, "SdkFail", "TimeOut, Inicializando SDK, ERROR INESPERADO");
                ViewResult();
            }
        };

        // TIMEOUT >> Duración máxima establecida para la realización del test
        cTimerMaxTest = new CountDownTimer(TIMEOUT_MAX_TEST, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                if (cTimerMaxTest != null) {
                    cTimerMaxTest.cancel();
                    cTimerMaxTest = null;
                }
                EVLog.log(TAG,"cTimerMaxTest TimeOut > SUPERADO TIEMPO PARA LA REALIZACIÓN DEL TEST");
                datos.setStatusDescarga(false);
                datos.setMsgfail(824, "Error", "TimeOut, Superado tiempo para la realización del Test");
                ViewResult();
            }
        };
        cTimerMaxTest.start();
    }

    private void initializeTimer() {
        if (cTimer == null) {
            Integer timeout = this.MEASURE_INTERVAL * 2;
            cTimer = new CountDownTimer(timeout, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    LiveData d = ecgMonitorViewModel.state();
//                    Log.e(TAG,"@@@@ ecgMonitorViewModel.state(): " + d.getValue().toString());

                    String state_process_ecg = d.getValue().toString();

                    if (state_process_ecg.equals(state.toString()) == false) {
                        if (state_process_ecg.equals(EStateK6L.IDLE.toString())) {
                            state = EStateK6L.IDLE;
                        } else if (state_process_ecg.equals(EStateK6L.STARTING.toString())) {
                            state = EStateK6L.STARTING;
                        } else if (state_process_ecg.equals(EStateK6L.LISTENING.toString())) {
                            state = EStateK6L.LISTENING;
                        } else if (state_process_ecg.equals(EStateK6L.PRE_CAPTURING.toString())) {
                            state = EStateK6L.PRE_CAPTURING;
                        } else if (state_process_ecg.equals(EStateK6L.CAPTURING.toString())) {
                            state = EStateK6L.CAPTURING;
                        } else if (state_process_ecg.equals(EStateK6L.EVALUATING.toString())) {
                            state = EStateK6L.EVALUATING;
                        } else if (state_process_ecg.equals(EStateK6L.SAVING_DISK.toString())) {
                            state = EStateK6L.SAVING_DISK;
                        } else if (state_process_ecg.equals(EStateK6L.EVALUATION_NEED_CONFIRM.toString())) {
                            state = EStateK6L.EVALUATION_NEED_CONFIRM;
                        } else if (state_process_ecg.equals(EStateK6L.EVALUATION_READY.toString())) {
                            state = EStateK6L.EVALUATION_READY;
                        }
                        else if (state_process_ecg.equals(EStateK6L.ERROR.toString())) {
                            state = EStateK6L.ERROR;
                            EVLog.log(TAG,"ERROR: Se ha producido un error. No detectado ningún dispositivo.");
                        }
                        EVLog.log(TAG,"State process ecg: " + state_process_ecg);
                    }

//                    if (d.getValue().toString().equals("ERROR")) {
//                        EVLog.log(TAG,"ERROR: Se ha producido un error. No detectado ningún dispositivo.");
//                    }
//                    else
                    if (d.getValue().toString().equals("EVALUATION_NEED_CONFIRM")) {
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
                    Log.e(TAG,"Timer State process ecg RESTART");
                    cTimer.start();
                }
            }.start();
        }
    }

    //region :: KARDIA
    private void startAliveCor() {

        // Check if mAliveCorKit already Initialized
        try {
            if (AliveCorKitLite.get() != null) {
                EVLog.log(TAG,"startAliveCor(): AliveCorKit ya está inicializado");
                mAliveCorKitLiteInitialized = true;
            }
        } catch (IllegalStateException exception) {
            EVLog.log(TAG,"startAliveCor(): AliveCorKit aún no está inicializado.");
            mAliveCorKitLiteInitialized = false;
        }

        if (mAliveCorKitLiteInitialized == false) {
            JSONObject jwt = MethodsAlivecor.getJWT_AlivecorDocker(idpaciente);
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
//        methodUI == EMethodUI.RecordEkgFragment)
        changeFondoRecording(); // Oculta views y muestra frame
        startCustomRecording(); // utilizando RecordEkgFragment invisible en ui custom
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

            cTimerSearch.start();
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

            EVLog.log(TAG,"Lanza AliveCorKitLite.initialize()");
            // Inicializa el SDK AliveCorKitLite
            AliveCorKitLite.initialize(
                    mContext,  // Contexto
                    apiKey,
                    new InitListener() {
                        @Override
                        public void onInitComplete() {
                            try {

                                EVLog.log(TAG, "SDK AliveCorKitLite inicializado correctamente. Version(" + AliveCorKitLite.getVersion() + ")");
                                List<AliveCorDevice> devices = AliveCorKitLite.get().getSupportedDevices();

                                if (devices.size() <= 0) {
                                    EVLog.log(TAG, "LISTADO DE DISPOSITIVOS PERMITIDOS: VACIA");
                                    datos.setStatusDescarga(false);
                                    datos.setMsgfail(819, "SdkFail", "Lista de dispositivos aceptados por el sdk vacía.");
                                    ViewResult();
                                } else {
                                    EVLog.log(TAG, "LISTADO DE DISPOSITIVOS PERMITIDOS: " + devices.toString());

                                    status_k6l = EStatusK6L.SdkInitialized;
                                    mAliveCorKitLiteInitialized = true;
                                    cTimerSearch.cancel();
                                    SharedPreferences prefs = mContext.getSharedPreferences("VariableEntorno", Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putBoolean("AliveCor", mAliveCorKitLiteInitialized);
                                    editor.commit();

                                    methodRecorgingUI();
                                }
                            }
                            catch (Exception e) {
                                EVLog.log(TAG, "Exception AliveCorKitLite.initialize onInitComplete(): " + e.getMessage());
                                datos.setStatusDescarga(false);
                                datos.setMsgfail(821, "SdkFail", "Exception AliveCorKitLite.initialize onInitComplete()");
                                ViewResult();
                            }
                        }

                        @Override
                        public void onInitError(Throwable throwable) {
                            try {
                                status_k6l = EStatusK6L.SdkFail;
                                mAliveCorKitLiteInitialized = false;
                                EVLog.log(TAG, "******  ERROR al inicializar el SDK AliveCorKitLite.");
                                EVLog.log(TAG, "onInitError(): " + throwable.getMessage());
                                datos.setStatusDescarga(false);
                                datos.setMsgfail(810, "SdkFail", "ERROR al inicializar el SDK AliveCorKitLite.");
                                ViewResult();
                            }
                            catch (Exception e) {
                                EVLog.log(TAG, "Exception AliveCorKitLite.initialize onInitError(): " + e.getMessage());
                                datos.setStatusDescarga(false);
                                datos.setMsgfail(822, "SdkFail", "Exception AliveCorKitLite.initialize onInitError()");
                                ViewResult();
                            }
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

    private void startCustomRecording() {
        runOnUiThread(() -> {
            try {
                // Configuración personalizada
                RecordingConfiguration configuration = new RecordingConfiguration();
                configuration.setDevice(AliveCorDevice.TRIANGLE);   // Configura el dispositivo KardiaMobile 6L

                configuration.setLeads(configK6L.getLeadConfiguration());   // Configuración de derivaciones (SINGLE/SIX)
                configuration.setFilterType(configK6L.getFilterType());     // Aplica filtro para ruido (ENHANCED/ORIGIN)
                configuration.setMaxDurationSeconds(configK6L.getMaxDuration());     // Duración máxima grabación del ecg 30 segundos por defecto
                configuration.setResetDurationSeconds(configK6L.getResetDuration()); // Tiempo de reinicio si pierde contacto 10 seg por defecto
                configuration.setMainsFrequency(configK6L.getMainsFrequency());      // Frecuencia de red (50Hz/60Hz)
                configuration.setEnableLeadsButtons(configK6L.getEnableLeadsButtons()); // Des/habilita botón para que el paciente cambie el número de derivaciones

                // Crear instancia del fragmento de grabación
                recordEkgFragment = RecordEkgFragment.newInstance(configuration);

                // Establece listener de grabación
                recordEkgFragment.addListener(this);

                status_k6l = EStatusK6L.Scanning;
                setTextView(txtStatus, status_k6l.toString());

                // Mostrar el fragmento en el contenedor de la UI
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_ecg, recordEkgFragment)
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

    //region ::  RecordEkgListener: Listener de eventos de la grabación de datos
    @Override
    public void onRecordCompleted(AliveCorEcg result) {
        EVLog.log(TAG, "****** onRecordCompleted(): Grabación completada con éxito");

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
                Log.e(TAG,"Batería baja en el dispositivo KardiaMobile 6L.");
                break;
            case MIC_PERMISSION_EKG:
                nerr = 4;
                message = "Permiso de micrófono requerido. Solicítalo al usuario. (MIC_PERMISSION_EKG)";
                Log.e(TAG,"Permiso de micrófono requerido. Solicítalo al usuario.");
                break;
            default:
                nerr = 5;
                message = error.name();
                Log.e(TAG,"Error durante la grabación: " + error.name());
        }

        datos.setStatusDescarga(false);
        datos.setMsgfail(nerr,"RecordError",message);
        ViewResult();
    }

    @Override
    public void onUserCancel() {
        EVLog.log(TAG, "****** onUserCancel()");
        EVLog.log(TAG, "RESULT_CANCELED: Operación cancelada por el paciente.");
        datos.setStatusDescarga(false);
        datos.setMsgfail(813,"Activity","Operación cancelada por el paciente.");
        ViewResult();
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
//        Log.i(TAG,"Configuración de derivaciones actualizada: %s", config.name());
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

//      Log.d(TAG "Valores actuales del ECG: " + latest.toString());
    }

    @Override
    public void onBPMUpdated(String bpm) {
        // Actualización de la frecuencia cardíaca
        EVLog.log(TAG, "onBPMUpdated(): status_k6l(" + status_k6l.toString() + "), bpm(" + bpm + ")");

        if (status_k6l == EStatusK6L.Detected) {
            status_k6l = EStatusK6L.WaitBPM;
        }
        else if (status_k6l == EStatusK6L.WaitBPM) {
            if (bpm.contains("---") == false) {
                status_k6l = EStatusK6L.Recording;
            }
        }
        else if (status_k6l == EStatusK6L.Recording) {
        }

    }

    @Override
    public void onDeviceId(String deviceId) {
        EVLog.log(TAG, "****** onDeviceId(): dispositivo conectado(" + deviceId + ")");

        status_k6l = EStatusK6L.Detected;
        setTextView(txtStatus, status_k6l.toString());
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

            // Mostrar al usuario
            Log.e(TAG, "----- AliveCorDetermination: " + determination.getSerializedName());
            Log.e(TAG, "----- kaiResult: " + kaiResult);
            Log.e(TAG, "----- kardiaAiVersion: " + kardiaAiVersion);
            Log.e(TAG, "----- Resultado: " + resultText);
            Log.e(TAG, "----- Detalles: " + resultDescription);
            Log.e(TAG, "----- " + heartRateDisplay);
            Log.e(TAG, "----- " + inversionText);

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
        Log.e(TAG,"*** Paciente cargado: " + paciente.toString());
        if (paciente.getBydefault()) {
            EVLog.log(TAG,"Datos de paciente cargados con valores por defecto");
            isCharacteristics = false;
        }

        age = (long)paciente.getAge();
//        long dateOfBirth = System.currentTimeMillis() - (age * 365 * 24 * 60 * 60 * 1000); // Fecha de nacimiento aproximada
        long dateOfBirth = util.obtenerTimeMillisDesdeFecha(paciente.getBirthday());
        EVLog.log(TAG, "paciente.getBirthday(): " + paciente.getBirthday());
        EVLog.log(TAG, "long dateOfBirth: " + dateOfBirth);
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
//        notes = notes + "\n\n" + notes2;

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

    private void generate2Pdf(String pdfLanguage, AliveCorEcg recordedEcg) {

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

        String patientId = idpaciente;    // ID del paciente
        boolean isCharacteristics = true;
        Patient paciente = ApiMethods.loadCharacteristics(idpaciente);
        Log.e(TAG,"*** Paciente cargado: " + paciente.toString());
        if (paciente.getBydefault()) {
            EVLog.log(TAG,"Datos de paciente cargados con valores por defecto");
            isCharacteristics = false;
        }

        long dateOfBirth = util.obtenerTimeMillisDesdeFecha(paciente.getBirthday());
        EVLog.log(TAG, "paciente.getBirthday(): " + paciente.getBirthday());
        EVLog.log(TAG, "long dateOfBirth: " + dateOfBirth);
        String gender = (paciente.getGender().equals("Hombre"))? "M" : "F";

        AliveCorPdfHelper.PatientInfo patientInfo = new AliveCorPdfHelper.PatientInfo(firstName, lastName, dateOfBirth, patientId, gender);

        String tags = "---";             // Etiquetas (opcional)
        //region :: Notas (opcional)
        String notes = (datos.getDetailsECG() != null) ? datos.getDetailsECG() : "";
        String notes2 = (datos.getResultScreenDeterminationInfo() != null) ? datos.getResultScreenDeterminationInfo() : "";
        notes = notes + "\n\n" + notes2;

        if ((!isPatinet) || (!isCharacteristics)) {
            notes = notes + "\n\n" + "No ha sido posible acceder a la información del paciente en el momento de la generación de este documento. Por este motivo, se han utilizado valores por defecto para completar los datos del paciente en el documento.";
        }
        //endregion

        // PDF Reports can be generated into any file.  The SDK will overwrite any data in this file
        File pdfFile = new File(FileAccess.getPATH_FILES(), "pdfs/" + recordedEcg.getUuid() + ".pdf");

        AliveCorPdfHelper pdfHelper = AliveCorKitLite.get().getPdfHelper();
        pdfHelper.setForceInvert(false);

        if (pdfLanguage != null) {
            pdfHelper.setLanguage(pdfLanguage);
        }
        pdfHelper.createPdfWithEncryptionPrompt(this,
                recordedEcg,
                AliveCorPdfHelper.Size.A_4,
                pdfFile,
                null,
                patientInfo,
                null,
                notes,
                new AliveCorPdfHelper.PdfListener() {
                    private void showPdf(File file) {
                    }

                    @Override
                    public void onEncryptPdf(@NonNull File file, @NonNull String s) {
                    }

                    @Override
                    public void onShowPdf(@NonNull File file) {
                        showPdf(file);
                    }

                    @Override
                    public void onError() {

                    }
                }, false, false);
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

            frase = "Sostenga el dispositivo con los pulgares en contacto con los dos electrodos superiores, y sobre la piel de la pierna izquierda, " +
                    "ya sea en la rodilla o en la parte interna del tobillo.";
            texto.add(frase);

            frase = "Asegurándose que el símbolo esté orientado correctamente.";
            texto.add(frase);

            textToSpeech.speak(texto);
            //endregion
        }
        else if (viewId == R.id.btEmpezarECG) {
            if (cTimerStart != null) {
                EVLog.log(TAG,"Pulsado EMPEZAR");
                cTimerStart.cancel();
                cTimerStart = null;
                btEmpezarECG.setEnabled(false);
                startAliveCor();
            }
        }
    }

    private void changeFondoRecording() {
        runOnUiThread(() -> {
            //findViewById(R.id.idfondoK6L).setBackground(ContextCompat.getDrawable(this, R.drawable.fondo_base_logo));
            setVisibleView(findViewById(R.id.fragment_container_ecg), View.VISIBLE);
        });

        setVisibleView(btEmpezarECG, View.INVISIBLE);
        setVisibleView(txtStatus, View.INVISIBLE);
        setVisibleView(findViewById(R.id.imageButtonAudio), View.INVISIBLE);

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

        if (cTimerMaxTest != null) {
            cTimerMaxTest.cancel();
            cTimerMaxTest = null;
        }

        if (cTimerSearch != null) {
            cTimerSearch.cancel();
            cTimerSearch = null;
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

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause()");
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
}