package com.eviahealth.eviahealth.ui.ensayo.pulsera.mambo6;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.models.SecuenciaActivity;
import com.eviahealth.eviahealth.models.transtek.mb6.data.M6Datapulsera;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class view_dataMAMBO6 extends BaseActivity implements View.OnClickListener {

    private String TAG = "VIEW_DATA_MAMBO6";
    final String FASE = "PULSERA ACTIVIDAD MAMBO6";
    Button btContinuar;
    TextView txtPasos, txtNivel, txtBateria;

    M6Datapulsera datos;
    CountDownTimer cTimer = null;
    TextToSpeechHelper textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data_mambo6);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Mantener la pantala activa

        EVLog.log(TAG,"onCreate()");

        //region :: Referencia Views
        btContinuar = findViewById(R.id.btderecha_m6);
        txtPasos = findViewById(R.id.m6_pasos);
        txtNivel = findViewById(R.id.m6_nivel);

        txtBateria = findViewById(R.id.txtbateria_m6);
        txtBateria.setVisibility(View.INVISIBLE);
        //endregion

        // texto >> Voz
        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = M6Datapulsera.getInstance();

        EnsayoLog.log(FASE,TAG,"Se han descargado los datos de la Pulsera de Actividad");

        // Comprobación del nivel de batería
        if (datos.getBattery() < 20){
            EVLog.log(TAG,"barrety_percent: " + datos.getBattery());
            EnsayoLog.log(FASE,TAG,"Batería Pulsera Actividad BAJA: " + datos.getBattery() +"%");
            txtBateria.setVisibility(View.VISIBLE);
        }

        setTextView(txtPasos,"" + datos.getTotalStepsDay());
        setTextView(txtNivel,"" + datos.getBattery().toString());

        EnsayoLog.log(FASE,TAG,"DATOS DE ACTIVIDAD DESCARGADOS CORRECTAMENTE");
//            EnsayoLog.log(FASE,TAG,"Pasos Totales: " + datos.getTotalStepsDay() + ", Calorias Totales: " + datos.getCaloriesDay());

        //region :: Timer >> Espera un poco para habiltar botones
        Integer timeout = 1000 * 2;
        cTimer = new CountDownTimer(timeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                cTimer.cancel();
                cTimer = null;
                btContinuar.setEnabled(true);
            }
        };
        cTimer.start();
        //endregion

        datos.setFilesActivity();
    }

    @Override
    public void onClick(View view) {
        Log.e(TAG," onClick()");
        int viewId = view.getId();
        if (viewId == R.id.btderecha_m6) {
            //region >> Button Continuar
            btContinuar.setEnabled(false);
            EVLog.log(TAG, "CONTINUAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "PACIENTE PULSA CONTINUAR");
            ir_siguiente_actividad();
            //endregion
        }
        else if (viewId == R.id.imageButtonAudio) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();

            texto.add("Sus datos de actividad se han descargado correctamente.");
            texto.add("Hoy ha realizado la siguente actividad:");

            String frase = "Actividad: " + datos.getTotalStepsDay().toString() + " pasos.";
            texto.add(frase);

            if (datos.getBattery() <= 20) {
                frase = "Batería baja, porfavor ponga a cargar su pulsera de actividad.";
                texto.add(frase);
            }

            textToSpeech.speak(texto);
            //endregion
        }
    }

    private void ir_siguiente_actividad() {
        SecuenciaActivity.getInstance().next(this);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (cTimer != null) cTimer.cancel();
        EVLog.log(TAG,"onDestroy()");
        super.onDestroy();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){
    }

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
            EVLog.log("FILEACCESS READ", "IOException: " + ex.toString());
        }
    }

    @Override
    protected  void onPause() {
        super.onPause();
        EVLog.log(TAG,"onPause()");
    }

    private void setTextView(View view, String texto) {
//        Log.e(TAG,"" + texto);
        runOnUiThread(new Runnable() {
            public void run() {

                if (view instanceof TextView) {
                    TextView obj = (TextView) view;
                    obj.setText(texto);
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
}