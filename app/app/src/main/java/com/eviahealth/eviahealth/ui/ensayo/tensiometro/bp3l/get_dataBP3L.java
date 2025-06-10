package com.eviahealth.eviahealth.ui.ensayo.tensiometro.bp3l;

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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.models.ihealth.bp3l.Datatensiometro;
import com.eviahealth.eviahealth.devices.NombresDispositivo;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;
import com.ihealth.communication.control.Bp3lControl;
import com.ihealth.communication.control.BpProfile;
import com.ihealth.communication.manager.DiscoveryTypeEnum;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

public class get_dataBP3L extends BaseActivity implements View.OnClickListener {

    final String TAG = "BP3L-DATA";
    final String FASE = "TENSIÓMETRO";
    final String TypeDevice = "BP3L";
    private final static int BLUETOOTH_ENABLED = 1;

    private Bp3lControl mBP3Lcontrol;
    int callbackId;
    public static BluetoothAdapter mBluetoothAdapter;
    public BluetoothDevice devicegatt;

    Thread h1;
    ProgressBar circulodescarga;
    Button btMedir;
    ConstraintLayout layout_principal;

    String DEVICE_MAC_ADDRESS = "";
    String macdecice;

    Boolean fgregister = false;         // true >> registrado callbackId iHealth
    Boolean fgdetectado = false;        // true >> escaneado dispositivo
    Boolean fgconectado = false;        // true >> dispositivo conectado
    Boolean fgdisconnect = false;       // true >> solicitada desconexión por nosostros
    Boolean fgconnectgatt = false;      // true >> conectado al dispositivo mediante gatt
    Boolean _status_download = false;   // true >> estamos en estado de descaga de datos
    Boolean fgerrorBP = false;          // true >> detectado error del dispositivo

    int cont_scan = 0;
    int cont_conexion = 0;
    int cont_gatt = 0;

    CountDownTimer cTimer = null;       // TIMEOUT >> Para visualizar el botón de medir
    CountDownTimer cTimer_InterruptMeasurer = null;   // TIMEOUT >> Para recivir el evento de que a parado de medir

    Datatensiometro datos;
    TextView txtStatus;
    TextToSpeechHelper textToSpeech;
    Boolean audio_on = false;
    Boolean viewdata = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_data_bp3l);

        EVLog.log(TAG, " onCreate()");

        circulodescarga = findViewById(R.id.cdescarga_ten);
        circulodescarga.setVisibility(View.INVISIBLE);
        btMedir= findViewById(R.id.btmedir_ten);
        btMedir.setVisibility(View.INVISIBLE);
        txtStatus = findViewById(R.id.txtStatus_ten);
        txtStatus.setTextSize(14);
        txtStatus.setVisibility(View.INVISIBLE);

        viewdata = false;
        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        checkBluetooth();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        fgconnectgatt = false;
        fgdetectado = false;
        fgconectado= false;
        fgdisconnect = false;
        fgregister = false;

        cont_scan = 0;
        cont_conexion = 0;
        cont_gatt = 0;

        datos = Datatensiometro.getInstance();
        datos.clear();

        DEVICE_MAC_ADDRESS = Config.getInstance().getIdentificador(NombresDispositivo.TENSIOMETRO);
        EVLog.log(TAG,"MODEL/MAC DEVICE: "+ DEVICE_MAC_ADDRESS);

        String[] dev = DEVICE_MAC_ADDRESS.split("-");
        macdecice = dev[1];
        DEVICE_MAC_ADDRESS = util.MontarMAC(dev[1]);
        EVLog.log(TAG,"MAC DEVICE: "+ macdecice);

        registeriHealth();

        //TimeOut >> Espera un poco antes de visualizar el botón de medir
        Integer timeout = 1000 * 2;
        cTimer = new CountDownTimer(timeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                cTimer.cancel();
                cTimer = null;
                btMedir.setVisibility(View.VISIBLE);
            }
        };
        cTimer.start();

        Integer timeout2 = 1000 * 3;
        cTimer_InterruptMeasurer = new CountDownTimer(timeout2, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                cTimer_InterruptMeasurer.cancel();
                cTimer_InterruptMeasurer = null;

                EVLog.log(TAG,"TIMEOUT onDeviceNotify >> ACTION_INTERRUPTED_BP");
                EnsayoLog.log(FASE,TAG,"TIMEOUT onDeviceNotify >> ACTION_INTERRUPTED_BP");

                _status_download = false;
                fgdisconnect = true;
                EVLog.log(TAG, " Solicitado >> mBP3Lcontrol.disconnect()");
                mBP3Lcontrol.disconnect();
            }
        };
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
        if (fgregister)
        {
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

        EVLog.log(TAG, "iHealth scan_iHealth(" + type + ") Intentos: "+Integer.toString(cont_scan));
        runOnUiThread(new Runnable() {
            public void run() {
                txtStatus.setText("SCAN iHealth(" + cont_scan +")");
            }
        });
        if ("BP3L".equals(type)) {
            // Comprobamos que no este descubriendo todabía
            if (iHealthDevicesManager.getInstance().isDiscovering()){
                EVLog.log(TAG, "iHealth isDiscovering() = true");
                iHealthDevicesManager.getInstance().stopDiscovery();
                SystemClock.sleep(1000);
            }
            SystemClock.sleep(1000);
            iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.BP3L);
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
        if ("BP3L".equals(type)) {
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

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.btmedir_ten) {
            //region :: Start Medición
            btMedir.setEnabled(false);
            btMedir.setVisibility(View.INVISIBLE);
            txtStatus.setVisibility(View.VISIBLE);

            EVLog.log(TAG, "onClick() >> INICIAR MEDIDA");

            findViewById(R.id.idlayout_ten).setBackground(ContextCompat.getDrawable(this, R.drawable.ten_descarga));

            runOnUiThread(new Runnable() {
                public void run() {
                    circulodescarga.setVisibility(View.VISIBLE);
                }
            });

            devicegatt = mBluetoothAdapter.getRemoteDevice(DEVICE_MAC_ADDRESS);
            connectGATT();
            //endregion
        }
        else if (viewId == R.id.imageButtonAudio) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            String frase = "Colóquese el brazalete en el brazo como se indica en la imagen.";
            texto.add(frase);
            frase = "Mantenga un espacio de 2 dedos entre su brazo y el brazalete.";
            texto.add(frase);
            frase = "Ponga su brazo sobre una superficie plana y a la altura del corazón.";
            texto.add(frase);
            frase = "Cuando esté listo, pulse el botón para iniciar la medición.";
            texto.add(frase);
            textToSpeech.speak(texto);
            audio_on = true;
            //endregion
        }
    }

    public void getdatos() {
        EVLog.log(TAG, "getdatos()");

        _status_download = true; // estamos realizando medidas

        Runnable hilo1 = new Runnable() {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        txtStatus.setText("DOWNLOAD iHealth()");
                    }
                });
                // Obtiene el estado del dispositivo y la información de la batería
                mBP3Lcontrol.getBattery();

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

    @Override
    protected void onDestroy() {

        if (mBP3Lcontrol != null) mBP3Lcontrol.destroy();
        if (cTimer != null) cTimer.cancel();
        if (cTimer_InterruptMeasurer != null) cTimer_InterruptMeasurer.cancel();

        textToSpeech.shutdown();

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
                        if (cont_gatt <= 2) {
                            connectGATT();
                        }
                        else{
                            EVLog.log(TAG, "gattCallback >> STATE_DISCONNECTED >> NO SE HA DETECTADO DISPOSITIVO EN GATT");
                            datos.set_status_descarga(false);
                            datos.setERROR_BP("{\"type\":\"BP3L\",\"error\":800,\"description\":\"No detectado Tensiómetro.\"}");
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
//            String _mac = macdecice.replace(":","");
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
                if (cont_scan <= 3) {
                    EVLog.log(TAG, "iHealth onScanFinish() >> NO ENCONTRADO REINTENTAMOS SCAN (" + Integer.toString(cont_scan) + ")");
                    if (cont_scan < 3) {
                        scan_iHealth(TypeDevice);
                    } else {
                        EVLog.log(TAG, "iHealth onScanFinish() >> CONECTO A LA FUERZA.");
                        iHealthDevicesManager.getInstance().stopDiscovery();
                        connect_iHealth(macdecice,TypeDevice); // Pedimos conectar con el dispositivo
                    }
                }
                else {
                    EVLog.log(TAG, "iHealth onScanFinish() >> NO ENCONTRADO FINALIZAMOS SCAN (" + Integer.toString(cont_scan)+")");
                    datos.set_status_descarga(false);
                    datos.setERROR_BP("{\"type\":\"BP3L\",\"error\":801,\"description\":\"No detectado Tensiómetro.\"}");
                    newensayo();
                }
            }
            else {
                // DISPOSITIVO ENCONTRADO
                EVLog.log(TAG, "iHealth onScanFinish() >> DISPOSITIVO ENCONTRADO");
                iHealthDevicesManager.getInstance().stopDiscovery();
                connect_iHealth(macdecice,TypeDevice); // Pedimos conectar con el dispositivo
            }
        }

        @Override
        public void onDeviceConnectionStateChange(String mac, String deviceType, int status, int errorID) {
            super.onDeviceConnectionStateChange(mac, deviceType, status, errorID);

            if (status == iHealthDevicesManager.DEVICE_STATE_CONNECTED) {

                EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTED");

                fgconectado = true;
                try {
                    mBP3Lcontrol = iHealthDevicesManager.getInstance().getBp3lControl(macdecice);
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

                    // Paramos el timeout de parar de medir
                    if (cTimer_InterruptMeasurer != null) {
                        cTimer_InterruptMeasurer.cancel();
                        cTimer_InterruptMeasurer = null;
                    }

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
                        datos.setERROR_BP("{\"type\":\"BP3L\",\"error\":802,\"description\":\"Tensiómetro: se ha desconectado inesperadamente.\"}");
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
//                            connectGATT();
                            SystemClock.sleep(500);
                            connect_iHealth(macdecice,TypeDevice);
                        }
                        else {

                            EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTIONFAIL >> FIN REINTENTOS DE CONECTAR (" + cont_conexion + ")");
                            datos.set_status_descarga(false);
                            datos.setERROR_BP("{\"type\":\"BP3L\",\"error\":803,\"description\":\"Superado los reintentos de conexión.\"}");
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

                case BpProfile.ACTION_BATTERY_BP:
                    try {
                        EVLog.log(TAG,"onDeviceNotify >> ACTION_BATTERY_BP: " + message);
                        datos.set_bateria_json(message);

                        //Sincronizar fecha y hora del reloj
                        EVLog.log(TAG, " Solicitado >> mBP3Lcontrol.startMeasure()");
                        SystemClock.sleep(500);
                        mBP3Lcontrol.startMeasure();
                    } catch (Exception e) {
                        datos.set_bateria_json("{}");
                    }
                break;

                case BpProfile.ACTION_ZOREING_BP:
//                    EVLog.log(TAG,"onDeviceNotify >> ACTION_ZOREING_BP: " + message);

                    break;

                case BpProfile.ACTION_ZOREOVER_BP:
//                    EVLog.log(TAG,"onDeviceNotify >> ACTION_ZOREOVER_BP: " + message);

                    break;

                case BpProfile.ACTION_ONLINE_PRESSURE_BP:
//                    EVLog.log(TAG,"onDeviceNotify >> ACTION_ONLINE_PRESSURE_BP: " + message);

                    break;

                case BpProfile.ACTION_ONLINE_PULSEWAVE_BP:
//                    EVLog.log(TAG,"onDeviceNotify >> ACTION_ONLINE_PULSEWAVE_BP: " + message);

                    break;

                case BpProfile.ACTION_ONLINE_RESULT_BP:
                    try {
                        EVLog.log(TAG,"onDeviceNotify >> ACTION_ONLINE_RESULT_BP: " + message);
                        datos.set_messure(message);

                        cTimer_InterruptMeasurer.start();

                        EVLog.log(TAG, " Solicitado >> mBP3Lcontrol.interruptMeasure()");
                        SystemClock.sleep(500);
                        mBP3Lcontrol.interruptMeasure();

                    } catch (Exception e) {
                        datos.set_messure("{}");
                    }
                break;

                case BpProfile.ACTION_INTERRUPTED_BP:
                    try {
                        EVLog.log(TAG,"onDeviceNotify >> ACTION_INTERRUPTED_BP: " + message);

                        _status_download = false;
                        fgdisconnect = true;
                        EVLog.log(TAG, " Solicitado >> mBP3Lcontrol.disconnect()");
                        SystemClock.sleep(500);
                        mBP3Lcontrol.disconnect();

                    } catch (Exception e) {
                    }
                    break;

                case BpProfile.ACTION_ERROR_BP:

                    EVLog.log(TAG,"onDeviceNotify >> ACTION_ERROR_BP ERROR: " + message);
                    datos.setERROR_BP(message);

                    cTimer_InterruptMeasurer.start();

                    fgerrorBP = true;
                    datos.set_status_descarga(false);
                    EVLog.log(TAG, " Solicitado >> mBP3Lcontrol.interruptMeasure()");
                    SystemClock.sleep(500);
                    mBP3Lcontrol.interruptMeasure();

                break;

                case BpProfile.ACTION_STOP_BP:
                    EVLog.log(TAG,"onDeviceNotify >> ACTION_STOP_BP");
                    fgerrorBP = true;
                    datos.setERROR_BP("{\"type\":\"BP3L\",\"error\":805,\"description\":\"Pulsado botón de STOP\"}");
                    datos.set_status_descarga(false);
                    EVLog.log(TAG, " Solicitado >> mBP3Lcontrol.interruptMeasure()");
                    SystemClock.sleep(500);
                    mBP3Lcontrol.interruptMeasure();

                break;

                default:
                    EVLog.log(TAG,"onDeviceNotify >> ACCION NO CONTEMPLADA: " + action + " MESSAGE: " + message);
                    fgerrorBP = true;
                    datos.set_status_descarga(false);
                    datos.setERROR_BP("{\"type\":\"BP3L\",\"error\":804,\"description\":\"ACCION NO CONTEMPLADA\"}");

                    EVLog.log(TAG, " Solicitado >> mBP3Lcontrol.interruptMeasure()");
                    SystemClock.sleep(500);
                    mBP3Lcontrol.interruptMeasure();
                break;

            }
        }

    };

    public void newensayo() {
        if (viewdata == false) {
            viewdata = true;
            unregisteriHealth();
            EVLog.log(TAG, " newensayo()");

            if (datos.get_status_descarga() == false) {
                Intent in = new Intent(get_dataBP3L.this, view_failBP3L.class);
                startActivity(in);
            }
            else {
                Intent in = new Intent(get_dataBP3L.this, view_dataBP3L.class);
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
        btMedir.setEnabled(true);

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