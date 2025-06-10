package com.eviahealth.eviahealth.ui.ensayo.tensiometro.bc87;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.bluetooth.models.Global;
import com.eviahealth.eviahealth.devices.BeurerReferences;
import com.eviahealth.eviahealth.bluetooth.beurer.bc87.bc87control;
import com.eviahealth.eviahealth.models.beurer.bc87.bc87DataTensiometro;
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

public class get_dataBC87 extends BaseActivity implements View.OnClickListener {

    private String TAG = "GET_DATA_BC87";
    private String FASE = "GET_DATA_BC87";

    ProgressBar circulodescarga;
    TextView txtStatus;
    Button btSaltar;
    CountDownTimer cTimer = null;
    CountDownTimer cTimerBle = null;
    Boolean active = false;

    String DEVICE_MAC_ADDRESS = "";
    private String mDeviceAddress = "A4:C1:38:CC:05:4F"; //
    private bc87control mBC87control = new bc87control();
    bc87DataTensiometro datos;
    private int retries;
    private Integer records = null;
    TextToSpeechHelper textToSpeech;
    Boolean viewdata = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_data_bc87);

        //region :: Views
        txtStatus = findViewById(R.id.txtStatus_bc87);
        txtStatus.setTextSize(14);
        txtStatus.setVisibility(View.VISIBLE);
        circulodescarga = findViewById(R.id.cdescarga_bc87);
        circulodescarga.setVisibility(View.INVISIBLE);

        btSaltar = findViewById(R.id.btSaltar_bc87);
        btSaltar.setVisibility(View.INVISIBLE);
        //endregion

        viewdata = false;
        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                return;
            }
        }

        DEVICE_MAC_ADDRESS = Config.getInstance().getIdentificador(NombresDispositivo.TENSIOMETRO);
        EVLog.log(TAG,"MAC DISPOSITIVO: "+ DEVICE_MAC_ADDRESS);
        if (DEVICE_MAC_ADDRESS.contains("BC87")){
            DEVICE_MAC_ADDRESS = DEVICE_MAC_ADDRESS.replace("BC87-", "");
            mDeviceAddress = util.MontarMAC(DEVICE_MAC_ADDRESS);
            EVLog.log(TAG,"MAC mDeviceAddress: "+ mDeviceAddress);
        }

        datos = bc87DataTensiometro.getInstance();
        datos.clear();
        retries = 0;

        setTextSatus("Esperando colocación.");

        //TimeOut >> Para mostrar el cirulo de pensar
        Integer timeout = 1000 * 15;  // 15 seg tiempo estimado para colocarse el tensiometro
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
                        setTextSatus("Esperando activación.");
                    }
                });
            }
        };
        cTimer.start();

        Integer timeoutBle = timeout + (1000 * 15);  // 15 seg tiempo estimado para colocarse el tensiometro + 20 seg espera para que esté midendo
        cTimerBle = new CountDownTimer(timeoutBle, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                cTimerBle.cancel();
                cTimerBle = null;

                runOnUiThread(new Runnable() {
                    public void run() {
//                        btSaltar.setVisibility(View.VISIBLE);
                        setTextSatus("Conectando con dispositivo...");
                        mBC87control.initialize(get_dataBC87.this,mDeviceAddress);
                    }
                });
            }
        };
        cTimerBle.start();

        registerReceiver(mCallbackBeurer, makeUpdateIntentFilter(), RECEIVER_EXPORTED);

    }

    @Override
    public void onClick(View view) {
        Log.e(TAG," onClick()");
        int viewId = view.getId();
        if (viewId == R.id.btSaltar_bc87) {
            //region >> Button Reintentar
            btSaltar.setEnabled(false);
            EVLog.log(TAG, "DETERNER >> onclick()");
            EnsayoLog.log(FASE, TAG, "PACIENTE PULSA REINTENTAR");
            ViewResult();
            //endregion
        }
        else if (viewId == R.id.imageButtonAudio) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();

            String frase = "Descúbrase la muñeca. Colóquese el brazalete de forma que la palma de la mano y la pantalla miren hacia arriba.";
            texto.add(frase);
            frase = "Coloque el brazo en posición que el aparato esté a la altura del corazón durante la medición.";
            texto.add(frase);
            frase = "Para iniciar la medición, pulse la tecla, INICIO.";
            texto.add(frase);
            frase = "Una vez finalizada la medición vuelva a pulsar la tecla de INICIO, para descargar los datos.";
            texto.add(frase);
            textToSpeech.speak(texto);
            //endregion
        }
    }

    //region :: BROADCAST RECEIVER
    private final BroadcastReceiver mCallbackBeurer = new BroadcastReceiver() {

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
                    runOnUiThread(new Runnable() {
                        public void run() {
                            btSaltar.setVisibility(View.INVISIBLE);
                        }
                    });
                    break;
                case BeurerReferences.ACTION_BEURER_DISCONNECTED:
                    // region :: DISCONNECTED
                    retries += 1;
                    Integer isconnect = Global.getKey(message);
                    Log.e(TAG,"DISCONNECTED STATE: " + Global.connectStatus.get(isconnect));

                    if (isconnect == Global.STATE_CONNECTING) {
                        if (retries < 10) {
                            Log.e(TAG, "Reintento de conexion: " + retries);
                            setTextSatus("CONNECT[" + retries + "]");
                            mBC87control.destroy();
                            SystemClock.sleep(1000);
                            mBC87control.initialize(get_dataBC87.this, mDeviceAddress);
                        }
                        else {
                            datos.set_status_descarga(false);
                            datos.setError("{\"type\":\"BC87\",\"error\":801,\"description\":\"No detectado dispositivo.\"}");
                            ViewResult();
                        }
                    }
                    else if (isconnect == Global.STATE_CONNECTED) {
                        // EL DISPOSITIVO SE HA DESCONECTADO SOLO
                        message = mBC87control.getRecordData();
                        Log.e(TAG,"message: " + message);
                        Integer status = util.getIntValueJSON(message, "status");
                        if (status == 0 ||status == 4 ) {
                            broadcastUpdate(BeurerReferences.ACTION_BEURER_BC54_DOWNLOAD_DATA_FINISHED, message);
                        } else {
                            if (status == null) {
                                String error = "{\"type\":\"BC87\",\"error\":-1,\"description\":\"No detectado dispositivo.\"}";
                                broadcastUpdate(BeurerReferences.ACTION_BEURER_BC54_MEASUREMENT_ERROR, error);
                            } else {
                                String error = mBC87control.msgGetMeasurementStatus(message);
                                Log.e(TAG, "Problems: " + error);

                                if (util.getIntValueJSON(error, "bodyMovement") == 1) {
                                    error = "{\"type\":\"BC87\",\"error\":1,\"description\":\"Movimiento del cuerpo.\"}";
                                    broadcastUpdate(BeurerReferences.ACTION_BEURER_BC54_MEASUREMENT_ERROR, error);
                                }
                                else if (util.getIntValueJSON(error, "cuffFit") == 1) {
                                    error = "{\"type\":\"BC87\",\"error\":2,\"description\":\"El manguito está demasiado suelto.\"}";
                                    broadcastUpdate(BeurerReferences.ACTION_BEURER_BC54_MEASUREMENT_ERROR, error);
                                }
                                else if (util.getIntValueJSON(error, "measurementPosition") == 1) {
                                    error = "{\"type\":\"BC87\",\"error\":6,\"description\":\"Detectada posición incorrecta del dispositivo.\"}";
                                    broadcastUpdate(BeurerReferences.ACTION_BEURER_BC54_MEASUREMENT_ERROR, error);
                                }
                                else if (util.getIntValueJSON(error, "pulseRateRange") == 1) {
                                    error = "{\"type\":\"BC87\",\"error\":3,\"description\":\"El pulso detectado excede el limite superior.\"}";
                                    broadcastUpdate(BeurerReferences.ACTION_BEURER_BC54_MEASUREMENT_ERROR, error);
                                }
                                else if (util.getIntValueJSON(error, "pulseRateRange") == 2) {
                                    error = "{\"type\":\"BC87\",\"error\":4,\"description\":\"El pulso detectado se encuentra por debajo del limite inferior.\"}";
                                    broadcastUpdate(BeurerReferences.ACTION_BEURER_BC54_MEASUREMENT_ERROR, error);
                                }
                                else if (util.getIntValueJSON(error, "pulseRateRange") == 3) {
                                    error = "{\"type\":\"BC87\",\"error\":5,\"description\":\"Detectado un problema con la medición del pulso.\"}";
                                    broadcastUpdate(BeurerReferences.ACTION_BEURER_BC54_MEASUREMENT_ERROR, error);
                                }
                                else {
                                    error = "{\"type\":\"BC87\",\"error\":-1,\"description\":\"not found.\"}";
                                    broadcastUpdate(BeurerReferences.ACTION_BEURER_BC54_MEASUREMENT_ERROR, error);
                                }
                            }
                        }
                    }
                    else {
                        datos.set_status_descarga(false);
                        datos.setError("{\"type\":\"BC87\",\"error\":801,\"description\":\"No detectado dispositivo.\"}");
                        ViewResult();
                    }

                    //endregion
                    break;

                case BeurerReferences.ACTION_BEURER_BC54_DOWNLOAD_DATA:
                    Log.e(TAG,"ACTION_BEURER_BC87_DOWNLOAD_DATA");
                    setTextSatus("Download");
                    break;

                case BeurerReferences.ACTION_BEURER_BC54_DOWNLOAD_DATA_FINISHED:
                    Log.e(TAG,"ACTION_BEURER_BC87_DOWNLOAD_DATA_FINISHED Message:" + message);
                    datos.setMessure(message);
                    EVLog.log(TAG,"" + message);
                    datos.set_status_descarga(true);
                    ViewResult();
                    break;

                case BeurerReferences.ACTION_BEURER_BC54_MEASUREMENT_ERROR:
                    Log.e(TAG,"ACTION_BEURER_BC87_MEASUREMENT_ERROR Message:" + message);
                    datos.set_status_descarga(false);
                    datos.setError(message);
                    ViewResult();
                    break;

                case BeurerReferences.ACTION_BEURER_CMD_FAIL:
                    Log.e(TAG,"Message: " + message);
                    function = util.getStringValueJSON(message,"function");
                    setTextSatus("Fail process: " + function);

                    datos.set_status_descarga(false);
                    datos.setError("{\"type\":\"BC87\",\"error\":803,\"description\":\"Detectado un fallo de comunicación con el dispositivo..\"}");

                    mBC87control.disconnect();
                    break;

                case BeurerReferences.ACTION_BEURER_GET_OTHER:
                    Log.e(TAG,"GET_OTHER Message:" + message);
                    mBC87control.disconnect();
                    break;

                case BeurerReferences.ACTION_BEURER_COMMUNICATION_TIMEOUT:
                    Log.e(TAG,"ACTION_BEURER_COMMUNICATION_TIMEOUT Message:" + message);

                    datos.set_status_descarga(false);
                    datos.setError("{\"type\":\"BC87\",\"error\":802,\"description\":\"Ha fallado la descarga de datos con el dispositivo.\"}");
                    mBC87control.disconnect();
                    break;

                default:
                    Log.e(TAG,"accion de broadcast no implementada");
                    datos.set_status_descarga(false);
                    datos.setError("{\"type\":\"BC87\",\"error\":804,\"description\":\"ACCION NO CONTEMPLADA\"}");
                    mBC87control.disconnect();
                    break;

            };
        }
    };

    private static IntentFilter makeUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_CONNECTED);
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_DISCONNECTED);
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_GET_OTHER);
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_CMD_FAIL);
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_COMMUNICATION_TIMEOUT);

        intentFilter.addAction(BeurerReferences.ACTION_BEURER_BC54_DOWNLOAD_DATA);
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_BC54_DOWNLOAD_DATA_FINISHED);
        intentFilter.addAction(BeurerReferences.ACTION_BEURER_BC54_MEASUREMENT_ERROR);

        return intentFilter;
    }

    private void broadcastUpdate(final String action, String message) {
        final Intent intent = new Intent(action);
        intent.putExtra(BeurerReferences.ACTION_EXTRA_MESSAGE, message);
        sendBroadcast(intent);
    }
    //endregion

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (cTimer != null) cTimer.cancel();
        if (cTimerBle != null) cTimerBle.cancel();
        textToSpeech.shutdown();
        if (mBC87control != null) { mBC87control.destroy(); }
        mBC87control = null;
        unregisterReceiver(mCallbackBeurer);

        Log.e(TAG, "onDestroy()");
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
                Intent in = new Intent(this, view_failBC87.class);
                startActivity(in);
            }
            else {
                Intent in = new Intent(this, view_dataBC87.class);
                startActivity(in);
            }

            EVLog.log(TAG, " ViewResult() >> finish()");
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
//        mBC87control.disconnect();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}

}