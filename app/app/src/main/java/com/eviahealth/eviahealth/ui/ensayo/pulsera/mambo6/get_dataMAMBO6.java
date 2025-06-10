package com.eviahealth.eviahealth.ui.ensayo.pulsera.mambo6;

import static com.eviahealth.eviahealth.utils.util.toArrayByte;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FileFuntions;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.transtek.mb6.models.storage.CStepSleep;
import com.eviahealth.eviahealth.models.transtek.mb6.models.storage.JS2StepSummary;
import com.eviahealth.eviahealth.models.transtek.mb6.models.storage.JS4SleepReport;
import com.eviahealth.eviahealth.models.transtek.mb6.models.storage.MAPDesfases;
import com.eviahealth.eviahealth.models.transtek.mb6.models.storage.MAPStepSleep;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.devices.NombresDispositivo;
import com.eviahealth.eviahealth.models.transtek.mb6.data.M6Datapulsera;
import com.eviahealth.eviahealth.models.transtek.mb6.models.setting.CmdTranstek;
import com.eviahealth.eviahealth.models.transtek.mb6.models.setting.DeviceSetting;
import com.eviahealth.eviahealth.models.transtek.mb6.models.storage.JSBatteryInfo;
import com.eviahealth.eviahealth.models.transtek.mb6.models.storage.JSBloodOxygenData;
import com.eviahealth.eviahealth.models.transtek.mb6.models.storage.JSHeartRateData;
import com.eviahealth.eviahealth.models.transtek.mb6.models.storage.JSStepSummary;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;
import com.lifesense.plugin.ble.LSBluetoothManager;
import com.lifesense.plugin.ble.OnReadingListener;
import com.lifesense.plugin.ble.OnSearchingListener;
import com.lifesense.plugin.ble.OnSettingListener;
import com.lifesense.plugin.ble.OnSyncingListener;
import com.lifesense.plugin.ble.data.IDeviceData;
import com.lifesense.plugin.ble.data.LSConnectState;
import com.lifesense.plugin.ble.data.LSDeviceInfo;
import com.lifesense.plugin.ble.data.LSDeviceType;
import com.lifesense.plugin.ble.data.LSManagerStatus;
import com.lifesense.plugin.ble.data.LSProtocolType;
import com.lifesense.plugin.ble.data.bgm.BGDataSummary;
import com.lifesense.plugin.ble.data.bpm.LSBloodPressure;
import com.lifesense.plugin.ble.data.scale.LSScaleWeight;
import com.lifesense.plugin.ble.data.tracker.ATBacklightData;
import com.lifesense.plugin.ble.data.tracker.ATBatteryInfo;
import com.lifesense.plugin.ble.data.tracker.ATBloodOxygenData;
import com.lifesense.plugin.ble.data.tracker.ATBuriedPointSummary;
import com.lifesense.plugin.ble.data.tracker.ATChargeRecordData;
import com.lifesense.plugin.ble.data.tracker.ATDeviceData;
import com.lifesense.plugin.ble.data.tracker.ATDeviceInfo;
import com.lifesense.plugin.ble.data.tracker.ATDialStyleData;
import com.lifesense.plugin.ble.data.tracker.ATHeartRateData;
import com.lifesense.plugin.ble.data.tracker.ATSleepReportData;
import com.lifesense.plugin.ble.data.tracker.ATStepRecordData;
import com.lifesense.plugin.ble.data.tracker.ATStepSummary;
import com.lifesense.plugin.ble.data.tracker.ATUploadDoneNotify;
import com.lifesense.plugin.ble.data.tracker.setting.ATDataQueryCmd;
import com.lifesense.plugin.ble.data.tracker.setting.ATDataQuerySetting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class get_dataMAMBO6 extends BaseActivity implements View.OnClickListener {

    final static String TAG = "DATA-MAMBO6";
    final static Boolean DEBUG = false;
    private int idDebug = 0;

    //    String deviceName = "IF1 05E1";
    String deviceMac = ""; // "CE:23:58:51:05:E1";

    ProgressBar circulodescarga;
    TextView txtStatus;

    CountDownTimer cTimerView = null;       // Mostrar ProgressBar
    CountDownTimer cTimerSearch = null;     // TIMEOUT >> Busqueda de dispositivo
    CountDownTimer cTimerDownload = null;   // TIMEOUT >> Download Data

    private LSDeviceInfo currentDevice;
    private String querySetting = null;

    // data
    private ArrayList<ATStepSummary> dataStepSummary = new ArrayList<>();
    private ArrayList<ATStepSummary> stepSummaryDay = new ArrayList<>();
    private ArrayList<ATStepSummary> stepSummaryHour = new ArrayList<>();

    private ArrayList<ATSleepReportData> dataSleepReportData = new ArrayList<>();

    // Datos de frecuencia cardíaca
    private ArrayList<ATHeartRateData> dataHeartRateData = new ArrayList<>();
    // Datos de oxígeno en sangre
    private ArrayList<ATBloodOxygenData> dataBloodOxygenData = new ArrayList<>();

    M6Datapulsera datos;
    private Integer contConnecting = 0;
    TextToSpeechHelper textToSpeech;
    Boolean viewdata = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_data_mambo6);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Mantener la pantala activa
        EVLog.log(TAG,"onCreate()");

        //region :: View
        txtStatus = findViewById(R.id.MB6txtStatus_pul);
        txtStatus.setTextSize(14);
        txtStatus.setVisibility(View.VISIBLE);
        circulodescarga = findViewById(R.id.MB6circulodescarga);
        setVisibleProgressBar(View.INVISIBLE);
        //endregion

        viewdata = false;
        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        clearDataLists();

        datos = M6Datapulsera.getInstance();
        datos.clear();
        datos.setDownloadState(false);
        datos.setTimeOut(false);

        String DEVICE_MAC_ADDRESS = Config.getInstance().getIdentificador(NombresDispositivo.ACTIVIDAD);
        EVLog.log(TAG,"MAC ACTIVIDAD: "+ DEVICE_MAC_ADDRESS);
        if (DEVICE_MAC_ADDRESS.contains("MAMBO6-")){
            DEVICE_MAC_ADDRESS = DEVICE_MAC_ADDRESS.replace("MAMBO6-", "");
            deviceMac = util.MontarMAC(DEVICE_MAC_ADDRESS);
            EVLog.log(TAG,"MAC mDeviceAddress: "+ deviceMac);
        }

        //region :: Timer ProcressBar Visible (3 segudos) >> VISIBLE
        cTimerView = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimerView.cancel();
                cTimerView = null;
                setVisibleProgressBar(View.VISIBLE);
                setTextSatus("Buscando dispositivo.");
            }
        };
        cTimerView.start();
        //endregion

        setTextSatus("Iniciando");

        //init LSBluetoothManager Transtek
        LSBluetoothManager.getInstance().initManager(getApplicationContext());
        //register bluetooth state change receiver
        LSBluetoothManager.getInstance().registerBluetoothReceiver(getApplicationContext());

        //region :: TIMER >> TIMEOUT Search Device 20 SEG
        Integer timeout = 1000 * 20; // x segundos
        cTimerSearch = new CountDownTimer(timeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimerSearch.cancel();
                cTimerSearch = null;

                LSBluetoothManager.getInstance().stopSearch(); // Detiene busqueda del dispositivo

                EVLog.log(TAG, "TIMEOUT: SEARCH DEVICE");
//                datos.setDownloadState(false);
//                datos.setTimeOut(true);
//                datos.setERROR_ACT("{ \"type\":\"MAMBO6\",\"error\":800 }");
//                viewResult();

                currentDevice = new LSDeviceInfo();
                currentDevice.setMacAddress(deviceMac);
                currentDevice.setBroadcastID(deviceMac.replace(":", ""));
                currentDevice.setDeviceType(LSDeviceType.ActivityTracker.getValue());
                currentDevice.setProtocolType(LSProtocolType.A5.toString());
                currentDevice.setDeviceName("IF1");

                EVLog.log(TAG,"Send connect device: " + deviceMac);
                connectDevice();
            }
        };
        //endregion

        //region :: TIMER >> TIMEOUT Download Data
        timeout = 1000 * 30; // x segundos
        cTimerDownload = new CountDownTimer(timeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                cTimerSearch.cancel();
                cTimerSearch = null;

                LSBluetoothManager.getInstance().stopSearch(); // Detiene busqueda del dispositivo

                EVLog.log(TAG, "TIMEOUT: DOWNLOAD DATA");
                datos.setDownloadState(false);
                datos.setTimeOut(true);
                datos.setERROR_ACT("{ \"type\":\"MAMBO6\",\"error\":804 }");
                viewResult();
            }
        };
        //endregion

        if (!DEBUG) {
            // Scan bluetooth device
            LSManagerStatus sdkStatus = LSBluetoothManager.getInstance().getManagerStatus();
            if (sdkStatus == LSManagerStatus.Free) {
                //scan activity tracker device
                List<LSDeviceType> types = new ArrayList<LSDeviceType>();
                types.add(LSDeviceType.ActivityTracker);
                LSBluetoothManager.getInstance().clearScanCache();
                LSBluetoothManager.getInstance().searchDevice(types, mSearchCallback);
                cTimerSearch.start(); // Enable timer timeout shearch
            }
        }
        else {
            // Cargamos datos fijos
            stepSummaryDay.clear();
//            setDebugDataStepOfDay();

            setDebugDataStepOfHour();
            setDebugDataSleepRecord();

            dataHeartRateData.clear();
//            setDebugDataHeartRate();

            setDebugDataBloodOxygen();

            gestionarDatos();
        }

        DeviceSetting.initActivityContext(this);
    }

    private void clearDataLists() {
        dataStepSummary.clear();
        stepSummaryDay.clear();
        stepSummaryHour.clear();
        dataSleepReportData.clear();

        dataHeartRateData.clear();
        dataBloodOxygenData.clear();
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

    //region :: The activity lifecycle
    @Override
    protected void onResume() {
        super.onResume();
        EVLog.log(TAG,"onResume()");

        // Comprobación si la fecha que se inició el ensayo en la actual
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
            EVLog.log(TAG, "FILEACCESS READ: IOException: " + ex.toString());
        }
    }

    @Override
    protected  void onStop() {
        super.onStop();
        EVLog.log(TAG,"onStop()");

        LSManagerStatus sdkStatus= LSBluetoothManager.getInstance().getManagerStatus();
        if(sdkStatus == LSManagerStatus.Scanning){
            //stop scan
            Log.e(TAG,"stop scan");
            LSBluetoothManager.getInstance().stopSearch();
        }
        if(sdkStatus == LSManagerStatus.Syncing){
            //stop device data sync
            Log.e(TAG,"stop device data sync");
            LSBluetoothManager.getInstance().stopDeviceSync();
        }
    }

    @Override
    protected  void onPause() {
        super.onPause();
        Log.e(TAG,"onPause()");
    }

    @Override
    protected void onDestroy() {

        EVLog.log(TAG,"onDestroy()");

        if (cTimerView != null) cTimerView.cancel();
        if (cTimerSearch != null) cTimerSearch.cancel();
        if (cTimerDownload != null) cTimerDownload.cancel();

        textToSpeech.shutdown();

        LSManagerStatus sdkStatus=LSBluetoothManager.getInstance().getManagerStatus();
        if(sdkStatus == LSManagerStatus.Scanning){
            //stop scan
            LSBluetoothManager.getInstance().stopSearch();
        }
        if(sdkStatus == LSManagerStatus.Syncing){
            //stop device data sync
            LSBluetoothManager.getInstance().stopDeviceSync();
        }

        LSBluetoothManager.getInstance().unregisterBluetoothReceiver();

        super.onDestroy();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){
    }
    //endregion

    private void setTextSatus(String texto) {
        runOnUiThread(new Runnable() {
            public void run() {
                txtStatus.setText(texto);
            }
        });
    }

    private void setVisibleProgressBar(int state) {
        runOnUiThread(new Runnable() {
            public void run() {
                circulodescarga.setVisibility(state);
            }
        });
    }

    /**
     * Reinicia el Timer de descarga de datos
     */
    private void reloadTimerDownload(){
        if (cTimerDownload != null) {
            cTimerDownload.cancel();
            cTimerDownload.start();
        }
    }

    public void viewResult() {

        if (viewdata == false) {
            viewdata = true;
            if (cTimerView != null) cTimerView.cancel();
            if (cTimerSearch != null) cTimerSearch.cancel();
            if (cTimerDownload != null) cTimerDownload.cancel();

            EVLog.log(TAG, " ViewResult()");

            if (datos.getDownloadState()) {
                Intent in = new Intent(this, view_dataMAMBO6.class);
                startActivity(in);
            }
            else {
                Intent in = new Intent(this, view_failMAMBO6.class);
                startActivity(in);
            }

            EVLog.log(TAG, " ViewResult() >> finish()");
            setVisibleProgressBar(View.INVISIBLE);
            finish();
        }
    }

    public void BluetoothError() {
        datos.setDownloadState(false);
        datos.setERROR_ACT("{ \"type\":\"MAMBO6\",\"error\":801 }");
        viewResult();
    }

    private void connectDevice() {

        contConnecting = 0;
        if (!LSBluetoothManager.getInstance().isSupportBLE()) {
            Toast.makeText(getApplicationContext(), "Bluetooth low energy no soportado", Toast.LENGTH_LONG).show();
            EVLog.log(TAG, "ERROR BLUETOOTH: Bluetooth low energy no soportado");
            BluetoothError();
            return;
        }
        if (!LSBluetoothManager.getInstance().isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext(), "Bluetooth desactivado", Toast.LENGTH_LONG).show();
            EVLog.log(TAG, "ERROR BLUETOOTH: Bluetooth desactivado");
            BluetoothError();
            return;
        }
        if (currentDevice == null) {
            Toast.makeText(getApplicationContext(), "No detectado dispositivo!", Toast.LENGTH_LONG).show();
            EVLog.log(TAG, "ERROR BLUETOOTH: No detectado dispositivo");
            BluetoothError();
            return;
        }
        if ( LSBluetoothManager.getInstance().getManagerStatus() == LSManagerStatus.Syncing
                && LSBluetoothManager.getInstance().checkConnectState(currentDevice.getMacAddress()) == LSConnectState.ConnectSuccess) {
            LSBluetoothManager.getInstance().resetSyncingListener(mSyncingCallback);
            EVLog.log(TAG, "ERROR: El Dispositivo ya está conectado.");
            BluetoothError();
            return;
        }

        // Obtener el estado de funcionamiento
        if (LSBluetoothManager.getInstance().getManagerStatus() == LSManagerStatus.Syncing) {
            // Estado de sincronización de los datos del dispositivo
            EVLog.log(TAG, "ERROR: El Dispositivo sincronizando datos");
            BluetoothError();
            return;
        }

        createConnection();
    }

    private void createConnection(){

        EVLog.log(TAG,"device MAC >> "+currentDevice.getMacAddress());
        setTextSatus("Iniciando");

        // Stop
        LSBluetoothManager.getInstance().stopDeviceSync();

        //clear measure device list
        LSBluetoothManager.getInstance().setDevices(null);

        //add target measurement devicef
        LSBluetoothManager.getInstance().addDevice(currentDevice);
        //start data syncing service
        LSBluetoothManager.getInstance().startDeviceSync(mSyncingCallback);

        EVLog.log(TAG, "startDeviceSync()");
    }

    private Boolean downloadBatteryInfo(){
        cTimerDownload.start();
        boolean isSuccess = readDeviceBattery(deviceMac, new OnReadingListener() {
            @Override
            public void onDeviceBatteryInfoUpdate(String deviceMac, ATBatteryInfo batteryInfo) {
//                Log.e(TAG,"DeviceBatteryInfo = {voltage:" + batteryInfo.getVoltage() + ", percentage:" + batteryInfo.getBattery() + "}");

                JSBatteryInfo dataBattery = new JSBatteryInfo(batteryInfo);
                String message = dataBattery.toString();

                EVLog.log(TAG,"DeviceBatteryInfo = " + message);

                datos.setBatteryData(message);

                // Lanza la descarga de los todos los datos de la pulsera
                downloadAllData();
            }
        });
        if (!isSuccess) {
            EVLog.log(TAG,"Fail Download Battery Info");
            datos.setDownloadState(false);
            datos.setERROR_ACT("{ \"type\":\"MAMBO6\",\"error\":100 }");
            viewResult();
        }
        return isSuccess;
    }

    private Boolean downloadAllData() {

        reloadTimerDownload();

        LSConnectState state = LSBluetoothManager.getInstance().checkConnectState(deviceMac);
        if(state == LSConnectState.ConnectSuccess) {
            EVLog.log(TAG, "downloadAllData()");
            setTextSatus("Download data: All");

            querySetting = "downloadAllData";
            ATDataQuerySetting querySetting = new ATDataQuerySetting(ATDataQueryCmd.All);
            LSBluetoothManager.getInstance().queryDeviceData(deviceMac, querySetting, mSettingListener);
        }
        else{
            EVLog.log(TAG,"Device is not connected");
            datos.setDownloadState(false);
            datos.setERROR_ACT("{ \"type\":\"MAMBO6\",\"error\":-1 }");
            viewResult();
            return false;
        }
        return true;
    }

    private void gestionarDatos(){
        Handler mainHandler = new Handler();
        mainHandler.post(new Runnable() {
            @Override
            public void run() {

                Log.e(TAG, "gestionarDatos()");
                if (cTimerDownload != null) {
                    cTimerDownload.cancel();
                    cTimerDownload = null;
                }

                setTextSatus("Parseando: StepOfDay");
                parseStepOfDay();
                setTextSatus("Parseando: StepOfHour");
                parseStepOfHour();
                setTextSatus("Parseando: Sleep");
                parseSleep();
                setTextSatus("Parseando: eartRate");
                parseHeartRate();
                setTextSatus("Parseando: BloodOxygen");
                parseBloodOxygen();

                setTextSatus("Update Datetime");

                if (DEBUG) {
                    datos.setDownloadState(true);
                    viewResult();
                }
                else {
                    querySetting = "setUpdateDateTime";
                    DeviceSetting.setUpdateDateTime(deviceMac,mSettingListener);
                }

            }
        });
    }

    private MAPDesfases desfases;

    private void parseStepOfHour(){

        String message = "{}";
        String message2 = "{}";
        if (stepSummaryHour.size() > 0) {

            JS2StepSummary stepHour = new JS2StepSummary(stepSummaryHour);

            EVLog.log(TAG,"1 ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo COMPENSADO DESFASE");
            List<CStepSleep> listSteps = stepHour.getListSteps();
            for (CStepSleep r: listSteps) {
                EVLog.log(TAG, "    " + r.toString());
            }
            EVLog.log(TAG,"2 ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo");

            Log.e("", "********** DESFACES **********");
            desfases = stepHour.getDesfases();

//          Mapa único de fechas con los datos de steps y sleep
            MAPStepSleep listStepSleep = new MAPStepSleep();
            for (CStepSleep obj: listSteps) {
                Log.e("", "    " + obj.toString());
                listStepSleep.add(obj.getDate(),obj);
            }
            EVLog.log(TAG, "END PARSE STEPS *******");

//            String paciente = Config.getInstance().getIdPacienteTablet();
            String paciente = Config.getInstance().getIdPacienteEnsayo();
            Integer dispositivo = 4;

            message = listStepSleep.getRecordsSteps();
            message2 = listStepSleep.getRecordsSteps(paciente,dispositivo);
        }

        datos.setStepOfHourData(message);
        // Bloque de datos completos para procesar
        EVLog.log(TAG, "JSON Steps: " + message2);
        datos.setStepOfHourDataComplet(message2);
    }

    private void parseStepOfDay(){
        String message = "{}";

        if (stepSummaryDay.size() > 0) {
            JSStepSummary stepDay = new JSStepSummary(stepSummaryDay);
            stepDay.adapterDateHours(); // FOR RECORDS SECONDS = 00

//        String message = stepDay.toString(); // Total steps varios dias

            message = stepDay.getStepToday(); // Pasos de hoy sino existe "{}"
            EVLog.log(TAG, "JSON Steps Day: " + message);
        }

        datos.setStepOfDayData(message);
    }

    private void parseSleep(){
        String message = "{}";
        String message2 = "{}";

        EVLog.log(TAG,"SLEEP ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo");
        if (dataSleepReportData.size() > 0) {

            MAPDesfases desfasesTotal = new MAPDesfases();

            //region :: Obtenemos desfases de la descarga anterior
            try {
                String content = FileFuntions.getSleepDesfaces();

//                Log.e(TAG,"Desfases URI: " + content);
                // Generamos desface de la descarga anterior
                if (content != null) {
                    EVLog.log(TAG,"desfase anterior cargado: " + content);
                    String[] tt = content.split("\n");
                    for (String l: tt) {
                        String[] d = l.split(";"); // date;desfase
//                        Log.e(TAG,"desfase anterior: " + d[1]);
                        desfasesTotal.add(d[0],Integer.parseInt(d[1]));
                    }
                    EVLog.log(TAG,"---------------------------------------------------");
                }
                else {
                    Log.e (TAG, "......(II) Desfases descarga anterior: null");
                    desfasesTotal.clear();
                }

                // Añadimos desfase actual >> [defase total] = [desfase de la descarga anterior] + [desfase de esta descarga]
                List<String> lDesfase = desfases.getListMapDesfases();
                for (String l: lDesfase) {
                    String[] d = l.split(";"); // date;desfase
                    desfasesTotal.add(d[0],Integer.parseInt(d[1]));
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                EVLog.log(TAG,"(EX) Exception, parseando datos de desfase de la descarga anterior");
//                Log.e(TAG,"(EX) Exception, parseando datos de desfase de la descarga anterior");
                desfasesTotal.clear();
            }
            //endregion

            JS4SleepReport tSleep = new JS4SleepReport(dataSleepReportData,desfasesTotal);
            EVLog.log(TAG,"2 ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo");

            ArrayList<CStepSleep> SleepHours = tSleep.getListSleepHour();
            Log.e("", "---- List<CStepSleep> SleepHours Compensado ----");

            // Mapa único de fechas con los datos de steps y sleep
            MAPStepSleep listStepSleep = new MAPStepSleep();
            for (CStepSleep obj: SleepHours) {
                EVLog.log(TAG, "CStepSleep: " + obj.toString());
                listStepSleep.add(obj.getDate(),obj);
            }

            EVLog.log(TAG, "******** FRACIONAR EN 5 SLEEP *******");
            message = listStepSleep.getRecordsSleep();

            String paciente = Config.getInstance().getIdPacienteTablet();
            Integer dispositivo = 5;
            message2 = listStepSleep.getRecordsSleep(paciente,dispositivo);

            //region :: URL guarda desfaces de esta descarga en proveedor de contenido
            String url_desface = null;
            for (String l: desfases.getListMapDesfases()) {
                if (url_desface == null) { url_desface = l + "\n"; } else { url_desface += l + "\n"; }
            }
            try {
                FileFuntions.setSleepDesfaces(url_desface);
            }
            catch (Exception e) {
                e.printStackTrace();
                EVLog.log(TAG, "********** EXCEPTION parseSleep(1): " + e.toString());
            }
            //endregion

        }
        else {
            // NO hay datos de sleep en esta descarga
            try {
                FileFuntions.setSleepDesfaces(null);
            }
            catch (Exception e) {
                e.printStackTrace();
                EVLog.log(TAG, "********** EXCEPTION parseSleep(2): " + e.toString());
            }
        }

        Log.e(TAG,"SLEEP: " + message);
        datos.setSleepData(message);
        datos.setSleepDataComplet(message2);
    }

    private void parseHeartRate(){
        String message = "{}";

        if (dataHeartRateData.size() > 0) {
            JSHeartRateData list = new JSHeartRateData(dataHeartRateData);
            String paciente = Config.getInstance().getIdPacienteTablet();
            message = list.toString(paciente);
            EVLog.log(TAG, "JSON HeartRate: " + message);
        }
        datos.setHeartRateData(message);
    }

    private void parseBloodOxygen(){
        String message = "{}";

        if (dataBloodOxygenData.size() > 0) {
            JSBloodOxygenData list = new JSBloodOxygenData(dataBloodOxygenData);
            String paciente = Config.getInstance().getIdPacienteTablet();
            message = list.toString(paciente);
            EVLog.log(TAG, "JSON BloodOxygen: " + message);
        }
        datos.setBloodOxygenData(message);
    }

    //region :: CALLBACKS
    // CallBack SCAN device
    private OnSearchingListener mSearchCallback=new OnSearchingListener() {

        // Devolver los resultados de la exploración
        @Override
        public void onSearchResults(LSDeviceInfo lsDevice) {

//            Log.e(TAG,"onSearchResults >> onSearchResults()");
            if(lsDevice==null){
                return ;
            }

            Log.e(TAG,"onSearchResults >> "+ lsDevice.getDeviceName() + " [" + lsDevice.getMacAddress() + "]");
            if (lsDevice.getMacAddress().equals(deviceMac)) {

                if (cTimerSearch != null) {
                    cTimerSearch.cancel();
                    cTimerSearch = null;
                }
                currentDevice = lsDevice;
                Log.e(TAG,"currentDevice name: " + currentDevice.getDeviceName());
                Log.e(TAG,"currentDevice address: " + currentDevice.getMacAddress());
                Log.e(TAG,"currentDevice DeviceType: " + currentDevice.getDeviceType());
                LSBluetoothManager.getInstance().stopSearch();

                EVLog.log(TAG,"Send connect device: " + deviceMac);
                connectDevice();
            }
        }

        // Devolución de resultados de dispositivos conectados al sistema
        @Override
        public void onSystemConnectedDevice(String deviceName, String deviceMac) {
            Log.e("LS-BLE","OnSearchingListener >> onSystemConnectedDevice >> "+deviceMac+"; ["+deviceName+"]");
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
//            if(device!=null){
//                LSDeviceInfo lsDevice=new LSDeviceInfo();
//                lsDevice.setDeviceName(device.getName());
//                lsDevice.setDeviceType(LSDeviceType.ActivityTracker.getValue());
//                lsDevice.setMacAddress(device.getAddress());
//                String broadcastId=device.getAddress()!=null?device.getAddress().replace(":",""):device.getAddress();
//                lsDevice.setBroadcastID(broadcastId);
//                lsDevice.setProtocolType(LSProtocolType.A5.toString());
//                lsDevice.setPairStatus(2);
            //update scan results
            //    updateScanResults(lsDevice);
//            }
        }

        // Interfaz clásica de retorno de resultados de exploración Bluetooth 2.0
        @SuppressLint("MissingPermission")
        @Override
        public void onBluetoothDeviceFound(BluetoothDevice device) {
            Log.e("LS-BLE","OnSearchingListener >> onBluetoothDeviceFound >> "+device);
        }
    };

    // Device Setting Listener
    private OnSettingListener mSettingListener = new OnSettingListener() {

        // Establecer devolución de llamada fallida
        @Override
        public void onFailure(int errorCode) {
            Log.e(TAG,"OnSettingListener >> onFailure()");
            Log.e(TAG,"Setting Failed >> errorCode=" + errorCode);
        }

        // Establecer devolución de llamada de éxito
        @Override
        public void onSuccess(String mac) {

            Log.e(TAG,"OnSettingListener >> onSuccess()");

            if (querySetting == null) { return; }

            Log.e(TAG,"Setting Success: " + querySetting);

            if (querySetting.equals("downloadAllData")) {
                querySetting = null;
            }
            else if (querySetting.equals("setUpdateDateTime")) {
                querySetting = null;
                datos.setDownloadState(true);
                viewResult();
            }

        }

        // Devolución de llamada de progreso de descarga de archivos o sincronización de datos
        @Override
        public void onProgressUpdate(String deviceMac, int value) {
            Log.e(TAG,"OnSettingListener >> onProgressUpdate()");
        }

        // Llamada de retorno al estado de descarga de archivos o sincronización de datos
        @Override
        public void onStateChanged(String deviceMac, int state, int errorCode) {
            Log.e(TAG,"OnSettingListener >> onStateChanged()");
            Log.e(TAG,"onStateChanged >> "+state + " ; errorCode="+errorCode);
        }

        // Llamada de datos del dispositivo
        @Override
        public void onDataUpdate(Object obj) {
            Log.e(TAG,"OnSettingListener >> onDataUpdate()");
//            if(obj!=null && obj instanceof ATDeviceData){
//                ATDeviceData data = (ATDeviceData)obj;
//                showDeviceMeasuringData(data, DeviceDataUtils.byte2hexString(data.getSrcData()));
//            }
//            else{
//                showDeviceMeasuringData(obj,null);
//            }
        }
    };

    // CallBack Estado Conecxión y Data receiver
    private OnSyncingListener mSyncingCallback = new OnSyncingListener() {

        // Devolución de llamada de cambios de estado de conexión del dispositivo
        @Override
        public void onStateChanged(String broadcastId, LSConnectState state) {
            super.onStateChanged(broadcastId, state);

            // LSConnectState >> [Unknown(0), Connecting(1), GattConnected(2), ConnectSuccess(3), ConnectFailure(4), Disconnect(5), RequestDisconnect(6)]
            Log.e(TAG,"OnSyncingListener >> onStateChanged() state: " + state.name());

            if (LSConnectState.Connecting == state) {
                EVLog.log(TAG,"STATE: Connecting");

                contConnecting += 1;
                setTextSatus("STATE: Connecting " + contConnecting.toString());
                if (contConnecting == 2) {
                    LSManagerStatus sdkStatus=LSBluetoothManager.getInstance().getManagerStatus();
                    Log.e(TAG,"sdkStatus: " + sdkStatus.toString());
                    if(sdkStatus == LSManagerStatus.Scanning){
                        //stop scan
                        LSBluetoothManager.getInstance().stopSearch();
                    }
                    if(sdkStatus == LSManagerStatus.Syncing){
                        //stop device data sync
                        LSBluetoothManager.getInstance().stopDeviceSync();
                        datos.setDownloadState(false);
                        datos.setERROR_ACT("{ \"type\":\"MAMBO6\",\"error\":802 }");
                        Log.e(TAG,"-----1: " + viewdata);
                        viewResult();
                    }
                    contConnecting = 0;
                }
            }
            else if (LSConnectState.GattConnected == state) {
                EVLog.log(TAG,"STATE: GattConnected");
            }
            else if (LSConnectState.ConnectSuccess == state) {
                EVLog.log(TAG,"STATE: Connect Device Success");
                setTextSatus("Download data: battery info");
                downloadBatteryInfo();
            }
            else if (LSConnectState.ConnectFailure == state) {
                EVLog.log(TAG,"STATE: Connect Device Failure");
                datos.setDownloadState(false);
                datos.setERROR_ACT("{ \"type\":\"MAMBO6\",\"error\":802 }");
                viewResult();
            }
            else if (LSConnectState.Disconnect == state) {
                EVLog.log(TAG, "STATE: Device Disconnect");
//                if (LSConnectState.Connecting == state) {
//                    datos.setDownloadState(false);
//                    datos.setERROR_ACT("{ \"type\":\"MAMBO6\",\"error\":800 }");
//                    Log.e(TAG,"-----2: " + viewdata);
//                    viewResult();
//                }
            }
            else if (LSConnectState.RequestDisconnect == state) {
                // Solicitar una desconexión
                EVLog.log(TAG, "STATE: Request Disconnect");
//                datos.setDownloadState(false);
//                datos.setERROR_ACT("{ \"type\":\"MAMBO6\",\"error\":803 }");
//                viewResult();
            }
            else {
                EVLog.log(TAG, "State Change Device: Unknown");
                datos.setDownloadState(false);
                datos.setERROR_ACT("{ \"type\":\"MAMBO6\",\"error\":-1 }");
                viewResult();
            }
        }

        // Devolución de los datos de medición de la pulsera de actividad o del reloj.
        @Override
        public void onActivityTrackerDataUpdate(String mac, int type, ATDeviceData data){
            super.onActivityTrackerDataUpdate(mac, type, data);

            if (data == null) {
                Log.e(TAG,"OnSyncingListener >> onActivityTrackerDataUpdate() >> data = NULL");
                return;
            }

            EVLog.log(TAG,"onActivityTrackerDataUpdate() data >> " + data.toString());

            // Success Notification
            if(data instanceof ATUploadDoneNotify){
                // EVENTO DE FINALIZACIÓN DE UNA CONSULTA
                Log.e(TAG,"onActivityTrackerDataUpdate() type >> " + data.toString());
                ATUploadDoneNotify complete = (ATUploadDoneNotify)data;
                ATDataQueryCmd typeData = complete.getDataType();   // Tipo de datos en la notificacion

                reloadTimerDownload();

                if (typeData.name().equals("HeartRate")) {
                    Log.e(TAG,"ATUploadDoneNotify() HeartRate Success");
                    // data type: ATHeartRateData
                }
                else if (typeData.name().equals("ExerciseSpeed")) {
                    Log.e(TAG,"ATUploadDoneNotify() ExerciseSpeed Success");
                    // data type: ???
                }
                else if (typeData.name().equals("ExerciseHeartRate")) {
                    Log.e(TAG,"ATUploadDoneNotify() ExerciseHeartRate Success");
                    // data type: ???
                }
                else if (typeData.name().equals("ExerciseCalories")) {
                    Log.e(TAG,"ATUploadDoneNotify() ExerciseCalories Success");
                    // data type: ???
                }
                else if (typeData.name().equals("StepRecordOfHistory")) {
                    Log.e(TAG,"ATUploadDoneNotify() StepRecordOfHistory Success");
                    // data type: ATStepRecordData

//                    for (ATStepRecordData obj: dataStepRecordData){
//                        stepHistoryData.add(obj);
//                    }
//                    // Para recibir los datos de StepRecord
//                    dataStepRecordData.clear();
                }
                else if (typeData.name().equals("StepRecord")) {
                    Log.e(TAG,"ATUploadDoneNotify() StepRecord Success");
                    // data type: ATStepRecordData
//                    for (ATStepRecordData obj: dataStepRecordData){
//                        stepRecordData.add(obj);
//                    }
                    dataStepSummary.clear();
                }
                else if (typeData.name().equals("HeartRateRecord")) {
                    Log.e(TAG,"ATUploadDoneNotify() HeartRateRecord Success");
                    // data type: ATHeartRateData
                }
                else if (typeData.name().equals("CharageRecord")) {
                    Log.e(TAG,"ATUploadDoneNotify() CharageRecord Success");
                    // data type: ATChargeRecordData
                }
                else if (typeData.name().equals("BacklightBrightness")) {
                    Log.e(TAG,"ATUploadDoneNotify() BacklightBrightness Success");
                    // data type: ATBacklightData
                }
                else if (typeData.name().equals("DialStyle")) {
                    Log.e(TAG,"ATUploadDoneNotify() DialStyle Success");
                    // data type: ATDialStyleData
                }
                else if (typeData.name().equals("ExerciseStep")) {
                    Log.e(TAG,"ATUploadDoneNotify() ExerciseStep Success");
                    // data type: ???
                }
                else if (typeData.name().equals("ExerciseSpeedWithImperial")) {
                    Log.e(TAG,"ATUploadDoneNotify() ExerciseSpeedWithImperial Success");
                    // data type: ???
                }
                else if (typeData.name().equals("HeartRateZone")) {
                    Log.e(TAG,"ATUploadDoneNotify() HeartRateZone Success");
                    // data type: ATHeartRateData

                    setTextSatus("Download data: StepOfHour");
                }
                else if (typeData.name().equals("StepOfHour")) {
                    EVLog.log(TAG,"ATUploadDoneNotify() StepOfHour Success");

                    // data type: ATStepSummary
                    for (ATStepSummary obj: dataStepSummary) {
                        // filtramos para que solo entren registro de horas
                        if (obj.getCmd() == CmdTranstek.SetpOfHour.getValue()) {
                            stepSummaryHour.add(obj);
                        }
                    }

                    setTextSatus("Download data: StepOfDay");
                    // limpiamos para recibir los registros de dia
                    dataStepSummary.clear();
                }
                else if (typeData.name().equals("StepOfDay")) {
                    EVLog.log(TAG,"ATUploadDoneNotify() StepOfDay Success");
                    // data type: ATStepSummary
                    for (ATStepSummary obj: dataStepSummary) {
                        // filtramos para que solo entren registro de dia
                        if (obj.getCmd() == CmdTranstek.StepOfDay.getValue()) {
                            stepSummaryDay.add(obj);
                        }
                    }
                    setTextSatus("Download data: SleepReport");
                }
                else if (typeData.name().equals("Exercise")) {
                    Log.e(TAG,"ATUploadDoneNotify() Exercise Success");
                    // data type: ???
                }
                else if (typeData.name().equals("BloodOxygen")) {
                    Log.e(TAG,"ATUploadDoneNotify() BloodOxygen Success");
                    // data type: ATBloodOxygenData
                }
                else if (typeData.name().equals("Meditation")) {
                    Log.e(TAG,"ATUploadDoneNotify() Meditation Success");
                    // data type: ???
                }
                else if (typeData.name().equals("SleepReport")) {
                    EVLog.log(TAG,"ATUploadDoneNotify() SleepReport Success");
                    // data type: ATSleepReportData
                    setTextSatus("Download data: ContinuousBloodOxygen");
                }
                else if (typeData.name().equals("BloodOxygenRecord")) {
                    Log.e(TAG,"ATUploadDoneNotify() BloodOxygenRecord Success");
                    // data type: ???
                }
                else if (typeData.name().equals("ContinuousBloodOxygen")) {
                    EVLog.log(TAG,"ATUploadDoneNotify() ContinuousBloodOxygen Success");
                    // data type: ATBloodOxygenData
                }
                else if (typeData.name().equals("BuriedPoint")) {
                    Log.e(TAG,"ATUploadDoneNotify() BuriedPoint Success");
                    // data type: ???
                }
                else if (typeData.name().equals("BuriedPointSummary")) {
                    Log.e(TAG,"ATUploadDoneNotify() BuriedPointSummary Success");
                    // data type: ATBuriedPointSummary
                }
                else if (typeData.name().equals("All")) {
                    EVLog.log(TAG,"ATUploadDoneNotify() All Success");
                    // Descarga finalizada
                    setTextSatus("Download data: Finish");
                    gestionarDatos();
                }
                // Other Query ?
                else {
                    Log.e(TAG,"ATUploadDoneNotify() typeData >> " + typeData.name() + " - Success");
                }
            }

            // Data from: StepRecordOfHistory, StepRecord
            else if (data instanceof ATStepRecordData) {
                Log.e(TAG,"onActivityTrackerDataUpdate() data >> ATStepRecordData");
                ATStepRecordData obj = (ATStepRecordData) data;
//                dataStepRecordData.add(obj);
                reloadTimerDownload();
            }
            // Data from: StepOfHour, StepOfDay
            else if (data instanceof ATStepSummary) {
                Log.e(TAG,"onActivityTrackerDataUpdate() data >> ATStepSummary");
                ATStepSummary obj = (ATStepSummary) data;
                EVLog.log(TAG,"contentData: " + util.getContentData(obj.getSrcData()));
                dataStepSummary.add(obj);
                reloadTimerDownload();
            }
            // Data from: SleepReport
            else if (data instanceof ATSleepReportData) {
                Log.e(TAG,"onActivityTrackerDataUpdate() data >> ATSleepReportData");
                ATSleepReportData obj = (ATSleepReportData) data;
                EVLog.log(TAG,"contentData: " + util.getContentData(obj.getSrcData()));
                dataSleepReportData.add(obj);
                reloadTimerDownload();
            }
            // Data from: ???, alguna veces salta cuando se realiza una conexión otras no
            else if( data instanceof ATDeviceInfo){
                Log.e(TAG,"onActivityTrackerDataUpdate() data >> ATDeviceInfo");
                ATDeviceInfo deviceInfo = (ATDeviceInfo)data;
                Log.e(TAG,"data >> " + deviceInfo.toString());
            }
            // Data from: HeartRate, HeartRateRecord, HeartRateZone
            else if (data instanceof ATHeartRateData) {
                Log.e(TAG,"onActivityTrackerDataUpdate() data >> ATHeartRateData");
                ATHeartRateData obj = (ATHeartRateData) data;
                EVLog.log(TAG,"contentData: " + util.getContentData(obj.getSrcData()));
                dataHeartRateData.add(obj);
                reloadTimerDownload();
            }
            // Data from: CharageRecord
            else if (data instanceof ATChargeRecordData) {
                Log.e(TAG,"onActivityTrackerDataUpdate() data >> ATChargeRecordData");
                ATChargeRecordData obj = (ATChargeRecordData) data;
                reloadTimerDownload();
            }
            // Data from: BackLightBrightness
            else if (data instanceof ATBacklightData) {
                Log.e(TAG,"onActivityTrackerDataUpdate() data >> ATBacklightData");
                ATBacklightData obj = (ATBacklightData) data;
                reloadTimerDownload();
            }
            // Data from: DialStyle
            else if (data instanceof ATDialStyleData) {
                Log.e(TAG,"onActivityTrackerDataUpdate() data >> ATDialStyleData");
                ATDialStyleData obj = (ATDialStyleData) data;
                reloadTimerDownload();
            }
            // Data from: BloodOxygen, ContinuousBloodOxygen
            else if (data instanceof ATBloodOxygenData) {
                Log.e(TAG,"onActivityTrackerDataUpdate() data >> ATBloodOxygenData");
                ATBloodOxygenData obj = (ATBloodOxygenData) data;
                EVLog.log(TAG,"contentData: " + util.getContentData(obj.getSrcData()));
                dataBloodOxygenData.add(obj);
                reloadTimerDownload();
            }
            // Data from: BuriedPointSummary
            else if (data instanceof ATBuriedPointSummary) {
                Log.e(TAG,"onActivityTrackerDataUpdate() data type >> ATBuriedPointSummary");
                ATBuriedPointSummary obj = (ATBuriedPointSummary) data;
                reloadTimerDownload();
            }

//            EVLog.log(TAG,"onActivityTrackerDataUpdate() data >> " + data.toString());
        }

        //region :: Others Receivers funtions NO USADAS

        // actualización de la información del dispositivo
        @Override
        public void onDeviceInformationUpdate(String broadcastId, LSDeviceInfo lsDevice) {
            super.onDeviceInformationUpdate(broadcastId, lsDevice);

            Log.e(TAG,"OnSyncingListener >> onDeviceInformationUpdate()");

            if (lsDevice == null || currentDevice == null) {
                return;
            }
            Log.e("LS-BLE", "Demo-Update Device Info:" + lsDevice.toString());
            //update and reset device's firmware version
            currentDevice.setFirmwareVersion(lsDevice.getFirmwareVersion());
            currentDevice.setHardwareVersion(lsDevice.getHardwareVersion());
            currentDevice.setModelNumber(lsDevice.getModelNumber());
            if(lsDevice.getPassword()!=null){
                currentDevice.setPassword(lsDevice.getPassword());
            }
//            if (getActivity() != null) {
//                //show device information
//                StringBuffer strBuffer = new StringBuffer();
//                strBuffer.append("Device Version Information....,");
//                strBuffer.append("ModelNumber:" + currentDevice.getModelNumber() + ",");
//                strBuffer.append("firmwareVersion:" + currentDevice.getFirmwareVersion() + ",");
//                strBuffer.append("hardwareVersion:" + currentDevice.getHardwareVersion() + ",");
//                showDeviceMeasuringData(strBuffer, null);
//            }
//            //refresh menu
//            mainHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    getActivity().invalidateOptionsMenu();
//                }
//            });
        }

        // Otras retrollamadas de datos de notificación, como el estado de la medición o información sobre errores.
        @Override
        public void onNotificationDataUpdate(String devMac, IDeviceData obj) {

            Log.e(TAG,"OnSyncingListener >> onNotificationDataUpdate()");
            Log.e(TAG,"onNotificationDataUpdate >> " + obj.toString());

        }

        // Devolución de datos de medición de la tensión arterial
        @Override
        public void onBloodPressureDataUpdate(String broadcastId, LSBloodPressure bloodPressure) {
            Log.e(TAG,"OnSyncingListener >> onBloodPressureDataUpdate()");
        }

        // Devolución de datos de medición del medidor de glucosa en sangre
        @Override
        public void onBloodGlucoseDataUpdate(java.lang.String broadcastId, BGDataSummary summary){
            Log.e(TAG,"OnSyncingListener >> onBloodGlucoseDataUpdate()");
        }

        // Devolución de datos de medición de peso
        @Override
        public void onScaleWeightDataUpdate(String broadcastId, LSScaleWeight weight) {
            Log.e(TAG,"OnSyncingListener >> onScaleWeightDataUpdate()");

        }
        //endregion

    };

    // CallBack Battery Info
    public static boolean readDeviceBattery(String deviceMac, OnReadingListener listener) {
        LSBluetoothManager.getInstance().readDeviceBattery(deviceMac, listener);
        return true;
    }

    //endregion

    //region :: DEBUG DATA

    private void setDebugDataStepOfDay() {

        stepSummaryDay.clear();

        String contentData= "1B01030001002ACB63CB1CEF0000FF000A810B2C1E1607FF000001001B4B63CC6E6F0000FF0006970000136706F80000010011E263CDBFEF0000FF00043401720CAC05ED00";
        stepSummaryDay.add(new ATStepSummary(toArrayByte(contentData)));

        contentData= "1B0101000100075C63CE3AC60000FF00023E0000053A05E938";
        stepSummaryDay.add(new ATStepSummary(toArrayByte(contentData)));

//        JSStepSummary tDays = new JSStepSummary(stepSummaryDay);
//        tDays.adapterDateHours();
//        Log.e(TAG,"" + tDays.toString());
//        Log.e(TAG,"HOY: " + tDays.getStepToday());
    }

    private void setDebugDataStepOfHour() {

        stepSummaryHour.clear();
        String contentData= "";

        //2023-03-10 12:58:57 Download
        if (idDebug == -1) {
            Log.e(TAG, "2023-03-10 12:58:57 Download");
            contentData = "570006426408407F0001FF00017B0000044F02D900018D64084E8F0001FF00005F0000011802D90009ED64085C9F0001FF00028C0000072F02D90008C864086AAF0001FF0002200000063A02D900045B640878BF0001FF0000DD0000030B02D9000489640886CF0001FF0001630000034202D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000489640894DF0001FF0001380000031902D80000D06408A2EF0001FF000041000000A502D80000736408B0FF0001FF00000A0000004F02D80006276408BF0F0001FF0001580000043F02D80002A06408CD1F0001FF000060000001DA02D80003B06408DB2F0001FF0000EA0000029702D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000F06408E93F0001FF00002A000000AA02D80000E76408F74F0000FF000023000000A602D80000B56409055F0000FF00001E0000007B02D80000DC6409136F0000FF00003B000000A202D80000006409217F0000FF0000000000000002D800003F64092F8F0000FF0000050000002A02D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064093D9F0000FF0000000000000002D700005264094BAF0000FF0000100000003802D7000000640959BF0000FF0000000000000002D7000000640967CF0000FF0000000000000002D70000A2640975DF0000FF00001B0000006F02D7000096640983EF0000FF0000200000006602D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570004CB640991FF0001FF00013B0000037C02D70001C66409A00F0001FF00008D0000013002D70003346409AE1F0001FF0000D20000025102D60003906409BC2F0001FF0000E60000028802D60005776409CA3F0001FF000192000003CA02D60003846409D84F0001FF0001060000028F02D6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006409E65F0000FF0000000000000001D600066E6409F46F0001FF0001AE0000044E01D60014EB640A027F0001FF00069D00000ED301D6000042640A108F0001FF0000120000003601D6000A47640A1E9F0001FF0002F30000071E01D50003F2640A2CAF0001FF000124000002CB01D5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000237640A3ABF0001FF00007B0000019D01D5000046640A48CF0000FF0000050000003001D5000000640A56DF0000FF0000000000000001D40000C1640A64EF0000FF0000280000008301D400003A640A72FF0000FF0000050000002701D4000000640A810F0000FF0000000000000001D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700002C640A8F1F0000FF0000030000001E01D4000000640A9D2F0000FF0000000000000001D4000046640AAB3F0000FF00000D0000002F01D4000000640AB94F0000FF0000000000000001D400005F640AC75F0000FF0000090000004101D4000000640AD56F0000FF0000000000000001D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000276640AE37F0001FF00007E000001A301D4000247640AF18F0001FF0000770000019E01D40002BE640AFF9F0001FF000081000001E101D3000877640B0DAF0001FF00028E0000060801D30003E8640B1BF90001FF00012D000002D001D3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-12 18:44:33 Download
        else if (idDebug == -2) {
            Log.e(TAG, "2023-03-12 18:44:33 Download");
            contentData = "570003E8640B29CF0001FF00012D000002D006FE000000640B37DF0000FF0000000000000007FF0000C9640B45EF0001FF0000250000008A07FF000342640B53FF0001FF0000E90000025707FF000384640B620F0001FF0000A70000026407FF0000D9640B701F0001FF0000250000009D07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000C9640B7E2F0001FF00002B0000009007FF0001E0640B8C3F0001FF00006D0000015807FF000189640B9A4F0000FF0000430000010507FF000288640BA85F0000FF0000AA000001E007FF00008B640BB66F0000FF00001E0000006107FF000000640BC47F0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000640BD28F0000FF0000000000000007FF000000640BE09F0000FF0000000000000007FF000000640BEEAF0000FF0000000000000007FF000000640BFCBF0000FF0000000000000007FF000000640C0ACF0000FF0000000000000007FF000000640C18DF0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000043640C26EF0000FF00000D0000002F07FF000077640C34FF0001FF0000170000005307FF000000640C430F0000FF0000000000000007FF000000640C511F0000FF0000000000000007FF000000640C5F2F0000FF0000000000000007FF0006C3640C6D3F0001FF000211000004CB07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000581640C7B4F0001FF000160000003E507FF000212640C895F0001FF0000890000018807FF00004E640C976F0001FF00000A0000003507FF0005C4640CA57F0001FF00011A000003ED07FF00004A640CB38F0001FF0000080000003207FF0001CB640CC19F0001FF00005A0000014107FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000666640CCFAF0001FF0001E80000049607FF000253640CDDBF0001FF0000AF000001AF07FF000000640CEBCF0000FF0000000000000007FF000000640CF9DF0000FF0000000000000007FF000874640D07EF0000FF0002780000060D07FF00015D640D15FF0000FF000026000000EE07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000640D240F0000FF0000000000000007FF000000640D321F0000FF0000000000000007FF000000640D402F0000FF0000000000000007FF000000640D4E3F0000FF0000000000000007FF000000640D5C4F0000FF0000000000000007FF000000640D6A5F0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000047640D786F0000FF00000C0000003007FF00044C640D867F0001FF0000E90000030607FF000147640D948F0001FF000048000000E707FF000000640DA29F0000FF0000000000000007FF000A27640D4E3F0001FF00030F0000073506FF000120640D5C4F0001FF00003C000000CC06FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000927640D6A5F0001FF0002BC000006A506FF000418640D786F0001FF0000E3000002E506FF000151640D867F0001FF000039000000E606FE0000F8640D948F0001FF0000260000009A06FD00038D640E0FE20001FF0000D70000027F06FD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-13 08:32:28 Download
        else if (idDebug == -3) {
            Log.e(TAG, "2023-03-13 08:32:28 Download");
            contentData = "57000451640E131F0001FF0000FE0000030806FD000011640E212F0000FF0000010000000C06FC000279640E2F3F0000FF00007D000001B106FC000123640E3D4F0000FF000037000000CC06FB00017C640E4B5F0000FF0000430000010306FB000019640E596F0000FF0000050000001106FB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000021640E677F0000FF0000020000001606FB000055640E758F0000FF0000140000003C06FB000000640E839F0000FF0000000000000006FB000000640E91AF0000FF0000000000000006FA000036640E9FBF0000FF0000030000002506FA000000640EADCF0000FF0000000000000006FA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000A4640EBBDF0000FF0000160000007006FA0000E3640EC9EF0000FF000046000000B106FA0003CE640ED1CA0001FF0000BB000002A406FA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-14 16:58:11 Download
        else if (idDebug == -4) {
            Log.e(TAG, "2023-03-14 16:58:11 Download");
            contentData = "570006FA640ED7FF0001FF0001B80000050E06F90002BE640EE60F0001FF0000D20000020206F9000000640EF41F0000FF0000000000000006F900010B640F022F0001FF00003E000000BF06F9000000640F103F0000FF0000000000000006F900036D640F1E4F0001FF0000E60000025C06F8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000CE9640F2C5F0001FF0003E50000091606F8000000640F3A6F0000FF0000000000000006F80008B2640F487F0001FF00024C0000061D06F80003EA640F568F0001FF000103000002B206F7000000640F649F0000FF0000000000000006F70004F8640F72AF0001FF0001610000038B06F7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700048D640F80BF0001FF00010A0000033A06F600005C640F8ECF0000FF00000E0000003E06F60001C1640F9CDF0000FF0000410000013606F5000091640FAAEF0000FF0000190000006206F5000046640FB8FF0000FF0000070000002F06F5000000640FC70F0000FF0000000000000006F5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700003E640FD51F0000FF0000080000002A06F5000000640FE32F0000FF0000000000000006F5000000640FF13F0000FF0000000000000006F500003D640FFF4F0000FF00000F0000002A06F500000064100D5F0000FF0000000000000006F50001B664101B6F0000FF0000370000012905F4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570004896410297F0001FF0000E70000032A05F400064B6410378F0001FF0001880000044E05F40000166410459F0000FF0000020000000F05F300016C641053AF0001FF00004B000000F805F300022E641061BF0001FF0000990000018E05F20004D164106FCF0001FF00011D0000034705F2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000E6464107DDF0001FF00047800000A1105F200076764108BEF0001FF0001CD0000052505F20003B6641099FF0001FF000115000002A105F200005564109DD60001FF00000E0000003A05F2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-15 11:34:59 Download
        else if (idDebug == -5) {
            Log.e(TAG, "2023-03-15 11:34:59 Download");
            contentData = "570002716410A80F0001FF000069000001AE05F10001026410B61F0001FF000038000000AD05F10003636410C42F0001FF0001040000029305F10003AF6410D23F0001FF0000A00000028A05F100009E6410E04F0000FF0000120000006B05F00002096410EE5F0000FF0000700000017205F0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000176410FC6F0000FF0000010000001005F000000064110A7F0000FF0000000000000005F000003B6411188F0000FF00000A0000002805F00000006411269F0000FF0000000000000005EF000000641134AF0000FF0000000000000005EF00004C641142BF0000FF0000120000003405EF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700001D641150CF0000FF0000020000001405EF00000064115EDF0000FF0000000000000005EF00006E64116CEF0000FF0000150000004B05EE00058D64117AFF0001FF0001D00000044305EE0005D26411890F0001FF00019C0000042405ED0000946411971F0001FF00000D0000006505ED";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700031764119F9E0001FF00010F0000025305ED";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-15 19:42:48 Download
        else if (idDebug == -6) {
            Log.e(TAG, "2023-03-15 19:42:48 Download");
            contentData = "570009466411A52F0001FF0002CF000006B405ED0001676411B33F0001FF0000690000010105ED0003856411C14F0001FF0000D90000027F05EC0005766411CF5F0001FF00013B000003E005EC0004A46411DD6F0001FF0001860000036E05EC0005FA6411EB7F0001FF0001930000040C05EB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002816411F98F0001FF0000A4000001E305EC0000006412079F0000FF0000000000000005EB0000CD641212010001FF0000290000008E05EB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-15 19:49:26 Download
        else if (idDebug == -7) {
            Log.e(TAG, "2023-03-15 19:49:26 Download");
            contentData = "57000000641213940000FF0000000000000006F6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-16 09:53:12 Download
        else if (idDebug == -8) {
            Log.e(TAG, "2023-03-16 09:53:12 Download");
            contentData = "570000C3641215AF0001FF00001F0000007D06F5000305641223BF0001FF0000B90000023205F4000184641231CF0000FF0000570000011505F400006664123FDF0000FF0000160000004705F300006D64124DEF0000FF0000150384004C05F300003C64125BFF0000FF0000020000002805F3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064126A0F0000FF0000000000000005F30000006412781F0000FF0000000000000005F30000006412862F0000FF0000000000000005F20000006412943F0000FF0000000000000005F20000006412A24F0000FF0000000000000005F20000516412B05F0000FF0000080000003705F2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000C26412BE6F0000FF0000160000008405F10000796412CC7F0000FF00001E0000005505F00004056412D9530001FF0000CE000002D105EC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-17 09:41:22 Download
        else if (idDebug == -9) {
            Log.e(TAG, "2023-03-17 09:41:22 Download");
            contentData = "570004266412DA8F0001FF0000D1000002E705EC00013A6412E89F0001FF000034000000D805EC0000926412F6AF0001FF00001E0000006505EC00023C641304BF0001FF0000740000019605EB0001BF641312CF0001FF0000500000013405EB00019E641320DF0001FF00004B0000011D05EB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700006D64132EEF0001FF0000120000004B05EB00001164133CFF0000FF0000020000000B05EB0006BD64134B0F0001FF0001DD000004B305EA0003136413591F0001FF0001140000025A05EA0007B56413672F0001FF0002350000058605EA0006B96413753F0001FF0001B8000004E105E9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570005026413834F0000FF0001400000038705E900039C6413915F0000FF0000C40000028005E800008364139F6F0000FF0000180000005A04E80000256413AD7F0000FF0000020000001904E80000006413BB8F0000FF0000000000000004E80000006413C99F0000FF0000000000000004E8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006413D7AF0000FF0000000000000004E80000006413E5BF0000FF0000000000000004E80000006413F3CF0000FF0000000000000004E700006F641401DF0000FF0000190000004D04E700000064140FEF0000FF0000000000000004E700000064141DFF0000FF0000000000000004E7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000546414280E0001FF0000110000003904E7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-19 20:18:56 Download
        else if (idDebug == -10) {
            Log.e(TAG, "2023-03-19 20:18:56 Download");
            contentData = "5700005464142C0F0001FF0000110000003904E700016364143A1F0001FF00003A000000F604E70003A46414482F0001FF0000D10000029404E70001B46414563F0001FF0000720000013D04E60000006414644F0000FF0000000000000004E6000E236414725F0001FF00040B000009EF04E6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700035D6414806F0001FF00008F0000024A04E600028964148E7F0001FF000076000001B304E60006F364149C8F0001FF0001A3000004E904E60004F96414AA9F0001FF0001290000037604E60003836414B8AF0001FF00009D0000027904E50004ED6414C6BF0001FF0000EE0000036F04E5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570003A56414D4CF0000FF0000C30000027504E500095B6414E2DF0000FF00021D0000067F04E50001D06414F0EF0000FF0000700000014304E50000006414FEFF0000FF0000000000000004E500000064150D0F0000FF0000000000000004E500000064151B1F0000FF0000000000000004E4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006415292F0000FF0000000000000004E40000006415373F0000FF0000000000000004E40000006415454F0000FF0000000000000004E40000006415535F0000FF0000000000000004E40000006415616F0000FF0000000000000004E400009464156F7F0000FF0000170000006404E4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700028264157D8F0001FF000079000001C304E30006E164158B9F0001FF00018D000004EA04E30006C0641599AF0001FF000162000004B904E30004DA6415A7BF0001FF0001150000037704E3000DB76415B5CF0001FF0003450000096A04E30007CF6415C3DF0001FF0002290000057704E3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570006CF6415D1EF0001FF0001DE000004BA04E20002806415DFFF0001FF00008B000001C904E20000006415EE0F0000FF0000000000000004E20004496415FC1F0001FF0001130000030504E20003CB64160A2F0001FF0000AF0000029E04E20003456416183F0001FF0000BC0000025904E2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700015F6416264F0000FF000032000000EF04E20004A66416345F0000FF0000D50000033104E100042A6416426F0000FF0000B4000002E204E10001966416507F0000FF0000320000011404E100000064165E8F0000FF0000000000000004E100000064166C9F0000FF0000000000000004E1";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064167AAF0000FF0000000000000004E1000000641688BF0000FF0000000000000004E1000000641696CF0000FF0000000000000004E00000006416A4DF0000FF0000000000000004E00000006416B2EF0000FF0000000000000004E00000006416C0FF0000FF0000000000000004E0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000FC6416CF0F0001FF000034000000AD04E00007136416DD1F0001FF00018E000004FA04E000079D6416EB2F0001FF0001C40000056303DF0005766416F93F0001FF00015B000003F603DF0004B86417074F0001FF0000EB0000034F03DF0004EE6417155F0001FF0001050000037A03DF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570004DE6417236F0001FF0000F00000035903DF00013C6417317F0001FF000037000000D703DE0002F264173F8F0001FF0000BC0000021D03DF00046A64174D9F0001FF00010B0000032A03DE00065964175BAF0001FF0001AF0000046F03DE0000D8641760D50001FF00003B000000AC03DE";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-20 18:17:42 Download
        else if (idDebug == -11) {
            Log.e(TAG, "2023-03-20 18:17:42 Download");
            contentData = "5700032D641769BF0001FF0000BE0000024A03DE000300641777CF0000FF00009B0000021F03DE000162641785DF0000FF000032000000F003DE000072641793EF0000FF0000160000004E03DE0000226417A1FF0000FF0000000000001703DE0000406417B00F0000FF00000A0000002B03DE";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006417BE1F0000FF0000000000000003DE00004A6417CC2F0000FF0000100000003203DD0000006417DA3F0000FF0000000000000003DD0000006417E84F0000FF0000000000000003DD0000336417F65F0000FF00000A0000002303DD0000606418046F0000FF0000130000004203DD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006418127F0000FF0000000000000003DD0003296418208F0001FF0000FC0000026803DD0001F464182E9F0001FF00006E0000015903DD00002564183CAF0000FF0000040000001903DD0003B164184ABF0001FF0000C30000029803DD0002F6641858CF0001FF0000C60000021D03DD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002A1641866DF0001FF00007F000001D603DC0000B6641874EF0001FF0000250000007E03DC0000A6641882FF0001FF00003B0000008103DC0000A46418910F0001FF0000250000007903DC00004C641895980001FF00000B0000003403DC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-21 09:52:37 Download
        else if (idDebug == -12) {
            Log.e(TAG, "2023-03-21 09:52:37 Download");
            contentData = "570000BF64189F1F0001FF0000190000008203DC0000346418AD2F0000FF0000060000002403DC0000F96418BB3F0000FF000037000000AB03DC0003EE6418C94F0000FF0000B9000002BF03DC00004F6418D75F0000FF0000180000003803DC00005F6418E56F0000FF0000100000004103DC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006418F37F0000FF0000000000000003DC0000006419018F0000FF0000000000000003DC00004464190F9F0000FF00000C0000002E03DC00000064191DAF0000FF0000000000000003DC00000064192BBF0000FF0000000000000003DB000000641939CF0000FF0000000000000003DB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000035641947DF0000FF0000070000002403DB000092641955EF0000FF0000230000006503DB000000641963FF0000FF0000000000000003DB0006DB641971B40001FF0001A5000004E303DB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-22 08:17:35 Download
        else if (idDebug == -13) {
            Log.e(TAG, "2023-03-22 08:17:35 Download");
            contentData = "570006DB6419720F0001FF0001A5000004E303DB0005316419801F0001FF000196000003BB03DB0000F264198E2F0001FF00004C000000B403DB00034264199C3F0001FF0000E10000025B03DB0004126419AA4F0001FF000134000002E003DB000BA56419B85F0001FF00035D0000084F03DB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570004066419C66F0001FF000113000002E303DA00034A6419D47F0001FF0000EB0000026203DA0001186419E28F0001FF000021000000BF03DA00009A6419F09F0001FF0000170000006903DA00041B6419FEAF0001FF0000CF000002E303DA00016F641A0CBF0001FF0000560000010903DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000263641A1ACF0000FF00008D000001AB03DA000232641A28DF0000FF0000770000018C02DA000000641A36EF0000FF0000000000000002DA00004C641A44FF0000FF00000E0000003A02DA000000641A530F0000FF0000000000000002DA00003C641A611F0000FF0000100000002E02DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000641A6F2F0000FF0000000000000002DA000000641A7D3F0000FF0000000000000002DA000000641A8B4F0000FF0000000000000002DA000097641A995F0000FF0000190000006702DA0001F9641AA76F0000FF0000640000015D02D900035B641AABEC0000FF00010C0000029102D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-24 07:57:21 Download
        else if (idDebug == -14) {
            Log.e(TAG, "2023-03-24 07:57:21 Download");
            contentData = "570006CB641AB57F0001FF0001CD000004E402D90003E3641AC38F0001FF000129000002BD02D9000CD2641AD19F0001FF0003830000094C02D9000315641ADFAF0001FF0000B20000022402D9000740641AEDBF0001FF0001DF0000052F02D9000635641AFBCF0001FF00019F0000045702D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000D07641B09DF0001FF0003ED000008FA02D90000CA641B17EF0001FF0000230000008A02D9000561641B25FF0001FF00016C000003C102D9000253641B340F0001FF0000A0000001C002D90002C2641B421F0001FF00008F000001EB02D900032B641B502F0001FF0000F70000023302D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000428641B5E3F0001FF00011B0000030F02D90000BD641B6C4F0000FF0000260000008402D800011C641B7A5F0000FF00003E000000C302D8000000641B886F0000FF0000000000000002D800004C641B967F0000FF00000D0000003402D8000000641BA48F0000FF0000000000000002D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000038641BB29F0000FF0000030000002602D8000000641BC0AF0000FF0000000000000002D8000000641BCEBF0000FF0000000000000002D8000000641BDCCF0000FF0000000000000002D8000050641BEADF0000FF0000080000003702D8000059641BF8EF0000FF0000110000003C02D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002D7641C06FF0001FF0000AE000001F402D800030D641C150F0001FF0000E20000023C02D800019F641C231F0001FF0000780000014002D8000408641C312F0001FF0000F3000002CE02D80000FD641C3F3F0001FF00002B000000AC02D80005D6641C4D4F0001FF0001FE0000043E02D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000D5D641C5B5F0001FF0004510000095C02D7000000641C696F0000FF0000000000000002D7000000641C777F0000FF0000000000000002D7000014641C858F0000FF0000020000000D02D700004B641C939F0001FF0000050000003302D70001C1641CA1AF0001FF0000820000014302D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000325641CAFBF0001FF0000B70000024402D7000040641CBDCF0000FF0000080000002B02D70000BE641CCBDF0000FF00001C0000008202D7000000641CD9EF0000FF0000000000000002D7000000641CE7FF0000FF0000000000000002D7000046641CF60F0000FF00000A0000002F02D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000641D041F0000FF0000000000000002D7000000641D122F0000FF0000000000000002D7000000641D203F0000FF0000000000000002D7000000641D2E4F0000FF0000000000000002D7000078641D3C5F0000FF00000F0000005202D700006B641D4A1E0000FF0000140000004902D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-24 15:23:22 Download
        else if (idDebug == -15) {
            Log.e(TAG, "2023-03-24 15:23:22 Download");
            contentData = "5700006B641D4A6F0000FF0000140000004902D70007A9641D587F0001FF00022F000005B602D6000215641D668F0001FF00004D0000016C02D600015B641D749F0001FF000041000000EE02D60005BB641D82AF0001FF000155000003F102D60001C1641D90BF0001FF0000AA0000015901D6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570003AA641D9ECF0001FF000116000002AE01D5000446641DACDF0001FF0001430000031F01D50000A2641DB3120001FF0000280000007001D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-26 11:32:14 Download
        else if (idDebug == -16) {
            Log.e(TAG, "2023-03-26 11:32:14 Download");
            contentData = "570000A2641DBAEF0001FF0000280000007005F2000000641DC8FF0000FF0000000000000007FF000260641DD70F0001FF000067000001A407FF00027B641DE51F0001FF0000B6000001BE07FF000137641DF32F0001FF000048000000DE07FF0003D8641E013F0001FF000137000002D607FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000160641E0F4F0000FF000048000000F807FF000000641E1D5F0000FF0000000000000007FF0002A0641E2B6F0000FF0000A2000001D807FF000000641E397F0000FF0000000000000007FF000000641E478F0000FF0000000000000007FF000000641E559F0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000641E63AF0000FF0000000000000007FF000000641E71BF0000FF0000000000000007FF00001B641E7FCF0000FF0000000000001207FF00012B641E8DDF0000FF000032000000CF07FF000000641E9BEF0000FF0000000000000007FF000000641EA9FF0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570003A5641EB80F0001FF000135000002B807FF0002EE641EC61F0001FF0000CD0000021506FF000485641ED42F0001FF0001180000033C06FF00070B641EE23F0001FF000206000004FF06FF000434641EF04F0001FF0001180000031006FE000428641EFE5F0001FF000117000002EC06FD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700019E641F0C6F0001FF0000510000012206FC000000641F1A7F0000FF0000000000000006FC00025B641F288F0001FF000098000001AE06FB000213641F369F0001FF00006C0000017606F9000000641F44AF0000FF0000000000000006F90002DD641F52BF0001FF00009D0000020506F8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700049F641F60CF0000FF00011C0000033D06F7000289641F6EDF0000FF00007D000001BC06F600004C641F7CEF0000FF0000080000002906F600011B641F8AFF0000FF00002A000000C006F500002F641F990F0000FF0000070000002006F5000000641FA71F0000FF0000000000000006F5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000641FB52F0000FF0000000000000006F500001A641FC33F0000FF0000010000001206F5000000641FD14F0000FF0000000000000006F5000042641FDF5F0000FF0000090000002C06F5000000641FED6F0000FF0000000000000005F4000000641FFB7F0000FF0000000000000005F4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000B56420098F0001FF0000200000008305F400018F642011630001FF0000610000011705F3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-27 08:17:52 Download
        else if (idDebug == -17) {
            Log.e(TAG, "2023-03-27 08:17:52 Download");
            contentData = "570001FC6420179F0001FF00006D0000016105F300014B642025AF0001FF00004B000000EA05F3000071642033BF0001FF00000F0000004D05F3000330642041CF0001FF0000960000023A05F10001D664204FDF0001FF00005A0000014A05F000004D64205DEF0001FF00000D0000003405EF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700006664206BFF0001FF0000160000004605EF00049764207A0F0001FF00013E0000034E05EE00036F6420881F0001FF0000A20000026405EE0002986420962F0001FF00008F000001CC05EE0002706420A43F0000FF000070000001AE05EE0003AE6420B24F0000FF0000FC000002BC05ED";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006420C05F0000FF0000000000000005ED0000006420CE6F0000FF0000000000000005ED00002C6420DC7F0000FF0000000000001D05ED0000006420EA8F0000FF0000000000000005ED0000216420F89F0000FF0000030000001705EC000000642106AF0000FF0000000000000005EC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000642114BF0000FF0000000000000005EC0000DC642122CF0000FF0000300000009805EC000000642130DF0000FF0000000000000005EC0000EA6421356E0000FF000046000000A605EC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-28 08:17:34 Download
        else if (idDebug == -18) {
            Log.e(TAG, "2023-03-28 08:17:34 Download");
            contentData = "5700036F64213EEF0001FF0000CD0000026C05EC0003B164214CFF0001FF000145000002C005EB00044864215B0F0001FF00015A0000030905EB00033C6421691F0001FF0000B50000024605EB0003EE6421772F0001FF0000A9000002B605EB0003166421853F0001FF0000670000020D05EB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002F36421934F0001FF0000C80000021B05EB0002446421A15F0001FF0000850000019C05EB00027B6421AF6F0001FF000060000001BD05EA0002036421BD7F0001FF00003C0000015E05EA0006636421CB8F0001FF000237000004E005EA00024E6421D99F0001FF0000850000019F05EA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700022A6421E7AF0001FF00006D0000018A05EA00030C6421F5BF0000FF0000AD0000022905EA000161642203CF0000FF0000520000010405EA000000642211DF0000FF0000000000000005EA00000064221FEF0000FF0000000000000005EA00003664222DFF0000FF0000050000002405E9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064223C0F0000FF0000000000000005E900000064224A1F0000FF0000000000000005E90000006422582F0000FF0000000000000005E90000006422663F0000FF0000000000000005E900005C6422744F0000FF0000100000003F05E90000006422825F0000FF0000000000000005E9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000032642286DB0000FF0000070000002205E9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }

        //2023-03-29 08:16:21 -> 1
        else if (idDebug == 1) {
            Log.e(TAG, "2023-03-29 08:16:21");
            contentData = "570004616422906F0001FF00011D0000032105E900033864229E7F0001FF0000B60000023D05E80000006422AC8F0000FF0000000000000005E80000006422BA9F0000FF0000000000000005E80003DB6422C8AF0001FF00012C000002E204E80005DF6422D6BF0001FF0001B50000041504E8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000DB06422E4CF0001FF00045A000009A204E80002906422F2DF0001FF000078000001D004E8000481642300EF0001FF0001440000035704E700017C64230EFF0001FF0000330000010504E700008864231D0F0001FF00000C0000005D04E700013964232B1F0001FF000052000000D604E7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700049B6423392F0001FF0000DB0000033C04E70002246423473F0000FF0000820000018D04E70001F96423554F0000FF0000670000015F04E70000006423635F0000FF0000000000000004E60000006423716F0000FF0000000000000004E600004264237F7F0000FF00000A0000002D04E6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064238D8F0000FF0000000000000004E600000064239B9F0000FF0000000000000004E60000006423A9AF0000FF0000000000000004E60000006423B7BF0000FF0000000000000004E60000606423C5CF0000FF0000090000004104E50000006423D3DF0000FF0000000000000004E5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700004F6423D8800000FF0000110000003604E5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-03-30 08:12:47 -> 2
        else if (idDebug == 2) {
            Log.e(TAG, "2023-03-30 08:12:47");
            contentData = "5700030E6423E1EF0001FF0000D10000024204E50002386423EFFF0001FF0000C80000018B04E5000F536423FE0F0001FF0004A700000B7004E500076F64240C1F0001FF0001E70000055104E500098B64241A2F0001FF000277000006DA04E50000536424283F0001FF00000D0000003904E4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001066424364F0001FF000038000000BD04E4000DAA6424445F0001FF00048A000009C104E40002DD6424526F0001FF0000DA0000020D04E40001CE6424607F0001FF0000640000014E04E400017664246E8F0001FF0000620000010A04E40005D364247C9F0001FF0001AE0000044604E4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700064D64248AAF0001FF00017C0000047E04E30001AD642498BF0000FF0000630000013404E30001B46424A6CF0000FF0000580000013004E300009A6424B4DF0000FF0000280000006D04E30000006424C2EF0000FF0000000000000004E30000006424D0FF0000FF0000000000000004E3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000416424DF0F0000FF00000A0000002C04E20000006424ED1F0000FF0000000000000004E20000006424FB2F0000FF0000000000000004E200002C6425093F0000FF0000030000001E04E20000EF6425174F0000FF00002D000000A204E20001BD6425255F0000FF00005D0000013404E2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002E1642528B30000FF0000CA0000022204E2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-04-07 00:55:39  -> 3
        else if (idDebug == 3) {
            Log.e(TAG, "2023-04-07 00:55:39");
            contentData = "570005376425336F0001FF000147000003BF04E10001B46425417F0001FF00006F0000013304E100031064254F8F0001FF0000C90000021E04E100020F64255D9F0001FF0000680000016E04E100030064256BAF0001FF0000A00000021504E00004AA642579BF0001FF0001AB0000038E04E0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570009CE642587CF0001FF000280000006D203DE00065C642595DF0001FF0001750000048503DE000B616425A3EF0001FF00030E000007EE03DE0004B16425B1FF0001FF00012C0000036A03DD0007506425C00F0001FF0001F40000053B03DD0004F06425CE1F0001FF00012D0000037F03DC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570005756425DC2F0001FF000172000003E603DC0002CD6425EA3F0000FF000091000001F103DB0005EF6425F84F0000FF0001590000043303DB0001506426065F0000FF000052000000EC03DB0000006426146F0000FF0000000000000003DB0000006426227F0000FF0000000000000003DB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000116426308F0000FF0000000000000B03DB00000064263E9F0000FF0000000000000003DB00000064264CAF0000FF0000000000000003DB00001864265ABF0000FF0000050000001003DA000000642668CF0000FF0000000000000003DA000116642676DF0000FF00002F000000B503DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000475642684EF0001FF0000FF0000031E02D900051B642692FF0001FF000134000003A802D90000006426A10F0000FF0000000000000002D900031A6426AF1F0001FF0000B80000023E02D90001AA6426BD2F0001FF0000510000012D02D80001A06426CB3F0001FF00003E0000011D02D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000796426D94F0001FF0000120000005302D80004D26426E75F0001FF0001480000037B02D80002FB6426F56F0001FF0000B60000021B02D80006AE6427037F0001FF0001DC000004D002D70005216427118F0001FF000122000003AA02D700045864271F9F0001FF0000E50000030402D6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700052264272DAF0001FF000170000003C802D600055464273BBF0000FF0001B3000003E401D600006C642749CF0000FF0000190000004E01D600014E642757DF0000FF00003F000000E701D5000000642765EF0000FF0000000000000001D5000000642773FF0000FF0000000000000001D5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006427820F0000FF0000000000000001D50000006427901F0000FF0000000000000001D500000064279E2F0000FF0000000000000001D500003F6427AC3F0000FF00000A0000002A01D50000006427BA4F0000FF0000000000000001D50000006427C85F0000FF0000000000000001D5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000146427D66F0000FF0000020000000E01D50005C66427E47F0001FF0001430000041501D50000006427F28F0000FF0000000000000001D40000006428009F0000FF0000000000000001D400061A64280EAF0001FF0001760000043201D3000CC264281CBF0001FF0002C6000008E801D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700073B64282ACF0001FF00017E000004F001D3000031642838DF0000FF00000A0000002101D3000815642846EF0001FF0001980000057D01D300090A642854FF0001FF0001CF0000064501D20003576428630F0001FF0000AA0000023E01D20007AB6428711F0001FF0001760000055A01D2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700057364287F2F0001FF0000F0000003B501D200074964288D3F0000FF000166000004FC01D20003E864289B4F0000FF0000CA000002AB00D2000C4D6428A95F0000FF00031E0000087F00D20001796428B76F0000FF0000360000010200D10000006428C57F0000FF0000000000000000D1";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006428D38F0000FF0000000000000000D10000006428E19F0000FF0000000000000000D10000006428EFAF0000FF0000000000000000D10000006428FDBF0000FF0000000000000000D100000064290BCF0000FF0000000000000000D1000017642919DF0000FF0000010000001000D0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570003B1642927EF0001FF0000D10000029F00D0000458642935FF0001FF000106000002FF00D00005606429440F0001FF000139000003C000D00006626429521F0001FF0001930000049000D000014B6429602F0001FF000044000000E500CF00000064296E3F0000FF0000000000000000CF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700017764297C4F0001FF000054000000FC00CE00086D64298A5F0001FF0002A70000061F00CD0003A66429986F0001FF0000E40000028B00CD0003186429A67F0001FF0000A90000022900CD0006CC6429B48F0001FF0001CA000004D800CD0000CC6429C29F0001FF00003C0000009F06FC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006429D0AF0000FF0000000000000007FF00005B6429DEBF0000FF00000A0000003E07FF00026C6429ECCF0000FF00006C000001A807FF0000356429FADF0000FF0000070000002407FF000000642A08EF0000FF0000000000000007FF000000642A16FF0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000642A250F0000FF0000000000000007FF000000642A331F0000FF0000000000000007FF000000642A412F0000FF0000000000000007FF000000642A4F3F0000FF0000000000000007FF000000642A5D4F0000FF0000000000000007FF000015642A6B5F0000FF0000000000000E07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000213642A796F0001FF0000570000016407FF000428642A877F0001FF0001190000030B07FF00038D642A958F0001FF00012A000002A207FF000539642AA39F0001FF000149000003D107FF0000C2642AB1AF0001FF0000110000008407FF00011F642ABFBF0001FF000032000000C507FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000578642ACDCF0001FF000186000003E707FF000119642ADBDF0001FF00003A000000CC07FF000188642AE9EF0001FF00004A0000011807FF00046E642AF7FF0001FF0001040000033207FF0004DF642B060F0001FF0001250000036A06FF000819642B141F0001FF0001FE000005AA06FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000305642B222F0001FF0000980000021F06FF00036A642B303F0000FF0001040000027606FE000034642B3E4F0000FF0000070000002406FD0001CB642B4C5F0000FF0000700000015106FD000000642B5A6F0000FF0000000000000006FD000000642B687F0000FF0000000000000006FD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000642B768F0000FF0000000000000006FD000000642B849F0000FF0000000000000006FD000000642B92AF0000FF0000000000000006FD000000642BA0BF0000FF0000000000000006FD000000642BAECF0000FF0000000000000006FD000062642BBCDF0000FF0000080000004206FC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000642BCAEF0000FF0000000000000006FC000000642BD8FF0000FF0000000000000006FC000050642BE70F0001FF0000060000003706FB000193642BF51F0001FF0000780000012406FA00073A642C032F0001FF00028E0000055106FA000540642C113F0001FF00016D0000039F06F8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700056B642C1F4F0001FF0001A7000003E806F8000359642C2D5F0001FF0000DF0000026206F8000465642C3B6F0001FF0001470000033006F8000716642C497F0001FF000168000004D906F8000560642C578F0001FF00010D000003CE06F70000F8642C659F0001FF000031000000AE06F7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570005E5642C73AF0001FF00015E0000042C06F60002BC642C81BF0000FF000064000001DE06F6000245642C8FCF0000FF0000780000019106F6000000642C9DDF0000FF0000000000000006F6000000642CABEF0000FF0000000000000006F600000F642CB9FF0000FF0000000000000A06F6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000642CC80F0000FF0000000000000006F6000000642CD61F0000FF0000000000000006F6000000642CE42F0000FF0000000000000006F5000000642CF23F0000FF0000000000000006F5000031642D004F0000FF0000070000002106F5000000642D0E5F0000FF0000000000000006F5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000257642D1C6F0001FF00007E0000019A06F50007FD642D2A7F0001FF0002A3000005F606F500028F642D388F0001FF0000BF000001E406F500016D642D469F0001FF000069000000EA05F40002CF642D54AF0001FF0000BC0000021905F40003D3642D62BF0001FF00014B000002B705F4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000CF1642D70CF0001FF0004310000090005F4000090642D7EDF0001FF0000410000008305F400018A642D8CEF0001FF00005D0000010F05F4000013642D9AFF0000FF0000020000000D05F40000AB642DA90F0001FF0000280000007705F40003F1642DB71F0001FF0000D1000002C105F4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570005A3642DC52F0001FF00013D000003F405F30003F4642DD33F0000FF0000E0000002D605F3000014642DE14F0000FF0000010000000E05F200007F642DEF5F0000FF0000180000005605F3000031642DFD6F0000FF0000050000002105F3000032642E0B7F0000FF00000C0000002205F3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000642E198F0000FF0000000000000005F3000000642E279F0000FF0000000000000005F2000000642E35AF0000FF0000000000000005F2000000642E43BF0000FF0000000000000005F2000000642E51CF0000FF0000000000000005F20000C2642E5FDF0000FF0000150000008405F2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700002F642E6DEF0000FF0000070000002005F20002D4642E7BFF0001FF00007A000001E605F20002D7642E8A0F0001FF0000700000020305F20001D3642E981F0001FF0000510000014B05F1000295642EA62F0001FF00008A000001C605F100032F642EB43F0001FF0000B30000024C05F1";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000580642EC24F0001FF000132000003E705F1000646642ED05F0001FF0001770000044E05F100074E642EDE6F0001FF0001AA0000053D05F00001D8642EEC7F0001FF0000610000014D05F00004A6642EFA8F0001FF0001090000035105F000047A642F089F0001FF0001470000035305F0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000359642F16AF0001FF0000E60000025D05EF0000D9642F24BF0000FF0000320000009C05EF0000E3642F32CF0000FF00001D0000009A05EE00034E642F40DF0000FF0001070000027E05EE000038642F4E620000FF0000020000002805EE";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-04-10 16:46:47  -> 4
        else if (idDebug == 4) {
            Log.e(TAG, "2023-04-10 16:46:47 ");
            contentData = "5700008C642F4EEF0000FF0000110000005005EE000019642F5CFF0000FF0000060000002205ED000000642F6B0F0000FF0000000000000005ED000000642F791F0000FF0000000000000005ED000000642F872F0000FF0000000000000005ED000000642F953F0000FF0000000000000005ED";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000642FA34F0000FF0000000000000005ED000000642FB15F0000FF0000000000000005ED0001F6642FBF6F0001FF00005F0000015E05EC000412642FCD7F0001FF0000D4000002D205EC000A35642FDB8F0001FF0002AB0000077005EC0005C5642FE99F0001FF0001490000042605EC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700001A642FF7AF0000FF0000000000000005EC00092F643005BF0001FF00021E0000069A05EB000345643013CF0001FF0000DD0000025D05EC0005A8643021DF0001FF0001650000040305EB00000064302FEF0000FF0000000000000005EB0000FF64303DFF0001FF00003B000000B605EB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000D564304C0F0001FF00001A0000009105EB00086C64305A1F0001FF0001E8000005E805EA00026E6430682F0001FF000087000001B505EA0000FC6430763F0000FF00003E000000B105EA00010A6430844F0000FF000024000000B905EA0000116430925F0000FF0000020000000C05EA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000236430A06F0000FF0000020000001705EA0001246430AE7F0000FF000025000000C705EA0000006430BC8F0000FF0000000000000005EA0000006430CA9F0000FF0000000000000005EA0000006430D8AF0000FF0000000000000005EA0000006430E6BF0000FF0000000000000005E9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006430F4CF0000FF0000000000000005E9000000643102DF0000FF0000000000000005E9000000643110EF0000FF0000000000000005E900006E64311EFF0001FF00000D0000003B05E900031164312D0F0001FF0000910000021E04E8000B1B64313B1F0001FF000253000007AD04E8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000D116431492F0001FF0003770000093504E80001F86431573F0001FF00004F0000015D04E80001936431654F0001FF0000490000011604E80002A06431735F0001FF0000A4000001EE04E70004126431816F0001FF0000DE000002CC04E700000064318F7F0000FF0000000000000004E7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700010964319D8F0001FF000023000000B404E700005F6431AB9F0001FF00000F0000004104E600041A6431B9AF0001FF0000E6000002E804E60001CF6431C7BF0000FF0000350000013B04E60000BB6431D5CF0000FF00001C0000007F04E600012A6431E3DF0000FF000036000000CF04E6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000216431F1EF0000FF0000000000001604E60000006431FFFF0000FF0000000000000004E600000064320E0F0000FF0000000000000004E600000064321C1F0000FF0000000000000004E500000064322A2F0000FF0000000000000004E50000006432383F0000FF0000000000000004E5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006432464F0000FF0000000000000004E50000006432545F0000FF0000000000000004E50000116432626F0000FF0000010000000C04E5000BA36432707F0001FF0004990000094104E4001BA264327E8F0001FF000BDE0000169C04E400176564328C9F0001FF0009250000123F04E4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000DFC64329AAF0001FF00036E000009AA04E4000C9C6432A8BF0001FF00032A000008C604E40002066432B6CF0001FF0000820000016A04E400006F6432C4DF0001FF00000A0000004C04E200034D6432D2EF0001FF0000D20000025104E20000866432E0FF0001FF0000280000006604E2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000E96432EF0F0001FF000025000000A104E20002E26432FD1F0001FF000091000001E604E1000E6364330B2F0001FF000350000009F804E10003DE6433193F0000FF000104000002C204E10002796433274F0000FF000094000001D004E10001606433355F0000FF000037000000F104E1";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006433436F0000FF0000000000000004E10000B06433517F0000FF00002E0000008004E100000064335F8F0000FF0000000000000004E100000064336D9F0000FF0000000000000004E000000064337BAF0000FF0000000000000004E0000000643389BF0000FF0000000000000004E0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000643397CF0000FF0000000000000004E00000006433A5DF0000FF0000000000000004E000009B6433B3EF0001FF0000130000006904E00002CA6433C1FF0001FF0000A50000020804E00006F46433D00F0001FF00014F000004BB04DF0006EB6433DE1F0001FF000160000004E003DF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002EE6433EC2F0001FF0000B10000021103DF0002DB6433FA3F0001FF0000D00000020603DF0003086434084F0001FF0000CD0000022B03DF0002456434165F0001FF00007A0000019403DF00005E643421AF0001FF00001F0000004F03DF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-04-12 12:49:29  -> 5
        else if (idDebug == 5) {
            Log.e(TAG, "2023-04-12 12:49:29");
            contentData = "570001856434246F0001FF0000410000010A03DF00014E6434327F0001FF000037000000F303DE00049E6434408F0001FF0000F80000033703DE0001B864344E9F0001FF0000580000013903DE0003F664345CAF0001FF0000AF000002C203DE00000064346ABF0000FF0000000000000003DE";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000046643478CF0000FF00000E0000003003DE000157643486DF0000FF00004D000000F203DE000000643494EF0000FF0000000000000003DE0000006434A2FF0000FF0000000000000003DE0000006434B10F0000FF0000000000000003DE0000006434BF1F0000FF0000000000000003DD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006434CD2F0000FF0000000000000003DD0000166434DB3F0000FF0000000000000E03DD0000006434E94F0000FF0000000000000003DD00001E6434F75F0000FF0000050000001503DD0001376435056F0000FF000031000000D303DD00059A6435137F0001FF0001AC0000041903DD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002626435218F0001FF0000EF000001E503DD00049164352F9F0001FF0001C70000038D03DD00012764353DAF0001FF00006B000000F003DD0003B064354BBF0001FF0000C30000029E03DD000688643559CF0001FF00020A000004BB03DC0003AB643567DF0001FF000138000002B203DC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000943643575EF0001FF000311000006A403DC000187643583FF0001FF0000710000011303DC0003696435920F0001FF0000C70000026603DC0004986435A01F0001FF0001080000033003DC0003DF6435AE2F0001FF0000C7000002BC03DC0006856435BC3F0000FF0001600000049603DC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700037D6435CA4F0000FF0000C20000027103DC00010C6435D85F0000FF00002B000000B603DC0000CC6435E66F0000FF00000F0000008A03DC00002B6435F47F0000FF0000040000001D03DB0000006436028F0000FF0000000000000003DB0000006436109F0000FF0000000000000003DB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064361EAF0000FF0000000000000003DB00000064362CBF0000FF0000000000000003DB00000064363ACF0000FF0000000000000003DB000064643648DF0000FF00001C0000004703DB00005D643656EF0000FF00001C0000004103DB00028D643664FF0001FF0000BB000001E403DB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000546436730F0001FF0000080000003903DB0002526436811F0001FF0000630000019203DB00038564368CFF0001FF0000C40000027D03DB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-04-14 08:08:41  -> 6
        else if (idDebug == 6) {
            Log.e(TAG, "2023-04-14 08:08:41");
            contentData = "570004A264368F2F0001FF00010F0000034103DB00067564369D3F0001FF0001780000049103DB0003456436AB4F0001FF0000C60000024903DB0001956436B95F0001FF0000490000011903DB0000F16436C76F0001FF00001E000000A303DB0001906436D57F0001FF00003B0000011003DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570005AA6436E38F0001FF0001420000040803DA0001126436F19F0001FF000039000000C303DA0002196436FFAF0001FF00008E0000017703DA0001D064370DBF0000FF00004D0000014103DA00004664371BCF0000FF00000A0000003003DA000183643729DF0000FF0000410000010C02DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000643737EF0000FF0000000000000002DA000039643745FF0000FF00000E0000002902DA0000006437540F0000FF0000000000000002DA0000006437621F0000FF0000000000000002DA0000006437702F0000FF0000000000000002DA00000064377E3F0000FF0000000000000002D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064378C4F0000FF0000000000000002D900006264379A5F0000FF00000B0000004202D90000006437A86F0000FF0000000000000002D90002FE6437B67F0001FF0000D10000022902D90001CA6437C48F0001FF0000600000014802D90000886437D29F0001FF00000C0000005C02D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000576437E0AF0001FF0000150000003C02D90002E46437EEBF0001FF000093000001FD02D90003A06437FCCF0001FF0000B10000028502D900043664380ADF0001FF00011A0000031302D9000382643818EF0001FF0000F80000029502D9000000643826FF0000FF0000000000000002D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002216438350F0001FF00007C0000018302D80008A96438431F0001FF0001F50000060402D800086C6438512F0001FF0001E6000005C402D800034C64385F3F0000FF0000BD0000025402D80000DE64386D4F0000FF00001A0000009702D700006E64387B5F0000FF00001E0000004D02D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700001D6438896F0000FF0000000000001302D70000006438977F0000FF0000000000000002D70000006438A58F0000FF0000000000000002D70000006438B39F0000FF0000000000000002D70000006438C1AF0000FF0000000000000002D70000006438CFBF0000FF0000000000000002D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006438DDCF0000FF0000000000000002D70000106438EBDF0000FF0000010000000B02D70000A86438EE600000FF00002F0000008002D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-04-18 20:47:21  -> 7
        else if (idDebug == 7) {
            Log.e(TAG, "2023-04-18 20:47:21");
            contentData = "570000A86438F9EF0000FF00002F0000008007FF0003C2643907FF0001FF0000E6000002B607FF00043D6439160F0001FF0000E7000002E607FF0001166439241F0001FF000044000000D907FF00002C6439322F0000FF0000030000001E07FF0003DE6439403F0001FF000110000002D907FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064394E4F0000FF0000000000000007FF00005A64395C5F0001FF0000070000003E07FF00004464396A6F0001FF0000080000002E06FF0000006439787F0000FF0000000000000006FE0002B76439868F0001FF000084000001E306FD0002676439949F0001FF000076000001B206FB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570003876439A2AF0001FF0000BD0000027D06FA0001C06439B0BF0000FF0000550000013D06F900008C6439BECF0000FF0000120000005F06F80003686439CCDF0000FF0000AF0000026206F70000006439DAEF0000FF0000000000000006F60000006439E8FF0000FF0000000000000006F6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006439F70F0000FF0000000000000006F6000000643A051F0000FF0000000000000006F6000030643A132F0000FF0000000000002806F6000012643A213F0000FF0000010000000C06F5000000643A2F4F0000FF0000000000000006F5000017643A3D5F0000FF0000020000001006F5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700036F643A4B6F0001FF0000CF0000027405F4000370643A597F0001FF0000C30000026D05F20006B0643A678F0001FF00018E000004A905F2000D66643A759F0001FF0003770000097605F2000BD0643A83AF0001FF0003D30000088C05F20005AE643A91BF0001FF00011C000003F005F1";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000311643A9FCF0001FF0000840000021805EF00050B643AADDF0001FF00010B0000036F05EF000FDF643ABBEF0001FF0004F800000B5505EE000586643AC9FF0001FF00014D000003FA05ED0008D4643AD80F0001FF0001C70000062105ED0007DA643AE61F0001FF00020F0000059405EB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700004B643AF42F0001FF00000F0000003405E900065B643B023F0000FF00019B0000048205E900039E643B104F0000FF0000C70000026405E80002B5643B1E5F0000FF00008A000001EC05E8000105643B2C6F0000FF000030000000BA05E8000010643B3A7F0000FF0000010000000A05E8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000643B488F0000FF0000000000000005E8000000643B569F0000FF0000000000000004E8000000643B64AF0000FF0000000000000004E8000000643B72BF0000FF0000000000000004E8000000643B80CF0000FF0000000000000004E8000000643B8EDF0000FF0000000000000004E7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000077643B9CEF0000FF0000110000005104E70000AC643BAAFF0001FF00000F0000007504E50005C5643BB90F0001FF0001460000040E04E3000390643BC71F0001FF0000E20000029904E200062B643BD52F0001FF0001AB0000046604E1000D07643BE33F0001FF0003200000090804E0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700039A643BF14F0001FF0000D00000028704E0000420643BFF5F0001FF0000B6000002CE03DF0005E0643C0D6F0001FF0001630000040B03DF00082D643C1B7F0001FF000210000005A903DE000642643C298F0001FF0001510000045903DE000715643C379F0001FF00019B000004E203DD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000BE643C45AF0000FF0000250000008203DC000185643C53BF0000FF00003C0000010A03DB000017643C61CF0000FF0000010000001003DA000161643C6FDF0000FF000054000000F403DA000000643C7DEF0000FF0000000000000003DA000000643C8BFF0000FF0000000000000003DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000643C9A0F0000FF0000000000000003DA000000643CA81F0000FF0000000000000003DA000000643CB62F0000FF0000000000000003DA000000643CC43F0000FF0000000000000003DA000000643CD24F0000FF0000000000000003DA000013643CE05F0000FF0000000000000C03DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000056643CEE6F0000FF00000E0000003B02DA000450643CFC7F0001FF0000E40000030202DA00051F643D0A8F0001FF0001110000038702DA0006A7643D189F0001FF000144000004A202DA000000643D26AF0000FF0000000000000002DA0000E9643D34BF0001FF00002B0000009F02D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000250643D42CF0001FF00005C0000019902D9000000643D50DF0000FF0000000000000002D9000000643D5EEF0000FF0000000000000002D9000209643D6CFF0001FF00006F0000016F02D9000000643D7B0F0000FF0000000000000002D9000221643D891F0001FF0000780000017402D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000771643D972F0001FF0001A00000054F02D9000260643DA53F0000FF00005A000001A502D9000540643DB34F0000FF000122000003B702D9000034643DC15F0000FF0000070000002302D9000034643DCF6F0000FF0000010000002302D9000019643DDD7F0000FF0000020000001102D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000643DEB8F0000FF0000000000000002D9000000643DF99F0000FF0000000000000002D8000000643E07AF0000FF0000000000000002D8000000643E15BF0000FF0000000000000002D8000000643E23CF0000FF0000000000000002D8000151643E31DF0000FF000049000000E802D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570007F4643E3FEF0001FF0002720000061602D8000337643E4DFF0001FF0001030000024202D8000234643E5C0F0001FF0000AA0000018B02D80003B0643E6A1F0001FF0000E40000029902D800015F643E782F0001FF00002A000000EE02D800019C643E863F0001FF0000500000010E02D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000281643E944F0001FF0000C4000001E202D8000137643EA25F0001FF000039000000ED02D80000B6643EB06F0001FF00001C0000007B02D80001C5643EBE7F0001FF0000520000013E02D800043D643ECC8F0001FF00010B000002FB02D8000507643EDA9F0001FF0001230000039502D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000021643EE6230000FF00000A0000001602D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-04-24 08:35:30  -> 8
        else if (idDebug == 8) {
            Log.e(TAG, "2023-04-24 08:35:30");
            contentData = "57000148643EE8AF0000FF00004E000000E002D8000773643EF6BF0000FF0002190000057702D70000EF643F04CF0000FF000028000000A902D700002B643F12DF0000FF0000030000001D02D7000012643F20EF0000FF0000000000000C02D7000000643F2EFF0000FF0000000000000002D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000643F3D0F0000FF0000000000000002D7000000643F4B1F0000FF0000000000000002D7000000643F592F0000FF0000000000000002D7000000643F673F0000FF0000000000000002D7000000643F754F0000FF0000000000000002D700006A643F835F0000FF00000C0000004802D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000643F916F0000FF0000000000000002D7000000643F9F7F0000FF0000000000000002D7000000643FAD8F0000FF0000000000000002D7000000643FBB9F0000FF0000000000000002D7000000643FC9AF0000FF0000000000000002D7000000643FD7BF0000FF0000000000000002D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000643FE5CF0000FF0000000000000002D7000000643FF3DF0000FF0000000000000002D7000000644001EF0000FF0000000000000002D700000064400FFF0000FF0000000000000002D700000064401E0F0000FF0000000000000002D700000064402C1F0000FF0000000000000002D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000B164403A2F0001FF0000170000007802D700003B6440483F0000FF00000F0000002B02D600031C6440564F0000FF0000BB0000024402D600007F6440645F0000FF0000110000005601D60000006440726F0000FF0000000000000001D60000006440807F0000FF0000000000000001D6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064408E8F0000FF0000000000000001D600000064409C9F0000FF0000000000000001D60000006440AAAF0000FF0000000000000001D50000006440B8BF0000FF0000000000000001D50000C26440C6CF0000FF0000200000008A01D60000006440D4DF0000FF0000000000000001D6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700038B6440E2EF0001FF0000DD0000028301D50002D56440F0FF0001FF0000C40000020E01D500032E6440FF0F0001FF0000DD0000024901D50003A764410D1F0001FF0000E50000029601D50004CF64411B2F0001FF0001270000035C01D50003CC6441293F0001FF00010E000002B701D5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000396441374F0000FF0000050000002701D50002076441455F0001FF0000460000016101D50000166441536F0000FF0000010000000F01D50000A96441617F0001FF0000140000007301D500011564416F8F0001FF00002E000000C501D500055164417D9F0001FF00016A000003BB01D5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700058164418BAF0001FF000135000003D101D40001C5644199BF0000FF0000580000013F01D40000006441A7CF0000FF0000000000000001D400002B6441B5DF0000FF0000070000001D01D40000006441C3EF0000FF0000000000000001D40000006441D1FF0000FF0000000000000001D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006441E00F0000FF0000000000000001D40000006441EE1F0000FF0000000000000001D40000006441FC2F0000FF0000000000000001D400000064420A3F0000FF0000000000000001D400001A6442184F0000FF0000000000001101D40000006442265F0000FF0000000000000001D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700049D6442346F0001FF00012B0000035F01D40007F96442427F0001FF000244000005AC01D40000006442508F0000FF0000000000000001D400016364425E9F0001FF000049000000F401D40001CA64426CAF0001FF0000880000014F01D300038064427ABF0001FF00010B0000028C01D3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000D55644288CF0001FF00040B0000093901D30000AD644296DF0001FF0000250000007601D30000006442508F0000FF0000000000000001D300014064425E9F0001FF00003D000000DA01D300024264426CAF0001FF0000B6000001B001D30005DE64427ABF0001FF0001A60000045101D3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000AF644288CF0000FF00001E0000007701D2000154644296DF0000FF000039000000E901D200002D6442A4EF0000FF0000060000001F01D20000006442B2FF0000FF0000000000000001D20000436442C10F0000FF0000070000002D01D20000426442CF1F0000FF00000C0000002D01D2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006442DD2F0000FF0000000000000001D20000006442EB3F0000FF0000000000000001D20000006442F94F0000FF0000000000000001D20000006443075F0000FF0000000000000001D20000006443156F0000FF0000000000000001D20000006443237F0000FF0000000000000001D2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001226443318F0001FF000037000000C500D200000064433F9F0000FF0000000000000000D200021F64434DAF0001FF0000490000017500D100067764435BBF0001FF0001C1000004C600D1000180644369CF0001FF00005F0000010C00D00000B4644377DF0001FF0000180000007E00D0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000243644385EF0001FF00009D000001A300D0000150644393FF0001FF00003C000000E500D000000D6443A20F0000FF0000000000000000D00001466443B01F0001FF000034000000E900D00008436443BE2F0001FF00024C000005D000D00003166443CC3F0001FF0000C20000022D00D0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570006346443DA4F0001FF00019F0000046700D00004036443E85F0000FF0000F7000002CC00D000014D6443F66F0000FF000049000000E800CF00003C6444047F0000FF0000070000002900CF0000006444128F0000FF0000000000000000CF0000006444209F0000FF0000000000000000CF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700005664442EAF0000FF0000090000003A00CF00000064443CBF0000FF0000000000000000CF00000064444ACF0000FF0000000000000000CF000000644458DF0000FF0000000000000000CF000000644466EF0000FF0000000000000000CF00006D644474FF0000FF0000120000004A00CF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000966444830F0001FF0000160000006600CF0000AE6444911F0001FF0000140000007600CE0001FF64449F2F0001FF00005F0000016700CE0000006444AD3F0000FF0000000000000000CE00004A6444BB4F0001FF00000B0000003300CE0001116444C95F0001FF00002D000000BF00CE";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001BA6444D76F0001FF00007E0000015400CE0006C86444E57F0001FF0002480000050F00CE000E6E6444F38F0001FF0004A300000A4C00CE0000106445019F0000FF0000020000000B00CD00019A64450FAF0001FF0000490000011B00CD00033F64451DBF0001FF00007B0000023C00CD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000EB64452BCF0000FF00002A000000A200CD0001DF644539DF0000FF0000610000014C01E2000000644547EF0000FF0000000000000007FF00001F644555FF0000FF0000010000001507FF00002A6445640F0000FF0000020000001C07FF0000006445721F0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700001A6445802F0000FF0000050000001207FF00000064458E3F0000FF0000000000000007FF00000064459C4F0000FF0000000000000007FF0000006445AA5F0000FF0000000000000007FF00000F6445B86F0000FF0000020000000A07FF0000006445C67F0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001F6644623AA0001FF0000440000015507FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-04-25 15:28:42  -> 9
        else if (idDebug == 9) {
            Log.e(TAG, "22023-04-25 15:28:42 ");
            contentData = "57000377644628EF0001FF00006F0000025B07FF000042644636FF0001FF0000050000002D07FF0000006446450F0000FF0000000000000007FF0000556446531F0001FF0000190000004107FF00038C6446612F0001FF0000970000027807FF0005E764466F3F0001FF0001B70000046307FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001F964467D4F0001FF00006F0000016807FF0000ED64468B5F0001FF00001D000000A107FF0004DC6446996F0001FF00014E0000038807FF0000FD6446A77F0001FF000027000000AF07FF00001C6446B58F0000FF0000050000001307FF0001B26446C39F0001FF00004B0000013107FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002476446D1AF0001FF0000700000019307FF0001C46446DFBF0000FF0000500000013607FF00004A6446EDCF0000FF00000C0000003307FF0000F76446FBDF0000FF000044000000BD07FF000258644709EF0000FF0000BE000001B607FF00014E644717FF0000FF000050000000EE07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006447260F0000FF0000000000000007FF0000006447341F0000FF0000000000000007FF0000006447422F0000FF0000000000000007FF0000006447503F0000FF0000000000000007FF00000064475E4F0000FF0000000000000007FF00002664476C5F0000FF0000030000001A07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700006A64477A6F0000FF0000160000004807FF00015C6447887F0001FF000060000000FB07FF0000AF6447968F0001FF0000160000007707FF0000246447A49F0000FF0000050000001807FF0003376447B2AF0001FF0000CB0000024C07FF0000006447C0BF0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700059E6447CECF0001FF00014A000003F107FF00012E6447D5EE0001FF00003E000000D607FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-05-06 11:56:37  -> 10
        else if (idDebug == 10) {
            Log.e(TAG, "2023-05-06 11:56:37 ");
            contentData = "570001FB6447DCDF0001FF00006B0000016207FF0004D36447EAEF0001FF00012A0000036607FF00023C6447F8FF0001FF000080000001A407FF0001206448070F0001FF00003E000000D807FF0000C26448151F0001FF00001B0000008406FF0001176448232F0001FF00003A000000C706FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001A46448313F0000FF0000550000012506FF00006864483F4F0000FF0000190000004B06FF00000064484D5F0000FF0000000000000006FF00002964485B6F0000FF0000010000001B06FF0000006448697F0000FF0000000000000006FF0000006448778F0000FF0000000000000006FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006448859F0000FF0000000000000006FF00000F644893AF0000FF0000020000000B06FF0000006448A1BF0000FF0000000000000006FF0000186448AFCF0000FF0000020000001006FF0001196448BDDF0000FF000035000000CA06FE00065D6448CBEF0001FF0001590000047506FE";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002316448D9FF0001FF00009E0000019606FE0009106448E80F0001FF00027B0000069306FE00075B6448F61F0001FF0002070000054A06FD0008E56449042F0001FF0002460000065D06FD0000F46449123F0001FF000053000000BC06FD0005226449204F0001FF00020A000003F006FD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064492E5F0000FF0000000000000006FD00029F64493C6F0001FF000092000001CF06FC0000006448F61F0000FF0000000000000006FC00001F6449042F0000FF0000030000001506FB0003A86449123F0001FF0000ED0000028006FB0003666449204F0001FF0001010000027206FB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700022164492E5F0000FF0000680000017806FB0000F864493C6F0000FF00003A000000AC06FB00000064494A7F0000FF0000000000000006FB0000006449588F0000FF0000000000000006FA00001A6449669F0000FF0000000000001106FA000000644974AF0000FF0000000000000006FA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000644982BF0000FF0000000000000006FA000000644990CF0000FF0000000000000006FA00000064499EDF0000FF0000000000000006FA0000826449ACEF0000FF00001E0000005A06FA0000006449BAFF0000FF0000000000000006FA0000006449C90F0000FF0000000000000006F9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570004066449D71F0001FF00016F0000031806F90001E26449E52F0001FF0000670000014E06F90001286449F33F0001FF000037000000D106F9000293644A014F0001FF0000AD000001DE06F80002FF644A0F5F0001FF0000DF0000023106F8000040644A1D6F0001FF0000050000002C06F8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000186644A2B7F0001FF00006B0000011E06F70002EE644A398F0001FF0000870000020706F7000506644A479F0001FF00010E0000037506F700023B644A55AF0001FF00006A0000019506F70001B9644A63BF0001FF0000410000012F06F6000509644A71CF0001FF0001380000038806F6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001FD644A7FDF0000FF00005C0000016406F6000183644A8DEF0000FF0000500000010A06F5000028644A9BFF0000FF0000060000001B06F5000000644AAA0F0000FF0000000000000006F5000000644AB81F0000FF0000000000000006F5000000644AC62F0000FF0000000000000006F5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000644AD43F0000FF0000000000000006F5000000644AE24F0000FF0000000000000006F5000000644AF05F0000FF0000000000000005F5000000644AFE6F0000FF0000000000000005F4000041644B0C7F0000FF0000050000002C05F4000301644B1A8F0001FF00009D0000021B05F4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000678644B289F0001FF0001FA000004D005F400049F644B36AF0001FF00015F0000035405F40004E6644B44BF0001FF0001730000039305F4000000644B52CF0000FF0000000000000005F3000000644B60DF0000FF0000000000000005F3000360644B6EEF0001FF0000D80000026105F3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000077644B7CFF0001FF0000170000005105F300001D644B8B0F0000FF0000020000001305F2000245644B991F0001FF0000910000019705F2000342644BA72F0001FF0000A30000024305F20003CC644BB53F0001FF00010B000002CB05F2000602644BC34F0001FF00016E0000042E05F2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700025C644BD15F0000FF000091000001B305F100007B644BDF6F0000FF0000130000004A05F1000124644BED7F0000FF00002D000000D005F1000000644BFB8F0000FF0000000000000005F1000015644C099F0000FF0000000000000E05F0000000644C17AF0000FF0000000000000005F0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000017644C25BF0000FF0000020000000F05F0000000644C33CF0000FF0000000000000005F0000000644C41DF0000FF0000000000000005F0000000644C4FEF0000FF0000000000000005EF000054644C5DFF0000FF00000C0000003705EF0005D4644C6C0F0001FF00013A0000041605EF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000322644C7A1F0001FF0000F30000024605EE000000644C882F0000FF0000000000000005ED000168644C963F0001FF00006A0000011705EC00064F644CA44F0001FF0001930000047C05EC00032A644CB25F0001FF00009F0000023905EC00021B644CC06F0001FF0000830000017505EC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002CC644CCE7F0001FF000093000001FD05EC000058644CDC8F0001FF0000060000003B05EC000012644CEA9F0000FF0000020000000D05EC000985644CF8AF0001FF0002B6000006C805EB000786644D06BF0001FF0001670000051F05EB00075C644D14CF0001FF0001C00000051705EB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000158644D22DF0000FF0000660000010405EB000288644D30EF0000FF0000A2000001D505EB000000644D3EFF0000FF0000000000000005EA0000E0644D4D0F0000FF0000240000009D05EA000000644D5B1F0000FF0000000000000005EA000000644D692F0000FF0000000000000005EA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000644D773F0000FF0000000000000005EA000000644D854F0000FF0000000000000005EA000000644D935F0000FF0000000000000005EA000000644DA16F0000FF0000000000000005EA000000644DAF7F0000FF0000000000000005EA00000F644DBD8F0000FF0000020000000A05EA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000101644DCB9F0001FF00002D000000B205E9000000644DD9AF0000FF0000000000000005E9000000644DE7BF0000FF0000000000000005E9000000644DF5CF0000FF0000000000000005E9000379644E03DF0001FF0000CA0000028505E900049A644E11EF0001FF0001220000033705E9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000224644E1FFF0001FF00006F0000018305E8000000644E2E0F0000FF0000000000000005E800005F644E3C1F0001FF0000100000004005E8000217644E4A2F0001FF00007F0000017004E800013A644E583F0001FF00003F000000DD04E70002D3644E664F0000FF00007A000001EC04E7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002BA644E745F0000FF0000A4000001F004E7000214644E826F0000FF0000640000017604E7000081644E907F0000FF0000160000005804E6000000644E9E8F0000FF0000000000000004E6000000644EAC9F0000FF0000000000000004E6000023644EBAAF0000FF0000000000001704E6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000644EC8BF0000FF0000000000000004E6000017644ED6CF0000FF0000010000001004E6000000644EE4DF0000FF0000000000000004E6000000644EF2EF0000FF0000000000000004E6000017644F00FF0000FF0000020000001004E500006C644F0F0F0001FF0000150000004A04E5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002B3644F1D1F0001FF0000A1000001FA04E50006F6644F2B2F0001FF0001A40000050204E5000289644F393F0001FF00007B000001CD04E30000E2644F474F0001FF00002B0000009B04E2000278644F555F0001FF000068000001B204E10000C0644F636F0001FF00002B0000008504E1";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000095644F717F0001FF0000190000006604E0000000644F7F8F0000FF0000000000000004E100028D644F8D9F0001FF00007B000001B804E0000535644F9BAF0001FF00012D000003B804E0000036644FA9BF0000FF0000050000002404E00002F1644FB7CF0001FF0000A50000020B04E0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001C6644FC5DF0000FF0000620000014604E00001E1644FD3EF0000FF0000650000014F04E0000000644FE1FF0000FF0000000000000004DF000000644FF00F0000FF0000000000000003DF000000644FFE1F0000FF0000000000000003DF00001064500C2F0000FF0000000000000A03DF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064501A3F0000FF0000000000000003DF0000006450284F0000FF0000000000000003DF0000006450365F0000FF0000000000000003DF00002E6450446F0000FF0000070000002003DF0000006450527F0000FF0000000000000003DF0004A66450608F0001FF0001070000034903DF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001B464506E9F0001FF0000570000012203DE0002C564507CAF0001FF0000AA000001F103DE0002BE64508ABF0001FF0000B2000001FF03DE000000645098CF0000FF0000000000000003DE0007166450A6DF0001FF0001DE000004EE03DE000D6F6450B4EF0001FF0004070000095003DE";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006450C2FF0000FF0000000000000003DE0002966450D10F0001FF0000D2000001DF03DE0001306450DF1F0001FF00003C000000CF03DE0000136450ED2F0000FF0000020000000D03DE00003A6450FB3F0000FF0000050000002703DE00062F6451094F0001FF0001740000045503DD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700031F6451175F0000FF00008D0000022A03DD00003C6451256F0000FF00000A0000002903DD0000236451337F0000FF0000040000001803DD0000006451418F0000FF0000000000000003DD00000064514F9F0000FF0000000000000003DD00000064515DAF0000FF0000000000000003DD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064516BBF0000FF0000000000000003DD000000645179CF0000FF0000000000000003DD000000645187DF0000FF0000000000000003DD00005C645195EF0000FF0000070000003E03DD0000006451A3FF0000FF0000000000000003DD0002286451B20F0001FF00006F0000018403DD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001086451C01F0001FF000040000000A903DD0009276451CE2F0001FF00022E0000068B03DD0007AB6451DC3F0001FF0001AF0000056503DC00055B6451EA4F0001FF000136000003D003DC00042F6451F85F0001FF000132000002DB03DC000EC46452066F0001FF00047900000A5503DC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001246452147F0001FF000059000000DA03DC0006266452228F0001FF0001AE0000044203DC0000F06452309F0001FF000042000000B303DC00002A64523EAF0000FF0000040000001C03DC0006A564524CBF0001FF0001D8000004AF03DC00058364525ACF0001FF00017A000003F003DC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000084645268DF0000FF0000170000005A03DB000139645276EF0000FF000030000000D603DB000000645284FF0000FF0000000000000003DB0000006452930F0000FF0000000000000003DB0000006452A11F0000FF0000000000000003DB0000006452AF2F0000FF0000000000000003DB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006452BD3F0000FF0000000000000003DB00000E6452CB4F0000FF0000000000000903DB0000006452D95F0000FF0000000000000003DB0000556452E76F0000FF00000A0000003A03DB0000006452F57F0000FF0000000000000003DB0004496453038F0001FF0000F0000002F003DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700041E6453119F0001FF000140000002F703DA0000CA64531FAF0001FF00002D0000009B03DA0002FA64532DBF0001FF0000D80000022D03DA00028C64533BCF0001FF0000CB000001E003DA000000645349DF0000FF0000000000000003DA000011645357EF0000FF0000000000000003DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000AA3645365FF0001FF0002DE0000075902DA0006786453740F0001FF0001D70000049002DA0000006453821F0000FF0000000000000002DA0000006453902F0000FF0000000000000002DA00048064539E3F0001FF0001530000034402D90003826453AC4F0001FF0000B80000027402DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001916453BA5F0000FF0000400000011602D900011D6453C86F0000FF000038000000C902D90000246453D67F0000FF0000040000001902D90000006453E48F0000FF0000000000000002D90000006453F29F0000FF0000000000000002D9000000645400AF0000FF0000000000000002D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064540EBF0000FF0000000000000002D900000064541CCF0000FF0000000000000002D900000064542ADF0000FF0000000000000002D9000062645438EF0000FF00000B0000004202D9000000645446FF0000FF0000000000000002D90001B46454550F0001FF0000500000013102D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002746454631F0001FF0000CF000001C902D90003AE6454712F0001FF00010F0000029A02D900036864547F3F0001FF0001110000028002D900004264548D4F0001FF0000080000002C02D80001C064549B5F0001FF0000AE0000015F02D800055F6454A96F0001FF0001DA0000040202D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000C426454B77F0001FF00037B0000088202D80002416454C58F0001FF0000A4000001A002D80001A66454D39F0001FF0000530000012802D80001376454E1AF0001FF000051000000D202D80005A36454EFBF0001FF0001BD0000042502D80004576454FDCF0001FF0000DD0000030F02D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700004A64550BDF0000FF0000150000003602D8000070645519EF0000FF0000290000005802D800003A645527FF0000FF0000050000002702D800003D6455360F0000FF0000070000002902D80000006455441F0000FF0000000000000002D80000006455522F0000FF0000000000000002D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006455603F0000FF0000000000000002D800000064556E4F0000FF0000000000000002D800000064557C5F0000FF0000000000000002D700000064558A6F0000FF0000000000000002D700003A6455987F0000FF00000F0000002802D700010D6455A68F0000FF000033000000B802D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000646455B49F0001FF0000130000004702D70000CF6455C2AF0001FF00002E0000009002D7000000645624C40000FF0000000000000002D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-05-08 20:35:30  -> 11
        else if (idDebug == 11) {
            Log.e(TAG, "2023-05-08 20:35:30 ");
            contentData = "570000216456251F0000FF00000A0000001702D70000606456332F0001FF00000C0000004102D70000F46456413F0001FF0000230000009D02D70006D264564F4F0001FF0001DD000004FB02D700003D64565D5F0001FF0000080000002A02D700015F64566B6F0001FF000053000000FB02D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570003396456797F0001FF0000CD0000025002D70003476456878F0001FF0000AD0000024902D70002546456959F0001FF00008C000001A302D70006786456A3AF0001FF00017F0000048E02D70004626456B1BF0000FF0001620000032302D70000006456BFCF0000FF0000000000000002D6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002866456CDDF0000FF0000CD000001CE02D60003AE6456DBEF0000FF0000FF0000029D02D600001B6456E9FF0000FF0000050000001302D60000A96456F80F0000FF0000210000007402D60000006457061F0000FF0000000000000002D60000006457142F0000FF0000000000000002D6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006457223F0000FF0000000000000001D60000006457304F0000FF0000000000000001D600000064573E5F0000FF0000000000000001D600000064574C6F0000FF0000000000000001D600000064575A7F0000FF0000000000000001D60002306457688F0001FF00004D0000017D01D6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001BE6457769F0001FF00005C0000013B01D60006DB645784AF0001FF00016F000004D201D5000398645792BF0001FF0000E00000029801D50002506457A0CF0001FF000078000001A001D50002096457AEDF0001FF00007C0000017201D40001DB6457BCEF0001FF0000680000014E01D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570005506457CAFF0001FF00012F000003A901D40004436457D90F0001FF00010D0000030701D40000166457E71F0000FF0000010000000F01D30000826457F52F0000FF00000C0000005901D30001286458033F0000FF00002F000000CC01D30001006458114F0000FF000029000000B201D3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064581F5F0000FF0000000000000001D200002364582D6F0000FF0000020000001701D200000F64583B7F0000FF0000020000000B01D20000116458498F0000FF0000010000000B01D20000006458579F0000FF0000000000000001D2000000645865AF0000FF0000000000000001D2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000645873BF0000FF0000000000000001D2000000645881CF0000FF0000000000000001D200004664588FDF0000FF0000090000003001D200000064589DEF0000FF0000000000000001D30008C46458ABFF0001FF0002820000066601D200024C6458BA0F0001FF0000B40000019A01D1";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570003CD6458C81F0001FF0000E6000002AF00D100008B6458D62F0001FF0000120000005F00D100020A6458E43F0001FF0000820000017B00D10002FC6458F24F0001FF0000CD0000021C00D00001AA6459005F0001FF00004A0000012A00D00000B764590E6F0001FF00001E0000007900CF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700031864591C7F0001FF0000BE0000021E00CF0001A964592A8F0001FF00007C0000013F00D90000006459389F0000FF0000000000000007FF000000645941510000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-05-09 09:32:45  -> 12
        else if (idDebug == 12) {
            Log.e(TAG, "2023-05-09 09:32:45 ");
            contentData = "5700018E645946AF0000FF0000440000011107FF000167645954BF0000FF000041000000F707FF0001BC645962CF0000FF0000450000013307FF000098645970DF0000FF0000170000006807FF00000064597EEF0000FF0000000000000007FF00000064598CFF0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064599B0F0000FF0000000000000007FF0000006459A91F0000FF0000000000000007FF0000006459B72F0000FF0000000000000007FF0000006459C53F0000FF0000000000000007FF0000006459D34F0000FF0000000000000007FF00002A6459E15F0000FF0000020000001C07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006459EF6F0000FF0000000000000007FF0003306459F7850001FF0000B00000023A07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-05-16 16:18:52  -> 13
        else if (idDebug == 13) {
            Log.e(TAG, "2023-05-16 16:18:52 ");
            contentData = "5700037F6459FD7F0001FF0000C40000027107FF000051645A0B8F0001FF0000050000003707FF00002A645A199F0000FF0000070000001C07FF000014645A27AF0000FF0000020000000E07FF0000CC645A35BF0001FF0000170000008B07FF00006D645A43CF0001FF0000130000004B07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000182645A51DF0001FF00003B0000010A07FF000422645A5FEF0001FF000110000002FA06FF000197645A6DFF0001FF00004B0000011806FF00014F645A7C0F0001FF000029000000E606FF00069C645A8A1F0001FF0001D0000004D906FF0000E8645A982F0001FF00003C000000AD06FE";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700016C645AA63F0000FF000054000000FF06FE00005E645AB44F0000FF00000A0000004006FD000000645AC25F0000FF0000000000000006FD000000645AD06F0000FF0000000000000006FD000000645ADE7F0000FF0000000000000006FD000000645AEC8F0000FF0000000000000006FD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000645AFA9F0000FF0000000000000006FD000000645B08AF0000FF0000000000000006FD000000645B16BF0000FF0000000000000006FC000040645B24CF0000FF0000160000003406FC000000645B32DF0000FF0000000000000006FC0001B9645B40EF0001FF0000610000012B06FC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700036E645B4EFF0001FF00011A0000029606FB000A17645B5D0F0001FF0002A80000073C06F90007AF645B6B1F0001FF0001F40000056506F9000893645B792F0001FF00026C0000064506F8000496645B873F0001FF0001780000033D06F7000E03645B954F0001FF00049D00000A4006F7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700005B645BA35F0001FF00000C0000003E06F6000089645BB16F0001FF0000150000005E06F5000237645BBF7F0001FF00009F0000019305F40000A1645BCD8F0001FF00000B0000006E05F4000133645BDB9F0001FF00003B000000DA05F30003D7645BE9AF0001FF000126000002CD05F3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000106645BF7BF0000FF00003F000000BF05F200008E645C05CF0000FF0000110000006105F2000055645C13DF0000FF00000A0000003A05F2000000645C21EF0000FF0000000000000005F2000000645C2FFF0000FF0000000000000005F2000000645C3E0F0000FF0000000000000005F2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000015645C4C1F0000FF0000000000000E05F2000000645C5A2F0000FF0000000000000005F1000000645C683F0000FF0000000000000005F1000000645C764F0000FF0000000000000005F100006F645C845F0000FF0000220000005305F1000000645C926F0000FF0000000000000005F1";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002F8645CA07F0001FF0000D20000021605F000006A645CAE8F0001FF0000140000005B05EE0001E3645CBC9F0001FF00007B0000015C05ED0001AA645CCAAF0001FF0000710000013405EC0000CB645CD8BF0001FF00001E0000009005EB0004AB645CE6CF0001FF00015E0000034005EB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000970645CF4DF0001FF0002BE000006A605EA000421645D02EF0001FF00015B000002F705EA000153645D10FF0001FF00006E000000F105E9000000645D1F0F0000FF0000000000000005E9000025645D2D1F0000FF0000020000001905E900058C645D3B2F0001FF00014C000003E504E8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000078645D493F0000FF00000C0000005104E80000CF645D574F0000FF0000170000008D04E7000000645D655F0000FF0000000000000004E7000000645D736F0000FF0000000000000004E7000000645D817F0000FF0000000000000004E7000000645D8F8F0000FF0000000000000004E7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000645D9D9F0000FF0000000000000004E7000000645DABAF0000FF0000000000000004E7000000645DB9BF0000FF0000000000000004E7000000645DC7CF0000FF0000000000000004E6000084645DD5DF0000FF00000E0000005904E6000000645DE3EF0000FF0000000000000004E6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001F9645DF1FF0001FF00008A0000016404E500034C645E000F0001FF0000E30000025104E5000423645E0E1F0001FF0000DF000002E504E3000447645E1C2F0001FF0000F00000030404E30004DF645E2A3F0001FF0001220000036704E200048A645E384F0001FF00014F0000033104E2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700021F645E465F0001FF0000600000018A04E1000000645E546F0000FF0000000000000004E10000BE645E627F0001FF0000250000008304E0000686645E708F0001FF0001C30000049F04DF000297645E7E9F0001FF00009C000001EA03DF000244645E8CAF0001FF00006C0000019503DF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000015645E9ABF0000FF0000020000000F03DE00002F645EA8CF0000FF0000070000002003DE0000A5645EB6DF0000FF00002B0000007703DE000000645EC4EF0000FF0000000000000003DE000015645ED2FF0000FF0000000000000E03DE000000645EE10F0000FF0000000000000003DE";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000645EEF1F0000FF0000000000000003DE000000645EFD2F0000FF0000000000000003DE000000645F0B3F0000FF0000000000000003DD000000645F194F0000FF0000000000000003DD000000645F275F0000FF0000000000000003DD0000DF645F356F0001FF0000220000009C03DD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000645F437F0000FF0000000000000003DD000000645F518F0000FF0000000000000003DD000033645F5F9F0000FF0000050000001403DD0008B9645F6DAF0001FF00028F0000063F03DC000750645F7BBF0001FF0001AF000004FF03DC000C55645F89CF0001FF000335000008A303DC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001E7645F97DF0001FF0000620000015C03DB000073645FA5EF0001FF0000190000004E03DB000012645FB3FF0000FF0000010000000C03DB0001A1645FC20F0001FF00004B0000011103DB000488645FD01F0001FF0001130000034602DA00018E645FDE2F0001FF00007B0000011702D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000645FEC3F0000FF0000000000000002D90000B6645FFA4F0000FF00001C0000007D02D80000106460085F0000FF0000020000000B02D900005C6460166F0000FF0000180000004702D90002656460247F0000FF000081000001AD02D90000006460328F0000FF0000000000000002D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006460409F0000FF0000000000000002D800000064604EAF0000FF0000000000000002D800000064605CBF0000FF0000000000000002D800000064606ACF0000FF0000000000000002D8000000646078DF0000FF0000000000000002D80000EF646086EF0001FF000021000000A202D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000AA646094FF0001FF0000200000007602D80000006460A30F0000FF0000000000000002D80000006460B11F0000FF0000000000000002D80000006460BF2F0000FF0000000000000002D800024D6460CD3F0001FF00007D0000018202D700036C6460DB4F0001FF0000CE0000028202D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002316460E95F0001FF0000750000018E02D70000266460F76F0000FF0000050000001A02D60002A96461057F0001FF000078000001CF01D60000006461138F0000FF0000000000000001D60000006461219F0000FF0000000000000001D600020F64612FAF0001FF0000460000016801D5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001B364613DBF0000FF0000410000012C01D500033F64614BCF0000FF00010A0000028801D5000000646159DF0000FF0000000000000001D5000000646167EF0000FF0000000000000001D5000012646175FF0000FF0000000000000C01D40000006461840F0000FF0000000000000001D5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006461921F0000FF0000000000000001D40000116461A02F0000FF0000020000000B01D40000006461AE3F0000FF0000000000000001D40000276461BC4F0000FF0000030000001B01D50000006461CA5F0000FF0000000000000001D500046F6461D86F0001FF0000F10000030801D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570005C26461E67F0001FF0001BA0000042601D40000786461F48F0001FF0000140000005201D30001F76462029F0001FF0000AE0000017801D300000E646210AF0000FF0000010000000901D300023264621EBF0001FF00005C0000018201D30002AA64622CCF0001FF0000BA000001E501D3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700011364623ADF0001FF00006E000000E901D2000282646248EF0001FF00009B000001CD01D20001D2646256FF0001FF00003E0000013D01D200042D6462650F0001FF000100000002E901D20003826462731F0001FF0000F50000029200D10006BB6462812F0001FF0001B0000004D700D1";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700016264628F3F0000FF00004D000000F800D00000F264629D4F0000FF000034000000AA00D00000286462AB5F0000FF0000050000001B00D000000F6462B96F0000FF0000000000000A00D00000006462C77F0000FF0000000000000000D00000006462D58F0000FF0000000000000000CF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006462E39F0000FF0000000000000000CF0000006462F1AF0000FF0000000000000000CF0000006462FFBF0000FF0000000000000000CF00003C64630DCF0000FF00000A0000002900D000000064631BDF0000FF0000000000000000D00006B8646329EF0001FF000165000004A000CF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000119646337FF0001FF00006A000000EB01E10000006463460F0000FF0000000000000007FF0000006463541F0000FF0000000000000007FF0000006463622F0000FF0000000000000007FF0000006463703F0000FF0000000000000007FF00019A64637E4F0001FF00003E0000011D07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700032464638C5F0001FF0000F70000024007FF0003C26463912F0001FF0000EF000002A907FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-05-16 17:28:26  -> 14
        else if (idDebug == 14) {
            Log.e(TAG, "2023-05-16 17:28:26 ");
            contentData = "5700054764639A6F0001FF000150000003C507FF0000206463A1900000FF0000010000001607FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-05-17 08:31:05  -> 15
        else if (idDebug == 15) {
            Log.e(TAG, "2023-05-17 08:31:05 ");
            contentData = "570001796463A87F0001FF0000490000010307FF0000006463B68F0000FF0000000000000007FF0000B06463C49F0001FF00001B0000007707FF0000A66463D2AF0001FF0000210000007407FF0003B36463E0BF0000FF0000B10000029707FF0001496463EECF0000FF00005B000000F107FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006463FCDF0000FF0000000000000007FF00000D64640AEF0000FF0000000000000807FF000000646418FF0000FF0000000000000007FF0000006464270F0000FF0000000000000007FF0000006464351F0000FF0000000000000007FF0000006464432F0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006464513F0000FF0000000000000007FF00003464645F4F0000FF0000050000002407FF00000064646D5F0000FF0000000000000007FF00001C646475070000FF0000050000001307FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-05-21 22:56:59  -> 16
        else if (idDebug == 16) {
            Log.e(TAG, "2023-05-21 22:56:59");
            contentData = "5700046E64647B6F0001FF0001030000032307FF0002006464897F0001FF0000900000016707FF0007DA6464978F0001FF00020D000005C407FF0006356464A59F0001FF0001330000044A06FF0005166464B3AF0001FF00014F0000039D06FF0005A56464C1BF0001FF0001C7000003FE06FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000CB46464CFCF0001FF0003D2000008C906FF0000006464DDDF0000FF0000000000000006FE00057C6464EBEF0001FF000179000003E506FD0001056464F9FF0001FF000034000000B706FC0001686465080F0001FF00004F0000010506FB00030C6465161F0001FF0000A20000021206FA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001006465242F0001FF00003D000000C106FA00011A6465323F0000FF000044000000C706F90001506465404F0000FF000055000000EE06F800000064654E5F0000FF0000000000000006F800000064655C6F0000FF0000000000000006F800000064656A7F0000FF0000000000000006F8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006465788F0000FF0000000000000006F80000006465869F0000FF0000000000000006F8000000646594AF0000FF0000000000000006F70000006465A2BF0000FF0000000000000006F70000196465B0CF0000FF0000000000001106F70000496465BEDF0000FF0000100000003106F7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570009C76465CCEF0001FF0002970000070606F60005876465DAFF0001FF0001A8000003F806F5000000646594AF0000FF0000000000000005F40003656465A2BF0001FF0000E10000026A05F30000256465B0CF0000FF00000D0000001C05F30003906465BEDF0001FF00011A0000028105F2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570009216465CCEF0001FF00029E0000063B05F20004AC6465DAFF0001FF00014F0000033B05F20001E26465E90F0001FF0000710000015005F10002146465F71F0001FF0000850000018505F10000176466052F0000FF0000020000001005F00000F96466133F0001FF00002F000000B105EF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700028C6466214F0001FF000064000001B205ED00003664662F5F0000FF0000080000003305ED00023664663D6F0000FF0000640000018A05EC00002A64664B7F0000FF0000070000001C05ED0000006466598F0000FF0000000000000005EC0000006466679F0000FF0000000000000005EC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000646675AF0000FF0000000000000005EC000000646683BF0000FF0000000000000005EC000000646691CF0000FF0000000000000005EC00000064669FDF0000FF0000000000000005EC0000336466ADEF0000FF0000040000002205EB0000006466BBFF0000FF0000000000000005EB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700024A6466CA0F0001FF0000A4000001A405EB0001C06466D81F0001FF0000B00000015805EA0000556466E62F0001FF0000170000004005EA0000456466F43F0001FF0000050000002F05EA0002F56467024F0001FF0000ED0000021305E90004D96467105F0001FF0001CF000003CB04E8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700079164671E6F0001FF00023A0000055B04E800026F64672C7F0001FF000079000001B304E700010464673A8F0001FF000043000000BD04E600017F6467489F0001FF00003F0000010504E50000B2646756AF0001FF0000190000007904E4000057646764BF0001FF00000D0000003C04E3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700035A646772CF0001FF0000930000024904E3000091646780DF0000FF0000190000006504E20000F264678EEF0000FF000030000000A804E100002864679CFF0000FF0000040000001B04E10000006467AB0F0000FF0000000000000004E10000696467B91F0000FF00000B0000004704E1";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006467C72F0000FF0000000000000004E10000006467D53F0000FF0000000000000004E10000006467E34F0000FF0000000000000004E10000006467F15F0000FF0000000000000004E10000006467FF6F0000FF0000000000000004E000006064680D7F0000FF0000160000004204E0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700002364681B8F0000FF0000030000001804E000009E6468299F0001FF00001D0000006C03DF0002EF646837AF0001FF0000AE0000021A03DF00066D646845BF0001FF00015D0000047E03DE00034D646853CF0001FF0000B50000025C03DE000000646861DF0000FF0000000000000003DE";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700016964686FEF0001FF00005F0000010B03DD0003C164687DFF0001FF0000E9000002A803DD00042164688C0F0001FF0000E2000002E103DD0000B864689A1F0001FF00002B0000008303DD0000316468A82F0000FF00000A0000002103DD0001046468B63F0001FF00002D000000B103DD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700001B6468C44F0000FF0000050000001203DD0000006468D25F0000FF0000000000000003DD0000486468E06F0000FF0000090000003103DD0003326468EE7F0000FF0000850000022A03DD0002036468FC8F0000FF0000530000015A03DD0001F364690A9F0000FF00005F0000015C03DD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000B4646918AF0000FF0000210000007B03DD000000646926BF0000FF0000000000000003DD000000646934CF0000FF0000000000000003DD000000646942DF0000FF0000000000000003DC0000E9646950EF0000FF000040000000AB03DC00006464695EFF0000FF0000120000004403DC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700003964696D0F0000FF0000070000002703DC00003D64697B1F0001FF0000050000002A03DC0000F76469892F0001FF000030000000A803DC0002326469973F0001FF0000870000019103DC00021E6469A54F0001FF0000930000019203DC0003516469B35F0001FF0000C60000026903DC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570004D36469C16F0001FF0001340000037703DC0002076469CF7F0001FF0000460000016403DC0002206469DD8F0001FF0000610000017A03DB0000006469EB9F0000FF0000000000000003DC0000006469F9AF0000FF0000000000000003DB0002BD646A07BF0001FF00007E000001EF03DB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000306646A15CF0001FF00009C0000022003DB00012A646A23DF0000FF00003B000000D003DB000201646A860E0000FF00005D0000016503DB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-06-03 13:22:26  -> 17
        else if (idDebug == 17) {
            Log.e(TAG, "2023-06-03 13:22:26");
            contentData = "57000201646A864F0000FF00005D0000016503DB000089646A945F0000FF0000170000005D03DB000000646AA26F0000FF0000000000000003DB000000646AB07F0000FF0000000000000003DB000000646ABE8F0000FF0000000000000003DB000000646ACC9F0000FF0000000000000003DB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000646ADAAF0000FF0000000000000003DB000000646AE8BF0000FF0000000000000003DB000000646AF6CF0000FF0000000000000003DB0000ED646B04DF0000FF000031000000A603DB000000646B12EF0000FF0000000000000003DB0003F3646B20FF0001FF0000E6000002DD03DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570005B1646B2F0F0001FF0001A1000003F303DA000439646B3D1F0001FF0001140000030F03DA000079646B4B2F0001FF00000F0000005203DA00013C646B593F0001FF000049000000E103DA0004CE646B674F0001FF0001820000039003DA0001AE646B755F0001FF0000560000012D02DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700014B646B836F0001FF00003A000000E802DA0000F8646B917F0001FF000027000000AA02DA000133646B9F8F0001FF000051000000CA02DA000A08646BAD9F0001FF0003030000071A02D90006C0646BBBAF0001FF0001A4000004CD02D900010B646BC9BF0000FF000021000000B602D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000A0646BD7CF0000FF0000160000006C02D900001C646BE5DF0000FF0000050000001302D9000000646BF3EF0000FF0000000000000002D9000000646C01FF0000FF0000000000000002D9000000646C100F0000FF0000000000000002D9000000646C1E1F0000FF0000000000000002D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000646C2C2F0000FF0000000000000002D9000000646C3A3F0000FF0000000000000002D9000000646C484F0000FF0000000000000002D90001A2646C565F0000FF0000550000012302D9000013646C646F0000FF0000020000000D02D90007B1646C727F0001FF0001BE0000055502D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002CD646C808F0001FF0000C2000001EB02D9000479646C8E9F0001FF0001560000033502D9000000646C9CAF0000FF0000000000000002D90003EE646CAABF0001FF000120000002CB02D8000BE4646CB8CF0001FF0003660000082D02D8000337646CC6DF0001FF0001050000025902D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700032A646CD4EF0001FF0000F70000023E02D800043C646CE2FF0001FF0000D2000002F002D8000080646CF10F0001FF0000120000005802D80007A0646CFF1F0001FF0001CE0000055002D80002BD646D0D2F0001FF00008C000001E902D80000AB646D1B3F0000FF0000260000007502D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000A7646D294F0000FF00001E0000007202D800001C646D375F0000FF0000050000001302D8000000646D456F0000FF0000000000000002D8000000646D537F0000FF0000000000000002D8000000646D618F0000FF0000000000000002D8000000646D6F9F0000FF0000000000000002D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700001C646D7DAF0000FF0000000000001302D8000000646D8BBF0000FF0000000000000002D8000000646D99CF0000FF0000000000000002D80000C7646DA7DF0000FF0000260000008B02D800028B646DB5EF0001FF00006C000001BD02D80002CA646DC3FF0001FF0000E40000020F02D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000952646DD20F0001FF00026E000006BB02D7000524646DE01F0001FF00015C000003AF02D7000E47646DEE2F0001FF0004DC00000AA402D700023A646DFC3F0001FF0000C3000001A402D7000C63646E0A4F0001FF0004570000095C02D70001CE646E185F0001FF0000680000014902D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000AF646E266F0001FF00002D0000008102D7000391646E347F0001FF0001050000028A02D70004EF646E428F0001FF00015D0000037D02D7000213646E509F0001FF0000720000017F02D7000218646E5EAF0001FF0000520000016E02D7000000646E6CBF0000FF0000000000000002D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001C7646E7ACF0000FF0000640000014102D7000000646E88DF0000FF0000000000000002D7000000646E96EF0000FF0000000000000002D7000000646EA4FF0000FF0000000000000002D7000000646EB30F0000FF0000000000000002D7000000646EC11F0000FF0000000000000002D6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000646ECF2F0000FF0000000000000002D6000000646EDD3F0000FF0000000000000002D6000126646EEB4F0000FF000032000000CC02D6000000646EF95F0000FF0000000000000002D600067F646F076F0001FF00018B0000049602D6000002646F157F0000FF0000010000000F02D6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000646F238F0000FF0000000000000001D600029D646F319F0001FF0000B6000001DE01D6000336646F3FAF0001FF0000F70000024A01D600005B646F4DBF0001FF00000A0000003D01D600036E646F5BCF0001FF0000F60000026B01D5000254646F69DF0001FF0000770000019901D5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000157646F77EF0001FF000033000000E901D5000000646F85FF0000FF0000000000000001D5000038646F940F0000FF00000F0000002901D50004CC646FA21F0001FF0001300000036501D5000189646FB02F0000FF0000560000011301D500014B646FBE3F0000FF000055000000F401D5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700007F646FCC4F0000FF00001E0000005801D5000000646FDA5F0000FF0000000000000001D5000029646FE86F0000FF0000000000002201D5000000646FF67F0000FF0000000000000001D40000006470048F0000FF0000000000000001D50000006470129F0000FF0000000000000001D5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000F647020AF0000FF0000020000000A01D500000064702EBF0000FF0000000000000001D400007F64703CCF0000FF00000F0000005601D500000064704ADF0000FF0000000000000001D5000000647058EF0000FF0000000000000001D5000000647066FF0000FF0000000000000001D5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000416470750F0001FF0000050000002C01D40001256470831F0001FF000030000000CD01D40003066470912F0001FF0000910000021501D400012164709F3F0001FF00003D000000D001D40002246470AD4F0001FF0000C90000019E01D40002026470BB5F0001FF00008B0000017901D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000116470C96F0000FF0000020000000C01D40000E16470D77F0001FF0000230000009B01D40000C16470E58F0001FF00001B0000008301D400068F6470F39F0001FF000209000004C601D4000227647101AF0001FF0000940000019A01D40002D364710FBF0000FF000072000001EC01D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000E64711DCF0000FF0000010000000901D300010464712BDF0000FF00002F000000B301D3000028647139EF0000FF0000020000001B01D3000000647147FF0000FF0000000000000001D30000006471560F0000FF0000000000000001D30000006471641F0000FF0000000000000001D3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006471722F0000FF0000000000000001D30000006471803F0000FF0000000000000001D200000064718E4F0000FF0000000000000001D200000064719C5F0000FF0000000000000001D20000A36471AA6F0001FF00001A0000007101D30000006471B87F0000FF0000000000000001D3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006471C68F0000FF0000000000000001D30002A16471D49F0001FF000090000001DD01D30004416471E2AF0001FF0000F3000002E501D20007116471F0BF0001FF0001AF000004FA01D20001656471FECF0001FF000034000000F201D200000064720CDF0000FF0000000000000001D2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700013E64721AEF0001FF000046000000D901D20003CD647228FF0001FF0000C8000002A001D20006DD6472370F0001FF0002360000051B01D20000006472451F0000FF0000000000000001D20000006472532F0000FF0000000000000001D20006916472613F0000FF0001C9000004A901D2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700003D64726F4F0000FF0000030000001B00D100042D64727D5F0000FF0000F6000002FC00D10000B164728B6F0000FF0000240000007C00D10000006472997F0000FF0000000000000000D10000006472A78F0000FF0000000000000000D10000006472B59F0000FF0000000000000000D1";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006472C3AF0000FF0000000000000000D10000006472D1BF0000FF0000000000000000D10001296472DFCF0000FF000055000000D800D10005BA6472EDDF0000FF00016E0000041600D10002966472FBEF0001FF0000AA000001CC00D100062C647309FF0001FF00020C000004A000D1";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700028E6473180F0001FF000072000001BE00D00003126473261F0001FF0000A10000022F00D00003346473342F0001FF0000940000023700D000036E6473423F0001FF0000C50000026900D00003146473504F0001FF0000A80000022400D000001164735E5F0000FF0000020000000C00CF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700038064736C6F0001FF0000CF0000027000CF00000064737A7F0000FF0000000000000000CF0003206473888F0001FF00006E0000022000CF0004F96473969F0001FF000140000003A600CF0001F16473A4AF0001FF0000640000015A00CF0001336473B2BF0000FF000040000000E300CE";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000516473C0CF0000FF00001A0000004100CE0000006473CEDF0000FF0000000000000003E80000006473DCEF0000FF0000000000000007FF0000006473EAFF0000FF0000000000000007FF0000006473F90F0000FF0000000000000007FF0000006474071F0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006474152F0000FF0000000000000007FF0000006474233F0000FF0000000000000007FF0000006474314F0000FF0000000000000007FF00000064743F5F0000FF0000000000000007FF0001C464744D6F0001FF0000590000012607FF00075B64745B7F0001FF0002360000053B07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000796474698F0001FF00001B0000005507FF0002A76474779F0001FF000082000001D207FF000247647485AF0001FF0000770000019B07FF0002C0647493BF0001FF000089000001E507FF0001976474A1CF0001FF00005C0000010A07FF00017E6474AFDF0001FF00003F0000011007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001486474BDEF0001FF00002D000000DF07FF0001366474CBFF0001FF000019000000C307FF0003DF6474DA0F0001FF00010B000002D907FF00027C6474E81F0001FF00006C000001B607FF0002876474F62F0001FF000083000001CB07FF00021A6475043F0000FF0000790000017207FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001AB6475124F0000FF0000710000013D07FF00000D6475205F0000FF0000000000000907FF00000064752E6F0000FF0000000000000007FF00000064753C7F0000FF0000000000000007FF00000064754A8F0000FF0000000000000007FF0000006475589F0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000647566AF0000FF0000000000000007FF00000F647574BF0000FF0000000000000A07FF000024647582CF0000FF00000E0000001A07FF0000E2647590DF0000FF000054000000B807FF00043364759EEF0001FF00011D0000030207FF00021C6475ACFF0001FF0000B8000001AB07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006475BB0F0000FF0000000000000007FF0000006475C91F0000FF0000000000000007FF0005A76475D72F0001FF0001AC0000040907FF00014D6475E53F0001FF000061000000DA07FF00081D6475F34F0001FF00027B0000062407FF0001A76476015F0001FF00003A0000011607FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700007F64760F6F0001FF0000230000006F07FF00000064761D7F0000FF0000000000000007FF00008F64762B8F0001FF00000F0000006107FF0000C46476399F0001FF00002D0000008A07FF000154647647AF0001FF000055000000EF07FF000125647655BF0000FF000030000000D707FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000137647663CF0000FF00004B000000E207FF000013647671DF0000FF0000020000000D07FF00000064767FEF0000FF0000000000000007FF00000E64768DFF0000FF0000000000000907FF00000064769C0F0000FF0000000000000007FF0000006476AA1F0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006476B82F0000FF0000000000000007FF0000106476C63F0000FF0000020000000B07FF00005D6476D44F0000FF0000110000003F07FF0000006476E25F0000FF0000000000000007FF00034D6476F06F0001FF0000F60000026007FF0000E96476FE7F0001FF0000390000009506FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000B0964770C8F0001FF0003040000080706FF0004B164771A9F0001FF0001020000034A06FF0005FE647728AF0001FF00014C0000041F06FF00082B647736BF0001FF00022B000005B006FF000DD9647744CF0001FF000413000009B106FF000000647752DF0000FF0000000000000006FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700048F647760EF0001FF0001160000033006FF00004164776EFF0001FF0000090000002D06FF00052264777D0F0001FF0001520000038A06FF0002D964778B1F0001FF0000B60000020B06FF00016C6477992F0001FF00002D000000F806FF0001286477A73F0000FF00003E000000D006FE";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001026477B54F0000FF00002D000000B706FD0000006477C35F0000FF0000000000000006FD0000006477D16F0000FF0000000000000006FD0000006477DF7F0000FF0000000000000006FD0000006477ED8F0000FF0000000000000006FD0000006477FB9F0000FF0000000000000006FD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000647809AF0000FF0000000000000006FD000000647817BF0000FF0000000000000006FD00002D647825CF0000FF0000020000001E06FC00001F647833DF0000FF0000020000000D06FC0004CF647841EF0001FF0000D60000036306FB0000FA64784FFF0001FF00002F000000B006FA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064785E0F0000FF0000000000000006FA00002364786C1F0000FF0000030000001806FA00002364787A2F0000FF0000040000001806F90001AC6478883F0001FF00004B0000012706F80000246478964F0000FF0000030000001806F80000226478A45F0000FF0000050000001706F7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000126478B26F0000FF0000000000000D06F60000F46478C07F0001FF00003A000000AB06F60003926478CE8F0001FF000123000002AB05F40002466478DC9F0001FF00005E0000018E05F400015A6478EAAF0000FF000051000000DE05F40003596478F8BF0000FF0000D30000027405F3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001DF647906CF0000FF00005A0000014F05F3000000647914DF0000FF0000000000000005F3000000647922EF0000FF0000000000000005F3000000647930FF0000FF0000000000000005F300000064793F0F0000FF0000000000000005F300000064794D1F0000FF0000000000000005F3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064795B2F0000FF0000000000000005F200002B6479693F0000FF0000020000001D05F20000636479774F0000FF00000C0000004305F20001346479855F0000FF00003F000000D705F20004336479936F0001FF0000E7000002FB05F10000316479A17F0000FF00001A0000002D05F1";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000666479AF8F0001FF0000090000004505F00000136479BD9F0000FF0000010000000D05EF0000006479CBAF0000FF0000000000000005EE00025C6479D9BF0001FF0000A2000001A105EC0005E26479E7CF0001FF0001810000041C05EC0000386479F5DF0000FF0000050000002605EB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000034647A03EF0000FF0000070000002305EB00023E647A11FF0001FF0000910000019C05EA0006BF647A200F0001FF0001F0000004E005E9000482647A2E1F0001FF0000F80000031D05E8000018647A3C2F0000FF0000020000001104E80001D9647A4A3F0000FF0000410000014104E7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700007A647A584F0000FF0000160000004904E70000A5647A665F0000FF0000280000008004E70000A9647A746F0000FF00001B0000007404E7000000647A827F0000FF0000000000000004E7000010647A908F0000FF0000010000000B04E6000000647A9E9F0000FF0000000000000004E6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000647AACAF0000FF0000000000000004E6000014647ABABF0000FF0000020000000E04E6000000647AC8CF0000FF0000000000000004E6000000647AD6DF0000FF0000000000000004E600003F647AE4EF0001FF0000050000002B04E500020C647AF2FF0001FF0000570000016804E4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000647B010F0000FF0000000000000004E40000B4647B0F1F0001FF0000230000007C04E400014D647B1D2F0001FF00003A000000F104E300025E647B22F10001FF0000C8000001CA04E2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-06-05 08:50:18  -> 18
        else if (idDebug == 18) {
            Log.e(TAG, "2023-06-05 08:50:18");
            contentData = "570003AB647B2B3F0001FF000132000002C204E2000396647B394F0001FF000169000002D504E1000042647B475F0001FF0000090000002C04E0000000647B556F0000FF0000000000000003DF00006F647B637F0001FF00000D0000004C03DE000104647B718F0001FF000048000000B903DD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000C6647B7F9F0001FF0000230000008703DD00069D647B8DAF0001FF000206000004A403DC0003E9647B9BBF0000FF0000D7000002A903DC000011647BA9CF0000FF0000020000000C03DC000242647BB7DF0000FF0000690000018803DC0001D6647BC5EF0000FF0000470000012F03DC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700053F647BD3FF0000FF00017A000003BC03DB0000F6647BE20F0000FF00002A000000A803DB000032647BF01F0000FF0000060000002203DB000000647BFE2F0000FF0000000000000003DB000000647C0C3F0000FF0000000000000003DB000000647C1A4F0000FF0000000000000003DB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700002D647C285F0000FF0000070000001E03DB000000647C366F0000FF0000000000000003DB00024D647C447F0001FF0000640000019A03DB00062B647C528F0001FF0001AB0000046003DA0000EF647C609F0001FF000048000000A903DA000343647C6EAF0001FF0001240000026903DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001CB647C7CBF0001FF0000580000013E02DA00045E647C8ACF0001FF00011C0000032202D9000225647C98DF0001FF00005A0000017502D9000000647CA6EF0000FF0000000000000002D9000083647CB4FF0001FF0000140000005B02D9000129647CC30F0001FF000039000000D102D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000D4647CD11F0001FF0000320000009402D9000062647CDF2F0000FF00000A0000004302D80002DD647CED3F0000FF000091000001FB02D800019A647CFB4F0000FF0000430000011C02D80000D4647D095F0000FF0000300000009302D8000000647D176F0000FF0000000000000002D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000647D257F0000FF0000000000000002D8000000647D338F0000FF0000000000000002D8000000647D419F0000FF0000000000000002D8000000647D4FAF0000FF0000000000000002D8000000647D5DBF0000FF0000000000000002D8000000647D6BCF0000FF0000000000000002D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000089647D79DF0000FF00000F0000005D02D800003A647D86050000FF00000C0000002702D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-06-06 09:19:07  -> 19
        else if (idDebug == 19) {
            Log.e(TAG, "2023-06-05 08:50:18");
            contentData = "570000FE647D87EF0001FF0000220000009D02D800081C647D95FF0001FF000230000005BF02D7000089647DA40F0001FF0000100000005D02D70002BE647DB21F0001FF0000BB000001F902D7000237647DC02F0001FF0000640000018C02D600011E647DCE3F0001FF000029000000C702D6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700028F647DDC4F0001FF00009A000001B601D6000120647DEA5F0001FF00003F000000DB01D5000230647DF86F0001FF00007B0000018201D500013E647E067F0001FF000031000000DB01D5000100647E148F0001FF00003D000000B701D500042A647E229F0001FF00010B000002F801D5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000A2C647E30AF0001FF00029C0000072301D500044C647E3EBF0000FF0000EB0000030101D500014B647E4CCF0000FF000037000000E501D5000000647E5ADF0000FF0000000000000001D5000000647E68EF0000FF0000000000000001D5000000647E76FF0000FF0000000000000001D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000647E850F0000FF0000000000000001D4000000647E931F0000FF0000000000000001D4000000647EA12F0000FF0000000000000001D4000000647EAF3F0000FF0000000000000001D4000000647EBD4F0000FF0000000000000001D4000033647ECB5F0000FF0000020000002201D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000647ED96F0000FF0000000000000001D4000010647EDE480000FF0000020000000B01D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }

        //2023-06-27 19:54:27 Download
        else if (idDebug == 20) {
            Log.e(TAG, "2023-06-27 19:54:27 Download");
            contentData = "57000010647EE77F0000FF0000020000000B05F9000000647EF58F0000FF0000000000000007FF000000647F039F0000FF0000000000000007FF000191647F11AF0001FF00006A0000011B07FF000420647F1FBF0001FF000154000002E807FF000EA1647F2DCF0001FF0004BB00000A7A07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000242647F3BDF0001FF00007E0000018E07FF0000B7647F49EF0001FF0000280000007F07FF0000C4647F57FF0001FF0000250000008607FF0004E4647F660F0001FF0001500000038C07FF000290647F741F0001FF0000AD000001DC07FF000095647F822F0001FF0000270000006D07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000267647F903F0000FF000074000001A907FF00005E647F9E4F0000FF00000A0000004007FF000000647FAC5F0000FF0000000000000007FF000000647FBA6F0000FF0000000000000007FF000000647FC87F0000FF0000000000000007FF000000647FD68F0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000647FE49F0000FF0000000000000007FF000000647FF2AF0000FF0000000000000007FF00003A648000BF0000FF0000070000002707FF00006664800ECF0000FF00000D0000004507FF00000064801CDF0000FF0000000000000007FF00050E64802AEF0001FF000145000003AF07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000223648038FF0001FF0000A20000017207FF0007BF6480470F0001FF000204000005AE07FF0006076480551F0001FF0001900000044407FF00055C6480632F0001FF00018E000003FB06FF00058C6480713F0001FF0001DA0000040606FF000BAB64807F4F0001FF0003E00000085006FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064808D5F0000FF0000000000000006FE0001B864809B6F0001FF00007F0000013C06FE0004FD6480A97F0001FF000198000003B606FD0000356480B78F0000FF00000C0000002406FC0005DD6480C59F0001FF0001810000042106FB0001366480D3AF0001FF00004A000000E006FB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700004A6480E1BF0000FF0000080000003306FA00020A6480EFCF0000FF0000780000017C06FA0000006480FDDF0000FF0000000000000006FA00000064810BEF0000FF0000000000000006FA000000648119FF0000FF0000000000000006FA0000006481280F0000FF0000000000000006FA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006481361F0000FF0000000000000006FA0000006481442F0000FF0000000000000006F90000006481523F0000FF0000000000000006F90000006481604F0000FF0000000000000006F900002B64816E5F0000FF0000010000001D06F90002BB64817C6F0001FF0000B10000020106F8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002E964818A7F0001FF00006B000001FF06F70000126481988F0000FF0000010000000C06F500007C6481A69F0001FF0000110000005505F40002E36481B4AF0001FF000099000001FB05F30002876481C2BF0001FF0000BC000001DB05F30004AC6481D0CF0001FF0001A60000038F05F3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700024B6481DEDF0001FF0000D3000001AB05F20000A26481ECEF0001FF0000210000007005F10000A46481FAFF0001FF0000200000007505F10000156482090F0000FF0000010000000F05F10003986482171F0001FF0001100000029205EF0001B16482252F0001FF0000690000014105EE";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006482333F0000FF0000000000000005EE00009F6482414F0000FF0000410000008205EE00014264824F5F0000FF00004B000000E005ED00000064825D6F0000FF0000000000000005ED00000064826B7F0000FF0000000000000005ED0000006482798F0000FF0000000000000005ED";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006482879F0000FF0000000000000005ED000000648295AF0000FF0000000000000005ED0000006482A3BF0000FF0000000000000005ED0000006482B1CF0000FF0000000000000005EC0000516482BFDF0000FF0000090000003705EC0001306482CDEF0001FF000041000000D305EC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001D66482DBFF0001FF00008B0000016005EB0001696482EA0F0001FF00006E0000010205EA0002566482F81F0001FF0000C2000001C505EA00039A6483062F0001FF000158000002C505E90003106483143F0001FF0000FA0000023604E700001C6483224F0000FF0000040000001304E6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570004D66482DBFF0001FF00013E0000036404E40000BF6482EA0F0001FF00002D0000008B04E40000276482F81F0000FF0000100000001F04E30003B16483062F0001FF0000D7000002A304E20000716483143F0001FF0000120000005004E10001746483224F0001FF00004B0000010404E1";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001276483305F0000FF00003C000000D204E100024A64833E6F0000FF00007A0000019A04E000000064834C7F0000FF0000000000000004E000001564835A8F0000FF0000000000000E04E00000006483689F0000FF0000000000000004E0000000648376AF0000FF0000000000000004E0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000648384BF0000FF0000000000000004E0000000648392CF0000FF0000000000000004DF0000006483A0DF0000FF0000000000000004DF0000006483AEEF0000FF0000000000000003DF0000006483BCFF0000FF0000000000000003DF0007036483CB0F0001FF0001790000050103DF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001646483D91F0001FF000048000000F503DE0000B76483E72F0001FF0000310000008D03DE0004DB6483F53F0001FF0000DD0000036503DD0002DC6484034F0001FF0000990000020203DD0000E26484115F0001FF0000370000009C03DC00013864841F6F0001FF000055000000DA03DC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700022964842D7F0001FF0000830000018603DC00013464843B8F0001FF000031000000D103DC0002BA6484499F0001FF0000A8000001F103DB000617648457AF0001FF00012F0000043D03DB00021A648465BF0001FF00004B0000016E03DB00041B648473CF0001FF000102000002FA03DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000254648481DF0000FF00008A000001A102D900000064848FEF0000FF0000000000000002D800012D64849DFF0000FF00004E000000D302D900017C6484AC0F0000FF0000400000010D02D800008F6484BA1F0000FF0000110000006202D80000006484C82F0000FF0000000000000002D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006484D63F0000FF0000000000000002D800002B6484E44F0000FF0000030000001D02D80000006484F25F0000FF0000000000000002D80000006485006F0000FF0000000000000002D800001364850E7F0000FF0000020000000D02D800000064851C8F0000FF0000000000000002D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700008E64852A9F0001FF0000190000006002D8000000648538AF0000FF0000000000000002D8000000648546BF0000FF0000000000000002D8000000648554CF0000FF0000000000000002D8000000648562DF0000FF0000000000000002D8000000648570EF0000FF0000000000000002D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700005264857EFF0001FF00000C0000003802D700000064858D0F0000FF0000000000000002D700022B64859B1F0001FF00005A0000017D02D70000B96485A92F0001FF0000200000008102D60006586485B73F0001FF0001930000047F02D60008E16485C54F0001FF0002550000063201D6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000456485D35F0000FF0000080000002E01D500011B6485E16F0000FF000033000000C701D50000006485EF7F0000FF0000000000000001D50000006485FD8F0000FF0000000000000001D500000064860B9F0000FF0000000000000001D5000000648619AF0000FF0000000000000001D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000648627BF0000FF0000000000000001D4000000648635CF0000FF0000000000000001D4000000648643DF0000FF0000000000000001D400001E648651EF0000FF0000000000001401D400000064865FFF0000FF0000000000000001D500009B64866E0F0001FF0000190000006A01D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700012864867C1F0001FF000053000000CD01D40003BC64868A2F0001FF00011D000002C101D300013C6486983F0001FF000066000000DE01D30000006486A64F0000FF0000000000000001D20007C36486B45F0001FF0002590000059501D20007446486C26F0001FF0002320000053C01D2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570003076486D07F0001FF0000A40000022C00D10000736486DE8F0001FF0000230000005500D10002A16486EC9F0001FF00008D000001CD00D00005A46486FAAF0001FF000149000003F900D0000061648708BF0001FF0000060000004200CF0000FA648716CF0000FF000022000000AA00CF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000045648724DF0000FF0000080000002F00CE000052648732EF0000FF00000A0000003700CE000000648740FF0000FF0000000000000000CE00000064874F0F0000FF0000000000000000CE00000064875D1F0000FF0000000000000000CE00000064876B2F0000FF0000000000000000CD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000166487793F0000FF0000000000000E00CD0000006487874F0000FF0000000000000000CD0000006487955F0000FF0000000000000000CD0000126487A36F0000FF0000020000000D00CD0000006487B17F0000FF0000000000000000CE00065F6487BF8F0001FF00016A0000046100CD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700064C6487CD9F0001FF0001C30000048000CD0000636487DBAF0001FF00001E0000005000CD0006016487E9BF0001FF0001D40000045A00CD0001106487F7CF0001FF00003C000000BA00CC000411648805DF0001FF0000D6000002D300CB0000006488846F0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006488927F0000FF0000000000000007FF0000006488A08F0000FF0000000000000007FF0000006488AE9F0000FF0000000000000007FF00000D6488BCAF0000FF0000000000000807FF0000006488CABF0000FF0000000000000007FF0000146488D8CF0000FF0000000000000E07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006488E6DF0000FF0000000000000007FF0000006488F4EF0000FF0000000000000007FF000026648902FF0000FF0000040000001A07FF0001FB6489110F0001FF0000580000015B07FF00068564891F1F0001FF0001EA000004C007FF00001164892D2F0000FF0000020000000C07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700056864893B3F0001FF00015C000003C707FF0001CD6489494F0001FF0000750000015007FF00043E6489575F0001FF00015E0000030307FF000D1F6489656F0001FF0003AA0000091F07FF00026F6489737F0001FF000099000001C607FF0003336489818F0001FF0000900000024206FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700046264898F9F0001FF0001370000032B06FF0000F164899DAF0001FF000026000000A706FF0004C26489ABBF0001FF0001260000035A06FF0001C26489B9CF0001FF00006F0000014306FE0000AE6489C7DF0000FF00001E0000007806FD0000DC6489D5EF0000FF0000210000009506FC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006489E3FF0000FF0000000000000006FC0000006489F20F0000FF0000000000000006FC000000648A001F0000FF0000000000000006FC000011648A0E2F0000FF0000000000000B06FB000000648A1C3F0000FF0000000000000006FB000000648A2A4F0000FF0000000000000006FB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000648A385F0000FF0000000000000006FB00004B648A466F0000FF00000A0000003306FB000036648A547F0000FF0000070000002506FB000315648A628F0001FF0000910000021606FA00024D648A709F0001FF0000C5000001B606F80002A3648A7EAF0001FF0000CB000001E606F7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000310648A8CBF0001FF0000CF0000021F06F600024C648A9ACF0001FF0000BA000001A706F5000450648AA8DF0001FF00015B0000032805F4000D54648AB6EF0001FF0004520000096205F300017F648AC4FF0001FF0000670000011205F3000570648AD30F0001FF000172000003E105F2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000314648AE11F0001FF0000D90000023805F10000C6648AEF2F0001FF00001C0000008D05F1000455648AFD3F0001FF00011B0000031F05EF0002BE648B0B4F0001FF00009D000001EA05EE0000CD648B195F0000FF00001A0000007A05EC0000EF648B276F0000FF000027000000B405EC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000648B357F0000FF0000000000000005EB00000F648B438F0000FF0000000000000A05EB000000648B519F0000FF0000000000000005EB000018648B5FAF0000FF0000050000001005EB000000648B6DBF0000FF0000000000000005EB000000648B7BCF0000FF0000000000000005EB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000016648B89DF0000FF0000020000000F05EB000016648B97EF0000FF0000020000000F05EB0000D1648BA5FF0000FF00001A0000008E05EA0003AE648BB40F0001FF0000890000028405E90006AE648BC21F0001FF0002550000050805E80000AB648BD02F0001FF00002F0000007704E7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000031648BDE3F0000FF0000070000002204E7000000648BEC4F0000FF0000000000000004E50003EA648BFA5F0001FF00013C000002CF04E40009F3648C086F0001FF000322000006F904E300000D648C167F0000FF0000000000000904E2000390648C248F0001FF0000D60000028D04E2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000038648C329F0000FF0000050000002604E2000737648C40AF0001FF0002130000053504E2000374648C4EBF0001FF0001090000029D04E20005B1648C5CCF0001FF0001DF0000041A04E1000000648C6ADF0000FF0000000000000004E1000013648C78EF0000FF0000010000000C04E0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570006B4648C86FF0000FF0001B2000004AE04E0000075648C950F0000FF00000F0000004F04E000009C648CA31F0000FF0000130000006A04E0000000648CB12F0000FF0000000000000004E0000000648CBF3F0000FF0000000000000004E0000000648CCD4F0000FF0000000000000004E0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000648CDB5F0000FF0000000000000004E0000000648CE96F0000FF0000000000000004E00000F1648CF77F0000FF00002D000000A504E00001F7648D058F0001FF0000640000016004E0000083648D139F0001FF00000D0000005904DF000134648D21AF0001FF000053000000DE04DF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570004B2648D2FBF0001FF0000FA0000034503DF0000E2648D3DCF0001FF0000320000009E03DF000436648D4BDF0001FF0000FE000002FA03DF0003CF648D59EF0001FF000117000002BA03DF0000A4648D67FF0001FF0000120000006F03DF000732648D760F0001FF00019C0000050C03DF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000023648D841F0000FF0000040000001703DF0004EE648D922F0001FF0000F00000036103DF000839648DA03F0001FF0001EA000005CD03DE000921648DAE4F0001FF0002180000067603DE0004FF648DBC5F0000FF00011D0000037A03DE000536648DCA6F0000FF0001B0000003D103DE";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000648DD87F0000FF0000000000000003DD000506648DE68F0000FF0000F00000037403DD000010648DF49F0000FF0000020000000B03DD000000648E02AF0000FF0000000000000003DD000000648E10BF0000FF0000000000000003DD000000648E1ECF0000FF0000000000000003DD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000648E2CDF0000FF0000000000000003DD000000648E3AEF0000FF0000000000000003DD000000648E48FF0000FF0000000000000003DD00014A648E570F0001FF000030000000E303DC0000E3648E651F0001FF000032000000A203DD000077648E732F0001FF0000140000005103DC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002AF648E813F0001FF0000F00000021703DC000051648E8F4F0001FF0000180000003A03DC00024F648E9D5F0001FF00008A000001A303DC000202648EAB6F0001FF0000640000016403DC000116648EB97F0001FF000033000000C503DC000111648EC78F0001FF000037000000BC03DC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000165648ED59F0001FF00004A000000FB03DC000257648EE3AF0001FF000095000001AE03DC000355648EF1BF0001FF0000E90000026303DB0001EB648EFFCF0001FF00006E0000015C03DB000019648F0DDF0000FF0000050000001103DB0001DC648F1BEF0000FF0000500000014703DB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700007B648F29FF0000FF00001E0000005803DB000000648F380F0000FF0000000000000003DB000000648F461F0000FF0000000000000003DB000000648F542F0000FF0000000000000003DB000000648F623F0000FF0000000000000003DB000000648F704F0000FF0000000000000003DB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000648F7E5F0000FF0000000000000003DB000043648F8C6F0000FF0000070000002D03DB000000648F9A7F0000FF0000000000000003DB00031E648FA88F0001FF0000B50000023503DB000228648FB69F0001FF0000760000018003DB000993648FC4AF0001FF00037B0000074403DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000557648FD2BF0001FF0001F50000040003DA000317648FE0CF0001FF0000D60000024003DA00014B648FEEDF0001FF000053000000E903DA000187648FFCEF0001FF00005E0000010B03DA0007DD64900AFF0001FF00024A0000057703DA0000446490190F0001FF0000110000003003DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570003986490271F0001FF0000D00000028602DA0000006490352F0000FF0000000000000002DA0008B56490433F0001FF0003180000067002DA0000A66490514F0000FF00001B0000007502DA00027964905F5F0000FF000080000001C002DA0001B464906D6F0000FF0000680000013D02DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700003364907B7F0000FF0000030000002302D900000D6490898F0000FF0000000000000802D90000006490979F0000FF0000000000000002D90000006490A5AF0000FF0000000000000002D90000166490B3BF0000FF0000000000000F02D90000006490C1CF0000FF0000000000000002D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006490CFDF0000FF0000000000000002D90000196490DDEF0000FF0000020000001102D90003076490EBFF0000FF0000AF0000023402D900090C6490FA0F0001FF0002970000066E02D90000626491081F0001FF00000F0000004302D90000006491162F0000FF0000000000000002D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000436491243F0001FF00000E0000002D02D80000846491324F0001FF00001E0000005D02D80000596491405F0001FF00000D0000003D02D7000C9464914E6F0001FF0004310000093F02D70003E764915C7F0001FF000125000002CA02D7000CCF64916A8F0001FF000396000008F002D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570008EF6491789F0001FF0002710000063B02D6000409649186AF0001FF000145000002E602D60004D9649194BF0001FF0001530000036501D50000BD6491A2CF0000FF0000120000008001D500019D6491B0DF0000FF0000440000011901D50001046491BEEF0000FF000036000000B401D5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006491CCFF0000FF0000000000000001D40000006491DB0F0000FF0000000000000001D40000006491E91F0000FF0000000000000001D40000006491F72F0000FF0000000000000001D40000006492053F0000FF0000000000000001D40000006492134F0000FF0000000000000001D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006492215F0000FF0000000000000001D400003364922F6F0000FF0000010000002201D400000064923D7F0000FF0000000000000001D400000064924B8F0000FF0000000000000001D40000006492599F0000FF0000000000000001D4000000649267AF0000FF0000000000000001D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000649275BF0000FF0000000000000001D4000000649283CF0000FF0000000000000001D4000000649291DF0000FF0000000000000001D400000064929FEF0000FF0000000000000001D40000006492ADFF0000FF0000000000000001D40000006492BC0F0000FF0000000000000001D4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006492CA1F0000FF0000000000000001D40000006492D82F0000FF0000000000000001D40002D56492E63F0001FF0000A00000020C01D30002316492F44F0001FF00005F0000018401D30001376493025F0000FF000032000000D501D20000426493106F0000FF0000080000002D01D2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700002264931E7F0000FF0000050000001701D200000064932C8F0000FF0000000000000001D200000064933A9F0000FF0000000000000001D2000000649348AF0000FF0000000000000001D2000000649356BF0000FF0000000000000001D2000000649364CF0000FF0000000000000001D2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700001D649372DF0000FF0000000000001301D2000086649380EF0000FF0000190000005B01D200000064938EFF0000FF0000000000000001D200069764939D0F0001FF000176000004B901D20002DA6493AB1F0001FF0000D70000020A00D10000636493B92F0001FF00000B0000004300D1";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570009E36493C73F0001FF0002FB0000072700D00000FB6493D54F0001FF000040000000B900CF0002E46493E35F0001FF0000D70000021200CF000A416493F16F0001FF0002EE0000074800CF0002106493FF7F0001FF00008D0000017800CE00000064940D8F0000FF0000000000000000CD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700025464941B9F0001FF0000750000019F00CD000350649429AF0001FF0000D20000024A00CD00040E649437BF0001FF0000D7000002DD00CD000508649445CF0001FF00013E0000038B00CD0001F3649453DF0000FF0000500000015900CD000135649461EF0000FF000058000000E900CC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700002464946FFF0000FF00000A0000001900CC00001264947E0F0000FF0000000000000C00CC00000064948C1F0000FF0000000000000000CC00000064949A2F0000FF0000000000000000CC00000F6494A83F0000FF0000020000000A00CC0000006494B64F0000FF0000000000000000CC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006494C45F0000FF0000000000000000CB00001A6494D26F0000FF0000020000001200CB000000649526CF0000FF0000000000000000DE000000649534DF0000FF0000000000000007FF000000649542EF0000FF0000000000000007FF000000649550FF0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064955F0F0000FF0000000000000007FF00025E64956D1F0001FF0000A1000001A307FF00070364957B2F0001FF0001F1000004DD07FF0009066495893F0001FF0002420000063507FF0001A76495974F0001FF0000540000011707FF0001666495A55F0000FF0000550000010E07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000906495B36F0000FF0000230000006407FF0000006495C17F0000FF0000000000000007FF0000006495CF8F0000FF0000000000000007FF0000006495DD9F0000FF0000000000000007FF0000006495EBAF0000FF0000000000000007FF00004C6495F9BF0000FF0000070000003307FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000649607CF0000FF0000000000000007FF000000649615DF0000FF0000000000000007FF000020649623EF0000FF00000A0000001607FF00001D649631FF0000FF0000050000001407FF0000426496400F0001FF0000080000002D07FF00031164964E1F0001FF00008A0000021E07FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064965C2F0000FF0000000000000007FF00044764966A3F0001FF0000F70000030907FF0003B26496784F0001FF0000CC0000028C06FF0008196496865F0001FF00019B000005AE06FF0001A16496946F0001FF00004E0000012706FF0000726496A27F0001FF00000C0000004D06FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000806496B08F0001FF00001A0000005E06FE0001C16496BE9F0001FF0000410000013306FD0009F46496CCAF0001FF000255000006E606FD0009656496DABF0001FF0002240000067006FC0008726496E8CF0001FF0001FB000005E506FB000A056496F6DF0000FF000241000006EB06FA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000158649704EF0000FF00002E000000ED06F9000085649712FF0000FF00000F0000005A06F90000006497210F0000FF0000000000000006F900000064972F1F0000FF0000000000000006F800000064973D2F0000FF0000000000000006F800000064974B3F0000FF0000000000000006F8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006497594F0000FF0000000000000006F80000006497675F0000FF0000000000000006F80000006497756F0000FF0000000000000006F80000006497837F0000FF0000000000000006F700001A6497918F0000FF0000000000001006F70000CB64979F9F0001FF00001C0000008B06F6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570004F26497ADAF0001FF00011B0000037D06F600011E6497BBBF0001FF000050000000D406F60001166497C9CF0001FF000031000000B606F60003A36497D7DF0001FF0000A30000029005F40003526497E5EF0001FF0000A20000024A05F30000006497F3FF0000FF0000000000000005F2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700042B6498020F0001FF000104000002E305F20000006498101F0000FF0000000000000005F200013C64981E2F0001FF000023000000D705F200066164982C3F0001FF00013A0000047705F20003F664983A4F0001FF0000C6000002C805F20004B96498485F0000FF0001280000036505F2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002256498566F0000FF00005F0000017A05F10000816498647F0000FF0000210000005C05F10000276498728F0000FF0000000000001E05F10000006498809F0000FF0000000000000005F000000064988EAF0000FF0000000000000005F000000064989CBF0000FF0000000000000005F0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006498AACF0000FF0000000000000005F00000006498B8DF0000FF0000000000000005EF0000226498C6EF0000FF0000030000001705EF00009D6498D4FF0000FF0000200000006D05EF00060D6498E30F0001FF0001110000044005EE0004BF6498F11F0001FF0001A80000039005EE";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002796498FF2F0001FF00008D000001B905ED00033764990D3F0001FF0000AF0000023205ED00052564991B4F0001FF00011A0000039805EC0008776499295F0001FF0002660000061705EC0005C46499376F0001FF0001BF0000044F05EC00013D6499457F0001FF000042000000DD05EC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001206499538F0001FF000022000000C305EC0002C96499619F0001FF00008C000001E405EC00014964996FAF0001FF000037000000F005EB00017864997DBF0001FF0000370000010305EB00024E64998BCF0001FF00006F0000019405EB0005D7649999DF0000FF0001780000043C05EA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002576499A7EF0000FF00005E0000019805EA00004F6499B5FF0000FF0000150000003505EA0000006499C40F0000FF0000000000000005EA0000006499D21F0000FF0000000000000005EA0000006499E02F0000FF0000000000000005EA0000006499EE3F0000FF0000000000000005EA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000006499FC4F0000FF0000000000000005EA000000649A0A5F0000FF0000000000000005E900006D649A186F0000FF00000A0000004A05E9000012649A267F0000FF0000000000000005E9000380649A348F0001FF0000CD0000027C05E900052E649A429F0001FF00018B000003CB05E9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700044B649A50AF0001FF0001330000031405E9000489649A5EBF0001FF0001430000034705E80006B0649A6CCF0001FF000205000004D005E800015C649A7ADF0001FF000065000000EE05E80005AA649A88EF0001FF0001E70000044A04E80000A7649A96FF0001FF0000190000007204E7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700035A649AA50F0001FF0000CE0000025304E70000DA649AB31F0001FF0000370000009604E7000178649AC12F0001FF0000540000010B04E60000B7649B22BF0001FF00001A0000007D04E6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-06-27 20:08:45 Download
        else if (idDebug == 21) {
            Log.e(TAG, "2023-06-27 20:08:45 Download");
            contentData = "570000E2649B239F0001FF0000210000009A04E600004F649B26050001FF00000A0000003604E6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2023-07-06 18:26:19 Download
        else if (idDebug == 22) {
            Log.e(TAG, "2023-07-06 18:26:19 Download");
            contentData = "57000258649B31AF0001FF0000660000019D04E6000219649B3FBF0000FF0000920000017704E60000DB649B4DCF0000FF000030000000A404E60000C7649B5BDF0000FF0000370000008C04E6000000649B69EF0000FF0000000000000004E5000000649B77FF0000FF0000000000000004E5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000649B860F0000FF0000000000000004E5000000649B941F0000FF0000000000000004E5000000649BA22F0000FF0000000000000004E5000000649BB03F0000FF0000000000000004E4000000649BBE4F0000FF0000000000000004E4000057649BCC5F0000FF00000A0000003B04E4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570003C5649BDA6F0001FF000125000002D104E4000363649BE87F0001FF0000CD0000026004E4000550649BF68F0001FF000190000003D304E3000467649C049F0001FF00011D0000031004E3000000649C12AF0000FF0000000000000004E30003D4649C20BF0001FF0000F8000002A304E3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000597649C2ECF0001FF00015E000003DC04E3000087649C3CDF0001FF00000D0000005C04E20000DF649C4AEF0001FF000041000000AB04E200017E649C58FF0001FF00002F0000010404E20006FE649C670F0001FF00018A000004CE04E200035E649C751F0001FF0000C70000027804E2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000567649C832F0001FF000171000003C304E2000242649C913F0000FF0000660000019D04E2000120649C9F4F0000FF00002D000000C404E1000019649CAD5F0000FF0000050000001104E1000000649CBB6F0000FF0000000000000004E1000000649CC97F0000FF0000000000000004E1";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000649CD78F0000FF0000000000000004E1000000649CE59F0000FF0000000000000004E1000092649CF3AF0000FF00001E0000006604E1000000649D01BF0000FF0000000000000004E1000034649D0FCF0000FF0000070000002404E1000042649D1DDF0000FF0000050000002C04E0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000531649D2BEF0001FF0000F40000039C04E0000275649D39FF0001FF000091000001B904E0000775649D480F0001FF0002820000056C04E000067E649D561F0001FF00020A000004C604E0000380649D642F0001FF0001160000027E04E0000679649D723F0001FF000200000004BB04E0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570004B6649D804F0001FF0001540000036E04DF000182649D8E5F0001FF0000440000010803DF000114649D9C6F0001FF00002A000000BC03DF0001D3649DAA7F0001FF0000930000015C03DF0004DE649DB88F0001FF00013D0000035E03DF0005E1649DC69F0001FF0001730000041203DF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000291649DD4AF0001FF000090000001D803DF000255649DE2BF0000FF000093000001B403DF000033649DF0CF0000FF00000C0000002303DF000026649DFEDF0000FF0000030000001A03DE000000649E0CEF0000FF0000000000000003DE000000649E1AFF0000FF0000000000000003DE";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000649E290F0000FF0000000000000003DE000000649E371F0000FF0000000000000003DE000000649E452F0000FF0000000000000003DE000011649E533F0000FF0000000000000B03DE00000F649E614F0000FF0000020000000A03DE000000649E6F5F0000FF0000000000000003DE";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000106649E7D6F0001FF000037000000B703DE0003D8649E8B7F0001FF000142000002D703DE00018C649E998F0001FF00004F0000011603DE000106649EA79F0001FF000049000000AC03DE00017C649EB5AF0001FF00004E0000010E03DD0003E9649EC3BF0001FF00014A000002DE03DD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000435649ED1CF0001FF0001420000030C03DD000104649EDFDF0001FF000029000000B203DD000184649EEDEF0001FF00004B0000011003DD00004C649EFBFF0001FF00000C0000003403DD00018E649F0A0F0001FF0000550000011803DD00061C649F181F0001FF0001AE0000044103DD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570005F3649F262F0001FF00016C0000043A03DD0000A0649F343F0000FF00001B0000006C03DC00006D649F424F0000FF0000080000004A03DC00007C649F505F0000FF0000160000005703DC000000649F5E6F0000FF0000000000000003DC000000649F6C7F0000FF0000000000000003DC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "57000000649F7A8F0000FF0000000000000003DC000000649F889F0000FF0000000000000003DC000000649F96AF0000FF0000000000000003DC000000649FA4BF0000FF0000000000000003DC000000649FB2CF0000FF0000000000000003DC000017649FC0DF0000FF0000000000000F03DC";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700001D649FCEEF0000FF0000000000001403DC00018F649FDCFF0001FF00005A0000011A03DC000000649FEB0F0000FF0000000000000003DB00051E649FF91F0001FF00014E000003A903DB00044264A0072F0001FF0000E3000002F303DB00047264A0153F0001FF00010C0000032A03DB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700020864A0234F0001FF0000640000016803DB00017564A0315F0001FF00003F0000010003DB00011264A03F6F0001FF000037000000C203DB00008F64A04D7F0001FF0000120000006103DB00010764A05B8F0001FF000023000000B303DB00016A64A0699F0001FF000039000000F803DB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570006B064A077AF0001FF0001A1000004BE03DB0001B364A085BF0000FF0000710000014903DA00012F64A093CF0000FF00002B000000CE03DA0000C664A0A1DF0000FF0000250000008803DA00000064A0AFEF0000FF0000000000000003DA00000064A0BDFF0000FF0000000000000003DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064A0CC0F0000FF0000000000000003DA00005564A0DA1F0000FF0000030000003903DA00000064A0E82F0000FF0000000000000003DA00000064A0F63F0000FF0000000000000003DA00000064A1044F0000FF0000000000000003DA00000064A1125F0000FF0000000000000003DA";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002BF64A1206F0001FF00006A000001DE03DA00035564A12E7F0001FF0000BD0000026D02DA00030664A13C8F0001FF00009E0000021702DA0003F664A14A9F0001FF00010B000002DB02DA00014B64A158AF0001FF00003E000000E502DA0001EF64A166BF0001FF0000520000015A02D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000FD64A174CF0001FF00004A000000C102D900000064A182DF0000FF0000000000000002D900022E64A190EF0001FF0000800000019702D90003D864A19EFF0001FF0000F5000002CD02D900005664A1AD0F0001FF00000D0000003A02D900008864A1BB1F0001FF0000190000005E02D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570001C564A1C92F0001FF0000590000013902D900026C64A1D73F0000FF00008C000001BD02D90000B764A1E54F0000FF0000170000007C02D900001A64A1F35F0000FF0000050000001202D900001964A2016F0000FF0000000000001102D900001664A20F7F0000FF0000010000000E02D9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000F64A21D8F0000FF0000020000000B02D800005764A22B9F0000FF0000100000003D02D800000064A239AF0000FF0000000000000002D800000064A247BF0000FF0000000000000002D800002464A255CF0000FF0000020000001902D800000064A263DF0000FF0000000000000002D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700006F64A271EF0001FF00000E0000004B02D800059A64A27FFF0001FF0001740000041902D800012364A28E0F0001FF000037000000D502D800040064A29C1F0001FF0001690000031B02D800048164A2AA2F0001FF0001E0000003B702D800086964A2B83F0001FF000207000005F802D8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700031A64A2C64F0001FF0000960000022202D80004FC64A2D45F0001FF000198000003A802D800029664A2E26F0001FF0000C4000001E602D800051264A2F07F0001FF00014B0000039D02D80002E664A2FE8F0001FF0000A30000020702D700056B64A30C9F0001FF000112000003C502D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700022A64A31AAF0001FF0000500000017902D700006164A328BF0000FF00000D0000004202D700024D64A336CF0000FF00007D000001AD02D700000064A344DF0000FF0000000000000002D700000064A352EF0000FF0000000000000002D700000064A360FF0000FF0000000000000002D7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064A36F0F0000FF0000000000000002D700000064A37D1F0000FF0000000000000002D700000064A38B2F0000FF0000000000000002D700000064A3993F0000FF0000000000000002D600001264A3A74F0000FF0000000000000C02D600004264A3B55F0000FF0000090000002D02D6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000B064A3C36F0000FF00001A0000007802D600009964A3D17F0001FF00001D0000006801D600024864A3DF8F0001FF0000540000019501D600031264A3ED9F0001FF0000BC0000023401D600000064A3FBAF0000FF0000000000000001D600039564A3B55F0001FF0000BC0000028B01D5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570003FD64A3C36F0001FF0000CB000002CF01D500003B64A3D17F0000FF0000090000002801D50001D764A3DF8F0001FF0000600000014B01D400061B64A3ED9F0001FF0001510000045601D40007A964A3FBAF0001FF0001A30000055E01D400023B64A409BF0001FF0000900000017F01D3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700056A64A417CF0001FF00013B000003E501D30006D164A425DF0000FF00016C000004C101D300043964A433EF0000FF0000CB000002F201D10001CD64A441FF0000FF0000590000014D01D200000064A4500F0000FF0000000000000001D200000064A45E1F0000FF0000000000000001D2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064A46C2F0000FF0000000000000001D200000064A47A3F0000FF0000000000000001D200001164A4884F0000FF0000000000000B01D200000064A4965F0000FF0000000000000001D200000064A4A46F0000FF0000000000000001D20000B464A4B27F0000FF00002A0000007F01D2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002EB64A4C08F0001FF00009D0000021901D100057364A4CE9F0001FF0001A4000003F300D10002A564A4DCAF0001FF0000BC000001E900D000058064A4EABF0001FF0001BB000003FC00D000075964A4F8CF0001FF0002DD000005C400CF000D9664A506DF0001FF00059A00000AF900CF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700042764A514EF0001FF00011C0000030B00CE00009F64A522FF0001FF00000D0000006C00CD00002C64A5856F0000FF0000070000001E00CD00001664A5937F0000FF0000010000000F06FF00000064A5A18F0000FF0000000000000007FF00000064A5AF9F0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700002D64A5BDAF0000FF0000110000002207FF00026264A5CBBF0000FF000071000001A907FF0001BB64A5D9CF0000FF0000630000013A07FF00001264A5E7DF0000FF0000010000000C07FF00000064A5F5EF0000FF0000000000000007FF00000064A603FF0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "5700000064A6120F0000FF0000000000000007FF00000064A6201F0000FF0000000000000007FF00000064A62E2F0000FF0000000000000007FF00000064A63C3F0000FF0000000000000007FF0000EE64A64A4F0000FF000030000000A507FF00000064A6585F0000FF0000000000000007FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570000AF64A6666F0001FF0000280000008007FF00012764A6747F0001FF000062000000DB07FF0000C864A6828F0001FF00002A0000009507FF00055F64A6909F0001FF000127000003B807FF0000A664A69EAF0001FF0000280000007507FF00013764A6ACBF0001FF00002D000000D407FF";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData = "570002B864A6BACF0001FF000093000001DB06FF00059464A6C8DF0001FF00014C000003EC06FF00000064A6D6EF0000FF0000000000000006FF00016A64A6E4FF0001FF0000600000010706FE00013464A6EB8C0001FF00003E000000D106FD";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
        //2024-10-28 Download
        else if (idDebug == 23) {
            Log.e(TAG, "2024-10-28 Download");
            contentData= "570000BC671B41EF0000FF0000140000008006FC0000BB671B4FFF0000FF0000230000008506FC0000A3671B5E0F0000FF0000180000006F06FC000037671B6C1F0000FF00000B0000002506FB000143671B7A2F0000FF000036000000DE06FB0001DA671B883F0000FF0000580000014306FB";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData= "570001ED671B964F0000FF00005C0000015B06FB000E4E671BA45F0000FF00039D00000A0306FB000D10671BB26F0000FF0002F50000091006FB00010B671BC07F0000FF000044000000BF06FA000095671BCE8F0000FF0000170000006606FA000000671BDC9F0000FF0000000000000006F9";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData= "570001B2671BEAAF0000FF00002F0000012706F9000023671BF8BF0000FF0000030000001806F9000064671C06CF0000FF0000130000004506F9000000671C14DF0000FF0000000000000006F9000000671C22EF0000FF0000000000000006F9000000671C30FF0000FF0000000000000006F8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData= "57000000671C3F0F0000FF0000000000000006F8000000671C4D1F0000FF0000000000000006F8000015671C5B2F0000FF0000000000000E06F8000000671C693F0000FF0000000000000006F8000000671C774F0000FF0000000000000006F8000011671C855F0000FF0000020000000B06F8";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData= "57000000671C936F0000FF0000000000000006F7000151671CA17F0000FF00004B000000E906F70002C4671CAF8F0000FF0000AA000001E906F7000349671CBD9F0000FF0000A60000025006F7000076671CCBAF0000FF0000130000005006F700030C671CD9BF0000FF0000960000022206F7";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData= "57000000671CE7CF0000FF0000000000000006F6000019671CF5DF0000FF0000020000001106F6000000671D03EF0000FF0000000000000006F6000000671D11FF0000FF0000000000000006F6000000671D200F0000FF0000000000000006F6000397671D2E1F0000FF0000A80000027A06F6";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData= "57000036671D3C2F0000FF0000050000002506F6000062671D4A3F0000FF0000120000004206F6000078671D584F0000FF0000180000005206F500001C671D665F0000FF0000050000001306F500000E671D746F0000FF0000000000000906F5000028671D827F0000FF0000040000001B06F5";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData= "57000000671D908F0000FF0000000000000006F5000000671D9E9F0000FF0000000000000006F5000000671DACAF0000FF0000000000000006F5000000671DBABF0000FF0000000000000006F5000000671DC8CF0000FF0000000000000005F4000000671DD6DF0000FF0000000000000005F4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData= "57000011671DE4EF0000FF0000010000000C05F400012D671DF2FF0000FF00003B000000D005F400006D671E010F0000FF00000E0000004A05F4000000671E0F1F0000FF0000000000000005F4000099671E1D2F0000FF00000F0000006805F40000CE671E2B3F0000FF00001E0000008C05F4";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData= "57000020671E394F0000FF0000050000001605F400003D671E475F0000FF0000070000002905F4000000671E556F0000FF0000000000000005F3000012671E637F0000FF0000020000000D05F3000054671E718F0000FF00000A0000003905F3000012671E7F9F0000FF0000020000000C05F3";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData= "570000E0671E8DAF0000FF0000300000009C05F3000000671E9BBF0000FF0000000000000005F3000000671EA9CF0000FF0000000000000005F3000037671EB7DF0000FF00000C0000002505F3000000671EC5EF0000FF0000000000000005F3000013671ED3FF0000FF0000000000000C05F2";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData= "57000000671EE20F0000FF0000000000000005F2000000671EF01F0000FF0000000000000005F2000034671EFE2F0000FF00000C0000002405F2000010671F0C3F0000FF0000020000000B05F2000000671F1A4F0000FF0000000000000005F1000000671F285F0000FF0000000000000005F1";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
            contentData= "5700018A671F366F0000FF0000520000011405F00004D5671F3F8F0000FF0001920000039205F0";
            stepSummaryHour.add(new ATStepSummary(toArrayByte(contentData)));
        }
    }

    private void setDebugDataSleepRecord() {
        dataSleepReportData.clear();
        String contentData = "";

        //2023-03-10 12:58:57 Download
        if (idDebug == -1) {
            Log.e(TAG, "2023-03-10 12:58:57 Download");
            contentData = "8264091B3564097355000000000000000000000049000000E10000004E0202000011000464091B3564091C9D020000000664091C9D6409214D04000000146409214D640923A5020000000A640923A564092B610300000021";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264091B3564097355000000000000000000000049000000E10000004E0202000011040464092B6164092E6D040000000D64092E6D640944B1020000005F640944B164094B41030000001C64094B4164094E4D040000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264091B3564097355000000000000000000000049000000E10000004E0202000011080464094E4D64095519020000001D64095519640958D90400000010640958D96409598D02000000036409598D64095BA90300000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264091B3564097355000000000000000000000049000000E10000004E02020000110C0464095BA96409681502000000356409681564096AA9040000000B64096AA964096CC5020000000964096CC564096EA50300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264091B3564097355000000000000000000000049000000E10000004E0202000011100164096EA5640973550200000014";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640A5DA0640AC40C0000001D000000010000005D000000A40000009702020100170004640A5DA0640A5F080200000006640A5F08640A71C80400000050640A71C8640A73300200000006640A7330640A77A40300000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640A5DA0640AC40C0000001D000000010000005D000000A40000009702020100170404640A77A4640A78580200000003640A7858640A7A740400000009640A7A74640A7F600200000015640A7F60640A81400300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640A5DA0640AC40C0000001D000000010000005D000000A40000009702020100170804640A8140640A88FC0200000021640A88FC640A8B180300000009640A8B18640A8DE8020000000C640A8DE8640A91A80300000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640A5DA0640AC40C0000001D000000010000005D000000A40000009702020100170C04640A91A8640A97840200000019640A9784640A98740400000004640A9874640A9ACC020000000A640A9ACC640AA7B00300000037";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640A5DA0640AC40C0000001D000000010000005D000000A40000009702020100171004640AA7B0640AAFA80200000022640AAFA8640AB32C030000000F640AB32C640AB5C0020000000B640AB5C0640AB8CC030000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640A5DA0640AC40C0000001D000000010000005D000000A40000009702020100171403640AB8CC640ABF98010000001D640ABF98640AC3580300000010640AC358640AC40C0200000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-03-12 18:44:33 Download
        else if (idDebug == -2) {
            Log.e(TAG, "2023-03-12 18:44:33 Download");
            contentData = "82640BB96B640C2A63000000190000000100000058000000E30000008E020300001B0004640BB96B640BBAD30200000006640BBAD3640BBF0B0400000012640BBF0B640BC163020000000A640BC163640BC73F0300000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640BB96B640C2A63000000190000000100000058000000E30000008E020300001B0404640BC73F640BCA0F020000000C640BCA0F640BCBEF0300000008640BCBEF640BCD1B0200000005640BCD1B640BD2F70400000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640BB96B640C2A63000000190000000100000058000000E30000008E020300001B0804640BD2F7640BD9FF020000001E640BD9FF640BDD83040000000F640BDD83640BE1F70200000013640BE1F7640BE2E70300000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640BB96B640C2A63000000190000000100000058000000E30000008E020300001B0C04640BE2E7640BE4130200000005640BE413640BE6E3040000000C640BE6E3640BE8870200000007640BE887640BEE630300000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640BB96B640C2A63000000190000000100000058000000E30000008E020300001B1004640BEE63640BF9A30200000030640BF9A3640C03030300000028640C0303640C04A70200000007640C04A7640C06FF040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640BB96B640C2A63000000190000000100000058000000E30000008E020300001B1404640C06FF640C0957020000000A640C0957640C0B370400000008640C0B37640C17A30200000035640C17A3640C1C170300000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640BB96B640C2A63000000190000000100000058000000E30000008E020300001B1803640C1C17640C1F9B020000000F640C1F9B640C25770100000019640C2577640C2A630300000015";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640C8745640C9861000000000000000000000000000000060000004302030100020002640C8745640C88AD0200000006640C88AD640C98610300000043";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640D22ED640D73C900000000000000000000003C000000B400000069020302000B0004640D22ED640D24550200000006640D2455640D31390400000037640D3139640D37150200000019640D3715640D3DA5030000001C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640D22ED640D73C900000000000000000000003C000000B400000069020302000B0404640D3DA5640D4CE10200000041640D4CE1640D51910300000014640D5191640D567D0200000015640D567D640D57A90400000005";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640D22ED640D73C900000000000000000000003C000000B400000069020302000B0803640D57A9640D61BD020000002B640D61BD640D6F190300000039640D6F19640D73C90200000014";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-03-13 08:32:28 Download
        else if (idDebug == -3) {
            Log.e(TAG, "2023-03-13 08:32:28 Download");
            contentData = "82640E4F2C640EB4E40000000B000000010000004C0000009A000000C1020100001F0004640E4F2C640E50940200000006640E5094640E5364040000000C640E5364640E57600200000011640E5760640E59040300000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640E4F2C640EB4E40000000B000000010000004C0000009A000000C1020100001F0404640E5904640E5B200200000009640E5B20640E5DF0030000000C640E5DF0640E60C0040000000C640E60C0640E73F80300000052";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640E4F2C640EB4E40000000B000000010000004C0000009A000000C1020100001F0804640E73F8640E75600200000006640E7560640E7B000400000018640E7B00640E7BB40200000003640E7BB4640E7DD00300000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640E4F2C640EB4E40000000B000000010000004C0000009A000000C1020100001F0C04640E7DD0640E83E8020000001A640E83E8640E8640030000000A640E8640640E87A80200000006640E87A8640E89C40400000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640E4F2C640EB4E40000000B000000010000004C0000009A000000C1020100001F1004640E89C4640E8C58010000000B640E8C58640E8CD00200000002640E8CD0640E92700300000018640E9270640E966C0200000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640E4F2C640EB4E40000000B000000010000004C0000009A000000C1020100001F1404640E966C640E98C4030000000A640E98C4640E9B1C020000000A640E9B1C640E9E64020000000E640E9E64640EA1AC040000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640E4F2C640EB4E40000000B000000010000004C0000009A000000C1020100001F1804640EA1AC640EA2600200000003640EA260640EA47C0300000009640EA47C640EA6980200000009640EA698640EA7C40400000005";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640E4F2C640EB4E40000000B000000010000004C0000009A000000C1020100001F1C03640EA7C4640EAAD0020000000D640EAAD0640EB1D8030000001E640EB1D8640EB4E4020000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-03-14 16:58:11 Download
        else if (idDebug == -4) {
            Log.e(TAG, "2023-03-14 16:58:11 Download");
            contentData = "82640FA82364100DDB00000000000000000000008D0000007E000000A702010000190004640FA823640FA98B0200000006640FA98B640FB723040000003A640FB723640FB84F0200000005640FB84F640FBE67040000001A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640FA82364100DDB00000000000000000000008D0000007E000000A702010000190404640FBE67640FBEDF0200000002640FBEDF640FC74F0300000024640FC74F640FCA5B020000000D640FCA5B640FCF830300000016";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640FA82364100DDB00000000000000000000008D0000007E000000A702010000190804640FCF83640FD64F040000001D640FD64F640FD9D3030000000F640FD9D3640FDA870200000003640FDA87640FDD1B040000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640FA82364100DDB00000000000000000000008D0000007E000000A702010000190C04640FDD1B640FDDCF0200000003640FDDCF640FDE0B0300000001640FDE0B640FE1CB0200000010640FE1CB640FE2070400000001";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640FA82364100DDB00000000000000000000008D0000007E000000A702010000191004640FE207640FE8D3030000001D640FE8D3640FEBDF040000000D640FEBDF640FF17F0300000018640FF17F640FF44F020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640FA82364100DDB00000000000000000000008D0000007E000000A702010000191404640FF44F640FFBCF0300000020640FFBCF641005E3020000002B641005E3641007C30300000008641007C364100D270200000017";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82640FA82364100DDB00000000000000000000008D0000007E000000A70201000019180164100D2764100DDB0400000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-03-15 11:34:59 Download
        else if (idDebug == -5) {
            Log.e(TAG, "2023-03-15 11:34:59 Download");
            contentData = "826410F4D6641158AE0000000800000001000000430000010600000059020100001800046410F4D66410F63E02000000066410F63E64110016040000002A64110016641105F20200000019641105F264110BCE0300000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826410F4D6641158AE00000008000000010000004300000106000000590201000018040464110BCE64110E62020000000B64110E6264111876020000002B6411187664111D26020000001464111D26641120E60300000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826410F4D6641158AE000000080000000100000043000001060000005902010000180804641120E66411233E020000000A6411233E6411273A03000000116411273A64112A0A020000000C64112A0A64112BEA0100000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826410F4D6641158AE000000080000000100000043000001060000005902010000180C0464112BEA64112D8E020000000764112D8E641132B60300000016641132B664113AAE020000002264113AAE64113EE60200000012";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826410F4D6641158AE00000008000000010000004300000106000000590201000018100464113EE6641141020300000009641141026411440E020000000D6411440E64114BCA020000002164114BCA64114E9A040000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826410F4D6641158AE00000008000000010000004300000106000000590201000018140464114E9A6411534A02000000146411534A641153C20200000002641153C2641156CE040000000D641156CE641158AE0200000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-03-15 19:42:48 Download
        else if (idDebug == -6) {
            Log.e(TAG, "2023-03-15 19:42:48 Download");
        }
        //2023-03-15 19:49:26 Download
        else if (idDebug == -7) {
            Log.e(TAG, "2023-03-15 19:49:26 Download");
        }
        //2023-03-16 09:53:12 Download
        else if (idDebug == -8) {
            Log.e(TAG, "2023-03-16 09:53:12 Download");
            contentData = "8264124C3F6412ABA3000000000000000000000054000000ED00000056020100000F000464124C3F64124DA7020000000664124DA764125DD3040000004564125DD364126247020000001364126247641263EB0300000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264124C3F6412ABA3000000000000000000000054000000ED00000056020100000F0404641263EB64126E3B020000002C64126E3B6412701B04000000086412701B6412775F020000001F6412775F64127CFF0300000018";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264124C3F6412ABA3000000000000000000000054000000ED00000056020100000F080464127CFF64127EDF020000000864127EDF64128083040000000764128083641285E70200000017641285E76412896B030000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264124C3F6412ABA3000000000000000000000054000000ED00000056020100000F0C036412896B641293F7020000002D641293F764129D57030000002864129D576412ABA3020000003D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-03-17 09:41:22 Download
        else if (idDebug == -9) {
            Log.e(TAG, "2023-03-17 09:41:22 Download");
            contentData = "826413A452641421020000000000000000000000A0000000E000000094010100002400046413A4526413A5BA02000000066413A5BA6413AC4A040000001C6413AC4A6413AF92030000000E6413AF926413B56E0400000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A452641421020000000000000000000000A0000000E000000094010100002404046413B56E6413B802020000000B6413B8026413BBFE03000000116413BBFE6413BF0A020000000D6413BF0A6413C2CA0300000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A452641421020000000000000000000000A0000000E000000094010100002408046413C2CA6413C5D6020000000D6413C5D66413C7B603000000086413C7B66413CF3604000000206413CF366413D1160300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A452641421020000000000000000000000A0000000E00000009401010000240C046413D1166413D5C602000000146413D5C66413D76A03000000076413D76A6413DF6202000000226413DF626413E17E0400000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A452641421020000000000000000000000A0000000E000000094010100002410046413E17E6413E1F602000000026413E1F66413E26E04000000026413E26E6413E5F2020000000F6413E5F26413EF160300000027";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A452641421020000000000000000000000A0000000E000000094010100002414046413EF166413F61E020000001E6413F61E6413F9A2040000000F6413F9A26413FB4602000000076413FB466413FECA040000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A452641421020000000000000000000000A0000000E000000094010100002418046413FECA6414015E020000000B6414015E641403020300000007641403026414077602000000136414077664140ABE030000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A452641421020000000000000000000000A0000000E00000009401010000241C0464140ABE64140F6E040000001464140F6E6414102202000000036414102264141202030000000864141202641413E20200000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A452641421020000000000000000000000A0000000E00000009401010000242004641413E26414172A040000000E6414172A641419FA020000000C641419FA64141C52030000000A64141C52641421020200000014";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A4526414226A0000000000000000000000A0000000E600000094010101002400046413A4526413A5BA02000000066413A5BA6413AC4A040000001C6413AC4A6413AF92030000000E6413AF926413B56E0400000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A4526414226A0000000000000000000000A0000000E600000094010101002404046413B56E6413B802020000000B6413B8026413BBFE03000000116413BBFE6413BF0A020000000D6413BF0A6413C2CA0300000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A4526414226A0000000000000000000000A0000000E600000094010101002408046413C2CA6413C5D6020000000D6413C5D66413C7B603000000086413C7B66413CF3604000000206413CF366413D1160300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A4526414226A0000000000000000000000A0000000E60000009401010100240C046413D1166413D5C602000000146413D5C66413D76A03000000076413D76A6413DF6202000000226413DF626413E17E0400000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A4526414226A0000000000000000000000A0000000E600000094010101002410046413E17E6413E1F602000000026413E1F66413E26E04000000026413E26E6413E5F2020000000F6413E5F26413EF160300000027";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A4526414226A0000000000000000000000A0000000E600000094010101002414046413EF166413F61E020000001E6413F61E6413F9A2040000000F6413F9A26413FB4602000000076413FB466413FECA040000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A4526414226A0000000000000000000000A0000000E600000094010101002418046413FECA6414015E020000000B6414015E641403020300000007641403026414077602000000136414077664140ABE030000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A4526414226A0000000000000000000000A0000000E60000009401010100241C0464140ABE64140F6E040000001464140F6E6414102202000000036414102264141202030000000864141202641413E20200000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A4526414226A0000000000000000000000A0000000E60000009401010100242004641413E26414172A040000000E6414172A641419FA020000000C641419FA64141C52030000000A64141C526414226A020000001A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-03-19 20:18:56 Download
        else if (idDebug == -10) {
            Log.e(TAG, "2023-03-19 20:18:56 Download");
            contentData = "826413A4526414226A0000000000000000000000A0000000E600000094020300002400046413A4526413A5BA02000000066413A5BA6413AC4A040000001C6413AC4A6413AF92030000000E6413AF926413B56E0400000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A4526414226A0000000000000000000000A0000000E600000094020300002404046413B56E6413B802020000000B6413B8026413BBFE03000000116413BBFE6413BF0A020000000D6413BF0A6413C2CA0300000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A4526414226A0000000000000000000000A0000000E600000094020300002408046413C2CA6413C5D6020000000D6413C5D66413C7B603000000086413C7B66413CF3604000000206413CF366413D1160300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A4526414226A0000000000000000000000A0000000E60000009402030000240C046413D1166413D5C602000000146413D5C66413D76A03000000076413D76A6413DF6202000000226413DF626413E17E0400000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A4526414226A0000000000000000000000A0000000E600000094020300002410046413E17E6413E1F602000000026413E1F66413E26E04000000026413E26E6413E5F2020000000F6413E5F26413EF160300000027";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A4526414226A0000000000000000000000A0000000E600000094020300002414046413EF166413F61E020000001E6413F61E6413F9A2040000000F6413F9A26413FB4602000000076413FB466413FECA040000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A4526414226A0000000000000000000000A0000000E600000094020300002418046413FECA6414015E020000000B6414015E641403020300000007641403026414077602000000136414077664140ABE030000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A4526414226A0000000000000000000000A0000000E60000009402030000241C0464140ABE64140F6E040000001464140F6E6414102202000000036414102264141202030000000864141202641413E20200000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826413A4526414226A0000000000000000000000A0000000E60000009402030000242004641413E26414172A040000000E6414172A641419FA020000000C641419FA64141C52030000000A64141C526414226A020000001A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826414ED40641563600000000900000001000000500000013A00000065020301001100046414ED406414EEA802000000066414EEA86414F9E804000000306414F9E864150078030000001C64150078641508E80200000024";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826414ED40641563600000000900000001000000500000013A0000006502030100110404641508E864150B7C030000000B64150B7C641513B00200000023641513B064151608030000000A64151608641520D0020000002E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826414ED40641563600000000900000001000000500000013A0000006502030100110804641520D064152AE4030000002B64152AE464152C4C020000000664152C4C641530FC0400000014641530FC64153CB40200000032";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826414ED40641563600000000900000001000000500000013A0000006502030100110C0464153CB464153ED0030000000964153ED06415585C020000006D6415585C64155A78010000000964155A7864156090020000001A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826414ED40641563600000000900000001000000500000013A00000065020301001110016415609064156360040000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826416513F6416C4530000000B000000010000005F000001160000006B020302001A00046416513F641652A70200000006641652A764165B53040000002564165B53641664EF0200000029641664EF641668EB0400000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826416513F6416C4530000000B000000010000005F000001160000006B020302001A0404641668EB64166F03020000001A64166F0364167377030000001364167377641677EB0200000013641677EB641679CB0300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826416513F6416C4530000000B000000010000005F000001160000006B020302001A0804641679CB64167DC7020000001164167DC7641684CF030000001E641684CF64168C13020000001F64168C1364169357030000001F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826416513F6416C4530000000B000000010000005F000001160000006B020302001A0C04641693576416A3FB02000000476416A3FB6416A5DB03000000086416A5DB6416AA1302000000126416AA136416AD1F040000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826416513F6416C4530000000B000000010000005F000001160000006B020302001A10046416AD1F6416AEFF02000000086416AEFF6416B37304000000136416B3736416B42702000000036416B4276416B5530400000005";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826416513F6416C4530000000B000000010000005F000001160000006B020302001A14046416B5536416B58F02000000016416B58F6416B67F04000000046416B67F6416B913010000000B6416B9136416B98B0200000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826416513F6416C4530000000B000000010000005F000001160000006B020302001A18026416B98B6416BC1F030000000B6416BC1F6416C4530200000023";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-03-20 18:17:42 Download
        else if (idDebug == -11) {
            Log.e(TAG, "2023-03-20 18:17:42 Download");
            contentData = "82641798856417FBE500000013000000010000005C000000FE0000003B020100000D000464179885641799ED0200000006641799ED6417ABBD040000004C6417ABBD6417AE15020000000A6417AE156417B0E5030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641798856417FBE500000013000000010000005C000000FE0000003B020100000D04046417B0E56417B3B5020000000C6417B3B56417B59503000000086417B5956417C54902000000436417C5496417C9090400000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641798856417FBE500000013000000010000005C000000FE0000003B020100000D08046417C9096417DDA902000000586417DDA96417E6CD03000000276417E6CD6417F159020000002D6417F1596417F5CD0100000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641798856417FBE500000013000000010000005C000000FE0000003B020100000D0C016417F5CD6417FBE5020000001A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-03-21 09:52:37 Download
        else if (idDebug == -12) {
            Log.e(TAG, "2023-03-21 09:52:37 Download");
            contentData = "826418E32B64194EFB000000130000000100000043000000D6000000A0020100001900046418E32B6418E49302000000066418E4936418EC8B04000000226418EC8B6418F57302000000266418F5736418F87F040000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826418E32B64194EFB000000130000000100000043000000D6000000A0020100001904046418F87F6418F9E702000000066418F9E76418FA2303000000016418FA236418FE1F02000000116418FE1F641903BF0300000018";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826418E32B64194EFB000000130000000100000043000000D6000000A002010000190804641903BF6419068F020000000C6419068F64190A13030000000F64190A1364190CE3040000000C64190CE3641911570100000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826418E32B64194EFB000000130000000100000043000000D6000000A002010000190C04641911576419133702000000086419133764191517040000000864191517641915CB0200000003641915CB64191DFF0300000023";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826418E32B64194EFB000000130000000100000043000000D6000000A00201000019100464191DFF6419227302000000136419227364193047030000003B641930476419369B020000001B6419369B64193E570200000021";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826418E32B64194EFB000000130000000100000043000000D6000000A00201000019140464193E5764194127030000000C641941276419464F02000000166419464F641947B70200000006641947B764194AFF030000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826418E32B64194EFB000000130000000100000043000000D6000000A00201000019180164194AFF64194EFB0200000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-03-22 08:17:35 Download
        else if (idDebug == -13) {
            Log.e(TAG, "2023-03-22 08:17:35 Download");
            contentData = "82641A2A07641A91DB0000000A00000001000000420000013A0000003502010000150004641A2A07641A2B6F0200000006641A2B6F641A2FA70400000012641A2FA7641A310F0200000006641A310F641A35470300000012";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641A2A07641A91DB0000000A00000001000000420000013A0000003502010000150404641A3547641A3EE30200000029641A3EE3641A413B030000000A641A413B641A440B040000000C641A440B641A469F030000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641A2A07641A91DB0000000A00000001000000420000013A0000003502010000150804641A469F641A496F020000000C641A496F641A4CB7030000000E641A4CB7641A53FB020000001F641A53FB641A5653040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641A2A07641A91DB0000000A00000001000000420000013A0000003502010000150C04641A5653641A58AB010000000A641A58AB641A5A4F0400000007641A5A4F641A5E870200000012641A5E87641A60670400000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641A2A07641A91DB0000000A00000001000000420000013A0000003502010000151004641A6067641A67E70200000020641A67E7641A6A7B040000000B641A6A7B641A838F020000006B641A838F641A8FFB0200000035";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641A2A07641A91DB0000000A00000001000000420000013A0000003502010000151401641A8FFB641A91DB0200000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-03-24 07:57:21 Download
        else if (idDebug == -14) {
            Log.e(TAG, "2023-03-24 07:57:21 Download");
            contentData = "82641B815B641BEC3B000000230000000100000066000001260000001902020000140004641B815B641B82C30200000006641B82C3641B8D13040000002C641B8D13641B93A3020000001C641B93A3641B95830300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641B815B641BEC3B000000230000000100000066000001260000001902020000140404641B9583641B9853020000000C641B9853641B99F70300000007641B99F7641B9B5F0200000006641B9B5F641B9D3F0400000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641B815B641BEC3B000000230000000100000066000001260000001902020000140804641B9D3F641BA5370200000022641BA537641BA78F030000000A641BA78F641BAC7B0200000015641BAC7B641BB2CF040000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641B815B641BEC3B000000230000000100000066000001260000001902020000140C04641BB2CF641BBA8B0200000021641BBA8B641BC2BF0100000023641BC2BF641BC517040000000A641BC517641BC6F70200000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641B815B641BEC3B000000230000000100000066000001260000001902020000141004641BC6F7641BD0CF020000002A641BD0CF641BD75F020000001C641BD75F641BDA6B040000000D641BDA6B641BEC3B020000004C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641CD110641D30B00000001400000001000000590000007E000000AD02020100140004641CD110641CD2780200000006641CD278641CDAAC0400000023641CDAAC641CDB600200000003641CDB60641CDE6C030000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641CD110641D30B00000001400000001000000590000007E000000AD02020100140404641CDE6C641CE6280200000021641CE628641CE970040000000E641CE970641CF078030000001E641CF078641CF384040000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641CD110641D30B00000001400000001000000590000007E000000AD02020100140804641CF384641CF9600300000019641CF960641CFA140200000003641CFA14641CFF780400000017641CFF78641D0A7C030000002F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641CD110641D30B00000001400000001000000590000007E000000AD02020100140C04641D0A7C641D0D10020000000B641D0D10641D11C00100000014641D11C0641D17D8020000001A641D17D8641D1A30030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641CD110641D30B00000001400000001000000590000007E000000AD02020100141004641D1A30641D1BD40200000007641D1BD4641D27140300000030641D2714641D2FC00200000025641D2FC0641D30B00400000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-03-24 15:23:22 Download
        else if (idDebug == -15) {
            Log.e(TAG, "2023-03-24 15:23:22 Download");
        }
        //2023-03-26 11:32:14 Download
        else if (idDebug == -16) {
            Log.e(TAG, "2023-03-26 11:32:14 Download");
            contentData = "82641E3547641E8137000000120000000200000021000000C90000004802030000140004641E3547641E36AF0200000006641E36AF641E3DF3040000001F641E3DF3641E47170200000027641E4717641E47530400000001";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641E3547641E8137000000120000000200000021000000C90000004802030000140404641E4753641E49330100000008641E4933641E4B4F0200000009641E4B4F641E4B8B0400000001641E4B8B641E4DE3020000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641E3547641E8137000000120000000200000021000000C90000004802030000140804641E4DE3641E4FC30300000008641E4FC3641E5293020000000C641E5293641E57F70300000017641E57F7641E5DD30200000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641E3547641E8137000000120000000200000021000000C90000004802030000140C04641E5DD3641E5F770300000007641E5F77641E689B0200000027641E689B641E70930300000022641E7093641E7327020000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641E3547641E8137000000120000000200000021000000C90000004802030000141004641E7327641E757F010000000A641E757F641E788B020000000D641E788B641E7EDF020000001B641E7EDF641E8137020000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F0A7B641F1C0F000000000000000000000012000000290000001002030100070004641F0A7B641F0BE30200000006641F0BE3641F0DC30400000008641F0DC3641F110B020000000E641F110B641F1363040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F0A7B641F1C0F000000000000000000000012000000290000001002030100070403641F1363641F15070200000007641F1507641F18C70300000010641F18C7641F1C0F020000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086010302001D0004641F89B7641F8B1F0200000006641F8B1F641F992F040000003C641F992F641F9AD30200000007641F9AD3641F9DA3030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086010302001D0404641F9DA3641FA037040000000B641FA037641FA37F030000000E641FA37F641FA59B0400000009641FA59B641FB6B70200000049";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086010302001D0804641FB6B7641FB90F040000000A641FB90F641FBB2B0200000009641FBB2B641FBEEB0400000010641FBEEB641FC143020000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086010302001D0C04641FC143641FC53F0400000011641FC53F641FCB93020000001B641FCB93641FD0F70300000017641FD0F7641FD5A70200000014";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086010302001D1004641FD5A7641FD5E30300000001641FD5E3641FDACF0200000015641FDACF641FDCAF0300000008641FDCAF641FDE8F0200000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086010302001D1404641FDE8F641FE0330400000007641FE033641FE55B0200000016641FE55B641FE73B0300000008641FE73B641FEA0B020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086010302001D1804641FEA0B641FF36B0300000028641FF36B641FF7A30200000012641FF7A3641FFE33030000001C641FFE33642000C7020000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086010302001D1C01642000C76420022F0400000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086010303001D0004641F89B7641F8B1F0200000006641F8B1F641F992F040000003C641F992F641F9AD30200000007641F9AD3641F9DA3030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086010303001D0404641F9DA3641FA037040000000B641FA037641FA37F030000000E641FA37F641FA59B0400000009641FA59B641FB6B70200000049";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086010303001D0804641FB6B7641FB90F040000000A641FB90F641FBB2B0200000009641FBB2B641FBEEB0400000010641FBEEB641FC143020000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086010303001D0C04641FC143641FC53F0400000011641FC53F641FCB93020000001B641FCB93641FD0F70300000017641FD0F7641FD5A70200000014";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086010303001D1004641FD5A7641FD5E30300000001641FD5E3641FDACF0200000015641FDACF641FDCAF0300000008641FDCAF641FDE8F0200000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086010303001D1404641FDE8F641FE0330400000007641FE033641FE55B0200000016641FE55B641FE73B0300000008641FE73B641FEA0B020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086010303001D1804641FEA0B641FF36B0300000028641FF36B641FF7A30200000012641FF7A3641FFE33030000001C641FFE33642000C7020000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086010303001D1C01642000C76420022F0400000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-03-27 08:17:52 Download
        else if (idDebug == -17) {
            Log.e(TAG, "2023-03-27 08:17:52 Download");
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086020200001D0004641F89B7641F8B1F0200000006641F8B1F641F992F040000003C641F992F641F9AD30200000007641F9AD3641F9DA3030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086020200001D0404641F9DA3641FA037040000000B641FA037641FA37F030000000E641FA37F641FA59B0400000009641FA59B641FB6B70200000049";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086020200001D0804641FB6B7641FB90F040000000A641FB90F641FBB2B0200000009641FBB2B641FBEEB0400000010641FBEEB641FC143020000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086020200001D0C04641FC143641FC53F0400000011641FC53F641FCB93020000001B641FCB93641FD0F70300000017641FD0F7641FD5A70200000014";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086020200001D1004641FD5A7641FD5E30300000001641FD5E3641FDACF0200000015641FDACF641FDCAF0300000008641FDCAF641FDE8F0200000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086020200001D1404641FDE8F641FE0330400000007641FE033641FE55B0200000016641FE55B641FE73B0300000008641FE73B641FEA0B020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086020200001D1804641FEA0B641FF36B0300000028641FF36B641FF7A30200000012641FF7A3641FFE33030000001C641FFE33642000C7020000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82641F89B76420022F000000000000000000000088000000F400000086020200001D1C01642000C76420022F0400000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826420B7B6642121E200000009000000010000003F0000012700000056020201001400046420B7B66420B91E02000000066420B91E6420BF36040000001A6420BF366420C72E02000000226420C72E6420CBA20300000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826420B7B6642121E200000009000000010000003F0000012700000056020201001404046420CBA26420DB1A02000000426420DB1A6420DE62040000000E6420DE626420E132030000000C6420E1326420EFBA020000003E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826420B7B6642121E200000009000000010000003F0000012700000056020201001408046420EFBA6420F2C6040000000D6420F2C66420F46A02000000076420F46A6420F60E03000000076420F60E642104D2020000003F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826420B7B6642121E200000009000000010000003F000001270000005602020100140C04642104D2642106EE0100000009642106EE64210A72020000000F64210A7264211012030000001864211012642116A2020000001C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826420B7B6642121E200000009000000010000003F000001270000005602020100141004642116A264211C42030000001864211C4264211CF6020000000364211CF664211F4E040000000A64211F4E642121E2020000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-03-28 08:17:34 Download
        else if (idDebug == -18) {
            Log.e(TAG, "2023-03-28 08:17:34 Download");
            contentData = "8264220947642269D70000000600000001000000540000010100000041020100001700046422094764220AAF020000000664220AAF64220FD7040000001664220FD76422131F020000000E6422131F642218BF0400000018";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264220947642269D7000000060000000100000054000001010000004102010000170404642218BF64221ADB020000000964221ADB64221C7F040000000764221C7F64221ED7020000000A64221ED7642220B70300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264220947642269D7000000060000000100000054000001010000004102010000170804642220B76422221F02000000066422221F6422261B04000000116422261B64222693020000000264222693642227BF0400000005";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264220947642269D7000000060000000100000054000001010000004102010000170C04642227BF6422324B020000002D6422324B64223BAB020000002864223BAB64223EB7030000000D64223EB764224187020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264220947642269D700000006000000010000005400000101000000410201000017100464224187642243A30400000009642243A36422450B01000000066422450B642246AF0200000007642246AF64224DB7030000001E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264220947642269D700000006000000010000005400000101000000410201000017140364224DB7642262CF020000005A642262CF64226617030000000E64226617642269D70200000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }

        //2023-03-29 08:16:21  -> 1
        else if (idDebug == 1) {
            Log.e(TAG, "2023-03-29 08:16:21");
            contentData = "82642361A46423B89800000013000000010000002E000000DE00000054020100000E0004642361A46423630C02000000066423630C64236C30040000002764236C3064236D98020000000664236D986423702C030000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642361A46423B89800000013000000010000002E000000DE00000054020100000E04046423702C64238238020000004D6423823864238AE4030000002564238AE464239B10020000004564239B106423A0B00300000018";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642361A46423B89800000013000000010000002E000000DE00000054020100000E08046423A0B06423A3F8020000000E6423A3F86423A6C8030000000C6423A6C86423AEC002000000226423AEC06423B0640400000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642361A46423B89800000013000000010000002E000000DE00000054020100000E0C026423B0646423B4D801000000136423B4D86423B8980200000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-03-30 08:12:47  -> 2
        else if (idDebug == 2) {
            Log.e(TAG, "2023-03-30 08:12:47");
            contentData = "826424AEFD64250D35000000000000000000000046000000F20000005A020100001000046424AEFD6424B06502000000066424B0656424B5C904000000176424B5C96424C39D020000003B6424C39D6424CF190300000031";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826424AEFD64250D35000000000000000000000046000000F20000005A020100001004046424CF196424D31502000000116424D3156424D38D04000000026424D38D6424DA1D020000001C6424DA1D6424DC75040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826424AEFD64250D35000000000000000000000046000000F20000005A020100001008046424DC756424F06102000000556424F0616424F9FD03000000296424F9FD6424FB6502000000066424FB656424FDF9040000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826424AEFD64250D35000000000000000000000046000000F20000005A02010000100C046424FDF964250489020000001C64250489642507D1040000000E642507D164250ADD020000000D64250ADD64250D35040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-04-07 00:55:39  -> 3
        else if (idDebug == 3) {
            Log.e(TAG, "22023-04-07 00:55:39 ");
            contentData = "82642600B164266C810000000000000000000000530000010900000070020700001A0004642600B1642602190200000006642602196426059D040000000F6426059D6426070502000000066426070564260EFD0300000022";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642600B164266C810000000000000000000000530000010900000070020700001A040464260EFD64260FB1020000000364260FB1642614250400000013642614256426158D02000000066426158D6426167D0400000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642600B164266C810000000000000000000000530000010900000070020700001A08046426167D642617A90200000005642617A964261BE1030000001264261BE164262091020000001464262091642623D9040000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642600B164266C810000000000000000000000530000010900000070020700001A0C04642623D964263405020000004564263405642639E10300000019642639E1642644A9020000002E642644A9642649590300000014";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642600B164266C810000000000000000000000530000010900000070020700001A10046426495964265025020000001D6426502564265205040000000864265205642657E10200000019642657E164265AB1040000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642600B164266C810000000000000000000000530000010900000070020700001A140464265AB1642660C9020000001A642660C96426644D030000000F6426644D6426697502000000166426697564266B190400000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642600B164266C810000000000000000000000530000010900000070020700001A180264266B1964266B91020000000264266B9164266C810400000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642756856427D02900000009000000010000007F000000E800000097020701001D000464275685642757ED0200000006642757ED64276981040000004B6427698164277089020000001E64277089642771B50200000005";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642756856427D02900000009000000010000007F000000E800000097020701001D0404642771B5642775ED0300000012642775ED64277A9D020000001464277A9D6427807902000000196427807964278709020000001C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642756856427D02900000009000000010000007F000000E800000097020701001D0804642787096427883504000000056427883564278EC5020000001C64278EC56427989D030000002A6427989D64279B31020000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642756856427D02900000009000000010000007F000000E800000097020701001D0C0464279B3164279C99040000000664279C9964279D89020000000464279D8964279E79040000000464279E7964279FA50200000005";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642756856427D02900000009000000010000007F000000E800000097020701001D100464279FA56427A2ED030000000E6427A2ED6427A50904000000096427A5096427A79D020000000B6427A79D6427A97D0400000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642756856427D02900000009000000010000007F000000E800000097020701001D14046427A97D6427AB2102000000076427AB216427AE69040000000E6427AE696427AF1D02000000036427AF1D6427B7510300000023";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642756856427D02900000009000000010000007F000000E800000097020701001D18046427B7516427C2CD02000000316427C2CD6427C3BD04000000046427C3BD6427C5D901000000096427C5D96427CFB1030000002A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642756856427D02900000009000000010000007F000000E800000097020701001D1C016427CFB16427D0290400000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826428B68A642915B2000000000000000000000050000000F300000053020702001500046428B68A6428B7F202000000066428B7F26428C2F6040000002F6428C2F66428C3AA02000000036428C3AA6428C602030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826428B68A642915B2000000000000000000000050000000F300000053020702001504046428C6026428CE3602000000236428CE366428D2E603000000146428D2E66428D5B6020000000C6428D5B66428D7960300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826428B68A642915B2000000000000000000000050000000F300000053020702001508046428D7966428DADE020000000E6428DADE6428DC0A04000000056428DC0A6428DC4602000000016428DC466428E0420400000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826428B68A642915B2000000000000000000000050000000F30000005302070200150C046428E0426428E4F202000000146428E4F26428E96603000000136428E9666428F6C202000000396428F6C26428FA46030000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826428B68A642915B2000000000000000000000050000000F300000053020702001510046428FA46642906B20200000035642906B264290946030000000B6429094664290F22020000001964290F22642911B6040000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826428B68A642915B2000000000000000000000050000000F30000005302070200151401642911B6642915B20200000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826429EE73642A60970000000D0000000100000050000000E2000000A8020703001700046429EE736429EFDB02000000066429EFDB6429FE63040000003E6429FE63642A05A7020000001F642A05A7642A0FF7030000002C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826429EE73642A60970000000D0000000100000050000000E2000000A802070300170404642A0FF7642A137B020000000F642A137B642A13B70400000001642A13B7642A16C3010000000D642A16C3642A1A0B020000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826429EE73642A60970000000D0000000100000050000000E2000000A802070300170804642A1A0B642A1C63030000000A642A1C63642A1D170200000003642A1D17642A1FAB040000000B642A1FAB642A236B0200000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826429EE73642A60970000000D0000000100000050000000E2000000A802070300170C04642A236B642A27DF0300000013642A27DF642A2B63020000000F642A2B63642A3577030000002B642A3577642A3847020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826429EE73642A60970000000D0000000100000050000000E2000000A802070300171004642A3847642A3ADB030000000B642A3ADB642A421F020000001F642A421F642A46930300000013642A4693642A54A3020000003C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826429EE73642A60970000000D0000000100000050000000E2000000A802070300171403642A54A3642A59CB0300000016642A59CB642A5F2F0200000017642A5F2F642A60970400000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642B4AB9642BB1D9000000000000000000000059000000DA0000008502070400170004642B4AB9642B4C210200000006642B4C21642B55810400000028642B5581642B5905030000000F642B5905642B60FD0200000022";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642B4AB9642BB1D9000000000000000000000059000000DA0000008502070400170404642B60FD642B65350300000012642B6535642B67510200000009642B6751642B6D69040000001A642B6D69642B7471020000001E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642B4AB9642BB1D9000000000000000000000059000000DA0000008502070400170804642B7471642B768D0400000009642B768D642B78A90200000009642B78A9642B7FB1030000001E642B7FB1642B81190200000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642B4AB9642BB1D9000000000000000000000059000000DA0000008502070400170C04642B8119642B82F90400000008642B82F9642B92E90200000044642B92E9642B96E50300000011642B96E5642B9AA50200000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642B4AB9642BB1D9000000000000000000000059000000DA0000008502070400171004642B9AA5642B9EDD0300000012642B9EDD642BA225020000000E642BA225642BA6210300000011642BA621642BA9A5020000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642B4AB9642BB1D9000000000000000000000059000000DA0000008502070400171403642BA9A5642BADDD0300000012642BADDD642BB071020000000B642BB071642BB1D90400000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642C9192642CF9A20000001F0000000100000075000000D10000005702070500150004642C9192642C92FA0200000006642C92FA642CA092040000003A642CA092642CA88A0200000022642CA88A642CADEE0400000017";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642C9192642CF9A20000001F0000000100000075000000D10000005702070500150404642CADEE642CB2260200000012642CB226642CB8B6040000001C642CB8B6642CBD2A0200000013642CBD2A642CBF82030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642C9192642CF9A20000001F0000000100000075000000D10000005702070500150804642CBF82642CC1260200000007642CC126642CC86A010000001F642CC86A642CCA860200000009642CCA86642CCC660400000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642C9192642CF9A20000001F0000000100000075000000D10000005702070500150C04642CCC66642CCFAE020000000E642CCFAE642CD63E030000001C642CD63E642CD9C2020000000F642CD9C2642CDD820300000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642C9192642CF9A20000001F0000000100000075000000D10000005702070500151004642CDD82642CE57A0200000022642CE57A642CE75A0300000008642CE75A642CF25E020000002F642CF25E642CF83A0300000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642C9192642CF9A20000001F0000000100000075000000D10000005702070500151401642CF83A642CF9A20200000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642DF3BF642E5ADF0000000F000000010000006F0000010100000039020706001B0004642DF3BF642DF5270200000006642DF527642DFD5B0400000023642DFD5B642E01930200000012642E0193642E076F0400000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642DF3BF642E5ADF0000000F000000010000006F0000010100000039020706001B0404642E076F642E08D70200000006642E08D7642E0C5B030000000F642E0C5B642E10570200000011642E1057642E12AF040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642DF3BF642E5ADF0000000F000000010000006F0000010100000039020706001B0804642E12AF642E184F0200000018642E184F642E1AE3030000000B642E1AE3642E1DB3020000000C642E1DB3642E2137030000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642DF3BF642E5ADF0000000F000000010000006F0000010100000039020706001B0C04642E2137642E2407040000000C642E2407642E28B70200000014642E28B7642E337F020000002E642E337F642E3B770200000022";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642DF3BF642E5ADF0000000F000000010000006F0000010100000039020706001B1004642E3B77642E3D570200000008642E3D57642E40DB010000000F642E40DB642E45C70200000015642E45C7642E476B0300000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642DF3BF642E5ADF0000000F000000010000006F0000010100000039020706001B1404642E476B642E49FF020000000B642E49FF642E4B2B0400000005642E4B2B642E4C1B0200000004642E4C1B642E51430400000016";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642DF3BF642E5ADF0000000F000000010000006F0000010100000039020706001B1803642E5143642E584B020000001E642E584B642E5A670300000009642E5A67642E5ADF0400000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-04-10 16:46:47  -> 4
        else if (idDebug == 4) {
            Log.e(TAG, "2023-04-10 16:46:47 ");
            contentData = "82642F58CA642FBC6600000000000000000000003F000000FA0000007002050000140004642F58CA642F5A320200000006642F5A32642F5FD20400000018642F5FD2642F64BE0200000015642F64BE642F6B4E040000001C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642F58CA642FBC6600000000000000000000003F000000FA0000007002050000140404642F6B4E642F6DA6020000000A642F6DA6642F712A030000000F642F712A642F7EFE020000003B642F7EFE642F83EA0300000015";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642F58CA642FBC6600000000000000000000003F000000FA0000007002050000140804642F83EA642F90560200000035642F9056642F92AE030000000A642F92AE642F9542020000000B642F9542642F96E60300000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642F58CA642FBC6600000000000000000000003F000000FA0000007002050000140C04642F96E6642F997A020000000B642F997A642F9CFE030000000F642F9CFE642FA47E0200000020642FA47E642FA5AA0400000005";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82642F58CA642FBC6600000000000000000000003F000000FA0000007002050000141004642FA5AA642FAB860200000019642FAB86642FB5D6030000002C642FB5D6642FBAFE0200000016642FBAFE642FBC660400000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826430AE6B6431158B000000000000000000000052000001190000004D020501001600046430AE6B6430AFD302000000066430AFD36430B40B04000000126430B40B6430BD2F02000000276430BD2F6430BF4B0400000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826430AE6B6431158B000000000000000000000052000001190000004D020501001604046430BF4B6430BFFF02000000036430BFFF6430C1DF03000000086430C1DF6430CE4B02000000356430CE4B6430D4DB030000001C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826430AE6B6431158B000000000000000000000052000001190000004D020501001608046430D4DB6430D67F02000000076430D67F6430D82304000000076430D8236430DEB3020000001C6430DEB36430E1FB040000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826430AE6B6431158B000000000000000000000052000001190000004D02050100160C046430E1FB6430E48F020000000B6430E48F6430E97B03000000156430E97B6430EB5B02000000086430EB5B6430ECFF0400000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826430AE6B6431158B000000000000000000000052000001190000004D020501001610046430ECFF6430F92F02000000346430F92F6430FDDF03000000146430FDDF6430FF4702000000066430FF47643100EB0400000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826430AE6B6431158B000000000000000000000052000001190000004D02050100161402643100EB643110DB0200000044643110DB6431158B0400000014";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264317BBC6431967400000016000000010000002200000022000000180205020006000464317BBC64317D24020000000664317D246431851C04000000226431851C6431859402000000026431859464318ABC0100000016";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264317BBC6431967400000016000000010000002200000022000000180205020006040264318ABC6431905C03000000186431905C64319674020000001A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826431DED4643258F00000000E000000010000002E00000123000000AA020503001600046431DED46431E03C02000000066431E03C6431E7BC04000000206431E7BC6431F33802000000316431F3386431F5540300000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826431DED4643258F00000000E000000010000002E00000123000000AA020503001604046431F5546431FBE4020000001C6431FBE46431FC2004000000016431FC206431FF68010000000E6431FF68643203640200000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826431DED4643258F00000000E000000010000002E00000123000000AA0205030016080464320364643206AC030000000E643206AC64320C88020000001964320C8864320F94040000000D64320F94643210480200000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826431DED4643258F00000000E000000010000002E00000123000000AA02050300160C046432104864322290030000004E6432229064322B00020000002464322B0064322EFC030000001164322EFC6432358C020000001C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826431DED4643258F00000000E000000010000002E00000123000000AA020503001610046432358C6432373003000000076432373064323A78020000000E64323A7864323EB0030000001264323EB06432448C0200000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826431DED4643258F00000000E000000010000002E00000123000000AA020503001614026432448C64324AE0030000001B64324AE0643258F0020000003C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264334C9A6433A442000000000000000000000066000000BA000000560205040017000464334C9A64334E02020000000664334E02643358CA040000002E643358CA64335C12020000000E64335C1264335EE2040000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264334C9A6433A442000000000000000000000066000000BA000000560205040017040464335EE2643360C20200000008643360C2643366DA030000001A643366DA64336842020000000664336842643369E60400000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264334C9A6433A442000000000000000000000066000000BA0000005602050400170804643369E664336F0E020000001664336F0E643373460400000012643373466433759E020000000A6433759E64337D1E0300000020";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264334C9A6433A442000000000000000000000066000000BA0000005602050400170C0464337D1E64338426020000001E64338426643385CA0400000007643385CA643387320200000006643387326433898A030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264334C9A6433A442000000000000000000000066000000BA00000056020504001710046433898A64338F2A020000001864338F2A6433914603000000096433914664339B96020000002C64339B9664339DB20300000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264334C9A6433A442000000000000000000000066000000BA000000560205040017140364339DB264339F56020000000764339F566433A226040000000C6433A2266433A4420200000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-04-12 12:49:29  -> 5
        else if (idDebug == 5) {
            Log.e(TAG, "2023-04-12 12:49:29");
            contentData = "82643482006434F58C0000000000000000000000720000011500000066020200001700046434820064348368020000000664348368643489BC040000001B643489BC64348F98030000001964348F98643491000200000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643482006434F58C0000000000000000000000720000011500000066020200001704046434910064349484040000000F64349484643499AC0200000016643499AC6434A1A403000000226434A1A46434A2580200000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643482006434F58C0000000000000000000000720000011500000066020200001708046434A2586434A4EC040000000B6434A4EC6434AC6C02000000206434AC6C6434AEC4040000000A6434AEC46434B590020000001D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643482006434F58C000000000000000000000072000001150000006602020000170C046434B5906434BB6C04000000196434BB6C6434BF2C02000000106434BF2C6434C50803000000196434C5086434D354020000003D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643482006434F58C0000000000000000000000720000011500000066020200001710046434D3546434D78C03000000126434D78C6434D96C02000000086434D96C6434DB4C04000000086434DB4C6434DF0C0200000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643482006434F58C0000000000000000000000720000011500000066020200001714036434DF0C6434E254040000000E6434E2546434F49C020000004E6434F49C6434F58C0400000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826435E73764363D3B0000000A0000000100000041000000C90000005B020201001200046435E7376435E89F02000000066435E89F6435ED8B04000000156435ED8B6435F1FF02000000136435F1FF6435F493030000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826435E73764363D3B0000000A0000000100000041000000C90000005B020201001204046435F4936435FE6B020000002A6435FE6B6436040B04000000186436040B643606270200000009643606276436096F030000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826435E73764363D3B0000000A0000000100000041000000C90000005B020201001208046436096F64360D6B020000001164360D6B6436121B04000000146436121B64361473010000000A64361473643616530200000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826435E73764363D3B0000000A0000000100000041000000C90000005B02020100120C046436165364361B7B030000001664361B7B6436242702000000256436242764362BE3030000002164362BE36436348F0200000025";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826435E73764363D3B0000000A0000000100000041000000C90000005B020201001210026436348F64363723030000000B6436372364363D3B020000001A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-04-14 08:08:41  -> 6
        else if (idDebug == 6) {
            Log.e(TAG, "2023-04-14 08:08:41");
            contentData = "8264372D2E643788D200000000000000000000004B000000E300000059020200000F000464372D2E64372E96020000000664372E9664373B7A040000003764373B7A64373D5A020000000864373D5A64373FEE040000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264372D2E643788D200000000000000000000004B000000E300000059020200000F040464373FEE64374246020000000A64374246643747E60300000018643747E6643755BA020000003B643755BA64375D760300000021";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264372D2E643788D200000000000000000000004B000000E300000059020200000F080464375D76643760BE020000000E643760BE6437626203000000076437626264377126020000003F64377126643773420400000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264372D2E643788D200000000000000000000004B000000E300000059020200000F0C036437734264377D56020000002B64377D5664378332030000001964378332643788D20200000018";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643877826438E5E600000000000000000000004E00000114000000750102010017000464387782643878EA0200000006643878EA64387E8A040000001864387E8A643882FE0300000013643882FE643888260200000016";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643877826438E5E600000000000000000000004E0000011400000075010201001704046438882664388AF6040000000C64388AF664388D12030000000964388D126438914A02000000126438914A64389492030000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643877826438E5E600000000000000000000004E00000114000000750102010017080464389492643897DA020000000E643897DA64389A6E040000000B64389A6E64389F96020000001664389F966438A2A2030000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643877826438E5E600000000000000000000004E000001140000007501020100170C046438A2A26438AFC202000000386438AFC26438B292040000000C6438B2926438B99A030000001E6438B99A6438C5CA0200000034";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643877826438E5E600000000000000000000004E0000011400000075010201001710046438C5CA6438C822030000000A6438C8226438D7D602000000436438D7D66438D9B603000000086438D9B66438DC86020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643877826438E5E600000000000000000000004E0000011400000075010201001714036438DC866438DFCE030000000E6438DFCE6438E17202000000076438E1726438E5E60400000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643877826438E5E600000000000000000000004E00000114000000750102020017000464387782643878EA0200000006643878EA64387E8A040000001864387E8A643882FE0300000013643882FE643888260200000016";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643877826438E5E600000000000000000000004E0000011400000075010202001704046438882664388AF6040000000C64388AF664388D12030000000964388D126438914A02000000126438914A64389492030000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643877826438E5E600000000000000000000004E00000114000000750102020017080464389492643897DA020000000E643897DA64389A6E040000000B64389A6E64389F96020000001664389F966438A2A2030000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643877826438E5E600000000000000000000004E000001140000007501020200170C046438A2A26438AFC202000000386438AFC26438B292040000000C6438B2926438B99A030000001E6438B99A6438C5CA0200000034";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643877826438E5E600000000000000000000004E0000011400000075010202001710046438C5CA6438C822030000000A6438C8226438D7D602000000436438D7D66438D9B603000000086438D9B66438DC86020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643877826438E5E600000000000000000000004E0000011400000075010202001714036438DC866438DFCE030000000E6438DFCE6438E17202000000076438E1726438E5E60400000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-04-18 20:47:21  -> 7
        else if (idDebug == 7) {
            Log.e(TAG, "2023-04-18 20:47:21");
            contentData = "82643877826438E5E600000000000000000000004E00000114000000750205000017000464387782643878EA0200000006643878EA64387E8A040000001864387E8A643882FE0300000013643882FE643888260200000016";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643877826438E5E600000000000000000000004E0000011400000075020500001704046438882664388AF6040000000C64388AF664388D12030000000964388D126438914A02000000126438914A64389492030000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643877826438E5E600000000000000000000004E00000114000000750205000017080464389492643897DA020000000E643897DA64389A6E040000000B64389A6E64389F96020000001664389F966438A2A2030000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643877826438E5E600000000000000000000004E000001140000007502050000170C046438A2A26438AFC202000000386438AFC26438B292040000000C6438B2926438B99A030000001E6438B99A6438C5CA0200000034";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643877826438E5E600000000000000000000004E0000011400000075020500001710046438C5CA6438C822030000000A6438C8226438D7D602000000436438D7D66438D9B603000000086438D9B66438DC86020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643877826438E5E600000000000000000000004E0000011400000075020500001714036438DC866438DFCE030000000E6438DFCE6438E17202000000076438E1726438E5E60400000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826439CE32643A3C1E0000000000000000000000580000011600000067020501001400046439CE326439CF9A02000000066439CF9A6439DA9E040000002F6439DA9E6439DDE6020000000E6439DDE66439DFC60300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826439CE32643A3C1E0000000000000000000000580000011600000067020501001404046439DFC66439E2D2020000000D6439E2D26439E5DE030000000D6439E5DE6439E7FA02000000096439E7FA6439EE8A040000001C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826439CE32643A3C1E0000000000000000000000580000011600000067020501001408046439EE8A6439FC9A020000003C6439FC9A6439FEF2040000000A6439FEF2643A023A020000000E643A023A643A04CE030000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826439CE32643A3C1E000000000000000000000058000001160000006702050100140C04643A04CE643A0D020200000023643A0D02643A16DA030000002A643A16DA643A24AE020000003B643A24AE643A26520300000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826439CE32643A3C1E000000000000000000000058000001160000006702050100141004643A2652643A31CE0200000031643A31CE643A36F60300000016643A36F6643A3B6A0200000013643A3B6A643A3C1E0400000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643B258B643B9A7F0000001B0000000100000056000001100000007202050200210004643B258B643B26F30200000006643B26F3643B2987040000000B643B2987643B2D0B020000000F643B2D0B643B339B040000001C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643B258B643B9A7F0000001B0000000100000056000001100000007202050200210404643B339B643B38FF0200000017643B38FF643B3C83030000000F643B3C83643B3DAF0200000005643B3DAF643B3EDB0300000005";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643B258B643B9A7F0000001B0000000100000056000001100000007202050200210804643B3EDB643B41E7020000000D643B41E7643B465B0400000013643B465B643B4B830200000016643B4B83643B4E8F030000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643B258B643B9A7F0000001B0000000100000056000001100000007202050200210C04643B4E8F643B515F020000000C643B515F643B542F040000000C643B542F643B58A30200000013643B58A3643B5B73030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643B258B643B9A7F0000001B0000000100000056000001100000007202050200211004643B5B73643B5D170200000007643B5D17643B5F330400000009643B5F33643B65C3020000001C643B65C3643B67670400000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643B258B643B9A7F0000001B0000000100000056000001100000007202050200211404643B6767643B69470200000008643B6947643B6AAF0300000006643B6AAF643B704F0200000018643B704F643B7793030000001F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643B258B643B9A7F0000001B0000000100000056000001100000007202050200211804643B7793643B79370200000007643B7937643B7E9B0300000017643B7E9B643B80B70200000009643B80B7643B82D30300000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643B258B643B9A7F0000001B0000000100000056000001100000007202050200211C04643B82D3643B87BF0200000015643B87BF643B8E13020000001B643B8E13643B90E3020000000C643B90E3643B9737010000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643B258B643B9A7F0000001B0000000100000056000001100000007202050200212001643B9737643B9A7F020000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643C701E643CE1CA000000000000000000000052000001360000005D02050300130004643C701E643C71860200000006643C7186643C7AAA0400000027643C7AAA643C7E2E020000000F643C7E2E643C800E0300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643C701E643CE1CA000000000000000000000052000001360000005D02050300130404643C800E643C83CE0200000010643C83CE643C8C7A0400000025643C8C7A643C9C6A0200000044643C9C6A643C9FEE030000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643C701E643CE1CA000000000000000000000052000001360000005D02050300130804643C9FEE643CA5520200000017643CA552643CA7E6030000000B643CA7E6643CAC960200000014643CAC96643CB0CE0300000012";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643C701E643CE1CA000000000000000000000052000001360000005D02050300130C04643CB0CE643CB9020200000023643CB902643CBA6A0400000006643CBA6A643CBC860200000009643CBC86643CC47E0300000022";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643C701E643CE1CA000000000000000000000052000001360000005D02050300131003643CC47E643CC87A0200000011643CC87A643CCA1E0300000007643CCA1E643CE1CA0200000065";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643DB68F643E2F430000002C00000001000000340000012B0000007802050400180004643DB68F643DB7F70200000006643DB7F7643DC0DF0400000026643DC0DF643DCB2F010000002C643DCB2F643DCEEF0200000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643DB68F643E2F430000002C00000001000000340000012B0000007802050400180404643DCEEF643DD273030000000F643DD273643DD4CB020000000A643DD4CB643DD6AB0400000008643DD6AB643DDF570200000025";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643DB68F643E2F430000002C00000001000000340000012B0000007802050400180804643DDF57643DE0470400000004643DE047643DE713020000001D643DE713643DE9E3030000000C643DE9E3643DED2B020000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643DB68F643E2F430000002C00000001000000340000012B0000007802050400180C04643DED2B643DEFFB030000000C643DEFFB643DF55F0200000017643DF55F643DFEFB0300000029643DFEFB643E0243020000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643DB68F643E2F430000002C00000001000000340000012B0000007802050400181004643E0243643E02BB0400000002643E02BB643E09C3020000001E643E09C3643E0BA30300000008643E0BA3643E0E73020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643DB68F643E2F430000002C00000001000000340000012B0000007802050400181404643E0E73643E12330300000010643E1233643E22230200000044643E2223643E25E30300000010643E25E3643E2F430200000028";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-04-24 08:35:30  -> 8
        else if (idDebug == 8) {
            Log.e(TAG, "2023-04-24 08:35:30");
            contentData = "82643EFBF7643F75D70000000B000000010000004A000001560000005D020700001B0004643EFBF7643EFD5F0200000006643EFD5F643F01970400000012643F0197643F07370200000018643F0737643F098F040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643EFBF7643F75D70000000B000000010000004A000001560000005D020700001B0404643F098F643F0C9B020000000D643F0C9B643F0EB70200000009643F0EB7643F14930300000019643F1493643F16AF0200000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643EFBF7643F75D70000000B000000010000004A000001560000005D020700001B0804643F16AF643F18530400000007643F1853643F1AE7010000000B643F1AE7643F1B9B0200000003643F1B9B643F1DB70300000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643EFBF7643F75D70000000B000000010000004A000001560000005D020700001B0C04643F1DB7643F1F5B0200000007643F1F5B643F240B0400000014643F240B643F377F0200000053643F377F643F39230300000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643EFBF7643F75D70000000B000000010000004A000001560000005D020700001B1004643F3923643F411B0200000022643F411B643F4427030000000D643F4427643F476F020000000E643F476F643F49C7040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643EFBF7643F75D70000000B000000010000004A000001560000005D020700001B1404643F49C7643F52370200000024643F5237643F557F030000000E643F557F643F5F57020000002A643F5F57643F65330300000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82643EFBF7643F75D70000000B000000010000004A000001560000005D020700001B1803643F6533643F6C3B020000001E643F6C3B643F6E570400000009643F6E57643F75D70200000020";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264405A9C6440BC94000000030000000100000051000000F9000000550207010015000464405A9C64405C04020000000664405C04644064B00400000025644064B064406BB8020000001E64406BB8644074280200000024";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264405A9C6440BC94000000030000000100000051000000F900000055020701001504046440742864407D88030000002864407D8864407FA4020000000964407FA464408454040000001464408454644088500200000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264405A9C6440BC94000000030000000100000051000000F900000055020701001508046440885064408B20030000000C64408B2064408E2C020000000D64408E2C644091B0030000000F644091B06440987C020000001D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264405A9C6440BC94000000030000000100000051000000F90000005502070100150C046440987C64409C00040000000F64409C0064409E94020000000B64409E946440A0B003000000096440A0B06440AD1C0200000035";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264405A9C6440BC94000000030000000100000051000000F900000055020701001510046440AD1C6440AF3803000000096440AF386440B0A002000000066440B0A06440B2BC04000000096440B2BC6440B3700100000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264405A9C6440BC94000000030000000100000051000000F900000055020701001514016440B3706440BC940200000027";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826441A3A16442133100000000000000000000005C0000012D00000053020702001100046441A3A16441A50902000000066441A5096441AB21040000001A6441AB216441AD79020000000A6441AD796441B751040000002A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826441A3A16442133100000000000000000000005C0000012D00000053020702001104046441B7516441C30902000000326441C3096441C59D030000000B6441C59D6441C65102000000036441C6516441C9D5040000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826441A3A16442133100000000000000000000005C0000012D00000053020702001108046441C9D56441DE3902000000576441DE396441E9B503000000316441E9B56441F5E502000000346441F5E56441F9A50300000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826441A3A16442133100000000000000000000005C0000012D0000005302070200110C046441F9A56441FBFD020000000A6441FBFD6441FE1904000000096441FE1964420C65020000003D64420C6564420E090300000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826441A3A16442133100000000000000000000005C0000012D000000530207020011100164420E09644213310200000016";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264429D54644321C0000000140000000200000022000001840000007B020703001B000464429D5464429EBC020000000664429EBC6442A2F404000000126442A2F46442A54C010000000A6442A54C6442A81C020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264429D54644321C0000000140000000200000022000001840000007B020703001B04046442A81C6442AC5402000000126442AC546442AF9C020000000E6442AF9C6442B08C04000000046442B08C6442B35C020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264429D54644321C0000000140000000200000022000001840000007B020703001B08046442B35C6442B53C02000000086442B53C6442B7D0020000000B6442B7D06442B8FC02000000056442B8FC6442BADC0400000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264429D54644321C0000000140000000200000022000001840000007B020703001B0C046442BADC6442BE60020000000F6442BE606442BF5004000000046442BF506442CACC02000000316442CACC6442D33C0300000024";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264429D54644321C0000000140000000200000022000001840000007B020703001B10046442D33C6442E05C02000000386442E05C6442EA34030000002A6442EA346442F4FC020000002E6442F4FC6442F808030000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264429D54644321C0000000140000000200000022000001840000007B020703001B14046442F8086442FB50020000000E6442FB506442FCF403000000076442FCF4644308E80200000033644308E864430EC40300000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264429D54644321C0000000140000000200000022000001840000007B020703001B180364430EC464431D4C020000003E64431D4C64431FA4010000000A64431FA4644321C00200000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264442AFF644471C700000000000000000000003A000000A90000004B0207040007000464442AFF64442C67020000000664442C67644439FF040000003A644439FF64443C1B020000000964443C1B644446E3030000002E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264442AFF644471C700000000000000000000003A000000A90000004B02070400070403644446E36444525F02000000316444525F6444592B030000001D6444592B644471C70200000069";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826444F7E1644509B10000000000000000000000010000000800000043020705000300036444F7E16444F9C102000000086444F9C16444F9FD04000000016444F9FD644509B10300000043";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C6E2000000000000000000000069000000CE0000006701070600190004644565DA6445674202000000066445674264456D96040000001B64456D9664456E4A020000000364456E4A644574260300000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C6E2000000000000000000000069000000CE000000670107060019040464457426644574DA0200000003644574DA64457A3E040000001764457A3E64457C1E020000000864457C1E64458272030000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C6E2000000000000000000000069000000CE0000006701070600190804644582726445893E020000001D6445893E64458CC2040000000F64458CC264458E66020000000764458E66644594060300000018";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C6E2000000000000000000000069000000CE0000006701070600190C04644594066445974E020000000E6445974E6445992E03000000086445992E64459D66020000001264459D666445A3F6040000001C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C6E2000000000000000000000069000000CE00000067010706001910046445A3F66445A59A02000000076445A59A6445A82E030000000B6445A82E6445AF72020000001F6445AF726445B1CA040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C6E2000000000000000000000069000000CE00000067010706001914046445B1CA6445B20602000000016445B2066445B27E04000000026445B27E6445BBDE02000000286445BBDE6445BDBE0300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C6E2000000000000000000000069000000CE00000067010706001918016445BDBE6445C6E20200000027";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C97600000000000000000000006B000000D700000067010707001A0004644565DA6445674202000000066445674264456D96040000001B64456D9664456E4A020000000364456E4A644574260300000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C97600000000000000000000006B000000D700000067010707001A040464457426644574DA0200000003644574DA64457A3E040000001764457A3E64457C1E020000000864457C1E64458272030000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C97600000000000000000000006B000000D700000067010707001A0804644582726445893E020000001D6445893E64458CC2040000000F64458CC264458E66020000000764458E66644594060300000018";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C97600000000000000000000006B000000D700000067010707001A0C04644594066445974E020000000E6445974E6445992E03000000086445992E64459D66020000001264459D666445A3F6040000001C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C97600000000000000000000006B000000D700000067010707001A10046445A3F66445A59A02000000076445A59A6445A82E030000000B6445A82E6445AF72020000001F6445AF726445B1CA040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C97600000000000000000000006B000000D700000067010707001A14046445B1CA6445B20602000000016445B2066445B27E04000000026445B27E6445BBDE02000000286445BBDE6445BDBE0300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C97600000000000000000000006B000000D700000067010707001A18026445BDBE6445C8FE02000000306445C8FE6445C9760400000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-04-25 15:28:42  -> 9
        else if (idDebug == 9) {
            Log.e(TAG, "2023-04-25 15:28:42");
            contentData = "82644565DA6445C97600000000000000000000006B000000D700000067020200001A0004644565DA6445674202000000066445674264456D96040000001B64456D9664456E4A020000000364456E4A644574260300000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C97600000000000000000000006B000000D700000067020200001A040464457426644574DA0200000003644574DA64457A3E040000001764457A3E64457C1E020000000864457C1E64458272030000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C97600000000000000000000006B000000D700000067020200001A0804644582726445893E020000001D6445893E64458CC2040000000F64458CC264458E66020000000764458E66644594060300000018";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C97600000000000000000000006B000000D700000067020200001A0C04644594066445974E020000000E6445974E6445992E03000000086445992E64459D66020000001264459D666445A3F6040000001C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C97600000000000000000000006B000000D700000067020200001A10046445A3F66445A59A02000000076445A59A6445A82E030000000B6445A82E6445AF72020000001F6445AF726445B1CA040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C97600000000000000000000006B000000D700000067020200001A14046445B1CA6445B20602000000016445B2066445B27E04000000026445B27E6445BBDE02000000286445BBDE6445BDBE0300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644565DA6445C97600000000000000000000006B000000D700000067020200001A18026445BDBE6445C8FE02000000306445C8FE6445C9760400000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826447168664476EA6000000080000000100000065000000CC0000003F0202010016000464471686644717EE0200000006644717EE644720D60400000026644720D664472586020000001464472586644729BE0400000012";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826447168664476EA6000000080000000100000065000000CC0000003F02020100160404644729BE6447304E030000001C6447304E644731B60200000006644731B66447366604000000146447366664473A620200000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826447168664476EA6000000080000000100000065000000CC0000003F0202010016080464473A6264473DAA040000000E64473DAA6447443A020000001C6447443A644748AE0300000013644748AE644749DA0200000005";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826447168664476EA6000000080000000100000065000000CC0000003F02020100160C04644749DA64474BBA010000000864474BBA64474D5E020000000764474D5E64474F3E030000000864474F3E644751D2020000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826447168664476EA6000000080000000100000065000000CC0000003F02020100161004644751D264475466040000000B6447546664476582020000004964476582644766AE0300000005644766AE6447679E0200000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826447168664476EA6000000080000000100000065000000CC0000003F020201001614026447679E6447685203000000036447685264476EA6020000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-05-06 11:56:37  -> 10
        else if (idDebug == 10) {
            Log.e(TAG, "2023-05-06 11:56:37 ");
            contentData = "8264483CCC6448AA4000000009000000010000006E000000E900000073020C000013000464483CCC64483E34020000000664483E3464484578040000001F6448457864484B54020000001964484B5464485A900400000041";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264483CCC6448AA4000000009000000010000006E000000E900000073020C000013040464485A9064485EC8020000001264485EC86448606C03000000076448606C64486738020000001D6448673864486BE80300000014";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264483CCC6448AA4000000009000000010000006E000000E900000073020C000013080464486BE864487CC8020000004864487CC864487EE4030000000964487EE464488088020000000764488088644883D0040000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264483CCC6448AA4000000009000000010000006E000000E900000073020C0000130C04644883D064488C04030000002364488C04644891E00200000019644891E064489A14030000002364489A146448A158020000001F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264483CCC6448AA4000000009000000010000006E000000E900000073020C00001310036448A1586448A37401000000096448A3746448A82402000000146448A8246448AA400300000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264493ED76449A1BF000000000000000000000046000000ED00000073020C010013000464493ED76449403F02000000066449403F64494D9B040000003964494D9B64494FB7020000000964494FB76449520F030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264493ED76449A1BF000000000000000000000046000000ED00000073020C01001304046449520F6449542B02000000096449542B6449589F03000000136449589F6449632B020000002D6449632B64496637040000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264493ED76449A1BF000000000000000000000046000000ED00000073020C010013080464496637644968170200000008644968176449704B03000000236449704B64498293020000004E64498293644984370300000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264493ED76449A1BF000000000000000000000046000000ED00000073020C0100130C0464498437644987BB020000000F644987BB6449895F03000000076449895F64498C6B020000000D64498C6B64498EC3030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264493ED76449A1BF000000000000000000000046000000ED00000073020C010013100364498EC364499607020000001F6449960764499C5B030000001B64499C5B6449A1BF0200000017";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644A940D644B0181000000070000000100000085000000E700000060020C02001D0004644A940D644A95750200000006644A9575644A9C7D040000001E644A9C7D644A9D310200000003644A9D31644AA12D0300000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644A940D644B0181000000070000000100000085000000E700000060020C02001D0404644AA12D644AA4B1020000000F644AA4B1644AA9610400000014644AA961644AAC6D020000000D644AAC6D644AAE890400000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644A940D644B0181000000070000000100000085000000E700000060020C02001D0804644AAE89644AB11D020000000B644AB11D644AB2C10400000007644AB2C1644AB8610200000018644AB861644ABA410400000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644A940D644B0181000000070000000100000085000000E700000060020C02001D0C04644ABA41644ABBA90200000006644ABBA9644AC3290300000020644AC329644AC4910200000006644AC491644AC6350400000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644A940D644B0181000000070000000100000085000000E700000060020C02001D1004644AC635644AC7D90100000007644AC7D9644ACB21020000000E644ACB21644ACF1D0300000011644ACF1D644AD571020000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644A940D644B0181000000070000000100000085000000E700000060020C02001D1404644AD571644AD7150300000007644AD715644ADC010200000015644ADC01644AE0750400000013644AE075644AEB3D020000002E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644A940D644B0181000000070000000100000085000000E700000060020C02001D1804644AEB3D644AEE49030000000D644AEE49644AF5C90200000020644AF5C9644AF821030000000A644AF821644AF9C50200000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644A940D644B0181000000070000000100000085000000E700000060020C02001D1C01644AF9C5644B01810400000021";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644BEDF0644C59FC0000000D0000000100000056000000DE0000008C020C0300160004644BEDF0644BEF580200000006644BEF58644BFE580400000040644BFE58644C02900200000012644C0290644C0A100300000020";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644BEDF0644C59FC0000000D0000000100000056000000DE0000008C020C0300160404644C0A10644C0BB40200000007644C0BB4644C0DD00300000009644C0DD0644C1064020000000B644C1064644C14240300000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644BEDF0644C59FC0000000D0000000100000056000000DE0000008C020C0300160804644C1424644C284C0200000056644C284C644C2A2C0400000008644C2A2C644C2AE00200000003644C2AE0644C2E64030000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644BEDF0644C59FC0000000D0000000100000056000000DE0000008C020C0300160C04644C2E64644C3170020000000D644C3170644C35A80300000012644C35A8644C37100200000006644C3710644C3A58040000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644BEDF0644C59FC0000000D0000000100000056000000DE0000008C020C0300161004644C3A58644C3E180200000010644C3E18644C4124010000000D644C4124644C4B38030000002B644C4B38644C57A40200000035";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644BEDF0644C59FC0000000D0000000100000056000000DE0000008C020C0300161402644C57A4644C59480300000007644C5948644C59FC0200000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644D4958644DB7F80000000C000000010000006D0000006B000000F4020C04001F0004644D4958644D4AC00200000006644D4AC0644D4F340400000013644D4F34644D51500200000009644D5150644D59480300000022";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644D4958644DB7F80000000C000000010000006D0000006B000000F4020C04001F0404644D5948644D5C54020000000D644D5C54644D5EAC040000000A644D5EAC644D6CBC030000003C644D6CBC644D6DAC0200000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644D4958644DB7F80000000C000000010000006D0000006B000000F4020C04001F0804644D6DAC644D70F4040000000E644D70F4644D752C0300000012644D752C644D77FC010000000C644D77FC644D7B80040000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644D4958644DB7F80000000C000000010000006D0000006B000000F4020C04001F0C04644D7B80644D824C030000001D644D824C644D84E0020000000B644D84E0644D87B0030000000C644D87B0644D8C240200000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644D4958644DB7F80000000C000000010000006D0000006B000000F4020C04001F1004644D8C24644D8D140400000004644D8D14644D8FA8020000000B644D8FA8644D932C030000000F644D932C644D9584040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644D4958644DB7F80000000C000000010000006D0000006B000000F4020C04001F1404644D9584644D96380200000003644D9638644D96740300000001644D9674644D98180200000007644D9818644D98CC0300000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644D4958644DB7F80000000C000000010000006D0000006B000000F4020C04001F1804644D98CC644D9B9C020000000C644D9B9C644D9DB80400000009644D9DB8644DA5740300000021644DA574644DA844020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644D4958644DB7F80000000C000000010000006D0000006B000000F4020C04001F1C03644DA844644DADA80300000017644DADA8644DB438040000001C644DB438644DB7F80300000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644E94A5644F068D00000017000000010000006F000000ED00000073020C0500180004644E94A5644E960D0200000006644E960D644EA3690400000039644EA369644EA6ED020000000F644EA6ED644EAF5D0400000024";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644E94A5644F068D00000017000000010000006F000000ED00000073020C0500180404644EAF5D644EB7CD0200000024644EB7CD644EBFC50300000022644EBFC5644EC259020000000B644EC259644EC4B1030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644E94A5644F068D00000017000000010000006F000000ED00000073020C0500180804644EC4B1644EC9250200000013644EC925644ECB410400000009644ECB41644ECD210200000008644ECD21644ECF3D0400000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644E94A5644F068D00000017000000010000006F000000ED00000073020C0500180C04644ECF3D644ED4A10100000017644ED4A1644EDCD50200000023644EDCD5644EE059030000000F644EE059644EE9050200000025";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644E94A5644F068D00000017000000010000006F000000ED00000073020C0500181004644EE905644EEBD5030000000C644EEBD5644EF0490200000013644EF049644EF4450300000011644EF445644EFBC50200000020";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644E94A5644F068D00000017000000010000006F000000ED00000073020C0500181404644EFBC5644EFDE10300000009644EFDE1644F00B1020000000C644F00B1644F04E90300000012644F04E9644F068D0200000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644F74CA644F838E00000000000000000000000C000000270000000C020C0600050004644F74CA644F76320200000006644F7632644F7902040000000C644F7902644F7DEE0200000015644F7DEE644F80BE030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644F74CA644F838E00000000000000000000000C000000270000000C020C0600050401644F80BE644F838E020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644FD79364503EEF00000004000000010000004C000000DB0000008E020C0700230004644FD793644FD8FB0200000006644FD8FB644FDCBB0400000010644FDCBB644FE03F020000000F644FE03F644FE297040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644FD79364503EEF00000004000000010000004C000000DB0000008E020C0700230404644FE297644FEB430200000025644FEB43644FF4A30300000028644FF4A3644FF7AF020000000D644FF7AF644FF8DB0400000005";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644FD79364503EEF00000004000000010000004C000000DB0000008E020C0700230804644FF8DB644FF9170200000001644FF917644FF9530400000001644FF9536450041B020000002E6450041B645005FB0200000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644FD79364503EEF00000004000000010000004C000000DB0000008E020C0700230C04645005FB64500853040000000A645008536450094301000000046450094364500B5F030000000964500B5F64500D030200000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644FD79364503EEF00000004000000010000004C000000DB0000008E020C070023100464500D036450113B04000000126450113B64501393020000000A6450139364501C3F030000002564501C3F64501F87020000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644FD79364503EEF00000004000000010000004C000000DB0000008E020C070023140464501F87645021A30300000009645021A3645025630200000010645025636450261704000000036450261764502A4F0200000012";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644FD79364503EEF00000004000000010000004C000000DB0000008E020C070023180464502A4F64502D97030000000E64502D9764502FB3020000000964502FB3645031CF0400000009645031CF645033EB0200000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644FD79364503EEF00000004000000010000004C000000DB0000008E020C0700231C04645033EB6450394F03000000176450394F64503A03040000000364503A0364503B2F020000000564503B2F64503B6B0400000001";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82644FD79364503EEF00000004000000010000004C000000DB0000008E020C070023200364503B6B64503C1F020000000364503C1F64503E77030000000A64503E7764503EEF0200000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264512832645193C60000001B0000000100000045000000F700000074020C0800190004645128326451299A02000000066451299A64512C2E040000000B64512C2E64513282010000001B64513282645132FA0200000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264512832645193C60000001B0000000100000045000000F700000074020C0800190404645132FA6451367E030000000F6451367E6451410A020000002D6451410A6451448E040000000F6451448E645146320200000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264512832645193C60000001B0000000100000045000000F700000074020C080019080464514632645147D60300000007645147D664514CFE020000001664514CFE64514EA2030000000764514EA26451547E0200000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264512832645193C60000001B0000000100000045000000F700000074020C0800190C046451547E6451569A04000000096451569A64515B86020000001564515B8664516432030000002564516432645168A60200000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264512832645193C60000001B0000000100000045000000F700000074020C0800191004645168A664516B76040000000C64516B76645172F60200000020645172F6645175C6030000000C645175C6645177E20200000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264512832645193C60000001B0000000100000045000000F700000074020C0800191404645177E264517C92040000001464517C9264517F26020000000B64517F266451880E03000000266451880E6451934E0200000030";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264512832645193C60000001B0000000100000045000000F700000074020C08001918016451934E645193C60400000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645275716452E35D0000000900000001000000610000010400000067020C090019000464527571645277510200000008645277516452796D01000000096452796D64527DA5020000001264527DA5645280ED040000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645275716452E35D0000000900000001000000610000010400000067020C0900190404645280ED645286C90200000019645286C964528921030000000A6452892164528EFD020000001964528EFD645293AD0300000014";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645275716452E35D0000000900000001000000610000010400000067020C0900190804645293AD64529731020000000F6452973164529B69030000001264529B6964529E75040000000D64529E756452A1F9020000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645275716452E35D0000000900000001000000610000010400000067020C0900190C046452A1F96452A48D040000000B6452A48D6452A88903000000116452A8896452AF19020000001C6452AF196452B3510200000012";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645275716452E35D0000000900000001000000610000010400000067020C09001910046452B3516452B47D02000000056452B47D6452BD6503000000266452BD656452C1D904000000136452C1D96452C64D0200000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645275716452E35D0000000900000001000000610000010400000067020C09001914046452C64D6452CC65040000001A6452CC656452DCCD02000000466452DCCD6452DF61040000000B6452DF616452E2A9020000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645275716452E35D0000000900000001000000610000010400000067020C09001918016452E2A96452E35D0400000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826453C5366454317E000000000000000000000054000000FB0000007F020C0A001400046453C5366453C69E02000000066453C69E6453D562040000003F6453D5626453D86E020000000D6453D86E6453DEC2030000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826453C5366454317E000000000000000000000054000000FB0000007F020C0A001404046453DEC26453E28202000000106453E2826453E33604000000036453E3366453E642020000000D6453E6426453E7E60300000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826453C5366454317E000000000000000000000054000000FB0000007F020C0A001408046453E7E66453EFDE02000000226453EFDE6453F39E03000000106453F39E6453F7D604000000126453F7D6645410720200000069";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826453C5366454317E000000000000000000000054000000FB0000007F020C0A00140C04645410726454137E030000000D6454137E645419D2020000001B645419D264542026030000001B645420266454227E020000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826453C5366454317E000000000000000000000054000000FB0000007F020C0A001410046454227E645427E20300000017645427E2645429FE0200000009645429FE64542D46030000000E64542D466454317E0200000012";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264552FCB6455910F0000000000000000000000780000009200000095020C0B0016000464552FCB645531330200000006645531336455383B040000001E6455383B64553D9F030000001764553D9F64553FBB0400000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264552FCB6455910F0000000000000000000000780000009200000095020C0B0016040464553FBB6455406F02000000036455406F64554303030000000B645543036455433F04000000016455433F6455460F020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264552FCB6455910F0000000000000000000000780000009200000095020C0B001608046455460F645550D7030000002E645550D764555CCB020000003364555CCB64555EAB030000000864555EAB645560130200000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264552FCB6455910F0000000000000000000000780000009200000095020C0B00160C04645560136455635B040000000E6455635B645567CF0200000013645567CF64556A27030000000A64556A2764556CBB020000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264552FCB6455910F0000000000000000000000780000009200000095020C0B0016100464556CBB6455703F040000000F6455703F6455716B02000000056455716B64557D5F030000003364557D5F6455824B0200000015";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264552FCB6455910F0000000000000000000000780000009200000095020C0B001614026455824B64558E3F040000003364558E3F6455910F020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-05-08 20:35:30  -> 11
        else if (idDebug == 11) {
            Log.e(TAG, "2023-05-08 20:35:30 ");
            contentData = "826456E55964575B0100000009000000010000004C0000014800000059020200001500046456E5596456E6C102000000066456E6C16456EC2504000000176456EC256456EE4101000000096456EE416456F111040000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826456E55964575B0100000009000000010000004C0000014800000059020200001504046456F1116456F459020000000E6456F4596456F6B1030000000A6456F6B1645706DD0200000045645706DD64570DA9040000001D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826456E55964575B0100000009000000010000004C00000148000000590202000015080464570DA9645714ED020000001F645714ED64571C31030000001F64571C31645733A10200000064645733A1645735810300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826456E55964575B0100000009000000010000004C000001480000005902020000150C046457358164573815020000000B6457381564573B21020000000D64573B2164574139020000001A6457413964574409040000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826456E55964575B0100000009000000010000004C000001480000005902020000151004645744096457457102000000066457457164574C01030000001C64574C0164574ED1020000000C64574ED1645751A1030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826456E55964575B0100000009000000010000004C000001480000005902020000151401645751A164575B010200000028";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826458105D645887310000000000000000000000600000012200000079020201001600046458105D645811C50200000006645811C56458194504000000206458194564581B9D020000000A64581B9D64581D7D0300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826458105D6458873100000000000000000000006000000122000000790202010016040464581D7D64582755020000002A64582755645828BD0400000006645828BD6458396102000000476458396164583BB9030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826458105D6458873100000000000000000000006000000122000000790202010016080464583BB964583E89040000000C64583E89645842C10200000012645842C16458473504000000136458473564584B6D0200000012";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826458105D64588731000000000000000000000060000001220000007902020100160C0464584B6D64584E01030000000B64584E01645851FD0200000011645851FD645855BD0300000010645855BD645861B10200000033";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826458105D64588731000000000000000000000060000001220000007902020100161004645861B164586841030000001C645868416458738102000000306458738164587651040000000C64587651645881910300000030";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826458105D6458873100000000000000000000006000000122000000790202010016140264588191645883AD0200000009645883AD64588731040000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-05-09 09:32:45  -> 12
        else if (idDebug == 12) {
            Log.e(TAG, "2023-05-09 09:32:45");
            contentData = "826459702E6459DB4A0000000B000000010000005F000000EB00000074020100001700046459702E645971960200000006645971966459801E040000003E6459801E6459858202000000176459858264598816040000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826459702E6459DB4A0000000B000000010000005F000000EB00000074020100001704046459881664598FD2020000002164598FD264599266040000000B6459926664599F4A020000003764599F4A6459A1DE010000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826459702E6459DB4A0000000B000000010000005F000000EB00000074020100001708046459A1DE6459A472040000000B6459A4726459A5DA02000000066459A5DA6459A7F603000000096459A7F66459AEC2020000001D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826459702E6459DB4A0000000B000000010000005F000000EB0000007402010000170C046459AEC26459B156030000000B6459B1566459B4DA020000000F6459B4DA6459BC1E030000001F6459BC1E6459BFA2020000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826459702E6459DB4A0000000B000000010000005F000000EB00000074020100001710046459BFA26459C7D603000000236459C7D66459CAE2020000000D6459CAE26459CDB2030000000C6459CDB26459D046020000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826459702E6459DB4A0000000B000000010000005F000000EB00000074020100001714036459D0466459D26203000000096459D2626459D92E020000001D6459D92E6459DB4A0300000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-05-16 16:18:52  -> 13
        else if (idDebug == 13) {
            Log.e(TAG, "2023-05-16 16:18:52");
            contentData = "82645AB28B645B20EF0000000E000000010000005F000000C1000000A9020900001C0004645AB28B645AB3F30200000006645AB3F3645AB9930400000018645AB993645ABD530200000010645ABD53645AC32F0400000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645AB28B645B20EF0000000E000000010000005F000000C1000000A9020900001C0404645AC32F645AC677010000000E645AC677645ACBDB0200000017645ACBDB645ACDF70300000009645ACDF7645AD2E30400000015";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645AB28B645B20EF0000000E000000010000005F000000C1000000A9020900001C0804645AD2E3645AD4870200000007645AD487645AD8470300000010645AD847645ADBCB020000000F645ADBCB645AE3870300000021";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645AB28B645B20EF0000000E000000010000005F000000C1000000A9020900001C0C04645AE387645AE7470200000010645AE747645AECE70300000018645AECE7645AF0E30200000011645AF0E3645AF42B040000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645AB28B645B20EF0000000E000000010000005F000000C1000000A9020900001C1004645AF42B645AF51B0300000004645AF51B645AF7EB020000000C645AF7EB645AFD130300000016645AFD13645AFE7B0200000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645AB28B645B20EF0000000E000000010000005F000000C1000000A9020900001C1404645AFE7B645B001F0400000007645B001F645B0B23020000002F645B0B23645B131B0300000022645B131B645B15AF020000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645AB28B645B20EF0000000E000000010000005F000000C1000000A9020900001C1804645B15AF645B1C03030000001B645B1C03645B1CF30200000004645B1CF3645B1DE30400000004645B1DE3645B20EF020000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645C0FBD645C7CF500000000000000000000006D0000010B0000005A020901001A0004645C0FBD645C11250200000006645C1125645C19D10400000025645C19D1645C1D19030000000E645C1D19645C2061040000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645C0FBD645C7CF500000000000000000000006D0000010B0000005A020901001A0404645C2061645C263D0200000019645C263D645C28D1030000000B645C28D1645C2EE9020000001A645C2EE9645C2F9D0400000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645C0FBD645C7CF500000000000000000000006D0000010B0000005A020901001A0804645C2F9D645C36A5020000001E645C36A5645C37950400000004645C3795645C3CBD0200000016645C3CBD645C4389030000001D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645C0FBD645C7CF500000000000000000000006D0000010B0000005A020901001A0C04645C4389645C452D0200000007645C452D645C47490400000009645C4749645C4BF90200000014645C4BF9645C4E51040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645C0FBD645C7CF500000000000000000000006D0000010B0000005A020901001A1004645C4E51645C524D0200000011645C524D645C57750400000016645C5775645C5A45020000000C645C5A45645C5C9D040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645C0FBD645C7CF500000000000000000000006D0000010B0000005A020901001A1404645C5C9D645C605D0300000010645C605D645C67A1020000001F645C67A1645C69450300000007645C6945645C6D410200000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645C0FBD645C7CF500000000000000000000006D0000010B0000005A020901001A1802645C6D41645C704D030000000D645C704D645C7CF50200000036";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645D598F645DCE0B0000000000000000000000760000010600000075020902001F0004645D598F645D5AF70200000006645D5AF7645D6583040000002D645D6583645D66370300000003645D6637645D6F970200000028";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645D598F645DCE0B0000000000000000000000760000010600000075020902001F0404645D6F97645D74BF0300000016645D74BF645D77CB020000000D645D77CB645D79AB0300000008645D79AB645D82570200000025";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645D598F645DCE0B0000000000000000000000760000010600000075020902001F0804645D8257645D886F040000001A645D886F645D89230200000003645D8923645D8C6B030000000E645D8C6B645D8DD30200000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645D598F645DCE0B0000000000000000000000760000010600000075020902001F0C04645D8DD3645D902B040000000A645D902B645D90DF0200000003645D90DF645D92BF0300000008645D92BF645D98230200000017";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645D598F645DCE0B0000000000000000000000760000010600000075020902001F1004645D9823645D9B2F040000000D645D9B2F645D9BE30200000003645D9BE3645D9CD30400000004645D9CD3645DA057020000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645D598F645DCE0B0000000000000000000000760000010600000075020902001F1404645DA057645DA363030000000D645DA363645DAF1B0200000032645DAF1B645DB29F040000000F645DB29F645DB92F020000001C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645D598F645DCE0B0000000000000000000000760000010600000075020902001F1804645DB92F645DBB0F0300000008645DBB0F645DBFBF0200000014645DBFBF645DC55F0300000018645DC55F645DC82F020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645D598F645DCE0B0000000000000000000000760000010600000075020902001F1C03645DC82F645DCC2B0300000011645DCC2B645DCCDF0200000003645DCCDF645DCE0B0400000005";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645EB2B9645F2951000000000000000000000080000001110000006902090300180004645EB2B9645EB4210200000006645EB421645EBC190400000022645EBC19645EC8FD0200000037645EC8FD645ECC45040000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645EB2B9645F2951000000000000000000000080000001110000006902090300180404645ECC45645ED16D0200000016645ED16D645ED5A50400000012645ED5A5645ED7490200000007645ED749645EDCAD0300000017";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645EB2B9645F2951000000000000000000000080000001110000006902090300180804645EDCAD645EDD610200000003645EDD61645EE0E5040000000F645EE0E5645EE4A50200000010645EE4A5645EE7B1040000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645EB2B9645F2951000000000000000000000080000001110000006902090300180C04645EE7B1645EEDC9020000001A645EEDC9645EF05D030000000B645EF05D645EF3E1020000000F645EF3E1645EF675030000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645EB2B9645F2951000000000000000000000080000001110000006902090300181004645EF675645EFD41020000001D645EFD41645F06DD0300000029645F06DD645F0B8D0400000014645F0B8D645F199D020000003C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645EB2B9645F2951000000000000000000000080000001110000006902090300181404645F199D645F1E110300000013645F1E11645F1EC50200000003645F1EC5645F220D040000000E645F220D645F2951020000001F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82645FADE0645FBD1C00000000000000000000000E000000270000000C02090400040004645FADE0645FB2900200000014645FB290645FB5D8040000000E645FB5D8645FBA4C0200000013645FBA4C645FBD1C030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646025DF64607A030000000B000000010000004A000000C40000004E02090500190004646025DF6460274702000000066460274764602A53040000000D64602A536460320F03000000216460320F646039CB0200000021";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646025DF64607A030000000B000000010000004A000000C40000004E02090500190404646039CB64603C9B040000000C64603C9B64603D13020000000264603D1364603D8B040000000264603D8B64603E7B0200000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646025DF64607A030000000B000000010000004A000000C40000004E0209050019080464603E7B64603EF3040000000264603EF364603F2F020000000164603F2F64603FE3040000000364603FE364604277010000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646025DF64607A030000000B000000010000004A000000C40000004E02090500190C0464604277646043A30200000005646043A36460479F03000000116460479F64605537020000003A6460553764605AD70400000018";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646025DF64607A030000000B000000010000004A000000C40000004E0209050019100464605AD764605C3F020000000664605C3F64605F4B030000000D64605F4B64606527020000001964606527646066CB0400000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646025DF64607A030000000B000000010000004A000000C40000004E02090500191404646066CB64606F3B020000002464606F3B646072BF030000000F646072BF64607463020000000764607463646076F7040000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646025DF64607A030000000B000000010000004A000000C40000004E02090500191801646076F764607A03020000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646103F66461262E00000000000000000000001B0000004B0000002C020906000A0004646103F66461055E02000000066461055E6461082E040000000C6461082E64610DCE030000001864610DCE646112BA0200000015";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646103F66461262E00000000000000000000001B0000004B0000002C020906000A0404646112BA646114D60400000009646114D664611896020000001064611896646119FE0400000006646119FE64612016020000001A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646103F66461262E00000000000000000000001B0000004B0000002C020906000A080264612016646124C60300000014646124C66461262E0200000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264614B086461B7C800000005000000010000005E000000FC00000071020907001C000464614B0864614C70020000000664614C706461555804000000266461555864615684010000000564615684646166B00200000045";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264614B086461B7C800000005000000010000005E000000FC00000071020907001C0404646166B064616A70030000001064616A7064616DB8020000000E64616DB864617010030000000A6461701064617358020000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264614B086461B7C800000005000000010000005E000000FC00000071020907001C080464617358646175EC030000000B646175EC64617A24020000001264617A2464617C7C040000000A64617C7C646184EC0200000024";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264614B086461B7C800000005000000010000005E000000FC00000071020907001C0C04646184EC64618B40030000001B64618B4064618EC4040000000F64618EC46461902C02000000066461902C646194640300000012";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264614B086461B7C800000005000000010000005E000000FC00000071020907001C10046461946464619734020000000C6461973464619E00040000001D64619E006461A4CC020000001D6461A4CC6461A6700300000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264614B086461B7C800000005000000010000005E000000FC00000071020907001C14046461A6706461A97C020000000D6461A97C6461AB5C03000000086461AB5C6461AE2C020000000C6461AE2C6461AFD00300000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264614B086461B7C800000005000000010000005E000000FC00000071020907001C18046461AFD06461B354020000000F6461B3546461B57003000000096461B5706461B75002000000086461B7506461B7C80400000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826462A12464630790000000000000000000000066000000FD00000052020908001A00046462A1246462A28C02000000066462A28C6462A64C04000000106462A64C6462A70002000000036462A7006462AD90030000001C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826462A12464630790000000000000000000000066000000FD00000052020908001A04046462AD906462B3A8020000001A6462B3A86462B600040000000A6462B6006462B90C020000000D6462B90C6462C2E4040000002A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826462A12464630790000000000000000000000066000000FD00000052020908001A08046462C2E46462CBCC02000000266462CBCC6462CE60040000000B6462CE606462D8B0020000002C6462D8B06462DC700300000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826462A12464630790000000000000000000000066000000FD00000052020908001A0C046462DC706462DF7C020000000D6462DF7C6462E19803000000096462E1986462E24C02000000036462E24C6462E51C040000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826462A12464630790000000000000000000000066000000FD00000052020908001A10046462E51C6462E6C002000000076462E6C06462E954040000000B6462E9546462F0D402000000206462F0D46462F2B40300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826462A12464630790000000000000000000000066000000FD00000052020908001A14046462F2B46462F6B002000000116462F6B06462F944030000000B6462F94464630010020000001D64630010646301F00300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826462A12464630790000000000000000000000066000000FD00000052020908001A1802646301F064630718020000001664630718646307900300000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-05-16 17:28:26  -> 14
        // SIN REGISTROS DE SLEEP

        //2023-05-17 08:31:05  -> 15
        else if (idDebug == 15) {
            Log.e(TAG, "2023-05-17 08:31:05");
            contentData = "826463F22464645AE800000000000000000000004B0000012000000054020100001000046463F2246463F38C02000000066463F38C646401D8040000003D646401D864640868020000001C6464086864640A480400000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826463F22464645AE800000000000000000000004B00000120000000540201000010040464640A48646423D4020000006D646423D4646426A4030000000C646426A464643298020000003364643298646437FC0300000017";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826463F22464645AE800000000000000000000004B000001200000005402010000100804646437FC64643BBC020000001064643BBC64643E14030000000A64643E1464644918020000002F64644918646450980300000020";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826463F22464645AE800000000000000000000004B000001200000005402010000100C0464645098646453E0020000000E646453E0646455840300000007646455846464598002000000116464598064645AE80400000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-05-21 22:56:59  -> 16
        else if (idDebug == 16) {
            Log.e(TAG, "2023-05-21 22:56:59");
            contentData = "8264653DF26465AAB2000000000000000000000053000000D2000000AB0205000018000464653DF264653F5A020000000664653F5A64654DA6040000003D64654DA664654F0E020000000664654F0E6465530A0300000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264653DF26465AAB2000000000000000000000053000000D2000000AB020500001804046465530A646554AE0200000007646554AE64655652040000000764655652646557F60200000007646557F6646561CE030000002A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264653DF26465AAB2000000000000000000000053000000D2000000AB02050000180804646561CE6465689A020000001D6465689A64656C1E040000000F64656C1E64656DFE020000000864656DFE646576AA0300000025";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264653DF26465AAB2000000000000000000000053000000D2000000AB02050000180C04646576AA6465788A02000000086465788A64657B96030000000D64657B966465838E02000000226465838E646585E6030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264653DF26465AAB2000000000000000000000053000000D2000000AB02050000181004646585E664658A1E020000001264658A1E64658ECE030000001464658ECE6465937E02000000146465937E6465964E030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264653DF26465AAB2000000000000000000000053000000D2000000AB020500001814046465964E64659E82020000002364659E826465A116030000000B6465A1166465A89602000000206465A8966465AAB20300000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264663E026466A70200000000000000000000007E000000C70000007B020501001A000464663E0264663F6A020000000664663F6A646642EE040000000F646642EE64664A6E020000002064664A6E6466500E0400000018";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264663E026466A70200000000000000000000007E000000C70000007B020501001A04046466500E646657CA0300000021646657CA64665A5E040000000B64665A5E646661A2020000001F646661A2646664EA040000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264663E026466A70200000000000000000000007E000000C70000007B020501001A0804646664EA646668E60200000011646668E664666C2E040000000E64666C2E646670660200000012646670666466758E0300000016";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264663E026466A70200000000000000000000007E000000C70000007B020501001A0C046466758E6466785E040000000C6466785E64667AB6030000000A64667AB664667B6A020000000364667B6A64667DFE040000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264663E026466A70200000000000000000000007E000000C70000007B020501001A100464667DFE646683DA0200000019646683DA6466875E030000000F6466875E64668A2E040000000C64668A2E6466900A0200000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264663E026466A70200000000000000000000007E000000C70000007B020501001A14046466900A6466938E030000000F6466938E646695AA0200000009646695AA646698B6040000000D646698B664669A1E0200000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264663E026466A70200000000000000000000007E000000C70000007B020501001A180264669A1E6466A0AE030000001C6466A0AE6466A702020000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264679C7E6467FC96000000000000000000000046000000F70000005D0205020011000464679C7E64679DE6020000000664679DE66467A836040000002C6467A8366467ABBA020000000F6467ABBA6467AF7A0300000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264679C7E6467FC96000000000000000000000046000000F70000005D020502001104046467AF7A6467B4A202000000166467B4A26467B64604000000076467B6466467B9CA020000000F6467B9CA6467BEB60300000015";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264679C7E6467FC96000000000000000000000046000000F70000005D020502001108046467BEB66467C76202000000256467C7626467CBD604000000136467CBD66467CE2E020000000A6467CE2E6467D2660300000012";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264679C7E6467FC96000000000000000000000046000000F70000005D02050200110C046467D2666467D69E02000000126467D69E6467DDE2030000001F6467DDE26467EB3E02000000396467EB3E6467ECE20300000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264679C7E6467FC96000000000000000000000046000000F70000005D020502001110016467ECE26467FC960200000043";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264690B0064694DCC0000001E000000010000001B000000AE00000036020503000B000464690B0064690C68020000000664690C68646912BC040000001B646912BC646919C4010000001E646919C464691C1C030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264690B0064694DCC0000001E000000010000001B000000AE00000036020503000B040464691C1C6469239C02000000206469239C6469266C030000000C6469266C64692F54020000002664692F546469347C0300000016";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264690B0064694DCC0000001E000000010000001B000000AE00000036020503000B08036469347C6469387802000000116469387864693AD0030000000A64693AD064694DCC0200000051";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826469DCF16469FFA100000000000000000000001F0000005000000025020504000900046469DCF16469DE5902000000066469DE596469E59D040000001F6469E59D6469EC2D020000001C6469EC2D6469EE85030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826469DCF16469FFA100000000000000000000001F0000005000000025020504000904046469EE856469F02902000000076469F0296469F49D03000000136469F49D6469F64102000000076469F6416469F8210300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826469DCF16469FFA100000000000000000000001F0000005000000025020504000908016469F8216469FFA10200000020";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-06-03 13:22:26  -> 17
        else if (idDebug == 17) {
            Log.e(TAG, "2023-06-03 13:22:26");
            contentData = "82646A3B24646AAA3C000000000000000000000094000000E400000062020D0000210004646A3B24646A3C8C0200000006646A3C8C646A48F80400000035646A48F8646A4CF40200000011646A4CF4646A4F4C040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646A3B24646AAA3C000000000000000000000094000000E400000062020D0000210404646A4F4C646A530C0200000010646A530C646A53840400000002646A5384646A5654020000000C646A5654646A5A140300000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646A3B24646AAA3C000000000000000000000094000000E400000062020D0000210804646A5A14646A5E4C0200000012646A5E4C646A60680300000009646A6068646A65CC0200000017646A65CC646A69C80400000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646A3B24646AAA3C000000000000000000000094000000E400000062020D0000210C04646A69C8646A6D4C030000000F646A6D4C646A701C020000000C646A701C646A70940400000002646A7094646A74900200000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646A3B24646AAA3C000000000000000000000094000000E400000062020D0000211004646A7490646A76340300000007646A7634646A779C0200000006646A779C646A7B20040000000F646A7B20646A7DB4020000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646A3B24646AAA3C000000000000000000000094000000E400000062020D0000211404646A7DB4646A84BC030000001E646A84BC646A89A80200000015646A89A8646A8F840400000019646A8F84646A91A00200000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646A3B24646AAA3C000000000000000000000094000000E400000062020D0000211804646A91A0646A93440400000007646A9344646A94AC0200000006646A94AC646A96C80300000009646A96C8646A98E40200000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646A3B24646AAA3C000000000000000000000094000000E400000062020D0000211C04646A98E4646A9BB4040000000C646A9BB4646A9FEC0200000012646A9FEC646AA2BC030000000C646AA2BC646AA910020000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646A3B24646AAA3C000000000000000000000094000000E400000062020D0000212001646AA910646AAA3C0400000005";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646BDE8B646C49E300000039000000010000004A000000C100000086020D01000F0004646BDE8B646BDFF30200000006646BDFF3646BF14B040000004A646BF14B646BFEA70100000039646BFEA7646C00FF030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646BDE8B646C49E300000039000000010000004A000000C100000086020D01000F0404646C00FF646C15630200000057646C1563646C1833030000000C646C1833646C1C2F0200000011646C1C2F646C2247030000001A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646BDE8B646C49E300000039000000010000004A000000C100000086020D01000F0804646C2247646C2553020000000D646C2553646C26F70300000007646C26F7646C2A03020000000D646C2A03646C339F0300000029";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646BDE8B646C49E300000039000000010000004A000000C100000086020D01000F0C03646C339F646C3633020000000B646C3633646C3F1B0300000026646C3F1B646C49E3020000002E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646D2908646D9898000000000000000000000067000000F200000083020D0200160004646D2908646D2A700200000006646D2A70646D3880040000003C646D3880646D3CB80200000012646D3CB8646D3CF40400000001";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646D2908646D9898000000000000000000000067000000F200000083020D0200160404646D3CF4646D421C0200000016646D421C646D45DC0300000010646D45DC646D499C0200000010646D499C646D4A500300000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646D2908646D9898000000000000000000000067000000F200000083020D0200160804646D4A50646D4D98040000000E646D4D98646D55180300000020646D5518646D5F2C020000002B646D5F2C646D62B0040000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646D2908646D9898000000000000000000000067000000F200000083020D0200160C04646D62B0646D688C0300000019646D688C646D69F40200000006646D69F4646D6D00040000000D646D6D00646D7B88020000003E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646D2908646D9898000000000000000000000067000000F200000083020D0200161004646D7B88646D7C780300000004646D7C78646D7DE00200000006646D7DE0646D8434030000001B646D8434646D886C0200000012";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646D2908646D9898000000000000000000000067000000F200000083020D0200161402646D886C646D8E0C0300000018646D8E0C646D9898020000002D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646E7C99646EE125000000000000000000000070000000E900000054020D03001B0004646E7C99646E7E010200000006646E7E01646E85F90400000022646E85F9646E8AE50200000015646E8AE5646E8DF1030000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646E7C99646EE125000000000000000000000070000000E900000054020D03001B0404646E8DF1646E8F590200000006646E8F59646E92DD040000000F646E92DD646E996D020000001C646E996D646E9B4D0300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646E7C99646EE125000000000000000000000070000000E900000054020D03001B0804646E9B4D646E9F490200000011646E9F49646EA291040000000E646EA291646EA4350200000007646EA435646EA6C9040000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646E7C99646EE125000000000000000000000070000000E900000054020D03001B0C04646EA6C9646EA8310200000006646EA831646EAD950300000017646EAD95646EB2090200000013646EB209646EB4250400000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646E7C99646EE125000000000000000000000070000000E900000054020D03001B1004646EB425646EB4D90200000003646EB4D9646EB58D0400000003646EB58D646EB76D0200000008646EB76D646EB7A90400000001";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646E7C99646EE125000000000000000000000070000000E900000054020D03001B1404646EB7A9646EBF290200000020646EBF29646EC1090300000008646EC109646EC75D020000001B646EC75D646ECEDD0300000020";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646E7C99646EE125000000000000000000000070000000E900000054020D03001B1803646ECEDD646ED135040000000A646ED135646EDDA10200000035646EDDA1646EE125040000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646FCA7F647037F300000000000000000000004F0000013200000052020D0400140004646FCA7F646FCBE70200000006646FCBE7646FD0970400000014646FD097646FD2770200000008646FD277646FD547040000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646FCA7F647037F300000000000000000000004F0000013200000052020D0400140404646FD547646FDA330200000015646FDA33646FDC8B030000000A646FDC8B646FE1EF0200000017646FE1EF646FE573030000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646FCA7F647037F300000000000000000000004F0000013200000052020D0400140804646FE573646FF30B020000003A646FF30B646FF923030000001A646FF92364700067020000001F64700067647002470400000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646FCA7F647037F300000000000000000000004F0000013200000052020D0400140C04647002476470067F02000000126470067F647009C7040000000E647009C764700C97020000000C64700C9764700E770400000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82646FCA7F647037F300000000000000000000004F0000013200000052020D040014100464700E7764701EA3020000004564701EA3647025E7030000001F647025E7647033F7020000003C647033F7647037F30400000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264712D2664719D2E00000000000000000000006D000000C0000000B1020D05001D000464712D2664712E8E020000000664712E8E647134A6040000001A647134A6647136C20200000009647136C264713C260400000017";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264712D2664719D2E00000000000000000000006D000000C0000000B1020D05001D040464713C266471405E02000000126471405E6471441E03000000106471441E647144D20200000003647144D264714C16040000001F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264712D2664719D2E00000000000000000000006D000000C0000000B1020D05001D080464714C1664714E32020000000964714E326471517A040000000E6471517A647154FE020000000F647154FE64715882030000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264712D2664719D2E00000000000000000000006D000000C0000000B1020D05001D0C046471588264715CBA020000001264715CBA64715F8A040000000C64715F8A647163860200000011647163866471706A0300000037";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264712D2664719D2E00000000000000000000006D000000C0000000B1020D05001D10046471706A647170E20200000002647170E26471759203000000146471759264717D8A020000002264717D8A64717DC60400000001";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264712D2664719D2E00000000000000000000006D000000C0000000B1020D05001D140464717DC6647181FE0200000012647181FE647183A20300000007647183A2647186EA020000000E647186EA64718FD20300000026";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264712D2664719D2E00000000000000000000006D000000C0000000B1020D05001D180464718FD264719662020000001C6471966264719B8A030000001664719B8A64719BC6020000000164719BC664719CB60300000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264712D2664719D2E00000000000000000000006D000000C0000000B1020D05001D1C0164719CB664719D2E0400000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647290326472DDC60000000C0000000100000030000000AB00000064020D0600140004647290326472919A02000000066472919A647293F2040000000A647293F26472964A020000000A6472964A64729F320400000026";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647290326472DDC60000000C0000000100000030000000AB00000064020D060014040464729F326472A09A02000000066472A09A6472A3A6030000000D6472A3A66472AAEA020000001F6472AAEA6472AC8E0300000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647290326472DDC60000000C0000000100000030000000AB00000064020D06001408046472AC8E6472B31E020000001C6472B31E6472B6A2030000000F6472B6A26472BB1602000000136472BB166472BE9A030000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647290326472DDC60000000C0000000100000030000000AB00000064020D0600140C046472BE9A6472C52A020000001C6472C52A6472C74603000000096472C7466472C99E020000000A6472C99E6472CC6E010000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647290326472DDC60000000C0000000100000030000000AB00000064020D06001410046472CC6E6472D3B2030000001F6472D3B26472D6BE020000000D6472D6BE6472D916030000000A6472D9166472DDC60200000014";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264736F3F64737DC700000000000000000000000F0000001900000016020D070005000464736F3F647370A70200000006647370A76473742B040000000F6473742B647377AF020000000F647377AF64737CD70300000016";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264736F3F64737DC700000000000000000000000F0000001900000016020D070005040164737CD764737DC70200000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264750E9F64757D7B00000000000000000000006E000000DB00000090020D080019000464750E9F64751007020000000664751007647523B70400000054647523B76475246B02000000036475246B6475264B0300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264750E9F64757D7B00000000000000000000006E000000DB00000090020D08001904046475264B647532B70200000035647532B7647536EF0400000012647536EF64753857020000000664753857647539FB0300000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264750E9F64757D7B00000000000000000000006E000000DB00000090020D0800190804647539FB64753DF7020000001164753DF76475422F03000000126475422F64754E5F020000003464754E5F647553FF0300000018";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264750E9F64757D7B00000000000000000000006E000000DB00000090020D0800190C04647553FF6475592702000000166475592764755ACB030000000764755ACB64755CE7020000000964755CE764756377030000001C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264750E9F64757D7B00000000000000000000006E000000DB00000090020D0800191004647563776475655702000000086475655764756863030000000D647568636475691702000000036475691764756AF70400000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264750E9F64757D7B00000000000000000000006E000000DB00000090020D080019140464756AF764756EB7020000001064756EB76475710F030000000A6475710F64757493020000000F6475749364757B5F030000001D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264750E9F64757D7B00000000000000000000006E000000DB00000090020D080019180164757B5F64757D7B0200000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647668516476C035000000000000000000000068000000BE00000051020D09001B000464766851647669B90200000006647669B964766F1D040000001764766F1D64767445020000001664767445647679310400000015";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647668516476C035000000000000000000000068000000BE00000051020D09001B04046476793164767CF1030000001064767CF164767FFD020000000D64767FFD647682CD030000000C647682CD6476859D020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647668516476C035000000000000000000000068000000BE00000051020D09001B08046476859D647688E5040000000E647688E564769029020000001F64769029647693AD030000000F647693AD647696B9020000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647668516476C035000000000000000000000068000000BE00000051020D09001B0C04647696B9647698D50300000009647698D564769D49020000001364769D496476A0CD040000000F6476A0CD6476A5410200000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647668516476C035000000000000000000000068000000BE00000051020D09001B10046476A5416476A6A904000000066476A6A96476AB9502000000156476AB956476AD7504000000086476AD756476AE290200000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647668516476C035000000000000000000000068000000BE00000051020D09001B14046476AE296476B2D903000000146476B2D96476B47D02000000076476B47D6476B7C5040000000E6476B7C56476BB850200000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647668516476C035000000000000000000000068000000BE00000051020D09001B18036476BB856476BDA103000000096476BDA16476BF8102000000086476BF816476C0350400000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826477B80664781F9E000000000000000000000050000001000000006A020D0A001700046477B8066477B96E02000000066477B96E6477BDE204000000136477BDE26477C3BE03000000196477C3BE6477C59E0200000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826477B80664781F9E000000000000000000000050000001000000006A020D0A001704046477C59E6477C8E6040000000E6477C8E66477CB0202000000096477CB026477D28203000000206477D2826477E48E020000004D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826477B80664781F9E000000000000000000000050000001000000006A020D0A001708046477E48E6477E66E04000000086477E66E6477E81202000000076477E8126477EC4A03000000126477EC4A6477EDB20200000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826477B80664781F9E000000000000000000000050000001000000006A020D0A00170C046477EDB26477F1AE04000000116477F1AE6477F92E02000000206477F92E6477FC76030000000E6477FC766477FDDE0200000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826477B80664781F9E000000000000000000000050000001000000006A020D0A001710046477FDDE6477FFBE04000000086477FFBE6478118E020000004C6478118E64781422040000000B647814226478190E0200000015";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826477B80664781F9E000000000000000000000050000001000000006A020D0A001714036478190E64781D0A030000001164781D0A64781EEA020000000864781EEA64781F9E0400000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826479037864796C78000000000000000000000083000000DD00000060020D0B0019000464790378647904E00200000006647904E064790B34040000001B64790B3464790FE4030000001464790FE46479132C020000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826479037864796C78000000000000000000000083000000DD00000060020D0B001904046479132C647916EC0300000010647916EC64791B9C020000001464791B9C64791DB8030000000964791DB86479213C040000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826479037864796C78000000000000000000000083000000DD00000060020D0B001908046479213C647923D0030000000B647923D0647926A0020000000C647926A064792A24030000000F64792A246479330C0400000026";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826479037864796C78000000000000000000000083000000DD00000060020D0B00190C046479330C647938AC0200000018647938AC64793B7C030000000C64793B7C647945CC020000002C647945CC647949C80400000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826479037864796C78000000000000000000000083000000DD00000060020D0B00191004647949C86479501C020000001B6479501C647952B0030000000B647952B064795544020000000B64795544647959F40400000014";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826479037864796C78000000000000000000000083000000DD00000060020D0B00191404647959F4647962DC0200000026647962DC64796534040000000A64796534647965AC0300000002647965AC64796B880200000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826479037864796C78000000000000000000000083000000DD00000060020D0B0019180164796B8864796C780400000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647A6C83647AD72700000000000000000000005D0000010500000065020D0C00180004647A6C83647A6DEB0200000006647A6DEB647A7B83040000003A647A7B83647A824F020000001D647A824F647A855B030000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647A6C83647AD72700000000000000000000005D0000010500000065020D0C00180404647A855B647A8B73020000001A647A8B73647A8E43040000000C647A8E43647A8EF70300000003647A8EF7647A92F30200000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647A6C83647AD72700000000000000000000005D0000010500000065020D0C00180804647A92F3647A990B030000001A647A990B647A9B63020000000A647A9B63647A9D7F0400000009647A9D7F647AA5770200000022";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647A6C83647AD72700000000000000000000005D0000010500000065020D0C00180C04647AA577647AA7CF030000000A647AA7CF647AB1A7020000002A647AB1A7647AB1E30300000001647AB1E3647AB52B040000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647A6C83647AD72700000000000000000000005D0000010500000065020D0C00181004647AB52B647ABD9B0200000024647ABD9B647ABF3F0300000007647ABF3F647AC20F020000000C647AC20F647AC6470300000012";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647A6C83647AD72700000000000000000000005D0000010500000065020D0C00181404647AC647647ACCD7020000001C647ACCD7647ACF6B030000000B647ACF6B647AD4570200000015647AD457647AD727030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-06-05 08:50:18  -> 18
        else if (idDebug == 18) {
            Log.e(TAG, "2023-06-05 08:50:18");
            contentData = "82647BEA55647C3555000000000000000000000045000000B00000004B02030000110004647BEA55647BEBBD0200000006647BEBBD647BED9D0400000008647BED9D647BEE510200000003647BEE51647BF3790300000016";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647BEA55647C3555000000000000000000000045000000B00000004B02030000110404647BF379647C00990200000038647C0099647C04950300000011647C0495647C0A350200000018647C0A35647C0C8D040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647BEA55647C3555000000000000000000000045000000B00000004B02030000110804647C0C8D647C0EA90200000009647C0EA9647C1179040000000C647C1179647C13D1020000000A647C13D1647C16DD040000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647BEA55647C3555000000000000000000000045000000B00000004B02030000110C04647C16DD647C1F890200000025647C1F89647C25A1040000001A647C25A1647C28E9020000000E647C28E9647C31590300000024";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647BEA55647C3555000000000000000000000045000000B00000004B02030000111001647C3159647C35550200000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647C9B91647CB1D500000000000000000000001C000000350000000E02030100050004647C9B91647C9CF90200000006647C9CF9647CA389040000001C647CA389647CA61D020000000B647CA61D647CA965030000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647C9B91647CB1D500000000000000000000001C000000350000000E02030100050401647CA965647CB1D50200000024";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647D074E647D6F5E00000016000000010000007D00000083000000A6020302001C0004647D074E647D08B60200000006647D08B6647D10EA0400000023647D10EA647D11620300000002647D1162647D146E020000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647D074E647D6F5E00000016000000010000007D00000083000000A6020302001C0404647D146E647D19960100000016647D1996647D1EBE0200000016647D1EBE647D218E030000000C647D218E647D2512040000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647D074E647D6F5E00000016000000010000007D00000083000000A6020302001C0804647D2512647D2896030000000F647D2896647D2AB20400000009647D2AB2647D2EAE0200000011647D2EAE647D326E0300000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647D074E647D6F5E00000016000000010000007D00000083000000A6020302001C0C04647D326E647D3502040000000B647D3502647D393A0300000012647D393A647D4042020000001E647D4042647D44020300000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647D074E647D6F5E00000016000000010000007D00000083000000A6020302001C1004647D4402647D4786040000000F647D4786647D4D9E030000001A647D4D9E647D5032020000000B647D5032647D564A030000001A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647D074E647D6F5E00000016000000010000007D00000083000000A6020302001C1404647D564A647D59CE020000000F647D59CE647D5E420400000013647D5E42647D5EF60200000003647D5EF6647D64D20300000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647D074E647D6F5E00000016000000010000007D00000083000000A6020302001C1804647D64D2647D681A040000000E647D681A647D6A72030000000A647D6A72647D6DBA020000000E647D6DBA647D6F5E0400000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-06-06 09:19:07  -> 19
        else if (idDebug == 19) {
            Log.e(TAG, "2023-06-06 09:19:07 ");
            contentData = "82647E5212647EC346000000050000000100000082000000C600000096020100001E0004647E5212647E537A0200000006647E537A647E5A82040000001E647E5A82647E5B360200000003647E5B36647E614E030000001A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647E5212647EC346000000050000000100000082000000C600000096020100001E0404647E614E647E627A0200000005647E627A647E654A040000000C647E654A647E66B20200000006647E66B2647E66EE0300000001";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647E5212647EC346000000050000000100000082000000C600000096020100001E0804647E66EE647E67DE0200000004647E67DE647E68CE0300000004647E68CE647E6AEA0400000009647E6AEA647E7102020000001A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647E5212647EC346000000050000000100000082000000C600000096020100001E0C04647E7102647E7486040000000F647E7486647E7C7E0200000022647E7C7E647E80F20400000013647E80F2647E8386030000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647E5212647EC346000000050000000100000082000000C600000096020100001E1004647E8386647E861A040000000B647E861A647E87BE0200000007647E87BE647E8E12030000001B647E8E12647E9556020000001F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647E5212647EC346000000050000000100000082000000C600000096020100001E1404647E9556647EA3DE030000003E647EA3DE647EA672020000000B647EA672647EA9BA040000000E647EA9BA647EAE6A0200000014";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647E5212647EC346000000050000000100000082000000C600000096020100001E1804647EAE6A647EAF960100000005647EAF96647EB3560400000010647EB356647EB9320200000019647EB932647EBDA60300000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647E5212647EC346000000050000000100000082000000C600000096020100001E1C02647EBDA6647EC2560200000014647EC256647EC3460400000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }

        //2023-06-27 19:54:27 Download
        else if (idDebug == 20) {
            Log.e(TAG, "2023-06-27 19:54:27 Download");
            contentData = "82647F9DA56480079500000014000000030000003F0000010E00000063021700001A0004647F9DA5647F9F0D0200000006647F9F0D647FA4AD0400000018647FA4AD647FA86D0200000010647FA86D647FABF1030000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647F9DA56480079500000014000000030000003F0000010E00000063021700001A0404647FABF1647FB3AD0200000021647FB3AD647FBB690300000021647FBB69647FBC1D0200000003647FBC1D647FBE390400000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647F9DA56480079500000014000000030000003F0000010E00000063021700001A0804647FBE39647FBEED0200000003647FBEED647FC1F9030000000D647FC1F9647FC4150200000009647FC415647FC9B50400000018";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647F9DA56480079500000014000000030000003F0000010E00000063021700001A0C04647FC9B5647FCEA10200000015647FCEA1647FD0BD0300000009647FD0BD647FE701020000005F647FE701647FECDD0300000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647F9DA56480079500000014000000030000003F0000010E00000063021700001A1004647FECDD647FF7E1020000002F647FF7E1647FF8D10400000004647FF8D1647FF90D0100000001647FF90D647FF9490400000001";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647F9DA56480079500000014000000030000003F0000010E00000063021700001A1404647FF949647FFAB10100000006647FFAB1647FFD09020000000A647FFD09647FFD450400000001647FFD4564800051010000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82647F9DA56480079500000014000000030000003F0000010E00000063021700001A1802648000516480014103000000046480014164800795020000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826480EDEE64815D0600000000000000000000005D00000089000000F4021701001900046480EDEE6480EF5602000000066480EF566480FDA2040000003D6480FDA26481046E020000001D6481046E648108E20300000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826480EDEE64815D0600000000000000000000005D00000089000000F402170100190404648108E264810B3A040000000A64810B3A64810F36030000001164810F3664810FAE020000000264810FAE64811602030000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826480EDEE64815D0600000000000000000000005D00000089000000F402170100190404648108E264810B3A040000000A64810B3A64810F36030000001164810F3664810FAE020000000264810FAE64811602030000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826480EDEE64815D0600000000000000000000005D00000089000000F402170100190404648108E264810B3A040000000A64810B3A64810F36030000001164810F3664810FAE020000000264810FAE64811602030000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826480EDEE64815D0600000000000000000000005D00000089000000F402170100190804648116026481176A02000000066481176A6481198604000000096481198664812412030000002D648124126481271E020000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826480EDEE64815D0600000000000000000000005D00000089000000F402170100190C046481271E64812976030000000A6481297664812CFA020000000F64812CFA6481316E03000000136481316E6481343E020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826480EDEE64815D0600000000000000000000005D00000089000000F4021701001910046481343E6481356A04000000056481356A648136960200000005648136966481455A030000003F6481455A648148DE020000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826480EDEE64815D0600000000000000000000005D00000089000000F402170100191404648148DE64814CDA030000001164814CDA64814EBA040000000864814EBA6481554A020000001C6481554A64815B9E030000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826480EDEE64815D0600000000000000000000005D00000089000000F40217010019180164815B9E64815D060200000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264824E8E6482A4560000000E000000010000004B000000DF000000360217020015000464824E8E64824FF6020000000664824FF6648255960400000018648255966482564A02000000036482564A64825992030000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264824E8E6482A4560000000E000000010000004B000000DF00000036021702001504046482599264825CDA040000000E64825CDA64825D16030000000164825D166482663A02000000276482663A648272A60200000035";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264824E8E6482A4560000000E000000010000004B000000DF0000003602170200150804648272A664827576030000000C64827576648279AE0400000012648279AE64827F8A020000001964827F8A648284EE0200000017";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264824E8E6482A4560000000E000000010000004B000000DF0000003602170200150C04648284EE64828CE6020000002264828CE66482902E010000000E6482902E6482933A030000000D6482933A6482998E020000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264824E8E6482A4560000000E000000010000004B000000DF00000036021702001510046482998E64829A7E030000000464829A7E64829D4E040000000C64829D4E64829FA6030000000A64829FA66482A2B2020000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264824E8E6482A4560000000E000000010000004B000000DF00000036021702001514016482A2B26482A4560400000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826483432A6483B54E0000001B000000010000005C000001050000006B021703001300046483432A6483449202000000066483449264835806040000005364835806648363460200000030648363466483659E030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826483432A6483B54E0000001B000000010000005C000001050000006B021703001304046483659E64836D96020000002264836D9664837336030000001864837336648377AA0200000013648377AA648379C60400000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826483432A6483B54E0000001B000000010000005C000001050000006B02170300130804648379C664837D0E020000000E64837D0E6483839E030000001C6483839E648389F2010000001B648389F2648391720200000020";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826483432A6483B54E0000001B000000010000005C000001050000006B02170300130C0464839172648395E60300000013648395E664839E92020000002564839E926483A03603000000076483A0366483AAC2020000002D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826483432A6483B54E0000001B000000010000005C000001050000006B021703001310036483AAC26483AD1A030000000A6483AD1A6483B332020000001A6483B3326483B54E0300000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826484B2DE64851AEE00000000000000000000005C000000EF00000071021704001B00046484B2DE6484B44602000000066484B4466484B6DA040000000B6484B6DA6484BA5E020000000F6484BA5E6484C472040000002B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826484B2DE64851AEE00000000000000000000005C000000EF00000071021704001B04046484C4726484C4AE03000000016484C4AE6484C92202000000136484C9226484CE0E03000000156484CE0E6484DAF20200000037";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826484B2DE64851AEE00000000000000000000005C000000EF00000071021704001B08046484DAF26484E0CE03000000196484E0CE6484E66E02000000186484E66E6484E6E604000000026484E6E66484E79A0200000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826484B2DE64851AEE00000000000000000000005C000000EF00000071021704001B0C046484E79A6484E90204000000066484E9026484EA6A02000000066484EA6A6484EC4A03000000086484EC4A6484EF1A020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826484B2DE64851AEE00000000000000000000005C000000EF00000071021704001B10046484EF1A6484F0BE03000000076484F0BE6484F5E602000000166484F5E66484F92E030000000E6484F92E64850036020000001E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826484B2DE64851AEE00000000000000000000005C000000EF00000071021704001B140464850036648503BA030000000F648503BA6485064E020000000B6485064E64850B76030000001664850B7664850D1A0200000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826484B2DE64851AEE00000000000000000000005C000000EF00000071021704001B180364850D1A64851332040000001A64851332648519FE020000001D648519FE64851AEE0400000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264857D9F64858FE700000000000000000000002600000012000000160217050005000464857D9F64857F07020000000664857F07648587EF0400000026648587EF64858A0B020000000964858A0B64858F330300000016";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264857D9F64858FE700000000000000000000002600000012000000160217050005040164858F3364858FE70200000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826485E39764864CD3000000000000000000000068000000F600000063021706001100046485E3976485E4FF02000000066485E4FF6485ED6F04000000246485ED6F6485F16B02000000116485F16B6485F7BF040000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826485E39764864CD3000000000000000000000068000000F600000063021706001104046485F7BF6485FBBB02000000116485FBBB6485FF03040000000E6485FF03648601D3020000000C648601D3648607370400000017";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826485E39764864CD3000000000000000000000068000000F600000063021706001108046486073764860B33020000001164860B3364860C23040000000464860C23648629AB020000007E648629AB648633FB030000002C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826485E39764864CD3000000000000000000000068000000F60000006302170600110C04648633FB6486377F020000000F6486377F6486395F03000000086486395F64863C6B020000000D64863C6B6486476F030000002F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826485E39764864CD3000000000000000000000068000000F600000063021706001110016486476F64864CD30200000017";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826487317764879C57000000000000000000000064000000B1000000B3021707001D000464873177648732DF0200000006648732DF648739AB040000001D648739AB64873B8B020000000864873B8B64873E1F040000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826487317764879C57000000000000000000000064000000B1000000B3021707001D040464873E1F64874077020000000A6487407764874437030000001064874437648744EB0200000003648744EB64874743040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826487317764879C57000000000000000000000064000000B1000000B3021707001D080464874743648748AB0200000006648748AB64874CA7030000001164874CA764874F77020000000C64874F77648751CF030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826487317764879C57000000000000000000000064000000B1000000B3021707001D0C04648751CF64875283020000000364875283648755CB040000000E648755CB64875A03020000001264875A03648761830300000020";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826487317764879C57000000000000000000000064000000B1000000B3021707001D100464876183648766AB0200000016648766AB64877083030000002A64877083648772DB020000000A648772DB6487787B0400000018";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826487317764879C57000000000000000000000064000000B1000000B3021707001D14046487787B64877E1B020000001864877E1B6487855F030000001F6487855F64878A4B020000001564878A4B64878B3B0300000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826487317764879C57000000000000000000000064000000B1000000B3021707001D180464878B3B64879027020000001564879027648793AB030000000F648793AB6487967B040000000C6487967B6487994B030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826487317764879C57000000000000000000000064000000B1000000B3021707001D1C016487994B64879C57020000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264888B076488F407000000000000000000000061000000E20000007D0217080020000464888B0764888C6F020000000664888C6F6488911F04000000146488911F64889377020000000A64889377648894DF0300000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264888B076488F407000000000000000000000061000000E20000007D02170800200404648894DF648899530200000013648899536488A763030000003C6488A7636488AAAB020000000E6488AAAB6488B177030000001D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264888B076488F407000000000000000000000061000000E20000007D021708002008046488B1776488B78F020000001A6488B78F6488B84304000000036488B8436488B93302000000046488B9336488B9E70400000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264888B076488F407000000000000000000000061000000E20000007D02170800200C046488B9E76488C16702000000206488C1676488C25704000000046488C2576488C38302000000056488C3836488C6CB040000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264888B076488F407000000000000000000000061000000E20000007D021708002010046488C6CB6488C8E702000000096488C8E76488CA8B04000000076488CA8B6488CBF302000000066488CBF36488CE4B030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264888B076488F407000000000000000000000061000000E20000007D021708002014046488CE4B6488D2BF02000000136488D2BF6488D73304000000136488D7336488D8D702000000076488D8D76488DBE3030000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264888B076488F407000000000000000000000061000000E20000007D021708002018046488DBE36488E3DB02000000226488E3DB6488E57F03000000076488E57F6488E72302000000076488E7236488E9F3040000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264888B076488F407000000000000000000000061000000E20000007D02170800201C046488E9F36488EA6B02000000026488EA6B6488ED77040000000D6488ED776488F38F020000001A6488F38F6488F4070400000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826489D925648A3EA100000000000000000000006F000000E000000062021709001800046489D9256489DA8D02000000066489DA8D6489E42904000000296489E4296489E9C902000000186489E9C96489ED890400000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826489D925648A3EA100000000000000000000006F000000E000000062021709001804046489ED896489F1FD02000000136489F1FD6489F67104000000136489F6716489FDB5020000001F6489FDB5648A02DD0400000016";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826489D925648A3EA100000000000000000000006F000000E00000006202170900180804648A02DD648A04810200000007648A0481648A06250300000007648A0625648A08F5020000000C648A08F5648A0C01030000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826489D925648A3EA100000000000000000000006F000000E00000006202170900180C04648A0C01648A0F0D020000000D648A0F0D648A13F90300000015648A13F9648A15610200000006648A1561648A186D040000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826489D925648A3EA100000000000000000000006F000000E00000006202170900181004648A186D648A1D1D0200000014648A1D1D648A1DD10300000003648A1DD1648A20A1020000000C648A20A1648A27A9030000001E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826489D925648A3EA100000000000000000000006F000000E00000006202170900181404648A27A9648A2AF1020000000E648A2AF1648A2DC1030000000C648A2DC1648A3BD1020000003C648A3BD1648A3EA1030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648B37CC648B94D8000000000000000000000057000000D40000006202170A00170004648B37CC648B39340200000006648B3934648B3F88040000001B648B3F88648B4BF40200000035648B4BF4648B511C0400000016";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648B37CC648B94D8000000000000000000000057000000D40000006202170A00170404648B511C648B51D00200000003648B51D0648B52840400000003648B5284648B53EC0200000006648B53EC648B56BC040000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648B37CC648B94D8000000000000000000000057000000D40000006202170A00170804648B56BC648B5914020000000A648B5914648B5C20040000000D648B5C20648B64180200000022648B6418648B6670040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648B37CC648B94D8000000000000000000000057000000D40000006202170A00170C04648B6670648B73900200000038648B7390648B75AC0300000009648B75AC648B7840020000000B648B7840648B8650030000003C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648B37CC648B94D8000000000000000000000057000000D40000006202170A00171004648B8650648B8920020000000C648B8920648B8D580300000012648B8D58648B91540200000011648B9154648B92080300000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648B37CC648B94D8000000000000000000000057000000D40000006202170A00171403648B9208648B92800200000002648B9280648B94600300000008648B9460648B94D80200000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648CA49D648CEBDD0000003800000001000000410000008F0000002802170B000E0004648CA49D648CA6050200000006648CA605648CAFDD040000002A648CAFDD648CB235020000000A648CB235648CB57D030000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648CA49D648CEBDD0000003800000001000000410000008F0000002802170B000E0404648CB57D648CB84D040000000C648CB84D648CBB95030000000E648CBB95648CCD65020000004C648CCD65648CDA850100000038";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648CA49D648CEBDD0000003800000001000000410000008F0000002802170B000E0804648CDA85648CDFAD0200000016648CDFAD648CE27D030000000C648CE27D648CE4210200000007648CE421648CE63D0400000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648CA49D648CEBDD0000003800000001000000410000008F0000002802170B000E0C02648CE63D648CEB650200000016648CEB65648CEBDD0400000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648DF4BC648E509C000000000000000000000052000000A40000009202170C00170004648DF4BC648DF6240200000006648DF624648E02900400000035648E0290648E07040200000013648E0704648E0B000300000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648DF4BC648E509C000000000000000000000052000000A40000009202170C00170404648E0B00648E0E48020000000E648E0E48648E10A0040000000A648E10A0648E19C40300000027648E19C4648E1BA40200000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648DF4BC648E509C000000000000000000000052000000A40000009202170C00170804648E1BA4648E1DC00300000009648E1DC0648E20CC020000000D648E20CC648E21440400000002648E2144648E21F80200000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648DF4BC648E509C000000000000000000000052000000A40000009202170C00170C04648E21F8648E23D80400000008648E23D8648E28100200000012648E2810648E29B40400000007648E29B4648E3404020000002C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648DF4BC648E509C000000000000000000000052000000A40000009202170C00171004648E3404648E3F440300000030648E3F44648E43400200000011648E4340648E47B40300000013648E47B4648E49940200000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648DF4BC648E509C000000000000000000000052000000A40000009202170C00171403648E4994648E4CDC030000000E648E4CDC648E5024020000000E648E5024648E509C0400000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648F2F67648F865B0000000000000000000000470000009A0000009202170D00150004648F2F67648F30CF0200000006648F30CF648F3813040000001F648F3813648F38C70200000003648F38C7648F3DB30300000015";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648F2F67648F865B0000000000000000000000470000009A0000009202170D00150404648F3DB3648F45330200000020648F4533648F478B030000000A648F478B648F4CEF0200000017648F4CEF648F5037040000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648F2F67648F865B0000000000000000000000470000009A0000009202170D00150804648F5037648F609F0300000046648F609F648F63E7020000000E648F63E7648F66030300000009648F6603648F6C1B040000001A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648F2F67648F865B0000000000000000000000470000009A0000009202170D00150C04648F6C1B648F6D830200000006648F6D83648F6F9F0300000009648F6F9F648F72AB020000000D648F72AB648F757B030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648F2F67648F865B0000000000000000000000470000009A0000009202170D00151004648F757B648F7C83020000001E648F7C83648F7F8F030000000D648F7F8F648F83C70200000012648F83C7648F843F0300000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82648F2F67648F865B0000000000000000000000470000009A0000009202170D00151401648F843F648F865B0200000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649075096490DFE9000000000000000000000079000000F10000005E02170E001B0004649075096490767102000000066490767164907AE5040000001364907AE5649083CD0200000026649083CD6490869D030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649075096490DFE9000000000000000000000079000000F10000005E02170E001B04046490869D649089E5040000000E649089E564908C79020000000B64908C796490925503000000196490925564909B790200000027";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649075096490DFE9000000000000000000000079000000F10000005E02170E001B080464909B796490A11903000000186490A1196490A51502000000116490A5156490A8D504000000106490A8D56490AC59020000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649075096490DFE9000000000000000000000079000000F10000005E02170E001B0C046490AC596490B01904000000106490B0196490B54102000000166490B5416490B84D030000000D6490B84D6490B9F10200000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649075096490DFE9000000000000000000000079000000F10000005E02170E001B10046490B9F16490BC49040000000A6490BC496490BE6502000000096490BE656490C08104000000096490C0816490C2610200000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649075096490DFE9000000000000000000000079000000F10000005E02170E001B14046490C2616490C78904000000166490C7896490C96902000000086490C9696490CC75030000000D6490CC756490D8E10200000035";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649075096490DFE9000000000000000000000079000000F10000005E02170E001B18036490D8E16490DA8503000000076490DA856490DC6502000000086490DC656490DFE9040000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826491BE1E64922016000000060000000100000051000000CF0000007C02170F001A00046491BE1E6491BF8602000000066491BF866491C1DE040000000A6491C1DE6491C472030000000B6491C4726491C7F6040000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826491BE1E64922016000000060000000100000051000000CF0000007C02170F001A04046491C7F66491CCE202000000156491CCE26491CE8603000000076491CE866491D33602000000146491D3366491D49E0400000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826491BE1E64922016000000060000000100000051000000CF0000007C02170F001A08046491D49E6491D60601000000066491D6066491DB6A03000000176491DB6A6491DD0E02000000076491DD0E6491E1820400000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826491BE1E64922016000000060000000100000051000000CF0000007C02170F001A0C046491E1826491E23603000000036491E2366491E7D602000000186491E7D66491E9F203000000096491E9F26491EC0E0200000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826491BE1E64922016000000060000000100000051000000CF0000007C02170F001A0C046491E1826491E23603000000036491E2366491E7D602000000186491E7D66491E9F203000000096491E9F26491EC0E0200000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826491BE1E64922016000000060000000100000051000000CF0000007C02170F001A0C046491E1826491E23603000000036491E2366491E7D602000000186491E7D66491E9F203000000096491E9F26491EC0E0200000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826491BE1E64922016000000060000000100000051000000CF0000007C02170F001A10046491EC0E6491EDEE03000000086491EDEE6491F0BE020000000C6491F0BE6491F87A03000000216491F87A6491FCEE0200000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826491BE1E64922016000000060000000100000051000000CF0000007C02170F001A14046491FCEE6491FF82030000000B6491FF826492073E02000000216492073E64920AC2040000000F64920AC2649217E20200000038";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826491BE1E64922016000000060000000100000051000000CF0000007C02170F001A1802649217E264921BA2040000001064921BA2649220160300000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826493165364937233000000000000000000000049000000AB00000094021710000F000464931653649317BB0200000006649317BB649326BB0400000040649326BB64932D87020000001D64932D87649332AF0300000016";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826493165364937233000000000000000000000049000000AB00000094021710000F0404649332AF64933E67020000003264933E67649344BB030000001B649344BB64934533020000000264934533649351630300000034";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826493165364937233000000000000000000000049000000AB00000094021710000F0804649351636493595B02000000226493595B64935D57030000001164935D5764935EFB020000000764935EFB649361170400000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826493165364937233000000000000000000000049000000AB00000094021710000F0C0364936117649362BB0200000007649362BB649369C3030000001E649369C3649372330200000024";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826494759C6494CF60000000000000000000000062000000BF0000005E021711001500046494759C64947704020000000664947704649483AC0400000036649483AC6494867C020000000C6494867C649488200400000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826494759C6494CF60000000000000000000000062000000BF0000005E02171100150404649488206494898802000000066494898864948C1C030000000B64948C1C64949270020000001B649492706494957C030000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826494759C6494CF60000000000000000000000062000000BF0000005E021711001508046494957C649497D4020000000A649497D464949AA4040000000C64949AA464949F18020000001364949F186494A4F40400000019";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826494759C6494CF60000000000000000000000062000000BF0000005E02171100150C046494A4F46494A6D402000000086494A6D46494A9A4030000000C6494A9A46494B4E402000000306494B4E46494BBEC030000001E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826494759C6494CF60000000000000000000000062000000BF0000005E021711001510046494BBEC6494BE0802000000096494BE086494C1C803000000106494C1C86494C81C020000001B6494C81C6494CAEC030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826494759C6494CF60000000000000000000000062000000BF0000005E021711001514016494CAEC6494CF600200000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826495B95B64962D230000000A0000000100000077000000AB000000C2021712001A00046495B95B6495BAC302000000066495BAC36495BD57040000000B6495BD576495BFAF010000000A6495BFAF6495CAEF0300000030";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826495B95B64962D230000000A0000000100000077000000AB000000C2021712001A04046495CAEF6495CD0B02000000096495CD0B6495CF9F030000000B6495CF9F6495D39B04000000116495D39B6495E0F70300000039";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826495B95B64962D230000000A0000000100000077000000AB000000C2021712001A08046495E0F76495E403040000000D6495E4036495E697030000000B6495E6976495ECEB020000001B6495ECEB6495EF7F040000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826495B95B64962D230000000A0000000100000077000000AB000000C2021712001A0C046495EF7F6495F19B02000000096495F19B6495F5D304000000126495F5D36495FABF02000000156495FABF6495FE43020000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826495B95B64962D230000000A0000000100000077000000AB000000C2021712001A10046495FE4364960113020000000C649601136496072B030000001A6496072B64960DBB040000001C64960DBB64960F9B0200000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826495B95B64962D230000000A0000000100000077000000AB000000C2021712001A140464960F9B649614C30300000016649614C364961DAB020000002664961DAB64962297040000001564962297649624B30200000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826495B95B64962D230000000A0000000100000077000000AB000000C2021712001A1802649624B36496292703000000136496292764962D230200000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649717C464978F880000000900000001000000670000010900000086021713000F0004649717C46497192C02000000066497192C64973150040000006764973150649738D00200000020649738D064973AB00300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649717C464978F880000000900000001000000670000010900000086021713000F040464973AB0649748C0020000003C649748C064974CF8030000001264974CF86497695402000000796497695464976EF40300000018";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649717C464978F880000000900000001000000670000010900000086021713000F080464976EF464977278020000000F6497727864977674030000001164977674649778CC020000000A649778CC64977C8C0300000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649717C464978F880000000900000001000000670000010900000086021713000F0C0364977C8C649781780200000015649781786497839401000000096497839464978F880300000033";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826497FFCA6498121200000000000000000000001A0000001F00000015021714000400046497FFCA649801320200000006649801326498074A040000001A6498074A64980D26020000001964980D26649812120300000015";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264986A686498C24C000000000000000000000049000000AE000000800217150015000464986A6864986BD0020000000664986BD06498756C04000000296498756C64987DA0020000002364987DA064988070040000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264986A686498C24C000000000000000000000049000000AE00000080021715001504046498807064988430020000001064988430649885D40400000007649885D46498882C020000000A6498882C649893A80300000031";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264986A686498C24C000000000000000000000049000000AE0000008002171500150804649893A864989600020000000A64989600649897E00300000008649897E06498989402000000036498989464989BA0040000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264986A686498C24C000000000000000000000049000000AE0000008002171500150C0464989BA064989E34020000000B64989E346498A848030000002B6498A8486498AB18020000000C6498AB186498ACF80300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264986A686498C24C000000000000000000000049000000AE00000080021715001510046498ACF86498AF1402000000096498AF146498B16C030000000A6498B16C6498BE8C02000000386498BE8C6498C0E4030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264986A686498C24C000000000000000000000049000000AE00000080021715001514016498C0E46498C24C0200000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826499ADF3649A0BB300000001000000010000004D000000FE00000044021716001000046499ADF36499AF5B02000000066499AF5B6499BC0304000000366499BC036499C347020000001F6499C3476499C4730400000005";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826499ADF3649A0BB300000001000000010000004D000000FE00000044021716001004046499C4736499CA1302000000186499CA136499D24703000000236499D2476499DBA702000000286499DBA76499DDFF030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826499ADF3649A0BB300000001000000010000004D000000FE00000044021716001008046499DDFF6499EB5B02000000396499EB5B6499EDEF030000000B6499EDEF6499F137020000000E6499F1376499F407030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "826499ADF3649A0BB300000001000000010000004D000000FE0000004402171600100C046499F407649A04E70200000048649A04E7649A091F0400000012649A091F649A095B0100000001649A095B649A0BB3020000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2023-06-27 20:08:45 Download
        else if (idDebug == 21) {
            Log.e(TAG, "2023-06-27 20:08:45 Download");
        }
        //2023-07-06 18:26:19 Download
        else if (idDebug == 22) {
            Log.e(TAG, "2023-07-06 18:26:19 Download");
            contentData = "82649B5A99649BC08D000000050000000100000056000000FC0000005C020A0000190004649B5A99649B5C010200000006649B5C01649B60390400000012649B6039649B668D020000001B649B668D649B6CA5030000001A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649B5A99649BC08D000000050000000100000056000000FC0000005C020A0000190404649B6CA5649B72090200000017649B7209649B749D030000000B649B749D649B77E5040000000E649B77E5649B79110100000005";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649B5A99649BC08D000000050000000100000056000000FC0000005C020A0000190804649B7911649B794D0200000001649B794D649B7D490300000011649B7D49649B7EB10200000006649B7EB1649B8235040000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649B5A99649BC08D000000050000000100000056000000FC0000005C020A0000190C04649B8235649B8AA50200000024649B8AA5649B8DED030000000E649B8DED649B9045040000000A649B9045649B94410200000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649B5A99649BC08D000000050000000100000056000000FC0000005C020A0000191004649B9441649B974D040000000D649B974D649B9B490200000011649B9B49649B9C750200000005649B9C75649B9E190300000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649B5A99649BC08D000000050000000100000056000000FC0000005C020A0000191404649B9E19649BAE810200000046649BAE81649BB27D0300000011649BB27D649BB5C5040000000E649BB5C5649BC015020000002C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649B5A99649BC08D000000050000000100000056000000FC0000005C020A0000191801649BC015649BC08D0400000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649CA39B649D011F0000000E0000000100000054000000ED00000040020A0100120004649CA39B649CA5030200000006649CA503649CAD730400000024649CAD73649CC3B7020000005F649CC3B7649CCA0B040000001B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649CA39B649D011F0000000E0000000100000054000000ED00000040020A0100120404649CCA0B649CCD53010000000E649CCD53649CD05F020000000D649CD05F649CD5C30300000017649CD5C3649CDAEB0200000016";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649CA39B649D011F0000000E0000000100000054000000ED00000040020A0100120804649CDAEB649CDC8F0400000007649CDC8F649CDEE7020000000A649CDEE7649CE08B0300000007649CE08B649CE3D3020000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649CA39B649D011F0000000E0000000100000054000000ED00000040020A0100120C04649CE3D3649CE8BF0300000015649CE8BF649CEC07040000000E649CEC07649CECBB0300000003649CECBB649CFDD70200000049";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649CA39B649D011F0000000E0000000100000054000000ED00000040020A0100121002649CFDD7649D002F030000000A649D002F649D011F0200000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649DF9C0649E5B04000000090000000100000055000000D30000006E020A02000E0004649DF9C0649DFB280200000006649DFB28649E06680400000030649E0668649E08FC020000000B649E08FC649E0B54040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649DF9C0649E5B04000000090000000100000055000000D30000006E020A02000E0404649E0B54649E0D700100000009649E0D70649E1B80020000003C649E1B80649E21D4040000001B649E21D4649E32000200000045";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649DF9C0649E5B04000000090000000100000055000000D30000006E020A02000E0804649E3200649E3A340300000023649E3A34649E3EE40200000014649E3EE4649E49AC030000002E649E49AC649E53480200000029";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649DF9C0649E5B04000000090000000100000055000000D30000006E020A02000E0C02649E5348649E5A14030000001D649E5A14649E5B040200000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649F4F3C649FCB740000000000000000000000700000012100000081020A03001B0004649F4F3C649F50A40200000006649F50A4649F58240400000020649F5824649F5AB8020000000B649F5AB8649F66340400000031";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649F4F3C649FCB740000000000000000000000700000012100000081020A03001B0404649F6634649F68140200000008649F6814649F69B80400000007649F69B8649F71EC0200000023649F71EC649F74080300000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649F4F3C649FCB740000000000000000000000700000012100000081020A03001B0804649F7408649F7ED0020000002E649F7ED0649F84340400000017649F8434649F8D580200000027649F8D58649F8EC00300000006";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649F4F3C649FCB740000000000000000000000700000012100000081020A03001B0C04649F8EC0649F90DC0200000009649F90DC649F93E8030000000D649F93E8649F94240400000001649F9424649F9DFC020000002A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649F4F3C649FCB740000000000000000000000700000012100000081020A03001B1004649F9DFC649FA180030000000F649FA180649FAC84020000002F649FAC84649FB0F80300000013649FB0F8649FB29C0200000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649F4F3C649FCB740000000000000000000000700000012100000081020A03001B1404649FB29C649FB5E4030000000E649FB5E4649FB878020000000B649FB878649FBA940300000009649FBA94649FBF800200000015";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "82649F4F3C649FCB740000000000000000000000700000012100000081020A03001B1803649FBF80649FC214030000000B649FC214649FC3B80200000007649FC3B8649FCB740300000021";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A098EA64A1146E00000013000000030000005D000000F8000000A7020A040023000464A098EA64A09A52020000000664A09A5264A09F7A040000001664A09F7A64A0A3B2020000001264A0A3B264A0A6FA030000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A098EA64A1146E00000013000000030000005D000000F8000000A7020A040023040464A0A6FA64A0A7AE020000000364A0A7AE64A0AA42040000000B64A0AA4264A0AD12020000000C64A0AD1264A0AFA6040000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A098EA64A1146E00000013000000030000005D000000F8000000A7020A040023080464A0AFA664A0B05A020000000364A0B05A64A0B7DA030000002064A0B7DA64A0BB22020000000E64A0BB2264A0BDF2030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A098EA64A1146E00000013000000030000005D000000F8000000A7020A0400230C0464A0BDF264A0C13A040000000E64A0C13A64A0C31A010000000864A0C31A64A0C356040000000164A0C35664A0C3CE0100000002";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A098EA64A1146E00000013000000030000005D000000F8000000A7020A040023100464A0C3CE64A0C69E020000000C64A0C69E64A0C842030000000764A0C84264A0CC3E020000001164A0CC3E64A0CD2E0300000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A098EA64A1146E00000013000000030000005D000000F8000000A7020A040023140464A0CD2E64A0D166020000001264A0D16664A0D9D6030000002464A0D9D664A0DBB6040000000864A0DBB664A0DDD20100000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A098EA64A1146E00000013000000030000005D000000F8000000A7020A040023180464A0DDD264A0DEFE020000000564A0DEFE64A0DFEE030000000464A0DFEE64A0EBE2020000003364A0EBE264A0EE3A030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A098EA64A1146E00000013000000030000005D000000F8000000A7020A0400231C0464A0EE3A64A0F1BE040000000F64A0F1BE64A0F84E020000001C64A0F84E64A10136030000002664A1013664A10B4A020000002B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A098EA64A1146E00000013000000030000005D000000F8000000A7020A040023200364A10B4A64A10DA2030000000A64A10DA264A11036040000000B64A1103664A1146E0200000012";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A173D964A182D90000000000000000000000160000001600000014020A050005000464A173D964A17541020000000664A1754164A17A69040000001664A17A6964A17D39020000000C64A17D3964A181E90300000014";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A173D964A182D90000000000000000000000160000001600000014020A050005040164A181E964A182D90200000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A1EF0F64A25ADF0000000A0000000200000065000000C800000095020A060020000464A1EF0F64A1F077020000000664A1F07764A1F617040000001864A1F61764A1F6CB010000000364A1F6CB64A1FAC70200000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A1EF0F64A25ADF0000000A0000000200000065000000C800000095020A060020040464A1FAC764A20463030000002964A2046364A2098B020000001664A2098B64A20B2F040000000764A20B2F64A20CD30100000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A1EF0F64A25ADF0000000A0000000200000065000000C800000095020A060020080464A20CD364A20DFF020000000564A20DFF64A20FA3030000000764A20FA364A21237020000000B64A2123764A2166F0400000012";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A1EF0F64A25ADF0000000A0000000200000065000000C800000095020A0600200C0464A2166F64A2197B020000000D64A2197B64A21B5B040000000864A21B5B64A21E67020000000D64A21E6764A22137030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A1EF0F64A25ADF0000000A0000000200000065000000C800000095020A060020100464A2213764A221EB040000000364A221EB64A22317020000000564A2231764A22353040000000164A2235364A224070200000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A1EF0F64A25ADF0000000A0000000200000065000000C800000095020A060020140464A2240764A228F3030000001564A228F364A22B0F040000000964A22B0F64A233F7020000002664A233F764A236C7030000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A1EF0F64A25ADF0000000A0000000200000065000000C800000095020A060020180464A236C764A24063020000002964A2406364A2436F040000000D64A2436F64A2454F020000000864A2454F64A24FDB030000002D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A1EF0F64A25ADF0000000A0000000200000065000000C800000095020A0600201C0464A24FDB64A252E7020000000D64A252E764A2571F040000001264A2571F64A259B3030000000B64A259B364A25ADF0200000005";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A33DEC64A3B40C000000000000000000000078000000EB00000095020A07001F000464A33DEC64A33F54020000000664A33F5464A34404040000001464A3440464A34788020000000F64A3478864A34A1C040000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A33DEC64A3B40C000000000000000000000078000000EB00000095020A07001F040464A34A1C64A34AD0020000000364A34AD064A34F80030000001464A34F8064A353F4020000001364A353F464A358680300000013";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A33DEC64A3B40C000000000000000000000078000000EB00000095020A07001F080464A3586864A35B38040000000C64A35B3864A35BEC020000000364A35BEC64A3618C030000001864A3618C64A36420040000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A33DEC64A3B40C000000000000000000000078000000EB00000095020A07001F0C0464A3642064A36510030000000464A3651064A365C4020000000364A365C464A36678030000000364A3667864A368D0040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A33DEC64A3B40C000000000000000000000078000000EB00000095020A07001F100464A368D064A36A74020000000764A36A7464A36F24040000001464A36F2464A37320030000001164A3732064A377580400000012";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A33DEC64A3B40C000000000000000000000078000000EB00000095020A07001F140464A3775864A387C0020000004664A387C064A38838040000000264A3883864A39B70020000005264A39B7064A39F6C0300000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A33DEC64A3B40C000000000000000000000078000000EB00000095020A07001F180464A39F6C64A3A278020000000D64A3A27864A3A674030000001164A3A67464A3A854020000000864A3A85464A3AA340400000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A33DEC64A3B40C000000000000000000000078000000EB00000095020A07001F1C0364A3AA3464A3AB9C020000000664A3AB9C64A3B22C030000001C64A3B22C64A3B40C0400000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A443A964A4A8E90000000000000000000000920000009D00000081020A080018000464A443A964A44511020000000664A4451164A4508D040000003164A4508D64A45411030000000F64A4541164A454C50200000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A443A964A4A8E90000000000000000000000920000009D00000081020A080018040464A454C564A45939040000001364A4593964A45BCD020000000B64A45BCD64A463C5030000002264A463C564A46B810400000021";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A443A964A4A8E90000000000000000000000920000009D00000081020A080018080464A46B8164A471D5020000001B64A471D564A47469040000000B64A4746964A47685020000000964A4768564A47EB90300000023";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A443A964A4A8E90000000000000000000000920000009D00000081020A0800180C0464A47EB964A4814D040000000B64A4814D64A484D1020000000F64A484D164A4850D040000000164A4850D64A48765020000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A443A964A4A8E90000000000000000000000920000009D00000081020A080018100464A4876564A489F9040000000B64A489F964A48D41030000000E64A48D4164A49935020000003364A4993564A49C41030000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A443A964A4A8E90000000000000000000000920000009D00000081020A080018140464A49C4164A49F4D020000000D64A49F4D64A4A385030000001264A4A38564A4A619040000000B64A4A61964A4A8E9020000000C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A5E90464A640E800000000000000000000004E000000C200000067020A090016000464A5E90464A5EA6C020000000664A5EA6C64A5ED78040000000D64A5ED7864A5F174020000001164A5F17464A5F4F8030000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A5E90464A640E800000000000000000000004E000000C200000067020A090016040464A5F4F864A5FBC4020000001D64A5FBC464A5FE58040000000B64A5FE5864A60434020000001964A6043464A60CA40300000024";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A5E90464A640E800000000000000000000004E000000C200000067020A090016080464A60CA464A60D58020000000364A60D5864A61280040000001664A6128064A614D8020000000A64A614D864A619C40300000015";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A5E90464A640E800000000000000000000004E000000C200000067020A0900160C0464A619C464A61BA4020000000864A61BA464A61E38040000000B64A61E3864A6266C020000002364A6266C64A628C4030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A5E90464A640E800000000000000000000004E000000C200000067020A090016100464A628C464A62DB0020000001564A62DB064A6329C030000001564A6329C64A63440020000000764A6344064A63698040000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData = "8264A5E90464A640E800000000000000000000004E000000C200000067020A090016140264A6369864A63E54020000002164A63E5464A640E8040000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
        //2024-10-28 Download
        else if (idDebug == 23) {
            Log.e(TAG, "2024-10-28 Download");
            contentData= "82671AC081671B267500000000000000000000005E000000AE000000A702070000150004671AC081671AC1E90200000006671AC1E9671AC5E50400000011671AC5E5671AC7890200000007671AC789671AD1D9030000002C";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671AC081671B267500000000000000000000005E000000AE000000A702070000150404671AD1D9671AD9590200000020671AD959671AE421030000002E671AE421671AE6B5040000000B671AE6B5671AE8950200000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671AC081671B267500000000000000000000005E000000AE000000A702070000150804671AE895671AEAED040000000A671AEAED671AEDF9020000000D671AEDF9671AF1F50400000011671AF1F5671AF7950300000018";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671AC081671B267500000000000000000000005E000000AE000000A702070000150C04671AF795671AFC090200000013671AFC09671B034D030000001F671B034D671B0ACD0200000020671B0ACD671B0DD9030000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671AC081671B267500000000000000000000005E000000AE000000A702070000151004671B0DD9671B10E5020000000D671B10E5671B13010300000009671B1301671B17390200000012671B1739671B205D0400000027";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671AC081671B267500000000000000000000005E000000AE000000A702070000151401671B205D671B2675020000001A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671BF00F671C9D0700000010000000030000007C000001D400000082020701002E0004671BF00F671BF1770200000006671BF177671BF40B040000000B671BF40B671BF8070200000011671BF807671BFB4F020000000E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671BF00F671C9D0700000010000000030000007C000001D400000082020701002E0404671BFB4F671BFED3040000000F671BFED3671C03BF0200000015671C03BF671C05DB0400000009671C05DB671C0833010000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671BF00F671C9D0700000010000000030000007C000001D400000082020701002E0804671C0833671C0D5B0200000016671C0D5B671C0F3B0300000008671C0F3B671C194F020000002B671C194F671C1B2F0300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671BF00F671C9D0700000010000000030000007C000001D400000082020701002E0C04671C1B2F671C1DFF020000000C671C1DFF671C21FB0300000011671C21FB671C2507020000000D671C2507671C29030300000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671BF00F671C9D0700000010000000030000007C000001D400000082020701002E1004671C2903671C2F57020000001B671C2F57671C31EB030000000B671C31EB671C3FBF020000003B671C3FBF671C45230300000017";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671BF00F671C9D0700000010000000030000007C000001D400000082020701002E1404671C4523671C45D70200000003671C45D7671C47B70400000008671C47B7671C54230200000035671C5423671C56030400000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671BF00F671C9D0700000010000000030000007C000001D400000082020701002E1804671C5603671C58D3020000000C671C58D3671C5BA3040000000C671C5BA3671C5DBF0200000009671C5DBF671C5FDB0400000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671BF00F671C9D0700000010000000030000007C000001D400000082020701002E1C04671C5FDB671C64130200000012671C6413671C684B0400000012671C684B671C6A2B0200000008671C6A2B671C6DAF030000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671BF00F671C9D0700000010000000030000007C000001D400000082020701002E2004671C6DAF671C7133020000000F671C7133671C73C7040000000B671C73C7671C747B0100000003671C747B671C7E53020000002A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671BF00F671C9D0700000010000000030000007C000001D400000082020701002E2404671C7E53671C8123040000000C671C8123671C8B37020000002B671C8B37671C8CDB0400000007671C8CDB671C8D8F0100000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671BF00F671C9D0700000010000000030000007C000001D400000082020701002E2804671C8D8F671C8FE7020000000A671C8FE7671C94D30300000015671C94D3671C97DF020000000D671C97DF671C9A37030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671BF00F671C9D0700000010000000030000007C000001D400000082020701002E2C02671C9A37671C9C170200000008671C9C17671C9D070400000004";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671CEEF9671D100500000024000000010000000B0000003D00000021020702000B0004671CEEF9671CF0610200000006671CF061671CF2F5040000000B671CF2F5671CF601020000000D671CF601671CF859030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671CEEF9671D100500000024000000010000000B0000003D00000021020702000B0404671CF859671CF9FD0200000007671CF9FD671CFCCD030000000C671CFCCD671CFE710200000007671CFE71671D06E10100000024";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671CEEF9671D100500000024000000010000000B0000003D00000021020702000B0803671D06E1671D0CBD0200000019671D0CBD671D0F51030000000B671D0F51671D10050200000003";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671D6861671DDAFD0000000A000000010000005A000000F70000008E020703001E0004671D6861671D69C90200000006671D69C9671D6C99040000000C671D6C99671D70D10300000012671D70D1671D78C90200000022";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671D6861671DDAFD0000000A000000010000005A000000F70000008E020703001E0404671D78C9671D7B5D040000000B671D7B5D671D7F590300000011671D7F59671D8265020000000D671D8265671D86250300000010";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671D6861671DDAFD0000000A000000010000005A000000F70000008E020703001E0804671D8625671D88B9020000000B671D88B9671D8A5D0300000007671D8A5D671D8CF1020000000B671D8CF1671D8F49030000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671D6861671DDAFD0000000A000000010000005A000000F70000008E020703001E0C04671D8F49671D91A1040000000A671D91A1671D94E9020000000E671D94E9671D96150400000005671D9615671DA2810200000035";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671D6861671DDAFD0000000A000000010000005A000000F70000008E020703001E1004671DA281671DA58D040000000D671DA58D671DA6410200000003671DA641671DB145030000002F671DB145671DB451020000000D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671D6861671DDAFD0000000A000000010000005A000000F70000008E020703001E1404671DB451671DB5F50400000007671DB5F5671DB84D020000000A671DB84D671DBDED0400000018671DBDED671DC045010000000A";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671D6861671DDAFD0000000A000000010000005A000000F70000008E020703001E1804671DC045671DC2D9030000000B671DC2D9671DCF090200000034671DCF09671DD0E90400000008671DD0E9671DD3050200000009";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671D6861671DDAFD0000000A000000010000005A000000F70000008E020703001E1C02671DD305671DD6C50300000010671DD6C5671DDAFD0200000012";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671DEEF6671E01F2000000110000000100000010000000210000000F02070400050004671DEEF6671DF05E0200000006671DF05E671DF41E0400000010671DF41E671DFA72020000001B671DFA72671DFE6E0100000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671DEEF6671E01F2000000110000000100000010000000210000000F02070400050401671DFE6E671E01F2030000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671E4377671E6FC30000001E00000001000000000000007900000026020705000A0004671E4377671E46FB020000000F671E46FB671E4C5F0300000017671E4C5F671E510F0200000014671E510F671E5E2F0200000038";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671E4377671E6FC30000001E00000001000000000000007900000026020705000A0404671E5E2F671E60C3020000000B671E60C3671E60FF0300000001671E60FF671E61EF0200000004671E61EF671E68F7010000001E";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671E4377671E6FC30000001E00000001000000000000007900000026020705000A0802671E68F7671E6C3F030000000E671E6C3F671E6FC3020000000F";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671E92B4671F2E180000000B000000010000005500000155000000E2020706001C0004671E92B4671E941C0200000006671E941C671E98CC0400000014671E98CC671E9BD8020000000D671E9BD8671E9E6C040000000B";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671E92B4671F2E180000000B000000010000005500000155000000E2020706001C0404671E9E6C671EAA240200000032671EAA24671EAE5C0200000012671EAE5C671EB0F0010000000B671EB0F0671EB9D80300000026";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671E92B4671F2E180000000B000000010000005500000155000000E2020706001C0804671EB9D8671EBD5C020000000F671EBD5C671EC9140300000032671EC914671ED1FC0200000026671ED1FC671EE048030000003D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671E92B4671F2E180000000B000000010000005500000155000000E2020706001C0C04671EE048671EE8F40200000025671EE8F4671EECF00300000011671EECF0671EF434020000001F671EF434671EF5D80400000007";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671E92B4671F2E180000000B000000010000005500000155000000E2020706001C1004671EF5D8671F05C80200000044671F05C8671F094C030000000F671F094C671F0DFC0200000014671F0DFC671F14C8040000001D";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671E92B4671F2E180000000B000000010000005500000155000000E2020706001C1404671F14C8671F166C0200000007671F166C671F1B1C0300000014671F1B1C671F1EA0020000000F671F1EA0671F20800300000008";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
            contentData= "82671E92B4671F2E180000000B000000010000005500000155000000E2020706001C1804671F2080671F21E80200000006671F21E8671F26200400000012671F2620671F2A1C0200000011671F2A1C671F2E180300000011";
            dataSleepReportData.add(new ATSleepReportData(toArrayByte(contentData)));
        }
    }

    private void setDebugDataBloodOxygen() {

        dataBloodOxygenData.clear();

        String contentData = "";

        contentData = "83000200000002653FC8026300653FCB386200";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));
        contentData = "8E01D2001E654039A80C016300016200016100016200016200016200016200016200016100016300016000016000016000016000015F00015F00016000015F00016000016000016000016000015F00015C00015F00016000015F00015F00015F00016000";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));
        contentData = "8E01B4001E654040B00C016100016000016100016000016000016000016100016100016000016100016100016100016100016100016100016100016100016100016000016300016200016100016200016100016000016200016200016000015F00015F00";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));
        contentData = "8E0196001E654047B80C016000016100016000016000016100016000016000016000016000016000016000016000016100016200016200016200016000016000015F00016300016000015F00016000016200016200016200016200016200016200016200";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));
        contentData = "8E0178001E65404EFC0C016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));
        contentData = "8E015A001E654056040C016200016200016200016200016200016200015F00015F00015C00016200015F00015F00015F00016200015A00016400016200016100016100016200016200016200016200016200016200016300016400016400016300016300";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));
        contentData = "8E013C001E65405D0C0C016300016300016300016400016200016100016200016100016100016200016100016000016100016000016000016000015F00015F00016000015F00016100016100016100015F00015F00016200016100016300016200016200";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));
        contentData = "8E011E001E654064140C016200016000016000016300016100016200016200016400016000016300016000016200016000016000016000015F00016100016300016300015F00015F00016000016100016100016100016100016100016100016100016100";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));
        contentData = "8E0100001E65406B1C0C016100016100016100016200016100016100016200016200016200016100016200016100016200016200016100016200016200016200016200016000016000016100016000016100015F00015F00016000016000016000016100";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));
        contentData = "8E00E2001E654072240C016100016000016100016100016000016000016000016000016300015F00016000015D00015F00016100016000016000016100016000016200016200016300016300016200016300016400016200016300016100016100016100";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));
        contentData = "8E00C4001E6540792C0C016100016100016100016000016100016200016100016200016100016200016000016200016100016300016200016200016200016200016200016200016200016000016100016100016100016300016200016300016100016100";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));
        contentData = "8E00A6001E654080340C015F00015A00015F00015F00015900016300016200016300016000016300016200016300016300016200016200016300016300016300016300016300016300016300016300016300016300016300016300016300016300016300";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));
        contentData = "8E0088001E6540873C0C016300016300016300016300016300016300016300016300016100015F00015F00015E00015F00015F00015F00015F00015F00015F00015F00016200016000016000016100016000016000016000016000016100016000015F00";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));
        contentData = "8E006A001E65408E440C016000016000016100016000016100016000016000016000016000016000016200016300016300015F00016000016400016300016200016200015F00016200016300016300016300016300016300016300016300016300016300";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));
        contentData = "8E004C001E6540954C0C016300016300016300016300016300016300016300016300016300016300016300016300016300016300016300016300016000016200016100016300016200016200016200016200016200016200016200016200016200016200";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));
        contentData = "8E002E001E65409C540C016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));
        contentData = "8E0010001E6540A35C0C016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));
        contentData = "8E000000106540AA640C016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200016200";
        dataBloodOxygenData.add(new ATBloodOxygenData(toArrayByte(contentData)));

    }

    private void setDebugDataHeartRate() {

        dataHeartRateData.clear();

        String contentData= "100463D3C8783C02AB000C535349454746464745474845";
        dataHeartRateData.add(new ATHeartRateData(toArrayByte(contentData)));

        contentData= "100463D3D6883C029F000C47444746484648474747484C";
        dataHeartRateData.add(new ATHeartRateData(toArrayByte(contentData)));

        contentData= "100463D3E4983C0293000C62654F4B48514E404A425850";
        dataHeartRateData.add(new ATHeartRateData(toArrayByte(contentData)));

//        contentData= "";
//        dataHeartRateData.add(new ATHeartRateData(util.toArrayByte(contentData)));
//
//        contentData= "";
//        dataHeartRateData.add(new ATHeartRateData(util.toArrayByte(contentData)));
    }

    //endregion
}