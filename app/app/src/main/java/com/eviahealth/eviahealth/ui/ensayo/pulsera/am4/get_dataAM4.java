package com.eviahealth.eviahealth.ui.ensayo.pulsera.am4;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.models.devices.Config;

import com.eviahealth.eviahealth.models.ihealth.am4.Datapulsera;
import com.eviahealth.eviahealth.devices.NombresDispositivo;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;
import com.ihealth.communication.control.Am4Control;
import com.ihealth.communication.control.AmProfile;
import com.ihealth.communication.manager.DiscoveryTypeEnum;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

public class get_dataAM4 extends BaseActivity implements View.OnClickListener {

    final String TAG = "AM4-DATA";
    final String TypeDevice = "AM4";
    private final static int BLUETOOTH_ENABLED = 1;

    private Am4Control am4control;

    int callbackId;
    public static BluetoothAdapter mBluetoothAdapter;
    public BluetoothDevice devicegatt;

    Thread h1;
    ProgressBar circulodescarga;
    TextView txtStatus;

    String DEVICE_MAC_ADDRESS = "";
    String macdevice;

    Boolean fgregister = false;
    Boolean fgdetectado = false;
    Boolean fgconectado = false;
    Boolean fgdisconnect = false;
    Boolean fgconnectgatt = false;
    Boolean _status_download = false;
    Boolean fgerrorBP = false;          // true >> detectado error del dispositivo

    int cont_scan = 0;
    int cont_conexion = 0;
    int cont_gatt = 0;
    int contTick = 30;
    Datapulsera datos;
    CountDownTimer cTimerConnectBLE = null;       // TIMEOUT >> Debe de haber conectado antes de que salte
    TextToSpeechHelper textToSpeech;
    Boolean viewdata = false;

    // ONCREATE() -----------------------------------------------------------------------
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulsera_getdata_am4);

        EVLog.log(TAG, " onCreate()");
        txtStatus = findViewById(R.id.txtStatus_pul);
        txtStatus.setTextSize(14);
        txtStatus.setVisibility(View.VISIBLE);

        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        checkBluetooth();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        fgconnectgatt = false;
        fgdetectado = false;
        fgconectado= false;
        fgdisconnect = false;
        fgregister = false;
        fgerrorBP = false;

        cont_scan = 0;
        cont_conexion = 0;
        cont_gatt = 0;
        viewdata = false;

        datos = Datapulsera.getInstance();
        datos.clear();
        //  PULSERA ENABLE
        circulodescarga = findViewById(R.id.circulodescarga);
        circulodescarga.setVisibility(View.INVISIBLE);

        DEVICE_MAC_ADDRESS = Config.getInstance().getIdentificador(NombresDispositivo.ACTIVIDAD);
        EVLog.log(TAG,"MODEL/MAC DEVICE: "+ DEVICE_MAC_ADDRESS);

        String[] dev = DEVICE_MAC_ADDRESS.split("-");
        macdevice = dev[1];                           // Para iHealth SIN ":"
        DEVICE_MAC_ADDRESS = util.MontarMAC(dev[1]); // PARA GATT CON ":"
        EVLog.log(TAG,"MAC DEVICE: "+ macdevice);

        int timeout = (int)(1000 * 60 * 1.5);
        cTimerConnectBLE = new CountDownTimer(timeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                contTick -=1;
                if (contTick <= 0) {
                    contTick = 30;
                    EVLog.log(TAG, "cTimerConnectBLE.onTick()");
                }
            }

            @Override
            public void onFinish() {
                stoptimer();
                EVLog.log(TAG, "SUCEDIDO TIMEOUT CONEXION BLE");
                datos.set_status_descarga(false);
                newensayo();
            }
        };
        cTimerConnectBLE.start();

        devicegatt = mBluetoothAdapter.getRemoteDevice(DEVICE_MAC_ADDRESS);
        connectGATT();
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.imageButtonAudio) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            String frase = "Acerque la pulsera de actividad y espere unos instantes a que se descarguen sus datos.";
            texto.add(frase);
            frase = "Esta operación puede durar un poco.";
            texto.add(frase);
            textToSpeech.speak(texto);
            //endregion
        }
    }

    private void stoptimer(){
        cTimerConnectBLE.cancel();
        cTimerConnectBLE = null;
        EVLog.log(TAG,"cTimerConnectBLE.stop()");
    }

    @SuppressLint("MissingPermission")
    private void checkBluetooth() {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            EVLog.log(TAG,"Device does not support Bluetooth");
        } else if (!bluetoothAdapter.isEnabled()) {
            EVLog.log(TAG,"Bluetooth is not enable >> enable");
            bluetoothAdapter.enable();
            SystemClock.sleep(2000);
//            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableIntent,BLUETOOTH_ENABLED);
        } else {
            EVLog.log(TAG,"Bluetooth is enable");
        }
    }

    public void registeriHealth(){

        if (fgregister == false) {
            EVLog.log(TAG, "registeriHealth()");
            callbackId = iHealthDevicesManager.getInstance().registerClientCallback(miHealthDevicesCallback);
//        iHealthDevicesManager.getInstance().addCallbackFilterForDeviceType(callbackId, iHealthDevicesManager.TYPE_AM4);
            iHealthDevicesManager.getInstance().addCallbackFilterForAddress(callbackId, macdevice);
            fgregister = true;
        }
    }

    public void unregisteriHealth(){
        if (fgregister)
        {
            EVLog.log(TAG, "unregisteriHealth()");
            fgregister = false;
            iHealthDevicesManager.getInstance().unRegisterClientCallback(callbackId);
            iHealthDevicesManager.getInstance().destroy();
        }
    }

    public void scan_iHealth(String type) {

        registeriHealth();

        cont_scan++;
        fgdisconnect = false;
        fgdetectado= false;

        EVLog.log(TAG, "iHealth scan_iHealth(" + type + ") Intentos: "+Integer.toString(cont_scan));

        runOnUiThread(new Runnable() {
            public void run() {
                txtStatus.setText("SCAN iHealth(" + cont_scan +")");
            }
        });

        if ("AM4".equals(type)) {
            // Comprobamos que no este descubriendo todabía
            if (iHealthDevicesManager.getInstance().isDiscovering()){
                EVLog.log(TAG, "iHealth isDiscovering() = true");
                iHealthDevicesManager.getInstance().stopDiscovery();
                SystemClock.sleep(1000);
            }
            SystemClock.sleep(1000);
            iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.AM4);
        }
    }

    public void connect_iHealth(String mac,String type) {

        registeriHealth();

        fgconectado = false;
        fgdisconnect = false;

        SystemClock.sleep(500);
        EVLog.log(TAG, "iHealth connect_iHealth(" + mac + ") Type: " + type);
        runOnUiThread(new Runnable() {
            public void run() {
                txtStatus.setText("CONNECT iHealth(" + macdevice +")");
            }
        });

        if ("AM4".equals(type)) {
            // Comprobamos que no este descubriendo todabía
            if (iHealthDevicesManager.getInstance().isDiscovering()){
                EVLog.log(TAG, "iHealth isDiscovering() = true");
                iHealthDevicesManager.getInstance().stopDiscovery();
                SystemClock.sleep(1000);
            }
            SystemClock.sleep(1000);
            iHealthDevicesManager.getInstance().connectDevice("", mac, type);
        }
    }

    public void getdatos() {
        EVLog.log(TAG, "getdatos()");

        _status_download = true;

        Runnable hilo1 = new Runnable() {
            public void run() {

                runOnUiThread(new Runnable() {
                    public void run() {
                        txtStatus.setText("DOWNLOAD iHealth()");
                    }
                });
                // Obtiene el estado del dispositivo y la información de la batería
                am4control.queryAMState();

                runOnUiThread(new Runnable() {
                    public void run() {
                        fgconectado = true;
                        circulodescarga.setVisibility(View.VISIBLE);
                    }
                });
            }
        };

        h1 = new Thread(hilo1);
        h1.start();
    }

    @SuppressLint("MissingPermission")
    public void connectGATT() {
        cont_gatt ++;

        unregisteriHealth();
        EVLog.log(TAG,"gatt >> connectGATT(" + cont_gatt +")");

        runOnUiThread(new Runnable() {
            public void run() {
                txtStatus.setText("Connect GATT(" + cont_gatt +")");
            }
        });

        devicegatt.connectGatt(this, false, gattCallback,TRANSPORT_LE);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    EVLog.log(TAG,"gattCallback >> STATE_CONNECTED");

                    fgconnectgatt = true;
                    gatt.discoverServices();
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    EVLog.log(TAG,"gattCallback >> STATE_DISCONNECTED >> entra (" + cont_gatt +")");
                    gatt.close();

                    if (status == 0) {
//                        connect_iHealth(macdecice,"AM4");
                        runOnUiThread(new Runnable() {
                            public void run() { circulodescarga.setVisibility(View.VISIBLE);
                            }
                        });
                        scan_iHealth(TypeDevice);
                    }
                    else {
                        fgconnectgatt = false;
                        if (cont_gatt <= 2) {
                            runOnUiThread(new Runnable() {
                                public void run() { circulodescarga.setVisibility(View.VISIBLE);
                                }
                            });
                            connectGATT();
                        }
                        else{
                            EVLog.log(TAG, "gattCallback >> STATE_DISCONNECTED >> NO SE HA DETECTADO DISPOSITIVO EN GATT");
                            datos.set_status_descarga(false);
                            datos.setERROR_ACT("{\"type\":\"AM4\",\"error\":800,\"description\":\"No detectado dispositivo.\"}");
                            newensayo();
                        }
                    }
                    break;

                case BluetoothProfile.STATE_CONNECTING:
                    EVLog.log(TAG,"gattCallback >> STATE_CONNECTING");
                    break;

                case BluetoothProfile.STATE_DISCONNECTING:
                    EVLog.log(TAG,"gattCallback >> STATE_DISCONNECTING");
                    break;

                default:
                    EVLog.log(TAG,"gattCallback >> STATE_OTHER Status: " + status);
                    break;
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            EVLog.log(TAG,"gattCallback >> onServicesDiscovered Status: " + status);

            for (BluetoothGattService gattService : gatt.getServices()) {
                Log.e(TAG, "\t"+gattService.getUuid().toString());
            }

            gatt.disconnect();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            EVLog.log(TAG,"gattCallback >> onCharacteristicRead Status: " + status);
        }
    };

    private iHealthDevicesCallback miHealthDevicesCallback = new iHealthDevicesCallback() {

        @Override
        public void onScanDevice(String mac, String deviceType, int rssi, Map manufactorData) {

            EVLog.log(TAG, "iHealth onScanDevice() >> ENCONTRADA PULSERA MAC[" + mac + "] Tipo[" + deviceType + "]");

            // macPulsera
            if (mac.equals(macdevice)) {
                EVLog.log(TAG, "iHealth onScanDevice() ENCOTRADA NUESTRA PULSERA MAC[" + mac + "]");

                fgdetectado = true;
                fgconectado = false;
                iHealthDevicesManager.getInstance().stopDiscovery();
            }
        }

        @Override
        public void onScanError(String reason, long latency) {
            super.onScanError(reason, latency);
            EVLog.log(TAG, "iHealth onScanError() " + reason);
        }

        @Override
        public void onUserStatus(String username, int userStatus) {
            super.onUserStatus(username, userStatus);
            EVLog.log(TAG, "iHealth onUserStatus()");
        }

        @Override
        public void onScanFinish() {
            super.onScanFinish();

            EVLog.log(TAG, "iHealth onScanFinish() >> entra");
            iHealthDevicesManager.getInstance().stopDiscovery();
            iHealthDevicesManager.getInstance().disconnectAllDevices(true);

            if (fgdetectado == false) {
                // MAC NO ESCANEADA
                if (cont_scan <= 3){
                    EVLog.log(TAG, "iHealth onScanFinish() >> NO ENCONTRADO REINTENTAMOS SCAN (" + Integer.toString(cont_scan)+")");
                    // reintentamos scanearlo
                    if (cont_scan < 3){
                        scan_iHealth(TypeDevice);
                    }
                    else {
                        EVLog.log(TAG, "iHealth onScanFinish() >> CONECTO A LA FUERZA.");
                        iHealthDevicesManager.getInstance().stopDiscovery();
                        cont_scan++;
                        connect_iHealth(macdevice,TypeDevice); // Pedimos conectar con la pulsera
                    }
                }
                else {
                    EVLog.log(TAG, "iHealth onScanFinish() >> NO ENCONTRADO FINALIZAMOS SCAN (" + Integer.toString(cont_scan)+")");
                    datos.set_status_descarga(false);
                    datos.setERROR_ACT("{\"type\":\"AM4\",\"error\":801,\"description\":\"No detectado dispositivo.\"}");
                    newensayo();
                }
            }
            else {
                // PULSERA ENCONTRADA
                EVLog.log(TAG, "iHealth onScanFinish() >> PULSERA ENCONTRADA");
                iHealthDevicesManager.getInstance().stopDiscovery();
                connect_iHealth(macdevice,TypeDevice); // Pedimos conectar con la pulsera
            }
        }

        @Override
        public void onDeviceConnectionStateChange(String mac, String deviceType, int status, int errorID) {
            super.onDeviceConnectionStateChange(mac, deviceType, status, errorID);

            if (status == iHealthDevicesManager.DEVICE_STATE_CONNECTED) {

                EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTED");

                fgconectado = true;
                try {
                    am4control = iHealthDevicesManager.getInstance().getAm4Control(macdevice);
                }catch (Exception e){
                    EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTED >> Exception getAm4Control()");
                }

                // Iniciamos el proceso de descaga de datos y ponemos la rueda en marcha
                getdatos();
            }

            if (status == iHealthDevicesManager.DEVICE_STATE_DISCONNECTED) {
                EVLog.log(TAG, "iHealth DEVICE_STATE_DISCONNECTED >> entra");

                if (fgdisconnect){
                    EVLog.log(TAG, "iHealth DEVICE_STATE_DISCONNECTED >> SOLICITADA DESCONEXION");
                    fgdisconnect = false;

                    if (fgconectado) {
                        EVLog.log(TAG, "iHealth DEVICE_STATE_DISCONNECTED >> SE A DESCONECTO LA PULSERA");
                        fgconectado = false;
//                        datos.set_status_descarga(true);

                        if (fgerrorBP == false)
                            datos.set_status_descarga(true);

                        runOnUiThread(new Runnable() {
                            public void run() {
                                circulodescarga.setVisibility(View.INVISIBLE);
                            }
                        });

                        newensayo();
                    }
                    else{
                        EVLog.log(TAG, "iHealth DEVICE_STATE_DISCONNECTED >> SE A DESCONECTO SIN ESTAR CONECTADO ANTES.");
                        datos.set_status_descarga(false);
                        datos.setERROR_ACT("{\"type\":\"AM4\",\"error\":802,\"description\":\"Dispositivo: se ha desconectado inesperadamente.\"}");
                        newensayo();
                    }

                }
                else
                {
                    EVLog.log(TAG, "iHealth DEVICE_STATE_DISCONNECTED >> DESCONEXION NO SOLICITADA");
                    datos.set_status_descarga(false);
                    fgconectado = false;
                    newensayo();
                }

            }

            if (status == iHealthDevicesManager.DEVICE_STATE_CONNECTIONFAIL) {
                EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTIONFAIL >> ERRORID: " + errorID);

                SystemClock.sleep(500);
                if (fgconectado){
                    EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTIONFAIL >> ESTABA CONECTADO PERO SALTA CONNECTIONFAIL");
                    datos.set_status_descarga(false);
                    newensayo();
                }
                else{
                    if (cont_conexion <= 3) {
                        EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTIONFAIL >> REINTENTAMOS CONECTAR (" + cont_conexion + ")");

                        cont_conexion ++;
                        connectGATT();
                    }
                    else {

                        EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTIONFAIL >> FIN REINTENTOS DE CONECTAR (" + cont_conexion + ")");
                        datos.set_status_descarga(false);
                        datos.setERROR_ACT("{\"type\":\"AM4\",\"error\":803,\"description\":\"Superado los reintentos de conexión.\"}");
                        newensayo();
                    }
                }
            }

            if (status == iHealthDevicesManager.DEVICE_STATE_RECONNECTING) {
                EVLog.log(TAG, "iHealth DEVICE_STATE_RECONNECTING");
            }
        }

        @Override
        public void onDeviceNotify(String mac, String deviceType, String action, String message) {

            switch (action) {

                case AmProfile.ACTION_QUERY_STATE_AM:
                    try {
                        EVLog.log(TAG, " onDeviceNotify >> ACTION_QUERY_STATE_AM BATERIA: " + message);
                        datos.set_bateria_json(message);

                        stoptimer(); // Paramos timer ya hemos conectado con el dispositivo.
                        EVLog.log(TAG, "STOP TIMER CONEXION BLE");

                        //Sincronizar fecha y hora del reloj
                        EVLog.log(TAG, " Solicitado >> am4control.syncRealTime()");
                        SystemClock.sleep(500);
                        am4control.syncRealTime();
                    } catch (Exception e) {
                        datos.set_bateria_json("{}");
                    }
                    break;

                case AmProfile.ACTION_SYNC_TIME_SUCCESS_AM:
                    EVLog.log(TAG, " onDeviceNotify >> ACTION_SYNC_TIME_SUCCESS_AM: " + message);

                    //Solicitar datos pulsera de la actividad en este momento (Actividad total)
                    EVLog.log(TAG, " Solicitado >> am4control.syncRealData()");
                    SystemClock.sleep(500);
                    am4control.syncRealData();
                    break;

                case AmProfile.ACTION_SYNC_REAL_DATA_AM:
                    try {
                        // datos de la actividad en el tiempo actual
                        EVLog.log(TAG, "ACTION_SYNC_REAL_DATA_AM >> DATOS ACTUALES: " + message);

                        datos.set_datos_actuales(message);

                        //Petición descarga de registros cada 5 minutos
                        EVLog.log(TAG, " Solicitado >> am4control.syncActivityData()");
                        SystemClock.sleep(500);
                        am4control.syncActivityData();

                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        EVLog.log(TAG, "ACTION_SYNC_REAL_DATA_AM >> EXCEPTION");
                        e.printStackTrace();

                        EVLog.log(TAG, " Solicitado >> am4control.disconnect()");

                        datos.set_datos_actuales("{}");

                        fgdisconnect = true;
                        SystemClock.sleep(500);
                        am4control.disconnect();
                    }
                    break;

                case AmProfile.ACTION_SYNC_ACTIVITY_DATA_AM:
                    try {
                        EVLog.log(TAG, "ACTION_SYNC_ACTIVITY_DATA_AM >> ACTIVIDAD REALIZADA: " + message);

                        datos.set_datos_actividad(message);

                        //Solicito datos sueño
                        EVLog.log(TAG, " Solicitado >> am4control.syncSleepData()");
                        SystemClock.sleep(500);
                        am4control.syncSleepData();

                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        EVLog.log(TAG, "ACTION_SYNC_ACTIVITY_DATA_AM >> EXCEPTION");
                        e.printStackTrace();

                        datos.set_datos_actividad("{}");

                        EVLog.log(TAG, " Solicitado >> am4control.disconnect()");

                        fgdisconnect = true;
                        SystemClock.sleep(500);
                        am4control.disconnect();
                    }
                    break;

                case AmProfile.ACTION_SYNC_SLEEP_DATA_AM:

                    try {

                        EVLog.log(TAG, "ACTION_SYNC_SLEEP_DATA_AM >> DATOS SUEÑO: " + message);

                        datos.set_datos_sleep(message);

                        EVLog.log(TAG, " Solicitado >> am4control.disconnect()");

                        _status_download = false;
                        fgdisconnect = true;
                        SystemClock.sleep(500);
                        am4control.disconnect();

//                        EVLog.log(TAG, " Solicitado >> am4control.syncStageReprotData()");
//                        SystemClock.sleep(500);
//                        am4control.syncStageReprotData();

                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        EVLog.log(TAG, "ACTION_SYNC_SLEEP_DATA_AM >> EXCEPTION");
                        e.printStackTrace();

                        EVLog.log(TAG, " Solicitado >> am4control.disconnect()");

                        datos.set_datos_sleep("{}");

                        fgdisconnect = true;
                        SystemClock.sleep(500);
                        am4control.disconnect();
                    }
                    break;

                case AmProfile.ACTION_COMMUNICATION_TIMEOUT:
                    EVLog.log(TAG, " onDeviceNotify >> ACTION_COMMUNICATION_TIMEOUT");
                    datos.set_timeout_act("{\"error\":[]}");

                    EVLog.log(TAG, " Solicitado >> am4control.disconnect()");
                    fgdisconnect = true;
                    SystemClock.sleep(500);
                    am4control.disconnect();
                    break;

                case AmProfile.ACTION_SYNC_STAGE_DATA_AM:
                    EVLog.log(TAG, " onDeviceNotify >> ACTION_SYNC_STAGE_DATA_AM");
                    datos.set_stagereport(message);

                    EVLog.log(TAG, " Solicitado >> am4control.disconnect()");

                    _status_download = false;
                    fgdisconnect = true;
                    SystemClock.sleep(500);
                    am4control.disconnect();
                    break;

                case AmProfile.ACTION_ERROR_AM:
                    EVLog.log(TAG,"onDeviceNotify >> ACTION_ERROR_AM: " + message);

                    datos.setERROR_ACT(message);

                    fgerrorBP = true;
                    datos.set_status_descarga(false);

                    EVLog.log(TAG, " Solicitado >> am4control.disconnect()");
                    _status_download = false;
                    fgdisconnect = true;
                    SystemClock.sleep(500);
                    am4control.disconnect();
                    break;

                default:
                    EVLog.log(TAG,"onDeviceNotify >> ACCION NO CONTEMPLADA: " + action + " MESSAGE: " + message);

                    fgerrorBP = true;
                    datos.set_status_descarga(false);
                    datos.setERROR_ACT("{\"type\":\"AM4\",\"error\":804,\"description\":\"ACCION NO CONTEMPLADA\"}");

                    EVLog.log(TAG, " Solicitado >> am4control.disconnect()");
                    _status_download = false;
                    fgdisconnect = true;
                    SystemClock.sleep(500);
                    am4control.disconnect();
                    break;

            }
        }

    };

    public void newensayo() {
        if (viewdata == false) {
            viewdata = true;
            if (cTimerConnectBLE != null) cTimerConnectBLE.cancel();
            unregisteriHealth();
            EVLog.log(TAG, " newensayo() >> view_dataAM4.class");

            int id_dispositivo = 3;
            Intent intent = new Intent(get_dataAM4.this, view_dataAM4.class);
            intent.putExtra("id_dispositivo", id_dispositivo);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (cTimerConnectBLE != null) {
            cTimerConnectBLE.cancel();
            cTimerConnectBLE = null;
        }

        if (am4control != null) am4control.destroy();

        unregisteriHealth();
        textToSpeech.shutdown();

        mBluetoothAdapter = null;
        devicegatt = null;
        iHealthDevicesManager.getInstance().destroy();
        EVLog.log(TAG,"onDestroy()");
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){
    }

    @Override
    protected void onStart() {
        super.onStart();
        EVLog.log(TAG,"onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        EVLog.log(TAG,"onResume()");
        // Comprobamos si la ficha que se inició el ensayo en la actual
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
    }

    @Override
    protected  void onPause() {
        super.onPause();
        EVLog.log(TAG,"onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        EVLog.log(TAG,"onStop()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        EVLog.log(TAG,"onRestart()");
    }
}



