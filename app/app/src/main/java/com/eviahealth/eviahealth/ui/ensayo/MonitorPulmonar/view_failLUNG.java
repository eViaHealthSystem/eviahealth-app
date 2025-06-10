package com.eviahealth.eviahealth.ui.ensayo.MonitorPulmonar;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.SecuenciaActivity;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;
import com.eviahealth.eviahealth.models.vitalograph.DescripcionErrorLung;
import com.eviahealth.eviahealth.models.vitalograph.LungData;
import com.eviahealth.eviahealth.models.vitalograph.LungErrors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class view_failLUNG extends BaseActivity implements View.OnClickListener {

    private String TAG = "VIEW_FAIL_LUNG";
    final String FASE = "M. PULMONAR FAIL LUNG";
    Button btContinuar, btReintentar;
    TextView txtMensaje, txtTitulo;
    String mensage = "";
    String titulo = "";
    LungData datos;
    Boolean _status_descarga= false;
    Boolean fgsaltar = false;
    CountDownTimer cTimer = null;
    TextToSpeechHelper textToSpeech;
    private Integer id_test = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_fail_lung);

        EVLog.log(TAG,"onCreate()");

        //region :: Referencia Views
        btContinuar = findViewById(R.id.buttonContinuar);
        btReintentar = findViewById(R.id.buttonReintentar);
        txtTitulo = findViewById(R.id.textTitulo);
        txtMensaje = findViewById(R.id.textTexto);

        setVisibleView(btContinuar, View.INVISIBLE);
        setVisibleView(btReintentar, View.INVISIBLE);
        //endregion

        // texto >> Voz
        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = LungData.getInstance();
        id_test = datos.getIdTest();
        fgsaltar = false;

        // DESCARGA INCOMPLETA
        EVLog.log(TAG,"Descarga de datos incorrecta");
        EnsayoLog.log(FASE, TAG, "EL TEST NO SE HA REALIZADO CORRECTAMENTE, INTENTOS: " + id_test);

        id_test += 1;
        datos.setIdTest(id_test);

        titulo = "NO se ha podido obtener datos !!!\r\n";
        txtTitulo.setTextColor(Color.rgb(180,33,33)); //color rojizo

        DescripcionErrorLung desc = LungErrors.DescripcionErrorLung(datos.getERROR());
        if (desc == null){
            desc = LungErrors.DescripcionErrorLung(-1);
        }

        String error = desc.getError();
        String solucion = desc.getSolucion();

        mensage = error + "\r\n\n";
        mensage += solucion;

        // Ponemos Botón derecha como SALTAR
        btContinuar.setText("SALTAR");
        fgsaltar = true;

        EVLog.log(TAG,"NERR[" + datos.getERROR() + "] CAUSA: " + error);
        EnsayoLog.log(FASE,TAG,"CAUSA: " + error);

        // Reajust del tamaño de letra
        if (datos.getERROR() == 801) {
            txtMensaje.setTextSize(26);
        }


        txtTitulo.setText(titulo);
        txtMensaje.setText(mensage);

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

                if (id_test >= 3) { setVisibleView(btContinuar, View.VISIBLE); }
                else { setVisibleView(btContinuar, View.INVISIBLE); }

                setVisibleView(btReintentar, View.VISIBLE);
            }
        };
        cTimer.start();
        //endregion

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
            startActivity(new Intent(this, get_dataLUNG.class));
            finish();
            //endregion
        }
        else if (viewId == R.id.buttonContinuar) {
            //region >> Button Continuar
            btReintentar.setEnabled(false);
            btContinuar.setEnabled(false);

            if (fgsaltar) {
                EVLog.log(TAG, "SALTAR >> onclick()");
                EnsayoLog.log(FASE, TAG, "PACIENTE PULSA SALTAR");
            } else {
                EVLog.log(TAG, "CONTINUAR >> onclick()");
                EnsayoLog.log(FASE, TAG, "PACIENTE PULSA CONTINUAR");
            }

            datos.clear();
            ir_siguiente_actividad();
            //endregion
        }
        else if (viewId == R.id.imageButtonAudio) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            texto.add(titulo);
            texto.add(mensage);
            textToSpeech.speak(texto);
            //endregion
        }
    }

    private void ir_siguiente_actividad() {
        SecuenciaActivity.getInstance().next(this);
//        finish();
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
}