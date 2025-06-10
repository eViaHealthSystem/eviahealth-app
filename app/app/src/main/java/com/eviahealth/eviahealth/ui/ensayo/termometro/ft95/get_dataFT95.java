package com.eviahealth.eviahealth.ui.ensayo.termometro.ft95;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.bluetooth.models.Global;
import com.eviahealth.eviahealth.devices.BeurerReferences;
import com.eviahealth.eviahealth.bluetooth.beurer.ft95.FT95Status;
import com.eviahealth.eviahealth.bluetooth.beurer.ft95.ft95control;
import com.eviahealth.eviahealth.models.beurer.ft95.ft95Data;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.devices.NombresDispositivo;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.utils.log.EVLogConfig;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class get_dataFT95 extends BaseActivity implements View.OnClickListener {

    private String TAG = "GET_DATAFT95";
    ProgressBar circulodescarga;
    TextView txtStatus;
    CountDownTimer cTimer = null;           // Circulo progreso TimeOut >> Para mostrar el cirulo de pensar
    CountDownTimer cTimeOutConnect = null;  // 20 SEG - TIMEOUT PARA QUE LLEGUE EL EVENTO ACTION_BEURER_CONNECTED O ACTION_BEURER_DISCONNECTED
    CountDownTimer cTimerSearch = null;     // TIMEOUT >> Busqueda de dispositivo

    // Variables scanner dispositivos beurer -------
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private Boolean found = false;

    // Para detectar FT95 pulsa power mientras se escanean dispositivos
    String DEVICE_MAC_ADDRESS = "";
    private String mDeviceAddress = "FFFFFFFFFFFF";
    String mDeviceMac = "FF:FF:FF:FF:FF:FF";
    private ft95control mFT95control = null;
    ft95Data datos;
    FT95Status status = FT95Status.None;

    private int retries;
    private Integer records = null;
    TextToSpeechHelper textToSpeech;
    Boolean viewdata = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_data_ft95);

        EVLog.log(TAG, " onCreate()");

        txtStatus = findViewById(R.id.txtStatus_pul);
        txtStatus.setTextSize(14);
        setVisibleView(txtStatus,View.VISIBLE);

        circulodescarga = findViewById(R.id.circulodescarga);
        setVisibleView(circulodescarga,View.INVISIBLE);

        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        String DEVICE_MAC_ADDRESS = Config.getInstance().getIdentificador(NombresDispositivo.TERMOMETRO);
        EVLog.log(TAG,"MAC TERMOMETRO: "+ DEVICE_MAC_ADDRESS);
        mDeviceAddress = DEVICE_MAC_ADDRESS.replace("FT95-", "");
        mDeviceMac = util.MontarMAC(mDeviceAddress);
        Log.e(TAG,"mDeviceMac: " + mDeviceMac);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                return;
            }
        }

        datos = ft95Data.getInstance();
        datos.clear();
        retries = 0;
        viewdata = false;

        //region :: Circulo progreso TimeOut >> Para mostrar el cirulo de pensar
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

        //region :: 20 SEG TIMEOUT PARA QUE LLEGUE EL EVENTO ACTION_BEURER_CONNECTED O ACTION_BEURER_DISCONNECTED
        cTimeOutConnect = new CountDownTimer(1000 * 20, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                cTimeOutConnect.cancel();
                Log.e(TAG,"TIMEOUT CONNECTING *************************");
                EVLog.log(TAG,"TIMEOUT CONNECTING *************************");
                // LANZA BROADCAST DE DISCONECT
                broadcastUpdate(BeurerReferences.ACTION_BEURER_DISCONNECTED,"");
            }
        };
        //endregion

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        searchDevicesBle();

//        registerReceiver(mCallbackBeurer, makeUpdateIntentFilter());
//        setTextSatus("Conectando dispositivo.");
//        mFT95control.initialize(this, mDeviceMac);
//        cTimeOutConnect.start();
    }

    // TimeOut entre reintentos de conexiones
    private void stopTimeOutConnect() {
        if (cTimeOutConnect != null) {
            cTimeOutConnect.cancel();
            cTimeOutConnect = null;
        }
    }

    @SuppressLint("MissingPermission")
    private void searchDevicesBle() {

        Log.e(TAG,"searchDevicesBle()");
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
                EVLogConfig.log(TAG, "(1) TimeOut SCAN. Stop Discovery bluetooth LE");
                Log.e(TAG, "(1) TimeOut SCAN. Stop Discovery bluetooth LE");

                datos.setStatusDescarga(false);
                datos.setERROR("{\"type\":\"FT95\",\"error\":800,\"description\":\"TIMEOUT, NO DETECTADO DISPOSITIVO\"}");
                viewResult();
            }
        };
        cTimerSearch.start();

        setTextSatus("searching device");
        EVLogConfig.log(TAG,"(1) Start Scanner FT95") ;

        //region :: SEARCH BLE FT95
        List<ScanFilter> filters = new ArrayList<>();

        ScanFilter nfilter = new ScanFilter.Builder()
                .setDeviceName("FT95")
                .build();
        filters.add(nfilter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        bluetoothLeScanner.startScan(filters, settings, scanCallback);
        status = FT95Status.Search;
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
                EVLogConfig.log(TAG, "device: " + deviceName + ", address: " + deviceHardwareAddress);
                Log.e(TAG, " device: " + deviceName + ", address: " + deviceHardwareAddress);

                if(deviceHardwareAddress.equals(mDeviceAddress)) {

                    if (found == false) {
                        Log.e(TAG, "Detectado dispositivo");

                        found = true;
                        bluetoothLeScanner.stopScan(scanCallback);

                        cTimerSearch.cancel();
                        cTimerSearch = null;

                        // Conectamos con el dispositivo
                        Log.e(TAG, "DEVICE CONNECTING");
                        status = FT95Status.Connecting;

                        mFT95control = new ft95control();
                        registerReceiver(mCallbackBeurer, makeUpdateIntentFilter(), RECEIVER_EXPORTED);
                        setTextSatus("Connecting");
                        mFT95control.initialize(get_dataFT95.this, mDeviceMac);
                        cTimeOutConnect.start();
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
                    setTextSatus("Connected");
                    stopTimeOutConnect();
                    status = FT95Status.Connected;
                    break;

                case BeurerReferences.ACTION_BEURER_DISCONNECTED:
                    // region :: ACTION_BEURER_DISCONNECTED
                    retries += 1;
                    Log.e(TAG, "DISCONNECTED STATE: " + mFT95control.getIsconnect());

                    if (mFT95control.getIsconnect() == Global.STATE_CONNECTING) {
                        if (retries < 3) {
                            Log.e(TAG, "Reintento de conexion: " + retries);
                            cTimeOutConnect.cancel();
                            setTextSatus("CONNECT[" + retries + "]");
                            mFT95control.destroy();
                            SystemClock.sleep(1000);
                            status = FT95Status.Connecting;
                            mFT95control.initialize(get_dataFT95.this, mDeviceMac);
                            cTimeOutConnect.start();
                        } else {
                            status = FT95Status.Fail;
                            datos.setStatusDescarga(false);
                            datos.setERROR("{\"type\":\"FT95\",\"error\":801,\"description\":\"Fallo en la descarga.\"}");
                            viewResult();
                        }
                    }
                    else if (mFT95control.getIsconnect() == Global.STATE_CONNECTED) {
                        status = FT95Status.Data;
                        setTextSatus("Download");
                    } else {
                        // Esto solo ocurre si no está vinculado
                        if (status != FT95Status.Bond) {
                            datos.setStatusDescarga(false);
                            datos.setERROR("{\"type\":\"FT95\",\"error\":801,\"description\":\"Desconexión inesperada.\"}");
                            status = FT95Status.Disconnected;
                            viewResult();
                        }
                        else {
                            Log.e(TAG,"EMPAREJANDOSE");
                        }
                    }
                    //endregion
                    break;

                case BeurerReferences.ACTION_BEURER_FT95_DOWNLOAD_DATA:
                    setTextSatus("Download");
                    stopTimeOutConnect();
                    status = FT95Status.Data;
                    break;

                case BeurerReferences.ACTION_BEURER_FT95_DOWNLOAD_DATA_FINISHED:
                    Log.e(TAG,"ACTION_BEURER_FT95_DOWNLOAD_DATA_FINISHED Message:" + message);

                    datos.setResponse(message);
                    EVLog.log(TAG,message);

                    datos.setStatusDescarga(true);
                    viewResult();
                    break;

                case BeurerReferences.ACTION_BEURER_CMD_FAIL:
                    //region :: ACTION_BEURER_CMD_FAIL
                    Log.e(TAG,"Message: " + message);
                    function = util.getStringValueJSON(message,"function");
                    setTextSatus("Fail process: " + function);

                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":\"FT95\",\"error\":801,\"description\":\"Detectado un fallo.\"}");

                    disconnecting();
                    viewResult();
                    //endregion
                    break;

                case BeurerReferences.ACTION_BEURER_GET_OTHER:
                    //region :: ACTION_BEURER_GET_OTHER
                    Log.e(TAG,"GET_OTHER Message:" + message);
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":\"FT95\",\"error\":801,\"description\":\"Detectado un fallo.\"}");
                    disconnecting();
                    viewResult();
                    //endregion
                    break;

                case BeurerReferences.ACTION_BEURER_COMMUNICATION_TIMEOUT:
                    //region :: ACTION_BEURER_COMMUNICATION_TIMEOUT
                    Log.e(TAG,"ACTION_BEURER_COMMUNICATION_TIMEOUT Message:" + message);

                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":\"FT95\",\"error\":801,\"description\":\"Detectado un fallo. TimeOut\"}");
                    disconnecting();
                    viewResult();
                    //endregion
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
                        status = FT95Status.Bond;
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
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":\"FT95\",\"error\":801,\"description\":\"Accion de broadcast no implementada.\"}");
                    disconnecting();
                    viewResult();
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
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_FT95_DOWNLOAD_DATA);
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_FT95_DOWNLOAD_DATA_FINISHED);
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
            texto.add("Encienda el termómetro pulsando brevemente el botón de ENCENDIDO. Oirá dos pitidos cortos cuando el termómetro esté listo.");
            texto.add("Compruebe que en su termómetro se encuentre en medición corporal. Esto se reconoce si se muestra el símbolo en pantalla.");
            texto.add("Mantenga el termómetro entre 2 y 3 cm del centro de la frente. Pulse el botón ESCAN. El termómetro realizará dos pitidos cortos cuando finalice la medida.");
            textToSpeech.speak(texto);
            //endregion
        }
    }

    public void setTextSatus(String texto) {
        runOnUiThread(new Runnable() {
            public void run() {
                txtStatus.setText(texto);
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

    public void viewResult() {
        if (viewdata == false) {
            viewdata = true;
            stopTimeOutConnect();
            EVLog.log(TAG, " ViewResult()");

            if (datos.getStatusDescarga()) {
                Intent in = new Intent(this, view_dataFT95.class);
                startActivity(in);
            }
            else {
                Intent in = new Intent(this, view_failFT95.class);
                startActivity(in);
            }

            finish();
        }
    }

    private void disconnecting() {
        setTextSatus("DISCONNECT.");
        status = FT95Status.Disconnecting;
        mFT95control.disconnect();
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

        if (status == FT95Status.Bond) {
            Log.e(TAG,"BUSCANDO TERMOMETRO OTRA VEZ");
            searchDevicesBle();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause()");
//        if (mFT95control != null) disconnecting();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy()");

        if (cTimer != null) cTimer.cancel();
        if (cTimeOutConnect != null) {
            cTimeOutConnect.cancel();
            cTimeOutConnect = null;
        }

        textToSpeech.shutdown();
        if (mFT95control != null) {
            mFT95control.destroy();
            unregisterReceiver(mCallbackBeurer);
        }
        mFT95control = null;

        if (bluetoothAdapter.isDiscovering()) {
            Log.e(TAG, "cancelDiscovery()");
            bluetoothAdapter.cancelDiscovery();
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }
}