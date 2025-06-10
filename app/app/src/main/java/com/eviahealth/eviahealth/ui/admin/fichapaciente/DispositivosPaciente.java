package com.eviahealth.eviahealth.ui.admin.fichapaciente;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.eviahealth.eviahealth.api.BackendConector.ApiConnector;
import com.eviahealth.eviahealth.api.BackendConector.ApiException;
import com.eviahealth.eviahealth.api.BackendConector.ApiUrl;
import com.eviahealth.eviahealth.bluetooth.BeurerControl;
import com.eviahealth.eviahealth.models.devices.FichaPacienteDatos;
import com.eviahealth.eviahealth.models.devices.FichaPacienteSpinnerValues;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FileFuntions;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.config.bf600.config_BF600;
import com.eviahealth.eviahealth.ui.config.ecg.config_Kardia6L;
import com.eviahealth.eviahealth.ui.config.ihealth.config_HS2S;
import com.eviahealth.eviahealth.bluetooth.models.Pairing;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.admin.tecnico.QRReader;
import com.eviahealth.eviahealth.bluetooth.models.Global;
import com.eviahealth.eviahealth.devices.BeurerReferences;
import com.eviahealth.eviahealth.devices.ClassDispositivo;
import com.eviahealth.eviahealth.models.devices.Dispositivo;
import com.eviahealth.eviahealth.models.devices.EquipoPaciente;
import com.eviahealth.eviahealth.devices.NombresDispositivo;
import com.eviahealth.eviahealth.devices.TranstekDevice;
import com.eviahealth.eviahealth.ui.config.mb6.config_Mambo6;
import com.eviahealth.eviahealth.ui.admin.tecnico.LoginSetting;
import com.eviahealth.eviahealth.utils.PermissionUtils;
import com.eviahealth.eviahealth.utils.dialogs.Alerts;
import com.eviahealth.eviahealth.utils.log.EVLogConfig;
import com.eviahealth.eviahealth.utils.log.ConfigLog;
import com.eviahealth.eviahealth.utils.util;
import com.eviahealth.eviahealth.ui.config.lung.config_LUNG;
import com.lifesense.plugin.ble.LSBluetoothManager;
import com.lifesense.plugin.ble.OnSearchingListener;
import com.lifesense.plugin.ble.data.LSDeviceInfo;
import com.lifesense.plugin.ble.data.LSDeviceType;
import com.lifesense.plugin.ble.data.LSManagerStatus;
import com.lifesense.plugin.ble.data.LSProtocolType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DispositivosPaciente extends BaseActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener  {

    final String TAG = "CONFIG_DEVICES";

    //region :: DISPOSITIVO VISIBLES > false = View.INVISIBLE
    private final Boolean SCALE_VISIBLE = true;
    private final Boolean LUNG_VISIBLE = true;
    private final Boolean ECG_VISIBLE = true;
    private final Boolean PEAK_VISIBLE = true;
    //endregion

    //region :: Variables
    private final List<String> macs_devices = new ArrayList<>();

    Integer cont_scan = 0;
    String idpaciente;
    Boolean btn_aplicar = false;
    Boolean btn_wireless = false;

    //region >> elementos ui
    Button btnScanOximetro, btnScanTensiometro, btnScanActividad, btnScanTermometro, btnScanScale, btnScanMONPUL, btnScanECG;
    Button btnBleOxi, btnBleTen, btnBleAct, btnBleThermo, btnBleScale, btnBleMONPUL, btnBleECG;
    Button btnCnfOxi, btnCnfTen, btnCnfAct, btnCnfThermo, btnCnfScale, btnCnfMONPUL, btnCnfECG;
    ImageView btn_cambiar, imgMultiPaciente;
    private Spinner spinnerOXI, spinnerTEN, spinnerACT, spinnerTER, spinnerBAS, spinnerMONPUL, spinnerECG;
    private Spinner spinnerPEAK, spinnerCAT, spinnerENC, spinnerLOGS;
    private TableLayout tabla;
    ProgressBar circulodescarga;

    Button btn_salirso, btn_salir, btn_ok;
    TextView txtConexion, txtStatus, txtCuentaAtras, txtMultiPaciente;
    //endregion

    CountDownTimer cTimerSearch = null;     // TIMEOUT >> Busqueda de dispositivo
    Integer cuentaAtras;

    // asociacion dispositivos con id
    Map<NombresDispositivo, Dispositivo> MAP_DISP_ID;
    Map<Integer, NombresDispositivo> MAP_ID_DISP;
    Map<NombresDispositivo, EquipoPaciente> EQUIPOS_PAC;

    // Variables scanner dispositivos beurer -------
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private static final Integer TIMEOUT_BLE = 15;
    private List<String> address = new ArrayList<>();
    private String typeScan = "all";
    private Pairing pairingModel = Pairing.None;
    private boolean scanning = false;
    private BeurerControl mBeurerControl = new BeurerControl();
    private int retries;
    private String mDeviceAddress = "";
    // ----------------------------------------------

    private boolean config = false;
    private boolean qrreader = false; // Para indicar si vamos a escanear nuevo QR

    private Boolean multipaciente = false;
    //endregionF

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispositivos_paciente);
        EVLogConfig.log(TAG, " onCreate()");

        PermissionUtils.requestAll(this);

        cont_scan = 0;
        qrreader = false;

        //region :: Asignación de Views

        tabla = findViewById(R.id.tablaizq);

        TextView numeroserie = findViewById(R.id.numero_serie);
        txtConexion = findViewById(R.id.text_conexión_fallida);
        txtConexion.setVisibility(View.INVISIBLE);
        txtStatus = findViewById(R.id.txtStatusConf);
        txtStatus.setTextSize(14);
        txtStatus.setVisibility(View.VISIBLE);

        circulodescarga = findViewById(R.id.cdescarga_paciente);
        circulodescarga.setVisibility(View.INVISIBLE);
        txtCuentaAtras = findViewById(R.id.txtCuentaAtras);
        txtCuentaAtras.setTextSize(22);
        txtCuentaAtras.setVisibility(View.INVISIBLE);

        btn_salirso = findViewById(R.id.activity_fichapaciente_btn_salirso);
        btn_salir = findViewById(R.id.activity_fichapaciente_btn_salir);
        btn_cambiar = findViewById(R.id.pacienteQR);
        btn_ok = findViewById(R.id.activity_fichapaciente_btn_ok);

        btnScanOximetro = findViewById(R.id.btnScanOximetro);
        btnScanTensiometro = findViewById(R.id.btnScanTensiometro);
        btnScanActividad = findViewById(R.id.btnScanActividad);
        btnScanTermometro = findViewById(R.id.btnScanTermometro);
        btnScanScale = findViewById(R.id.btnScanScale);
        btnScanMONPUL = findViewById(R.id.btnScanMonitorPulmonar);
        btnScanECG = findViewById(R.id.btnScanECG);

        btnBleOxi = findViewById(R.id.btnVincularOxi);
        btnBleOxi.setVisibility(View.INVISIBLE);
        btnBleTen = findViewById(R.id.btnVincularTen);
        btnBleTen.setVisibility(View.INVISIBLE);
        btnBleAct = findViewById(R.id.btnVincularAct);
        btnBleAct.setVisibility(View.INVISIBLE);
        btnBleThermo = findViewById(R.id.btnVincularTermo);
        btnBleThermo.setVisibility(View.INVISIBLE);
        btnBleScale = findViewById(R.id.btnVincularScale);
        btnBleScale.setVisibility(View.INVISIBLE);
        btnBleMONPUL = findViewById(R.id.btnVincularMonitorPulmonar);
        btnBleMONPUL.setVisibility(View.INVISIBLE);
        btnBleECG = findViewById(R.id.btnVinculaECG);
        btnBleECG.setVisibility(View.INVISIBLE);

        btnCnfOxi = findViewById(R.id.btnConfigOxi);
        btnCnfOxi.setVisibility(View.INVISIBLE);
        btnCnfTen = findViewById(R.id.btnConfigTen);
        btnCnfTen.setVisibility(View.INVISIBLE);
        btnCnfAct = findViewById(R.id.btnConfigAct);
        btnCnfAct.setVisibility(View.INVISIBLE);
        btnCnfThermo = findViewById(R.id.btnConfigTermo);
        btnCnfThermo.setVisibility(View.INVISIBLE);
        btnCnfScale = findViewById(R.id.btnConfigScale);
        btnCnfScale.setVisibility(View.INVISIBLE);
        btnCnfMONPUL = findViewById(R.id.btnConfigMonitorPulmonar);
        btnCnfMONPUL.setVisibility(View.INVISIBLE);
        btnCnfECG = findViewById(R.id.btnConfigECG);
        btnCnfECG.setVisibility(View.INVISIBLE);

        txtMultiPaciente = findViewById(R.id.txtmultipaciente);
        txtMultiPaciente.setVisibility(View.INVISIBLE);

        imgMultiPaciente = findViewById(R.id.multipaciente);

        //endregion

        String serial = leerSerial();
        numeroserie.setText("IMEI: " + serial);

        //region :: Multi Paciente
        try {
            File pathPaciente = new File(FileAccess.getPATH_FILES(), FilePath.CONFIG_PACIENTE.getNameFile());
            if (FileFuntions.checkFileExist(pathPaciente.getName())) {
                JSONObject json_paciente = util.readFileJSON(pathPaciente);
                Log.e(TAG, "paciente.json: " + json_paciente.toString());
                if (json_paciente.has("multipaciente")) {
                    multipaciente = json_paciente.getBoolean("multipaciente");
                    if (multipaciente) {
                        txtMultiPaciente.setVisibility(View.VISIBLE);
                        imgMultiPaciente.setImageResource(R.drawable.multiuser_active);
                    }
                }
            }
            else { Log.e(TAG, "Fichero paciente.json no existe"); }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
        //endregion

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        //init LSBluetoothManager Transtek
        LSBluetoothManager.getInstance().initManager(getApplicationContext());

        registerReceiver(mCallbackBeurer, makeUpdateIntentFilter(), RECEIVER_EXPORTED);

        //region :: Obtener el idpaciente del intent
        idpaciente = getIntent().getStringExtra("numeropaciente");
        EditText text_idpaciente = findViewById(R.id.activity_fichapaciente_text_idpaciente);
        if (idpaciente != null) {
            text_idpaciente.setText(idpaciente);
            text_idpaciente.setEnabled(false);
        }else {
            try {
                JSONObject json_paciente = FileAccess.leerJSON(FilePath.CONFIG_PACIENTE);
                idpaciente = json_paciente.getString("idpaciente");
                text_idpaciente.setText(idpaciente);
                text_idpaciente.setEnabled(false);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }
        //endregion

        //region :: SPINNERS

        // spinners
        spinnerOXI = findViewById(R.id.spnOXI);
        spinnerTEN = findViewById(R.id.spnTEN);
        spinnerACT = findViewById(R.id.spnACT);
        spinnerTER = findViewById(R.id.spinnerTER);
        spinnerBAS = findViewById(R.id.spinnerBAS);
        spinnerMONPUL = findViewById(R.id.spinnerMONITORPULMONAR);
        spinnerECG = findViewById(R.id.spinnerECG);

        List<String> values_spinner_tmp = new ArrayList<>();
        values_spinner_tmp.add("Deshabilitado");

        //region :: Spinner Dinamicos
        setSpinnerContent(spinnerOXI,values_spinner_tmp);
        spinnerOXI.setOnItemSelectedListener(this);

        setSpinnerContent(spinnerTEN,values_spinner_tmp);
        spinnerTEN.setOnItemSelectedListener(this);

        setSpinnerContent(spinnerACT,values_spinner_tmp);
        spinnerACT.setOnItemSelectedListener(this);

        setSpinnerContent(spinnerTER, FichaPacienteSpinnerValues.values_spinner_termometro);
        spinnerTER.setOnItemSelectedListener(this);

        setSpinnerContent(spinnerBAS,FichaPacienteSpinnerValues.values_spinner_scale);
        spinnerBAS.setOnItemSelectedListener(this);

        setSpinnerContent(spinnerMONPUL,values_spinner_tmp);
        spinnerMONPUL.setOnItemSelectedListener(this);

        setSpinnerContent(spinnerECG,values_spinner_tmp);
        spinnerECG.setOnItemSelectedListener(this);
        //endregion

        //region :: Carga única
        //spinner encuesta
        spinnerENC = findViewById(R.id.spinnerENC);
        setSpinnerContent(spinnerENC,values_spinner_tmp);
        spinnerENC.setOnItemSelectedListener(this);

        // spinner peakflow
        spinnerPEAK = findViewById(R.id.spinnerPEAK);
        setSpinnerContent(spinnerPEAK, FichaPacienteSpinnerValues.values_spinner_peakflow);
        spinnerPEAK.setOnItemSelectedListener(this);

        // spinner cat
        spinnerCAT = findViewById(R.id.spinnerCAT);
        setSpinnerContent(spinnerCAT, FichaPacienteSpinnerValues.values_spinner_cat);
        spinnerCAT.setOnItemSelectedListener(this);

        // spinner fichero logs
        spinnerLOGS = findViewById(R.id.spinnerlogs);
        setSpinnerContent(spinnerLOGS, FichaPacienteSpinnerValues.values_spinner_log);
        //endregion

        //endregion

        if (!isOnlineNet() || !isNetDisponible()){
            disableView();
            runOnUiThread(new Runnable() {
                public void run() {
                    circulodescarga.setVisibility(View.INVISIBLE);
                }
            });
//            txtConexion.setText("SIN CONEXIÓN A INTERNET");
            txtConexion.setVisibility(View.VISIBLE);
        }
        else {
            btn_ok.setEnabled(true);
            try {
                this.getRemoteData();
                EVLogConfig.log(TAG,"GuardarDatosInicialesDB(getUIData())");
                Log.e(TAG,"onCreate() >> GuardarDatosInicialesDB(getUIData())");
                this.GuardarDatosInicialesDB(this.getUIData());
            }
            catch (IOException e) {
                Log.e(TAG,"IOException");
                e.printStackTrace();
            }
            catch (ApiException e) {
                EVLogConfig.log(TAG,"**************** ApiException: " + e.getMessage());
                Toast toast1 = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
                toast1.show();
                e.printStackTrace();
            }
            catch (JSONException e) {
                Log.e(TAG,"JSONException");
                e.printStackTrace();
            }
        }

    }

    //region :: ONCLICK()
    @Override
    public void onClick(View view) {
        Log.e(TAG, " onClick()");
        String[] device;
        String model;

        int viewId = view.getId();
        if (viewId == R.id.activity_fichapaciente_btn_salirso) {
            //region >> Button Android >> Salir a SO
            EVLogConfig.log(TAG, "WIRELESS SETTINGS >> onclick()");
            btn_salirso.setEnabled(false);
            btn_salir.setEnabled(false);
            btn_ok.setEnabled(false);
            btn_wireless = true;
//                Intent openWirelessSettings = new Intent("android.settings.WIRELESS_SETTINGS");
//                Intent openWirelessSettings = new Intent(android.provider.Settings.ACTION_SETTINGS);
//                startActivity(openWirelessSettings);

            Intent intentlogin = new Intent(DispositivosPaciente.this, LoginSetting.class);
            startActivity(intentlogin);
            finish();

            //endregion
        }
        else if (viewId == R.id.imagePaciente) {
            //region >> Foto Paciente
            EVLogConfig.log(TAG, "FOTO PACIENTE >> onclick()");

        Alerts.mostrarAlertFoto(this, "No se han guardado los cambios antes de acceder a la cámara,\n ¿Que desea hacer?",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == DialogInterface.BUTTON_POSITIVE) {
                            try {
                                btn_salirso.setEnabled(false);
                                btn_salir.setEnabled(false);
                                btn_ok.setEnabled(false);
                                btn_wireless = true;

                                EVLogConfig.log(TAG, "onClick() >> GUARDAR");
                                BtnOk();

                                while (!btn_aplicar) {
                                    SystemClock.sleep(500);
                                }

                                setFilesBackup();
                                Intent intentFoto = new Intent(DispositivosPaciente.this, CameraFoto.class);
                                intentFoto.putExtra("key", "Ficha");
                                intentFoto.putExtra("numeropaciente", idpaciente);
                                startActivity(intentFoto);

                                finish();

                            } finally {
                                dialogInterface.dismiss();
                            }
                        } else if (i == DialogInterface.BUTTON_NEGATIVE) {
                            try {
                                EVLogConfig.log(TAG, "onClick() >> CANCELAR");
                            } finally {
                                dialogInterface.dismiss();
                            }
                        }
                    }
                }
        );

        //endregion
        }
        else if (viewId == R.id.activity_fichapaciente_btn_salir) {
            //region >> Button Volver/Salir
            EVLogConfig.log(TAG, "SALIR >> onclick()");
            if (!btn_aplicar) {
                BtnAccept();
            } else {
                Log.e(TAG, "********** salir >> setFilesBackup()");
                setFilesBackup();
                finish();
            }
            //endregion
        }
        else if (viewId == R.id.pacienteQR) {
            //region >> Boton cambiar paciente
            EVLogConfig.log(TAG, "CAMBIAR PACIENTE >> onclick()");

            qrreader = true; // indicamos que vamos a escanear nuevo QR

            Intent i = new Intent(DispositivosPaciente.this, QRReader.class);
            i.putExtra("key", "Ficha");
            i.putExtra("id", idpaciente);
            startActivity(i);
            finish();

            //endregion
        }
        else if (viewId == R.id.activity_fichapaciente_btn_ok) {
            //region >> Button OK/Guardar
            EVLogConfig.log(TAG, "GUARDAR >> onclick()");
            btn_aplicar = true;
            BtnOk();
            //endregion
        }
        else if (viewId == R.id.pacienteCaracteristicas) {
            //region >> CARACTERISTICAS PACIENTE
            config = true;
            Intent intent = new Intent(DispositivosPaciente.this, CaracteristicasPaciente.class);
            intent.putExtra("idpaciente", idpaciente);
            startActivity(intent);
            //endregion
        }
        else if (viewId == R.id.pacienteRemove) {
            //region >> ELIMINAR PACIENTE ASIGNADO
            Log.e(TAG, "ELIMINAR PACIENTE ASIGNADO >> onclick()");

            Alerts.mostrarAlertRemove(this, "¿Seguro que quiere eliminar el paciente actual?",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (i == DialogInterface.BUTTON_POSITIVE) {
                                try {
                                    Log.e(TAG, "PUSH SI");
                                    deleteAllLogAndJsonFiles();
                                    deleteAllLogAndJsonFiles("ihealth_sdk");
                                    deleteAllLogAndJsonFiles("_subido");
                                    deleteFilesBackup();
                                    finish();
                                } finally {
                                    dialogInterface.dismiss();
                                }
                            } else if (i == DialogInterface.BUTTON_NEGATIVE) {
                                Log.e(TAG, "PUSH NO");
                                dialogInterface.dismiss();
                            }
                        }
                    }
            );
            //endregion
        }
        else if (viewId == R.id.multipaciente) {
            //region :: Multi Paciente
            Log.e(TAG, "ulti Paciente >> onclick()");
            selectMultiPaciente(multipaciente);
            //endregion
        }

        else if (viewId == R.id.btnScanOximetro) {
            //region >> Lupa Scan OXI
            EVLogConfig.log(TAG, "onclick() >> Scanner Oximetros");
            typeScan = "oxi";
            scanDevices();
            btn_aplicar = false;
            //endregion
        }
        else if (viewId == R.id.btnScanTensiometro) {
            //region >> Lupa Scan TEN
            EVLogConfig.log(TAG, "onclick() >> Scanner Tensiometros");
            typeScan = "ten";
            scanDevices();
            btn_aplicar = false;
            //endregion
        }
        else if (viewId == R.id.btnScanActividad) {
            //region >> Lupa Scan ACT
            EVLogConfig.log(TAG, "onclick() >> Scanner Pulseras Actividad");
            typeScan = "act";
            scanDevices();
            btn_aplicar = false;
            //endregion
        }
        else if (viewId == R.id.btnScanTermometro) {
            //region >> Lupa Scan TERMO
            EVLogConfig.log(TAG, "onclick() >> Scanner Termómetros");
            typeScan = "thermo";
            scanDevices();
            btn_aplicar = false;
            //endregion
        }
        else if (viewId == R.id.btnScanScale) {
            //region >> Lupa Scan SCALE
            EVLogConfig.log(TAG, "onclick() >> Scanner Básculas");
            typeScan = "bas";
            scanDevices();
            btn_aplicar = false;
            //endregion
        }
        else if (viewId == R.id.btnScanMonitorPulmonar) {
            //region >> Lupa Scan MONPUL
            EVLogConfig.log(TAG, "onclick() >> Scanner Monitor Pulmonar");
            typeScan = "monpul";
            scanDevices();
            btn_aplicar = false;
            //endregion
        }
        else if (viewId == R.id.btnScanECG) {
            //region >> Lupa Scan ECG
            EVLogConfig.log(TAG, "onclick() >> Scanner ECG");
            typeScan = "ecg";
            scanDevices();
            btn_aplicar = false;
            //endregion
        }

        else if (viewId == R.id.btnVincularOxi) {
            //region >> PULSIOXIMETRO
            device = spinnerOXI.getSelectedItem().toString().split("-");
            mDeviceAddress = util.MontarMAC(device[1]);

            EVLogConfig.log(TAG, "PULSIOXIMETRO mDeviceAddress: " + mDeviceAddress);
            retries = 0;
            pairingModel = Pairing.None;
            disableView();
            visibleView(txtCuentaAtras, false);

            mBeurerControl = new BeurerControl();
            mBeurerControl.initialize(this, mDeviceAddress);
            //endregion
        }
        else if (viewId == R.id.btnVincularTen) {
            //region >> TENSIOMETRO
            device = spinnerTEN.getSelectedItem().toString().split("-");
            mDeviceAddress = util.MontarMAC(device[1]);

            EVLogConfig.log(TAG, "TENSIOMETRO mDeviceAddress: " + mDeviceAddress);
            retries = 0;
            pairingModel = Pairing.None;
            disableView();
            visibleView(txtCuentaAtras, false);

            mBeurerControl = new BeurerControl();
            mBeurerControl.initialize(this, mDeviceAddress);
            //endregion
        }
        else if (viewId == R.id.btnVincularAct) {
            //region >> ACTIVIDAD
            device = spinnerACT.getSelectedItem().toString().split("-");
            mDeviceAddress = util.MontarMAC(device[1]);

            EVLogConfig.log(TAG, " ACTIVIDADmDeviceAddress: " + mDeviceAddress);
            retries = 0;
            disableView();
            visibleView(txtCuentaAtras, false);

            mBeurerControl = new BeurerControl();
            mBeurerControl.initialize(this, mDeviceAddress);
            //endregion
        }
        else if (viewId == R.id.btnVincularTermo) {
            //region >> TERMOMETRO
            device = spinnerTER.getSelectedItem().toString().split("-");
            mDeviceAddress = util.MontarMAC(device[1]);

            EVLogConfig.log(TAG, "TERMOMETRO mDeviceAddress: " + mDeviceAddress);
            retries = 0;
            pairingModel = Pairing.None;
            disableView();
            visibleView(txtCuentaAtras, false);

            mBeurerControl = new BeurerControl();
            mBeurerControl.initialize(this, mDeviceAddress);
            //endregion
        }
        else if (viewId == R.id.btnVincularScale) {
            //region >> SCALE
            device = spinnerBAS.getSelectedItem().toString().split("-");
            mDeviceAddress = util.MontarMAC(device[1]);

            EVLogConfig.log(TAG, "BÁSCULA mDeviceAddress: " + mDeviceAddress);
            retries = 0;
            disableView();
            visibleView(txtCuentaAtras, false);

            if (device[0].equals("BF600")) {
                pairingModel = Pairing.bf600;
            }

            mBeurerControl = new BeurerControl();
            mBeurerControl.initialize(this, mDeviceAddress);
            //endregion
        }

        else if (viewId == R.id.btnConfigOxi) {
            //region >> CONFIG OXIMETROS
            device = spinnerOXI.getSelectedItem().toString().split("-");
            mDeviceAddress = util.MontarMAC(device[1]);
            EVLogConfig.log(TAG, "mDeviceAddress: " + mDeviceAddress);
//                retries = 0;
//                disableView();
//                mBeurerControl = new BeurerControl();
//                mBeurerControl.initialize(this,mDeviceAddress);
            //endregion
        }
        else if (viewId == R.id.btnConfigTen) {
            //region >> CONFIG TENSIOMETROS
            device = spinnerTEN.getSelectedItem().toString().split("-");
            mDeviceAddress = util.MontarMAC(device[1]);
            EVLogConfig.log(TAG, "mDeviceAddress: " + mDeviceAddress);
//                retries = 0;
//                disableView();
//                mBeurerControl = new BeurerControl();
//                mBeurerControl.initialize(this,mDeviceAddress);
            //endregion
        }
        else if (viewId == R.id.btnConfigAct) {
            //region >> CONFIG ACTIVIDAD
            device = spinnerACT.getSelectedItem().toString().split("-");
            mDeviceAddress = util.MontarMAC(device[1]);
            model = device[0];
            EVLogConfig.log(TAG, "" + model + " [" + mDeviceAddress + "]");
            if (model.contains("MAMBO6")) {
                disableView();
                visibleView(txtCuentaAtras, false);
                unregisterReceiver(mCallbackBeurer);

                Intent intent = new Intent(DispositivosPaciente.this, config_Mambo6.class);
                intent.putExtra("deviceMac", mDeviceAddress);
                startActivity(intent);
                config = true;
            } else {
                setTextSatus("Dispositivo sin configuración.");
            }
            //endregion
        }
        else if (viewId == R.id.btnConfigScale) {
            //region >> CONFIG SCALE
            device = spinnerBAS.getSelectedItem().toString().split("-");
            mDeviceAddress = util.MontarMAC(device[1]);
            EVLogConfig.log(TAG, "mDeviceAddress: " + mDeviceAddress);
            Log.e(TAG, "mDeviceAddress: " + mDeviceAddress);

            // Comprueba si el modelo del dispositivo es de Beurer
            String modelo = device[0];
            if (BeurerReferences.deviceBAS_CNF.contains(modelo)) {
                config = true;
                if (modelo.equals("BF600")) {
                    unregisterReceiver(mCallbackBeurer);
                    Intent intent = new Intent(DispositivosPaciente.this, config_BF600.class);
                    intent.putExtra("deviceMac", util.MontarMAC(device[1]));
                    intent.putExtra("idpaciente", idpaciente);
                    startActivity(intent);
                }
            } else {
                if (modelo.equals("HS2S")) {
                    config = true;
                    Intent intent = new Intent(DispositivosPaciente.this, config_HS2S.class);

                    EquipoPaciente equipo = EQUIPOS_PAC.get(NombresDispositivo.BASCULA);
                    try {
                        JSONObject jextras = new JSONObject(equipo.getExtra());

                        if (jextras.has("hs2s") == false) {
                            JSONObject hs2s = new JSONObject();
                            hs2s.put("impedance", false);
                            jextras.put("hs2s", hs2s);
                        }

                        if (jextras.has("dayOfweek") == false) {
                            JSONArray jdayofweek = new JSONArray();
                            jdayofweek.put("Mon");
                            jdayofweek.put("Tue");
                            jdayofweek.put("Wed");
                            jdayofweek.put("Thu");
                            jdayofweek.put("Fri");
                            jdayofweek.put("Sat");
                            jdayofweek.put("Sun");
                            jextras.put("dayOfweek", jdayofweek);
                        }

                        intent.putExtra("deviceMac", device[1]);
                        intent.putExtra("idpaciente", idpaciente);
                        intent.putExtra("hs2s", jextras.toString());
                        startActivity(intent);
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Error al cargar el dispositivo.", Toast.LENGTH_LONG).show();
                    }

                }
            }

            //endregion
        }
        else if (viewId == R.id.btnConfigMonitorPulmonar) {
            //region >> CONFIG MONITOR PULMONAR
            device = spinnerMONPUL.getSelectedItem().toString().split("-");
            mDeviceAddress = util.MontarMAC(device[1]);
            EVLogConfig.log(TAG, "mDeviceAddress: " + mDeviceAddress);
            Log.e(TAG, "mDeviceAddress: " + mDeviceAddress);

            // Comprueba si el modelo del dispositivo es de Beurer
            String modelo = device[0];
            if (BeurerReferences.deviceMONPUL_CNF.contains(modelo)) {
//                    retries = 0;
//                    disableView();
//                    mBeurerControl = new BeurerControl();
//                    mBeurerControl.initialize(this,mDeviceAddress);
            } else {
                if (modelo.equals("LUNG")) {
                    config = true;
                    Intent intent = new Intent(DispositivosPaciente.this, config_LUNG.class);

                    EquipoPaciente equipo = EQUIPOS_PAC.get(NombresDispositivo.MONITORPULMONAR);
                    try {
                        JSONObject jextras = new JSONObject(equipo.getExtra());

                        if (jextras.has("lung") == false) {
                            JSONObject lung = new JSONObject();
                            lung.put("FEV1PB", 300);
                            lung.put("GreenZone", 50);  // 80
                            lung.put("YellowZone", 35); // 50
                            lung.put("OrangeZone", 20); // 30
                            jextras.put("lung", lung);
                        }

                        if (jextras.has("dayOfweek") == false) {
                            JSONArray jdayofweek = new JSONArray();
                            jdayofweek.put("Mon");
                            jdayofweek.put("Tue");
                            jdayofweek.put("Wed");
                            jdayofweek.put("Thu");
                            jdayofweek.put("Fri");
                            jdayofweek.put("Sat");
                            jdayofweek.put("Sun");
                            jextras.put("dayOfweek", jdayofweek);
                        }

                        String dispositivo = spinnerMONPUL.getSelectedItem().toString();
                        String[] tmp  = dispositivo.split("-");
                        String mac = util.MontarMAC(tmp[1]);

                        intent.putExtra("idpaciente", idpaciente);
                        intent.putExtra("mac", mac);
                        intent.putExtra("lung", jextras.toString());
                        startActivity(intent);
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Error al cargar el dispositivo.", Toast.LENGTH_LONG).show();
                    }
                }
            }
            //endregion
        }
        else if (viewId == R.id.btnConfigECG) {
            //region >> CONFIG ECG
            device = spinnerECG.getSelectedItem().toString().split("-");
            mDeviceAddress = util.MontarMAC(device[1]);
            EVLogConfig.log(TAG, "mDeviceAddress: " + mDeviceAddress);
            Log.e(TAG, "mDeviceAddress: " + mDeviceAddress);

            // Comprueba si el modelo del dispositivo es de Beurer
            String modelo = device[0];
            if (modelo.equals("K6L")) {
                config = true;
                Intent intent = new Intent(DispositivosPaciente.this, config_Kardia6L.class);

                EquipoPaciente equipo = EQUIPOS_PAC.get(NombresDispositivo.ECG);
                try {
                    JSONObject jextras = new JSONObject(equipo.getExtra());

                    if (jextras.has("k6l") == false) {
                        JSONObject params = new JSONObject();
                        params.put("enableLeadsButtons", true);
                        params.put("leadConfiguration", "SINGLE");
                        params.put("filterType", "ENHANCED");
                        params.put("maxDuration", 30);
                        params.put("resetDuration", 10);
                        params.put("mainsFrequency", 50);
                        jextras.put("k6l", params);
                    }

                    if (jextras.has("dayOfweek") == false) {
                        JSONArray jdayofweek = new JSONArray();
                        jdayofweek.put("Mon");
                        jdayofweek.put("Tue");
                        jdayofweek.put("Wed");
                        jdayofweek.put("Thu");
                        jdayofweek.put("Fri");
                        jdayofweek.put("Sat");
                        jdayofweek.put("Sun");
                        jextras.put("dayOfweek", jdayofweek);
                    }

                    String dispositivo = spinnerECG.getSelectedItem().toString();
                    String[] tmp  = dispositivo.split("-");
                    String mac = util.MontarMAC(tmp[1]);

                    intent.putExtra("idpaciente", idpaciente);
                    intent.putExtra("mac", mac);
                    intent.putExtra("k6l", jextras.toString());
                    startActivity(intent);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Error al cargar el dispositivo.", Toast.LENGTH_LONG).show();
                }
            }
            //endregion
        }

        else if (viewId == R.id.btnCalendarOximetro) {
            //region >> CALENDARIO OXIMETROS
            selectDayOfweek(NombresDispositivo.OXIMETRO);
            //endregion
        }
        else if (viewId == R.id.btnCalendarTensiometro) {
            //region >> CALENDARIO TENSIOMETRO
//            mostrarEquipos();
            selectDayOfweek(NombresDispositivo.TENSIOMETRO);
            //endregion
        }
        else if (viewId == R.id.btnCalendarActividad) {
            //region >> CALENDARIO OXIMETROS
            selectDayOfweek(NombresDispositivo.ACTIVIDAD);
            //endregion
        }
        else if (viewId == R.id.btnCalendarTermometro) {
            //region >> CALENDARIO OXIMETROS
            selectDayOfweek(NombresDispositivo.TERMOMETRO);
            //endregion
        }
        else if (viewId == R.id.btnCalendarPEAK) {
            //region >> CALENDARIO OXIMETROS
            selectDayOfweek(NombresDispositivo.PEAKFLOW);
            //endregion
        }
        else if (viewId == R.id.btnCalendarENC) {
            //region >> CALENDARIO OXIMETROS
            selectDayOfweek(NombresDispositivo.ENCUESTA);
            //endregion
        }
        else if (viewId == R.id.btnCalendarCAT) {
            //region >> CALENDARIO OXIMETROS
            selectDayOfweek(NombresDispositivo.CAT);
            //endregion
        }
        else if (viewId == R.id.btnCalendarScale) {
            //region >> CALENDARIO OXIMETROS
            selectDayOfweek(NombresDispositivo.BASCULA);
            //endregion
        }
        else if (viewId == R.id.btnCalendarMonitorPulmonar) {
            //region >> CALENDARIO OXIMETROS
            selectDayOfweek(NombresDispositivo.MONITORPULMONAR);
            //endregion
        }
        else if (viewId == R.id.btnCalendarECG) {
            //region >> CALENDARIO ECG
            selectDayOfweek(NombresDispositivo.ECG);
            //endregion
        }
    }

    private void BtnOk() {
        EVLogConfig.log(TAG,"BtnOk()");
        btn_salirso.setEnabled(false);
        btn_salir.setEnabled(false);
        btn_ok.setEnabled(false);
        if (isNetDisponible() || isOnlineNet()) {
            this.GuardarDatosInicialesDB(this.getUIData());
        }
        else {
            Alerts.mostrarAlert(this, "No hay conexión a internet.");
        }
        btn_salirso.setEnabled(true);
        btn_salir.setEnabled(true);
        btn_ok.setEnabled(true);
    }

    private void BtnAccept() {
        EVLogConfig.log(TAG,"BtnAccept()");
        Alerts.mostrarAlert(this, "No se han guardado los cambios,\n ¿Que desea hacer?",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.e (TAG,"mostrarAlert i:" + i);
                        if (i == DialogInterface.BUTTON_POSITIVE) {
                            try {
                                EVLogConfig.log(TAG, "onClick() >> GUARDAR Y SALIR");
                                BtnOk();

                                while (!btn_aplicar) {
                                    SystemClock.sleep(500);
                                }

                                setFilesBackup();
                                finish();

                            } finally {
                                dialogInterface.dismiss();
                            }
                        }
                        else if (i == DialogInterface.BUTTON_NEGATIVE){
                            try {
                                EVLogConfig.log(TAG, "onClick() >> SALIR SIN GUARDAR");
                                finish();
//                                startActivity(new Intent(DispositivosPaciente.this, Inicio.class));
                            } finally {
                                dialogInterface.dismiss();
                            }
                        }
                    }
                }
        );
    }

    //endregion

    private void mostrarEquipos() {
        for (NombresDispositivo ndisp: NombresDispositivo.values()) {
            Log.e(""+ndisp.toString(),"" + EQUIPOS_PAC.get(ndisp).getDesc() + ", " + EQUIPOS_PAC.get(ndisp).getExtra());
        }
    }

    private void disableView() {
        btn_salirso.setEnabled(false);
        btn_salir.setEnabled(false);
        btn_ok.setEnabled(false);
        btn_cambiar.setEnabled(false);

        //region >> Deshabilitar tabla
        TableLayout tableLayout = (TableLayout) findViewById(R.id.tablaizq);
        tableLayout.setEnabled(false);

        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            View child = tableLayout.getChildAt(i);

            // Comprobar si el hijo es una TableRow
            if (child instanceof TableRow) {
                TableRow tableRow = (TableRow) child;

                // Recorrer los hijos de la TableRow y deshabilitarlos
                for (int j = 0; j < tableRow.getChildCount(); j++) {
                    View grandChild = tableRow.getChildAt(j);
                    grandChild.setEnabled(false);
                }
            } else {
                child.setEnabled(false);
            }
        }
        //endregion

        runOnUiThread(new Runnable() {
            public void run() {
                circulodescarga.setVisibility(View.VISIBLE);
                txtCuentaAtras.setVisibility(View.VISIBLE);
            }
        });
    }

    private void enableView() {
        btn_salirso.setEnabled(true);
        btn_salir.setEnabled(true);
        btn_ok.setEnabled(true);
        btn_cambiar.setEnabled(true);

        //region >> Habilitar tabla
        TableLayout tableLayout = (TableLayout) findViewById(R.id.tablaizq);
        tableLayout.setEnabled(true);

        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            View child = tableLayout.getChildAt(i);

            // Comprobar si el hijo es una TableRow
            if (child instanceof TableRow) {
                TableRow tableRow = (TableRow) child;

                // Recorrer los hijos de la TableRow y deshabilitarlos
                for (int j = 0; j < tableRow.getChildCount(); j++) {
                    View grandChild = tableRow.getChildAt(j);
                    grandChild.setEnabled(true);
                }
            } else {
                child.setEnabled(true);
            }
        }
        //endregion

        runOnUiThread(new Runnable() {
            public void run() {
                circulodescarga.setVisibility(View.INVISIBLE);
                txtCuentaAtras.setVisibility(View.INVISIBLE);
            }
        });
    }

    //region :: SEARCH BLUETOOTH
    public void scanDevices(){
        EVLogConfig.log(TAG, "scanDevices()");

        disableView();

        address.clear();
        macs_devices.clear();

        if (typeScan.equals("act")) { cuentaAtras = TIMEOUT_BLE * 2; } else { cuentaAtras = TIMEOUT_BLE; }
        setTextCuentaAtras(cuentaAtras.toString());

        searchDevicesBle();
    }
    @SuppressLint("MissingPermission")
    private void searchDevicesBle() {

        Log.e(TAG,"searchDevicesBle()");

        setTextSatus("Searching...");

        cTimerSearch = new CountDownTimer(1000 * TIMEOUT_BLE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                cuentaAtras -= 1;
                if (cuentaAtras < 0) { cuentaAtras = 0; }
                setTextCuentaAtras(cuentaAtras.toString());
            }

            @Override
            public void onFinish() {
                Log.e(TAG, "(1) cTimerSearch");
                cTimerSearch.cancel();
                cTimerSearch = null;
                bluetoothLeScanner.stopScan(scanCallback);
                scanning = false;

                Log.e(TAG, "(1) mac devives: " + macs_devices);

                EVLogConfig.log(TAG, "(1) Stop Discovery bluetooth LE");
                if (typeScan.equals("act")) { searchDevicesActivityTracker(); }
                else { uploadSpinnerDeviceScanBle(); }
            }
        };
        cTimerSearch.start();

        EVLogConfig.log(TAG,"(1) Start Scanner:" + typeScan) ;

        // * SCAN_MODE_LOW_POWER
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        /*
        SCAN_MODE_LOW_POWER: Este modo prioriza el ahorro de energía sobre la velocidad de escaneo. Es el mejor para las aplicaciones en segundo plano.
        SCAN_MODE_BALANCED: Este modo proporciona un equilibrio entre el ahorro de energía y la velocidad de escaneo. Es adecuado para la mayoría de las aplicaciones.
        SCAN_MODE_LOW_LATENCY: Este modo prioriza la velocidad de escaneo sobre el ahorro de energía. Escaneo continuo sin pausas (sin timeout)
                               Es útil para las aplicaciones que necesitan descubrir dispositivos cercanos en tiempo real.
        */
        scanning = true;
        bluetoothLeScanner.startScan(null, settings, scanCallback);
    }

    //region :: ScanCallback BLE
    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
//            Log.e("ScanCallback", "Dispositivo encontrado: " + device.getName());

            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress(); // MAC address
            deviceHardwareAddress = deviceHardwareAddress.replace(":","");

            if (deviceName != null) {
//                EVLogConfig.log(TAG, "type: " + typeScan +", device: " + deviceName + ", address: " + deviceHardwareAddress);

                if(deviceName.equals("Pulse Oximeter")) { deviceName = "PO3M"; }
                if(deviceName.contains("BP3L")) { deviceName = "BP3L"; }
                if(deviceName.contains("IF1")) { deviceName = "MAMBO6"; }
                if(deviceName.contains("NT13B")) { deviceName = "NT13B"; }
                if(deviceName.contains("FT95")) { deviceName = "FT95"; }
                if(deviceName.contains("HS2S")) { deviceName = "HS2S"; }
                if(deviceName.contains("BF600")) { deviceName = "BF600"; }
                if(deviceName.contains("GBS-2012-B")) { deviceName = "GBS2012B"; }
                if(deviceName.contains("LUNG")) { deviceName = "LUNG"; }
                if(deviceName.contains("Kardia6L")) { deviceName = "K6L"; }

                if (address.contains(deviceHardwareAddress) == true) {
//                        Log.e(TAG,"dispositivo en la lista de encontrados");
                    return;
                }
                EVLogConfig.log(TAG, "type: " + typeScan +", device: " + deviceName + ", address: " + deviceHardwareAddress);
                address.add(deviceHardwareAddress);

                String name = "" + deviceName + "-" + deviceHardwareAddress;  // EJ: "AM4-XXXXXXX"

                if (typeScan.equals("oxi")) {
                    // Buscamos Oximetros que sean válidos
                    if (ClassDispositivo.devices_OXI.contains(deviceName)){
                        macs_devices.add(name);
                        Log.e(TAG, "Add Oximetro: " + name);
                    }
                }
                else if (typeScan.equals("ten")) {
                    // Buscamos Tensiometros que sean válidos
                    if (ClassDispositivo.devices_TEN.contains(deviceName)){
                        macs_devices.add(name);
                        Log.e(TAG, "Add Tensiometro: " + name);
                    }
                }
                else if (typeScan.equals("act")) {
                    // Buscamos ACTIVIDAD que sean válidos
                    if (ClassDispositivo.devices_ACT.contains(deviceName)){
                        macs_devices.add(name);
                        Log.e(TAG, "Add Actividad: " + name);
                    }
                }
                else if (typeScan.equals("thermo")) {
                    // Buscamos TERMOMETROS que sean válidos
//                        Log.e(TAG,"typeScan.equals(thermo)");
                    if (ClassDispositivo.devices_TER.contains(deviceName)){
                        macs_devices.add(name);
                        Log.e(TAG, "Add Termómetro: " + name);
                    }
                }
                else if (typeScan.equals("bas")) {
                    // Buscamos Tensiometros que sean válidos
                    if (ClassDispositivo.devices_BAS.contains(deviceName)){
                        macs_devices.add(name);
                        Log.e(TAG, "Add Bascula: " + name);
                    }
                }
                else if (typeScan.equals("monpul")) {
                    // Buscamos Tensiometros que sean válidos
                    if (ClassDispositivo.devices_MONPUL.contains(deviceName)){
                        macs_devices.add(name);
                        Log.e(TAG, "Add Monitor Pulmonar: " + name);
                        Toast.makeText(getApplicationContext(), "DETECTADO DISPOSITIVO", Toast.LENGTH_LONG).show();
                        uploadSpinnerDeviceScanBle(); // CUANDO SE DETECTA EL PRIMERO PARA
                    }
                }
                else if (typeScan.equals("ecg")) {
                    // Buscamos Dispositivos que sean válidos
                    if (ClassDispositivo.devices_ECG.contains(deviceName)){
                        macs_devices.add(name);
                        Log.e(TAG, "Add ECG: " + name);
                        Toast.makeText(getApplicationContext(), "DETECTADO DISPOSITIVO", Toast.LENGTH_LONG).show();
                        uploadSpinnerDeviceScanBle(); // CUANDO SE DETECTA EL PRIMERO PARA
                    }
                }

                setTextSatus("Found [" + macs_devices.size() + "] devices.");
            }

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e("ScanCallback", "Error de escaneo: " + errorCode);
        }
    };
    //endregion

    @SuppressLint("MissingPermission")
    private void searchDevicesActivityTracker() {
        Log.e(TAG,"searchDevicesActivityTracker()");
        LSManagerStatus sdkStatus = LSBluetoothManager.getInstance().getManagerStatus();
        if(sdkStatus != LSManagerStatus.Free) {
            LSBluetoothManager.getInstance().stopSearch();
            LSBluetoothManager.getInstance().stopDeviceSync();
        }

        List<LSDeviceType> types = new ArrayList<LSDeviceType>();
        types.add(LSDeviceType.ActivityTracker);

        LSBluetoothManager.getInstance().clearScanCache();
        LSBluetoothManager.getInstance().searchDevice(types, mSearchCallback); // ACTIVA ESCANEO
        scanning = true;

        setTextCuentaAtras(cuentaAtras.toString());
        cTimerSearch = new CountDownTimer(1000 * TIMEOUT_BLE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                cuentaAtras -= 1;
                if (cuentaAtras < 0) { cuentaAtras = 0; }
                setTextCuentaAtras(cuentaAtras.toString());
            }

            @Override
            public void onFinish() {
                Log.e(TAG, "(2) cTimerSearch");
                cTimerSearch.cancel();
                cTimerSearch = null;
                LSBluetoothManager.getInstance().stopSearch(); // Detiene busqueda del dispositivo
                scanning = false;

                Log.e(TAG, "(2) mac devives: " + macs_devices);
                EVLogConfig.log(TAG, "(2) Stop Discovery bluetooth");
                uploadSpinnerDeviceScanBle();
            }
        };
        cTimerSearch.start();

        EVLogConfig.log(TAG,"(2) Start Scanner:" + typeScan) ;
    }

    @SuppressLint("MissingPermission")
    private void uploadSpinnerDeviceScanBle(){

        if (scanning) {
            scanning = false;
            try {
                bluetoothLeScanner.stopScan(scanCallback);
                LSBluetoothManager.getInstance().stopSearch();
            }
            catch (Exception e) { e.printStackTrace(); }
        }

        // CARGA SPINNER
        if (typeScan.equals("oxi")) {
            //region >> spinner oximetro
            if (!macs_devices.contains(FichaPacienteSpinnerValues.DESHABILITADO)) {
                macs_devices.add(FichaPacienteSpinnerValues.DESHABILITADO);
            }
            Log.e(TAG, "MACS OXI: " + macs_devices);

            List<String> mac_oxi = new ArrayList<>(macs_devices);
            setSpinnerContent(spinnerOXI, mac_oxi);
            //endregion
        }
        else if (typeScan.equals("ten")) {
            //region >>  spinner tensiometro
            if (!macs_devices.contains(FichaPacienteSpinnerValues.DESHABILITADO)) {
                macs_devices.add(FichaPacienteSpinnerValues.DESHABILITADO);
            }

            List<String> mac_ten = new ArrayList<>(macs_devices);
            setSpinnerContent(spinnerTEN, mac_ten);
            //endregion
        }
        else if (typeScan.equals("act")) {
            //region >> spinner actividad
            if (!macs_devices.contains(FichaPacienteSpinnerValues.DESHABILITADO)) {
                macs_devices.add(FichaPacienteSpinnerValues.DESHABILITADO);
            }

            List<String> mac_act = new ArrayList<>(macs_devices);
            setSpinnerContent(spinnerACT, mac_act);
            //endregion
        }
        else if (typeScan.equals("thermo")) {
            //region >> spinner thermometros
            List<String> macs_tmp = new ArrayList<>();
            macs_tmp.add(FichaPacienteSpinnerValues.DESHABILITADO);
            macs_tmp.add("Clasico");

            for (String ndevice: macs_devices) { macs_tmp.add(ndevice); }

            setSpinnerContent(spinnerTER, macs_tmp);

            //endregion
        }
        else if (typeScan.equals("bas")) {
            //region >>  spinner báscula
            if (!macs_devices.contains(FichaPacienteSpinnerValues.DESHABILITADO)) {
                macs_devices.add(FichaPacienteSpinnerValues.DESHABILITADO);
            }

            List<String> mac_bas = new ArrayList<>(macs_devices);
            mac_bas.add("Clasico");
            setSpinnerContent(spinnerBAS, mac_bas);
            //endregion
        }
        else if (typeScan.equals("monpul")) {
            //region >>  spinner monitor pulmonar
            List<String> mac_monpul = new ArrayList<>();
            mac_monpul.add(FichaPacienteSpinnerValues.DESHABILITADO);
            for (String ndevice: macs_devices) { mac_monpul.add(ndevice); }

//            if (!macs_devices.contains(FichaPacienteSpinnerValues.DESHABILITADO)) {
//                macs_devices.add(FichaPacienteSpinnerValues.DESHABILITADO);
//            }
//            List<String> mac_monpul = new ArrayList<>(macs_devices);
            setSpinnerContent(spinnerMONPUL, mac_monpul);
            //endregion
        }
        else if (typeScan.equals("ecg")) {
            //region >>  spinner ecg
            List<String> mac_ecg = new ArrayList<>();
            mac_ecg.add(FichaPacienteSpinnerValues.DESHABILITADO);
            for (String ndevice: macs_devices) { mac_ecg.add(ndevice); }

//            if (!macs_devices.contains(FichaPacienteSpinnerValues.DESHABILITADO)) {
//                macs_devices.add(FichaPacienteSpinnerValues.DESHABILITADO);
//            }
//            List<String> mac_ecg = new ArrayList<>(macs_devices);
            setSpinnerContent(spinnerECG, mac_ecg);
            //endregion
        }

        enableView();
    }

    //region CALLBACK SEARCH DEVICE TRANSKER
    private OnSearchingListener mSearchCallback=new OnSearchingListener() {

        // Devolver los resultados de la exploración
        @Override
        public void onSearchResults(LSDeviceInfo lsDevice) {

            if(lsDevice==null){
                return ;
            }

            String deviceName = lsDevice.getDeviceName();
            Log.e(TAG,"deviceName: " + deviceName);

            if(deviceName.contains("IF1")) { deviceName = "MAMBO6"; }

            String macDevice = lsDevice.getMacAddress().replace(":","");
            if (address.contains(macDevice) == true) {
//                        Log.e(TAG,"dispositivo en la lista de encontrados");
                return;
            }

            EVLogConfig.log(TAG, "type: " + typeScan +", device: " + deviceName + ", address: " + macDevice);
            address.add(macDevice);

            String name = "" + deviceName + "-" + macDevice;  // EJ: "AM4-XXXXXXX"

            if (typeScan.equals("act")) {
                // Buscamos ACTIVIDAD que sean válidos
                if (ClassDispositivo.devices_ACT.contains(deviceName)){
                    macs_devices.add(name);
                    Log.e(TAG, "Add Actividad: " + name);
                }
            }
            else {
                Log.e(TAG,"All onSearchResults");
            }
            setTextSatus("Found [" + macs_devices.size() + "] devices.");
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

            //update scan results
            //updateScanResults(lsDevice);
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
    //endregion

    //endregion

    //region :: DATOS
    private void getIdDispositivos() throws IOException, ApiException, JSONException {
        EVLogConfig.log(TAG,"getIdDispositivos()");

        // Petición al servidor del listado de dispositivos del sistema
        // {"devices":[{"id":1,"nombre":"Oximetro","params":"[{\"col\":\"bloodoxygen\",\"name\":\"Saturación de oxígeno\"},{\"col\":\"heartrate\",\"name\":\"Ritmo cardíaco\"}]","display":"Oxímetro"},{"id":2,"nombre":"Tensiometro","params":"[{\"col\":\"sys\",\"name\":\"Presión sistólica\"},{\"col\":\"dia\",\"name\":\"Presión diastólica\"},{\"col\":\"heartRate_ten\",\"name\":\"Ritmo cardíaco\"}]","display":"Tensiómetro"},{"id":3,"nombre":"Actividad Total","params":"[]","display":"Actividad Total"},{"id":4,"nombre":"Actividad Diaria","params":"[]","display":"Actividad Diaria"},{"id":5,"nombre":"Sueño","params":"[]","display":""},{"id":6,"nombre":"Viadhox FC","params":"[]","display":""},{"id":7,"nombre":"Viadhox AD","params":"[]","display":""},{"id":8,"nombre":"CPAP","params":"[]","display":""},{"id":9,"nombre":"Encuesta","params":"[]","display":""},{"id":10,"nombre":"Termometro","params":"[{\"col\":\"temperatura\",\"name\":\"Temperatura\"}]","display":"Termómetro"},{"id":11,"nombre":"PeakFlow","params":"[{\"col\":\"flujo_resp\",\"name\":\"Flujo espiratorio máximo\"}]","display":"PeakFlow"},{"id":12,"nombre":"CAT","params":"[]","display":""},{"id":13,"nombre":"ConcentradorO2","params":"[]","display":"Con. O2"},{"id":14,"nombre":"Pulsera AM5","params":"[]","display":" "},{"id":15,"nombre":"Bascula","params":"[]","display":"Bascula"}]}
        JSONObject params = new JSONObject();
        params.put("idpaciente", this.idpaciente);
//        JSONObject respuesta = ApiConnector.peticionJSONObject(ApiUrl.DISPOSITIVOS_GET, new JSONObject());
        JSONObject respuesta = ApiConnector.peticionJSONObject(ApiUrl.DISPOSITIVOS_GET, params);
        EVLogConfig.log(TAG,"getIdDispositivos(): requestAPI: " + respuesta.toString());

        JSONArray dispositivos = respuesta.getJSONArray("devices");  // ArrayJSON
        Map<NombresDispositivo, Dispositivo> map = new HashMap<>();
        Map<Integer, NombresDispositivo> id_disp_map = new HashMap<>();
        int length = dispositivos.length();

        EVLogConfig.log(TAG,"MAP_DISP_ID: ");

        for(int i = 0; i < length; i++) {
            JSONObject dispositivo = dispositivos.getJSONObject(i);
//            Log.e(TAG,"Dispositivo: " + dispositivo.toString());

            int id = dispositivo.getInt("id");
            String nombre = dispositivo.getString("nombre");
            NombresDispositivo cual = NombresDispositivo.fromName(nombre);

            if(cual != null) {
                map.put(cual, new Dispositivo(id));
                id_disp_map.put(id, cual);
                EVLogConfig.log(TAG,"id: " + id +", NombresDispositivo: " + cual);
            }
        }
        MAP_DISP_ID = map;
        MAP_ID_DISP = id_disp_map;

        EVLogConfig.log(TAG,"MAP_ID_DISP: " + MAP_ID_DISP.toString());
    }

    /***
     *
     * @return Id de la encuesta actual asignada al paciente
     * @throws IOException, ApiException, JSONException
     */
    private int getIdEncuesta() throws IOException, ApiException, JSONException {
        EVLogConfig.log(TAG,"getIdEncuesta()");
        JSONObject params = new JSONObject();
        params.put("idpaciente", this.idpaciente);
        JSONObject respuesta = ApiConnector.peticionJSONObject(ApiUrl.PACIENTE_GET_ID_ENCUESTA, params);
        Log.e(TAG,"Encuestas: " + respuesta);
        return respuesta.getInt("id_encuesta");
    }

    /***
     *
     * @return List<String> Id de escuestas existentes
     * @throws IOException, ApiException, JSONException
     */
    private List<String> getIdTodasEncuestas() throws IOException, ApiException, JSONException {
        Log.e(TAG,"getIdTodasEncuestas()");
        List<String> res = new ArrayList<>();

        JSONObject params = new JSONObject();
        params.put("idpaciente", this.idpaciente);
//        JSONObject respuesta = ApiConnector.peticionJSONObject(ApiUrl.ENCUESTAS_GET, new JSONObject());
        JSONObject respuesta = ApiConnector.peticionJSONObject(ApiUrl.ENCUESTAS_GET, params);

        Log.e(TAG,"IDs ENCUESTAS DISPONIBLES: " + respuesta);
        JSONArray encuestas = respuesta.getJSONArray("encuestas");
        int length = encuestas.length();
        for(int i = 0; i < length; i++) {
            JSONObject encuesta = encuestas.getJSONObject(i);
            res.add(encuesta.getInt("id_encuesta") + "");
        }
        res.add(FichaPacienteSpinnerValues.DESHABILITADO);
        return res;
    }

    private JSONArray getEncuesta() throws IOException, ApiException, JSONException {
        Log.e(TAG,"getEncuesta()");
        JSONObject params = new JSONObject();
        params.put("idpaciente", this.idpaciente);
        JSONObject respuesta = ApiConnector.peticionJSONObject(ApiUrl.PACIENTE_GET_ENCUESTA, params);
        Log.e(TAG,"GET_ENCUESTA: " + respuesta);
        return respuesta.getJSONArray("preguntas");
    }

    private Map<NombresDispositivo, EquipoPaciente> getEquiposPaciente() throws JSONException, ApiException, IOException {
        EVLogConfig.log(TAG,"getEquiposPaciente()");

        Map<NombresDispositivo, EquipoPaciente> equipos_paciente = new HashMap<>();

        JSONObject params = new JSONObject();
        params.put("idpaciente", this.idpaciente);
        JSONArray dispositivos = ApiConnector.peticionJSONArray(ApiUrl.PACIENTE_EQUIPO_GET, params);
        int length = dispositivos.length();
        EVLogConfig.log(TAG,"DispositivosPaciente: " + dispositivos.toString());

        for(int i = 0; i < length; i++) {
            JSONObject dispositivo = dispositivos.getJSONObject(i);
            int id = dispositivo.getInt("id_device");
            boolean enable = dispositivo.getInt("enable") == 1;
            String mac = dispositivo.getString("desc");
            String extra = dispositivo.getString("extra");

            if (extra.equals("{}") && enable == true) {
                extra ="{ \"dayOfweek\":[\"Mon\", \"Tue\", \"Wed\", \"Thu\", \"Fri\", \"Sat\", \"Sun\"] }";
            }

            NombresDispositivo cual = MAP_ID_DISP.get(id);
            EVLogConfig.log(TAG,"id: " + id + ", cual: " + cual);

            if(cual != null) {
                EVLogConfig.log(TAG, ">> Equipo: " + cual+  " >> id_device: " + id + ", enable: " + enable + ", desc: " + mac + ", extra: " + extra);
                equipos_paciente.put(cual, new EquipoPaciente(new Dispositivo(id), enable, mac, extra));
            }
        }

        EVLogConfig.log(TAG,"DispositivosPaciente: RETURN");
        return equipos_paciente;
    }

    private void getRemoteData() throws IOException, ApiException, JSONException {
        Log.e(TAG,"getRemoteData()");
        this.getIdDispositivos();
        int id_encuesta = getIdEncuesta();
        List<String> ids_enc = getIdTodasEncuestas();

//        Map<NombresDispositivo, EquipoPaciente> equipos_paciente = getEquiposPaciente();
        EQUIPOS_PAC = getEquiposPaciente();

        int logs = leerConfigLogs();

        // Carga spinners con los datos actuales del paciente
//        this.setDisplay(equipos_paciente, id_encuesta, ids_enc, logs);
        this.setDisplay(EQUIPOS_PAC, id_encuesta, ids_enc, logs);
    }

    private int leerConfigLogs() {
        Log.e(TAG,"leerConfigLogs()");
        int res = 2;
        try {
            JSONObject json_log = FileAccess.leerJSON(FilePath.CONFIG_LOG);
            String tipo_log = json_log.getString("log");
            if(tipo_log.equals("no_log")) {
                res = 0;
            } else if(tipo_log.equals("file_log")) {
                res = 1;
            }
        } catch (IOException | JSONException e) {}
        return res;
    }

    //#-- update datos --
    // falta update hora ensayo
    private void UpdateDevice(int id_dispositivo, int enable, String mac, String extra) throws IOException, ApiException, JSONException {
//        EVLogConfig.log(TAG,"UpdateDevice()");
        EVLogConfig.log(TAG,"UpdateDevice() >> getId(): " + id_dispositivo + ", isEnabled(): " + enable + ", getDesc(): " + mac + ", getExtra(): " + extra);

        JSONObject params = new JSONObject();
        params.put("idpaciente", idpaciente);
        params.put("id_device", id_dispositivo);
        params.put("enable", enable);
        params.put("desc", mac);
        params.put("extra", extra);
        EVLogConfig.log("PARAMS", params.toString());
        ApiConnector.peticion(ApiUrl.PACIENTE_EQUIPO_UPDATE, params);
    }

    private void UpdateEncuestaPaciente(int id_encuesta) throws IOException, ApiException, JSONException  {
        Log.e(TAG,"UpdateEncuestaPaciente()");
        JSONObject params = new JSONObject();
        params.put("idpaciente", idpaciente);
        params.put("id_encuesta", id_encuesta);
        ApiConnector.peticion(ApiUrl.PACIENTE_UPDATE_ENCUESTA, params);
    }
    //#--------------

    private String nameSpinner(Spinner spinner){
        String name = "";

        int viewId = spinner.getId();
        if (viewId == R.id.spnOXI) {
            name = "spinnerOXI";
        }
        else if (viewId == R.id.spnTEN) {
            name = "spinnerTEN";
        }
        else if (viewId == R.id.spnACT) {
            name = "spinnerACT";
        }
        else if (viewId == R.id.spinnerTER) {
            name = "spinnerTER";
        }
        else if (viewId == R.id.spinnerBAS) {
            name = "spinnerBAS";
        }
        else if (viewId == R.id.spinnerMONITORPULMONAR) {
            name = "spinnerMONPUL";
        }
        else if (viewId == R.id.spinnerECG) {
            name = "spinnerECG";
        }
        else if (viewId == R.id.spinnerPEAK) {
            name = "spinnerPEAK";
        }
        else if (viewId == R.id.spinnerENC) {
            name = "spinnerENC";
        }
        else if (viewId == R.id.spinnerCAT) {
            name = "spinnerCAT";
        }
        else if (viewId == R.id.spinnerlogs) {
            name = "spinnerlogs";
        }
        else {
            name = "otro";
        }
        return name;
    }

    // le pasas el spinner que quieras y una lista con las opciones
    private void setSpinnerContent(Spinner spinner, List<String> list) {
        Log.e(TAG,"setSpinnerContent(" + nameSpinner(spinner) + "): " + list);

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.partial_spinners, R.id.partial_spinners_text, list);
        spinnerArrayAdapter.setDropDownViewResource(R.layout.partial_spinners);

        // Limpiar cualquier contenido existente en el Spinner
        spinner.setAdapter(null);

        spinner.setAdapter(spinnerArrayAdapter);

        // Selecciona el último elemento del spinner
        spinner.setSelection(list.size() - 1);
    }

    // le pasas un spinner y el equipo que quieres.
    // mira si el equipo esta habilitado para poner la mac, o solo pone deshabilirado si no.
    private void setSpinnerDefaultContent(Spinner spinner, EquipoPaciente equipo) {
        Log.e(TAG,"setSpinnerDefaultContent()");
        List<String> list = new ArrayList<>();
        if(equipo.isEnabled()) {
            if (equipo.getDesc().contains(FichaPacienteSpinnerValues.DESHABILITADO) == false) {
                list.add(equipo.getDesc());
            }
        }
        list.add(FichaPacienteSpinnerValues.DESHABILITADO);
        setSpinnerContent(spinner, list);
        spinner.setSelection(0);
    }

    private EquipoPaciente getDeviceUIData(Dispositivo disp, Spinner spinner) {
        Log.e(TAG,"************** getDeviceUIData(" + nameSpinner(spinner)+")");

        try {
            String desc = spinner.getSelectedItem().toString();
            boolean enable = (desc.equals(FichaPacienteSpinnerValues.DESHABILITADO)) ? false : true;

            String nameSpinner = nameSpinner(spinner);
            Log.e(TAG, ">> device: " + desc + ", nameSpinner: " + nameSpinner + ", enable: " + enable);
            NombresDispositivo ndisp = null;

            //region :: EQUIPOS
            if (nameSpinner.equals("spinnerOXI")) {
                ndisp = NombresDispositivo.OXIMETRO;
            }
            else if (nameSpinner.equals("spinnerTEN")) {
                ndisp = NombresDispositivo.TENSIOMETRO;
            }
            else if (nameSpinner.equals("spinnerBAS")) {
                ndisp = NombresDispositivo.BASCULA;
            }
            else if (nameSpinner.equals("spinnerACT")) {
                ndisp = NombresDispositivo.ACTIVIDAD;
            }
            else if (nameSpinner.equals("spinnerTER")) {
                ndisp = NombresDispositivo.TERMOMETRO;
            }
            else if (nameSpinner.equals("spinnerMONPUL")) {
                ndisp = NombresDispositivo.MONITORPULMONAR;
            }
            else if (nameSpinner.equals("spinnerECG")) {
                ndisp = NombresDispositivo.ECG;
            }
            else if (nameSpinner.equals("spinnerPEAK")) {
                ndisp = NombresDispositivo.PEAKFLOW;
            }
            else if (nameSpinner.equals("spinnerENC")) {
                ndisp = NombresDispositivo.ENCUESTA;
            }
            else if (nameSpinner.equals("spinnerCAT")) {
                ndisp = NombresDispositivo.CAT;
            }
            //endregion

            String extra = "{}";

            if (ndisp != null) {
                try {
//                    Log.e(TAG,"EQUIPOS_PAC: " + EQUIPOS_PAC);
                    EquipoPaciente equipo = EQUIPOS_PAC.get(ndisp);
//                    Log.e(TAG, "equipo: " + equipo);

                    if (equipo == null) {
                        equipo = new EquipoPaciente(disp, enable, desc, extra);
                        EQUIPOS_PAC.put(ndisp, equipo);
                    }

                    Log.e(TAG, "id equipo: " + equipo.getDispositivo().getId());

                    if (enable) {
                        extra = equipo.getExtra();

                        // Realizar comprobación de la configuración del monitor pulmonar si está habilitado y no configurado
                        if (ndisp == NombresDispositivo.MONITORPULMONAR) {
                            Log.e(TAG, "ndisp == NombresDispositivo.MONITORPULMONAR: " + extra);
                            String[] device = desc.split("-");
                            String modelo = device[0];
                            Log.e(TAG, "Model device? " + modelo);

                            if (modelo.equals("LUNG")) {
                                JSONObject jextras = new JSONObject(extra);

                                if (jextras.has("lung") == false) {
                                    JSONObject lung = new JSONObject();
                                    lung.put("FEV1PB", 300);
                                    lung.put("GreenZone", 50);  // 80
                                    lung.put("YellowZone", 35); // 50
                                    lung.put("OrangeZone", 20); // 30
                                    jextras.put("lung", lung);
                                }

                                if (jextras.has("dayOfweek") == false) {
                                    JSONArray jdayofweek = new JSONArray();
                                    jdayofweek.put("Mon");
                                    jdayofweek.put("Tue");
                                    jdayofweek.put("Wed");
                                    jdayofweek.put("Thu");
                                    jdayofweek.put("Fri");
                                    jdayofweek.put("Sat");
                                    jdayofweek.put("Sun");
                                    jextras.put("dayOfweek", jdayofweek);
                                }

                                extra = jextras.toString();
                            }
                        }

                        // Realizar comprobación de la configuración de la báscula si está habilitado y no configurado
                        if (ndisp == NombresDispositivo.BASCULA) {
                            Log.e(TAG, "ndisp == NombresDispositivo.BASCULA: " + extra);
                            String[] device = desc.split("-");
                            String modelo = device[0];
                            Log.e(TAG, "Model device? " + modelo);

                            if (modelo.equals("HS2S")) {
                                JSONObject jextras = new JSONObject(extra);

                                if (jextras.has("hs2s") == false) {
                                    JSONObject hs2s = new JSONObject();
                                    hs2s.put("impedance", false);
                                    jextras.put("hs2s", hs2s);
                                }

                                if (jextras.has("dayOfweek") == false) {
                                    JSONArray jdayofweek = new JSONArray();
                                    jdayofweek.put("Mon");
                                    jdayofweek.put("Tue");
                                    jdayofweek.put("Wed");
                                    jdayofweek.put("Thu");
                                    jdayofweek.put("Fri");
                                    jdayofweek.put("Sat");
                                    jdayofweek.put("Sun");
                                    jextras.put("dayOfweek", jdayofweek);
                                }

                                extra = jextras.toString();
                            }
                        }

                        if (ndisp == NombresDispositivo.ECG) {
                            Log.e(TAG, "ndisp == NombresDispositivo.ECG: " + extra);
                            String[] device = desc.split("-");
                            String modelo = device[0];
                            Log.e(TAG, "Model device? " + modelo);

                            if (modelo.equals("K6L")) {
                                JSONObject jextras = new JSONObject(extra);

                                if (jextras.has("k6l") == false) {
                                    JSONObject params = new JSONObject();
                                    params.put("enableLeadsButtons", true);
                                    params.put("leadConfiguration", "SINGLE");
                                    params.put("filterType", "ENHANCED");
                                    params.put("maxDuration", 30);
                                    params.put("resetDuration", 10);
                                    params.put("mainsFrequency", 50);
                                    jextras.put("k6l", params);
                                }

                                if (jextras.has("dayOfweek") == false) {
                                    JSONArray jdayofweek = new JSONArray();
                                    jdayofweek.put("Mon");
                                    jdayofweek.put("Tue");
                                    jdayofweek.put("Wed");
                                    jdayofweek.put("Thu");
                                    jdayofweek.put("Fri");
                                    jdayofweek.put("Sat");
                                    jdayofweek.put("Sun");
                                    jextras.put("dayOfweek", jdayofweek);
                                }

                                extra = jextras.toString();
                            }
                        }

                        Log.e(TAG, "Extra device: " + extra);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG,"EX extra: " + e);
                }
            }

            return new EquipoPaciente(disp, enable, desc, extra);
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"Exception getDeviceUIData(): " + e);
        }
        return null;
    }

    // Carga información de los Views para generar FichaPacienteDatos >> donde se genera dispositivos.json
    private FichaPacienteDatos getUIData() {
        try {
            Log.e(TAG,"getUIData()");
            Map<NombresDispositivo, EquipoPaciente> equipos = new HashMap<>();

            equipos.put(NombresDispositivo.OXIMETRO, this.getDeviceUIData(MAP_DISP_ID.get(NombresDispositivo.OXIMETRO), spinnerOXI));
            equipos.put(NombresDispositivo.TENSIOMETRO, this.getDeviceUIData(MAP_DISP_ID.get(NombresDispositivo.TENSIOMETRO), spinnerTEN));
            equipos.put(NombresDispositivo.BASCULA, this.getDeviceUIData(MAP_DISP_ID.get(NombresDispositivo.BASCULA), spinnerBAS));
            equipos.put(NombresDispositivo.ACTIVIDAD, this.getDeviceUIData(MAP_DISP_ID.get(NombresDispositivo.ACTIVIDAD), spinnerACT));
            equipos.put(NombresDispositivo.TERMOMETRO, this.getDeviceUIData(MAP_DISP_ID.get(NombresDispositivo.TERMOMETRO), spinnerTER));
            equipos.put(NombresDispositivo.MONITORPULMONAR, this.getDeviceUIData(MAP_DISP_ID.get(NombresDispositivo.MONITORPULMONAR), spinnerMONPUL));
            equipos.put(NombresDispositivo.ECG, this.getDeviceUIData(MAP_DISP_ID.get(NombresDispositivo.ECG), spinnerECG));
            equipos.put(NombresDispositivo.PEAKFLOW, this.getDeviceUIData(MAP_DISP_ID.get(NombresDispositivo.PEAKFLOW), spinnerPEAK));
            equipos.put(NombresDispositivo.ENCUESTA, this.getDeviceUIData(MAP_DISP_ID.get(NombresDispositivo.ENCUESTA), spinnerENC));
            equipos.put(NombresDispositivo.CAT, this.getDeviceUIData(MAP_DISP_ID.get(NombresDispositivo.CAT), spinnerCAT));

            int id_encuesta = 0;
            if (spinnerENC.getSelectedItem().toString().equals("Deshabilitado")) { id_encuesta = 0; }
            else { id_encuesta = Integer.parseInt(spinnerENC.getSelectedItem().toString()); }

            int logs = spinnerLOGS.getSelectedItemPosition();

            return new FichaPacienteDatos(equipos, id_encuesta, logs);
        }
        catch (Exception e){
            e.printStackTrace();
            Log.e(TAG, "Exception getUIData(): " + e);
        }
        return null;
    }

    // configura los componentes visuales para mostrar la informacion que se la pasa por parametros
    private void setDisplay(Map<NombresDispositivo, EquipoPaciente> equipos_paciente, int id_encuesta, List<String> ids_enc, int logs) {
        Log.e(TAG,"setDisplay()");

        spinnerLOGS.setSelection(logs);
        for(NombresDispositivo ndisp: equipos_paciente.keySet()) {
            EquipoPaciente equipo = equipos_paciente.get(ndisp);

            Log.e(TAG, "enable: " + equipo.isEnabled() + ", des: " + equipo.getDesc());

            if(ndisp == NombresDispositivo.OXIMETRO) {
                this.setSpinnerDefaultContent(spinnerOXI, equipo);
            }
            else if(ndisp == NombresDispositivo.TENSIOMETRO) {
                this.setSpinnerDefaultContent(spinnerTEN, equipo);
            }
            else if(ndisp == NombresDispositivo.BASCULA) {

                List<String> list = new ArrayList<>();
                list.add(FichaPacienteSpinnerValues.DESHABILITADO);
                list.add(FichaPacienteSpinnerValues.CLASICO);

                if(equipo.isEnabled()) {
                    if (equipo.getDesc().contains("Clasico") == false) {
                        list.add(equipo.getDesc());
                    }
                }

                setSpinnerContent(spinnerBAS, list);

                if(equipo.isEnabled()) {
                    spinnerBAS.setSelection(list.size() - 1);
                }
                else { spinnerBAS.setSelection(0); }

                //this.setSpinnerDefaultContent(spinnerBAS, equipo);
            }
            else if(ndisp == NombresDispositivo.ACTIVIDAD) {
                this.setSpinnerDefaultContent(spinnerACT, equipo);
            }
            else if(ndisp == NombresDispositivo.MONITORPULMONAR) {
                this.setSpinnerDefaultContent(spinnerMONPUL, equipo);
            }
            else if(ndisp == NombresDispositivo.ECG) {
                this.setSpinnerDefaultContent(spinnerECG, equipo);
            }
            else if(ndisp == NombresDispositivo.TERMOMETRO) {

                List<String> list = new ArrayList<>();
                list.add(FichaPacienteSpinnerValues.DESHABILITADO);
                list.add(FichaPacienteSpinnerValues.CLASICO);

                if(equipo.isEnabled()) {
                    if (equipo.getDesc().contains("Clasico") == false) {
                        list.add(equipo.getDesc());
                    }
                }

                setSpinnerContent(spinnerTER, list);

                if(equipo.isEnabled()) {
                    spinnerTER.setSelection(list.size() - 1);
                }
                else { spinnerTER.setSelection(0); }
            }
            else if(ndisp == NombresDispositivo.PEAKFLOW) {
                int pos = equipo.isEnabled() ? 0 : 1;
                spinnerPEAK.setSelection(pos);
            }
            else if(ndisp == NombresDispositivo.CAT) {
                int pos = equipo.isEnabled() ? 0 : 1;
                spinnerCAT.setSelection(pos);
            }
            else if(ndisp == NombresDispositivo.ENCUESTA) {
                setSpinnerContent(spinnerENC, ids_enc);
                int pos = ids_enc.indexOf(id_encuesta+"");
                if(pos == -1) pos = ids_enc.size() - 1;
                spinnerENC.setSelection(pos);
            }
        }
    }

    // escribir ficheros
    private void escribirFicheros(String idpaciente, Map<NombresDispositivo, EquipoPaciente> equipos_paciente, int id_encuesta, JSONArray arrayencuesta, int logs) throws IOException, JSONException {
        EVLogConfig.log(TAG,"escribirFicheros()");

        JSONObject jsonequipos = new JSONObject();
        for(NombresDispositivo ndisp: equipos_paciente.keySet()) {
            EquipoPaciente equipo = equipos_paciente.get(ndisp);
            JSONObject jsoneq = new JSONObject();
            jsoneq.put("id", equipo.getDispositivo().getId());
            jsoneq.put("enable", equipo.isEnabled());
            jsoneq.put("desc", equipo.getDesc());
            jsoneq.put("extra", equipo.getExtra());
            jsonequipos.put(ndisp.getNombre(), jsoneq);
            Log.e(TAG, "ndisp: " + ndisp);
        }
        FileAccess.escribirJSON(FilePath.CONFIG_DISPOSITIVOS, jsonequipos);
// COPY       FileAccess.copyFiles(FilePath.CONFIG_DISPOSITIVOS, true);

        JSONObject jsonencuesta = new JSONObject();
        jsonencuesta.put("id_encuesta", id_encuesta);
        jsonencuesta.put("arrayencuesta", arrayencuesta);
        FileAccess.escribirJSON(FilePath.CONFIG_ENCUESTA, jsonencuesta);
// COPY       FileAccess.copyFiles(FilePath.CONFIG_ENCUESTA, true);

        JSONObject jsonpaciente = new JSONObject();
        jsonpaciente.put("idpaciente", idpaciente);
        jsonpaciente.put("multipaciente", multipaciente);
        FileAccess.escribirJSON(FilePath.CONFIG_PACIENTE, jsonpaciente);
// COPY       FileAccess.copyFiles(FilePath.CONFIG_PACIENTE, true);

        JSONObject jsonlogs = new JSONObject();
        if(logs == 0) {
            jsonlogs.put("log","no_log");
        } else if(logs == 1) {
            jsonlogs.put("log","file_log");
        } else {
            jsonlogs.put("log","dev_log");
        }
        FileAccess.escribirJSON(FilePath.CONFIG_LOG, jsonlogs);
// COPY        FileAccess.copyFiles(FilePath.CONFIG_LOG, true);

        ConfigLog.configurar();
    }

    // subir datos
    private void subirDatos(Map<NombresDispositivo, EquipoPaciente> equipos_paciente, int id_encuesta) throws JSONException, ApiException, IOException {
        EVLogConfig.log(TAG,"subirDatos()");

        for(NombresDispositivo ndisp: equipos_paciente.keySet()) {
            EquipoPaciente equipo = equipos_paciente.get(ndisp);

//            Log.e(TAG,"equipo >> getId(): " + equipo.getDispositivo().getId() + ", isEnabled(): " + equipo.isEnabled() + ", getDesc(): " + equipo.getDesc());

            this.UpdateDevice(equipo.getDispositivo().getId(), equipo.isEnabled() ? 1 : 0, equipo.getDesc(), equipo.getExtra());
        }
        this.UpdateEncuestaPaciente(id_encuesta);
    }

    private void GuardarDatosInicialesDB(FichaPacienteDatos fichaPacienteDatos2) {
        EVLogConfig.log(TAG,"GuardarDatosInicialesDB()");
        try {
            if (fichaPacienteDatos2.getEquipos() != null) {
                btn_aplicar = false;

                FichaPacienteDatos fichaPacienteDatos = this.getUIData();

                this.subirDatos(fichaPacienteDatos.getEquipos(), fichaPacienteDatos.getId_encuesta());

                JSONArray arrayencuesta = this.getEncuesta();
                this.escribirFicheros(this.idpaciente, fichaPacienteDatos.getEquipos(), fichaPacienteDatos.getId_encuesta(), arrayencuesta, fichaPacienteDatos.getLogs());

                btn_aplicar = true;
                Toast.makeText(getApplicationContext(), "Fichero de dispositivos actualizado de la DB.", Toast.LENGTH_LONG).show();

            }else {
                Alerts.mostrarAlert(this, "ERROR al generar el fichero de dispositivos.");
            }
        }
        catch (JSONException | ApiException | IOException e) {
            e.printStackTrace();
            Alerts.mostrarAlert(this, "ERROR al generar el fichero de dispositivos.");
        }
    }
    //endregion

    //region :: SELECCION ELEMENTO DE UN SPINNER
    /**
     * Evento cuando se selecciona un nuevo elemento de un spinner
     * @param parent
     * @param v
     * @param position
     * @param id
     */
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        Spinner spinner = (Spinner) parent;
        Log.e(TAG,"onItemSelected(" + nameSpinner(spinner) + ")");
        btn_aplicar = false;

        visibleAllBLE();
        visibleAllCNF();
        visibleCalendar();
    }

    public void onNothingSelected(AdapterView<?> parent) {
        return;
    }

    //endregion

    private void setTextSatus(String texto) {
        runOnUiThread(new Runnable() {
            public void run() {
                txtStatus.setText(texto);
            }
        });
    }

    private void setTextCuentaAtras(String texto) {
        runOnUiThread(new Runnable() {
            public void run() {
                txtCuentaAtras.setText(texto);
            }
        });
    }

    //region :: INTERNET
    private boolean isNetDisponible() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo actNetInfo = connectivityManager.getActiveNetworkInfo();

        return (actNetInfo != null && actNetInfo.isConnected());
    }

    private Boolean isOnlineNet() {

        try {
            Process p = java.lang.Runtime.getRuntime().exec("ping -c 1 www.google.es");

            int val = p.waitFor();
            boolean reachable = (val == 0);
            return reachable;

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }
    //endregion

    //region :: CICLO DE VIDA
    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        EVLogConfig.log(TAG,"onDestroy()");

        if( mCallbackBeurer.isOrderedBroadcast() ){
            // receiver object is registered
            if (mBeurerControl != null) mBeurerControl.destroy();
        }

        unregisterReceiver(mCallbackBeurer);

        if (bluetoothAdapter.isDiscovering()) {
            Log.e(TAG,"cancelDiscovery()");
            try {
                bluetoothAdapter.cancelDiscovery();
                bluetoothLeScanner.stopScan(scanCallback);
            }
            catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG,"EXCEPTION onDestroy(): " + e.toString());
            }
        }

        super.onDestroy();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}

    @Override
    protected void onResume() {
        super.onResume();
        EVLogConfig.log(TAG, "onResume()");

        if (config) {
            Log.e(TAG,"onResume() > config(true)");
            config = false;
            enableView();
            registerReceiver(mCallbackBeurer, makeUpdateIntentFilter(), RECEIVER_EXPORTED);
        }
        else {
            Log.e(TAG,"onResume() > config(false)");
            if (btn_wireless) {
                Log.e(TAG,"onResume() > config(false) >> btn_wireless");
//                Intent intent = new Intent(DispositivosPaciente.this, DispositivosPaciente.class);
//                intent.putExtra("numeropaciente", idpaciente);
                setFilesBackup();
                finish();
//                startActivity(new Intent(DispositivosPaciente.this, DispositivosPaciente.class));
            }
            btn_salirso.setEnabled(true);
            btn_salir.setEnabled(true);
            enableView();
        }
    }

    @Override
    protected  void onPause() {
        super.onPause();
        EVLogConfig.log(TAG,"onPause()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        EVLogConfig.log(TAG,"onRestart()");
    }
    //endregion

    private String leerSerial(){
        Log.e(TAG,"leerSerial()");
        File file = new File(FileAccess.getPATH_FILES(),"serial.txt");

        //Read text from file
        String text = new String();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text = line;
            }
            br.close();

            return text;
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
        return text;
    }

    //region :: SEND FILES TO BACKUP
    public void setFilesBackup(){

        EVLogConfig.log(TAG,"setFilesBackup()");

        // BACKUP
//        String filename = FilePath.CONFIG_DISPOSITIVOS.getNameFile();
//        String content = FileFuntions.readfile(filename);
//        UriAccess.updateUriContent(this, MAPFileUri.get(filename),content);
//
//        filename = FilePath.CONFIG_ENCUESTA.getNameFile();
//        content = FileFuntions.readfile(filename);
//        UriAccess.updateUriContent(this, MAPFileUri.get(filename),content);
//
//        filename = FilePath.CONFIG_PACIENTE.getNameFile();
//        content = FileFuntions.readfile(filename);
//        Log.e(TAG,"CONFIG_PACIENTE content: " + content);
//        UriAccess.updateUriContent(this, MAPFileUri.get(filename),content);
//
//        filename = FilePath.CONFIG_LOG.getNameFile();
//        content = FileFuntions.readfile(filename);
//        UriAccess.updateUriContent(this, MAPFileUri.get(filename),content);

    }
    //endregion

    private List<String> getPairedDevices() {
        @SuppressLint("MissingPermission")
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        List<String> listado = new ArrayList<>();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                @SuppressLint("MissingPermission")
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                deviceHardwareAddress = deviceHardwareAddress.replace(":","");
                String dev = "" + deviceName + "-" + deviceHardwareAddress;
                listado.add(dev);
//                Log.e(TAG,"Vinculado: " + dev);
            }
        }
        return listado;
    }

    private void visibleView (View view,Boolean visible) {
//        Log.e(TAG,"visibleView(" + visible + ")");
        runOnUiThread(new Runnable() {
            public void run() {
                if (visible) { view.setVisibility(View.VISIBLE); }
                else { view.setVisibility(View.INVISIBLE); }
            }
        });
    }

    private void visibleAllBLE() {
        List<String> pairedDevices = getPairedDevices(); // Obtenemos los dispositivos Emparejados
        Log.e(TAG,"visibleAllBLE() >> pairedDevices: " + pairedDevices);
        String text = "";

        //region :: PULSIOXIMETROS
        text = spinnerOXI.getSelectedItem().toString().replace(":","");
//        Log.e(TAG,"OXI ************** " + text);
        if (text.equals("Deshabilitado")) {
            visibleView(btnBleOxi,false);
        }
        else {
            String[] device = text.split("-");
            String model = device[0];
            if (BeurerReferences.deviceOXI.contains(model)) {
                if (pairedDevices.contains(text)){
                    visibleView(btnBleOxi, false);
                }
                else {
                    visibleView(btnBleOxi, true);
                }
            }
        }
        //endregion

        //region :: TENSIOMETROS
        text = spinnerTEN.getSelectedItem().toString().replace(":","");
//        Log.e(TAG,"TEN ************** " + text);
        if (text.equals("Deshabilitado")) {
            visibleView(btnBleTen,false);
        }
        else {
            String[] device = text.split("-");
            String model = device[0];
            if (BeurerReferences.deviceTEN.contains(model)) {
                if (pairedDevices.contains(text)){
                    visibleView(btnBleTen, false);
                }
                else {
                    visibleView(btnBleTen, true);
                }
            }
        }
        //endregion

        //region :: ACTIVIDAD
        text = spinnerACT.getSelectedItem().toString().replace(":","");
//        Log.e(TAG,"ACT ************** " + text);
        if (text.equals("Deshabilitado")) {
            visibleView(btnBleAct,false);
        }
        else {
            String[] device = text.split("-");
            String model = device[0];
            if (BeurerReferences.deviceACT.contains(model)) {
                if (pairedDevices.contains(text)){
                    visibleView(btnBleAct, false);
                }
                else {
                    visibleView(btnBleAct, true);
                }
            }
        }
        //endregion

        //region :: TERMOMETROS
        text = spinnerTER.getSelectedItem().toString().replace(":","");
//        Log.e(TAG,"TER ************** " + text);
        if (text.equals("Deshabilitado") || text.equals("Clasico")) {
            visibleView(btnBleThermo,false);
        }
        else {
            String[] device = text.split("-");
            String model = device[0];

            if (BeurerReferences.deviceTHERMO.contains(model)) {
                if (pairedDevices.contains(text)){
                    // Dispositivo no emparejado
                    visibleView(btnBleThermo, false);
                }
                else {
                    // Dispositivo emparejado
                    visibleView(btnBleThermo, true);
                }
            }
            else {
                // Dispositivo no es necesario emparejar
                visibleView(btnBleThermo, false);
            }
        }
        //endregion

        //region :: BÁSCULAS
        text = spinnerBAS.getSelectedItem().toString().replace(":","");
//        Log.e(TAG,"BAS ************** " + text);
        if (text.equals("Deshabilitado") || text.equals("Clasico")) {
            visibleView(btnBleScale,false);
        }
        else {
            String[] device = text.split("-");
            String model = device[0];
            if (BeurerReferences.deviceSCALE.contains(model)) {
                if (pairedDevices.contains(text)){
                    visibleView(btnBleScale, false);
                }
                else {
                    visibleView(btnBleScale, true);
                }
            }
        }
        //endregion

        //region :: MONITOR PULMONAR
        text = spinnerMONPUL.getSelectedItem().toString().replace(":","");
//        Log.e(TAG,"BAS ************** " + text);
        if (text.equals("Deshabilitado")) {
            visibleView(btnBleMONPUL,false);
        }
        else {
            String[] device = text.split("-");
            String model = device[0];
            if (BeurerReferences.deviceMONPUL.contains(model)) {
                if (pairedDevices.contains(text)){
                    visibleView(btnBleMONPUL, false);
                }
                else {
                    visibleView(btnBleMONPUL, true);
                }
            }
        }
        //endregion

        //region :: ECG
        text = spinnerECG.getSelectedItem().toString().replace(":","");
//        Log.e(TAG,"BAS ************** " + text);
        if (text.equals("Deshabilitado")) {
            visibleView(btnBleECG,false);
        }
        else {
            String[] device = text.split("-");
            String model = device[0];
            if (BeurerReferences.deviceECG.contains(model)) {
                if (pairedDevices.contains(text)){
                    visibleView(btnBleECG, false);
                }
                else {
                    visibleView(btnBleECG, true);
                }
            }
        }
        //endregion

        RowInvisibles();
    }

    //region :: BROADCAST RECEIVER BLE BEURER Y OTROS
    private final BroadcastReceiver mCallbackBeurer = new BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            String message = extras.getString(BeurerReferences.ACTION_EXTRA_MESSAGE,"");
            Log.e(TAG, "mCallbackBeurer(): " + action);

            switch(action) {
                case BeurerReferences.ACTION_BEURER_CONNECTED:
                    //region :: CONNECTED
                    setTextSatus("Paired Device");

                    if (pairingModel == Pairing.bf600) {
                        mBeurerControl.UserActive(1);
                    }
                    else {
                        mBeurerControl.disconnect();
                    }
                    //endregion
                    break;

                case BeurerReferences.ACTION_BEURER_DISCONNECTED:
                    // region :: DISCONNECTED
                    retries += 1;

                    if (mBeurerControl != null) {
                        Log.e(TAG, "DISCONNECTED STATE: " + mBeurerControl.getIsconnect());
                        if (mBeurerControl.getIsconnect() == Global.STATE_CONNECTING) {
                            if (retries < 4) {
                                Log.e(TAG, "Reintento de conexion: " + retries);
                                setTextSatus("RECONNECT[" + retries + "]");
                                if (mBeurerControl != null) mBeurerControl.destroy();
                                SystemClock.sleep(1000);
                                mBeurerControl = new BeurerControl();
                                mBeurerControl.initialize(DispositivosPaciente.this, mDeviceAddress);
                            } else {
                                enableView();
                                visibleAllBLE();
                                visibleAllCNF();
                                visibleCalendar();
                                setTextSatus("Not Found device");
                                if (mBeurerControl != null) mBeurerControl.destroy();
                            }
                        } else {
                            enableView();
                            visibleAllBLE();
                            visibleAllCNF();
                            visibleCalendar();
                            if (mBeurerControl != null) mBeurerControl.destroy();
                        }
                    }
                    else {
                        Log.e(TAG,"DISCONNECTED");
                    }

                    //endregion
                    break;

                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    //region :: ACTION_BOND_STATE_CHANGED
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    Log.e(TAG,"ACTION_BOND_STATE_CHANGED: " + bondState);
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        Log.e(TAG, "BOND_BONDED: dispositivo remoto está enlazado (emparejado).");
                        Log.e(TAG, "pairingModel: " + pairingModel.toString());
                        if (pairingModel == Pairing.bf600) {
                            pairingModel = Pairing.None;
                            mBeurerControl.disconnect();
                        }

                    }
                    else if (bondState == BluetoothDevice.BOND_NONE) {
                        Log.e(TAG, "BOND_NONE: dispositivo remoto NO está enlazado (emparejado).");
                    }
                    else if (bondState == BluetoothDevice.BOND_BONDING) {
                        Log.e(TAG, "BOND_BONDING: la vinculación (emparejamiento) está en curso con el dispositivo remoto.");
                        setTextSatus("Viculación en curso");
                    }
                    //endregion
                    break;

                case BluetoothDevice.ACTION_PAIRING_REQUEST:
                    //region :: ACTION_PAIRING_REQUEST
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.e(TAG,"ACTION_PAIRING_REQUEST: " + device.getName() + " " + device.getAddress());
                    //endregion
                    break;

                case BeurerReferences.ACTION_BEURER_BF600_USER_CONTROL_POINT:
                    mBeurerControl.disconnect();
                    break;

                case BeurerReferences.ACTION_EXTRA_DATA:
                    setTextSatus("ACTION_EXTRA_DATA");
                    Log.e(TAG,"ACTION_EXTRA_DATA: " + message);

                    try {
                        JSONObject jmessage = new JSONObject(message);
                        String deviceinfo  = jmessage.getString("deviceinfo");

                        if (deviceinfo.equals("lung")) {
                            JSONObject jextras = new JSONObject(EQUIPOS_PAC.get(NombresDispositivo.MONITORPULMONAR).getExtra());
                            JSONObject lung = new JSONObject(jmessage.getString("lung"));
                            jextras.put("lung", lung);

                            EQUIPOS_PAC.get(NombresDispositivo.MONITORPULMONAR).setExtra(jextras.toString());
                            EQUIPOS_PAC.get(NombresDispositivo.MONITORPULMONAR).getDispositivo().setExtra(jextras.toString());
                            MAP_DISP_ID.put(NombresDispositivo.MONITORPULMONAR, EQUIPOS_PAC.get(NombresDispositivo.MONITORPULMONAR).getDispositivo());

                            Log.e(TAG, "EQUIPOS_PAC.get(NombresDispositivo.MONITORPULMONAR).getExtra(): " + EQUIPOS_PAC.get(NombresDispositivo.MONITORPULMONAR).getExtra());
                        }
                        else if (deviceinfo.equals("hs2s")) {
                            JSONObject jextras = new JSONObject(EQUIPOS_PAC.get(NombresDispositivo.BASCULA).getExtra());
                            JSONObject hs2s = new JSONObject(jmessage.getString("hs2s"));
                            jextras.put("hs2s", hs2s);

                            EQUIPOS_PAC.get(NombresDispositivo.BASCULA).setExtra(jextras.toString());
                            EQUIPOS_PAC.get(NombresDispositivo.BASCULA).getDispositivo().setExtra(jextras.toString());
                            MAP_DISP_ID.put(NombresDispositivo.BASCULA, EQUIPOS_PAC.get(NombresDispositivo.BASCULA).getDispositivo());

                            Log.e(TAG, "EQUIPOS_PAC.get(NombresDispositivo.BASCULA).getExtra(): " + EQUIPOS_PAC.get(NombresDispositivo.BASCULA).getExtra());
                        }
                        else if (deviceinfo.equals("k6l")) {
                            JSONObject jextras = new JSONObject(EQUIPOS_PAC.get(NombresDispositivo.ECG).getExtra());
                            JSONObject k6l = new JSONObject(jmessage.getString("k6l"));
                            jextras.put("k6l", k6l);

                            EQUIPOS_PAC.get(NombresDispositivo.ECG).setExtra(jextras.toString());
                            EQUIPOS_PAC.get(NombresDispositivo.ECG).getDispositivo().setExtra(jextras.toString());
                            MAP_DISP_ID.put(NombresDispositivo.ECG, EQUIPOS_PAC.get(NombresDispositivo.ECG).getDispositivo());

                            Log.e(TAG, "EQUIPOS_PAC.get(NombresDispositivo.ECG).getExtra(): " + EQUIPOS_PAC.get(NombresDispositivo.ECG).getExtra());
                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                default:
                    Log.e(TAG,"accion de broadcast no implementada");
                    mBeurerControl.disconnect();
                    break;

            };
        }
    };

    private static IntentFilter makeUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_CONNECTED);
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_DISCONNECTED);

        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);

        // Para recibir mensajes de configuración de dispositivos
        intentFilter.addAction(BeurerReferences.ACTION_EXTRA_DATA);

        // Para vincular BL600
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_BF600_USER_CONTROL_POINT);

        return intentFilter;
    }
    //endregion

    private void visibleAllCNF() {
        // Obtenemos los dispositivos Emparejados
        List<String> pairedDevices = getPairedDevices();
//        Log.e(TAG,"visibleAllCNF() >> pairedDevices: " + pairedDevices);

        //region :: PULSIOXIMETROS
        try {
            String text = spinnerOXI.getSelectedItem().toString();
//            Log.e("OXI *******************", text);
            text = text.replace(":", "");

            if (text.equals("Deshabilitado")) {
                visibleView(btnCnfOxi, false);
            } else {
                String[] device = text.split("-");
                String model = device[0];
                if (BeurerReferences.deviceOXI_CNF.contains(model)) {
                    if (pairedDevices.contains(text)) {
                        visibleView(btnCnfOxi, true);
                    } else {
                        visibleView(btnCnfOxi, false);
                    }
                } else {
                    visibleView(btnCnfOxi, false);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"EX OXI: " + e);
        }
        //endregion

        //region :: TENSIOMETROS
        try {
            String text = spinnerTEN.getSelectedItem().toString();
//            Log.e("TEN *******************", text);
            text = text.replace(":", "");

            if (text.equals("Deshabilitado")) {
                visibleView(btnCnfTen, false);
            } else {
                String[] device = text.split("-");
                String model = device[0];
                if (BeurerReferences.deviceTEN_CNF.contains(model)) {
                    if (pairedDevices.contains(text)) {
                        visibleView(btnCnfTen, true);
                    } else {
                        visibleView(btnCnfTen, false);
                    }
                } else {
                    visibleView(btnCnfTen, false);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"EX TEN: " + e);
        }
        //endregion

        //region :: ACTIVIDAD
        try {
            String text = spinnerACT.getSelectedItem().toString();
//            Log.e("ACT *******************", text);
            text = text.replace(":", "");

            if (text.equals("Deshabilitado")) {
                visibleView(btnCnfAct, false);
            } else {
                String[] device = text.split("-");
                String model = device[0];
                if (BeurerReferences.deviceACT_CNF.contains(model)) {
//                Log.e(TAG,"MODEL: " + model + ", text: " + text);
                    if (pairedDevices.contains(text)) {
                        visibleView(btnCnfAct, true);
                    } else {
                        visibleView(btnCnfAct, false);
                    }
                } else if (TranstekDevice.deviceACT_CNF.contains(model)) {
                    visibleView(btnCnfAct, true);
                } else {
                    visibleView(btnCnfAct, false);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"EX ACT: " + e);
        }
        //endregion

        //region :: TERMOMETROS
        try {
            String text = spinnerTER.getSelectedItem().toString();
//            Log.e("TER *******************", text);
            text = text.replace(":", "");

            if (text.equals("Deshabilitado") || text.equals("Clasico")) {
                visibleView(btnCnfThermo, false);
            } else {
                String[] device = text.split("-");
                String model = device[0];
                if (BeurerReferences.deviceTHERMO_CNF.contains(model)) {
//                    Log.e(TAG, "deviceTHERMO_CNF: " + model + ", text: " + text);
                    if (pairedDevices.contains(text)) {
                        // Dispositivo no emparejado
                        visibleView(btnCnfThermo, true);
                    } else {
                        // Dispositivo emparejado
                        visibleView(btnCnfThermo, false);
                    }
                } else {
                    // Dispositivo que no se empareja
                    visibleView(btnCnfThermo, false);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"EX TER: " + e);
        }
        //endregion

        //region :: BÁSCULAS
        try {
            String text = spinnerBAS.getSelectedItem().toString();
//            Log.e("BAS *******************", text);
            text = text.replace(":", "");

            if (text.equals("Deshabilitado") || text.equals("Clasico")) {
                visibleView(btnCnfScale, false);
            } else {
                String[] device = text.split("-");
                String model = device[0];
                if (BeurerReferences.deviceBAS_CNF.contains(model)) {
                    if (pairedDevices.contains(text)) {
                        visibleView(btnCnfScale, true);
                    } else {
                        visibleView(btnCnfScale, false);
                    }
                }
                else if (TranstekDevice.device.contains(model)) {
                    if (TranstekDevice.deviceSCALE_CNF.contains(model)) {
                        visibleView(btnCnfScale, true);
                    }
                    else { visibleView(btnCnfScale, false); }
                }
                else {
                    visibleView(btnCnfScale, true);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"EX OXI: " + e);
        }
        //endregion

        //region :: MONITOR PULMONAR
        try {
            String text = spinnerMONPUL.getSelectedItem().toString();
            text = text.replace(":", "");

            if (text.equals("Deshabilitado")) {
                visibleView(btnCnfMONPUL, false);
            } else {
                String[] device = text.split("-");
                String model = device[0];
                if (BeurerReferences.deviceMONPUL_CNF.contains(model)) {
                    if (pairedDevices.contains(text)) {
                        visibleView(btnCnfMONPUL, true);
                    } else {
                        visibleView(btnCnfMONPUL, false);
                    }
                } else {
                    visibleView(btnCnfMONPUL, true);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"EX OXI: " + e);
        }
        //endregion

        //region :: ECG
        try {
            String text = spinnerECG.getSelectedItem().toString();
            text = text.replace(":", "");

            if (text.equals("Deshabilitado")) {
                visibleView(btnCnfECG, false);
            } else {
                String[] device = text.split("-");
                String model = device[0];
                if (BeurerReferences.deviceECG_CNF.contains(model)) {
                    if (pairedDevices.contains(text)) {
                        visibleView(btnCnfECG, true);
                    } else {
                        visibleView(btnCnfECG, false);
                    }
                } else {
                    visibleView(btnCnfECG, true);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"EX ECG: " + e);
        }
        //endregion

        RowInvisibles();
    }

    private void visibleCalendar() {

        String text = "";
        Button btnCalendar;

        //region :: PULSIOXIMETROS
        text = spinnerOXI.getSelectedItem().toString().replace(":","");
        btnCalendar = findViewById(R.id.btnCalendarOximetro);
        if (text.equals("Deshabilitado")) { visibleView(btnCalendar,false); }
        else {
            visibleView(btnCalendar, true);
            loadCalendar(NombresDispositivo.OXIMETRO);
        }
        //endregion

        //region :: TENSIOMETROS
        text = spinnerTEN.getSelectedItem().toString().replace(":","");
        btnCalendar = findViewById(R.id.btnCalendarTensiometro);
        if (text.equals("Deshabilitado")) { visibleView(btnCalendar,false); }
        else {
            visibleView(btnCalendar, true);
            loadCalendar(NombresDispositivo.TENSIOMETRO);
        }
        //endregion

        //region :: ACTIVIDAD
        text = spinnerACT.getSelectedItem().toString().replace(":","");
        btnCalendar = findViewById(R.id.btnCalendarActividad);
        if (text.equals("Deshabilitado")) { visibleView(btnCalendar,false); }
        else {
            visibleView(btnCalendar, true);
            loadCalendar(NombresDispositivo.ACTIVIDAD);
        }
        //endregion

        //region :: TERMOMETROS
        text = spinnerTER.getSelectedItem().toString().replace(":","");
        btnCalendar = findViewById(R.id.btnCalendarTermometro);
        if (text.equals("Deshabilitado")) { visibleView(btnCalendar,false); }
        else {
            visibleView(btnCalendar, true);
            loadCalendar(NombresDispositivo.TERMOMETRO);
        }
        //endregion

        //region :: BÁSCULAS
        text = spinnerBAS.getSelectedItem().toString().replace(":","");
        btnCalendar = findViewById(R.id.btnCalendarScale);
        if (text.equals("Deshabilitado")) { visibleView(btnCalendar,false); }
        else {
            visibleView(btnCalendar, true);
            loadCalendar(NombresDispositivo.BASCULA);
        }
        //endregion

        //region :: MONITOR PULMONAR
        text = spinnerMONPUL.getSelectedItem().toString().replace(":","");
        btnCalendar = findViewById(R.id.btnCalendarMonitorPulmonar);
        if (text.equals("Deshabilitado")) { visibleView(btnCalendar,false); }
        else {
            visibleView(btnCalendar, true);
            loadCalendar(NombresDispositivo.MONITORPULMONAR);
        }
        //endregion

        //region :: ECG
        text = spinnerECG.getSelectedItem().toString().replace(":","");
        btnCalendar = findViewById(R.id.btnCalendarECG);
        if (text.equals("Deshabilitado")) { visibleView(btnCalendar,false); }
        else {
            visibleView(btnCalendar, true);
            loadCalendar(NombresDispositivo.ECG);
        }
        //endregion

        //region :: PEAKFLOW
        text = spinnerPEAK.getSelectedItem().toString().replace(":","");
        btnCalendar = findViewById(R.id.btnCalendarPEAK);
        if (text.equals("Deshabilitado")) { visibleView(btnCalendar,false); }
        else {
            visibleView(btnCalendar, true);
            loadCalendar(NombresDispositivo.PEAKFLOW);
        }
        //endregion

        //region :: ENCUESTA
        text = spinnerENC.getSelectedItem().toString().replace(":","");
        btnCalendar = findViewById(R.id.btnCalendarENC);
        if (text.equals("Deshabilitado")) { visibleView(btnCalendar,false); }
        else {
            visibleView(btnCalendar, true);
            loadCalendar(NombresDispositivo.ENCUESTA);
        }
        //endregion

        //region :: CAT
        text = spinnerCAT.getSelectedItem().toString().replace(":","");
        btnCalendar = findViewById(R.id.btnCalendarCAT);
        if (text.equals("Deshabilitado")) { visibleView(btnCalendar,false); }
        else {
            visibleView(btnCalendar, true);
            loadCalendar(NombresDispositivo.CAT);
        }
        //endregion

        RowInvisibles();
    }

    //region :: BORRAR PACIENTE
    private void deleteAllLogAndJsonFiles() {
        // Obtenemos el directorio de archivos de la aplicación
        File filesDirectory = FileAccess.getPATH();
        Log.e("Directory", "" + filesDirectory.toString());

        // Si el directorio no existe, salimos
        if (!filesDirectory.exists()) {
            Log.e("Directory", "directorio no existe");
            return;
        }

        // Obtenemos una lista de todos los archivos del directorio
        File[] files = filesDirectory.listFiles();

        // Recorremos la lista de archivos
        for (File file : files) {
            Log.e("LISTA","File: " + file.getName());
            // Si el archivo tiene extensión .log o .json, lo eliminamos
            if (file.getName().endsWith(".log") || file.getName().endsWith(".json") || file.getName().matches("\\*SDK_Debug.txt")) {
                try {
                    Log.e("LISTA","Borro > " + file.getName());
                    file.delete();
                } catch (Exception e) {
                    // Controlamos la excepción
                    Log.e("MyApp", "Error al eliminar el archivo " + file.getName(), e);
                }
            }
        }
    }

    private void deleteAllLogAndJsonFiles(String directorio) {
        // Obtenemos el directorio de archivos de la aplicación
        String path = FileAccess.getPATH().toString();

        File filesDirectory = new File(path + "/" + directorio);
        Log.e("Directory", "" + filesDirectory.toString());

        // Si el directorio no existe, salimos
        if (!filesDirectory.exists()) {
            Log.e("Directory", "directorio no existe");
            return;
        }

        // Obtenemos una lista de todos los archivos del directorio
        File[] files = filesDirectory.listFiles();

        // Recorremos la lista de archivos
        for (File file : files) {
            Log.e("LISTA","File: " + file.getName());
            // Si el archivo tiene extensión .log o .json, lo eliminamos
            if (file.getName().endsWith(".log") || file.getName().endsWith(".json") || file.getName().endsWith(".txt")) {
                try {
                    Log.e("LISTA","Borro > " + file.getName());
                    file.delete();
                } catch (Exception e) {
                    // Controlamos la excepción
                    Log.e("MyApp", "Error al eliminar el archivo " + file.getName(), e);
                }
            }
        }
    }

    private void deleteFilesBackup(){
        Log.e(TAG,"deleteFilesBackup()");
        // DELETE PROVEEDOR CONTENIDOS
    }
    //endregion

    public void selectDayOfweek(NombresDispositivo ndisp) {
        // Los días de la semana
        final String[] dias = new String[]{"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

        // Usa el array de seleccionados iniciales
        final boolean[] seleccionados = getDayOfweek(ndisp);
        Log.e(TAG,"getDayOfweek(): " + Arrays.toString(seleccionados));

        // Crea el AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecciona los días de la semana")
                .setMultiChoiceItems(dias, seleccionados, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        // Actualiza el estado actual de los elementos seleccionados
                        seleccionados[which] = isChecked;
                    }
                })
                .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Cuando se cierra el desplegable, obtén la información seleccionada
                        EQUIPOS_PAC.get(ndisp).setExtra(arrayDayOfWeek(seleccionados));

                        int nselct = 0;
                        for (int i = 0; i < seleccionados.length; i++) {
                            if (seleccionados[i]) { nselct += 1; }
                        }
                        Button button;
                        if (ndisp == NombresDispositivo.OXIMETRO) { button = (Button) findViewById(R.id.btnCalendarOximetro); }
                        else if (ndisp == NombresDispositivo.TENSIOMETRO) { button = (Button) findViewById(R.id.btnCalendarTensiometro); }
                        else if (ndisp == NombresDispositivo.ACTIVIDAD) { button = (Button) findViewById(R.id.btnCalendarActividad); }
                        else if (ndisp == NombresDispositivo.TERMOMETRO) { button = (Button) findViewById(R.id.btnCalendarTermometro); }
                        else if (ndisp == NombresDispositivo.BASCULA) { button = (Button) findViewById(R.id.btnCalendarScale); }
                        else if (ndisp == NombresDispositivo.MONITORPULMONAR) { button = (Button) findViewById(R.id.btnCalendarMonitorPulmonar); }
                        else if (ndisp == NombresDispositivo.ECG) { button = (Button) findViewById(R.id.btnCalendarECG); }
                        else if (ndisp == NombresDispositivo.PEAKFLOW) { button = (Button) findViewById(R.id.btnCalendarPEAK); }
                        else if (ndisp == NombresDispositivo.ENCUESTA) { button = (Button) findViewById(R.id.btnCalendarENC); }
                        else { button = (Button) findViewById(R.id.btnCalendarCAT); }

                        if (nselct == 0) { button.setBackgroundResource(R.drawable.calendar_w0); }
                        else if (nselct == 1) { button.setBackgroundResource(R.drawable.calendar_w1); }
                        else if (nselct == 2) { button.setBackgroundResource(R.drawable.calendar_w2); }
                        else if (nselct == 3) { button.setBackgroundResource(R.drawable.calendar_w3); }
                        else if (nselct == 4) { button.setBackgroundResource(R.drawable.calendar_w4); }
                        else if (nselct == 5) { button.setBackgroundResource(R.drawable.calendar_w5); }
                        else if (nselct == 6) { button.setBackgroundResource(R.drawable.calendar_w6); }
                        else { button.setBackgroundResource(R.drawable.calendar_w7); }

                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Acción para el botón "Cancelar", si es necesario
                    }
                });

        // Muestra el AlertDialog
        builder.create().show();
    }

    private String arrayDayOfWeek(boolean[] seleccionados) {
        final String[] days = new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        List<String> dayOfweek = new ArrayList<>();

        try {
            JSONObject json = new JSONObject();
            for (int i = 0; i < seleccionados.length; i++) {
                if (seleccionados[i]) {
                    Log.e("Días seleccionados", days[i]);
                    dayOfweek.add(days[i]);
                }
            }

            JSONArray jsonArray = new JSONArray(dayOfweek);
            json.put("dayOfweek", jsonArray);
            return json.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return "{}";
        }
    }

    private boolean[] getDayOfweek(NombresDispositivo ndisp) {
        final String[] days = new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        boolean[] seleccionados = new boolean[]{false, false, false, false, false, false, false};
        try {
            JSONObject extras = new JSONObject(EQUIPOS_PAC.get(ndisp).getExtra());
//            Log.e(TAG,"extras: " + extras);

            if (!extras.has("dayOfweek")) {
                return new boolean[]{true, true, true, true, true, true, true};
            }

            JSONArray values = new JSONArray(extras.getString("dayOfweek"));
            for (int i = 0; i < values.length(); i++) {

                int pos = Arrays.asList(days).indexOf(values.getString(i));
//                Log.e(TAG,"values.getString(i): " + values.getString(i));
//                Log.e(TAG,"pos: " + pos);
                if (pos != -1) {
                    seleccionados[pos] = true;
                }
            }
            return seleccionados;
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadCalendar(NombresDispositivo ndisp) {
        final boolean[] seleccionados = getDayOfweek(ndisp);
        int nselct = 0;
        for (int i = 0; i < seleccionados.length; i++) {
            if (seleccionados[i]) { nselct += 1; }
        }
        Button button;
        if (ndisp == NombresDispositivo.OXIMETRO) {
            button = (Button) findViewById(R.id.btnCalendarOximetro);
        }
        else if (ndisp == NombresDispositivo.TENSIOMETRO) { button = (Button) findViewById(R.id.btnCalendarTensiometro); }
        else if (ndisp == NombresDispositivo.ACTIVIDAD) { button = (Button) findViewById(R.id.btnCalendarActividad); }
        else if (ndisp == NombresDispositivo.TERMOMETRO) { button = (Button) findViewById(R.id.btnCalendarTermometro); }
        else if (ndisp == NombresDispositivo.BASCULA) { button = (Button) findViewById(R.id.btnCalendarScale); }
        else if (ndisp == NombresDispositivo.MONITORPULMONAR) { button = (Button) findViewById(R.id.btnCalendarMonitorPulmonar); }
        else if (ndisp == NombresDispositivo.ECG) { button = (Button) findViewById(R.id.btnCalendarECG); }
        else if (ndisp == NombresDispositivo.PEAKFLOW) { button = (Button) findViewById(R.id.btnCalendarPEAK); }
        else if (ndisp == NombresDispositivo.ENCUESTA) { button = (Button) findViewById(R.id.btnCalendarENC); }
        else { button = (Button) findViewById(R.id.btnCalendarCAT); }

        if (nselct == 0) { button.setBackgroundResource(R.drawable.calendar_w0); }
        else if (nselct == 1) { button.setBackgroundResource(R.drawable.calendar_w1); }
        else if (nselct == 2) { button.setBackgroundResource(R.drawable.calendar_w2); }
        else if (nselct == 3) { button.setBackgroundResource(R.drawable.calendar_w3); }
        else if (nselct == 4) { button.setBackgroundResource(R.drawable.calendar_w4); }
        else if (nselct == 5) { button.setBackgroundResource(R.drawable.calendar_w5); }
        else if (nselct == 6) { button.setBackgroundResource(R.drawable.calendar_w6); }
        else { button.setBackgroundResource(R.drawable.calendar_w7); }
    }
    //{ "dayOfweek":["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"] }

    public void selectMultiPaciente(Boolean multiPaciente) {

        final String[] dias = new String[]{"Estado"};

        // Usa el array de seleccionados iniciales
        final boolean[] seleccionados = new boolean[1];
        seleccionados[0] = multiPaciente;

        // Crea el AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modo multi-paciente:")
                .setMultiChoiceItems(dias, seleccionados, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        // Actualiza el estado actual de los elementos seleccionados
                        seleccionados[which] = isChecked;
                    }
                })
                .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        Log.e(TAG,"MultiPaciente selecionado: " + seleccionados[0]);
                        arrayMultiPaciente(seleccionados);
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Acción para el botón "Cancelar", si es necesario
                    }
                });

        // Muestra el AlertDialog
        builder.create().show();
    }

    private void arrayMultiPaciente(boolean[] seleccionados) {

        multipaciente = seleccionados[0];

        try {

            File pathPaciente = new File(FileAccess.getPATH_FILES(), FilePath.CONFIG_PACIENTE.getNameFile());
            Log.e(TAG,"pathPaciente: " + pathPaciente);

            if (FileFuntions.checkFileExist(pathPaciente.getName())) {
                JSONObject json_paciente = util.readFileJSON(pathPaciente);
                Log.e(TAG,"read paciente.json: " + json_paciente.toString());

                json_paciente.put("multipaciente",multipaciente);

                if (multipaciente) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            txtMultiPaciente.setVisibility(View.VISIBLE);
                            imgMultiPaciente.setImageResource(R.drawable.multiuser_active);
                        }
                    });
                }
                else {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            txtMultiPaciente.setVisibility(View.INVISIBLE);
                            imgMultiPaciente.setImageResource(R.drawable.multiuser_inactive);
                        }
                    });
                }

                util.writeFileJSON(pathPaciente, json_paciente.toString());
            }
            else { Log.e(TAG, "paciente.json no existe"); }
        }
        catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"JSONException: " + e);
        }catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"Exception: " + e);
        }
    }

    private void RowInvisibles() {
        if (SCALE_VISIBLE == false) {
            ocultarTableRow(this, R.id.tbrow_bascula);
            ocultarTableRow(this, R.id.tbrow_separador_bascula);
        }
        if (LUNG_VISIBLE == false) {
            ocultarTableRow(this, R.id.tbrow_lung);
            ocultarTableRow(this, R.id.tbrow_separador_lung);
        }
        if (ECG_VISIBLE == false) {
            ocultarTableRow(this, R.id.tbrow_ecg);
            ocultarTableRow(this, R.id.tbrow_separador_ecg);
        }
        if (PEAK_VISIBLE == false) {
            ocultarTableRow(this, R.id.tbrow_peak);
            ocultarTableRow(this, R.id.tbrow_separador_peak);
        }
    }

    private void ocultarTableRow(Activity activity, int tableRowId) {
        TableRow tableRow = activity.findViewById(tableRowId);
        if (tableRow != null) {
            tableRow.setVisibility(View.GONE); // Oculta la fila
        }
    }
}