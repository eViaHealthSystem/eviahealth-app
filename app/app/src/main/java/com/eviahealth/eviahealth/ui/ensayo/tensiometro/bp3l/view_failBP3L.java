package com.eviahealth.eviahealth.ui.ensayo.tensiometro.bp3l;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.ihealth.bp3l.Datatensiometro;
import com.eviahealth.eviahealth.models.SecuenciaActivity;
import com.eviahealth.eviahealth.models.ihealth.bp3l.DescripcionErrorBP;
import com.eviahealth.eviahealth.models.ihealth.bp3l.ErroresTensiometroBP;
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

public class view_failBP3L extends BaseActivity implements View.OnClickListener {

    final String TAG = "BP3L-FAIL";
    final String FASE = "TENSIÓMETRO";
    Button btContinuar, btReintentar;
    TextView txtMensaje, txtTitulo, txtBateria;
    ImageView imgBateria;
    Datatensiometro datos;
    Boolean _status_descarga;
    Boolean fgsaltar = false;
    CountDownTimer cTimer = null;
    TextToSpeechHelper textToSpeech;
    String mensage = "";
    String titulo = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_fail_bp3_l);
        EVLog.log(TAG,"onCreate()");

        //region :: Views
        btContinuar = findViewById(R.id.btderecha_ten);
        btReintentar = findViewById(R.id.btizquierda_ten);
        txtMensaje = findViewById(R.id.txtTexto_ten);
        txtTitulo = findViewById(R.id.txtTitulo_ten);
        imgBateria = findViewById(R.id.igbateria_ten);
        imgBateria.setVisibility(View.INVISIBLE);
        txtBateria = findViewById(R.id.txtbateria_ten);
        txtBateria.setVisibility(View.INVISIBLE);
        //endregion

        // texto >> Voz
        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = Datatensiometro.getInstance();
        mensage = "";
        titulo = "";
        fgsaltar = false;

        // DESCARGA INCOMPLETA
        EVLog.log(TAG,"Descarga de datos incorrecta ERROR[" + datos.get_error_num()+"]");
        EnsayoLog.log(FASE,TAG,"Ha fallado la medición con el Tensiómetro");

        titulo = "La medida NO se ha realizado correctamente. !!!";

        DescripcionErrorBP desc = ErroresTensiometroBP.getDescripcionErrorBP(datos.get_error_num());
        if (desc == null){
            desc = ErroresTensiometroBP.getDescripcionErrorBP(-1);
        }

        String error = desc.getError();
        String solucion = desc.getSolucion();

        mensage = error + "\r\n\n";
        mensage += solucion;

        if (datos.get_error_num() == 13) {
            EVLog.log(TAG, "(SDK) Batería Baja: " + datos.getBattery());
            imgBateria.setVisibility(View.VISIBLE);
            txtBateria.setVisibility(View.VISIBLE);
        }

        // Ponemos Botón derecha como SALTAR
        btContinuar.setText("SALTAR");
        fgsaltar = true;

        EVLog.log(TAG,"NERR[" + datos.get_error_num() + "] CAUSA: " + error);
        EnsayoLog.log(FASE,TAG,"CAUSA: " + error);

        Integer err = datos.get_error_num();
        if ((err >=0 && err <= 700) || err == 804 || err == 805)
            EnsayoLog.log(FASE,"ERR_TEN",error);
        else
            EnsayoLog.log(FASE,"ERR_TEN","Fallo de conexión con el dispositivo.");

        txtTitulo.setText(titulo);
        txtMensaje.setText(mensage);

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
            Intent in = new Intent(view_failBP3L.this, get_dataBP3L.class);
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
            texto.add(titulo);
            if (_status_descarga == true) {

                String frase = "Sistólica " + datos.get_highPressure().toString() + ", milímetros de mercurio.";
                texto.add(frase);
                frase = "Diastólica " + datos.get_lowPressure().toString() + ", milímetros de mercurio.";
                texto.add(frase);

                if (datos.getBattery() <= 20) {
                    frase = "Batería baja, porfavor ponga a cargar su tensiómetro.";
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
}