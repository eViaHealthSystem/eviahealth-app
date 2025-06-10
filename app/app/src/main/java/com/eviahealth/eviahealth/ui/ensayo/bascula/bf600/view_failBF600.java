package com.eviahealth.eviahealth.ui.ensayo.bascula.bf600;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.beurer.bf600.DataBF600;
import com.eviahealth.eviahealth.models.SecuenciaActivity;
import com.eviahealth.eviahealth.models.errors.scale.DescriptionErrorScale;
import com.eviahealth.eviahealth.models.errors.scale.ScaleErrors;
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

public class view_failBF600 extends BaseActivity implements View.OnClickListener {

    private String TAG = "VIEW_FAIL_BF600";
    final String FASE = "BÁSCULA BF600";
    Button btContinuar, btReintentar;
    TextView txtMensaje, txtTitulo;
    int type_puls = 0;

    DataBF600 datos;
    Boolean _status_descarga= false;
    Boolean fgsaltar = false;

    CountDownTimer cTimer = null;
    TextToSpeechHelper textToSpeech;
    String mensage = "";
    String titulo = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_fail_bf600);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Mantener la pantala activa

        EVLog.log(TAG,"onCreate()");
        Bundle extras = getIntent().getExtras();
        type_puls = extras.getInt("id_dispositivo");

        //region :: Referencia Views
        btContinuar = findViewById(R.id.BF600_btderecha_fail);
        btReintentar = findViewById(R.id.BF600_btizquierda_fail);

        txtTitulo = findViewById(R.id.BF600_txtTitulo_pul_fail);
        txtMensaje = findViewById(R.id.BF600_txtTexto_pul_fail);

        //endregion

        // texto >> Voz
        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = DataBF600.getInstance();
        mensage = "";
        titulo = "";
        fgsaltar = false;

        // DESCARGA INCOMPLETA
        EVLog.log(TAG,"Descarga de datos incorrecta");
        EnsayoLog.log(FASE,TAG,"Ha fallado la descarga de datos.");

        btReintentar.setVisibility(View.VISIBLE);

        titulo = "NO se han podido descargar los datos de su báscula !!!\r\n";
        txtTitulo.setTextColor(Color.rgb(180,33,33)); //color rojizo

        DescriptionErrorScale desc = ScaleErrors.getErrorDescription(datos.get_error_num());
        if (desc == null){
            desc = ScaleErrors.getErrorDescription(-1);
        }

        String error = desc.getError();

        mensage = error + "\r\n\n";
        mensage += desc.getSolucion() + "\r\n\n";
        mensage += desc.getContinuar() + "\r\n\n";

        // Ponemos Botón derecha como SALTAR
        btContinuar.setText("SALTAR");
        fgsaltar = true;

        EVLog.log(TAG,"NERR[" + datos.get_error_num() + "] CAUSA: " + error);
        EnsayoLog.log(FASE,TAG,"CAUSA: " + error);

        Integer err = datos.get_error_num();
        if ((err >=0 && err <= 700) || err == 804)
            EnsayoLog.log(FASE,"ERR_ACT",error);
        else
            EnsayoLog.log(FASE,"ERR_ACT","Fallo de conexión con el dispositivo.");

        txtTitulo.setText(titulo);
        txtMensaje.setText(mensage);

        //region :: Timer >> Espera un poco para habilitar botones
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

    }

    @Override
    public void onClick(View view) {
        Log.e(TAG," onClick()");
        int viewId = view.getId();
        if (viewId == R.id.BF600_btizquierda_fail) {
            //region >> Button Reintentar
            btReintentar.setEnabled(false);
            btContinuar.setEnabled(false);
            EVLog.log(TAG, "REINTENTAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "PACIENTE PULSA REINTENTAR");
            startActivity(new Intent(this, get_dataBF600.class));
            finish();
            //endregion
        }
        else if (viewId == R.id.BF600_btderecha_fail) {
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

            ir_siguiente_actividad();

            //endregion
        }
        else if (viewId == R.id.imageButtonAudio) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            texto.add(titulo);
            if (_status_descarga == true) {
                String frase = "Hoy lleva realizados la siguiente actividad: .";
                ;
                texto.add(frase);
//                    frase = "Pasos " + datos.getTotalStepsDay().toString() + ".";
                texto.add(frase);

                if (datos.getBattery() <= 20) {
                    frase = "Batería baja, porfavor pongase en contacto con el servicio técnico para su sustitución.";
                    texto.add(frase);
                }
            } else
                texto.add(mensage);
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
}