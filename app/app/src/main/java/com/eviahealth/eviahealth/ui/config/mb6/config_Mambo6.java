package com.eviahealth.eviahealth.ui.config.mb6;

import androidx.annotation.NonNull;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.transtek.mb6.models.setting.DeviceSetting;
import com.eviahealth.eviahealth.models.transtek.mb6.models.setting.ENErrorCode;
import com.eviahealth.eviahealth.models.transtek.mb6.models.setting.LSQuerySetting;
import com.eviahealth.eviahealth.models.transtek.mb6.models.setting.PairMode;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.utils.PermissionUtils;
import com.eviahealth.eviahealth.utils.log.EVLog;
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
import com.lifesense.plugin.ble.data.tracker.ATConfigItemData;
import com.lifesense.plugin.ble.data.tracker.ATDeviceData;
import com.lifesense.plugin.ble.data.tracker.ATDeviceInfo;
import com.lifesense.plugin.ble.data.tracker.ATLoginInfo;
import com.lifesense.plugin.ble.data.tracker.ATPairConfirmInfo;
import com.lifesense.plugin.ble.data.tracker.ATPairConfirmState;
import com.lifesense.plugin.ble.data.tracker.ATPairResultsCode;
import com.lifesense.plugin.ble.data.tracker.ATUploadDoneNotify;
import com.lifesense.plugin.ble.data.tracker.config.ATBloodOxygenMonitor;
import com.lifesense.plugin.ble.data.tracker.config.ATConfigItem;
import com.lifesense.plugin.ble.data.tracker.config.ATDisturbMode;
import com.lifesense.plugin.ble.data.tracker.config.ATHeartRateSwitch;
import com.lifesense.plugin.ble.data.tracker.config.ATMeasureUnit;
import com.lifesense.plugin.ble.data.tracker.setting.ATConfigItemSetting;
import com.lifesense.plugin.ble.data.tracker.setting.ATConfigQueryCmd;
import com.lifesense.plugin.ble.data.tracker.setting.ATConfigQuerySetting;
import com.lifesense.plugin.ble.data.tracker.setting.ATDataQueryCmd;
import com.lifesense.plugin.ble.data.tracker.setting.ATDataQuerySetting;
import com.lifesense.plugin.ble.data.tracker.setting.ATDistanceFormatSetting;
import com.lifesense.plugin.ble.data.tracker.setting.ATSedentaryItem;
import com.lifesense.plugin.ble.data.tracker.setting.ATVibrationMode;

import java.util.ArrayList;

public class config_Mambo6 extends BaseActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    final static String TAG = "CONFIG-MAMBO6";

    String deviceName = "IF1 05E1";
    //    String deviceMac = "CE:23:58:51:05:00"; // MALA
    String deviceMac = ""; //""CE:23:58:51:05:E1";

    private static PairMode devicePairMode = PairMode.Auto;
    private LSDeviceInfo currentDevice = null;
    ATDeviceInfo deviceInfo = null;

    private ArrayList<LSDeviceInfo> listLSDevice = new ArrayList<>();
    LSQuerySetting querySetting = LSQuerySetting.None;

    CountDownTimer cTimerView = null;
    CountDownTimer cTimerDownload = null;   // TIMEOUT >> Download Data

    TextView txtTitulo, txtStatus, txtResult, txtProgress;
    Spinner cmbLanguage, cmbBrazo, cmbQuietMode, cmbHeartRate, cmbBloodOxygen, cmbInactividad;
    Button btEmparejar, btClearData, btConfigurar;
    ProgressBar circulodescarga;

    String textResult = "";
    private Integer contConnecting = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_mambo6);
        Log.e(TAG,"onCreate()");

        //region >> Views
        txtTitulo = findViewById(R.id.CMB6_txtTitulo);
        txtTitulo.setVisibility(View.VISIBLE);

        txtStatus = findViewById(R.id.CMB6_txtStatus);
        txtStatus.setVisibility(View.VISIBLE);

        txtResult = findViewById(R.id.CMB6_txtResult);
        txtResult.setVisibility(View.VISIBLE);
        txtResult.setMovementMethod(new ScrollingMovementMethod());

        txtProgress = findViewById(R.id.CMB6_txtProgress);
        txtProgress.setVisibility(View.INVISIBLE);

        // spinners
        cmbLanguage = findViewById(R.id.CMB6_cmbLanguage);
        cmbLanguage.setOnItemSelectedListener(this);

        cmbBrazo = findViewById(R.id.CMB6_cmbBrazo);
        cmbBrazo.setOnItemSelectedListener(this);

        cmbQuietMode = findViewById(R.id.CMB6_cmbQuietMode);
        cmbQuietMode.setOnItemSelectedListener(this);

        cmbHeartRate = findViewById(R.id.CMB6_cmbHeartRate);
        cmbHeartRate.setOnItemSelectedListener(this);

        cmbBloodOxygen = findViewById(R.id.CMB6_cmbBloodOxygen);
        cmbBloodOxygen.setOnItemSelectedListener(this);

        cmbInactividad = findViewById(R.id.CMB6_cmbInactividad);
        cmbInactividad.setOnItemSelectedListener(this);

        // buttons
        btEmparejar = findViewById(R.id.CMB6_btEmparejar);
        btEmparejar.setVisibility(View.INVISIBLE);
        btClearData = findViewById(R.id.CMB6_btClearData);
        btClearData.setVisibility(View.INVISIBLE);
        btConfigurar = findViewById(R.id.CMB6_btConfigurar);
        btConfigurar.setVisibility(View.INVISIBLE);

        circulodescarga = findViewById(R.id.CMB6_ProgressBar);
        setVisibleProgressBar(View.INVISIBLE);

        //endregion

        PermissionUtils.requestAll(this);

        Intent intent = getIntent();
        Bundle b = intent.getExtras();
        if (b != null) {
            deviceMac = intent.getStringExtra("deviceMac");
            setTextTitulo("Configuración dispositivo MAMBO 6 [" + deviceMac + "]");
        }

        setRellenarLanguage();
        setRellenarBrazo();
        setRellenarHabilitarDeshabilitar(cmbQuietMode);
        setRellenarHabilitarDeshabilitar(cmbHeartRate);
        setRellenarHabilitarDeshabilitar(cmbBloodOxygen);
        setRellenarHabilitarDeshabilitar(cmbInactividad);

        //init LSBluetoothManager Transtek
        LSBluetoothManager.getInstance().initManager(getApplicationContext());
        //register bluetooth state change receiver
//        LSBluetoothManager.getInstance().registerBluetoothReceiver(getApplicationContext());

        DeviceSetting.initActivityContext(this);

        clearTextResult();

        //region :: Timer ProcressBar Visible (1 segudos) >> VISIBLE + CONNECT DEVICE
        cTimerView = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimerView.cancel();
                cTimerView = null;
                clearTextResult();
                conectar();
            }
        };
        cTimerView.start();
        //endregion

        //region :: TIMER >> TIMEOUT Download Data
        cTimerDownload = new CountDownTimer(1000 * 30, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                cTimerDownload.cancel();

                setTextResult("(E) TimeOut Download Clear Data");

                EVLog.log(TAG, "TIMEOUT: DOWNLOAD DATA");
                setVisibleProgressBar(View.INVISIBLE);
            }
        };
        //endregion
    }

    // region :: ONCLICK
    @Override
    public void onClick(View view) {
        Log.e(TAG, " onClick()");
        LSManagerStatus sdkStatus = LSBluetoothManager.getInstance().getManagerStatus();
        setTextResult("");

        int viewId = view.getId();
        if (viewId == R.id.CMB6_btConfigurar) {
            //region :: CONFIGURAR MAMBO 6
            LSConnectState connectState = LSBluetoothManager.getInstance().checkConnectState(deviceMac);
            if (connectState == LSConnectState.ConnectSuccess) {
                if (sdkStatus == LSManagerStatus.Free) {
                    Log.e(TAG, "Reconectando dispositivo");
                    LSBluetoothManager.getInstance().stopSearch();
                    LSBluetoothManager.getInstance().stopDeviceSync();

                    clearTextResult();
                    setTextResult("Reconectado con la pulsera. Una vez finalice vuelva a pulsar el botón de configurar.");
                    conectar();
                    return;
                }
            }
            //:: Cambio de idioma
            String language = (String) cmbLanguage.getSelectedItem();
            Log.e(TAG, "CHANGE LANGUAGE TO " + language);
            setTextSatus(">> CHANGE LANGUAGE TO " + language);
            clearTextResult();
            setTextResult("Setting language: " + language);
            setTextProgess("setting");
            setVisibleProgressBar(View.VISIBLE);
            querySetting = LSQuerySetting.Language;
            DeviceSetting.setDeviceLanguage(deviceMac, language, mSettingListener);
            //endregion
        }
        else if (viewId == R.id.CMB6_btEmparejar) {
            //region :: EMPAREJAR MAMBO 6
            String macdevice = deviceMac;
            setTextSatus(">> Binding Device(" + macdevice + ")");
            setTextProgess("Pairing");

            if (sdkStatus == LSManagerStatus.Scanning) {
                //stop
                LSBluetoothManager.getInstance().stopSearch();
            } else if (sdkStatus == LSManagerStatus.Syncing) {
                // Cuando hay que llamarlo repetidamente, hay que pararlo primero
                LSBluetoothManager.getInstance().stopDeviceSync();
            }

            bindingDevice(macdevice, devicePairMode);

            //endregion
        }
        else if (viewId == R.id.CMB6_btClearData) {
            //region :: Clear Data
            clearAllData();
            //endregion
        }
        else if (viewId == R.id.CMB6_imfAtras) {
            //region :: STOP MAMBO 6
            setTextSatus("onClick() >> VOLVER");
            LSBluetoothManager.getInstance().stopSearch();
            LSBluetoothManager.getInstance().stopDeviceSync();
            finish();
            //endregion
        }

    }
    //endregion

    //region :: Callback Permisos denegate
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtils.PERMISSION_CODE) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    PermissionUtils.handleRequestFailed(this, permissions[i]);
                    Log.e("MAIN","PERMISSION_DENIED: " + permissions[i]);
                }
            }
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

        LSManagerStatus sdkStatus= LSBluetoothManager.getInstance().getManagerStatus();
        if(sdkStatus == LSManagerStatus.Scanning){
            //stop scan
            Log.e(TAG,"stop scan");
            LSBluetoothManager.getInstance().stopSearch();
        }
        if(sdkStatus == LSManagerStatus.Syncing){
            //stop device data sync
            Log.e(TAG,"stop device data sync");
            LSBluetoothManager.getInstance().stopDeviceSync();
        }
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
        super.onDestroy();

        if (cTimerView != null) cTimerView.cancel();

        LSManagerStatus sdkStatus=LSBluetoothManager.getInstance().getManagerStatus();
        if(sdkStatus == LSManagerStatus.Scanning){
            //stop scan
            LSBluetoothManager.getInstance().stopSearch();
        }
        if(sdkStatus == LSManagerStatus.Syncing){
            //stop device data sync
            LSBluetoothManager.getInstance().stopDeviceSync();
        }

//        LSBluetoothManager.getInstance().unregisterBluetoothReceiver();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}
    //endregion

    //region :: runOnUiThread VIEWs
    private void setTextSatus(String texto) {
        runOnUiThread(new Runnable() {
            public void run() {
                txtStatus.setText(texto);
            }
        });
    }

    private void setTextResult(String texto) {
        runOnUiThread(new Runnable() {
            public void run() {
                String txt = txtResult.getText().toString();
                txtResult.setText(txt + "\n" + texto);
            }
        });
    }

    private void setTextView(TextView view,String texto) {
        runOnUiThread(new Runnable() {
            public void run() {
                view.setText(texto);
            }
        });
    }

    private void clearTextResult() {
        runOnUiThread(new Runnable() {
            public void run() {
                txtResult.setText("");
            }
        });
    }

    private void setTextTitulo(String texto) {
        runOnUiThread(new Runnable() {
            public void run() {
                txtTitulo.setText(texto);
            }
        });
    }

    private void setTextProgess(String texto) {
        runOnUiThread(new Runnable() {
            public void run() {
                txtProgress.setText(texto);
            }
        });
    }

    private void setVisibleProgressBar(int state) {
        runOnUiThread(new Runnable() {
            public void run() {
                circulodescarga.setVisibility(state);

                if (state == View.VISIBLE) {
                    setEnableSpinners(false);

                    setVisibleView(txtProgress,View.VISIBLE);
                    setEnableView(btEmparejar,false);
                    setEnableView(btClearData,false);
                    setEnableView(btConfigurar,false);
                }
                else {
                    setEnableSpinners(true);
                    setVisibleView(txtProgress,View.INVISIBLE);
                    setEnableView(btEmparejar,true);
                    setEnableView(btClearData,true);
                    setEnableView(btConfigurar,true);
                }

            }
        });
    }

    private void setVisibleView(View view, int state) {
        runOnUiThread(new Runnable() {
            public void run() {
                view.setVisibility(state);
            }
        });
    }

    private void setEnableView(View view, boolean enable) {
        runOnUiThread(new Runnable() {
            public void run() {
                view.setEnabled(enable);
            }
        });
    }

    private void setSelectionIdSpinner(Spinner spn, int id) {
        runOnUiThread(new Runnable() {
            public void run() {
                spn.setSelection(id);
            }
        });
    }

    private void setNumberPicker(NumberPicker view,int number) {
        runOnUiThread(new Runnable() {
            public void run() {
                view.setValue(number);
            }
        });
    }

    //endregion

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

        setVisibleProgressBar(View.VISIBLE);

        setTextSatus(">> SEND CONNECT DEVICE MAC: " + deviceMac);

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
        currentDevice.setMacAddress(deviceMac);
        currentDevice.setBroadcastID(deviceMac.replace(":", ""));
        currentDevice.setDeviceType(LSDeviceType.ActivityTracker.getValue());
        currentDevice.setProtocolType(LSProtocolType.A5.toString());
        currentDevice.setDeviceName(deviceName);

        //clear measure device list
        LSBluetoothManager.getInstance().setDevices(null);

        Handler mainHandler = new Handler();
        mainHandler.post(new Runnable(){
            @Override
            public void run() {
                setTextProgess("Connecting");
                connectDevice();
            }
        });
    }

    private void connectDevice() {

        contConnecting = 0;
        if (!LSBluetoothManager.getInstance().isSupportBLE()) {
            Toast.makeText(getApplicationContext(), "Bluetooth low energy no soportado", Toast.LENGTH_LONG).show();
            EVLog.log(TAG, "ERROR BLUETOOTH: Bluetooth low energy no soportado");
            return;
        }
        if (!LSBluetoothManager.getInstance().isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext(), "Bluetooth desactivado", Toast.LENGTH_LONG).show();
            EVLog.log(TAG, "ERROR BLUETOOTH: Bluetooth desactivado");
            return;
        }
        if (currentDevice == null) {
            Toast.makeText(getApplicationContext(), "No detectado dispositivo!", Toast.LENGTH_LONG).show();
            EVLog.log(TAG, "ERROR BLUETOOTH: No detectado dispositivo");
            return;
        }
        if ( LSBluetoothManager.getInstance().getManagerStatus() == LSManagerStatus.Syncing
                && LSBluetoothManager.getInstance().checkConnectState(currentDevice.getMacAddress()) == LSConnectState.ConnectSuccess) {
            LSBluetoothManager.getInstance().resetSyncingListener(mSyncingCallback);
            EVLog.log(TAG, "ERROR: El Dispositivo ya está conectado.");
            return;
        }

        // Obtener el estado de funcionamiento
        if (LSBluetoothManager.getInstance().getManagerStatus() == LSManagerStatus.Syncing) {
            // Estado de sincronización de los datos del dispositivo
            EVLog.log(TAG, "ERROR: El Dispositivo sincronizando datos");
            return;
        }

        createConnection();
    }

    private void createConnection(){

        EVLog.log(TAG,"deviceMAC >> "+currentDevice.getMacAddress());

        // Stop
        LSBluetoothManager.getInstance().stopDiscovery();   // Cancelar la exploración de dispositivos Bluetooth 2.0
        LSBluetoothManager.getInstance().stopSearch();
        LSBluetoothManager.getInstance().stopDeviceSync();

        //clear measure device list
        LSBluetoothManager.getInstance().setDevices(null);

        //add target measurement device
        LSBluetoothManager.getInstance().addDevice(currentDevice);
        //start data syncing service
        LSBluetoothManager.getInstance().startDeviceSync(mSyncingCallback);

        EVLog.log(TAG, "Send connect devive");
    }

    /**
     * Vinculación del dispositivo Modo emparejamiento Auto
     *
     * @param mac
     * @param pairMode
     */
    private void bindingDevice(String mac, PairMode pairMode) {

        LSDeviceInfo deviceInfo = new LSDeviceInfo();
        deviceInfo.setMacAddress(mac);
        deviceInfo.setBroadcastID(mac.replace(":", ""));
        deviceInfo.setDeviceType(LSDeviceType.ActivityTracker.getValue());
        deviceInfo.setProtocolType(LSProtocolType.A5.toString());
        deviceInfo.setPairMode(pairMode.getValue());

        //delay 3 seconds to binding if need or test
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                clearTextResult();
                setVisibleProgressBar(View.VISIBLE);
                LSBluetoothManager.getInstance().pairDevice(deviceInfo, mPairCallback);
            }
        }, 1 * 1000L);

    }

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

    //region :: CALLBACKS
    // CallBack SCAN device
    private OnSearchingListener mSearchCallback=new OnSearchingListener() {

        // Devolver los resultados de la exploración
        @Override
        public void onSearchResults(LSDeviceInfo lsDevice) {

            if(lsDevice==null){
                return ;
            }

            if(!isDeviceExists(lsDevice.getDeviceName(),lsDevice.getMacAddress())) {
                if (lsDevice.getDeviceName().contains("IF1")) {
                    Log.e(TAG, "onSearchResults >> " + lsDevice.getDeviceName() + " [" + lsDevice.getMacAddress() + "]");
                    listLSDevice.add(lsDevice);
                    textResult += lsDevice.getDeviceName() + " [" + lsDevice.getMacAddress() + "]\n";
                    Log.e(TAG, ">> " + lsDevice.toString());

//                    if (lsDevice.getMacAddress().equals(editMacDevice.getText().toString())) {
//                        currentDevice = lsDevice;
//                    }

                }
            }
        }

        // Devolución de resultados de dispositivos conectados al sistema
        @Override
        public void onSystemConnectedDevice(String deviceName, String deviceMac) {
            Log.e("LS-BLE","OnSearchingListener >> onSystemConnectedDevice ???? >> "+deviceMac+"; ["+deviceName+"]");
            LSDeviceInfo lsDevice=new LSDeviceInfo();
            lsDevice.setDeviceType(LSDeviceType.ActivityTracker.getValue());
            lsDevice.setMacAddress(deviceMac);
            String broadcastId=deviceMac!=null?deviceMac.replace(":",""):deviceMac;
            lsDevice.setBroadcastID(broadcastId);
            lsDevice.setProtocolType(LSProtocolType.A5.toString());
            lsDevice.setDeviceName(deviceName);

            //update scan results
            //updateScanResults(lsDevice);
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
                setTextResult(lsDevice.toString());
                setTextResult("Pair Success");
                setVisibleProgressBar(View.INVISIBLE);
                setVisibleView(btEmparejar,View.INVISIBLE);
            }
            else{
                //TODO Pair Failure
                EVLog.log(TAG,"Pair Failure, lsDevice: " + lsDevice.toString());
                setTextResult(lsDevice.toString());
                setTextResult("Pair Failure");
                setVisibleProgressBar(View.INVISIBLE);
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

            setTextSatus(">> Setting fisnish");
            setTextResult("Setting onFailure() >> Error: " + err.getMsg());

            setVisibleProgressBar(View.INVISIBLE);
            querySetting = LSQuerySetting.None;
        }

        // Establecer devolución de llamada de éxito
        @Override
        public void onSuccess(String mac) {
            Log.e(TAG,"OnSettingListener >> onSuccess()");
            Log.e(TAG,"Setting Success: " + querySetting.getQuerySetting());

            setTextResult("Setting Success: " + querySetting.getQuerySetting());

            if (querySetting == LSQuerySetting.Language) {
                String value = (String) cmbBrazo.getSelectedItem();
                Log.e(TAG, "BRAZO " + value);
                setTextSatus(">> BRAZO " + value);
                setTextResult("Setting Brazo: " + value);

                querySetting = LSQuerySetting.Arm;
                DeviceSetting.updateWearingStyles(deviceMac,value,mSettingListener);
            }
            else if (querySetting == LSQuerySetting.Arm) {
                Log.e(TAG, "Update Time Format 24H");
                setTextSatus(">> Update Time Format 24H");
                setTextResult("Setting Update Time Format 24H");

                querySetting = LSQuerySetting.TimeFormat;
                DeviceSetting.updateTimeFormat(deviceMac,mSettingListener);
            }
            else if (querySetting == LSQuerySetting.TimeFormat) {
                int type = 0;
                String language = (String) cmbLanguage.getSelectedItem();
                if (language.equals("English")) { type = ATDistanceFormatSetting.DISTANCE_FORMAT_MILE; }
                else { type = ATDistanceFormatSetting.DISTANCE_FORMAT_KILOMETER; }

                String unit = (type == ATDistanceFormatSetting.DISTANCE_FORMAT_KILOMETER) ? "Km" : "Mile";

                Log.e(TAG, "Distance Unit " + unit);
                setTextSatus(">> Distance Unit " + unit);
                setTextResult("Setting Distance Unit " + unit);

                querySetting = LSQuerySetting.DistanceUnit;
                DeviceSetting.updateDistanceUnit(deviceMac,type,mSettingListener);
            }
            else if (querySetting == LSQuerySetting.DistanceUnit) {
                int type = 0;
                String language = (String) cmbLanguage.getSelectedItem();
                if (language.equals("English")) { type = ATMeasureUnit.UNIT_IMPERIAL; }
                else { type = ATMeasureUnit.UNIT_METRIC; }

                String unit = (type == ATMeasureUnit.UNIT_METRIC) ? "Metric" : "Imperial";

                Log.e(TAG, "Measurement Unit " + unit);
                setTextSatus(">> Measurement Unit " + unit);
                setTextResult("Setting Measurement Unit " + unit);

                querySetting = LSQuerySetting.MeasurementUnit;
                DeviceSetting.measurementUnitSetting(deviceMac,type,mSettingListener);
            }
            else if (querySetting == LSQuerySetting.MeasurementUnit) {
                String value = (String) cmbQuietMode.getSelectedItem();
                boolean enable = (value.equals("Habilitar")) ? true : false;

                Log.e(TAG, "Quiet Mode " + value);
                setTextSatus(">> Quiet Mode " + value);
                setTextResult("Setting Quiet Mode - " + value);

                querySetting = LSQuerySetting.QuietMode;
                DeviceSetting.updateQuietModeSetting(deviceMac,"00:00","23:59",enable,mSettingListener);
            }
            else if (querySetting == LSQuerySetting.QuietMode) {
                String value = (String) cmbHeartRate.getSelectedItem();
                boolean enable = (value.equals("Habilitar")) ? true : false;

                Log.e(TAG, "Realtime Heart Rate " + value);
                setTextSatus(">> Realtime Heart Rate " + value);
                setTextResult("Setting Realtime Heart Rate - " + value);

                querySetting = LSQuerySetting.RealtimeHeartRate;
                DeviceSetting.syncRealtimeHeartRate(deviceMac, enable, mSettingListener);
            }
            else if (querySetting == LSQuerySetting.RealtimeHeartRate) {
                String value = (String) cmbBloodOxygen.getSelectedItem();
                boolean enable = (value.equals("Habilitar")) ? true : false;

                Log.e(TAG, "Realtime Blood Oxygen " + value);
                setTextSatus(">> Realtime Blood Oxygen " + value);
                setTextResult("Setting Realtime Blood Oxygen - " + value);

                querySetting = LSQuerySetting.RealtimeBloodOxygen;
                DeviceSetting.syncRealtimeBloodOxygen(deviceMac, enable, mSettingListener);
            }
            else if (querySetting == LSQuerySetting.RealtimeBloodOxygen) {
                Log.e(TAG, "Sedentary Info");
                setTextSatus(">> Sedentary Info");
                setTextResult("Setting Sedentary Info");

                querySetting = LSQuerySetting.SedentaryInfo;

                String value = (String) cmbInactividad.getSelectedItem();
                boolean enable = (value.equals("Habilitar")) ? true : false;

                ATSedentaryItem sedentary = new ATSedentaryItem();
                sedentary.setEnable(enable);            //开关
                sedentary.setSedentaryTime(120);        //设置久坐时间，即在2分钟内，手环步数据无更新或处于静止状态时
                sedentary.setStartTime("08:30");        //开始时间
                sedentary.setEndTime("20:30");          //结束时间
                sedentary.setRepeatDay(DeviceSetting.getWeekDays());    //重复星期

                sedentary.setVibrationMode(ATVibrationMode.Continuous);//振动方式
                sedentary.setVibrationStrength1(7);    //振动等级1
                sedentary.setVibrationStrength2(5);    //振动等级2
                sedentary.setVibrationTime(10);        //振动时间

                DeviceSetting.updateSedentaryInfo(deviceMac, sedentary, mSettingListener);
            }
            else if (querySetting == LSQuerySetting.SedentaryInfo) {
                Log.e(TAG, "Update User Info");
                setTextSatus(">> Update User Info");
                setTextResult("Setting User Info");

                querySetting = LSQuerySetting.WatchPage;
                DeviceSetting.updateCustomPage(deviceMac, mSettingListener);
            }
            else if (querySetting == LSQuerySetting.WatchPage) {
                Log.e(TAG, "Update Datetime");
                setTextSatus(">> Update Datetime");
                setTextResult("Setting Update Datetime");

                querySetting = LSQuerySetting.UpdateDatetime;
                DeviceSetting.setUpdateDateTime(deviceMac,mSettingListener);
            }
            else if (querySetting == LSQuerySetting.UpdateDatetime) {
                querySetting = LSQuerySetting.fisnish;
                // Descarga la configuracion actual
                ATConfigQuerySetting setting = new ATConfigQuerySetting(ATConfigQueryCmd.Settings);
                LSBluetoothManager.getInstance().queryDeviceData(deviceMac, setting, mSettingListener);
            }
            else if (querySetting == LSQuerySetting.fisnish) {
                setVisibleProgressBar(View.INVISIBLE);
                querySetting = LSQuerySetting.None;
                Log.e(TAG, "Setting fisnish");
                setTextSatus(">> Setting fisnish");
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
                setTextResult("STATE: Connecting " + contConnecting.toString());
                if (contConnecting == 4) {
                    LSManagerStatus sdkStatus=LSBluetoothManager.getInstance().getManagerStatus();
                    if(sdkStatus == LSManagerStatus.Scanning){
                        //stop scan
                        LSBluetoothManager.getInstance().stopSearch();
                    }
                    if(sdkStatus == LSManagerStatus.Syncing){
                        //stop device data sync
                        LSBluetoothManager.getInstance().stopDeviceSync();
                        setVisibleProgressBar(View.INVISIBLE);
                        setEnableSpinners(false);
                        setTextResult("!!! Connection time expired.");
                    }
                    contConnecting = 0;
                }

            }
            else if (LSConnectState.GattConnected == state) {
                EVLog.log(TAG,"STATE: GattConnected");
                setTextResult("STATE: GattConnected");
            }
            else if (LSConnectState.ConnectSuccess == state) {
                EVLog.log(TAG,"STATE: Connect Device Success");
                setTextResult("STATE: Connect Device Success");
                setVisibleProgressBar(View.INVISIBLE);
            }
            else if (LSConnectState.ConnectFailure == state) {
                EVLog.log(TAG,"STATE: Connect Device Failure");
                setTextResult("STATE: Connect Device Failure");
                setVisibleProgressBar(View.INVISIBLE);
            }
            else if (LSConnectState.Disconnect == state) {
                EVLog.log(TAG, "STATE: Device Disconnect");
                setTextResult("STATE: Device Disconnect");
                setVisibleProgressBar(View.INVISIBLE);
            }
            else if (LSConnectState.RequestDisconnect == state) {
                // Solicitar una desconexión
                setTextResult("STATE: Request Disconnect");
                setVisibleProgressBar(View.INVISIBLE);
            }
            else {
                EVLog.log(TAG, "State Change Device: Unknown");
                setTextResult("STATE: State Change Device: Unknown");
                setVisibleProgressBar(View.INVISIBLE);
            }
        }

        // Devolución de los datos de medición de la pulsera de actividad o del reloj.
        @Override
        public void onActivityTrackerDataUpdate(String mac, int type, ATDeviceData data){
            super.onActivityTrackerDataUpdate(mac, type, data);

            if (data == null) { return; }

//            Log.e(TAG,"OnSyncingListener >> onActivityTrackerDataUpdate() data >> " + data.toString());

            // Success Notification
            if(data instanceof ATUploadDoneNotify){
                // EVENTO DE FINALIZACIÓN DE UNA CONSULTA
                Log.e(TAG,"onActivityTrackerDataUpdate() type >> " + data.toString());
                ATUploadDoneNotify complete = (ATUploadDoneNotify)data;
                ATDataQueryCmd typeData = complete.getDataType();   // Tipo de datos en la notificacion

                reloadTimerDownload();

                if (typeData.name().equals("All")) {
                    // Finalizada la descarga de datos
                    if (cTimerDownload != null) {
                        cTimerDownload.cancel();
                    }
                    setVisibleProgressBar(View.INVISIBLE);
                    setTextResult("Cleaning operation completed");
                }

                Log.e(TAG,"ATUploadDoneNotify() typeData >> " + typeData.name() + " - Success");
                setTextResult(typeData.name() + " - Success");
            }
            else if( data instanceof ATDeviceInfo) {
                deviceInfo = (ATDeviceInfo) data;
                setTextResult(deviceInfo.toString());
                Log.e(TAG,"OnSyncingListener >> onActivityTrackerDataUpdate() data ATDeviceInfo >> " + data.toString());

                // Pedimos la configuración actual de la pulsera
                ATConfigQuerySetting setting = new ATConfigQuerySetting(ATConfigQueryCmd.Settings);
//                ATConfigQuerySetting setting = new ATConfigQuerySetting(ATConfigQueryCmd.UserInfo);
                LSBluetoothManager.getInstance().queryDeviceData(deviceMac, setting, mSettingListener);

            }
            else if (data instanceof ATConfigItemData) {
                Log.e(TAG,"onActivityTrackerDataUpdate() data >> " + data.toString());
                ATConfigItemData obj = (ATConfigItemData) data;
                ATConfigItemSetting setting = (ATConfigItemSetting) obj.getSetting();

                Log.e(TAG,"ATConfigItemSetting: " + setting.toString());

                for (ATConfigItem item: setting.getItems()) {

                    if (item instanceof ATDisturbMode) {
                        ATDisturbMode noMolestar = (ATDisturbMode) item;
                        if (noMolestar.isStatus()) { setSelectionIdSpinner(cmbQuietMode,0); } // Habilitar
                        else { setSelectionIdSpinner(cmbQuietMode,1); } // Deshabilitar
                    }
                    else if (item instanceof ATBloodOxygenMonitor) {
                        ATBloodOxygenMonitor bloodOxygenMonitor = (ATBloodOxygenMonitor) item;
                        Log.e(TAG,"ATBloodOxygenMonitor: " + bloodOxygenMonitor.toString());
                        if (bloodOxygenMonitor.isEnable()) { setSelectionIdSpinner(cmbBloodOxygen,0); } // Habilitar
                        else { setSelectionIdSpinner(cmbBloodOxygen,1); } // Deshabilitar
                    }
                    else if (item instanceof ATHeartRateSwitch) {
                        ATHeartRateSwitch heartRateSwitch = (ATHeartRateSwitch) item;
                        // 0x00:Desactivar la detección de frecuencia cardíaca, 0x01:Activar la detección de frecuencia cardíaca, 0x02:Activar la detección inteligente de frecuencia cardíaca
                        if (heartRateSwitch.getState() > 0) { setSelectionIdSpinner(cmbHeartRate,0); } // Habilitar
                        else { setSelectionIdSpinner(cmbHeartRate,1); } // Deshabilitar
                    }
                    else if (item instanceof ATSedentaryItem) {
                        ATSedentaryItem sedentaryItem = (ATSedentaryItem) item;
                        if (sedentaryItem.isEnable()) { setSelectionIdSpinner(cmbInactividad,0); } // Habilitar
                        else { setSelectionIdSpinner(cmbInactividad,1); } // Deshabilitar
                    }

                }

                if (deviceInfo.isStateOfBond() == false) {
                    setVisibleView(btEmparejar,View.VISIBLE);
                }

//                ATDisturbMode


                setVisibleView(btClearData,View.VISIBLE);
                setVisibleView(btConfigurar,View.VISIBLE);
            }
            else {
                reloadTimerDownload();
                Log.e(TAG,"OnSyncingListener >> onActivityTrackerDataUpdate() data >> " + data.toString());
                setTextResult(data.toString());
            }

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

        // Devolución de datos de medición de peso
        @Override
        public void onScaleWeightDataUpdate(String broadcastId, LSScaleWeight weight) {
            Log.e(TAG,"OnSyncingListener >> onScaleWeightDataUpdate()");

        }

    };

    //endregion

    /**
     * Reinicia el Timer de descarga de datos
     */
    private void reloadTimerDownload(){
        if (cTimerDownload != null) {
            cTimerDownload.cancel();
            cTimerDownload.start();
        }
    }

    private void clearAllData() {

        setTextSatus("Clear data: All");
        clearTextResult();

        LSManagerStatus sdkStatus = LSBluetoothManager.getInstance().getManagerStatus();
        LSConnectState state = LSBluetoothManager.getInstance().checkConnectState(deviceMac);

        if(state == LSConnectState.ConnectSuccess) {

            if (sdkStatus == LSManagerStatus.Free) {
                Log.e(TAG,"Reconectando dispositivo");
                LSBluetoothManager.getInstance().stopSearch();
                LSBluetoothManager.getInstance().stopDeviceSync();

                clearTextResult();
                setTextResult("Reconectado con la pulsera. Una vez finalice vuelva a pulsar el botón.");
                conectar();
                return;
            }

            EVLog.log(TAG, "ATDataQueryCmd.All");

            setTextProgess("data cleaning");
            setVisibleProgressBar(View.VISIBLE);

            cTimerDownload.start();

            ATDataQuerySetting querySetting = new ATDataQuerySetting(ATDataQueryCmd.All);
            LSBluetoothManager.getInstance().queryDeviceData(deviceMac, querySetting, mSettingListener);
        }
        else{
            EVLog.log(TAG,"Device is not connected");
            Log.e(TAG,"Reconectando dispositivo");
            LSBluetoothManager.getInstance().stopSearch();
            LSBluetoothManager.getInstance().stopDeviceSync();

            clearTextResult();
            setTextResult("Reconectado con la pulsera. Una vez finalice vuelva a pulsar el botón.");
            conectar();
        }

    }

    //region :: Rellenado de view
    private void setEnableSpinners(boolean enable) {
        setEnableView(cmbLanguage,enable);
        setEnableView(cmbBrazo,enable);
        setEnableView(cmbQuietMode,enable);
        setEnableView(cmbHeartRate,enable);
        setEnableView(cmbBloodOxygen,enable);
        setEnableView(cmbInactividad,enable);
    }

    private void setRellenarLanguage(){
        ArrayList<String> typeList = new ArrayList<>();

        typeList.add("Spanish");
        typeList.add("English");
        typeList.add("French");

//        ArrayAdapter types = new ArrayAdapter(this,android.R.layout.simple_spinner_item, typeList);
        ArrayAdapter types = new ArrayAdapter(this,R.layout.custom_spinner_item, typeList);
        types.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        cmbLanguage.setAdapter(types);
    }

    private void setRellenarBrazo(){

        final String[] BRAZO = new String[]{
                "Izquierdo",
                "Derecho"
        };

        ArrayAdapter types = new ArrayAdapter(this,R.layout.custom_spinner_item, BRAZO);
        types.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        cmbBrazo.setAdapter(types);
    }

    private void setRellenarHabilitarDeshabilitar(Spinner spinner){

        final String[] ESTADO = new String[]{
                "Habilitar",
                "Deshabilitar"
        };

        ArrayAdapter types = new ArrayAdapter(this,R.layout.custom_spinner_item, ESTADO);
        types.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(types);
    }
    //endregion

}