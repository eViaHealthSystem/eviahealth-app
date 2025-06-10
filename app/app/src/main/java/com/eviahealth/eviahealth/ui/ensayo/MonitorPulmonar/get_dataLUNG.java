package com.eviahealth.eviahealth.ui.ensayo.MonitorPulmonar;

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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.bluetooth.models.BleReferences;
import com.eviahealth.eviahealth.bluetooth.models.EStatusDevice;
import com.eviahealth.eviahealth.models.devices.DeviceID;
import com.eviahealth.eviahealth.devices.NombresDispositivo;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.utils.PermissionUtils;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;
import com.eviahealth.eviahealth.bluetooth.vitalograph.LUNGcontrol;
import com.eviahealth.eviahealth.models.vitalograph.LungData;
import com.eviahealth.eviahealth.models.vitalograph.Patient;
import com.eviahealth.eviahealth.models.vitalograph.SigleTestData;
import com.telit.terminalio.TIOManager;
import com.telit.terminalio.TIOManagerCallback;
import com.telit.terminalio.TIOPeripheral;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class get_dataLUNG extends BaseActivity implements View.OnClickListener, TIOManagerCallback {

    private String TAG = "GET_DATALUNG";
    private LUNGcontrol mLungControl = null;
    TextToSpeechHelper textToSpeech;
    TextView txtStatus;
    CountDownTimer cTimerSearch = null;       // TIMEOUT >> Busqueda de dispositivo
    CountDownTimer cTimerMeassure = null;     // TIMEOUT >> Tiempo de prueba superado
    CountDownTimer cTimerMax = null;          // TIMEOUT >> Tiempo de Test Máximo por intento
    private static int TIMEOUT_MAX_TEST = 1000 * 60 * 5;  // 5 minutos
    String DEVICE_MAC_ADDRESS = "";
    private String mDeviceAddress = "";  // "00:80:25:D8:5A:5B";
    EStatusDevice status = EStatusDevice.None;
    private int retries;
    Boolean viewdata = false;
    private Patient paciente = null;
    String configlung = "{}";
    private LungData datos;

    //region :: TIO
    private static final int 		ENABLE_BT_REQUEST_ID = 1;
    private static final int		SCAN_INTERVAL 		 = 15000;
    private static final int		MEASURE_INTERVAL     = 60000;
    private TIOManager mTio = null;
    private TIOPeripheral 	mPeripheral;
    //endregion

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_data_lung);

        EVLog.log(TAG, "onCreate()");
        PermissionUtils.requestAll(this);

        txtStatus = findViewById(R.id.txtStatusLUNG);
        txtStatus.setTextSize(14);
        txtStatus.setVisibility(View.VISIBLE);
        setVisibleView(findViewById(R.id.cdescarga_lung), View.INVISIBLE);
        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        retries = 0;
        datos = LungData.getInstance();

        if (datos.getPatient() == null) {
//        String idpaciente = Config.getInstance().getIdPacienteTablet();
            String idpaciente = Config.getInstance().getIdPacienteEnsayo();
            paciente = ApiMethods.loadCharacteristics(idpaciente);
            datos.setPatient(paciente);

            if (paciente.getBydefault()) {
                EVLog.log(TAG,"Datos de paciente cargados con valores por defecto");
            }
        }

        DEVICE_MAC_ADDRESS = Config.getInstance().getIdentificador(NombresDispositivo.MONITORPULMONAR);
        EVLog.log(TAG,"MAC DISPOSITIVO: "+ DEVICE_MAC_ADDRESS);
        if (DEVICE_MAC_ADDRESS.contains("LUNG")){
            DEVICE_MAC_ADDRESS = DEVICE_MAC_ADDRESS.replace("LUNG-", "");
            mDeviceAddress = util.MontarMAC(DEVICE_MAC_ADDRESS);
            EVLog.log(TAG,"MAC mDeviceAddress: "+ mDeviceAddress);
        }

        //region >> Carga desde la db
        try {
            // Cargamos datos de la configuracion del lung desde la db
            String id_paciente = Config.getInstance().getIdPacienteEnsayo();
            String response = ApiMethods.getExtrasDevice(id_paciente, DeviceID.Lung.getValue());

            JSONObject obj = new JSONObject(response);
            if (obj.has("code")) {
                Log.e(TAG,"ERROR: " + obj.toString());
                datos.setIdTest(3);
                datos.setStatusDescarga(false);
                datos.setERROR("{\"type\":\"LUNG\",\"error\":803,\"description\":\"ERROR ACCEDER A SERVIDOR\"}");
                ViewResult();
                return;
            }
            else if (obj.has("extra")) {
                JSONObject jextras = new JSONObject(obj.getString("extra"));
                Log.e(TAG, "jextras: " + jextras.toString());

                if (jextras.has("lung") == false) {
                    JSONObject lung = new JSONObject();
                    lung.put("FEV1PB", calculateFEV1PersonalBest(id_paciente, paciente));
                    lung.put("GreenZone", 50);  // 80
                    lung.put("YellowZone", 35); // 50
                    lung.put("OrangeZone", 20); // 30
                    jextras.put("lung", lung);
                }

                if (jextras.has("lung")) {
                    JSONObject lung = jextras.getJSONObject("lung");
                    configlung = lung.toString();
                }

                EVLog.log(TAG, "JEXTRAS LUNG: " + configlung);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        //endregion

        TIOManager.initialize(this.getApplicationContext());
        mTio = TIOManager.getInstance();
        mTio.enableTrace(true);

        // displays a dialog requesting user permission to enable Bluetooth.
        if (! mTio.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ENABLE_BT_REQUEST_ID);
            Log.e(TAG,"Bluetooth no activado");
        }

        Integer timeout = this.MEASURE_INTERVAL * 2;  // 60seg x 2 = 120seg
        cTimerMeassure = new CountDownTimer(timeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                cTimerMeassure.cancel();
                cTimerMeassure = null;
                EVLog.log(TAG,"Tiempo para la rezalización de la prueba superado");

                if (status == EStatusDevice.WaitTest) {
                    mLungControl.disconnect();
                }
                else {
                    // Esto no debería pasar nunca
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":\"LUNG\",\"error\":802,\"description\":\"TIMEOUT, TEST NO REALIZADO\"}");
                    ViewResult();
                }
            }
        };

        cTimerMax = new CountDownTimer(timeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                cTimerMax.cancel();
                cTimerMax = null;
                EVLog.log(TAG,"Tiempo superado para la rezalización de la prueba.");

                if (status == EStatusDevice.WaitTest) {
                    mLungControl.disconnect();
                }
                else {
                    // Esto no debería pasar nunca
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":\"LUNG\",\"error\":802,\"description\":\"TIMEOUT, TEST NO REALIZADO\"}");
                    ViewResult();
                }
            }
        };
        cTimerMax.start();

        onScanDevice();
    }

    public void onScanDevice() {
        EVLog.log(TAG, "onScanDevice");
        mTio.removeAllPeripherals();
        startTimedScan();
    }

    private void startTimedScan() {
        EVLog.log(TAG, "startTimedScan");
        status = EStatusDevice.Scanning;
        setTextSatus(status.toString());

        Integer timeout = this.SCAN_INTERVAL * 6;  // 15seg x 6 = 90seg
        cTimerSearch = new CountDownTimer(timeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                cTimerSearch.cancel();
                cTimerSearch = null;
                try {
                    EVLog.log(TAG, "Monitor pulmonar no detectado (stopScan Timeout)");
                    mTio.stopScan();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                datos.setStatusDescarga(false);
                datos.setERROR("{\"type\":\"LUNG\",\"error\":800,\"description\":\"TIMEOUT, NO DETECTADO DISPOSITIVO\"}");
                ViewResult();
            }
        };
        cTimerSearch.start();

        mTio.startScan(this);
    }

    //region :: TIOManagerCallback
    @Override
    public void onPeripheralFound(TIOPeripheral peripheral) {
        EVLog.log(TAG, "onPeripheralFound(): " + peripheral.toString());

        // overrule default behaviour: peripheral shall be saved only after having been connected
        peripheral.setShallBeSaved(false);
        mTio.savePeripherals();

        Log.e(TAG, "mTio.getPeripherals().length: " + mTio.getPeripherals().length);

        if (peripheral.getAddress().equals(mDeviceAddress)) {

            if (cTimerSearch != null) {
                cTimerSearch.cancel();
                cTimerSearch = null;
            }

            status = EStatusDevice.Found;
            setTextSatus(status.toString());

            try {
                Log.e(TAG, "stopScan detected device");
                mTio.stopScan();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            findViewById(R.id.fondoLUNG).setBackground(getDrawable(R.drawable.fondo_lung));
            setVisibleView(findViewById(R.id.cdescarga_lung), View.VISIBLE);

            registerReceiver(mCallbackBeurer, makeUpdateIntentFilter(), RECEIVER_EXPORTED);
            mLungControl = new LUNGcontrol();
            mLungControl.initialize(this, mDeviceAddress);
            status = EStatusDevice.Connecting;
            setTextSatus(status.toString());
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

            EVLog.log(TAG, "mCallbackReceiver(): " + action);
            switch(action) {
                case BleReferences.ACTION_BLE_CONNECTED:
                    //region :: CONEXION EXITOSA
                    status = EStatusDevice.Connected;
                    setTextSatus(status.toString());

                    setVisibleView(findViewById(R.id.cdescarga_lung), View.INVISIBLE);
                    findViewById(R.id.fondoLUNG).setBackground(getDrawable(R.drawable.lung_prueba));
                    textToSpeech.stop();

                    cTimerMeassure.start();   // 2 minutos para la prueba

                    status = EStatusDevice.WaitTest;
                    setTextSatus(status.toString());
                    //endregion
                    break;

                case BleReferences.ACTION_BLE_DISCONNECTED:
                    // region :: DISCONNECTED
                    retries += 1;
                    if (cTimerMeassure != null) cTimerMeassure.cancel();

                    EVLog.log(TAG,"DISCONNECTED STATE: " + status.toString());
                    if ((status == EStatusDevice.Connecting) || (status == EStatusDevice.Reconnecting)) {
                        status = EStatusDevice.Reconnecting;
                        setTextSatus(status.toString() + " " + retries);
                        if (retries < 3) {
                            EVLog.log(TAG,"REINTENTO DE CONEXION: " + retries);
                            setTextSatus("CONNECT[" + retries + "]");

                            status = EStatusDevice.Connecting;
                            setTextSatus(status.toString());
                            try {
                                mLungControl.connect();
                            }
                            catch (Exception ex) {
                                ex.printStackTrace();
                            }

                        }
                        else {
                            datos.setStatusDescarga(false);
                            datos.setERROR("{\"type\":\"LUNG\",\"error\":801,\"description\":\"REINTENTOS DE CONEXIÓN SUPERADO.\"}");
                            ViewResult();
                        }
                    }
                    else {
                        if (status == EStatusDevice.WaitTest) {
                            datos.setStatusDescarga(false);
                            datos.setERROR("{\"type\":\"LUNG\",\"error\":802,\"description\":\"TIMEOUT, TEST NO REALIZADO\"}");
                        }
                        ViewResult();
                    }
                    //endregion
                    break;

                case BleReferences.ACTION_BLE_CONNECT_FAILED:
                    EVLog.log(TAG,"Message: " + message);
                    mLungControl.disconnect();
                    break;


                case BleReferences.ACTION_BLE_EXTRA_DATA:
                    byte[] data = extras.getByteArray(BleReferences.ACTION_BLE_EXTRA_DATA);
                    EVLog.log(TAG, "Data: " + Arrays.toString(data));

                    if (cTimerMeassure != null) cTimerMeassure.cancel();

                    status = EStatusDevice.EndTest;
                    setTextSatus(status.toString());

                    SigleTestData std = new SigleTestData(data, configlung);
                    EVLog.log(TAG,"Data Test: " + std.toString());

                    datos.setSingleTestData(std);
                    datos.setStatusDescarga(true);

                    mLungControl.disconnect();
                    break;

                case BleReferences.ACTION_BLE_FAIL:
                    Log.e(TAG,"Message: " + message);
                    mLungControl.disconnect();
                    break;

                case BleReferences.ACTION_BLE_CMD_FAIL:
                    Log.e(TAG,"Message: " + message);
                    function = util.getStringValueJSON(message,"function");
                    setTextSatus("Fail process: " + function);

//                    datos.set_status_descarga(false);
//                    datos.setERROR_PO("{\"type\":\"PO60\",\"error\":803,\"description\":\"Detectado un fallo de comunicación con el dispositivo..\"}");

                    mLungControl.disconnect();
                    break;

                case BleReferences.ACTION_BLE_COMMUNICATION_TIMEOUT:
                    EVLog.log(TAG,"ACTION_BEURER_COMMUNICATION_TIMEOUT Message:" + message);

//                    datos.set_status_descarga(false);
//                    datos.setERROR_PO("{\"type\":\"PO60\",\"error\":802,\"description\":\"Ha fallado la descarga de datos con el dispositivo.\"}");
                    mLungControl.disconnect();
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
//                    datos.set_status_descarga(false);
//                    datos.setERROR_PO("{\"type\":\"PO60\",\"error\":804,\"description\":\"ACCION NO CONTEMPLADA\"}");
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

        return intentFilter;
    }
    //endregion

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.imageButtonAudio) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            String frase = "";
            if (status == EStatusDevice.Scanning) {
                frase = "Encienda el monitor pulmonar pulsando el botón de encendido, durante unos segundos. ." +
                        "Oirá dos pitidos cortos cuando el dispositivo esté encendido. Este deberá mostrar el símbolo que se muestra en la imagen";
                texto.add(frase);
            }
            else if ((status == EStatusDevice.Found) || (status == EStatusDevice.Connecting)) {
                frase = "Inserte la boquilla en el monitor pulmonar.";
                texto.add(frase);
                frase = "Espere unos segundos hasta que se establezca conexión con el monitor pulmonar.";
                texto.add(frase);
            }
//            else if (status == EStatusDevice.Connected) {
            else {
                frase = "Compruebe que el dispositivo esté listo para realizar la prueba. Siéntese en posición erguida para soplar.";
                texto.add(frase);
                frase = "Respire (inspire) lo más profundamente posible y sostenga la respiración. Coloque la boquilla en la boca, mordiéndola ligeramente y con los labios firmemente sellados alrededor de ella.";
                texto.add(frase);
                frase = "Sople lo más " +
                        "FUERTE y RÁPIDO " +
                        "que pueda durante el " +
                        "MAYOR TIEMPO " +
                        "posible. Espere a que se muestren los resultados.";
                texto.add(frase);
            }
            textToSpeech.speak(texto);
            //endregion
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {

        EVLog.log(TAG, "onDestroy()");

        if (cTimerSearch != null) {
            cTimerSearch.cancel();
            cTimerSearch = null;
        }
        if (cTimerMeassure != null) {
            cTimerMeassure.cancel();
            cTimerMeassure = null;
        }
        if (cTimerMax != null) {
            cTimerMax.cancel();
            cTimerMax = null;
        }

        textToSpeech.shutdown();
        if (mLungControl != null) {
            mLungControl.destroy();
            unregisterReceiver(mCallbackBeurer);
        }
        mLungControl = null;

        if (mTio != null) {
            TIOManager.getInstance().done();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause()");
//        finish();
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

    public void ViewResult() {
        if (viewdata == false) {
            viewdata = true;
//            EVLog.log(TAG, " ViewResult()");
            Intent intent;
            if (datos.getStatusDescarga()) {
                Log.e(TAG, " ViewResult(view_dataLUNG.class)");
//                intent = new Intent(this, view_dataLUNG.class);
                intent = new Intent(this, view_data2LUNG.class);
            }
            else {
                Log.e(TAG, " ViewResult(view_failLUNG.class)");
                intent = new Intent(this, view_failLUNG.class);
            }
            startActivity(intent);
            finish();
        }
        finish();
    }

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

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){
    }
}