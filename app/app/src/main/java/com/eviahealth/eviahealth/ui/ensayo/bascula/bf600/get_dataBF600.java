package com.eviahealth.eviahealth.ui.ensayo.bascula.bf600;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
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

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.devices.BeurerReferences;
import com.eviahealth.eviahealth.bluetooth.beurer.bf600.bf600Funtion;
import com.eviahealth.eviahealth.bluetooth.beurer.bf600.bf600control;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.models.beurer.bf600.DataBF600;
import com.eviahealth.eviahealth.bluetooth.models.EStatusDevice;
import com.eviahealth.eviahealth.devices.NombresDispositivo;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.utils.PermissionUtils;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;
import com.eviahealth.eviahealth.models.vitalograph.Patient;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class get_dataBF600 extends BaseActivity implements View.OnClickListener {

    private String TAG = "GET_DATABF600";
    final String TypeDevice = "BF600";
    ProgressBar circulodescarga;
    Button btContinuar;
    TextView txtStatus;
    TextToSpeechHelper textToSpeech;
    private int numToSpeech = 1;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private Boolean found = false;
    EStatusDevice status = EStatusDevice.None;
    bf600Funtion manager =  bf600Funtion.None;
    bf600Funtion operacion =  bf600Funtion.None;
    Integer user_index = 1;
    // Para detectar PO60 pulsa power mientras se escanean dispositivos
    String DEVICE_MAC_ADDRESS = "";
    private String mDeviceAddress = ""; // device: BF600, address: 187A937A3C4B >> 18:7A:93:7A:3C:4B
    private bf600control mBF600control = null;
    private int retries;
    DataBF600 datos;
    Boolean viewdata = false;               // Para evitar que se lance dos veces la siguiente actividad
    CountDownTimer cTimer = null;           // TimeOut para que se realice la medición de peso
    CountDownTimer cTimerSearch = null;     // TIMEOUT >> Busqueda de dispositivo
    CountDownTimer cTimerProgress = null;  // Timer para mostrar el Circulo de Progreso inicial

    private Patient paciente = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_data_bf600);

        EVLog.log(TAG,"onCreate()");
        PermissionUtils.requestAll(this);

        //region :: views
        txtStatus = findViewById(R.id.txtStatus_bf600);
        txtStatus.setTextSize(14);
        setVisibleView(txtStatus,View.VISIBLE);

        circulodescarga = findViewById(R.id.circulodescarga);
        setVisibleView(circulodescarga,View.INVISIBLE);

        btContinuar = findViewById(R.id.btcontinuar_bf600);
        setVisibleView(btContinuar,View.INVISIBLE);
        //endregion

        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = DataBF600.getInstance();
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
        mDeviceAddress = util.MontarMAC(DEVICE_MAC_ADDRESS.replace("BF600-", ""));
        EVLog.log(TAG,"MAC BASCULA: "+ DEVICE_MAC_ADDRESS + ", MAC BF600: " + mDeviceAddress);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                return;
            }
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

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

//        searchDevicesBle();
        connectDevice();
    }

    @SuppressLint("MissingPermission")
    private void searchDevicesBle() {
        EVLog.log(TAG,"searchDevicesBle()");
        Integer timeout = 90; // 90 segundos
        cTimerSearch = new CountDownTimer(1000 * timeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                cTimerSearch.cancel();
                cTimerSearch = null;
                bluetoothLeScanner.stopScan(scanCallback);
                Log.e(TAG, "(1) TimeOut SCAN. Stop Discovery bluetooth LE");

                status = EStatusDevice.NotFound;
                setTextView(txtStatus,status.toString());

                EVLog.log(TAG, "SUPERADO EL TIEMPO MÁXIMO DE TIMEOUT DE SCANEADO");
                datos.setStatusDescarga(false);
                datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":800,\"description\":\".\"}");
                viewResult();
            }
        };
        cTimerSearch.start();

        status = EStatusDevice.Scanning;
        setTextView(txtStatus,status.toString());
        EVLog.log(TAG,"(1) Start Scanner DEVICE") ;

        retries = 0;
        //region :: SEARCH BLE
        List<ScanFilter> filters = new ArrayList<>();

//        ScanFilter nfilter = new ScanFilter.Builder()
//                .setDeviceName("PO60")
//                .build();
//        filters.add(nfilter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

//        bluetoothLeScanner.startScan(filters, settings, scanCallback);
        bluetoothLeScanner.startScan(null, settings, scanCallback);
        //endregion
    }

    @SuppressLint("MissingPermission")
    private void connectDevice() {
        Log.e(TAG, "Detectado dispositivo");

        found = true;
        bluetoothLeScanner.stopScan(scanCallback);

        if (cTimerSearch != null) {
            cTimerSearch.cancel();
            cTimerSearch = null;
        }

        // Conectamos con el dispositivo
        EVLog.log(TAG, "DEVICE CONNECTING");
        status = EStatusDevice.Connecting;
        setTextView(txtStatus,status.toString());

        mBF600control = new bf600control();
        registerReceiver(mCallbackBeurer, makeUpdateIntentFilter(), RECEIVER_EXPORTED);
        mBF600control.initialize(get_dataBF600.this, mDeviceAddress);
    }

    //region :: ScanCallback BLE
    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.e(TAG,"onScanResult()");

            BluetoothDevice device = result.getDevice();

            String deviceName = device.getName();
            String deviceMac = device.getAddress(); // MAC address
            String deviceHardwareAddress = deviceMac.replace(":","");

            if (deviceName != null) {
                Log.e(TAG, "device: " + deviceName + ", address: " + deviceHardwareAddress);
                if (deviceName.contains("BF600")) {
                    if (found == false) {
                        if (deviceMac.equals(mDeviceAddress)) {

                            EVLog.log(TAG, "Detectado dispositivo: " + deviceName + ", address: " + deviceHardwareAddress);

                            found = true;
                            bluetoothLeScanner.stopScan(scanCallback);

                            cTimerSearch.cancel();
                            cTimerSearch = null;

                            // Conectamos con el dispositivo
                            EVLog.log(TAG, "DEVICE CONNECTING");
                            status = EStatusDevice.Connecting;
                            setTextView(txtStatus,status.toString());

                            mBF600control = new bf600control();
                            registerReceiver(mCallbackBeurer, makeUpdateIntentFilter(), RECEIVER_EXPORTED);
                            mBF600control.initialize(get_dataBF600.this, deviceMac);
                        }
                    }
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.e(TAG,"onBatchScanResults()");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            EVLog.log(TAG, "onScanFailed: Error de escaneo: " + errorCode);
            datos.setStatusDescarga(false);
            datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":800,\"description\":\"No detectado dispositivo.\"}");
            viewResult();
        }
    };
    //endregion

    //region :: BROADCAST RECEIVER
    private final BroadcastReceiver mCallbackBeurer = new BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            String message = extras.getString(BeurerReferences.ACTION_EXTRA_MESSAGE,"");
            byte[] data;
            String function;

//            EVLog.log(TAG, "mCallbackBeurer(): " + action);
            switch(action) {
                case BeurerReferences.ACTION_BEURER_CONNECTED:
                    //region :: CONNECTED
                    EVLog.log(TAG,"ACTION_BEURER_CONNECTED Message:" + message);
                    status = EStatusDevice.Connected;
                    setTextView(txtStatus,status.toString());
                    Log.e(TAG," ");

                    mBF600control.setCurrentTime();
                    //endregion
                    break;

                case BeurerReferences.ACTION_BEURER_DISCONNECTED:
                    // region :: DISCONNECTED
                    EVLog.log(TAG,"ACTION_BEURER_DISCONNECTED");
                    retries += 1;

                    if (status == EStatusDevice.Disconnecting) {
                        viewResult();
                    }
                    else if (status == EStatusDevice.Connecting) {
                        if (retries < 3) {
                            EVLog.log(TAG, "Reintento de conexion: " + retries);
//                            cTimeOutConnect.cancel();
                            setTextView(txtStatus,status.toString() + " [" + retries + "]");
                            mBF600control.destroy();
                            SystemClock.sleep(1000);

                            status = EStatusDevice.Connecting;
                            mBF600control.initialize(get_dataBF600.this, mDeviceAddress);
//                            cTimeOutConnect.start();
                        } else {
                            status = EStatusDevice.Failed;
                            setTextView(txtStatus,status.toString());

                            datos.setStatusDescarga(false);
                            datos.setERROR("{\"type\":\"LUNG\",\"error\":801,\"description\":\"REINTENTOS DE CONEXIÓN SUPERADO.\"}");
                            viewResult();
                        }
                    }
                    else if (status == EStatusDevice.Connected) {
                        status = EStatusDevice.Failed;
                        setTextView(txtStatus,status.toString());

                        datos.setStatusDescarga(false);
                        datos.setERROR("{\"type\":\"LUNG\",\"error\":803,\"description\":\"Dispositivo: se ha desconectado inesperadamente.\"}");
                        viewResult();
                    }
                    else {
                        status = EStatusDevice.Disconnect;
                        setTextView(txtStatus, status.toString());
                    }

                    //endregion
                    break;

                case BeurerReferences.ACTION_BEURER_BF600_BATTERY_LEVEL:
                    EVLog.log(TAG,"***** BF600_BATTERY_LEVEL Message:" + message);
                    datos.setBattery(message);
                    cTimer.cancel();
                    cTimer = null;

                    // Operación finalizada
                    if (status != EStatusDevice.Disconnecting) {
                        status = EStatusDevice.Disconnecting;
                        mBF600control.disconnect();
                    }
                    break;

                case BeurerReferences.ACTION_BEURER_BF600_UPDATE_TIME:
                    EVLog.log(TAG,"ACTION_BEURER_BF600_UPDATE_TIME Message:" + message);
                    operacion = bf600Funtion.OperateUserActivate;
                    user_index = 1;
                    mBF600control.UserActive(user_index);
                    break;

                case BeurerReferences.ACTION_BEURER_BF600_UPDATE_USER:
                    EVLog.log(TAG,"ACTION_BEURER_BF600_UPDATE_USER Message:" + message);
//                    endScanViews();
                    break;

                case BeurerReferences.ACTION_BEURER_BF600_USER_CONTROL_POINT:
                    //region :: USER CONTROL POINT
                    EVLog.log(TAG,"BF600_USER_CONTROL_POINT Message:" + message);
                    EVLog.log(TAG,"Manage status: " + mBF600control.getFuntionManager().toString());

                    manager = mBF600control.getFuntionManager();

                    if (manager == bf600Funtion.CreateUser) {

                        user_index = null;
                        try {
                            JSONObject json = new JSONObject(message);
                            if (json.has("user_index")) {
                                user_index = json.getInt("user_index");
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            datos.setStatusDescarga(false);
                            datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                            viewResult();
                        }

                        if (user_index != null) {
                            EVLog.log(TAG,"user_index: " + user_index.toString());
                            operacion = bf600Funtion.OperateActivateCreate;
                            mBF600control.UserActive(user_index);
                            EVLog.log(TAG,"Creando usuario P-" + util.dosDigitos(user_index));
                        }
                        else {
                            EVLog.log(TAG, "Error al crear usuario");
                            datos.setStatusDescarga(false);
                            datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                            viewResult();
                        }
                    }
                    else if (manager == bf600Funtion.UserActive) {
                        Integer status = util.getIntValueJSON(message, "status");
                        EVLog.log(TAG, "operacion: " + operacion.toString());

                        if (operacion == bf600Funtion.OperateUserActivate) {
                            if (status != 1) {
                                EVLog.log(TAG,"Error al activar usuario P-" + util.dosDigitos(user_index));
                                operacion = bf600Funtion.OperateCreateUser;
                                mBF600control.CreateUser();
                            }
                            else {
                                EVLog.log(TAG, "Usuario P-" + util.dosDigitos(user_index) + " activado");

                                setVisibleView(circulodescarga,View.INVISIBLE);
                                // Cambio de pantalla
                                findViewById(R.id.idlayout_bf600).setBackground(ContextCompat.getDrawable(get_dataBF600.this, R.drawable.fondo_bf600_peso));
                                numToSpeech = 2;
                                cTimerProgress.start(); // Para mostrar el circulo de progreso

                                textToSpeech.stop();
                                cTimer.start();     // Activamos el tiempo para que se realice la medición.
                                mBF600control.TakeMeasurement();
                            }
                        }
                        else if (operacion == bf600Funtion.OperateActivateCreate) {
                            Log.e(TAG,"---");
                            if (status == 1) {
                                mBF600control.setCurrentTime();  // Actualizamos fecha y hora a la báscula
                            } else {
                                EVLog.log(TAG, "Error al activar usuario, P-" + util.dosDigitos(user_index));
                                datos.setStatusDescarga(false);
                                datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                                viewResult();
                            }
                        }
                        else if (operacion == bf600Funtion.OperateTest) {
                            if (status == 1) {
                                EVLog.log(TAG, "Activando test");
                                mBF600control.TakeMeasurement();  // inicia secuencia de medición
                            } else {
                                EVLog.log(TAG, "Error al iniciar medición, " + user_index);
                                datos.setStatusDescarga(false);
                                datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                                viewResult();
                            }
                        }
                    }
                    else {
                        Log.e(TAG, "Entrada en este proceso cuando no se esperaba");
                    }
                    //endregion
                    break;

                case BeurerReferences.ACTION_BEURER_BF600_WEIGHT_MEASUREMENT:
                    EVLog.log(TAG,"***** BF600_WEIGHT_MEASUREMENT Message:" + message);

                    if(mBF600control.getFuntionManager() == bf600Funtion.TakeMeasurement) {
                        // son datos obtenidos de una medición que nosotros hemos solicitado
                        datos.setWeightMeasurement(message);
                        datos.setStatusDescarga(true);
                    }

                    break;

                case BeurerReferences.ACTION_BEURER_BF600_BODY_COMPOSITION:
                    EVLog.log(TAG,"***** BF600_BODY_COMPOSITION Message:" + message);

                    if(mBF600control.getFuntionManager() == bf600Funtion.TakeMeasurement) {
                        // son datos obtenidos de una medición que nosotros hemos solicitado
                        datos.setBodyComposition(message);
                        mBF600control.getBatteryLevel();
                    }
                    break;

                case BeurerReferences.ACTION_BEURER_BF600_TAKE_MEASUREMENT:
                    EVLog.log(TAG,"***** BF600_TAKE_MEASUREMENT Message:" + message);
                    break;

                case BeurerReferences.ACTION_BEURER_BF600_USER_LIST:
//                    Log.e(TAG,"***** ACTION_BEURER_BF600_USER_LIST Message:" + message);
                    break;

                //region :: EMPAREJAMIENTO - VINCULAR DISPOSITIVO
                case BluetoothDevice.ACTION_PAIRING_REQUEST:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.e(TAG,"ACTION_PAIRING_REQUEST: " + device.getName() + " " + device.getAddress());

                    status = EStatusDevice.Pairing;
                    setTextView(txtStatus,status.toString());

                    Log.e(TAG,"manager: " + mBF600control.getFuntionManager());

                    break;

                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    //region :: EMPAREJAMIENTO EN CURSO
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    Log.e(TAG,"ACTION_BOND_STATE_CHANGED: " + bondState);
                    if (bondState == BluetoothDevice.BOND_BONDED) { // 12
                        Log.e(TAG, "BOND_BONDED: dispositivo remoto está enlazado (emparejado).");
                        status = EStatusDevice.BondBonded;
                    }
                    else if (bondState == BluetoothDevice.BOND_NONE) { // 10
                        Log.e(TAG, "BOND_NONE: dispositivo remoto NO está enlazado (emparejado).");
                        status = EStatusDevice.BondNone;
                    }
                    else if (bondState == BluetoothDevice.BOND_BONDING) { // 11

                        Log.e(TAG, "BOND_BONDING: la vinculación (emparejamiento) está en curso con el dispositivo remoto.");
                        setTextView(txtStatus,"Viculación en curso");
                        status = EStatusDevice.BondBonding;
                    }
                    //endregion
                    break;

                //endregion

                case BeurerReferences.ACTION_BEURER_COMMUNICATION_TIMEOUT:
                    //region :: TIMEOUT
                    EVLog.log(TAG,"ACTION_BEURER_COMMUNICATION_TIMEOUT Message:" + message);

                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");
                    mBF600control.disconnect();
                    //endregion
                    break;

                case BeurerReferences.ACTION_BEURER_WRITE_FAIL:
                    //region :: TIMEOUT
                    EVLog.log(TAG,"ACTION_BEURER_WRITE_FAIL Message:" + message);

                    Log.e(TAG,"status: " + status.toString());
                    Log.e(TAG,"manager: " + mBF600control.getFuntionManager());

                    if (status == EStatusDevice.Pairing || status == EStatusDevice.BondBonding || status == EStatusDevice.BondNone) {
                        datos.setStatusDescarga(false);
                        datos.setERROR(util.RequestError(TypeDevice,806,"Error en la vinculación del dispositivo."));
                    }
                    else if (status == EStatusDevice.BondBonded) {
                        datos.setStatusDescarga(false);
                        datos.setERROR(util.RequestError(TypeDevice,807,"Dispositivo vinculado."));
                    }
                    else {
                        datos.setStatusDescarga(false);
                        datos.setERROR(util.RequestError(TypeDevice,802,"Error inesperado."));
                    }

                    desconectar();
                    //endregion
                    break;


                default:
                    //region :: ACCION NO CONTEMPLADA
                    Log.e(TAG,"accion de broadcast no implementada");
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"Error inesperado.\"}");

                    mBF600control.disconnect();
                    status = EStatusDevice.Disconnecting;
                    setTextView(txtStatus, "" + status);
                    //endregion
                    break;

            };
        }
    };

    private static IntentFilter makeUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        // COMUNES
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_CONNECTED);
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_DISCONNECTED);
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_GET_OTHER);
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_CMD_FAIL);
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_COMMUNICATION_TIMEOUT);
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_WRITE_FAIL);

        // EMPAREJAMIENTO
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);

        // ESPECIFICOS DEL DISPOSITIVO
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_BF600_UPDATE_TIME);           // FECHA Y HORA ACTUALIZADA
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_BF600_USER_CONTROL_POINT);    //
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_BF600_UPDATE_USER);           // DATOS DE USUARIO Y CONFIGURACIÓN DE BÁSCULA REALIZADOS
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_BF600_WEIGHT_MEASUREMENT);    // MEDIA DE PESO OBTENIDA
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_BF600_BODY_COMPOSITION);      // MEDIDA DE LA COMPOSICIÓN CORPORAL OBTENIDA
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_BF600_TAKE_MEASUREMENT);      // MEDICIÓN FINALIZADA
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_BF600_USER_LIST);             // LISTA DE USUARIOS
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_BF600_BATTERY_LEVEL);         // NIVEL DE BATERIA

        return intentFilter;
    }

    //endregion

    private void desconectar() {
        Log.e(TAG, "Desconectando dispositivo");
        status = EStatusDevice.Disconnecting;
        setTextView(txtStatus,status.toString());
        if (mBF600control != null) mBF600control.disconnect();
    }

    @Override
    public void onClick(View view) {
        Log.e(TAG, "onClick()");

        int viewId = view.getId();
        if (viewId == R.id.imageButtonAudio) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            if (numToSpeech == 1) {
                texto.add("Coloque la báscula en el suelo, cerca de la Tablet.");
                texto.add("Espere unos segundos hasta que se establezca conexión con la báscula.");
            } else {
                texto.add("Súbase a la báscula y espere a que la medición finalice.");
                texto.add("Si sus pies están mojados o húmedos, séquelos bien antes de subir a la báscula.");
            }

            textToSpeech.speak(texto);
            //endregion
        }
    }

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

            if (status == EStatusDevice.Connected) {
                mBF600control.disconnect();
                status = EStatusDevice.Disconnecting;
                setTextView(txtStatus, "" + status);
            }
            else if (status == EStatusDevice.Scanning) {
                bluetoothLeScanner.stopScan(scanCallback);
                status = EStatusDevice.NotFound;
                datos.setStatusDescarga(false);
            }

            if (datos.getStatusDescarga()) {
                Intent intent = new Intent(this, view_dataBF600.class);
                intent.putExtra("typeScale", TypeDevice);
                startActivity(intent);
            }
            else {
                Intent intent = new Intent(this, view_failBF600.class);
                intent.putExtra("typeScale", TypeDevice);
                startActivity(intent);
            }

            Log.e(TAG,"datos.getStatusDescarga(): " + datos.getStatusDescarga());

            finish();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        super.onDestroy();
        EVLog.log(TAG, "onDestroy()");

        if (mBF600control != null) {
            mBF600control.destroy();
            unregisterReceiver(mCallbackBeurer);
        }
        mBF600control = null;
        textToSpeech.shutdown();

        if (bluetoothAdapter.isDiscovering()) {
            Log.e(TAG, "cancelDiscovery()");
            bluetoothAdapter.cancelDiscovery();
            bluetoothLeScanner.stopScan(scanCallback);
        }
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