package com.eviahealth.eviahealth.ui.ensayo.oximetro.po60;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.beurer.po60.po60Dataoximetro;
import com.eviahealth.eviahealth.models.beurer.po60.po60Errors;
import com.eviahealth.eviahealth.models.SecuenciaActivity;
import com.eviahealth.eviahealth.models.errors.DescripcionErrorGeneric;
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

public class view_failPO60 extends BaseActivity implements View.OnClickListener  {

    private String TAG = "VIEW_FAILPO60";
    final String FASE = "PULSIOXIMETRO PO60";
    Button btContinuar, btReintentar;
    TextView txtMensaje, txtTitulo;

    po60Dataoximetro datos;
    Boolean fgsaltar = false;

    CountDownTimer cTimer = null;
    TextToSpeechHelper textToSpeech;
    String mensage = "";
    String titulo = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_fail_po60);


        EVLog.log(TAG,"onCreate()");

        //region :: Referencia Views
        btContinuar = findViewById(R.id.btContinuar_oxi);
        btReintentar = findViewById(R.id.btReintentar_oxi);
        txtTitulo = findViewById(R.id.txtTitulo_oxi);
        txtMensaje = findViewById(R.id.txtTexto_oxi);
        //endregion

        // texto >> Voz
        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = po60Dataoximetro.getInstance();
        mensage = "";
        titulo = "";
        fgsaltar = false;

        // DESCARGA INCOMPLETA
        EVLog.log(TAG,"Descarga de datos incorrecta");
        EnsayoLog.log(FASE,TAG,"Ha fallado la medición con el Pulsioxímetro");

        titulo = "La medida NO se ha realizado correctamente. !!!";
        txtTitulo.setTextColor(Color.rgb(180,33,33)); //color rojizo

        DescripcionErrorGeneric desc = po60Errors.getDescripcionErrorPO(datos.get_error_num());
        if (desc == null){
            desc = po60Errors.getDescripcionErrorPO(-1);
        }

        String error = desc.getError();
        String solucion = desc.getSolucion();

        mensage = error + "\r\n\n";
        mensage += solucion;

//            if (datos.get_error_num() == 700 || datos.get_error_num() == 800){
//                // No se ha pulsado START
//                imagen.setImageResource(R.drawable.pulsi_start);
//            }

        // Ponemos Botón derecha como SALTAR
        btContinuar.setText("SALTAR");
        fgsaltar = true;

        EVLog.log(TAG,"NERR[" + datos.get_error_num() + "] CAUSA: " + error);
        EnsayoLog.log(FASE,TAG,"CAUSA: " + error);

        Integer err = datos.get_error_num();
        if ((err >=0 && err <= 700) || err == 804)
            EnsayoLog.log(FASE,"ERR_OXI",error);
        else
            EnsayoLog.log(FASE,"ERR_OXI","Fallo de conexión con el dispositivo.");

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
        Intent intent;
        int viewId = view.getId();
        if (viewId == R.id.btReintentar_oxi) {
            //region >> Button Reintentar
            btReintentar.setEnabled(false);
            btContinuar.setEnabled(false);
            EVLog.log(TAG, "REINTENTAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "PACIENTE PULSA REINTENTAR");
            startActivity(new Intent(this, get_dataPO60.class));
            finish();
            //endregion
        }
        else if (viewId == R.id.btContinuar_oxi) {
            //region >> Button Continuar
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