package com.eviahealth.eviahealth.ui.config.bf600;

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
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.devices.BeurerReferences;
import com.eviahealth.eviahealth.bluetooth.beurer.bf600.bf600Funtion;
import com.eviahealth.eviahealth.bluetooth.beurer.bf600.bf600control;
import com.eviahealth.eviahealth.bluetooth.models.EStatusDevice;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.utils.log.EVLogConfig;
import com.eviahealth.eviahealth.utils.PermissionUtils;
import com.eviahealth.eviahealth.utils.util;
import com.eviahealth.eviahealth.models.vitalograph.Patient;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class config_BF600 extends BaseActivity implements View.OnClickListener {

    private String TAG = "CONFIG_BF600";
    final String TypeDevice = "BF600";
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private Boolean found = false;

    CountDownTimer cTimerSearch = null;     // TIMEOUT >> Busqueda de dispositivo
    private String mDeviceAddress = ""; // device: BF600, address: 187A937A3C4B, 18:7A:93:7A:3C:4B
    private bf600control mBF600control = null;
    TextView txtTitulo, txtStatus, txtResult, txtProgress;
    EditText editUser;
    Button btConfigurar, btRealizarPrueba, btDeleterUser;
    ProgressBar circulodescarga;
    String textResult = "";
    private int retries;
    EStatusDevice status = EStatusDevice.None;
    bf600Funtion manager =  bf600Funtion.None;
    bf600Funtion operacion =  bf600Funtion.None;
    Integer user_index = 1;
    private Patient paciente = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_bf600);

        Log.e("","onCreate()");
        PermissionUtils.requestAll(this);

        //region >> Views
        TextView txtInfoPaciente = findViewById(R.id.CHS2S_txtInfoPaciente);
        txtInfoPaciente.setVisibility(View.INVISIBLE);

        txtTitulo = findViewById(R.id.BF600_txtTitulo);
        txtTitulo.setVisibility(View.VISIBLE);

        txtStatus = findViewById(R.id.BF600_txtStatus);
        txtStatus.setVisibility(View.VISIBLE);

        txtResult = findViewById(R.id.BF600_txtResult);
        txtResult.setVisibility(View.VISIBLE);
        txtResult.setMovementMethod(new ScrollingMovementMethod());

        txtProgress = findViewById(R.id.BF600_txtProgress);
        txtProgress.setVisibility(View.INVISIBLE);

        circulodescarga = findViewById(R.id.BF600_ProgressBar);
        setVisibleView(circulodescarga,View.VISIBLE);

        // buttons
        btConfigurar = findViewById(R.id.BF600_btConfigurar);
        btConfigurar.setVisibility(View.INVISIBLE);

        btRealizarPrueba = findViewById(R.id.BF600_btPrueba);
        btRealizarPrueba.setVisibility(View.INVISIBLE);

        btDeleterUser = findViewById(R.id.BF600_btDeleteUser);
        btDeleterUser.setVisibility(View.INVISIBLE);

        editUser = findViewById(R.id.edit_user_index);
        editUser.setVisibility(View.INVISIBLE);

        //endregion

        //region :: INTENT
        Intent intent = getIntent();
        Bundle b = intent.getExtras();
        if (b != null) {
            mDeviceAddress = intent.getStringExtra("deviceMac");
            setTextStatus(txtTitulo,"Configuración dispositivo BF600 [" + mDeviceAddress + "]");

            //region :: Muestra caracteristicas paciente
            String idpaciente = intent.getStringExtra("idpaciente");
            paciente = ApiMethods.loadCharacteristics(idpaciente);;

            setTextStatus(findViewById(R.id.BF600_txtGenero),paciente.getGender());
            setTextStatus(findViewById(R.id.BF600_txtEdad),paciente.getAge().toString());
            setTextStatus(findViewById(R.id.BF600_txtAltura),paciente.getHeight().toString());
            setTextStatus(findViewById(R.id.BF600_txtPeso),paciente.getWeight().toString());

            if (paciente.getBydefault()) {
                EVLogConfig.log(TAG, "Datos de paciente cargados con valores por defecto");
                txtInfoPaciente.setVisibility(View.VISIBLE);
            }
            //endregion

        }
        //endregion

        clearTextResult();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                return;
            }
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        searchDevicesBle();
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
                Log.e(TAG, "(1) TimeOut SCAN. Stop Discovery bluetooth LE");

                status = EStatusDevice.NotFound;
                setTextStatus(txtStatus,status.toString());
            }
        };
        cTimerSearch.start();

        status = EStatusDevice.Scanning;
        setTextStatus(txtStatus,status.toString());
        Log.e(TAG,"(1) Start Scanner DEVICE") ;

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

    //region :: ScanCallback BLE
    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
//            Log.e(TAG,"onScanResult()");
            BluetoothDevice device = result.getDevice();

            String deviceName = device.getName();
            String deviceMac = device.getAddress(); // MAC address
            String deviceHardwareAddress = deviceMac.replace(":","");

            if (deviceName != null) {
                Log.e(TAG, "device: " + deviceName + ", address: " + deviceHardwareAddress);
                if (deviceName.contains("BF600")) {
                    if (found == false) {
                        if (deviceMac.equals(mDeviceAddress)) {

                            Log.e(TAG, "Detectado dispositivo");

                            found = true;
                            bluetoothLeScanner.stopScan(scanCallback);

                            cTimerSearch.cancel();
                            cTimerSearch = null;

//                            ScanRecord scanRecord = result.getScanRecord();
//                            if (scanRecord != null) {
//                                byte[] scanData = scanRecord.getBytes();
//                            Log.e(TAG, "dato: " + util.byteArrayInHexFormat(scanData));
//                            }

                            // Conectamos con el dispositivo
                            Log.e(TAG, "DEVICE CONNECTING");
                            status = EStatusDevice.Connecting;
                            setTextStatus(txtStatus,status.toString());

                            mBF600control = new bf600control();
                            registerReceiver(mCallbackBeurer, makeUpdateIntentFilter(), RECEIVER_EXPORTED);
                            mBF600control.initialize(config_BF600.this, deviceMac);
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
                    //region :: CONNECTED
                    Log.e(TAG,"ACTION_BEURER_CONNECTED Message:" + message);
                    status = EStatusDevice.Connected;
                    setTextStatus(txtStatus,status.toString());
                    Log.e(TAG," ");

                    operacion = bf600Funtion.OperateUserActivate;
                    user_index = 1;
                    mBF600control.UserActive(user_index);
                    //endregion
                    break;

                case BeurerReferences.ACTION_BEURER_DISCONNECTED:
                    // region :: DISCONNECTED
                    retries += 1;

                    if (status == EStatusDevice.Disconnecting) {
                        finish();
                    }
                    else if (status == EStatusDevice.Connecting) {
                        if (retries < 3) {
                            Log.e(TAG, "Reintento de conexion: " + retries);
//                            cTimeOutConnect.cancel();
                            setTextStatus(txtStatus,status.toString() + " [" + retries + "]");
                            mBF600control.destroy();
                            SystemClock.sleep(1000);

                            status = EStatusDevice.Connecting;
                            mBF600control.initialize(config_BF600.this, mDeviceAddress);
//                            cTimeOutConnect.start();
                        } else {
                            status = EStatusDevice.Failed;
                            setTextStatus(txtStatus,status.toString());
                        }
                    }
                    else {
                        status = EStatusDevice.Disconnect;
                        setTextStatus(txtStatus, status.toString());
                    }

                    //endregion
                    break;

                case BeurerReferences.ACTION_BEURER_BF600_UPDATE_TIME:
                    Log.e(TAG,"ACTION_BEURER_BF600_UPDATE_TIME Message:" + message);
                    operacion = bf600Funtion.OperateUserSetting;
                    mBF600control.UpdateUserData(paciente);
                    break;

                case BeurerReferences.ACTION_BEURER_BF600_UPDATE_USER:
                    Log.e(TAG,"ACTION_BEURER_BF600_UPDATE_USER Message:" + message);
                    setTextStatus(txtResult,message);
                    endScanViews();
                    break;

                case BeurerReferences.ACTION_BEURER_BF600_USER_CONTROL_POINT:
                    //region :: USER CONTROL POINT
                    Log.e(TAG,"ACTION_BEURER_BF600_USER_CONTROL_POINT Message:" + message);
                    Log.e(TAG,"Manage status: " + mBF600control.getFuntionManager().toString());

                    manager = mBF600control.getFuntionManager();
                    setTextView(txtResult, manager.toString() + ": " + message);

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
                        }

                        if (user_index != null) {
                            Log.e(TAG,"user_index: " + user_index.toString());
                            operacion = bf600Funtion.OperateActivateCreate;
                            mBF600control.UserActive(user_index);
                            setTextView(txtResult, "Creando usuario P-" + util.dosDigitos(user_index));
                        }
                        else {
                            setTextView(txtResult, "Error al crear usuario");
                        }
                    }
                    else if (manager == bf600Funtion.UserActive) {
                        Integer status = util.getIntValueJSON(message, "status");

                        Log.e(TAG, "operacion: " + operacion.toString());

                        if (operacion == bf600Funtion.OperateUserActivate) {
                            if (status != 1) {
                                setTextView(txtResult, "Error al activar usuario P-" + util.dosDigitos(user_index));
                                operacion = bf600Funtion.OperateCreateUser;
                                mBF600control.CreateUser();
                            }
                            else {
                                setTextView(txtResult, "Usuario P-" + util.dosDigitos(user_index) + " activado");
                                endScanViews();
                            }
                        }
                        else if (operacion == bf600Funtion.OperateActivateCreate) {
                            Log.e(TAG,"---");
                            if (status == 1) {
                                mBF600control.setCurrentTime();  // Actualizamos fecha y hora a la báscula
                            } else {
                                setTextView(txtResult, "Error al activar usuario, P-" + util.dosDigitos(user_index));
                            }
                        }
                        else if (operacion == bf600Funtion.OperateTest) {
                            if (status == 1) {
                                mBF600control.TakeMeasurement();  // inicia secuencia de medición
                            } else {
                                setTextView(txtResult, "Error al iniciar medición, " + user_index);
                            }
                        }
                    }
                    else if (manager == bf600Funtion.DeleteUser) {
                        Log.e(TAG,"Usuario activo eliminado");
                    }
                    else {
                        setTextView(txtResult, "ERROR");
                    }
                    //endregion
                    break;

                case BeurerReferences.ACTION_BEURER_BF600_WEIGHT_MEASUREMENT:
//                    Log.e(TAG,"***** ACTION_BEURER_BF600_WEIGHT_MEASUREMENT Message:" + message);
                    clearTextResult();
                    setTextStatus(txtResult,message);
                    break;

                case BeurerReferences.ACTION_BEURER_BF600_BODY_COMPOSITION:
//                    Log.e(TAG,"***** ACTION_BEURER_BF600_BODY_COMPOSITION Message:" + message);
                    setTextView(txtResult,message);
                    break;

                case BeurerReferences.ACTION_BEURER_BF600_TAKE_MEASUREMENT:
//                    Log.e(TAG,"***** ACTION_BEURER_BF600_TAKE_MEASUREMENT Message:" + message);
                    setTextView(txtResult,message);
                    break;

                case BeurerReferences.ACTION_BEURER_BF600_USER_LIST:
                    setTextView(txtResult,message);
                    endScanViews();

                    break;



                //region :: EMPAREJAMIENTO - VINCULAR DISPOSITIVO
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
                        setTextStatus(txtStatus,"Viculación en curso");
                    }
                    //endregion
                    break;

                case BluetoothDevice.ACTION_PAIRING_REQUEST:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.e(TAG,"ACTION_PAIRING_REQUEST: " + device.getName() + " " + device.getAddress());
                    break;

                //endregion

                case BeurerReferences.ACTION_BEURER_COMMUNICATION_TIMEOUT:
                    //region :: TIMEOUT
                    Log.e(TAG,"ACTION_BEURER_COMMUNICATION_TIMEOUT Message:" + message);

//                    datos.set_status_descarga(false);
//                    datos.setERROR_PO("{\"type\":\"PO60\",\"error\":802,\"description\":\"Ha fallado la descarga de datos con el dispositivo.\"}");
                    mBF600control.disconnect();
                    //endregion
                    break;

                default:
                    //region :: ACCION NO CONTEMPLADA
                    Log.e(TAG,"accion de broadcast no implementada");
//                    datos.set_status_descarga(false);
//                    datos.setERROR_PO("{\"type\":\"PO60\",\"error\":804,\"description\":\"ACCION NO CONTEMPLADA\"}");
                    mBF600control.disconnect();
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
        return intentFilter;
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

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy()");

        if (mBF600control != null) {
            mBF600control.destroy();
            unregisterReceiver(mCallbackBeurer);
        }
        mBF600control = null;

        if (bluetoothAdapter.isDiscovering()) {
            Log.e(TAG, "cancelDiscovery()");
            bluetoothAdapter.cancelDiscovery();
            bluetoothLeScanner.stopScan(scanCallback);
        }

        super.onDestroy();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}
    //endregion

    // region :: ONCLICK
    @SuppressLint("MissingPermission")
    @Override
    public void onClick(View view) {
        Log.e(TAG, " onClick()");

        int viewId = view.getId();
        if (viewId == R.id.BF600_btConfigurar) {
            //region :: CONFIGURACION BF600
            // inicia secuencia de configuración
            clearTextResult();

            // Inicia la secuencia de configuracion de la báscula
            mBF600control.setCurrentTime();
            //endregion
        }
        else if (viewId == R.id.BF600_btDeleteUser) {
            clearTextResult();

            String tp = ((EditText) findViewById(R.id.edit_user_index)).getText().toString();
            Integer user = Integer.parseInt(tp);

            operacion = bf600Funtion.OperateDeleteUser;
            mBF600control.UserActive(user);

//            mBF600control.getBatteryLevel();
        }
        else if (viewId == R.id.BF600_btPrueba) {
            clearTextResult();
            operacion = bf600Funtion.OperateTest;
            mBF600control.UserActive(user_index);
//            mBF600control.TakeMeasurement();  // inicia secuencia de medición
        }
        else if (viewId == R.id.BF600_btUserList) {
            clearTextResult();
            mBF600control.UserList();
        }
        else if (viewId == R.id.BF600_imfAtras) {
            //region :: STOP BF200
            clearTextResult();
            Log.e(TAG, "state: " + status.toString());
            if (status != EStatusDevice.Disconnecting) {
                status = EStatusDevice.Disconnecting;
                if (mBF600control != null) {
                    mBF600control.disconnect();
                }
                else { finish(); }
            }
            else if (status != EStatusDevice.Disconnect) {
                if (mBF600control != null) {
                    mBF600control = null;
                }
                finish();
            }
            else if (status == EStatusDevice.Scanning) {
                bluetoothLeScanner.stopScan(scanCallback);
                status = EStatusDevice.NotFound;
                finish();
            }
            //endregion
        }
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

    //endregion

    private void endScanViews() {
        txtProgress.setVisibility(View.INVISIBLE);
        setVisibleView(circulodescarga,View.INVISIBLE);
        setVisibleView(btConfigurar,View.VISIBLE);
        setVisibleView(btRealizarPrueba,View.VISIBLE);
//        setVisibleView(btDeleterUser,View.VISIBLE);
    }
}