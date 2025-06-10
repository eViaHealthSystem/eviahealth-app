package com.eviahealth.eviahealth.ui.ensayo.tensiometro.bm57;

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
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.models.beurer.bm57.bm57DataTensiometro;
import com.eviahealth.eviahealth.models.SecuenciaActivity;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class view_dataBM57 extends BaseActivity implements View.OnClickListener {

    private String TAG = "VIEW_DATA_BM57";
    final String FASE = "TENSIOMETRO BM57";
    bm57DataTensiometro datos;
    Button btContinuar,btReintentar;
    TextView txtSistolica, txtDiastolica, txtPulso;
    Boolean fgsaltar = false;
    CountDownTimer cTimer = null;
    TextToSpeechHelper textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data_bm57);
        EVLog.log(TAG,"onCreate()");

        //region :: Views
        btContinuar = findViewById(R.id.btderecha_ten);
        btReintentar = findViewById(R.id.btizquierda_ten);

        txtSistolica = findViewById(R.id.bm57_sis);
        txtDiastolica = findViewById(R.id.bm57_dia);
        txtPulso = findViewById(R.id.bm57_pulso);

        //endregion

        // texto >> Voz
        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = bm57DataTensiometro.getInstance();
        fgsaltar = false;

        EnsayoLog.log(FASE,TAG,"Se ha realizado la medición con el Tensiómetro");

        setTextView(txtSistolica, datos.getHighPressure().toString());
        setTextView(txtDiastolica, datos.getLowPressure().toString());
        setTextView(txtPulso, datos.getPulsaciones().toString());

        EnsayoLog.log(FASE,TAG,"DATOS DEL TENSIOMETRO DESCARGADOS CORRECTAMENTE");
//            EnsayoLog.log(FASE,TAG,"Sistólica: " +datos.get_highPressure() + ", Diastólica: " + datos.get_lowPressure() + ", p.p.m: " + datos.get_pulsaciones());

        //TimeOut >> Espera un poco para habiltar botones
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
    }
    @Override
    public void onClick(View view) {
        Log.e(TAG," onClick()");
        int viewId = view.getId();
        if (viewId == R.id.btizquierda_ten) {
            //region >> Button Reintentar
            btReintentar.setEnabled(false);
            btContinuar.setEnabled(false);
            EVLog.log(TAG, "REINTENTAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "PACIENTE PULSA REINTENTAR");
            Intent in = new Intent(this, get_dataBM57.class);
            startActivity(in);
            finish();
            //endregion
        }
        else if (viewId == R.id.btderecha_ten) {
            //region >> Button Continuar / Saltar
            btReintentar.setEnabled(false);
            btContinuar.setEnabled(false);
            if (fgsaltar) {
                EVLog.log(TAG, "SALTAR >> onclick()");
                EnsayoLog.log(FASE, TAG, "PACIENTE PULSA SALTAR");
            } else {
                EVLog.log(TAG, "CONTINUAR >> onclick()");
                EnsayoLog.log(FASE, TAG, "PACIENTE PULSA CONTINUAR");
                datos.setFilesActivity();
            }

            ir_siguiente_actividad();
            //endregion
        }
        else if (viewId == R.id.imageButtonAudio) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            texto.add("Resultados obtenidos de su medición:");

            String frase = "Sistólica " + datos.getHighPressure().toString() + ", milímetros de mercurio.";
            texto.add(frase);
            frase = "Diastólica " + datos.getLowPressure().toString() + ", milímetros de mercurio.";
            texto.add(frase);

            textToSpeech.speak(texto);
            //endregion
        }
    }

    private void ir_siguiente_actividad() {
        SecuenciaActivity.getInstance().next(this);
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