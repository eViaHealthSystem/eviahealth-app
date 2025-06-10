package com.eviahealth.eviahealth.ui.ensayo.bascula.hs2s;

import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.models.ihealth.hs2s.DataBascula;
import com.eviahealth.eviahealth.models.ihealth.iHQuerySetting;
import com.eviahealth.eviahealth.bluetooth.models.EStatusDevice;
import com.eviahealth.eviahealth.models.devices.Dispositivo;
import com.eviahealth.eviahealth.devices.NombresDispositivo;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;
import com.eviahealth.eviahealth.models.vitalograph.Patient;
import com.ihealth.communication.control.Hs2sControl;
import com.ihealth.communication.control.Hs2sProfile;
import com.ihealth.communication.manager.DiscoveryTypeEnum;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class get_dataHS2S extends BaseActivity implements View.OnClickListener {

    final String TAG = "HS2S-DATA";
    final String FASE = "BÁSCULA HS2S";
    final String TypeDevice = "HS2S";
    final int REINTENTOS = 2;
    private Hs2sControl mHS2SControl;
    private int mClientCallbackId;
    private SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
    EStatusDevice statusDevice= EStatusDevice.None;
    iHQuerySetting queryStatus= iHQuerySetting.None;
    String mDeviceMac = null; // HS2S-004D3212FD3E;
    ProgressBar circulodescarga;
    Button btContinuar;
    TextView txtStatus;
    TextToSpeechHelper textToSpeech;
    private int numToSpeech = 1;
    CountDownTimer cTimer = null;           // TimeOut para que se realice la medición de peso
    CountDownTimer cTimerOut = null;        // TimeOut entre scaneo de bluetooth por sino salta el de iHealth
    CountDownTimer cTimerContinuar = null;  // Timer para mostrar con retardo el button de continuar
    CountDownTimer cTimerProgress = null;  // Timer para mostrar el Circulo de Progreso inicial

    DataBascula datos;
    Boolean audio_on = false;
    int cont_scan = 0;
    Boolean viewdata = false;           // Para evitar que se lance dos veces la siguiente actividad

    private Integer user_info_count = null;
    private Integer user_count = null;
    private String user_id = "abcdef1234567890";
    private Boolean impedance = false; // true = Mide grasa corporal, false = Solo peso
    private Patient paciente;
    private Double peso = 0.0;
    private int height = 0;
    private Integer contHistoryData = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_data_hs2s);

        EVLog.log(TAG, " onCreate()");

        //region :: views
        txtStatus = findViewById(R.id.txtStatus_hs2s);
        txtStatus.setTextSize(14);
        setVisibleView(txtStatus,View.VISIBLE);

        circulodescarga = findViewById(R.id.circulodescarga);
        setVisibleView(circulodescarga,View.INVISIBLE);

        btContinuar = findViewById(R.id.btcontinuar_bas);
        setVisibleView(btContinuar,View.INVISIBLE);

        //endregion

        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = DataBascula.getInstance();
        datos.clear();

        viewdata = false;

        //region :: Carga datos del paciente de la DB
//        String idpaciente = Config.getInstance().getIdPacienteTablet();
        String idpaciente = Config.getInstance().getIdPacienteEnsayo();
        paciente = ApiMethods.loadCharacteristics(idpaciente);
        datos.setHeight(paciente.getHeight());
        if (paciente.getBydefault()) {
            EVLog.log(TAG,"Datos de paciente cargados con valores por defecto");
        }
        //endregion

        String DEVICE_MAC_ADDRESS = Config.getInstance().getIdentificador(NombresDispositivo.BASCULA);
        mDeviceMac = DEVICE_MAC_ADDRESS.replace("HS2S-", "");
        EVLog.log(TAG,"MAC BASCULA: "+ DEVICE_MAC_ADDRESS);

        //region :: Cargar extras establecidos al dispositivo
        Map<NombresDispositivo, Dispositivo> dispositivos = Config.getInstance().getDispositivos();
        Dispositivo equipo = dispositivos.get(NombresDispositivo.BASCULA);
        try {
            JSONObject jextras = new JSONObject(equipo.getExtra());

            if (jextras.has("code")) {
                Log.e(TAG,"ERROR: " + jextras.toString());
                datos.setStatusDescarga(false);
                String description = "Error al realizar comunicación con el servidor.\n" +
                        "    Causa: " + jextras.getString("cause") + ".\n" +
                        "    Descripción: " + jextras.getString("description");
                EVLog.log(TAG,description);
                datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":808,\"description\":\".\"}");
                viewResult();
                return;
            }
            else if (jextras.has("hs2s")) {
                JSONObject hs2s = jextras.getJSONObject("hs2s");
                impedance = hs2s.getBoolean("impedance");
                EVLog.log(TAG, "JEXTRAS HS2S: " + hs2s.toString());
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        //endregion

        // register ihealthDevicesCallback id
        mClientCallbackId = iHealthDevicesManager.getInstance().registerClientCallback(miHealthDevicesCallback);
        // Limited wants to receive notification specified device
        iHealthDevicesManager.getInstance().addCallbackFilterForDeviceType(mClientCallbackId, iHealthDevicesManager.TYPE_HS2S, iHealthDevicesManager.TYPE_HS2SPRO);

        // Busca dispositivos tipo HS2S
        statusDevice = EStatusDevice.Scanning;
        iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.HS2S);
        setTextView(txtStatus, statusDevice.toString());

        // TimeOut entre scaneo de bluetooth por sino salta el de iHealth
        cTimerOut = new CountDownTimer(1000*30, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimerOut.cancel();
                cTimerOut = null;
                EVLog.log(TAG, "SUPERADO EL TIEMPO MÁXIMO DE SCANEADO");
                datos.setStatusDescarga(false);
                datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":800,\"description\":\".\"}");
                viewResult();
            }
        };
        cTimerOut.start();

        int timeout = (int)(1000 * 120); // TimeOut 120 segundos para realizar la medición desde el borrado de histórico
        cTimer = new CountDownTimer(timeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimer.cancel();
                cTimer = null;
                EVLog.log(TAG, "OCURRIDO TIMEOUT EN LA REALIZACIÓN DE MEDICIÓN");
                datos.setStatusDescarga(false);
                datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":804,\"description\":\"No se ha realizado medición.\"}");
                viewResult();
            }
        };

        // Timer para mostrar el botón de contuniar
        cTimerContinuar = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimerContinuar.cancel();
                cTimerContinuar = null;
                setVisibleView(btContinuar,View.VISIBLE);
            }
        };

        // Timer para mostrar el Circulo de Progreso
        cTimerProgress = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimerProgress.cancel();
                cTimerProgress = null;
                setVisibleView(circulodescarga,View.VISIBLE);
            }
        };
        cTimerProgress.start();
    }

    private iHealthDevicesCallback miHealthDevicesCallback = new iHealthDevicesCallback() {

        @Override
        public void onScanDevice(String mac, String deviceType, int rssi, Map manufactorData) {

            EVLog.log(TAG, "iHealth onScanDevice() >> mac[" + mac + "], deviceType[" + deviceType + "]");

            if (mac.equals(mDeviceMac)) {
                EVLog.log(TAG, "iHealth onScanDevice() Device " +deviceType +" [" + mac + "] detected");
                iHealthDevicesManager.getInstance().stopDiscovery();
                statusDevice = EStatusDevice.Detected;

                if (cTimerOut != null) { cTimerOut.cancel(); }
            }
        }

        @Override
        public void onScanFinish() {
            super.onScanFinish();

            // Salta cuando se envia stopDiscovery(); o salta timeout de conexión de ihealth

            EVLog.log(TAG, "iHealth onScanFinish()");
            iHealthDevicesManager.getInstance().stopDiscovery();
            iHealthDevicesManager.getInstance().disconnectAllDevices(true);

            Log.e(TAG,"statusDevice: " + statusDevice);

            if (statusDevice == EStatusDevice.Detected) {
                // DISPOSITIVO DETECTADO
                EVLog.log(TAG, "iHealth onScanFinish() >> " + TypeDevice + " DETECTADO");

                // Conectado con el dispositivo
                if (iHealthDevicesManager.getInstance().isDiscovering()){
                    EVLog.log(TAG, "iHealth isDiscovering() = true");
                    iHealthDevicesManager.getInstance().stopDiscovery();
                    SystemClock.sleep(1000);
                }
                cont_scan = 0;
                statusDevice = EStatusDevice.Connecting;
                iHealthDevicesManager.getInstance().connectDevice( mDeviceMac, TypeDevice);
                setTextView(txtStatus,"" + statusDevice);

            }
            else {
                // DISPOSITIVO NO ENCONTRADO
                cont_scan += 1;
                if (cont_scan <= REINTENTOS){
                    EVLog.log(TAG, "iHealth onScanFinish() >> NO ENCONTRADO REINTENTAMOS SCAN (" + Integer.toString(cont_scan)+")");
                    // reintentamos scanearlo
                    statusDevice= EStatusDevice.Scanning;
                    iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.HS2S);
                    setTextView(txtStatus,"" + statusDevice + " [" + cont_scan + "]");
                    cTimerOut.cancel();
                    cTimerOut.start();
                }
                else {
                    EVLog.log(TAG, "iHealth onScanFinish() >> NO SCANEADO EL DISPOSITIVO BÁSCULA (" + Integer.toString(cont_scan)+")");
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":800,\"description\":\"No detectado dispositivo.\"}");
                    viewResult();
                }
            }

        }

        @Override
        public void onDeviceConnectionStateChange(String mac, String deviceType, int status, int errorID) {
            EVLog.log(TAG,"onDeviceConnectionStateChange() >> deviceType: " + deviceType + ", mac: " + mac + ", status: " + status);

            if (status == iHealthDevicesManager.DEVICE_STATE_CONNECTED) {

                EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTED");
                statusDevice = EStatusDevice.Connected;
                setTextView(txtStatus,"" + statusDevice);

                try {
                    // Get HS2S controller
                    mHS2SControl = iHealthDevicesManager.getInstance().getHs2sControl(mDeviceMac);

                    // Solicitamos información del usuario incluye estado batería, este método sincroniza la hora
                    queryStatus = iHQuerySetting.DeviceInfo;
                    setTextView(txtStatus,">> " + queryStatus);
                    mHS2SControl.getDeviceInfo();
//                    cTimer.start();

                }catch (Exception e){
                    EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTED >> Exception getDeviceInfo()");
                    statusDevice = EStatusDevice.Failed;

                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                    viewResult();
                }
            }

            if (status == iHealthDevicesManager.DEVICE_STATE_DISCONNECTED) {
                mHS2SControl = null;
                EVLog.log(TAG, "iHealth DEVICE_STATE_DISCONNECTED");
                statusDevice = EStatusDevice.Disconnect;
                setTextView(txtStatus,"" + statusDevice);
                setTextView(txtStatus,":: " + queryStatus);

            }

            if (status == iHealthDevicesManager.DEVICE_STATE_CONNECTIONFAIL) {
                EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTIONFAIL >> ERRORID: " + errorID);

                setTextView(txtStatus,statusDevice.toString());
                Log.e(TAG,"********************* " + statusDevice.toString());

                if (statusDevice == EStatusDevice.Connecting) {
                    cont_scan += 1;
                    if (cont_scan <= REINTENTOS){
                        EVLog.log(TAG, "iHealth onScanFinish() >> ERROR CONECTANDO, REINTENTAMOS (" + Integer.toString(cont_scan)+")");
                        // reintentamos scanearlo
                        statusDevice= EStatusDevice.Scanning;
                        iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.HS2S);
                        setTextView(txtStatus,"" + statusDevice + " [" + cont_scan + "]");
                        cTimerOut.cancel();
                        cTimerOut.start();
                    }
                    else {
                        statusDevice = EStatusDevice.Failed;
                        EVLog.log(TAG, "iHealth onScanFinish() >> NO SE HA PODIDO CONECTAR (" + Integer.toString(cont_scan)+")");
                        datos.setStatusDescarga(false);
                        datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":800,\"description\":\"No se ha podido conectar con el dispositivo.\"}");
                        viewResult();
                    }
                }
                else {
                    statusDevice = EStatusDevice.Failed;
                    EVLog.log(TAG, "iHealth onScanFinish() >> NO SE HA PODIDO CONECTAR (" + Integer.toString(cont_scan)+")");
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":800,\"description\":\"No se ha podido conectar con el dispositivo.\"}");
                    viewResult();
                }
            }

            if (status == iHealthDevicesManager.DEVICE_STATE_RECONNECTING) {
                EVLog.log(TAG, "iHealth DEVICE_STATE_RECONNECTING");
                statusDevice = EStatusDevice.Reconnecting;
            }

        }

        @Override
        public void onScanError(String reason, long latency) {
            Log.e(TAG,"onScanError() >> reason: " + reason + ", latency: " + latency);
        }

        @Override
        public void onSDKStatus(int statusId, String statusMessage) {
            Log.e(TAG,"onSDKStatus() >> statusId: " + statusId + ", statusMessage: " + statusMessage);
        }

        @Override
        public void onUserStatus(String username, int userStatus) {
            Log.e(TAG,"onUserStatus() >> username: " + username + ", userState: " + userStatus);
        }

        @Override
        public void onDeviceNotify(String mac, String deviceType, String action, String message) {
            super.onDeviceNotify(mac, deviceType, action, message);
//            Log.e(TAG, "onDeviceNotify() >> deviceType: " + deviceType + ", mac:" + mac + ", action:" + action + ", message" + message);
            EVLog.log(TAG, "onDeviceNotify() >> action: " + action + ", message: " + message);

            if (Hs2sProfile.ACTION_GET_DEVICE_INFO.equals(action)) {
                // {"user_count":0,"unit_current":1,"battery":100}
                // {"user_count":1,"unit_current":1,"battery":100}
                setTextView(txtStatus,"<< " + queryStatus);
                try {
                    JSONObject obj = new JSONObject(message);

                    user_count = obj.getInt(Hs2sProfile.HS_USER_COUNT);
                    datos.setBattery(message);

                    queryStatus = iHQuerySetting.UserInfo;
                    setTextView(txtStatus,">> " + queryStatus.getQuerySetting());
                    mHS2SControl.getUserInfo();

                } catch (JSONException e) {
                    e.printStackTrace();
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                    viewResult();
                }
            }
            else if (Hs2sProfile.ACTION_GET_USER_INFO.equals(action)) {
                // {"user_info_count":0,"user_info_array":[]}
                // {"user_info_count":1,"user_info_array":[{"user_id":"abcdef1234567890","create_time":1701424594,"weight":"98.02","gender":1,"age":71,"height":172,"impedance":1,"body_building":0,"body_form":1}]}
                setTextView(txtStatus,"<< " + queryStatus);
                try {
                    JSONObject obj = new JSONObject(message);
                    user_info_count = obj.getInt(Hs2sProfile.USER_INFO_COUNT);

                    if (user_info_count > 0) {
                        // existe usuario
                        String info = getUserInfoId(message,0);
                        EVLog.log(TAG,"info user: " + info);

                        user_id = util.getStringValueJSON(info,"user_id");
                        peso = util.getDoubleValueJSON(info,"weight").doubleValue();
                        int weight = (int)util.getDoubleValueJSON(info,"weight").doubleValue();
                        height = util.getIntValueJSON(info,"height");
                        Boolean grasaCorporal = (util.getIntValueJSON(info,"impedance") == 0) ? false : true;
                        String gender = (util.getIntValueJSON(info,"gender") == 0) ? "Mujer" : "Hombre";

                        if (!paciente.getGender().equals(gender) || paciente.getHeight() != height || impedance != grasaCorporal) {
                            Toast.makeText(getApplicationContext(), "La báscula no tiene la misma configuración que los datos establecidos.", Toast.LENGTH_LONG).show();
                            EVLog.log(TAG, "La báscula no tiene la misma configuración que los datos establecidos.");
                            Log.e(TAG,"gender: " + gender + ", height: " + height + ", impedance: " + grasaCorporal);
                            Log.e(TAG,"paciente.gender: " + paciente.getGender() + ", paciente.height: " + paciente.getHeight() + ", impedance: " + impedance);
                            if (!paciente.getGender().equals(gender)) { Log.e(TAG,"gende DISTINTO"); }
                            if (paciente.getHeight() != height) { Log.e(TAG,"height DISTINTO"); }
                            if (impedance != grasaCorporal) { Log.e(TAG,"impedance DISTINTO"); }
                        }

                    }

                    // Establecemos la Unidad a KG
                    queryStatus = iHQuerySetting.Unit;
                    setTextView(txtStatus,">> " + queryStatus.getQuerySetting());
                    mHS2SControl.setUnit(Hs2sProfile.UNIT_KG);

                } catch (JSONException e) {
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"error inesperado.\"}");
                    viewResult();
                }
            }
            else if (Hs2sProfile.ACTION_SET_UNIT_SUCCESS.equals(action)) {
                // message = null
                setTextView(txtStatus,"<< " + queryStatus);

                int gender = (paciente.getGender().equals("Hombre")) ? 1:0;
                Integer masacorporal = (impedance) ? 1:0;

                if (user_count == 0) {
                    // No hay usuario creado
                    queryStatus = iHQuerySetting.CreateUserInfo;
                    setTextView(txtStatus,">> " + queryStatus.getQuerySetting());

                    // Crea un usuario por defecto (Hombre,70 Kg, 170 cm, medición solo peso)
                    mHS2SControl.createOrUpdateUserInfo(user_id, paciente.getWeight().floatValue(),gender,paciente.getAge(),paciente.getHeight(),masacorporal,0);
                }
                else {
                    // Existe usuario creado >> Activamos que usuario
                    queryStatus = iHQuerySetting.SpecifyOnlineUsers;
                    setTextView(txtStatus,">> " + queryStatus.getQuerySetting());

                    mHS2SControl.specifyOnlineUsers(user_id,peso.floatValue(),gender,paciente.getAge(),paciente.getHeight(),masacorporal,0);
                }
            }
            else if (Hs2sProfile.ACTION_CREATE_OR_UPDATE_USER_INFO.equals(action)) {
                // {"status":0,"describe":"Successful"}
                setTextView(txtStatus,"<< " + queryStatus);
                try {
                    JSONObject obj = new JSONObject(message);
                    Integer status = obj.getInt(Hs2sProfile.OPERATION_STATUS);

                    if (status == 0) {
                        // Usuario creado/actualizado correctamente
                        if (queryStatus == iHQuerySetting.CreateUserInfo) {
                            // Usuario creado por defecto
                            queryStatus = iHQuerySetting.SpecifyOnlineUsers;
                            setTextView(txtStatus,">> " + queryStatus.getQuerySetting());

                            int gender = (paciente.getGender().equals("Hombre")) ? 1:0;
                            Integer masacorporal = (impedance) ? 1:0;
                            mHS2SControl.specifyOnlineUsers(user_id,paciente.getWeight().floatValue(),gender,paciente.getAge(),paciente.getHeight(),masacorporal,0);
                        }
                        else if (queryStatus == iHQuerySetting.UpdateUserInfo) {
                            // Usuario actualizado con los valores medidos
                            queryStatus = iHQuerySetting.Fisnish;
                            setTextView(txtStatus,">> " + queryStatus.getQuerySetting());
                            datos.setStatusDescarga(true);
                            viewResult();
                        }
                        else {
                            EVLog.log(TAG,"ERROR INESPETADO, NO DEBERÍAS SER POSIBLE");
                            datos.setStatusDescarga(false);
                            datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                            viewResult();
                        }
                    }
                    else {
                        EVLog.log(TAG, "CREATE_OR_UPDATE_USER_INFO: status: " + status.toString() + ", description: " + obj.getString(Hs2sProfile.OPERATION_DESCRIBE));
                        datos.setStatusDescarga(false);
                        datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                        viewResult();
                    }

                } catch (JSONException e) {
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                    viewResult();
                }
            }
            else if (Hs2sProfile.ACTION_SPECIFY_USERS.equals(action)) {
                // {"status":0,"describe":"Successful"}
                setTextView(txtStatus,"<< " + queryStatus);
                try {
                    // {"status":0,"describe":"Successful"}
                    JSONObject obj = new JSONObject(message);
                    Integer status = obj.getInt(Hs2sProfile.OPERATION_STATUS);

                    if (status == 0) {
                        queryStatus = iHQuerySetting.OfflineData;
                        setTextView(txtStatus, ">> " + queryStatus.getQuerySetting());
                        mHS2SControl.getOfflineData(user_id);
                    }
                    else {
                        String description = "";
                        if (obj.has(Hs2sProfile.OPERATION_DESCRIBE)) { description = obj.getString(Hs2sProfile.OPERATION_DESCRIBE); }

                        EVLog.log(TAG, "ACTION_SPECIFY_USERS: status: " + status.toString() + ", description: " + description);
                        datos.setStatusDescarga(false);
                        datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                        viewResult();
                    }

                } catch (JSONException e) {
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                    viewResult();
                }
            }
            else if (Hs2sProfile.ACTION_HISTORY_DATA.equals(action)) {
//                message: [] sin datos
//                message: [{"dataID":"004D3212FD3E17014273539800000000","weight":"97.83","impedance":[{"impedance":515},{"impedance":444},{"impedance":420},{"impedance":396},{"impedance":1},{"impedance":18348},{"impedance":25961},{"impedance":47257}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701427353,"right_time":1,"body_building":0,"instruction_type":1,"body_fit_percentage":"33.1","muscle_mas":"61.4","bone_salt_content":"4.0","body_water_rate":"49.0","protein_rate":"13.8","visceral_fat_grade":"20","physical_age":"68"},{"dataID":"004D3212FD3E17014273829800000000","weight":"97.83","impedance":[{"impedance":515},{"impedance":444},{"impedance":1243},{"impedance":0},{"impedance":1},{"impedance":18348},{"impedance":25961},{"impedance":47286}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701427382,"right_time":1,"body_building":0,"instruction_type":0},{"dataID":"004D3212FD3E17014273989800000000","weight":"97.81","impedance":[{"impedance":513},{"impedance":442},{"impedance":416},{"impedance":1295},{"impedance":1},{"impedance":18348},{"impedance":25961},{"impedance":47302}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701427398,"right_time":1,"body_building":0,"instruction_type":0},{"dataID":"004D3212FD3E17014274199800000000","weight":"97.79","impedance":[{"impedance":512},{"impedance":440},{"impedance":415},{"impedance":391},{"impedance":1},{"impedance":18348},{"impedance":25961},{"impedance":47323}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701427419,"right_time":1,"body_building":0,"instruction_type":1,"body_fit_percentage":"33.1","muscle_mas":"61.4","bone_salt_content":"4.0","body_water_rate":"49.0","protein_rate":"13.8","visceral_fat_grade":"20","physical_age":"68"},{"dataID":"004D3212FD3E17014274479800000000","weight":"97.8","impedance":[{"impedance":516},{"impedance":443},{"impedance":418},{"impedance":394},{"impedance":1},{"impedance":18348},{"impedance":25961},{"impedance":47351}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701427447,"right_time":1,"body_building":0,"instruction_type":1,"body_fit_percentage":"33.1","muscle_mas":"61.4","bone_salt_content":"4.0","body_water_rate":"49.0","protein_rate":"13.8","visceral_fat_grade":"20","physical_age":"68"},{"dataID":"004D3212FD3E17014274799800000000","weight":"98.02","impedance":[{"impedance":518},{"impedance":445},{"impedance":420},{"impedance":395},{"impedance":1},{"impedance":18348},{"impedance":25961},{"impedance":47383}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701427479,"right_time":1,"body_building":0,"instruction_type":1,"body_fit_percentage":"33.2","muscle_mas":"61.4","bone_salt_content":"4.1","body_water_rate":"48.9","protein_rate":"13.8","visceral_fat_grade":"20","physical_age":"68"}]
//                message: [{"dataID":"004D3212FD3E17013646579900000000","weight":"99.02","impedance":[{"impedance":65535},{"impedance":65535},{"impedance":65535},{"impedance":65535},{"impedance":1},{"impedance":18348},{"impedance":25960},{"impedance":50097}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701364657,"right_time":1,"body_building":0,"instruction_type":0}]
                setTextView(txtStatus,"<< " + queryStatus);
                try {
                    JSONArray historyArr = new JSONArray(message);
                    datos.setHistoryData(message);
                    contHistoryData = historyArr.length();

                    if (contHistoryData > 0) {
                        // Hay datos de histórico que evaluar
                        datos.setStatusDataHistory(true);
                    }

                    queryStatus = iHQuerySetting.DeleteHistoryData;
                    setTextView(txtStatus, ">> " + queryStatus.getQuerySetting());
                    mHS2SControl.deleteOfflineData(user_id);

                } catch (JSONException e) {
                    e.printStackTrace();
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":801,\"description\":\"No detectado dispositivo.\"}");
                    viewResult();
                }
            }
            else if (Hs2sProfile.ACTION_DELETE_HISTORY_DATA.equals(action)) {
//                message: {"status":0,"describe":"Successful"}
                setTextView(txtStatus,"<< " + queryStatus);
                try {
                    JSONObject obj = new JSONObject(message);

                    Integer status = -1;
                    if (obj.has(Hs2sProfile.OPERATION_STATUS)) {
                        status = obj.getInt(Hs2sProfile.OPERATION_STATUS);
                    }

                    if (status == 0) {
                        queryStatus = iHQuerySetting.WaitMeasure;
                        setTextView(txtStatus, ">> " + queryStatus.getQuerySetting());

                        // Si hay registros de datos historico activamos el timer para que muestre el botón de continuar
                        if (contHistoryData > 0) {
                            cTimerContinuar.start();
                        }

                        setVisibleView(circulodescarga,View.INVISIBLE);
                        // Cambiamos imagen a la de medición de peso
                        if (impedance == false) {
                            findViewById(R.id.idlayout_hs2s).setBackground(ContextCompat.getDrawable(get_dataHS2S.this, R.drawable.fondo_hs2s_peso));
                            numToSpeech = 2;
                        }
                        else {
                            findViewById(R.id.idlayout_hs2s).setBackground(ContextCompat.getDrawable(get_dataHS2S.this, R.drawable.fondo_hs2s_peso2));
                            numToSpeech = 3;
                        }

                        textToSpeech.stop();
                        cTimer.start();     // Activamos el tiempo para que se realice la medición.
                    }
                    else {
                        String describe = "";
                        if (obj.has(Hs2sProfile.OPERATION_DESCRIBE)) {
                            describe = obj.getString(Hs2sProfile.OPERATION_DESCRIBE);
                        }

                        EVLog.log(TAG, "ACTION_DELETE_HISTORY_DATA: status: " + status.toString() + ", description: " + describe);
                        datos.setStatusDescarga(false);
                        datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                        viewResult();
                    }
                } catch (JSONException e) {
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                    viewResult();
                }
            }
            else if (Hs2sProfile.ACTION_ONLINE_REAL_TIME_WEIGHT.equals(action)) {
                // message: {"weight":99.06}
                setTextView(txtStatus,"<< " + queryStatus);
                try {
                    JSONObject obj = new JSONObject(message);
                    Double weight = 0.0;
                    if (obj.has(Hs2sProfile.DATA_WEIGHT)) { weight = obj.getDouble(Hs2sProfile.DATA_WEIGHT); }

                    if (queryStatus != iHQuerySetting.OnlineRealTime) {
                        queryStatus = iHQuerySetting.OnlineRealTime;
                        setTextView(txtStatus, ">> " + queryStatus.getQuerySetting());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado\"}");
                    viewResult();
                }
            }
            else if (Hs2sProfile.ACTION_ONLINE_RESULT.equals(action)) {
                // {"dataID":"004D3212FD3E17013645330600000000","status":0,"weight":99.06}
                setTextView(txtStatus,"<< " + queryStatus);
                try {
                    JSONObject obj = new JSONObject(message);
                    Integer status = -1;
                    if (obj.has(Hs2sProfile.OPERATION_STATUS)) { status = obj.getInt(Hs2sProfile.OPERATION_STATUS); }

                    if (status == 0) {
                        datos.setMessageWeight(message);
                        peso = obj.getDouble(Hs2sProfile.DATA_WEIGHT);
                        datos.setStatusOnline(true);

                        // Configurado la medición de la composición corporal
                        if (impedance == false) {
                            queryStatus = iHQuerySetting.UpdateUserInfo;
                            setTextView(txtStatus, ">> " + queryStatus.getQuerySetting());

                            int gender = (paciente.getGender().equals("Hombre")) ? 1:0;
                            Integer masacorporal = (impedance) ? 1:0;
                            // Actualizamos el valor del usuario con la última medida realizada
                            mHS2SControl.createOrUpdateUserInfo(user_id, peso.floatValue(), gender, paciente.getAge(), paciente.getHeight(), masacorporal, 0);
                        } else {
                            // Hay que esperar a DATA_BODY_FAT_RESULT
                            queryStatus = iHQuerySetting.OnlineResult;
                            setTextView(txtStatus, ">> " + queryStatus.getQuerySetting());
                        }
                    }
                    else {

                        String describe = "";
                        if (obj.has(Hs2sProfile.OPERATION_DESCRIBE)) {
                            describe = obj.getString(Hs2sProfile.OPERATION_DESCRIBE);
                        }

                        EVLog.log(TAG, "ACTION_ONLINE_RESULT: status: " + status.toString() + ", description: " + describe);

                        if (contHistoryData > 0) {
                            viewResult();
                        }
                        else {
                            datos.setStatusDescarga(false);
                            datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":805,\"description\":\"Error realizando la medida.\"}");
                            viewResult();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                    viewResult();
                }
            }
            else if (Hs2sProfile.ACTION_BODY_FAT_RESULT.equals(action)) {
//                message: {"status":0,"describe":"Measure Successful","data_body_fat_result":{"dataID":"004D3212FD3E17014246269800000000","weight":97.94999694824219,"impedance":[{"impedance":515},{"impedance":445},{"impedance":419},{"impedance":395},{"impedance":1},{"impedance":18348},{"impedance":25961},{"impedance":44530}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701424626,"right_time":1,"body_building":0,"instruction_type":1,"body_fit_percentage":"33.2","muscle_mas":"61.4","bone_salt_content":"4.1","body_water_rate":"48.9","protein_rate":"13.8","visceral_fat_grade":"20","physical_age":"68"}}
                setTextView(txtStatus,"<< " + queryStatus);
                try {
                    JSONObject obj = new JSONObject(message);
                    datos.setBodyFatResult(message);

                    Integer status = -1;

                    if (obj.has(Hs2sProfile.OPERATION_STATUS)) { status = obj.getInt(Hs2sProfile.OPERATION_STATUS); }

                    if (status == 0) {
                        datos.setStatusBodyFat(true);

                        queryStatus = iHQuerySetting.UpdateUserInfo;
                        setTextView(txtStatus, ">> " + queryStatus.getQuerySetting());

                        // ESTABLECE NUEVO PESO
                        JSONObject dataBodyFat = obj.getJSONObject(Hs2sProfile.DATA_BODY_FAT_RESULT);
                        peso = dataBodyFat.getDouble(Hs2sProfile.DATA_WEIGHT);

                        int gender = (paciente.getGender().equals("Hombre")) ? 1:0;
                        Integer masacorporal = (impedance) ? 1:0;
                        // Actualizamos el valor del usuario con la última medida realizada
                        mHS2SControl.createOrUpdateUserInfo(user_id, peso.floatValue(), gender, paciente.getAge(), paciente.getHeight(), masacorporal, 0);
                    }
                    else {
                        datos.setStatusBodyFat(false);
                        datos.setStatusDescarga(false);
                        datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":805,\"description\":\"Error realizando la medida.\"}");
                        viewResult();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                    viewResult();
                }
            }
            else if (Hs2sProfile.ACTION_MEASURE_FINISH_AT_CRITICAL.equals(action)) {
                // Esto se produce cuando no se ha lanzado primero specifyOnlineUsers(....)
                // Cuando te pesas y no se ha lanzado previamente specifyOnlineUsers(....)
                setTextView(txtStatus,"<< " + queryStatus);

                queryStatus = iHQuerySetting.MeasureFinishAtCritical;
                setTextView(txtStatus, ">> " + queryStatus.getQuerySetting());

                datos.setStatusDescarga(false);
                datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                viewResult();
            }
            else if (Hs2sProfile.ACTION_COMMUNICATION_TIMEOUT.equals(action)) {

                setTextView(txtStatus,"<< " + queryStatus);

                queryStatus = iHQuerySetting.ErrorHS;
                setTextView(txtStatus, ">> " + queryStatus.getQuerySetting());

                EVLog.log(TAG, "COMMUNICATION TIMEOUT");
                datos.setStatusDescarga(false);
                datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":804,\"description\":\"COMMUNICATION_TIMEOUT.\"}");
                viewResult();
            }
            else if (Hs2sProfile.ACTION_ERROR_HS.equals(action)) {
                setTextView(txtStatus,"<< " + queryStatus);

                queryStatus = iHQuerySetting.ErrorHS;
                setTextView(txtStatus, ">> " + queryStatus.getQuerySetting());

                // Se ha producido un error en la báscula
                EVLog.log(TAG,"ACTION_ERROR_HS. " + message);

                datos.setStatusDescarga(false);
                datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                viewResult();
            }
        }
    };

    public void viewResult() {

        if (viewdata == false) {
            viewdata = true;

            EVLog.log(TAG, " ViewResult()");

            //region .. stop timers
            if (cTimerOut != null) cTimerOut.cancel();
            cTimerOut = null;

            if (cTimer != null) cTimer.cancel();
            cTimer = null;

            if (cTimerContinuar != null) cTimerContinuar.cancel();
            cTimerContinuar = null;

            if (cTimerProgress != null) cTimerProgress.cancel();
            cTimerProgress = null;
            //endregion

            setVisibleView(circulodescarga, View.INVISIBLE);

            if (statusDevice == EStatusDevice.Connected) {
                mHS2SControl.disconnect();
                statusDevice = EStatusDevice.Disconnecting;
                setTextView(txtStatus, "" + statusDevice);
            }

             if (datos.getStatusOnline() || datos.getStatusDataHistory() || datos.getStatusBodyFat()) {
                datos.setStatusDescarga(true);
            }

            if (datos.getStatusDescarga()) {
                Intent intent = new Intent(this, view_dataHS2S.class);
                intent.putExtra("typeScale", TypeDevice);
                startActivity(intent);
            }
            else {
                Intent intent = new Intent(this, view_failHS2S.class);
                intent.putExtra("typeScale", TypeDevice);
                startActivity(intent);
            }

            Log.e(TAG,"datos.getStatusDescarga(): " + datos.getStatusDescarga());

            finish();
        }
    }

    private String getUserInfoId(String message, Integer position) throws JSONException {

        JSONObject jsonData = new JSONObject(message);
        if (!jsonData.has("user_info_array")) {
            throw new JSONException("El campo 'user_info_array' no existe");
        }

        // Obtiene el array de usuarios
        JSONArray userArray = jsonData.getJSONArray("user_info_array");

        // Comprueba que la posición indicada existe
        if (position < 0 || position >= userArray.length()) {
            throw new JSONException("La posición indicada no existe");
        }

        // Obtiene el usuario de la posición indicada
        JSONObject user = userArray.getJSONObject(position);

        // Devuelve el json del usuario
        return user.toString();
    }

    @Override
    public void onClick(View view) {
        Log.e(TAG, "onClick()");

        int viewId = view.getId();
        if (viewId == R.id.imageButtonAudio) {
            audio_on = true;
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            if (numToSpeech == 1) {
                texto.add("Coloque la báscula en el suelo, cerca de la tablet.");
                texto.add("Para poder realizar una medida y descargar su historial.");
            } else if (numToSpeech == 2) {
                texto.add("Súbase a la báscula y espere a que la medición finalice.");
                texto.add("Si sus pies están mojados o húmedos, séquelos bien antes de subir a la báscula.");
            } else {
                texto.add("Súbase a la báscula, para realizar una medición.");
                texto.add("Es necesario descalzarse para poder tomar medidas de su composición corporal.");
            }
            textToSpeech.speak(texto);
            //endregion
        }
        else if (viewId == R.id.btcontinuar_bas) {
            //region :: CONTINUAR
            EVLog.log(TAG, "CONTINUAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "PACIENTE PULSA CONTINUAR");
            viewResult();
            //endregion
        }
    }

    private void setTextView(View view, String texto) {
        runOnUiThread(new Runnable() {
            public void run() {
//                Log.e(TAG,"Status:" + texto);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EVLog.log(TAG," onDestroy()");

        //region .. stop timers
        if (cTimerOut != null) cTimerOut.cancel();
        cTimerOut = null;

        if (cTimer != null) cTimer.cancel();
        cTimer = null;

        if (cTimerContinuar != null) cTimerContinuar.cancel();
        cTimerContinuar = null;

        if (cTimerProgress != null) cTimerProgress.cancel();
        cTimerProgress = null;
        //endregion

        textToSpeech.shutdown();

        if(mHS2SControl != null){
            mHS2SControl.disconnect();
        }
        iHealthDevicesManager.getInstance().unRegisterClientCallback(mClientCallbackId);

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){
    }

    @Override
    protected void onResume() {
        super.onResume();
        EVLog.log(TAG, "onResume()");

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
}