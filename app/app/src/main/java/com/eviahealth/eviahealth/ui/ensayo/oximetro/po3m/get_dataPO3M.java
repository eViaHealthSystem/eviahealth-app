package com.eviahealth.eviahealth.ui.ensayo.oximetro.po3m;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.models.ihealth.po3m.Dataoximetro;
import com.eviahealth.eviahealth.devices.NombresDispositivo;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;
import com.ihealth.communication.control.Po3Control;
import com.ihealth.communication.control.PoProfile;
import com.ihealth.communication.manager.DiscoveryTypeEnum;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

public class get_dataPO3M extends BaseActivity implements View.OnClickListener {
    final String TAG = "OXI-DATA";
    final String TypeDevice = "PO3";
    private final static int BLUETOOTH_ENABLED = 1;

    private Po3Control mPo3Control;
    int callbackId;
    public static BluetoothAdapter mBluetoothAdapter;
    public BluetoothDevice devicegatt;

    Thread h1;
    ProgressBar circulodescarga;
    CountDownTimer cTimer = null;       // TIMEOUT >> Pulsar botón START
    CountDownTimer cTimer2 = null;      // TIMEOUT >> Quitarse Oxímtero del dedo.

    String DEVICE_MAC_ADDRESS = "";
    String macdecice;
    String message_acum;

    Boolean fgregister = false;         // true >> registrado callbackId iHealth
    Boolean fgdetectado = false;        // true >> escaneado dispositivo
    Boolean fgconectado = false;        // true >> dispositivo conectado
    Boolean fgdisconnect = false;       // true >> solicitada desconexión por nosostros
    Boolean fgconnectgatt = false;      // true >> conectado al dispositivo mediante gatt
    Boolean _status_download = false;   // true >> estamos en estado de descaga de datos
    Boolean fgerrorBP = false;          // true >> detectado error del dispositivo
    Boolean minimValue = false;          // true >> ha recibido 30 o más valores

    Boolean fgMidiendo = false;          // true >> cuando se realizan medidas salta ACTION_LIVEDA_PO
    Integer cont_medidas = 0;
    Integer cont_medidas_estables = 0;
    Boolean fgtimeout_dedo = false;     // true >> timeout cuando se esperaba que quitara el dedo

    Integer cont_scan = 0;
    Integer cont_conexion = 0;
    Integer cont_gatt = 0;

    Dataoximetro datos;
    TextView txtStatus;
    TextToSpeechHelper textToSpeech;
    Boolean audio_on = false;
    Boolean viewdata = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oximetro_getdata);

        EVLog.log(TAG, " onCreate()");

        circulodescarga = findViewById(R.id.cdescarga_oxi);
        circulodescarga.setVisibility(View.INVISIBLE);
        txtStatus = findViewById(R.id.txtStatus_oxi);
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
        fgMidiendo = false;
        fgerrorBP = false;

        cont_medidas = 0;
        cont_medidas_estables = 0;
        cont_scan = 0;
        cont_conexion = 0;
        cont_gatt = 0;
        viewdata = false;

        datos = Dataoximetro.getInstance();
        datos.clear();

        DEVICE_MAC_ADDRESS = Config.getInstance().getIdentificador(NombresDispositivo.OXIMETRO);
        if (DEVICE_MAC_ADDRESS.contains("PO3M")){
            DEVICE_MAC_ADDRESS = DEVICE_MAC_ADDRESS.replace("PO3M-", "");
        }

        macdecice = DEVICE_MAC_ADDRESS;
        DEVICE_MAC_ADDRESS = MontarMAC(macdecice);

        EVLog.log(TAG,"MAC DEVICE: "+ DEVICE_MAC_ADDRESS);

        registeriHealth();

        //TimeOut >> Pulsar START del oxímetro
        Integer timeout = 1000 * 90;  // 90 seg
        cTimer = new CountDownTimer(timeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                // Se ha producido un Timeout, no se ha detectado botón de start
                cTimer.cancel();
                cTimer = null;
                EVLog.log(TAG, "TIMEOUT >> Pulsar botón START");
                datos.set_status_descarga(false);
                datos.setERROR_PO("{\"type\":\"PO3\",\"error_num\":700,\"description\":\"TIMEOUT, no se ha detectado botón de start.\"}");
                newensayo();
            }
        };

        //TimeOut >> Pulsar START del oxímetro
        Integer timeout2 = 1000 * 60;  // 60 seg
        cTimer2 = new CountDownTimer(timeout2, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                // Se ha producido un Timeout, no se ha detectado botón de start
                cTimer2.cancel();
                cTimer2 = null;
                EVLog.log(TAG, "TIMEOUT >> Quitarse Oxímtero del dedo.");
                fgtimeout_dedo = true;
            }
        };

        devicegatt = mBluetoothAdapter.getRemoteDevice(DEVICE_MAC_ADDRESS);
        connectGATT();
    }


    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.imageButtonAudio) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            audio_on = true; // para que dicte cuando hay que retirar el dedo

            String paso = "Abra la abrazadera del pulsioxímetro y coloque su dedo medio, anular, o índice de la mano izquierda " +
                    "sobre la apertura de goma del pulsioxímetro, con las uñas hacia abajo. " +
                    "Como se muestra en la figura.";
            texto.add(paso);

            paso = "En el panel frontal, pulse el botón Start una vez para activar el pulsioxímetro..";
            texto.add(paso);

            paso = "Mantenga la mano inmóvil para la lectura.";
            texto.add(paso);

            textToSpeech.speak(texto);

            //endregion
        }
    }

    @SuppressLint("MissingPermission")
    private void checkBluetooth() {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            EVLog.log(TAG,"Device does not support Bluetooth");
        } else if (!bluetoothAdapter.isEnabled()) {
            EVLog.log(TAG,"Bluetooth is not enable >> enable");
            bluetoothAdapter.enable();
            SystemClock.sleep(500);
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
            iHealthDevicesManager.getInstance().addCallbackFilterForAddress(callbackId, macdecice);
            fgregister = true;
        }
    }

    public void unregisteriHealth(){
        if (fgregister) {
            EVLog.log(TAG, "unregisteriHealth()");
            fgregister = false;
            iHealthDevicesManager.getInstance().unRegisterClientCallback(callbackId);
//            iHealthDevicesManager.getInstance().destroy();
        }
    }

    public String MontarMAC(String mac){

        String newMAC;
        char[] chars = mac.toCharArray();

        newMAC = "" + chars[0] + chars[1] + ":" + chars[2] + chars[3] + ":" + chars[4] + chars[5] + ":" +
                chars[6] + chars[7] + ":" + chars[8] + chars[9] + ":" + chars[10] + chars[11];

        return newMAC;
    }

    public void scan_iHealth(String type) {

        registeriHealth();

        cont_scan++;
        fgdisconnect = false;
        fgdetectado= false;

        EVLog.log(TAG, "iHealth scan_iHealth(" + type + ") Intentos: " + Integer.toString(cont_scan));
        runOnUiThread(new Runnable() {
            public void run() {
                txtStatus.setText("SCAN iHealth(" + cont_scan +")");
            }
        });
        if ("PO3".equals(type)) {
            // Comprobamos que no este descubriendo todabía
            if (iHealthDevicesManager.getInstance().isDiscovering()){
                EVLog.log(TAG, "iHealth isDiscovering() = true");
                iHealthDevicesManager.getInstance().stopDiscovery();
                SystemClock.sleep(1000);
            }
            SystemClock.sleep(1000);
            iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.PO3);
        }
    }

    public void connect_iHealth(String mac,String type) {

        registeriHealth();

        fgconectado = false;
        fgdisconnect = false;

        EVLog.log(TAG, "iHealth connect_iHealth(" + mac + ") Type: " + type);
        runOnUiThread(new Runnable() {
            public void run() {
                txtStatus.setText("CONNECT iHealth(" + macdecice +")");
            }
        });
        if ("PO3".equals(type)) {
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

        _status_download = true; // estamos realizando medidas
        cTimer.start();

        Runnable hilo1 = new Runnable() {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        txtStatus.setText("DOWNLOAD iHealth()");
                    }
                });
                // Obtiene el estado del dispositivo y la información de la batería
                mPo3Control.getBattery();
                fgconectado = true;
            }
        };

        h1 = new Thread(hilo1);
        h1.start();
    }

    @Override
    protected void onDestroy() {

        if (mPo3Control != null) mPo3Control.destroy();

        if (cTimer != null) cTimer.cancel();
        if (cTimer2 != null) cTimer2.cancel();

        unregisteriHealth();
        mBluetoothAdapter = null;
        devicegatt = null;

        iHealthDevicesManager.getInstance().destroy();
        EVLog.log(TAG,"onDestroy()");
        super.onDestroy();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){
    }

    @SuppressLint("MissingPermission")
    public void connectGATT() {
        cont_gatt ++;

        unregisteriHealth();

        SystemClock.sleep(500);
        EVLog.log(TAG,"gatt connectGATT(" + cont_gatt +")");
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
//                    cont_gatt = 0;
                    gatt.discoverServices();
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    EVLog.log(TAG,"gattCallback >> STATE_DISCONNECTED >> entra (" + cont_gatt +")");
                    gatt.close();

                    if (status == 0) {
                        scan_iHealth(TypeDevice);
                    }
                    else {
                        fgconnectgatt = false;

                        if (cont_gatt == 2){
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    circulodescarga.getIndeterminateDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                                    circulodescarga.setVisibility(View.VISIBLE); }
                            });
                        }


                        if (cont_gatt <= 2) {
                            connectGATT();
                        }
                        else{
                            EVLog.log(TAG, "gattCallback >> STATE_DISCONNECTED >> NO SE HA DETECTADO DISPOSITIVO EN GATT");
                            datos.set_status_descarga(false);
                            datos.setERROR_PO("{\"type\":\"PO3\",\"error_num\":800,\"description\":\"No detectado dispositivo.\"}");
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

            EVLog.log(TAG, "iHealth onScanDevice() >> ENCONTRADO DISPOSITIVO MAC[" + mac + "] Tipo[" + deviceType + "]");

            // MAC EQUIPO
            if (mac.equals(macdecice)) {
                EVLog.log(TAG, "iHealth onScanDevice() ENCOTRADA NUESTRO EQUIPO MAC[" + mac + "]");

                fgdetectado = true;
                fgconectado = false;
                iHealthDevicesManager.getInstance().stopDiscovery();
            }
        }

        @Override
        public void onScanError(String reason, long latency) {
            super.onScanError(reason, latency);
            EVLog.log(TAG, " iHealth onScanError() " + reason);
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

                if (cont_scan == 1){
                    runOnUiThread(new Runnable() {
                        public void run() {
                            circulodescarga.getIndeterminateDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                            circulodescarga.setVisibility(View.VISIBLE); }
                    });
                }

                if (cont_scan <= 3) {
                    EVLog.log(TAG, "iHealth onScanFinish() >> NO ENCONTRADO REINTENTAMOS SCAN (" + Integer.toString(cont_scan) + ")");
                    if (cont_scan < 3) {
                        scan_iHealth(TypeDevice);
                    } else {
                        EVLog.log(TAG, "iHealth onScanFinish() >> CONECTO A LA FUERZA.");
                        iHealthDevicesManager.getInstance().stopDiscovery();
                        connect_iHealth(macdecice,TypeDevice); // Pedimos conectar con la pulsera
                    }
                }
                else {
                    EVLog.log(TAG, "iHealth onScanFinish() >> NO ENCONTRADO FINALIZAMOS SCAN (" + Integer.toString(cont_scan)+")");
                    datos.set_status_descarga(false);
                    datos.setERROR_PO("{\"type\":\"PO\",\"error_num\":801,\"description\":\"No detectado dispositivo.\"}");
                    newensayo();
                }
            }
            else {
                // DISPOSITIVO ENCONTRADA
                EVLog.log(TAG, "iHealth onScanFinish() >> DISPOSITIVO ENCONTRADA");
                iHealthDevicesManager.getInstance().stopDiscovery();
                connect_iHealth(macdecice,TypeDevice); // Pedimos conectar con la pulsera
            }
        }

        @Override
        public void onDeviceConnectionStateChange(String mac, String deviceType, int status, int errorID) {
            super.onDeviceConnectionStateChange(mac, deviceType, status, errorID);

            if (status == iHealthDevicesManager.DEVICE_STATE_CONNECTED) {

                EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTED >> entra");

                fgconectado = true;
                try {
                    mPo3Control = iHealthDevicesManager.getInstance().getPo3Control(macdecice);
                }catch (Exception e){
                    EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTED >> Exception getBp3lControl()");
                }

                // Iniciamos el proceso medición par obtención de datos y ponemos la rueda en marcha
                getdatos();
            }

            if (status == iHealthDevicesManager.DEVICE_STATE_DISCONNECTED) {
                EVLog.log(TAG, "iHealth DEVICE_STATE_DISCONNECTED >> entra");

                if (fgdisconnect){
                    EVLog.log(TAG, "iHealth DEVICE_STATE_DISCONNECTED >> SOLICITADA DESCONEXION");
                    fgdisconnect = false;

                    if (fgconectado) {
                        EVLog.log(TAG, "iHealth DEVICE_STATE_DISCONNECTED >> SE DESCONECTO EL DISPOSITIVO");
                        fgconectado = false;

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
                        EVLog.log(TAG, "iHealth DEVICE_STATE_DISCONNECTED >> SE DESCONECTO SIN ESTAR CONECTADO ANTES.");
                        datos.set_status_descarga(false);
                        datos.setERROR_PO("{\"type\":\"PO3\",\"error_num\":802,\"description\":\"Dispositivo: se ha desconectado inesperadamente.\"}");
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
                        datos.setERROR_PO("{\"type\":\"PO3\",\"error_num\":803,\"description\":\"Superado los reintentos de conexión.\"}");
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
                case PoProfile.ACTION_BATTERY_PO:

                    try {
                        EVLog.log(TAG,"onDeviceNotify >> ACTION_BATTERY_PO: " + message);
                        datos.set_bateria_json(message);

                        fgMidiendo = false;
                        cont_medidas = 0;

                        //Inicia medicion
                        EVLog.log(TAG, " Solicitado >> mPo3Control.startMeasure();()");
                        SystemClock.sleep(500);
                        mPo3Control.startMeasure();
                    } catch (Exception e) {
                        datos.set_bateria_json("{}");
                    }
                    break;

                case PoProfile.ACTION_OFFLINEDATA_PO:
                    EVLog.log(TAG,"onDeviceNotify >> ACTION_OFFLINEDATA_PO: " + message);

                    break;

                case PoProfile.ACTION_NO_OFFLINEDATA_PO:
                    EVLog.log(TAG,"onDeviceNotify >> ACTION_NO_OFFLINEDATA_PO: " + message);

                    break;

                case PoProfile.ACTION_READY_MEASURE:
//                    EVLog.log(TAG,"onDeviceNotify >> ACTION_READY_MEASURE: " + message);
                    // SE PRODUCE CUANDO EMPIEZA A MEDIR
                    break;

                case PoProfile.ACTION_LIVEDA_PO:
                    // Mostrar los datos de medición en tiempo real para el dispositivo Po.
//                    EVLog.log(TAG,"onDeviceNotify >> ACTION_LIVEDA_PO: " + message);
                    fgMidiendo = true; // midiendo datos
                    cont_medidas ++;

                    // Para mostrar que no retire el dedo y poner la rueda girando.
                    if (cont_medidas == 1){
                        cTimer.cancel(); // Paramos TIMEOUT de BOTON
                        findViewById(R.id.idlayout_oxi).setBackground(ContextCompat.getDrawable(get_dataPO3M.this, R.drawable.pulsi_descargando));
                        runOnUiThread(new Runnable() {
                            public void run() {
                                circulodescarga.getIndeterminateDrawable().setColorFilter(Color.argb(255,18,20,42), PorterDuff.Mode.SRC_IN);
                                circulodescarga.setVisibility(View.VISIBLE);
                            }
                        });

                        cont_medidas_estables = 0;

                        if (audio_on) {
                            textToSpeech.stop();
                            textToSpeech.speak("No retire el dedo hasta que lo indique la aplicación.");
                        }
                    }

                    if (cont_medidas > 10){
                        Integer bloodoxygen;
                        try {
                            JSONObject info = new JSONObject(message);
                            bloodoxygen = info.getInt(PoProfile.BLOOD_OXYGEN_PO);
                        }catch (Exception e){
                            EVLog.log(TAG,"EXCEPTION ACTION_LIVEDA_PO >> json(BLOOD_OXYGEN_PO)");
                            bloodoxygen = 70;
                        }

                        if (bloodoxygen > 70) cont_medidas_estables ++;
                        else cont_medidas_estables = 0;

                        if (cont_medidas_estables == 30){
                            // LA MEDIDA ES ESTABLE HAY QUE QUITAR EL DEDO
                            minimValue = true;
                            cont_medidas_estables = 0;
                            // Me seguro de guardarme un valor estable
                            message_acum = message;
                            EVLog.log(TAG,"onDeviceNotify >> ACTION_LIVEDA_PO >> MEDIDA ESTABLE");
                            findViewById(R.id.idlayout_oxi).setBackground(ContextCompat.getDrawable(get_dataPO3M.this, R.drawable.pulsi_quitar));
                            runOnUiThread(new Runnable() {
                                public void run() { circulodescarga.setVisibility(View.INVISIBLE);}
                            });
                            fgtimeout_dedo = false;
                            cTimer2.start();

                            if (audio_on) {
                                textToSpeech.stop();
                                textToSpeech.speak("Retire el dedo.");
                                audio_on = false;
                            }
                        }

                        if (fgtimeout_dedo){
                            // No a quitado el dado vamos a parar guardando la info actual
                            EVLog.log(TAG,"onDeviceNotify >> ACTION_LIVEDA_PO: " + message);
                            datos.set_messure(message);

                            _status_download = false;
                            fgdisconnect = true;
                            EVLog.log(TAG, " Solicitado >> mPo3Control.disconnect()");
                            SystemClock.sleep(500);
                            mPo3Control.disconnect();
                        }
                    }

                    break;

                case PoProfile.ACTION_RESULTDATA_PO:
                    // Indica los datos del resultado para el dispositivo Po.
                    if (minimValue) { // Sitengo un valor estable me lo guardo
                        EVLog.log(TAG,"onDeviceNotify >> ACTION_RESULTDATA_PO_ACUM: " + message_acum);
                        datos.set_messure(message_acum);
                        minimValue = false;
                    }else { // Si no tengo un valor estable me quedo con el último recibido
                        EVLog.log(TAG,"onDeviceNotify >> ACTION_RESULTDATA_PO: " + message);
                        datos.set_messure(message);
                    }
                    if (cTimer2 != null) cTimer2.cancel();

                    _status_download = false;
                    fgdisconnect = true;
                    EVLog.log(TAG, " Solicitado >> mPo3Control.disconnect()");
                    SystemClock.sleep(500);
                    mPo3Control.disconnect();
                    break;

                case PoProfile.ACTION_ERROR_PO:
                    EVLog.log(TAG,"onDeviceNotify >> ACTION_ERROR_PO: " + message);

                    datos.setERROR_PO(message);

                    fgerrorBP = true;
                    datos.set_status_descarga(false);

                    _status_download = false;
                    fgdisconnect = true;
                    EVLog.log(TAG, " Solicitado >> mPo3Control.disconnect()");
                    SystemClock.sleep(500);
                    mPo3Control.disconnect();
                    break;

                default:
                    EVLog.log(TAG,"onDeviceNotify >> ACCION NO CONTEMPLADA: " + action + " MESSAGE: " + message);

                    fgerrorBP = true;
                    datos.set_status_descarga(false);
                    datos.setERROR_PO("{\"type\":\"PO3\",\"error_num\":804,\"description\":\"ACCION NO CONTEMPLADA\"}");

                    _status_download = false;
                    fgdisconnect = true;
                    EVLog.log(TAG, " Solicitado >> mPo3Control.disconnect()");
                    SystemClock.sleep(500);
                    mPo3Control.disconnect();
                    break;
            }
        }

    };

    public void newensayo() {
        if (viewdata == false) {
            viewdata = true;
            unregisteriHealth();
            EVLog.log(TAG, " newensayo()");

            if (datos.get_status_descarga()) {
                Intent in = new Intent(get_dataPO3M.this, view_dataPO3M.class);
                startActivity(in);
            }
            else {
                Intent in = new Intent(get_dataPO3M.this, view_failPO3M.class);
                startActivity(in);
            }
            EVLog.log(TAG, " newensayo() >> finish()");
            finish();
        }
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