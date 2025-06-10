package com.eviahealth.eviahealth.ui.config.lung;

import static com.eviahealth.eviahealth.models.vitalograph.PersonalBest.calculateFEV1PersonalBest;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.devices.BeurerReferences;
import com.eviahealth.eviahealth.bluetooth.models.BleReferences;
import com.eviahealth.eviahealth.bluetooth.models.EStatusDevice;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.utils.log.EVLogConfig;
import com.eviahealth.eviahealth.utils.PermissionUtils;
import com.eviahealth.eviahealth.utils.util;
import com.eviahealth.eviahealth.bluetooth.vitalograph.LUNGcontrol;
import com.eviahealth.eviahealth.models.vitalograph.Patient;
import com.eviahealth.eviahealth.models.vitalograph.SigleTestData;
import com.telit.terminalio.TIOManager;
import com.telit.terminalio.TIOManagerCallback;
import com.telit.terminalio.TIOPeripheral;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class config_LUNG extends BaseActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener, TIOManagerCallback {
    final static String TAG = "config_LUNG";
    private LUNGcontrol mLungControl = null;
    CountDownTimer cTimerSearch = null;       // TIMEOUT >> Busqueda de dispositivo
    private String mDeviceAddress = ""; // "00:80:25:D8:5A:5B";
    private int retries;
    EStatusDevice status = EStatusDevice.None;
    private EStatusDevice myfunction = EStatusDevice.None;
    TextView txtTitulo, txtStatus, txtResult, txtProgress;
    NumberPicker pckZonaVerde, pckZonaAmarrilla, pckZonaNaranja;
    NumberPicker pckOPBUnidad, pckOPBDecimalUno, pckOPBDecimalDos;
    ProgressBar circulodescarga;
    String textResult = "";

    //region :: TIO
    private static final int 		ENABLE_BT_REQUEST_ID = 1;
    private static final int		SCAN_INTERVAL 		 = 15000;
    private static final int		MEASURE_INTERVAL     = 60000;
    private TIOManager mTio;
    private TIOPeripheral mPeripheral;
    //endregion
    private Patient paciente = null;
    private Integer ZonaVerde = 50; // 80
    private Integer ZonaAmarrilla = 35; // 50
    private Integer ZonaNaranja = 20; // 30
    private Integer FEV1PB = 300;

    String idpaciente = null;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_lung);
        Log.e(TAG,"onCreate()");
        EVLogConfig.log(TAG, " onCreate()");

        PermissionUtils.requestAll(this);

        //region >> Views
        TextView txtInfoPaciente = findViewById(R.id.txtInfoPaciente);
        txtInfoPaciente.setVisibility(View.INVISIBLE);

        txtTitulo = findViewById(R.id.LUNG_txtTitulo);
        txtTitulo.setVisibility(View.VISIBLE);

        txtStatus = findViewById(R.id.LUNG_txtStatus);
        txtStatus.setVisibility(View.VISIBLE);

        txtResult = findViewById(R.id.LUNG_txtResult);
        txtResult.setVisibility(View.VISIBLE);
        txtResult.setMovementMethod(new ScrollingMovementMethod());

        txtProgress = findViewById(R.id.LUNG_txtProgress);
        txtProgress.setVisibility(View.INVISIBLE);

        circulodescarga = findViewById(R.id.LUNG_ProgressBar);
        setVisibleView(circulodescarga,View.INVISIBLE);
        //endregion

        //region :: View NumberPicker
        int size = 40;

        pckZonaVerde = findViewById(R.id.LUNG_ZG);
        pckZonaVerde.setMaxValue(99);
        pckZonaVerde.setMinValue(1);
        pckZonaVerde.setValue(50);
        pckZonaVerde.setTextSize(size);

        NumberPicker pckPOR1 = findViewById(R.id.LUNG_POR_ZG);
        String[] valuesPOR = {"%"};
        pckPOR1.setDisplayedValues(valuesPOR);
        pckPOR1.setTextSize(size);

        pckZonaAmarrilla = findViewById(R.id.LUNG_ZA);
        pckZonaAmarrilla.setMaxValue(99);
        pckZonaAmarrilla.setMinValue(1);
        pckZonaAmarrilla.setValue(35);
        pckZonaAmarrilla.setTextSize(size);

        NumberPicker pckPOR2 = findViewById(R.id.LUNG_POR_ZA);
        pckPOR2.setDisplayedValues(valuesPOR);
        pckPOR2.setTextSize(size);

        pckZonaNaranja = findViewById(R.id.LUNG_ZO);
        pckZonaNaranja.setMaxValue(99);
        pckZonaNaranja.setMinValue(1);
        pckZonaNaranja.setValue(35);
        pckZonaNaranja.setTextSize(size);

        NumberPicker pckPOR3 = findViewById(R.id.LUNG_POR_ZO);
        pckPOR3.setDisplayedValues(valuesPOR);
        pckPOR3.setTextSize(size);

        //region :: FEV1PB
        pckOPBUnidad = findViewById(R.id.LUNG_PBUnidad);
        pckOPBUnidad.setMaxValue(9);
        pckOPBUnidad.setMinValue(0);
        pckOPBUnidad.setValue(3);
        pckOPBUnidad.setTextSize(size);

        NumberPicker pckPUNTO = findViewById(R.id.LUNG_PBPunto);
        String[] valuespunto= {"."};
        pckPUNTO.setDisplayedValues(valuespunto);
        pckPUNTO.setTextSize(size);

        pckOPBDecimalUno = findViewById(R.id.LUNG_PBDecimalUno);
        pckOPBDecimalUno.setMaxValue(9);
        pckOPBDecimalUno.setMinValue(0);
        pckOPBDecimalUno.setValue(5);
        pckOPBDecimalUno.setTextSize(size);

        pckOPBDecimalDos = findViewById(R.id.LUNG_PBDecimalDos);
        pckOPBDecimalDos.setMaxValue(9);
        pckOPBDecimalDos.setMinValue(0);
        pckOPBDecimalDos.setValue(0);
        pckOPBDecimalDos.setTextSize(size);

        NumberPicker pckPUNI = findViewById(R.id.LUNG_PBUni);
        String[] valuespUNI= {"litros"};
        pckPUNI.setDisplayedValues(valuespUNI);
        pckPUNI.setTextSize(size);

        //endregion

        //endregion

        setEnableView(findViewById(R.id.LUNG_btUpdateDate),false);
        setEnableView(findViewById(R.id.LUNG_btDeleteHistoryData),false);
        setEnableView(findViewById(R.id.LUNG_btResetFactory),false);

        clearTextResult();
        setTextStatus(txtResult,"'Remote Mode':\nEncienda el Monitor Pulmonar en modo 'Remote Mode', manteniendo pulsando \n" +
                "simnultanemanete el botón de encendido más el botón de subir unos segundos.\n" +
                "Suelte cuando se muestre el símbolo de un archivo pdf. Pulse conectar\n\n" +
                "Test:\nPara probar el Monitor Pulmonar enciendalo y pulse conectar");

        EnableNumberPicker(false);

        //region :: Intent
        Intent intent = getIntent();
        Bundle b = intent.getExtras();
        if (b != null) {

            //region :: Muestra caracteristicas paciente
            idpaciente = intent.getStringExtra("idpaciente");
            paciente = ApiMethods.loadCharacteristics(idpaciente);;

            setTextStatus(findViewById(R.id.LUNG_txtGenero),paciente.getGender());
            setTextStatus(findViewById(R.id.LUNG_txtEdad),paciente.getAge().toString());
            setTextStatus(findViewById(R.id.LUNG_txtAltura),paciente.getHeight().toString());
            setTextStatus(findViewById(R.id.LUNG_txtPeso),paciente.getWeight().toString());

            if (paciente.getBydefault()) {
                EVLogConfig.log(TAG, "Datos de paciente cargados con valores por defecto");
                txtInfoPaciente.setVisibility(View.VISIBLE);
            }
            //endregion

            String extra = intent.getStringExtra("lung");
            try {
                JSONObject params = new JSONObject(extra);
                Log.e(TAG, "extra: " + params);

                if (params.has("lung")) {
                    JSONObject lung = params.getJSONObject("lung");
                    Log.e(TAG, "lung: " + lung);
                    if (lung.has("FEV1PB")) { FEV1PB = lung.getInt("FEV1PB"); }
                    if (lung.has("GreenZone")) { ZonaVerde = lung.getInt("GreenZone"); }
                    if (lung.has("YellowZone")) { ZonaAmarrilla = lung.getInt("YellowZone"); }
                    if (lung.has("OrangeZone")) { ZonaNaranja = lung.getInt("OrangeZone"); }
                }
                else {
                    Toast.makeText(getApplicationContext(), "No se ha encontrado configuración correcta del paciente.", Toast.LENGTH_LONG).show();
                    finish();
                }

            }
            catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG,"Exceprion: " + e.toString());
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            mDeviceAddress = intent.getStringExtra("mac");
        }
        //endregion

        //region :: Establecer valores en los view
        setNumberPicker(pckZonaVerde,ZonaVerde);
        setNumberPicker(pckZonaAmarrilla,ZonaAmarrilla);
        setNumberPicker(pckZonaNaranja,ZonaNaranja);

        setNumberPicker(pckOPBUnidad,FEV1PB/100);
        int tmp = FEV1PB % 100;
        setNumberPicker(pckOPBDecimalUno,tmp/10);
        setNumberPicker(pckOPBDecimalDos,tmp%10);
        //endregion

        EnableNumberPicker(true);
        retries = 0;
        status = EStatusDevice.None;

        TIOManager.initialize(this.getApplicationContext());
        mTio = TIOManager.getInstance();
        mTio.enableTrace(true);

        // displays a dialog requesting user permission to enable Bluetooth.
        if (! mTio.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ENABLE_BT_REQUEST_ID);
            Log.e(TAG,"Bluetooth no activado");
        }

//        onScanDevice();
    }

    public void onScanDevice() {
        Log.e(TAG, "onScanDevice");
        mTio.removeAllPeripherals();

        setVisibleView(circulodescarga, View.VISIBLE);
        setVisibleView(txtProgress, View.VISIBLE);
        startTimedScan();
    }

    private void startTimedScan() {
        Log.e(TAG, "startTimedScan");
        status = EStatusDevice.Scanning;
        setTextStatus(txtStatus,status.toString());

        Integer timeout = this.SCAN_INTERVAL * 6;  // 15seg x 6 = 90seg
        cTimerSearch = new CountDownTimer(timeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                cTimerSearch.cancel();
                cTimerSearch = null;
                try {
                    Log.e(TAG, "stopScan Timeout");
                    mTio.stopScan();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                setVisibleView(txtProgress, View.INVISIBLE);
                setVisibleView(circulodescarga, View.INVISIBLE);
                String text = txtResult.getText().toString();
                status = EStatusDevice.NotFound;
                setTextStatus(txtStatus,status.toString());
                setTextStatus(txtResult,text + "\nMONITOR PULMONAR NO ENCONTRADO");
            }
        };
        cTimerSearch.start();

        mTio.startScan(this);
    }

    //region :: TIOManagerCallback
    @Override
    public void onPeripheralFound(TIOPeripheral peripheral) {
        Log.e(TAG, "onPeripheralFound(): " + peripheral.toString());

        // overrule default behaviour: peripheral shall be saved only after having been connected
        peripheral.setShallBeSaved(false);
        mTio.savePeripherals();

        Log.e(TAG, "mTio.getPeripherals().length: " + mTio.getPeripherals().length);
        Log.e(TAG,"mDeviceAddress: " + mDeviceAddress);
        if (peripheral.getAddress().equals(mDeviceAddress)) {

            if (cTimerSearch != null) {
                cTimerSearch.cancel();
                cTimerSearch = null;
            }

            status = EStatusDevice.Found;
            setTextStatus(txtStatus, status.toString());

            try {
                Log.e(TAG, "stopScan detected device");
                mTio.stopScan();
            } catch (Exception ex) {
                ex.printStackTrace();
            }


            registerReceiver(mCallbackBeurer, makeUpdateIntentFilter(), RECEIVER_EXPORTED);
            mLungControl = new LUNGcontrol();
            mLungControl.initialize(this, mDeviceAddress);
            status = EStatusDevice.Connecting;
            setTextStatus(txtStatus, status.toString());
            mLungControl.connect();
        }

    }

    @Override
    public void onPeripheralUpdate(TIOPeripheral peripheral) {
        Log.e(TAG, "onPeripheralUpdate() " + peripheral.toString());
    }
    //endregion

    //region :: BROADCAST RECEIVER
    private final BroadcastReceiver mCallbackBeurer = new BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            String message = extras.getString(BleReferences.ACTION_BLE_EXTRA_MESSAGE,"");
            String function;

            Log.e(TAG, "mCallbackReceiver(): " + action);
            switch(action) {
                case BleReferences.ACTION_BLE_CONNECTED:
                    //region :: CONNECTED
                    status = EStatusDevice.Connected;
                    setTextStatus(txtStatus,status.toString());

                    setVisibleView(txtProgress, View.INVISIBLE);
                    setVisibleView(circulodescarga, View.INVISIBLE);

                    setEnableView(findViewById(R.id.LUNG_btUpdateDate),true);
                    setEnableView(findViewById(R.id.LUNG_btDeleteHistoryData),true);
                    setEnableView(findViewById(R.id.LUNG_btResetFactory),true);
                    setTextStatus(txtResult,"Dispositivo Conectado");
                    //endregion
                    break;

                case BleReferences.ACTION_BLE_DISCONNECTED:
                    // region :: DISCONNECTED
                    retries += 1;
                    Log.e(TAG,"DISCONNECTED STATE: " + status.toString());
                    if ((status == EStatusDevice.Connecting) || (status == EStatusDevice.Reconnecting)) {
                        status = EStatusDevice.Reconnecting;
                        setTextStatus(txtStatus,status.toString() + " " + retries);
                        if (retries < 3) {
                            Log.e(TAG,"REINTENTO DE CONEXION: " + retries);

                            String text = txtResult.getText().toString() + "\n";
                            setTextStatus(txtResult,text + status.toString() + " " + retries);
                            status = EStatusDevice.Connecting;
                            try {
                                mLungControl.connect();
                            }
                            catch (Exception ex) {
                                ex.printStackTrace();
                            }

                        }
                        else {
                            String text = txtResult.getText().toString() + "\n";
                            setTextStatus(txtResult,text + status.toString() + " " + retries);
                            text = txtResult.getText().toString() + "\n";
                            setTextStatus(txtResult,text + "Reintentos de conexión excedidos");
                            status = EStatusDevice.Disconnect;
                            setTextStatus(txtStatus,status.toString());

                            setEnableView(findViewById(R.id.LUNG_btConectar),true);
                        }
                    }
                    else if (status == EStatusDevice.Disconnecting) {
                        finish();
                    }
                    else {
                        if (status == EStatusDevice.WaitTest) {
                            String text = txtResult.getText().toString() + "\n";
                            setTextStatus(txtResult,text + "Desconectado en medio de una operación");
                        }
                        status = EStatusDevice.Disconnect;
                        setTextStatus(txtStatus,status.toString());
                        setTextView(txtResult,"Dispositivo desconectado");
                    }
                    //endregion
                    break;

                case BleReferences.ACTION_BLE_CONNECT_FAILED:
                    Log.e(TAG,"Message: " + message);
                    mLungControl.disconnect();
                    break;


                case BleReferences.ACTION_BLE_EXTRA_DATA:
                    //region :: SINGLE TEST DATA
                    byte[] data = extras.getByteArray(BleReferences.ACTION_BLE_EXTRA_DATA);
                    Log.e(TAG, "Data: " + Arrays.toString(data));

                    status = EStatusDevice.EndTest;
                    setTextStatus(txtStatus,status.toString());

//                    JSONObject jextras = new JSONObject();
                    SigleTestData std = new SigleTestData(data,"");
                    Log.e(TAG,"Test: " + std.toString());
                    //endregion
                    break;

                case BleReferences.ACTION_BLE_CMD_DATA:
                    byte[] cmdData = extras.getByteArray(BleReferences.ACTION_BLE_EXTRA_DATA);
                    Log.e(TAG, "CMD DATA: " + Arrays.toString(cmdData));

                    String cmd = "" + (char) cmdData[2] + (char) cmdData[3];
                    Log.e(TAG, "CMD: " + cmd);

                    if (cmd.equals("GT")) {
                        // GET TIME
                        setTextStatus(txtResult,mLungControl.parseGetTime(cmdData));
                    }
                    else if (cmd.equals("ST") || cmd.equals("CM") || cmd.equals("RD") || cmd.equals("XR")) {
                        setTextStatus(txtResult,"ACK: " + myfunction.toString());
                        if (cmd.equals("XR")) {
                            mLungControl.disconnect();
                        }
                    }




//                    status = EStatusDevice.EndTest;
//                    setTextStatus(txtStatus,status.toString());

//                    mLungControl.disconnect();
                    break;

                case BleReferences.ACTION_BLE_FAIL:
                    Log.e(TAG,"Message: " + message);
                    mLungControl.disconnect();
                    break;

                case BleReferences.ACTION_BLE_CMD_FAIL:
                    Log.e(TAG,"Message: " + message);
                    function = util.getStringValueJSON(message,"function");
                    setTextStatus(txtStatus,"Fail process: " + function);

                    mLungControl.disconnect();
                    break;

                case BleReferences.ACTION_BLE_COMMUNICATION_TIMEOUT:
                    Log.e(TAG,"ACTION_BLE_COMMUNICATION_TIMEOUT Message:" + message);

                    String text = util.getStringValueJSON(message,"function");
                    if (text.equals("ExitRemoteMode")) {
                        mLungControl.disconnect();
                    }
                    else {
                        setTextView(txtResult,"Monitor Pulmonar no se encuentra en 'Remote Mode");
                    }
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
                        setTextStatus(txtStatus,"Viculación en curso");
                    }
                    //endregion
                    break;

                case BluetoothDevice.ACTION_PAIRING_REQUEST:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.e(TAG,"ACTION_PAIRING_REQUEST: " + device.getName() + " " + device.getAddress());
                    break;

                default:
                    Log.e(TAG,"accion de broadcast no implementada");
                    mLungControl.disconnect();
                    break;

            };
        }
    };

    private static IntentFilter makeUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        // COMUNES LUNG
        intentFilter.addAction(BleReferences.ACTION_BLE_CONNECTED);
        intentFilter.addAction(BleReferences.ACTION_BLE_DISCONNECTED);
        intentFilter.addAction(BleReferences.ACTION_BLE_CONNECT_FAILED);
        intentFilter.addAction(BleReferences.ACTION_BLE_GET_OTHER);
        intentFilter.addAction(BleReferences.ACTION_BLE_CMD_FAIL);
        intentFilter.addAction(BleReferences.ACTION_BLE_FAIL);
        intentFilter.addAction(BleReferences.ACTION_BLE_COMMUNICATION_TIMEOUT);

        // EMPAREJAMIENTO
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);

        // MESSAGE AND DATA
        intentFilter.addAction(BleReferences.ACTION_BLE_EXTRA_MESSAGE);
        intentFilter.addAction(BleReferences.ACTION_BLE_EXTRA_DATA);
        intentFilter.addAction(BleReferences.ACTION_BLE_CMD_DATA);

        return intentFilter;
    }
    //endregion

    // region :: ONCLICK
    @Override
    public void onClick(View view) {
        Log.e(TAG, " onClick()");

        int viewId = view.getId();
        if (viewId == R.id.LUNG_btSavePatient) {
            //region :: GUARDAR CONFIGURACION DEL MONITOR PULMONAR

            ZonaVerde = pckZonaVerde.getValue();
            ZonaAmarrilla = pckZonaAmarrilla.getValue();
            ZonaNaranja = pckZonaNaranja.getValue();

            FEV1PB = pckOPBUnidad.getValue() * 100 + pckOPBDecimalUno.getValue() * 10 + pckOPBDecimalDos.getValue();

            try {
                JSONObject message = new JSONObject();
                message.put("deviceinfo","lung");

                JSONObject lung = new JSONObject();
                lung.put("FEV1PB",FEV1PB);
                lung.put("GreenZone",ZonaVerde);
                lung.put("YellowZone",ZonaAmarrilla);
                lung.put("OrangeZone",ZonaNaranja);

                message.put("lung",lung);
                Log.e(TAG, "************* config lung: " + message.toString());
                broadcastUpdate(BeurerReferences.ACTION_EXTRA_DATA, message.toString());

                if (status == EStatusDevice.Connected) {
                    status = EStatusDevice.Disconnecting;
                    setTextStatus(txtStatus,status.toString());

                    setTextStatus(txtResult,"Desconectando con el dispositivo\nESPERE");

                    setEnableView(findViewById(R.id.LUNG_imfAtras),false);
                    setEnableView(findViewById(R.id.LUNG_btUpdateDate),false);
                    setEnableView(findViewById(R.id.LUNG_btDeleteHistoryData),false);
                    setEnableView(findViewById(R.id.LUNG_btResetFactory),false);
                    setEnableView(findViewById(R.id.LUNG_btSavePatient),false);

                    myfunction = EStatusDevice.ExitRemoteMode;
                    mLungControl.ExitRemoteMode();
                }
                else {
                    finish();
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG,"Exceprion: " + e.toString());
            }
            //endregion
        }
        else if (viewId == R.id.LUNG_btCalcularPB) {
            FEV1PB = calculateFEV1PersonalBest(idpaciente, paciente);
            if (FEV1PB == null) {
                Toast.makeText(getApplicationContext(), "Error al calcular el FEV1PB.", Toast.LENGTH_LONG).show();
                FEV1PB = 300;
            }
            setNumberPicker(pckOPBUnidad,FEV1PB/100);
            int tmp = FEV1PB % 100;
            setNumberPicker(pckOPBDecimalUno,tmp/10);
            setNumberPicker(pckOPBDecimalDos,tmp%10);
        }
        else if (viewId == R.id.LUNG_btConectar) {
            clearTextResult();
            setEnableView(findViewById(R.id.LUNG_btConectar),false);
            onScanDevice();
        }
        else if (viewId == R.id.LUNG_btUpdateDate) {
            myfunction = EStatusDevice.SetTime;
            setTextStatus(txtResult, "Send >> " + myfunction.toString());
            mLungControl.setTime();
        }
        else if (viewId == R.id.LUNG_btDeleteHistoryData) {
            myfunction = EStatusDevice.ClearMemory;
            setTextStatus(txtResult, "Send >> " + myfunction.toString());
            mLungControl.setClearMemory();
        }
        else if (viewId == R.id.LUNG_btResetFactory) {
            myfunction = EStatusDevice.ResetDefault;
            setTextStatus(txtResult, "Send >> " + myfunction.toString());
            mLungControl.ResetDefaults();
        }
        else if (viewId == R.id.LUNG_imfAtras) {
            //region :: STOP
            if (status == EStatusDevice.Connected) {
                try {
                    status = EStatusDevice.Disconnecting;
                    setTextStatus(txtStatus,status.toString());

                    setTextStatus(txtResult,"Desconectando con el dispositivo\nESPERE");

                    setEnableView(findViewById(R.id.LUNG_imfAtras),false);
                    setEnableView(findViewById(R.id.LUNG_btUpdateDate),false);
                    setEnableView(findViewById(R.id.LUNG_btDeleteHistoryData),false);
                    setEnableView(findViewById(R.id.LUNG_btResetFactory),false);
                    setEnableView(findViewById(R.id.LUNG_btSavePatient),false);

                    myfunction = EStatusDevice.ExitRemoteMode;
                    mLungControl.ExitRemoteMode();  // Exit remote mode + mLungControl.disconnect();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            finish();
            //endregion
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
        finish();
    }

    @Override
    protected  void onPause() {
        super.onPause();
        Log.e(TAG,"onPause()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG,"onDestroy()");

        if (cTimerSearch != null) {
            cTimerSearch.cancel();
            cTimerSearch = null;
        }

        if (mLungControl != null) {
            mLungControl.destroy();
            unregisterReceiver(mCallbackBeurer);
        }
        mLungControl = null;

        TIOManager.getInstance().done();

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}
    //endregion

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

    //region :: Rellenado de view

    private void EnableNumberPicker(boolean enable) {
        // Zonas
        setEnableView(pckZonaVerde,enable);
        setEnableView(pckZonaAmarrilla,enable);
        setEnableView(pckZonaNaranja,enable);

        // FEV1PB
        setEnableView(pckOPBUnidad,enable);
        setEnableView(pckOPBDecimalUno,enable);
        setEnableView(pckOPBDecimalDos,enable);
    }
    //endregion

    //region :: runOnUiThread VIEWs
    private void setTextView(View view, String texto) {
//        Log.e(TAG,"len: " + txtResult.getText().length());
        textResult += '\n' + texto;
        runOnUiThread(new Runnable() {
            public void run() {

                if (view instanceof TextView) {
                    TextView obj = (TextView) view;
                    obj.setText(textResult);
//                    Log.e(TAG,":: "+texto);
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
            }
        });
    }

    private void clearTextResult() {
        textResult = "";
        setTextView(txtResult,"");
    }

    private void setSelectionIdSpinner(Spinner spn, int id) {
        runOnUiThread(new Runnable() {
            public void run() {
                spn.setSelection(id);
            }
        });
    }

    private void setNumberPicker(NumberPicker view, int number) {
        runOnUiThread(new Runnable() {
            public void run() {
                view.setValue(number);
            }
        });
    }

    //endregion

    //region :: Broadcast
    private void broadcastUpdate(final String action, String message) {
        final Intent intent = new Intent(action);
        intent.putExtra(BeurerReferences.ACTION_EXTRA_MESSAGE, message);
        this.sendBroadcast(intent);
    }

    //endregion
}