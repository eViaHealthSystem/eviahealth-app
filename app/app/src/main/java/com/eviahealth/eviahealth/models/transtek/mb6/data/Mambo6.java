package com.eviahealth.eviahealth.models.transtek.mb6.data;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.eviahealth.eviahealth.models.transtek.mb6.models.setting.ENErrorCode;
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
import com.lifesense.plugin.ble.data.tracker.ATDeviceInfo;
import com.lifesense.plugin.ble.data.tracker.ATLoginInfo;
import com.lifesense.plugin.ble.data.tracker.ATPairConfirmInfo;
import com.lifesense.plugin.ble.data.tracker.ATPairConfirmState;
import com.lifesense.plugin.ble.data.tracker.ATPairResultsCode;
import com.lifesense.plugin.ble.data.tracker.ATUploadDoneNotify;
import com.lifesense.plugin.ble.data.tracker.setting.ATDataQueryCmd;

import java.util.ArrayList;

public class Mambo6 {

    final static String TAG = "MamboService";
    String deviceName = "IF1 05E1";
    int connecting;
    private String deviceMac;
    private LSDeviceInfo currentDevice = null;
    private ArrayList<LSDeviceInfo> listLSDevice = new ArrayList<>();

    private Context mcontext;
    // SINGLETON
    private static Mambo6 instance = null;
    public static Mambo6 getInstance(Context context) {
        if(instance == null) {
            instance = new Mambo6( context,"");
        }
        return instance;
    }

    // Constructor
    private Mambo6(Context context, String mac) {
        this.deviceMac = mac;
        this.mcontext = context;

        //init LSBluetoothManager Transtek
        LSBluetoothManager.getInstance().initManager(mcontext);
        //register bluetooth state change receiver
        LSBluetoothManager.getInstance().registerBluetoothReceiver(mcontext);
    }

    public void setDeviceMac(String mac) { deviceMac = mac; }

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
                    //textResult += lsDevice.getDeviceName() + " [" + lsDevice.getMacAddress() + "]\n";
                    Log.e(TAG, ">> " + lsDevice.toString());

//                    if (lsDevice.getMacAddress().equals(deviceMac)) {
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

            Log.e(TAG,"OnPairingListener >> onStateChanged()");

            if(status == ATPairResultsCode.PAIR_SUCCESSFULLY){
                // Enlazar correctamente, configurar el dispositivo para que se conecte directamente y omitir el primer ciclo de exploración.
                Log.e(TAG,"PAIR SUCCESSFULLY");
                LSScanIntervalConfig config = new LSScanIntervalConfig();
                config.setPairDevice(lsDevice);
                LSBluetoothManager.getInstance().setManagerConfig(config);
            }
            else Log.e(TAG,"Pair Failure 01");

            if(status == 0 && lsDevice!=null){
                //TODO Pair Success
                Log.e(TAG,"Pair Success");
            }
            else{
                //TODO Pair Failure
                Log.e(TAG,"Pair Failure: lsDevice: " + lsDevice.toString());
                if (lsDevice!=null) {
                    currentDevice = lsDevice;
                }
            }
        }

        // Durante el proceso de emparejamiento o vinculación, las instrucciones de funcionamiento o
        // notificaciones de información interactiva cargadas por el dispositivo
        @Override
        public void onMessageUpdate(String macAddress, LSDevicePairSetting msg) {
            Log.e(TAG,"OnPairingListener >> onMessageUpdate() msg: " +msg.toString());
            Log.e(TAG,"OnPairingListener >> onMessageUpdate() msg.getPairCmd(): " + msg.getPairCmd().toString());

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
//            setTextResult("onFailure() >> Error: " + err.getMsg());
        }

        // Establecer devolución de llamada de éxito
        @Override
        public void onSuccess(String mac) {
            Log.e(TAG,"OnSettingListener >> onSuccess()");
//            setTextResult("Setting Success");
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

//            setTextResult("...");

            if (LSConnectState.Connecting == state) {
                Log.e(TAG,"STATE: Connecting");
//                setTextBleStatus("STATE: Connecting");
                connecting += 1;
                if (connecting > 1) { stop(); }
            }
            else if (LSConnectState.GattConnected == state) {
                Log.e(TAG,"STATE: GattConnected");
//                setTextBleStatus("STATE: GattConnected");
            }
            else if (LSConnectState.ConnectSuccess == state) {
                Log.e(TAG,"STATE: Connect Device Success");
//                setTextBleStatus("STATE: Connect Device Success");
            }
            else if (LSConnectState.ConnectFailure == state) {
                Log.e(TAG,"STATE: Connect Device Failure");
//                setTextBleStatus("STATE: Connect Device Failure");
            }
            else if (LSConnectState.Disconnect == state) {
                Log.e(TAG, "STATE: Device Disconnect");
//                setTextBleStatus("STATE: Device Disconnect");
            }
            else if (LSConnectState.RequestDisconnect == state) {
                // Solicitar una desconexión
                Log.e(TAG, "STATE: Request Disconnect");
//                setTextBleStatus("STATE: Request Disconnect");
            }
            else {
                Log.e(TAG, "State Change Device: Unknown");
//                setTextBleStatus("STATE: State Change Device: Unknown");
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

                Log.e(TAG,"ATUploadDoneNotify() typeData >> " + typeData.name() + " - Success");
//                String result = txtResult.getText().toString() + "\n" + typeData.name() + " - Success";
//                setTextResult(result);
            }
            else if( data instanceof ATDeviceInfo) {
                ATDeviceInfo deviceInfo = (ATDeviceInfo) data;
//                setTextResult(deviceInfo.toString());
                Log.e(TAG,"OnSyncingListener >> onActivityTrackerDataUpdate() data ATDeviceInfo >> " + data.toString());
//                disconnect();
                stop();
            }
            else {
                Log.e(TAG,"OnSyncingListener >> onActivityTrackerDataUpdate() data >> " + data.toString());
//                String result = txtResult.getText().toString() + "\n" + data.toString() + "\n";
//                setTextResult(result);
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

    public void connect(String mac) {
        connecting = 0;

        LSManagerStatus status = LSBluetoothManager.getInstance().getManagerStatus();
        if(status == LSManagerStatus.Scanning){
            //stop
            LSBluetoothManager.getInstance().stopSearch();
        }
        else if(status == LSManagerStatus.Syncing){
            // Cuando hay que llamarlo repetidamente, hay que pararlo primero
            LSBluetoothManager.getInstance().stopDeviceSync();
        }

        if (currentDevice == null) {
            currentDevice = new LSDeviceInfo();
            currentDevice.setMacAddress(mac);
            currentDevice.setBroadcastID(mac.replace(":", ""));
            currentDevice.setDeviceType(LSDeviceType.ActivityTracker.getValue());
            currentDevice.setProtocolType(LSProtocolType.A5.toString());
            currentDevice.setDeviceName(deviceName);
        }

        //clear measure device list
        LSBluetoothManager.getInstance().setDevices(null);

        if ( LSBluetoothManager.getInstance().getManagerStatus() == LSManagerStatus.Syncing
                && LSBluetoothManager.getInstance().checkConnectState(currentDevice.getMacAddress()) == LSConnectState.ConnectSuccess) {
            LSBluetoothManager.getInstance().resetSyncingListener(mSyncingCallback);
            Log.e(TAG, "ERROR: El Dispositivo ya está conectado.");
            return;
        }

        // Obtener el estado de funcionamiento
        if (LSBluetoothManager.getInstance().getManagerStatus() == LSManagerStatus.Syncing) {
            // Estado de sincronización de los datos del dispositivo
            Log.e(TAG, "ERROR: El Dispositivo sincronizando datos");
            return;
        }

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

        Log.e(TAG, "Send connect devive");
    }

    public void disconnect() {
        connecting = 0;
        LSBluetoothManager.getInstance().stopSearch();
        LSBluetoothManager.getInstance().stopDeviceSync();
        LSBluetoothManager.getInstance().unregisterBluetoothReceiver();
    }

    public void stop() {
        connecting = 0;
        LSBluetoothManager.getInstance().stopSearch();
        LSBluetoothManager.getInstance().stopDeviceSync();
    }
}
