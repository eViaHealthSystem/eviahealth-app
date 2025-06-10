package com.eviahealth.eviahealth.ui.config.ihealth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.devices.BeurerReferences;
import com.eviahealth.eviahealth.models.ihealth.iHQuerySetting;
import com.eviahealth.eviahealth.bluetooth.models.EStatusDevice;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.utils.log.EVLogConfig;
import com.eviahealth.eviahealth.utils.PermissionUtils;
import com.eviahealth.eviahealth.utils.log.EVLog;
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

import java.util.ArrayList;
import java.util.Map;

public class config_HS2S extends BaseActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener{

    final static String TAG = "config_HS2S";
    String deviceName = "HS2S 11070";
    String mDeviceMac = "";
    final String TypeDevice = "HS2S";
    final int REINTENTOS = 2;

    private Hs2sControl mHS2SControl;
    private int mClientCallbackId;

    TextView txtTitulo, txtStatus, txtResult, txtProgress;
    Spinner cmbImpedance;
    Button btOnlineUser, btConfigurar, btFactoryReset, btOfflineData, btDeleteData, btGetUserInfo, btSavePatient;
    ProgressBar circulodescarga;

    String textResult = "";
    int cont_scan = 0;

    EStatusDevice statusDevice= EStatusDevice.None;
    iHQuerySetting queryStatus= iHQuerySetting.None;
    private Patient paciente = null;
    private Boolean impedance = false;
    private Integer user_count = null;
    private String user_id = null;

    private Boolean update = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_hs2s);
        Log.e(TAG,"onCreate()");

        PermissionUtils.requestAll(this);
        update = false;

        //region >> Views
        TextView txtInfoPaciente = findViewById(R.id.CHS2S_txtInfoPaciente);
        txtInfoPaciente.setVisibility(View.INVISIBLE);

        txtTitulo = findViewById(R.id.CHS2S_txtTitulo);
        txtTitulo.setVisibility(View.VISIBLE);

        txtStatus = findViewById(R.id.CHS2S_txtStatus);
        txtStatus.setVisibility(View.VISIBLE);

        txtResult = findViewById(R.id.CHS2S_txtResult);
        txtResult.setVisibility(View.VISIBLE);
        txtResult.setMovementMethod(new ScrollingMovementMethod());

        txtProgress = findViewById(R.id.CHS2S_txtProgress);
        txtProgress.setVisibility(View.INVISIBLE);

        circulodescarga = findViewById(R.id.CHS2S_ProgressBar);
        setVisibleView(circulodescarga,View.VISIBLE);

        // buttons
        btSavePatient = findViewById(R.id.CHS2S_btSavePatient);
        btSavePatient.setVisibility(View.INVISIBLE);
        btConfigurar = findViewById(R.id.CHS2S_btConfigurar);
        btConfigurar.setVisibility(View.INVISIBLE);
        btOnlineUser= findViewById(R.id.CHS2S_btSpecifyOnlineUser);
        btOnlineUser.setVisibility(View.INVISIBLE);
        btOfflineData= findViewById(R.id.CHS2S_btSpecifyOfflineData);
        btOfflineData.setVisibility(View.INVISIBLE);
        btDeleteData= findViewById(R.id.CHS2S_btDeleteHistoryData);
        btDeleteData.setVisibility(View.INVISIBLE);
        btFactoryReset = findViewById(R.id.CHS2S_btFactoryReset);
        btFactoryReset.setVisibility(View.INVISIBLE);
        btGetUserInfo = findViewById(R.id.CHS2S_btGetUserInfo);
        btGetUserInfo.setVisibility(View.INVISIBLE);

        cmbImpedance = findViewById(R.id.CHS2S_cmbIMpedance);
        setRellenarImpedance();     // rellena spinner con los valores
        setEnableView(cmbImpedance,false);
        //endregion

        setVisibleButtons(View.INVISIBLE);

        //region :: INTENT
        Intent intent = getIntent();
        Bundle b = intent.getExtras();
        if (b != null) {
            mDeviceMac = intent.getStringExtra("deviceMac");
            setTextStatus(txtTitulo,"Configuración dispositivo HS2S [" + mDeviceMac + "]");

            //region :: Muestra caracteristicas paciente
            String idpaciente = intent.getStringExtra("idpaciente");
            paciente = ApiMethods.loadCharacteristics(idpaciente);;

            setTextStatus(findViewById(R.id.CHS2S_txtGenero),paciente.getGender());
            setTextStatus(findViewById(R.id.CHS2S_txtEdad),paciente.getAge().toString());
            setTextStatus(findViewById(R.id.CHS2S_txtAltura),paciente.getHeight().toString());
            setTextStatus(findViewById(R.id.CHS2S_txtPeso),paciente.getWeight().toString());

            if (paciente.getBydefault()) {
                EVLogConfig.log(TAG, "Datos de paciente cargados con valores por defecto");
                txtInfoPaciente.setVisibility(View.VISIBLE);
            }
            //endregion

            String extra = intent.getStringExtra("hs2s");
            try {
                JSONObject params = new JSONObject(extra);
                Log.e(TAG, "extra: " + params);

                if (params.has("hs2s")) {
                    JSONObject hs2s = params.getJSONObject("hs2s");
                    Log.e(TAG, "hs2s: " + hs2s);
                    if (hs2s.has("impedance")) { impedance = hs2s.getBoolean("impedance"); }
                    else { impedance = false; }
                }
                else {
                    Toast.makeText(getApplicationContext(), "No se ha encontrado configuración correcta del dispositivo", Toast.LENGTH_LONG).show();
                    impedance = false;
                }

            }
            catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG,"Exceprion: " + e.toString());
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
                return;
            }

        }
        //endregion

        setImpedance(impedance);    // seleciona valor de impedance
        clearTextResult();

        mClientCallbackId = iHealthDevicesManager.getInstance().registerClientCallback(miHealthDevicesCallback);
        iHealthDevicesManager.getInstance().addCallbackFilterForDeviceType(mClientCallbackId, iHealthDevicesManager.TYPE_HS2S, iHealthDevicesManager.TYPE_HS2SPRO);

        // Busca dispositivos tipo HS2S
        statusDevice = EStatusDevice.Scanning;
        setTextStatus(txtStatus, statusDevice.toString());
        iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.HS2S);
    }

    private iHealthDevicesCallback miHealthDevicesCallback = new iHealthDevicesCallback() {

        @Override
        public void onScanDevice(String mac, String deviceType, int rssi, Map manufactorData) {

            Log.e(TAG, "iHealth onScanDevice() >> mac[" + mac + "], deviceType[" + deviceType + "]");

            if (mac.equals(mDeviceMac)) {
                Log.e(TAG, "iHealth onScanDevice() Device " +deviceType +" [" + mac + "] detected");
                iHealthDevicesManager.getInstance().stopDiscovery();
                statusDevice = EStatusDevice.Detected;
            }
        }

        @Override
        public void onScanFinish() {
            super.onScanFinish();

            // Salta cuando se envia stopDiscovery(); o salta timeout de conexión de ihealth

            Log.e(TAG, "iHealth onScanFinish() >> entra");
            iHealthDevicesManager.getInstance().stopDiscovery();
            iHealthDevicesManager.getInstance().disconnectAllDevices(true);

            Log.e(TAG,"statusDevice: " + statusDevice);

            if (statusDevice == EStatusDevice.Detected) {
                // DISPOSITIVO DETECTADO
                Log.e(TAG, "iHealth onScanFinish() >> " + TypeDevice + " DETECTADO");
                iHealthDevicesManager.getInstance().stopDiscovery();

                // Conectando con el dispositivo
                if (iHealthDevicesManager.getInstance().isDiscovering()){
                    EVLog.log(TAG, "iHealth isDiscovering() = true");
                    iHealthDevicesManager.getInstance().stopDiscovery();
                    SystemClock.sleep(1000);
                }

                statusDevice = EStatusDevice.Connecting;
                iHealthDevicesManager.getInstance().connectDevice("", mDeviceMac, TypeDevice);
                setTextStatus(txtStatus,"" + statusDevice);
            }
            else {
                // DISPOSITIVO NO ENCONTRADO
                cont_scan += 1;
                if (cont_scan <= REINTENTOS){
                    EVLog.log(TAG, "iHealth onScanFinish() >> NO ENCONTRADO REINTENTAMOS SCAN (" + Integer.toString(cont_scan)+")");
                    // reintentamos scanearlo
                    statusDevice= EStatusDevice.Scanning;
                    iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.HS2S);
                    setTextStatus(txtStatus,"" + statusDevice + " [" + cont_scan + "]");
                }
                else {
                    EVLog.log(TAG, "iHealth onScanFinish() >> NO ENCONTRADO FINALIZAMOS SCAN (" + Integer.toString(cont_scan)+")");
                    statusDevice= EStatusDevice.Failed;
                    setTextStatus(txtStatus,"scan failed");
                    setTextStatus(txtResult,"No detectado dispositivo");
                    endScanViews();
                }
            }

        }

        @Override
        public void onDeviceConnectionStateChange(String mac, String deviceType, int status, int errorID) {
            Log.e(TAG,"onDeviceConnectionStateChange() >> deviceType: " + deviceType + ", mac: " + mac + ", status: " + status);

            if (status == iHealthDevicesManager.DEVICE_STATE_CONNECTED) {

                Log.e(TAG, "iHealth DEVICE_STATE_CONNECTED");
                statusDevice = EStatusDevice.Connected;
                try {
                    setTextStatus(txtStatus, statusDevice.toString());
                    // Get HS2S controller
                    mHS2SControl = iHealthDevicesManager.getInstance().getHs2sControl(mDeviceMac);

                    // Solicitamos información del usuario incluye estado batería, este método sincroniza la hora
                    queryStatus= iHQuerySetting.DeviceInfo;
                    textResult = "Connected\n" + queryStatus.getQuerySetting();
                    setTextStatus(txtResult, textResult);
                    mHS2SControl.getDeviceInfo();

                    setVisibleButtons(View.VISIBLE);
                    endScanViews();
//                    cTimer.start();

                }catch (Exception e){
                    EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTED >> Exception getDeviceInfo()");
                    statusDevice = EStatusDevice.Failed;
                }
            }

            if (status == iHealthDevicesManager.DEVICE_STATE_DISCONNECTED) {
                EVLog.log(TAG, "iHealth DEVICE_STATE_DISCONNECTED >> entra");

                if (statusDevice == EStatusDevice.Disconnecting) {
                    finish();
                }

                statusDevice = EStatusDevice.Disconnect;
            }

            if (status == iHealthDevicesManager.DEVICE_STATE_CONNECTIONFAIL) {
                EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTIONFAIL >> ERRORID: " + errorID);

                setTextStatus(txtStatus,statusDevice.toString());
                Log.e(TAG,"********************* " + statusDevice.toString());
//                if (statusDevice == EStatusDevice.WaitMeasurement) {

//                }
//                else if (statusDevice != EStatusDevice.Disconnecting){

//                }
                statusDevice = EStatusDevice.Failed;
            }

            if (status == iHealthDevicesManager.DEVICE_STATE_RECONNECTING) {
                EVLog.log(TAG, "iHealth DEVICE_STATE_RECONNECTING");
                statusDevice = EStatusDevice.Reconnecting;
            }

        }

        @Override
        public void onUserStatus(String username, int userStatus) {
            Log.e(TAG,"onUserStatus() >> username: " + username + ", userState: " + userStatus);
        }

        @Override
        public void onDeviceNotify(String mac, String deviceType, String action, String message) {
            super.onDeviceNotify(mac, deviceType, action, message);

            Log.e(TAG, "onDeviceNotify() >> deviceType: " + deviceType + ", mac:" + mac + ", action:" + action + ", message: " + message);

            if (Hs2sProfile.ACTION_GET_DEVICE_INFO.equals(action)) {
                // {"user_count":0,"unit_current":1,"battery":100}
                user_count = util.getIntValueJSON(message,"user_count");
                setTextView(txtResult,message);

                if (user_count == null) {
                    Log.e(TAG,"Error obteniendo user_count de message.");
                    statusDevice = EStatusDevice.Disconnecting;
                    setTextStatus(txtStatus,statusDevice.toString());
                    mHS2SControl.disconnect();
                }

                if (user_count > 0) {
                    queryStatus = iHQuerySetting.UserInfo;
                    setTextView(txtResult,queryStatus.getQuerySetting());
                    mHS2SControl.getUserInfo();
                }
            }
            else if (Hs2sProfile.ACTION_GET_USER_INFO.equals(action)) {

/* ejemplo de resultado con dos usuarios creados
            {
                "user_info_count":2,
                "user_info_array":[
                    {
                        "user_id":"wujian1234567890",
                            "create_time":1558504190,
                            "weight":62,
                            "gender":1,
                            "age":28,
                            "height":182,
                            "impedance":1,
                            "bodybuilding":0
                    },
                    {
                        "user_id":"xqhs012345678900",
                        "create_time":1558062785,
                        "weight":64,
                        "gender":0,
                        "age":30,
                        "height":164,
                        "impedance":1,
                        "bodybuilding":1
                    }
                ]
            }
*/
                setTextView(txtResult,"Action get device message: " + message);
                Integer user_info_count = util.getIntValueJSON(message,"user_info_count");
                if (user_info_count == null) {
                    Log.e(TAG,"Error obteniendo 'user_info_count' de message.");
                    statusDevice = EStatusDevice.Disconnecting;
                    setTextStatus(txtStatus,statusDevice.toString());
                    mHS2SControl.disconnect();
                }

                if (user_info_count > 0) {
                    try {
                        String info = getUserInfoId(message,0); // Obtiene el primer usuario del array
//                        setTextView(txtResult,"info user: " + info);

                        int weight = (int)util.getDoubleValueJSON(info,"weight").doubleValue();
                        int height = util.getIntValueJSON(info,"height");
                        Boolean grasaCorporal = (util.getIntValueJSON(info,"impedance") == 0) ? false : true;
                        String gender = (util.getIntValueJSON(info,"gender") == 0) ? "Mujer" : "Hombre";
                        user_id = util.getStringValueJSON(info,"user_id");

                        if (!paciente.getGender().equals(gender) || paciente.getHeight() != height || paciente.getWeight() != weight  || impedance != grasaCorporal) {
                            setTextView(txtResult,"EL USUARIO DE LA BÁSCULAS NO COINCIDE CON EL PACIENTE\nACTUALIZA EL USUARIO DE LA BÁSCULA");
                        }

                        setTextStatus(btConfigurar,"ACTUALIZAR\nUSUARIO");

                    }
                    catch (JSONException e) {
                        Log.e(TAG, "JSONException getUserInfoId(): " + e);
                        Disconect();
                    }
                }
                else {
                    setTextStatus(btConfigurar,"Crear\nUsuario");
                }
            }
            else if (Hs2sProfile.ACTION_SET_UNIT_SUCCESS.equals(action)) {
                setTextView(txtResult,"Action unit success message:" + message);
                if (message == null) {
                    if (user_count != null) {
                        if (user_count == 0) { queryStatus = iHQuerySetting.CreateUserInfo; }
                        else { queryStatus = iHQuerySetting.UpdateUserInfo; }
                        setTextView(txtResult,queryStatus.getQuerySetting());
                    }

                    int gender = (paciente.getGender().equals("Hombre")) ? 1:0;

                    String seleccion = (String) cmbImpedance.getSelectedItem();
                    Integer impedance = (seleccion.equals("Si")) ? 1:0;

                    user_id = "abcdef1234567890";
                    mHS2SControl.createOrUpdateUserInfo(user_id,paciente.getWeight().floatValue(),gender,paciente.getAge(),paciente.getHeight(),impedance,0);
                }
            }
            else if (Hs2sProfile.ACTION_CREATE_OR_UPDATE_USER_INFO.equals(action)) {
                setTextView(txtResult,message);
                Integer status = util.getIntValueJSON(message,"status");
                if (status != null) {
                    if (status == 0) {
                        if (user_count == 0) { setTextView(txtResult, "Usuario creado correctamente."); }
                        else { setTextView(txtResult, "Usuario actualizado correctamente."); }

                        setVisibleView(btOnlineUser,View.VISIBLE);
                        setVisibleView(btDeleteData,View.VISIBLE);
                        setVisibleView(btOfflineData,View.VISIBLE);

                        if (update) {
                           update = false;
                            Toast.makeText(getApplicationContext(), "Báscula actualizada con existo.", Toast.LENGTH_LONG).show();
                           statusDevice = EStatusDevice.Disconnecting;
                           mHS2SControl.disconnect();
                        }
                    }
                    else {
                        setTextView(txtResult, "FALLO al Crear o Actualizar el Usuario.");
                        update = false;
                    }
                }
            }
            else if (Hs2sProfile.ACTION_SPECIFY_USERS.equals(action)) {
                setTextView(txtResult,message);
                try {
                    JSONObject obj = new JSONObject(message);
                    int status = obj.getInt(Hs2sProfile.OPERATION_STATUS);
                    String description = obj.getString(Hs2sProfile.OPERATION_DESCRIBE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else if (Hs2sProfile.ACTION_ONLINE_REAL_TIME_WEIGHT.equals(action)) {
                // message: {"weight":99.06}
//                setTextView(txtResult,message);
                try {
                    JSONObject obj = new JSONObject(message);
                    Double weight = obj.getDouble(Hs2sProfile.DATA_WEIGHT);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else if (Hs2sProfile.ACTION_ONLINE_RESULT.equals(action)) {
                // {"dataID":"004D3212FD3E17013645330600000000","status":0,"weight":99.06}
                setTextView(txtResult,message);
                try {
                    JSONObject obj = new JSONObject(message);
                    String dataId = obj.getString(Hs2sProfile.DATA_ID);
                    int status = obj.getInt(Hs2sProfile.OPERATION_STATUS);
                    Double weight = obj.getDouble(Hs2sProfile.DATA_WEIGHT);
                    String description = obj.getString(Hs2sProfile.OPERATION_DESCRIBE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else if (Hs2sProfile.DATA_BODY_FAT_RESULT.equals(action)) {
//                message: {"status":0,"describe":"Measure Successful","data_body_fat_result":{"dataID":"004D3212FD3E17014246269800000000","weight":97.94999694824219,"impedance":[{"impedance":515},{"impedance":445},{"impedance":419},{"impedance":395},{"impedance":1},{"impedance":18348},{"impedance":25961},{"impedance":44530}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701424626,"right_time":1,"body_building":0,"instruction_type":1,"body_fit_percentage":"33.2","muscle_mas":"61.4","bone_salt_content":"4.1","body_water_rate":"48.9","protein_rate":"13.8","visceral_fat_grade":"20","physical_age":"68"}}
                setTextView(txtResult,message);
                try {
                    JSONObject obj = new JSONObject(message);
                    int status = obj.getInt(Hs2sProfile.OPERATION_STATUS);
                    Double weight = obj.getDouble(Hs2sProfile.OPERATION_DESCRIBE);
                    String dataId = obj.getString(Hs2sProfile.DATA_ID);
                    Double description = obj.getDouble(Hs2sProfile.DATA_WEIGHT);
                    int userCount = obj.getInt(Hs2sProfile.DATA_USER_NUM);
                    int gender = obj.getInt(Hs2sProfile.DATA_GENDER);
                    int age = obj.getInt(Hs2sProfile.DATA_AGE);
                    int height = obj.getInt(Hs2sProfile.DATA_HEIGHT);
                    long measureTs = obj.getLong(Hs2sProfile.DATA_MEASURE_TIME);
                    int bodyBuilding = obj.getInt(Hs2sProfile.DATA_BODYBUILDING);
                    // type=0: Instrucciones cortas, no contienen campos de datos de medición de grasa corporal
                    // type=1: Instrucciones largas, contienen campos de datos de medición de grasa corporal
                    int type = obj.getInt(Hs2sProfile.DATA_INSTRUCTION_TYPE);

                    JSONObject objResult = obj.getJSONObject(Hs2sProfile.DATA_BODY_FAT_RESULT);
                    String bodyFit = obj.getString(Hs2sProfile.DATA_BODY_FIT_PERCENTAGE);
                    String muscleMass = obj.getString(Hs2sProfile.DATA_MUSCLE_MASS);
                    String boneSaltContent = obj.getString(Hs2sProfile.DATA_BONE_SALT_CONTENT);
                    String bodyWater = obj.getString(Hs2sProfile.DATA_BODY_WATER_RATE);
                    String protein = obj.getString(Hs2sProfile.DATA_PROTEIN_RATE);
                    String skeletalMuscleMass = obj.getString(Hs2sProfile.DATA_SKELETAL_MUSCLE_MASS);
                    String visceralFat = obj.getString(Hs2sProfile.DATA_VISCERAL_FAT_GRADE);
                    String physicalAge = obj.getString(Hs2sProfile.DATA_PHYSICAL_AGE);
                    String standardWeight = obj.getString(Hs2sProfile.DATA_STANDARD_WEIGHT);
                    String weightControl = obj.getString(Hs2sProfile.DATA_WEIGHT_CONTROL);
                    String muscleControl = obj.getString(Hs2sProfile.DATA_MUSCLE_CONTROL);
                    String fatControl = obj.getString(Hs2sProfile.DATA_FAT_CONTROL);
                    String fatWeight = obj.getString(Hs2sProfile.DATA_FAT_WEIGHT);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else if (Hs2sProfile.ACTION_HISTORY_DATA_NUM.equals(action)) {
//                {"history_data_user_count":1,"history_data_count_array":[{"history_data_count":0}]}
                setTextView(txtResult,message);
                try {
                    JSONObject obj = new JSONObject(message);
                    int userCount = obj.getInt(Hs2sProfile.HISTORY_DATA_USER_COUNT);
                    JSONArray countArr = obj.getJSONArray(Hs2sProfile.HISTORY_DATA_COUNT_ARRAY);
                    for (int i = 0; i < userCount; i++) {
                        JSONObject countObj = countArr.getJSONObject(i);
                        int count = countObj.getInt(Hs2sProfile.HISTORY_DATA_COUNT);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else if (Hs2sProfile.ACTION_HISTORY_DATA.equals(action)) {
//                message: [] sin datos
//                [{"dataID":"004D3212FD3E17013646579900000000","weight":"99.02","impedance":[{"impedance":65535},{"impedance":65535},{"impedance":65535},{"impedance":65535},{"impedance":1},{"impedance":18348},{"impedance":25960},{"impedance":50097}],"user_num":0,"gender":1,"age":71,"height":172,"measure_time":1701364657,"right_time":1,"body_building":0,"instruction_type":0}]
                setTextView(txtResult,message);
                try {
                    JSONArray historyArr = new JSONArray(message);
                    for (int i = 0; i < historyArr.length(); i++) {
                        JSONObject obj = historyArr.getJSONObject(i);
                        int status = obj.getInt(Hs2sProfile.OPERATION_STATUS);
                        Double weight = obj.getDouble(Hs2sProfile.OPERATION_DESCRIBE);
                        String dataId = obj.getString(Hs2sProfile.DATA_ID);
                        Double description = obj.getDouble(Hs2sProfile.DATA_WEIGHT);
//                        int userCount = obj.getString(Hs2sProfile.DATA_USER_NUM);
                        int gender = obj.getInt(Hs2sProfile.DATA_GENDER);
                        int age = obj.getInt(Hs2sProfile.DATA_AGE);
                        int height = obj.getInt(Hs2sProfile.DATA_HEIGHT);
                        long measureTs = obj.getLong(Hs2sProfile.DATA_MEASURE_TIME);
                        int bodyBuilding = obj.getInt(Hs2sProfile.DATA_BODYBUILDING);
                        int type = obj.getInt(Hs2sProfile.DATA_INSTRUCTION_TYPE);

                        JSONObject objResult = obj.getJSONObject(Hs2sProfile.DATA_BODY_FAT_RESULT);
                        String bodyFit = obj.getString(Hs2sProfile.DATA_BODY_FIT_PERCENTAGE);
                        String muscleMass = obj.getString(Hs2sProfile.DATA_MUSCLE_MASS);
                        String boneSaltContent = obj.getString(Hs2sProfile.DATA_BONE_SALT_CONTENT);
                        String bodyWater = obj.getString(Hs2sProfile.DATA_BODY_WATER_RATE);
                        String protein = obj.getString(Hs2sProfile.DATA_PROTEIN_RATE);
                        String skeletalMuscleMass = obj.getString(Hs2sProfile.DATA_SKELETAL_MUSCLE_MASS);
                        String visceralFat = obj.getString(Hs2sProfile.DATA_VISCERAL_FAT_GRADE);
                        String physicalAge = obj.getString(Hs2sProfile.DATA_PHYSICAL_AGE);
                        String standardWeight = obj.getString(Hs2sProfile.DATA_STANDARD_WEIGHT);
                        String weightControl = obj.getString(Hs2sProfile.DATA_WEIGHT_CONTROL);
                        String muscleControl = obj.getString(Hs2sProfile.DATA_MUSCLE_CONTROL);
                        String fatControl = obj.getString(Hs2sProfile.DATA_FAT_CONTROL);
                        String fatWeight = obj.getString(Hs2sProfile.DATA_FAT_WEIGHT);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else if (Hs2sProfile.ACTION_DELETE_HISTORY_DATA.equals(action)) {
//                message: {"status":0,"describe":"Successful"}
                setTextView(txtResult,message);
                try {
                    JSONObject obj = new JSONObject(message);
                    int status = obj.getInt(Hs2sProfile.OPERATION_STATUS);
                    String description = obj.getString(Hs2sProfile.OPERATION_DESCRIBE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else if (Hs2sProfile.ACTION_MEASURE_FINISH_AT_CRITICAL.equals(action)) {
                setTextView(txtResult,message);
                // Esto se produce cuando no se ha lanzado primero specifyOnlineUsers(....)
                // Cuando te pesas y no se ha lanzado previamente specifyOnlineUsers(....)
            }
            else if (Hs2sProfile.ACTION_COMMUNICATION_TIMEOUT.equals(action)) {
//               setTextView(txtResult,message);
//                queryStatus = iHQuerySetting.DeviceInfo;
//                setTextView(txtResult,"Reintentamos getDeviceInfo()");
//                setTextView(txtResult,queryStatus.getQuerySetting());
//                mHS2SControl.getDeviceInfo();
            }
            else if (Hs2sProfile.ACTION_ERROR_HS.equals(action)) {
                setTextView(txtResult,message);
                Disconect();
            }
            else if (Hs2sProfile.ACTION_RESTORE_FACTORY_SETTINGS.equals(action)) {
                if (queryStatus == iHQuerySetting.RestoreFactory) {
/*                    int status = util.getIntValueJSON(message,"status");
                    statusDevice = EStatusDevice.Disconnecting;
                    setTextStatus(txtStatus,statusDevice.toString());
                    mHS2SControl.disconnect();*/
                    finish();
                }
            }

        }
    };

    private void Disconect() {
        statusDevice = EStatusDevice.Disconnecting;
        setTextStatus(txtStatus,statusDevice.toString());
        mHS2SControl.disconnect();
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

    // region :: ONCLICK
    @Override
    public void onClick(View view) {
        Log.e(TAG, " onClick()");

        int viewId = view.getId();
        if (viewId == R.id.CHS2S_btConfigurar) {
            //region :: ACTUALIZA O CREA USUARIO
            // inicia secuencia de configuración
            update = false;
            clearTextResult();
            queryStatus = iHQuerySetting.Unit;
            mHS2SControl.setUnit(Hs2sProfile.UNIT_KG);
            //endregion
        }
        else if (viewId == R.id.CHS2S_btSpecifyOnlineUser) {
            //region :: Specify Online User
            clearTextResult();
            queryStatus = iHQuerySetting.SpecifyOnlineUsers;
            setTextView(txtResult, queryStatus.getQuerySetting());

            int gender = (paciente.getGender().equals("Hombre")) ? 1 : 0;

            String seleccion = (String) cmbImpedance.getSelectedItem();
            Integer impedance = (seleccion.equals("Si")) ? 1 : 0;

            mHS2SControl.specifyOnlineUsers(user_id, paciente.getWeight().floatValue(), gender, paciente.getAge(), paciente.getHeight(), impedance, 0);
            //endregion
        }
        else if (viewId == R.id.CHS2S_btSpecifyOfflineData) {
            //region :: Specify Online Data
            clearTextResult();
            queryStatus = iHQuerySetting.OfflineData;
            setTextView(txtResult, queryStatus.getQuerySetting());
            mHS2SControl.getOfflineData(user_id);
            //endregion
        }
        else if (viewId == R.id.CHS2S_btDeleteHistoryData) {
            //region :: Delete History Data
            clearTextResult();
            queryStatus = iHQuerySetting.DeleteHistoryData;
            setTextView(txtResult, queryStatus.getQuerySetting());
            mHS2SControl.deleteOfflineData(user_id);
            //endregion
        }
        else if (viewId == R.id.CHS2S_btFactoryReset) {
            //region :: Restore Factory
            clearTextResult();
            queryStatus = iHQuerySetting.RestoreFactory;
            setTextView(txtResult, queryStatus.getQuerySetting());

            mHS2SControl.restoreFactorySettings();
            //endregion
        }
        else if (viewId == R.id.CHS2S_btGetUserInfo) {
            //region :: Get User Info
            clearTextResult();
            queryStatus = iHQuerySetting.UserInfo;
            setTextView(txtResult, queryStatus.getQuerySetting());
            mHS2SControl.getUserInfo();
            //endregion
        }
        else if (viewId == R.id.CHS2S_imfAtras) {
            //region :: STOP HS2S
            clearTextResult();
            if (statusDevice != EStatusDevice.Disconnecting) {
                statusDevice = EStatusDevice.Disconnecting;
                if (mHS2SControl != null) {
                    mHS2SControl.disconnect();
                }
                else { finish(); }
            }
            //endregion
        }
        else if (viewId == R.id.CHS2S_btSavePatient) {
            //region :: SAVE CONFIG SCALE
            clearTextResult();
            setTextStatus(txtResult,"Estado: " + statusDevice.toString());

            String seleccion = (String) cmbImpedance.getSelectedItem();
            impedance = (seleccion.equals("Si")) ? true : false;

            try {
                JSONObject message = new JSONObject();
                message.put("deviceinfo","hs2s");

                JSONObject hs2s = new JSONObject();
                hs2s.put("impedance",impedance);

                message.put("hs2s",hs2s);
                Log.e(TAG, "************* config hs2s: " + message.toString());
                broadcastUpdate(BeurerReferences.ACTION_EXTRA_DATA, message.toString());

                if (statusDevice == EStatusDevice.Connected) {
                    update = true;
                    setTextView(txtResult,"queryStatus: " + queryStatus.toString());
                    setEnableButtons(false);

                    // Inicia secuencia de configuración en la báscula
                    queryStatus = iHQuerySetting.Unit;
                    mHS2SControl.setUnit(Hs2sProfile.UNIT_KG);
                }
                else if (statusDevice != EStatusDevice.Disconnecting) {
                    statusDevice = EStatusDevice.Disconnecting;
                    Toast.makeText(getApplicationContext(), "No se ha detectado la báscula.\nNo se pudo realizar su configuración.", Toast.LENGTH_LONG).show();
                    if (mHS2SControl != null) {
                        mHS2SControl.disconnect();
                    }
                    else {
                        finish();
                    }
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG,"Exceprion: " + e.toString());
            }

            //endregion
        }

    }
    //endregion

    //region :: Ciclo de vida
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG,"onResume()");
    }

    @Override
    protected  void onStop() {
        super.onStop();
        Log.e(TAG,"onStop()");
        finish();
    }

    @Override
    protected  void onPause() {
        super.onPause();
        Log.e(TAG,"onPause()");
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG,"onDestroy()");

        if (statusDevice == EStatusDevice.Scanning) {
            // Está escaneando paramos
            iHealthDevicesManager.getInstance().stopDiscovery();
        }

//        if (cTimer != null) cTimer.cancel();
//        cTimer = null;

        if(mHS2SControl != null){
//            mHS2SControl.disconnect();
            mHS2SControl = null;
        }

        iHealthDevicesManager.getInstance().unRegisterClientCallback(mClientCallbackId);

        super.onDestroy();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}
    //endregion

    //region :: LISTENER SPINNER
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//        if (adapterView.equals(cmbSelectQuery)){
//            typeQuery = (String) cmbSelectQuery.getSelectedItem();
//        }
//        else if (adapterView.equals(cmbLanguage)){
//            language = (String) cmbLanguage.getSelectedItem();
//        }
//        else if (adapterView.equals(cmbFunciones)){
//            setting = (String) cmbFunciones.getSelectedItem();
//        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    //endregion

    //region :: runOnUiThread VIEWs
    private void setTextView(View view, String texto) {
//        Log.e(TAG,"" + texto);
        textResult += '\n' + texto;
        runOnUiThread(new Runnable() {
            public void run() {

                if (view instanceof TextView) {
                    TextView obj = (TextView) view;
                    obj.setText(textResult);
                    Log.e(TAG,":: "+texto);
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

    private void clearTextResult() {
        textResult = "";
        setTextStatus(txtResult,"");
    }
    private void setTextStatus(View view, String texto) {
//        Log.e(TAG,"" + texto);
        runOnUiThread(new Runnable() {
            public void run() {

                if (view instanceof TextView) {
                    TextView obj = (TextView) view;
                    obj.setText(texto);
                    Log.e(TAG,":: "+texto);
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

    private void setEnableView(View view, boolean state) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (view instanceof TextView) {
                    TextView obj = (TextView) view;
                    obj.setEnabled(state);
                }
                else if (view instanceof EditText) {
                    EditText obj = (EditText) view;
                    obj.setEnabled(state);
                }
                else if (view instanceof Button) {
                    Button obj = (Button) view;
                    obj.setEnabled(state);
                }
                else if (view instanceof ImageView) {
                    ImageView obj = (ImageView) view;
                    obj.setEnabled(state);
                }
                else if (view instanceof ProgressBar) {
                    ProgressBar obj = (ProgressBar) view;
                    obj.setEnabled(state);
                }
                else if (view instanceof NumberPicker) {
                    NumberPicker obj = (NumberPicker) view;
                    obj.setEnabled(state);
                }
                else if (view instanceof Spinner) {
                    Spinner obj = (Spinner) view;
                    obj.setEnabled(state);
                }
            }
        });
    }

    private void setVisibleButtons(int state) {
        setVisibleView(findViewById(R.id.CHS2S_btConfigurar),state);
        setVisibleView(findViewById(R.id.CHS2S_btSpecifyOnlineUser),state);
        setVisibleView(findViewById(R.id.CHS2S_btSpecifyOfflineData),state);
        setVisibleView(findViewById(R.id.CHS2S_btDeleteHistoryData),state);
        setVisibleView(findViewById(R.id.CHS2S_btFactoryReset),state);
        setVisibleView(findViewById(R.id.CHS2S_btGetUserInfo),state);
    }

    private void setSelectionIdSpinner(Spinner spn, int id) {
        runOnUiThread(new Runnable() {
            public void run() {
                spn.setSelection(id);
            }
        });
    }

    private void setEnableButtons(Boolean state) {
        setEnableView(findViewById(R.id.CHS2S_btConfigurar),state);
        setEnableView(findViewById(R.id.CHS2S_btSpecifyOnlineUser),state);
        setEnableView(findViewById(R.id.CHS2S_btSpecifyOfflineData),state);
        setEnableView(findViewById(R.id.CHS2S_btDeleteHistoryData),state);
        setEnableView(findViewById(R.id.CHS2S_btFactoryReset),state);
        setEnableView(findViewById(R.id.CHS2S_btGetUserInfo),state);
        setEnableView(findViewById(R.id.CHS2S_btSavePatient),state);
    }

    //endregion

    private void setRellenarImpedance(){
        ArrayList<String> typeList = new ArrayList<>();

        typeList.add("No");
        typeList.add("Si");

        ArrayAdapter types = new ArrayAdapter(this,R.layout.custom2_spinner_item, typeList);
        types.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        cmbImpedance.setAdapter(types);
    }

    private void setImpedance(Boolean impedance) {
        if (impedance) {
            setSelectionIdSpinner(cmbImpedance, 1);
        } else {
            setSelectionIdSpinner(cmbImpedance, 0);
        }
    }

    private void endScanViews() {
        txtProgress.setVisibility(View.INVISIBLE);
        setVisibleView(circulodescarga,View.INVISIBLE);
        btSavePatient.setVisibility(View.VISIBLE);
        setEnableView(cmbImpedance,true);
    }

    //region :: Broadcast
    private void broadcastUpdate(final String action, String message) {
        final Intent intent = new Intent(action);
        intent.putExtra(BeurerReferences.ACTION_EXTRA_MESSAGE, message);
        this.sendBroadcast(intent);
    }
    //endregion
}