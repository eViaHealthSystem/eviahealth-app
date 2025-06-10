package com.eviahealth.eviahealth.ui.ensayo.bascula.gbs2012b;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.models.transtek.gbs2012b.DataGBS2012B;
import com.eviahealth.eviahealth.bluetooth.models.EStatusDevice;
import com.eviahealth.eviahealth.devices.NombresDispositivo;
import com.eviahealth.eviahealth.models.transtek.mb6.models.setting.DeviceSetting;
import com.eviahealth.eviahealth.models.transtek.mb6.models.setting.ENErrorCode;
import com.eviahealth.eviahealth.models.transtek.mb6.models.setting.LSQuerySetting;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.utils.PermissionUtils;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;
import com.eviahealth.eviahealth.models.vitalograph.Patient;
import com.lifesense.plugin.ble.LSBluetoothManager;
import com.lifesense.plugin.ble.OnPairingListener;
import com.lifesense.plugin.ble.OnSearchingListener;
import com.lifesense.plugin.ble.OnSettingListener;
import com.lifesense.plugin.ble.OnSyncingListener;
import com.lifesense.plugin.ble.data.IDeviceData;
import com.lifesense.plugin.ble.data.LSConnectState;
import com.lifesense.plugin.ble.data.LSDeviceInfo;
import com.lifesense.plugin.ble.data.LSDevicePairSetting;
import com.lifesense.plugin.ble.data.LSDeviceType;
import com.lifesense.plugin.ble.data.LSManagerStatus;
import com.lifesense.plugin.ble.data.LSPairCommand;
import com.lifesense.plugin.ble.data.LSProtocolType;
import com.lifesense.plugin.ble.data.LSScanIntervalConfig;
import com.lifesense.plugin.ble.data.bgm.BGDataSummary;
import com.lifesense.plugin.ble.data.bpm.LSBloodPressure;
import com.lifesense.plugin.ble.data.scale.LSScaleWeight;
import com.lifesense.plugin.ble.data.tracker.ATDeviceData;
import com.lifesense.plugin.ble.data.tracker.ATLoginInfo;
import com.lifesense.plugin.ble.data.tracker.ATPairConfirmInfo;
import com.lifesense.plugin.ble.data.tracker.ATPairConfirmState;
import com.lifesense.plugin.ble.data.tracker.ATPairResultsCode;

import java.util.ArrayList;
import java.util.List;

public class get_dataGBS2012B extends BaseActivity implements View.OnClickListener {

    final static String TAG = "GET_DATA_GBS2012B";
    String TypeDevice = "GBS-2012-B";
    private String mDeviceAddress = ""; // "D8:E7:2F:B4:8A:10";
    ProgressBar circulodescarga;
    TextView txtStatus;
    TextToSpeechHelper textToSpeech;
//    private static PairMode devicePairMode = PairMode.Auto;
    private LSDeviceInfo currentDevice = null;
//    ATDeviceInfo deviceInfo = null;
    private ArrayList<LSDeviceInfo> listLSDevice = new ArrayList<>();
    LSQuerySetting querySetting = LSQuerySetting.None;
    private Integer contConnecting = 0;
    EStatusDevice statusDevice = EStatusDevice.None;

    DataGBS2012B datos;
    Boolean viewdata = false;               // Para evitar que se lance dos veces la siguiente actividad
    private Patient paciente = null;
    CountDownTimer cTimer = null;           // TimeOut para que se realice la medición de peso
    CountDownTimer cTimerProgress = null;  // Timer para mostrar el Circulo de Progreso inicial

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_data_gbs2012_b);

        EVLog.log(TAG,"onCreate()");
        PermissionUtils.requestAll(this);

        //region :: views
        txtStatus = findViewById(R.id.txtStatus_gbs2012b);
        txtStatus.setTextSize(14);
        setVisibleView(txtStatus, View.VISIBLE);

        circulodescarga = findViewById(R.id.circulodescarga);
        setVisibleView(circulodescarga,View.INVISIBLE);
        //endregion

        textToSpeech = new TextToSpeechHelper(getApplicationContext());
        datos = DataGBS2012B.getInstance();
        datos.clear();
        viewdata = false;

        //region :: Carga datos del paciente de la DB
//        String idpaciente = Config.getInstance().getIdPacienteTablet();
        String idpaciente = Config.getInstance().getIdPacienteEnsayo();
        paciente = ApiMethods.loadCharacteristics(idpaciente);
        if (paciente.getBydefault()) {
            EVLog.log(TAG,"Datos de paciente cargados con valores por defecto");
        }
        //endregion

        String DEVICE_MAC_ADDRESS = Config.getInstance().getIdentificador(NombresDispositivo.BASCULA);
        mDeviceAddress = util.MontarMAC(DEVICE_MAC_ADDRESS.replace("GBS2012B-", ""));
        EVLog.log(TAG,"MAC BASCULA: "+ DEVICE_MAC_ADDRESS + ", MAC GBS2012B: " + mDeviceAddress);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                return;
            }
        }

        // Timer para mostrar el Circulo de Progreso
        cTimerProgress = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimerProgress.cancel();
                setVisibleView(circulodescarga,View.VISIBLE);
            }
        };
        cTimerProgress.start();

        int timeout = (int)(1000 * 120); // TimeOut 120 segundos para realizar la medición
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
        cTimer.start();

        LSBluetoothManager.getInstance().initManager(getApplicationContext());
        DeviceSetting.initActivityContext(this);

        conectar();
    }

    @Override
    public void onClick(View view) {
        Log.e(TAG, "onClick()");

        int viewId = view.getId();
        if (viewId == R.id.imageButtonAudio) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();

            texto.add("Coloque la báscula en el suelo, cerca de la Tablet.");
            texto.add("Súbase a la báscula y espere a que la medición finalice.");
            texto.add("Si sus pies están mojados o húmedos, séquelos bien antes de subir a la báscula.");
            texto.add("Una vez finalizada la medición, baje de la báscula.");

            textToSpeech.speak(texto);
            //endregion
        }
    }

    private boolean isDeviceExists(String name,String address) {
        if(listLSDevice!=null && listLSDevice.size()>0){
            for (int i = 0; i < listLSDevice.size(); i++){
                LSDeviceInfo tempDeInfo=listLSDevice.get(i);
                if (tempDeInfo!=null && tempDeInfo.getMacAddress()!=null
                        && tempDeInfo.getMacAddress().equalsIgnoreCase(address)){
                    return true;
                }
            }
            return false;
        }
        else return false;
    }

    private void conectar() {

//        setTextSatus(">> SEND CONNECT DEVICE MAC: " + deviceMac);

        LSManagerStatus status = LSBluetoothManager.getInstance().getManagerStatus();
        if(status == LSManagerStatus.Scanning){
            //stop
            LSBluetoothManager.getInstance().stopSearch();
        }
        else if(status == LSManagerStatus.Syncing){
            // Cuando hay que llamarlo repetidamente, hay que pararlo primero
            LSBluetoothManager.getInstance().stopDeviceSync();
        }

        //new device
        currentDevice = new LSDeviceInfo();
        currentDevice.setMacAddress(mDeviceAddress);
        currentDevice.setBroadcastID(mDeviceAddress.replace(":", ""));
        currentDevice.setDeviceType(LSDeviceType.WeightScale.getValue());
        currentDevice.setProtocolType(LSProtocolType.A5.toString());
        currentDevice.setDeviceName(mDeviceAddress);

        //clear measure device list
        LSBluetoothManager.getInstance().setDevices(null);

        Handler mainHandler = new Handler();
        mainHandler.post(new Runnable(){
            @Override
            public void run() {
//                setTextProgess("Connecting");
                connectDevice();
            }
        });
    }

    private void connectDevice() {

        contConnecting = 0;
        if (!LSBluetoothManager.getInstance().isSupportBLE()) {
            Toast.makeText(getApplicationContext(), "Bluetooth low energy no soportado", Toast.LENGTH_LONG).show();
            EVLog.log(TAG, "ERROR BLUETOOTH: Bluetooth low energy no soportado");
            failConnection();
            return;
        }
        if (!LSBluetoothManager.getInstance().isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext(), "Bluetooth desactivado", Toast.LENGTH_LONG).show();
            EVLog.log(TAG, "ERROR BLUETOOTH: Bluetooth desactivado");
            failConnection();
            return;
        }
        if (currentDevice == null) {
            Toast.makeText(getApplicationContext(), "No detectado dispositivo!", Toast.LENGTH_LONG).show();
            EVLog.log(TAG, "ERROR BLUETOOTH: No detectado dispositivo");
            failConnection();
            return;
        }
        if ( LSBluetoothManager.getInstance().getManagerStatus() == LSManagerStatus.Syncing
                && LSBluetoothManager.getInstance().checkConnectState(currentDevice.getMacAddress()) == LSConnectState.ConnectSuccess) {
            LSBluetoothManager.getInstance().resetSyncingListener(mSyncingCallback);
            EVLog.log(TAG, "ERROR: El Dispositivo ya está conectado.");
            failConnection();
            return;
        }

        // Obtener el estado de funcionamiento
        if (LSBluetoothManager.getInstance().getManagerStatus() == LSManagerStatus.Syncing) {
            // Estado de sincronización de los datos del dispositivo
            EVLog.log(TAG, "ERROR: El Dispositivo sincronizando datos");
            failConnection();
            return;
        }

        createConnection();
    }

    private void failConnection() {
//        status = EStatusDevice.Failed;
//        setTextView(txtStatus,status.toString());

        datos.setStatusDescarga(false);
        datos.setERROR("{\"type\":\"GBS2012B\",\"error\":803,\"description\":\"ERROR CON EN BLUTOOTH\"}");
        viewResult();
    }

    private void createConnection(){

        EVLog.log(TAG,"deviceMAC >> " + currentDevice.getMacAddress());

        // Stop
        LSBluetoothManager.getInstance().stopDiscovery();   // Cancelar la exploración de dispositivos Bluetooth 2.0
        LSBluetoothManager.getInstance().stopSearch();
        LSBluetoothManager.getInstance().stopDeviceSync();

        //clear measure device list
        LSBluetoothManager.getInstance().setDevices(null);

        //add target measurement device
        LSBluetoothManager.getInstance().addDevice(currentDevice);

        // Inicio del servicio de sincronización automática de los datos de medición del dispositivo
        //start data syncing service
        LSBluetoothManager.getInstance().startDeviceSync(mSyncingCallback);

        statusDevice = EStatusDevice.Connecting;
        setTextView(txtStatus,statusDevice.toString());

        EVLog.log(TAG, "Send connect devive");
    }

    /**
     * Vinculación del dispositivo Modo emparejamiento Auto
     *
     * @param mac
     * @param pairMode
     */

    //region :: CALLBACKS
    // CallBack SCAN device
    private OnSearchingListener mSearchCallback=new OnSearchingListener() {

        // Devolver los resultados de la exploración
        @Override
        public void onSearchResults(LSDeviceInfo lsDevice) {

            Log.e(TAG,"OnSearchingListener >> onSearchResults()");
            if(lsDevice==null){
                return ;
            }

            if(!isDeviceExists(lsDevice.getDeviceName(),lsDevice.getMacAddress())) {
                if (lsDevice.getDeviceName().contains("GBS-2012-B")) {
                    Log.e(TAG, "onSearchResults >> " + lsDevice.getDeviceName() + " [" + lsDevice.getMacAddress() + "]");
                    listLSDevice.add(lsDevice);
                    Log.e(TAG, ">> " + lsDevice.toString());
                }
            }
        }

        // Devolución de resultados de dispositivos conectados al sistema
        @Override
        public void onSystemConnectedDevice(String deviceName, String deviceMac) {
            Log.e("LS-BLE","OnSearchingListener >> onSystemConnectedDevice ???? >> "+deviceMac+"; ["+deviceName+"]");
            LSDeviceInfo lsDevice=new LSDeviceInfo();
            lsDevice.setDeviceType(LSDeviceType.WeightScale.getValue());
            lsDevice.setMacAddress(deviceMac);
            String broadcastId=deviceMac!=null?deviceMac.replace(":",""):deviceMac;
            lsDevice.setBroadcastID(broadcastId);
            lsDevice.setProtocolType(LSProtocolType.A5.toString());
            lsDevice.setDeviceName(deviceName);
        }

        // Devuelve el BluetoothDevice emparejados del sistema
        @SuppressLint("MissingPermission")
        @Override
        public void onSystemBondDevice(BluetoothDevice device) {
            Log.e("LS-BLE","OnSearchingListener >> onSystemBondDevice >> Emparajado: " + device);
        }

        // Interfaz clásica de retorno de resultados de exploración Bluetooth 2.0
        @SuppressLint("MissingPermission")
        @Override
        public void onBluetoothDeviceFound(BluetoothDevice device) {
            Log.e("LS-BLE","OnSearchingListener >> onBluetoothDeviceFound >> "+device);
        }
    };

    // CallBack Emparejamiento
    private OnPairingListener mPairCallback = new OnPairingListener() {

        // El estado de emparejamiento o vinculación del dispositivo cambia la devolución de llamada
        @Override
        public void onStateChanged(LSDeviceInfo lsDevice, int status) {
            super.onStateChanged(lsDevice, status);

            EVLog.log(TAG,"OnPairingListener >> onStateChanged()");

            if(status == ATPairResultsCode.PAIR_SUCCESSFULLY){
                // Enlazar correctamente, configurar el dispositivo para que se conecte directamente y omitir el primer ciclo de exploración.
                EVLog.log(TAG,"PAIR SUCCESSFULLY");
                LSScanIntervalConfig config = new LSScanIntervalConfig();
                config.setPairDevice(lsDevice);
                LSBluetoothManager.getInstance().setManagerConfig(config);
            }
//            else EVLog.log(TAG,"Pair Failure 01");

            if(status == 0 && lsDevice!=null){
                //TODO Pair Success
                EVLog.log(TAG,"Pair Success, lsDevice: " + lsDevice.toString());
                EVLog.log(TAG,"Pair Success");
//                setTextResult(lsDevice.toString());
//                setTextResult("Pair Success");
//                setVisibleProgressBar(View.INVISIBLE);
//                setVisibleView(btEmparejar,View.INVISIBLE);
            }
            else{
                //TODO Pair Failure
                EVLog.log(TAG,"Pair Failure, lsDevice: " + lsDevice.toString());
//                setTextResult(lsDevice.toString());
//                setTextResult("Pair Failure");
//                setVisibleProgressBar(View.INVISIBLE);
            }
        }

        // Durante el proceso de emparejamiento o vinculación, las instrucciones de funcionamiento o
        // notificaciones de información interactiva cargadas por el dispositivo
        @Override
        public void onMessageUpdate(String macAddress, LSDevicePairSetting msg) {
            EVLog.log(TAG,"OnPairingListener >> onMessageUpdate() msg: " +msg.toString());
            EVLog.log(TAG,"OnPairingListener >> onMessageUpdate() msg.getPairCmd(): " + msg.getPairCmd().toString());

            if (msg.getPairCmd() == LSPairCommand.PairConfirm) {
                Log.e(TAG,"OnPairingListener >> onMessageUpdate() >> PairConfirm");
                ATPairConfirmInfo pairedConfirmInfo = new ATPairConfirmInfo(ATPairConfirmState.Success);
                pairedConfirmInfo.setUserNumber(0);
                msg.setObj(pairedConfirmInfo);
                LSBluetoothManager.getInstance().pushPairSetting(macAddress, msg);
            }
            else if(msg.getPairCmd() == LSPairCommand.PairRequest){
                Log.e(TAG,"OnPairingListener >> onMessageUpdate() >> PairRequest");
                ATLoginInfo loginInfo=(ATLoginInfo)msg.getObj();
                if(loginInfo.getStateOfBond() == ATLoginInfo.STATE_BOUND){
                    msg.setObj(true);
                    LSBluetoothManager.getInstance().pushPairSetting(macAddress,msg);
                    //Se avisa al usuario de que el dispositivo está vinculado y debe restablecerse si se vuelve a vincular.
                    Log.e(TAG,"***** Dispositivo está vinculado y debe restablecerse si se vuelve a vincular.");
                }
                else{
                    //volver a emparejar
                    msg.setObj(true);
                    LSBluetoothManager.getInstance().pushPairSetting(macAddress,msg);
                }
            }
        }

/*        public void onMessageUpdate(String macAddress, LSDevicePairSetting msg) {
            super.onMessageUpdate(macAddress, msg);

            Log.e(TAG,"OnPairingListener >> onMessageUpdate()");
            // (Enter a random number confirmation)
            if(msg.getPairCmd() == LSPairCommand.RandomCodeConfirm){
                msg.setObj("123456");
                LSBluetoothManager.getInstance().pushPairSetting(macAddress, msg);
            }
            else if(msg.getPairCmd() == LSPairCommand.DeviceIdRequest){
                // ID
                msg.setObj(macAddress.replace(":",""));
                LSBluetoothManager.getInstance().pushPairSetting(macAddress,msg);
            }
        }*/
    };

    // Device Setting Listener
    private OnSettingListener mSettingListener = new OnSettingListener() {

        // Establecer devolución de llamada fallida
        @Override
        public void onFailure(int errorCode) {
            ENErrorCode err = ENErrorCode.toErrorCode(errorCode);
            Log.e(TAG,"OnSettingListener >> onFailure() >> errorCode=" + errorCode + ", "+ err.getMsg() );

//            setTextSatus(">> Setting fisnish");
//            setTextResult("Setting onFailure() >> Error: " + err.getMsg());
//            setVisibleProgressBar(View.INVISIBLE);

            querySetting = LSQuerySetting.None;
        }

        // Establecer devolución de llamada de éxito
        @Override
        public void onSuccess(String mac) {
            Log.e(TAG,"OnSettingListener >> onSuccess()");
            Log.e(TAG,"Setting Success: " + querySetting.getQuerySetting());
            Log.e(TAG,"onSuccess(): " + querySetting.getQuerySetting());

//            setTextResult("Setting Success: " + querySetting.getQuerySetting());

            if (querySetting == LSQuerySetting.Language) {
//                String value = (String) cmbBrazo.getSelectedItem();
//                Log.e(TAG, "BRAZO " + value);
//                setTextSatus(">> BRAZO " + value);
//                setTextResult("Setting Brazo: " + value);
//                querySetting = LSQuerySetting.Arm;
//                DeviceSetting.updateWearingStyles(deviceMac,value,mSettingListener);
            }

            else if (querySetting == LSQuerySetting.fisnish) {
                querySetting = LSQuerySetting.None;
                Log.e(TAG, "Setting fisnish");
//                setVisibleProgressBar(View.INVISIBLE);
//                setTextSatus(">> Setting fisnish");
            }

        }

        // Devolución de llamada de progreso de descarga de archivos o sincronización de datos
        @Override
        public void onProgressUpdate(String deviceMac, int value) {
            Log.e(TAG,"OnSettingListener >> onProgressUpdate()");
        }

        // Llamada de retorno al estado de descarga de archivos o sincronización de datos
        @Override
        public void onStateChanged(String deviceMac, int state, int errorCode) {
            Log.e(TAG,"OnSettingListener >> onStateChanged()");
            Log.e(TAG,"onStateChanged >> "+state + " ; errorCode="+errorCode);
        }

        // Llamada de datos del dispositivo
        @Override
        public void onDataUpdate(Object obj) {
            Log.e(TAG,"OnSettingListener >> onDataUpdate()");
//            if(obj!=null && obj instanceof ATDeviceData){
//                ATDeviceData data = (ATDeviceData)obj;
//                showDeviceMeasuringData(data, DeviceDataUtils.byte2hexString(data.getSrcData()));
//            }
//            else{
//                showDeviceMeasuringData(obj,null);
//            }
        }
    };

    // CallBack Estado Conecxión y Data receiver
    private OnSyncingListener mSyncingCallback = new OnSyncingListener() {

        // Devolución de llamada de cambios de estado de conexión del dispositivo
        @Override
        public void onStateChanged(String broadcastId, LSConnectState state) {
            super.onStateChanged(broadcastId, state);

            // LSConnectState >> [Unknown(0), Connecting(1), GattConnected(2), ConnectSuccess(3), ConnectFailure(4), Disconnect(5), RequestDisconnect(6)]
            Log.e(TAG,"OnSyncingListener >> onStateChanged() broadcastId: " + broadcastId + ", state: " + state.name());

            if (LSConnectState.Connecting == state) {
                EVLog.log(TAG,"STATE: Connecting");

                contConnecting += 1;
                Log.e(TAG,"STATE: Connecting " + contConnecting.toString());

                setTextView(txtStatus,statusDevice.toString() + " " + contConnecting);

                if (contConnecting == 4) {
                    LSManagerStatus sdkStatus=LSBluetoothManager.getInstance().getManagerStatus();
                    if(sdkStatus == LSManagerStatus.Scanning){
                        //stop scan
                        LSBluetoothManager.getInstance().stopSearch();
                    }
                    if(sdkStatus == LSManagerStatus.Syncing){
                        //stop device data sync
                        LSBluetoothManager.getInstance().stopDeviceSync();
                        Log.e(TAG,"!!! Connection time expired.");
                    }
                    contConnecting = 0;
                }

            }
            else if (LSConnectState.GattConnected == state) {
                EVLog.log(TAG,"STATE: GattConnected");
//                setTextResult("STATE: GattConnected");
            }
            else if (LSConnectState.ConnectSuccess == state) {
                EVLog.log(TAG,"STATE: Connect Device Success");
                statusDevice = EStatusDevice.Connected;
                setTextView(txtStatus,statusDevice.toString());
            }
            else if (LSConnectState.ConnectFailure == state) {
                EVLog.log(TAG,"STATE: Connect Device Failure");
                statusDevice = EStatusDevice.Failed;
                setTextView(txtStatus,statusDevice.toString());
            }
            else if (LSConnectState.Disconnect == state) {
                EVLog.log(TAG, "STATE: Device Disconnect");
                statusDevice = EStatusDevice.Disconnect;
                setTextView(txtStatus,statusDevice.toString());
            }
            else if (LSConnectState.RequestDisconnect == state) {
                // Solicitar una desconexión
                statusDevice = EStatusDevice.Disconnecting;
                setTextView(txtStatus,statusDevice.toString());
            }
            else {
                EVLog.log(TAG, "State Change Device: Unknown");
                statusDevice = EStatusDevice.Unknown;
                setTextView(txtStatus,statusDevice.toString());
            }
        }

        // Devolución de los datos de medición de la pulsera de actividad o del reloj.
        @Override
        public void onActivityTrackerDataUpdate(String mac, int type, ATDeviceData data){
            super.onActivityTrackerDataUpdate(mac, type, data);
            if (data == null) { return; }
            Log.e(TAG,"OnSyncingListener >> onActivityTrackerDataUpdate() data >> " + data.toString());
        }

        // Actualización de la información del dispositivo
        @Override
        public void onDeviceInformationUpdate(String broadcastId, LSDeviceInfo lsDevice) {
            super.onDeviceInformationUpdate(broadcastId, lsDevice);

            Log.e(TAG,"OnSyncingListener >> onDeviceInformationUpdate()");

            if (lsDevice == null || currentDevice == null) {
                return;
            }
            Log.e("LS-BLE", "Demo-Update Device Info:" + lsDevice.toString());

        }

        // Otras retrollamadas de datos de notificación, como el estado de la medición o información sobre errores.
        @Override
        public void onNotificationDataUpdate(String devMac, IDeviceData obj) {

            Log.e(TAG,"OnSyncingListener >> onNotificationDataUpdate()");
            Log.e(TAG,"onNotificationDataUpdate >> " + obj.toString());

        }

        // Devolución de datos de medición de la tensión arterial
        @Override
        public void onBloodPressureDataUpdate(String broadcastId, LSBloodPressure bloodPressure) {
            Log.e(TAG,"OnSyncingListener >> onBloodPressureDataUpdate()");
        }

        // Devolución de datos de medición del medidor de glucosa en sangre
        @Override
        public void onBloodGlucoseDataUpdate(java.lang.String broadcastId, BGDataSummary summary){
            Log.e(TAG,"OnSyncingListener >> onBloodGlucoseDataUpdate()");
        }

        // Devolución de datos medición de peso
        @Override
        public void onScaleWeightDataUpdate(String broadcastId, LSScaleWeight weight) {
            Log.e(TAG,"OnSyncingListener >> onScaleWeightDataUpdate()");
            Log.e(TAG,"onScaleWeightDataUpdate >> " + weight.toString());

            Log.e(TAG,"Peso: " + weight.getWeight() + " kg, remainCount: " + weight.getRemainCount() + ", isRealTimeData: " + weight.isRealtimeData());

            if (weight.getRemainCount() == 0) {
                Log.e(TAG,"ultima medición: " + weight.getWeight());
                datos.setStatusDescarga(true);
                datos.setWeightMeasurement(weight,paciente.getHeight());
                viewResult();
            }
        }
    };
    //endregion

    @Override
    protected  void onStop() {
        super.onStop();
        Log.e(TAG,"onStop()");

//        LSManagerStatus sdkStatus= LSBluetoothManager.getInstance().getManagerStatus();
//        if(sdkStatus == LSManagerStatus.Scanning){
//            //stop scan
//            Log.e(TAG,"stop scan");
//            LSBluetoothManager.getInstance().stopSearch();
//        }
//        if(sdkStatus == LSManagerStatus.Syncing){
//            //stop device data sync
//            Log.e(TAG,"stop device data sync");
//            LSBluetoothManager.getInstance().stopDeviceSync();
//        }
//
//        LSBluetoothManager.getInstance().unregisterBluetoothReceiver();
//        finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG,"onDestroy()");

        textToSpeech.shutdown();
//        if (cTimerView != null) cTimerView.cancel();

        LSManagerStatus sdkStatus=LSBluetoothManager.getInstance().getManagerStatus();
        if(sdkStatus == LSManagerStatus.Scanning){
            //stop scan
            LSBluetoothManager.getInstance().stopSearch();
        }
        if(sdkStatus == LSManagerStatus.Syncing){
            //stop device data sync
            LSBluetoothManager.getInstance().stopDeviceSync();
        }
        LSBluetoothManager.getInstance().unregisterBluetoothReceiver();

        super.onDestroy();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}

    @SuppressLint("MissingPermission")
    public void viewResult() {

        if (viewdata == false) {
            viewdata = true;
            EVLog.log(TAG, " ViewResult()");

            //region .. stop timers
            if (cTimer != null) cTimer.cancel();
            cTimer = null;

            if (cTimerProgress != null) cTimerProgress.cancel();
            cTimerProgress = null;
            //endregion

            setVisibleView(circulodescarga, View.INVISIBLE);

            if (datos.getStatusDescarga()) {
                Intent intent = new Intent(this, view_dataGBS2012B.class);
                intent.putExtra("typeScale", TypeDevice);
                startActivity(intent);
            }
            else {
                Intent intent = new Intent(this, view_failGBS2012B.class);
                intent.putExtra("typeScale", TypeDevice);
                startActivity(intent);
            }

            Log.e(TAG,"datos.getStatusDescarga(): " + datos.getStatusDescarga());

            finish();
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
}