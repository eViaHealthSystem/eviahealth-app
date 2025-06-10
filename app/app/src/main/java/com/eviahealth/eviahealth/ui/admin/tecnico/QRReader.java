package com.eviahealth.eviahealth.ui.admin.tecnico;

import androidx.annotation.RequiresApi;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.eviahealth.eviahealth.api.BackendConector.ApiConnector;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FileFuntions;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.admin.fichapaciente.DispositivosPaciente;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.utils.log.EVLogConfig;
import com.eviahealth.eviahealth.utils.PermissionUtils;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

public class QRReader extends BaseActivity {

    final String TAG = "QRReader";

    private CameraSource cameraSource;
    private SurfaceView cameraView;
    private String token = "";
    private String tokenanterior = "";
    private boolean once = true;
    private static TextView serialError;

    private String serial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_q_r_reader);
        Log.e(TAG,"onCreate() *****************");

        PermissionUtils.requestAll(this);

        try {
            serial = FileFuntions.readfile(FilePath.CONFIG_SERIAL.getNameFile());
            TextView numeroserie = findViewById(R.id.textView4);
            numeroserie.setText("IDENTIFICADOR TABLET: " + serial);

            serialError = findViewById(R.id.textView3);
            serialError.setText("LEYENDO...");

            cameraView = (SurfaceView) findViewById(R.id.camera_view);
            final Button btnsalir = findViewById(R.id.salir_qr_reader);
            btnsalir.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bundle extras = getIntent().getExtras();
                    String key = extras.getString("key");
                    String id = extras.getString("id");
                    if (extras != null) {
                        if (key.equals("Ficha")) {
                            Log.e(TAG, "Key: Ficha");
                            Intent i = new Intent(QRReader.this, DispositivosPaciente.class);
                            i.putExtra("numeropaciente", id);
                            startActivity(i);
                            finish();
                        }
                        if (key.equals("Login")) {
                            Log.e(TAG, "Key: Login");
                            startActivity(new Intent(QRReader.this, Inicio.class));
                            finish();
                        }
                    }
                    else { Log.e(TAG, "No extras values intent");}
                }
            });

            once = true;
            Log.e(TAG,"initQR()");
            initQR();
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"onCreate() Exception: " + e);
        }
    }

    public void initQR() {
        // creo el detector qr
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.ALL_FORMATS).build();

        // creo la camara
        cameraSource = new CameraSource
                .Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1600, 1024)
                .setAutoFocusEnabled(true)
                .build();

        // listener de ciclo de vida de la camara
        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @SuppressLint("MissingPermission")
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // DEBE DE HABESE SOLICITADO PERMISO
                try {
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException ie) {
                    Log.e("CAMERA SOURCE IOException: ", ie.getMessage());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) { cameraSource.stop(); }

        });

        // preparo el detector de QR
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {}

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                if (barcodes.size() > 0 && once == true) {

                    // obtenemos el token
                    token = barcodes.valueAt(0).displayValue.toString();
                    try {
                        JSONObject json = new JSONObject(token);
                        Log.e("TOKEN", "TOKENNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN" + json);
                        String idpaciente = json.getString("idpaciente");
                        String token = json.getString("token");
                        String idtablet = json.getString("idtablet");
                        String url = json.getString("url");

//                        String serial = leerSerial();
                        // Compara nº serial con el almacenado en el archivo
                        if (!idtablet.equals(serial)){
                            serialError.setText("EL NÚMERO DE SERIE RECIBIDO NO CORRESPONDE CON ESTA TABLETA\n   " + "RECIBIDO: " + idtablet + "  LOCAL: " + serial);
                        }else {
                            serialError.setText("EL NÚMERO DE SERIE RECIBIDO CORRESPONDE CON ESTA TABLETA\n   " + "RECIBIDO: " + idtablet + "  LOCAL: " + serial);

                            escribirTOKENenFichero(token, FilePath.CONFIG_TOKEN, "token");
//                            escribirTOKENenFichero(idpaciente, FilePath.CONFIG_PACIENTE, "idpaciente");

                            JSONObject jsonpaciente = new JSONObject();
                            jsonpaciente.put("idpaciente", idpaciente);
                            jsonpaciente.put("multipaciente", false);
                            FileAccess.escribirJSON(FilePath.CONFIG_PACIENTE, jsonpaciente);

                            escribirADMINFichero(FilePath.CONFIG_ADMIN, url, idtablet, "URL", "imei");

                            setFilesBackup();

                            ApiConnector.setToken(token);
                            ApiConnector.setHost(url);

                            if (!token.equals(tokenanterior)) {
                                tokenanterior = token;
                                Log.e("token", token);
                            }
                            once = false;

                            finish();
                            Intent intent = new Intent(QRReader.this, DispositivosPaciente.class);
                            intent.putExtra("numeropaciente", idpaciente);
                            startActivity(intent);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "receiveDetections IOException: " + e);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(TAG, "receiveDetections JSONException: " + e);
                    }
                }
            }
        });

    }

    private void escribirTOKENenFichero(String data, FilePath filePath,  String name) throws IOException, JSONException {
        JSONObject json = new JSONObject();
        json.put(name, data);
        FileAccess.escribirJSON(filePath, json);
    }

    private void escribirADMINFichero(FilePath filePath, String data,  String data1, String name, String name1) throws IOException, JSONException {
        JSONObject json = new JSONObject();
        json.put(name, data);
        json.put(name1, data1);
        FileAccess.escribirJSON(filePath, json);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy()");
        super.onDestroy();
    }

    /**
     * Envía el contenido al ContantProvider Backup
     */
    public void setFilesBackup(){
        EVLogConfig.log(TAG,"QRReader: setFilesBackup()");
    }

}