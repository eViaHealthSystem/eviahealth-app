package com.eviahealth.eviahealth.ui.ensayo.termometro.nt13b;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.ihealth.nt13b.DataThermometer;
import com.eviahealth.eviahealth.models.SecuenciaActivity;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class view_dataNT13B extends BaseActivity implements View.OnClickListener {

    private String TAG = "VIEW_DATA_NT13B";
    final String FASE = "TERMOMETRO NT13B";
    Button btContinuar, btReintentar;
    TextView txtTemperatura;
    DataThermometer datos;
    CountDownTimer cTimer = null;
    TextToSpeechHelper textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data_nt13b);

        EVLog.log(TAG,"onCreate()");

        //region :: Referencia Views
        btContinuar = findViewById(R.id.buttonContinuar);
        btReintentar = findViewById(R.id.buttonReintentar);
        txtTemperatura = findViewById(R.id.nt13b_ter);
        //endregion

        // texto >> Voz
        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = DataThermometer.getInstance();

        EnsayoLog.log(FASE,TAG,"Se ha descargado la medición de temperatura");

        setTextView(txtTemperatura, datos.getValor().toString());

        EnsayoLog.log(FASE,TAG,"MEDIDA DE TEMPERATURA DESCARGADA CORRECTAMENTE");

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
                btReintentar.setEnabled(true);
            }
        };
        cTimer.start();
        //endregion

        // Crea fichero del dato de temperatura
        datos.setFilesActivity();
    }

    @Override
    public void onClick(View view) {
        Log.e(TAG," onClick()");
        Intent intent;
        int viewId = view.getId();
        if (viewId == R.id.buttonReintentar) {
            //region >> Button Reintentar
            btReintentar.setEnabled(false);
            btContinuar.setEnabled(false);
            EVLog.log(TAG, "REINTENTAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "PACIENTE PULSA REINTENTAR");
            startActivity(new Intent(this, get_dataNT13B.class));
            finish();
            //endregion
        }
        else if (viewId == R.id.buttonContinuar) {
            //region >> Button Continuar
            btReintentar.setEnabled(false);
            btContinuar.setEnabled(false);

            EVLog.log(TAG, "CONTINUAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "PACIENTE PULSA CONTINUAR");

            ir_siguiente_actividad();
            //endregion
        }
        else if (viewId == R.id.imageButtonAudio) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();

            texto.add("Su medida de temperatura se ha descargado correctamente.");

            String tp = datos.getValor().toString();
            String[] dev = tp.split("\\.");
            String t;
            if (dev.length == 2)
                if (dev[1].equals("1")) {
                    t = "Su temperatura es de " + dev[0] + " con un grados.";
                }
                else {
                        t = "Su temperatura es de " + dev[0] + " con " + dev[1] + " grados.";
                }
            else
                t = "Su temperatura es de " + dev[0] + " grados.";
            texto.add(t);

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
        textToSpeech.shutdown();
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