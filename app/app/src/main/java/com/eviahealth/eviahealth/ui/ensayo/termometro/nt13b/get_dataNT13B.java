package com.eviahealth.eviahealth.ui.ensayo.termometro.nt13b;

import android.annotation.SuppressLint;
import android.content.Intent;
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

import androidx.core.content.ContextCompat;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.models.ihealth.nt13b.DataThermometer;
import com.eviahealth.eviahealth.bluetooth.models.EStatusDevice;
import com.eviahealth.eviahealth.devices.NombresDispositivo;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;
import com.ihealth.communication.control.NT13BControl;
import com.ihealth.communication.control.NT13BProfile;
import com.ihealth.communication.manager.DiscoveryTypeEnum;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class get_dataNT13B extends BaseActivity implements View.OnClickListener {

    final String TAG = "NT13B-DATA";
    final String TypeDevice = "NT13B";
    final int REINTENTOS = 2;
    private NT13BControl mNT13BControl;
    private int mClientCallbackId;
    EStatusDevice statusDevice= EStatusDevice.None;
    String mDeviceMac; // = "E1893DEA41F8"; // NT13B-E1893DEA41F8
    ProgressBar circulodescarga;
    TextView txtStatus;
    TextToSpeechHelper textToSpeech;
    CountDownTimer cTimer = null;       // TimeOut para que se realice la medición de temperatura

    DataThermometer datos;
    Boolean audio_on = false;
    int cont_scan = 0;
    Boolean viewdata = false;           // Para evitar que se lance dos veces la siguiente actividad

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_data_nt13b);
        EVLog.log(TAG, " onCreate()");

        txtStatus = findViewById(R.id.txtStatus_pul);
        txtStatus.setTextSize(14);
        setVisibleView(txtStatus,View.VISIBLE);

        circulodescarga = findViewById(R.id.circulodescarga);
        setVisibleView(circulodescarga,View.INVISIBLE);

        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = DataThermometer.getInstance();
        datos.clear();

        viewdata = false;

        String DEVICE_MAC_ADDRESS = Config.getInstance().getIdentificador(NombresDispositivo.TERMOMETRO);
        mDeviceMac = DEVICE_MAC_ADDRESS.replace("NT13B-", "");
        EVLog.log(TAG,"MAC TERMOMETRO: "+ DEVICE_MAC_ADDRESS);

        // register ihealthDevicesCallback id
        mClientCallbackId = iHealthDevicesManager.getInstance().registerClientCallback(miHealthDevicesCallback);
        // Limited wants to receive notification specified device
        iHealthDevicesManager.getInstance().addCallbackFilterForDeviceType(mClientCallbackId, iHealthDevicesManager.TYPE_NT13B);
        // Busca dispositivos tipo NT13B
        statusDevice = EStatusDevice.Scanning;
        iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.NT13B);
        setTextView(txtStatus, statusDevice.toString());

        int timeout = (int)(1000 * 40); // 40 segundo si no se hace nada el termometro se apaga
        cTimer = new CountDownTimer(timeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimer.cancel();
                cTimer = null;
                EVLog.log(TAG, "OCURRIDO TIMEOUT DE REALIZACIÓN DE MEDICIÓN");
                datos.setStatusDescarga(false);
                datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":804,\"description\":\"No ha pulsado botón MEASURE.\"}");
                viewResult();
            }
        };
    }

    /*
    mDeviceMac: E1893DEA41F8, mDeviceName: NT13B
    NT13B - action_enable_measurement_success - {}
    NT13B - action_measurement_result - {"unit_flag":0,"result":37,"ts_flag":0,"thermometer_type_flag":1,"thermometer_type":2}
    NT13B - action_measurement_result - {"unit_flag":0,"result":35.900001525878906,"ts_flag":0,"thermometer_type_flag":1,"thermometer_type":2}
     */
    private iHealthDevicesCallback miHealthDevicesCallback = new iHealthDevicesCallback() {

        @Override
        public void onScanDevice(String mac, String deviceType, int rssi, Map manufactorData) {

            EVLog.log(TAG, "iHealth onScanDevice() >> mac[" + mac + "], deviceType[" + deviceType + "]");

            if (mac.equals(mDeviceMac)) {
                EVLog.log(TAG, "iHealth onScanDevice() Device " +deviceType +" [" + mac + "] detected");
                iHealthDevicesManager.getInstance().stopDiscovery();
                statusDevice = EStatusDevice.Detected;
            }
        }

        @Override
        public void onScanFinish() {
            super.onScanFinish();

            // Salta cuando se envia stopDiscovery(); o salta timeout de conexión de ihealth

            EVLog.log(TAG, "iHealth onScanFinish() >> entra");
            iHealthDevicesManager.getInstance().stopDiscovery();
            iHealthDevicesManager.getInstance().disconnectAllDevices(true);

            Log.e(TAG,"statusDevice: " + statusDevice);

            if (statusDevice == EStatusDevice.Detected) {
                // DISPOSITIVO DETECTADO
                EVLog.log(TAG, "iHealth onScanFinish() >> " + TypeDevice + " DETECTADO");
                iHealthDevicesManager.getInstance().stopDiscovery();

                // Conectado con el dispositivo
                if (iHealthDevicesManager.getInstance().isDiscovering()){
                    EVLog.log(TAG, "iHealth isDiscovering() = true");
                    iHealthDevicesManager.getInstance().stopDiscovery();
                    SystemClock.sleep(1000);
                }
                setFondo(2);
                statusDevice = EStatusDevice.Connecting;

                iHealthDevicesManager.getInstance().connectDevice( mDeviceMac, TypeDevice);
                setTextView(txtStatus,"" + statusDevice);

            }
            else {
                // DISPOSITIVO NO ENCONTRADO
                cont_scan += 1;
                if (cont_scan <= REINTENTOS){
                    EVLog.log(TAG, "iHealth onScanFinish() >> NO ENCONTRADO REINTENTAMOS SCAN (" + Integer.toString(cont_scan)+")");
                    // reintentamos scanearlo
                    statusDevice= EStatusDevice.Scanning;
                    iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.NT13B);
                    setTextView(txtStatus,"" + statusDevice + " [" + cont_scan + "]");
                }
                else {
                    EVLog.log(TAG, "iHealth onScanFinish() >> NO ENCONTRADO FINALIZAMOS SCAN (" + Integer.toString(cont_scan)+")");
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":800,\"description\":\"No detectado dispositivo.\"}");
                    viewResult();
                }
            }

        }

        @Override
        public void onDeviceConnectionStateChange(String mac, String deviceType, int status, int errorID) {
            Log.e(TAG,"onDeviceConnectionStateChange() >> deviceType: " + deviceType + ", mac: " + mac + ", status: " + status);

            if (status == iHealthDevicesManager.DEVICE_STATE_CONNECTED) {

                EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTED");
                statusDevice = EStatusDevice.Connected;
                try {
                    setFondo(3);
                    // Get NT13B controller
                    mNT13BControl = iHealthDevicesManager.getInstance().getNT13BControl(mDeviceMac);

                    // Esperamos que se realiza la medición
                    mNT13BControl.getMeasurement();
                    cTimer.start();

                }catch (Exception e){
                    EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTED >> Exception getNT13BControl()");
                    statusDevice = EStatusDevice.Failed;

                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":801,\"description\":\"No detectado dispositivo.\"}");
                    viewResult();
                }
            }

            if (status == iHealthDevicesManager.DEVICE_STATE_DISCONNECTED) {
                EVLog.log(TAG, "iHealth DEVICE_STATE_DISCONNECTED >> entra");

                if (statusDevice == EStatusDevice.Measurement) {
                    // Datos medidos correctamente
                    datos.setStatusDescarga(true);
                    viewResult();
                }
                else if (statusDevice == EStatusDevice.WaitMeasurement) {
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":802,\"description\":\"No ha pulsado botón MEASURE.\"}");
                    viewResult();
                }
                else {
                    statusDevice = EStatusDevice.Disconnecting;
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":803,\"description\":\"Error no esperado\"}");
                    viewResult();
                }
            }

            if (status == iHealthDevicesManager.DEVICE_STATE_CONNECTIONFAIL) {
                EVLog.log(TAG, "iHealth DEVICE_STATE_CONNECTIONFAIL >> ERRORID: " + errorID);

                setTextView(txtStatus,statusDevice.toString());
                Log.e(TAG,"********************* " + statusDevice.toString());
                if (statusDevice == EStatusDevice.WaitMeasurement) {
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":805,\"description\":\"No ha pulsado botón MEASURE.\"}");
                    viewResult();
                }
                else if (statusDevice != EStatusDevice.Disconnecting){
                    datos.setStatusDescarga(false);
                    datos.setERROR("{\"type\":" + TypeDevice + ",\"error\":801,\"description\":\"Connection Fail\"}");
                    viewResult();
                }
                statusDevice = EStatusDevice.Failed;
            }

            if (status == iHealthDevicesManager.DEVICE_STATE_RECONNECTING) {
                EVLog.log(TAG, "iHealth DEVICE_STATE_RECONNECTING");
                statusDevice = EStatusDevice.Reconnecting;
            }

        }

        @Override
        public void onUserStatus(String username, int userStatus) {
            Log.e(TAG,"onUserStatus() >> username: " + username + ", userState: " + userStatus);
        }

        @Override
        public void onDeviceNotify(String mac, String deviceType, String action, String message) {
            super.onDeviceNotify(mac, deviceType, action, message);

            Log.e(TAG, "onDeviceNotify() >> deviceType: " + deviceType + ", mac:" + mac + ", action:" + action + ", message" + message);

            if (NT13BProfile.ACTION_ENABLE_MEASUREMENT_SUCCESS.equals(action)) {
                // Se ha pulsado el botón de MEASURE
                Log.e(TAG, "onDeviceNotify() >> ACTION_MEASUREMENT_RESULT: " + message);
                statusDevice = EStatusDevice.WaitMeasurement;
                setTextView(txtStatus,statusDevice.toString());
            }
            else if (NT13BProfile.ACTION_MEASUREMENT_RESULT.equals(action)) {
                // {"unit_flag":0,"result":36.900001525878906,"ts_flag":0,"thermometer_type_flag":1,"thermometer_type":2}
                Log.e(TAG, "onDeviceNotify() >> ACTION_MEASUREMENT_RESULT: " + message);
                cTimer.cancel();
                statusDevice = EStatusDevice.Measurement;
                setTextView(txtStatus,statusDevice.toString());
                datos.setResponse(message);
                mNT13BControl.disconnect();
            }

        }
    };

    public void viewResult() {

        if (viewdata == false) {
            viewdata = true;

            EVLog.log(TAG, " ViewResult()");
            if (cTimer != null) cTimer.cancel();
            cTimer = null;
            setVisibleView(circulodescarga, View.INVISIBLE);

            if (datos.getStatusDescarga()) {
                Intent in = new Intent(this, view_dataNT13B.class);
                startActivity(in);
            }
            else {
                Intent in = new Intent(this, view_failNT13B.class);
                startActivity(in);
            }

            finish();
        }
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.imageButtonAudio) {
            audio_on = true;
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            texto.add("Encienda el termómetro pulsando brevemente el botón de ENCENDIDO. Oirá un pitido corto cuando el termómetro esté listo.");
            texto.add("Compruebe que el termómetro está configurado correctamente.");
            texto.add("Mantenga el termómetro a unos 3 cm de la frente y Pulse el botón MEASURE. El termómetro dejará de pitar cuando finalice la medida.");
            textToSpeech.speak(texto);
            //endregion
        }
    }

    private void setTextView(View view, String texto) {
        runOnUiThread(new Runnable() {
            public void run() {

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

    private void setFondo(int id){

        if (id == 2) {

            CountDownTimer cTimerPause = new CountDownTimer(1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) { }

                @Override
                public void onFinish() {
                    findViewById(R.id.idlayout_nt13b).setBackground(ContextCompat.getDrawable(get_dataNT13B.this, R.drawable.nt13b_fase02));
//                    iHealthDevicesManager.getInstance().connectDevice( mDeviceMac, TypeDevice);
//                    setTextView(txtStatus,"" + statusDevice);
                }
            };
            cTimerPause.start();

        }
        else {
            findViewById(R.id.idlayout_nt13b).setBackground(ContextCompat.getDrawable(this, R.drawable.nt13b_fase03));
        }
    }

    @Override
    protected void onDestroy() {
        if (cTimer != null) cTimer.cancel();
        cTimer = null;

        textToSpeech.shutdown();

        if(mNT13BControl != null){
            mNT13BControl.disconnect();
        }
        iHealthDevicesManager.getInstance().unRegisterClientCallback(mClientCallbackId);
        EVLog.log(TAG," onDestroy()");
        super.onDestroy();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume()");

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
}