package com.eviahealth.eviahealth.ui.ensayo.oximetro.po60;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.bluetooth.beurer.po60.PO60Status;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.bluetooth.models.Global;
import com.eviahealth.eviahealth.devices.BeurerReferences;
import com.eviahealth.eviahealth.bluetooth.beurer.po60.po60control;
import com.eviahealth.eviahealth.models.beurer.po60.po60Dataoximetro;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.devices.NombresDispositivo;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class get_dataPO60 extends BaseActivity implements View.OnClickListener {

    private String TAG = "GET_DATAPO60";

    ProgressBar circulodescarga;
    TextView txtStatus;
    CountDownTimer cTimer = null;           // Para mostrar el circulo de pensar
    CountDownTimer cTimerSearch = null;     // TIMEOUT >> Busqueda de dispositivo
    private static int TIMEOUT_SEARCH = 1000 * 90;  // 1 minuto, 30 segundos
    CountDownTimer cTimerTimeOut = null;    // TIMEOUT >> Tiempo máximo para realizarse la medida
    private static int TIMEOUT_MEASURE = 1000 * 60 * 3;  // 3 minutos


    // Variables scanner dispositivos beurer -------
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private Boolean found = false;
    PO60Status status = PO60Status.None;

    // Para detectar PO60 pulsa power mientras se escanean dispositivos
    String DEVICE_MAC_ADDRESS = "";
    private String mDeviceAddress = "FF:8D:F0:FC:3C:00"; // PO60

    private po60control mPO60control = null;
    po60Dataoximetro datos;
    private int retries;
    private Integer records = null;
    TextToSpeechHelper textToSpeech;
    Boolean viewdata = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_data_po60);

        txtStatus = findViewById(R.id.txtStatusPO60);
        txtStatus.setTextSize(14);
        txtStatus.setVisibility(View.VISIBLE);
        circulodescarga = findViewById(R.id.cdescarga_oxi);
        circulodescarga.setVisibility(View.INVISIBLE);

        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                return;
            }
        }

        datos = po60Dataoximetro.getInstance();
        datos.clear();
        retries = 0;
        viewdata = false;

        DEVICE_MAC_ADDRESS = Config.getInstance().getIdentificador(NombresDispositivo.OXIMETRO);
        EVLog.log(TAG,"MAC DISPOSITIVO: "+ DEVICE_MAC_ADDRESS);
        if (DEVICE_MAC_ADDRESS.contains("PO60")){
            DEVICE_MAC_ADDRESS = DEVICE_MAC_ADDRESS.replace("PO60-", "");
            mDeviceAddress = util.MontarMAC(DEVICE_MAC_ADDRESS);
            EVLog.log(TAG,"MAC mDeviceAddress: "+ mDeviceAddress);
        }

        //region :: Circulo progreso TimeOut >> Para mostrar el circulo de pensar
        Integer timeout = 1000 * 7;  // 7 seg
        cTimer = new CountDownTimer(timeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                cTimer.cancel();
                cTimer = null;
                runOnUiThread(new Runnable() {
                    public void run() {
                        circulodescarga.setVisibility(View.VISIBLE);
                    }
                });
            }
        };
        //endregion
        cTimer.start();

        //region :: TIMEOUT >> Tiempo máximo para realizarse la medición completa
        cTimerTimeOut = new CountDownTimer(TIMEOUT_MEASURE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                cTimerTimeOut.cancel();
                cTimerTimeOut = null;
                EVLog.log(TAG,"TIMEOUT >> Transcurrido tiempo máximo para realizarse la medición");
                datos.set_status_descarga(false);
                datos.setERROR_PO("{\"type\":\"PO60\",\"error\":805,\"description\":\"TIMEOUT, MAXIMO TIEMPO PARA MEDICION\"}");
                ViewResult();
            }
        };
        cTimerTimeOut.start();
        //endregion

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        searchDevicesBle();
    }

    @SuppressLint("MissingPermission")
    private void searchDevicesBle() {

        Log.e(TAG,"searchDevicesBle()");
        // 90 segundos para detectar dispositivo
        cTimerSearch = new CountDownTimer(TIMEOUT_SEARCH, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                cTimerSearch.cancel();
                cTimerSearch = null;
                bluetoothLeScanner.stopScan(scanCallback);
                EVLog.log(TAG, "(1) TimeOut SCAN. Stop Discovery bluetooth LE");

                datos.set_status_descarga(false);
                datos.setERROR_PO("{\"type\":\"PO60\",\"error\":800,\"description\":\"TIMEOUT, NO DETECTADO DISPOSITIVO\"}");
                ViewResult();
            }
        };
        cTimerSearch.start();

        setTextSatus("searching device");
        EVLog.log(TAG,"(1) Start Scanner PO60") ;

        //region :: SEARCH BLE FPO60
        List<ScanFilter> filters = new ArrayList<>();

        ScanFilter nfilter = new ScanFilter.Builder()
                .setDeviceName("PO60")
                .build();
        filters.add(nfilter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        bluetoothLeScanner.startScan(filters, settings, scanCallback);
        status = PO60Status.Search;
        //endregion
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
//                EVLog.log(TAG, "device: " + deviceName + ", address: " + deviceHardwareAddress);
                Log.e(TAG, "device: " + deviceName + ", address: " + deviceHardwareAddress);

                if(deviceHardwareAddress.equals(DEVICE_MAC_ADDRESS)) {

                    if (found == false) {
                        Log.e(TAG, "Detectado dispositivo");

                        found = true;
                        bluetoothLeScanner.stopScan(scanCallback);

                        cTimerSearch.cancel();
                        cTimerSearch = null;

                        // Conectamos con el dispositivo
                        Log.e(TAG, "DEVICE CONNECTING");
                        status = PO60Status.Connecting;

                        mPO60control = new po60control();
                        registerReceiver(mCallbackBeurer, makeUpdateIntentFilter(), RECEIVER_EXPORTED);
                        setTextSatus("Conectando dispositivo.");
                        mPO60control.initialize(get_dataPO60.this,mDeviceAddress);
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
            Log.e("ScanCallback", "Error de escaneo: " + errorCode);
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

            Log.e(TAG, "mCallbackBeurer(): " + action);
            switch(action) {
                case BeurerReferences.ACTION_BEURER_CONNECTED:
                    setTextSatus("Conectado.");

//                    mPO60control.getDeviceVersion();
                    mPO60control.setTime();
//                    mPO60control.getTime();
//                    mPO60control.setTypeStorageData();      // Obtiene el número de registros guardados
//                    mPO60control.getStartDownloadData();

                    break;
                case BeurerReferences.ACTION_BEURER_DISCONNECTED:
                    // region :: DISCONNECTED
                    retries += 1;
                    Log.e(TAG,"DISCONNECTED STATE: " + mPO60control.getIsconnect());
                    if (mPO60control.getIsconnect() == Global.STATE_CONNECTING) {
                        if (retries < 10) {
                            Log.e(TAG,"Reintento de conexion: " + retries);
                            setTextSatus("CONNECT[" + retries + "]");
                            mPO60control.destroy();
                            SystemClock.sleep(1000);
                            mPO60control.initialize(get_dataPO60.this,mDeviceAddress);
                        }
                        else {
                            datos.set_status_descarga(false);
                            datos.setERROR_PO("{\"type\":\"PO60\",\"error\":801,\"description\":\"No detectado dispositivo.\"}");
                            ViewResult();
                        }
                    } else { ViewResult(); }

                    //endregion
                    break;

                case BeurerReferences.ACTION_BEURER_PO60_DEVICE_VERSION:
                    Log.e(TAG,"ACTION_BEURER_PO60_DEVICE_VERSION Message:" + message);
                    mPO60control.disconnect();
                    break;

                case BeurerReferences.ACTION_BEURER_PO60_UPDATE_TIME:
                    Log.e(TAG,"ACTION_BEURER_PO60_UPDATE_TIME Message:" + message);
                    setTextSatus("DOWNLOAD DATA.");
                    mPO60control.setTypeStorageData();      // Obtiene el número de registros guardados
                    break;

                case BeurerReferences.ACTION_BEURER_PO60_RECORDS_STORAGE:
                    Log.e(TAG,"ACTION_BEURER_PO60_RECORDS_STORAGE Message:" + message);

                    // número de registros guardados
                    records = util.getIntValueJSON(message,"records");
                    if (records > 0)
                        mPO60control.getStartDownloadData(); // inicializa descarga de registros
                    else {
                        Log.e(TAG,"No encontrados registro del Oxipulsímetro");
                        datos.setMessure("{}");
                        mPO60control.disconnect();
                    }
                    break;

                case BeurerReferences.ACTION_BEURER_PO60_DOWNLOAD_DATA:
                    Log.e(TAG,"ACTION_BEURER_PO60_DOWNLOAD_DATA Message:" + message);
                    records = mPO60control.getNrecords();
                    if (records > 0)
                        mPO60control.getContinueDownloadData();
                    else
                        broadcastUpdate(BeurerReferences.ACTION_BEURER_PO60_DOWNLOAD_DATA_FINISHED,"{}");
                    break;

                case BeurerReferences.ACTION_BEURER_PO60_DOWNLOAD_DATA_FINISHED:
                    Log.e(TAG,"ACTION_BEURER_PO60_DOWNLOAD_DATA_FINISHED Message:" + message);
                    datos.setMessure(message);
                    EVLog.log(TAG,"bloodoxygen: " + datos.get_bloodoxygen() + ", heartrate: " + datos.get_heartrate());
                    setTextSatus("DELETE STORAGE.");
                    mPO60control.setDeleteDataStorage();
                    break;

                case BeurerReferences.ACTION_BEURER_PO60_DELETE_DATA_STORAGE:
                    Log.e(TAG,"ACTION_BEURER_PO60_DELETE_DATA_STORAGE Message:" + message);

                    String status = util.getStringValueJSON(message,"success");
                    if (status.equals("ok")) {
                        datos.set_status_descarga(true);
                    }
                    else {
                        datos.set_status_descarga(false);
                    }
                    setTextSatus("DISCONNECT.");
                    mPO60control.disconnect();
                    break;


                case BeurerReferences.ACTION_BEURER_CMD_FAIL:
                    Log.e(TAG,"Message: " + message);
                    function = util.getStringValueJSON(message,"function");
                    setTextSatus("Fail process: " + function);

                    datos.set_status_descarga(false);
                    datos.setERROR_PO("{\"type\":\"PO60\",\"error\":803,\"description\":\"Detectado un fallo de comunicación con el dispositivo..\"}");

                    mPO60control.disconnect();
                    break;

                case BeurerReferences.ACTION_BEURER_GET_OTHER:
                    Log.e(TAG,"GET_OTHER Message:" + message);
                    mPO60control.disconnect();
                    break;

                case BeurerReferences.ACTION_BEURER_COMMUNICATION_TIMEOUT:
                    Log.e(TAG,"ACTION_BEURER_COMMUNICATION_TIMEOUT Message:" + message);

                    datos.set_status_descarga(false);
                    datos.setERROR_PO("{\"type\":\"PO60\",\"error\":802,\"description\":\"Ha fallado la descarga de datos con el dispositivo.\"}");
                    mPO60control.disconnect();
                    break;

                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    //region :: EMPAREJAMIENTO EN CURSO
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    Log.e(TAG,"ACTION_BOND_STATE_CHANGED: " + bondState);
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        Log.e(TAG, "BOND_BONDED: dispositivo remoto está enlazado (emparejado).");

                    }
                    else if (bondState == BluetoothDevice.BOND_NONE) {
                        Log.e(TAG, "BOND_NONE: dispositivo remoto NO está enlazado (emparejado).");
                    }
                    else if (bondState == BluetoothDevice.BOND_BONDING) {
//                        status = PO60Status.Bond;
                        Log.e(TAG, "BOND_BONDING: la vinculación (emparejamiento) está en curso con el dispositivo remoto.");
                        setTextSatus("Viculación en curso");
                    }
                    //endregion
                    break;

                case BluetoothDevice.ACTION_PAIRING_REQUEST:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.e(TAG,"ACTION_PAIRING_REQUEST: " + device.getName() + " " + device.getAddress());
                    break;

                default:
                    Log.e(TAG,"accion de broadcast no implementada");
                    datos.set_status_descarga(false);
                    datos.setERROR_PO("{\"type\":\"PO60\",\"error\":804,\"description\":\"ACCION NO CONTEMPLADA\"}");
                    mPO60control.disconnect();
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

        // EMPAREJAMIENTO
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);

        // ESPECIFICOS DEL DISPOSITIVO
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_PO60_DEVICE_VERSION);
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_PO60_UPDATE_TIME);

        intentFilter.addAction(BeurerReferences.ACTION_BEURER_PO60_RECORDS_STORAGE);
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_PO60_DOWNLOAD_DATA);
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_PO60_DOWNLOAD_DATA_FINISHED);
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_PO60_DELETE_DATA_STORAGE);


        return intentFilter;
    }

    private void broadcastUpdate(final String action, String message) {
        final Intent intent = new Intent(action);
        intent.putExtra(BeurerReferences.ACTION_EXTRA_MESSAGE, message);
        sendBroadcast(intent);
    }
    //endregion

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.imageButtonAudio) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            String frase = "Introduzca el dedo en la abertura del pulsioxímetro, como se muestra en la imagen, y no lo mueva.";
            texto.add(frase);
            frase = "Pulse la tecla de función. El pulsioxímetro comenzará la medición. No se mueva durante el proceso de medición.";
            texto.add(frase);
            frase = "Transcurridos unos segundos, aparecerán en la pantalla de su pulsioxímetro los valores medidos.";
            texto.add(frase);
            frase = "Una vez visualice una medida estable, retire el dedo.";
            texto.add(frase);
            textToSpeech.speak(texto);
            //endregion
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy()");
        if (cTimer != null) cTimer.cancel();
        if (cTimerSearch != null) {
            cTimerSearch.cancel();
            cTimerSearch = null;
        }
        if (cTimerTimeOut != null) {
            cTimerTimeOut.cancel();
            cTimerTimeOut = null;
        }

        textToSpeech.shutdown();
        if (mPO60control != null) {
            mPO60control.destroy();
            unregisterReceiver(mCallbackBeurer);
        }
        mPO60control = null;

        if (bluetoothAdapter.isDiscovering()) {
            Log.e(TAG, "cancelDiscovery()");
            bluetoothAdapter.cancelDiscovery();
            bluetoothLeScanner.stopScan(scanCallback);
        }
        super.onDestroy();
    }

    public void setTextSatus(String texto) {
        runOnUiThread(new Runnable() {
            public void run() {
                txtStatus.setText(texto);
            }
        });
    }

    public void ViewResult() {
        if (viewdata == false) {
            viewdata = true;
            EVLog.log(TAG, " ViewResult()");

            if (datos.get_status_descarga() == false) {
                Intent intent = new Intent(this, view_failPO60.class);
                startActivity(intent);
            }
            else {
                Intent intent = new Intent(this, view_dataPO60.class);
                startActivity(intent);
            }
//            EVLog.log(TAG, " ViewResult() >> finish()");
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume()");

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
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause()");
//        mPO60control.disconnect();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}

}