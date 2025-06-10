package com.eviahealth.eviahealth.ui.ensayo.pulsera.am4;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
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
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.models.ihealth.am4.Datapulsera;
import com.eviahealth.eviahealth.models.SecuenciaActivity;
import com.eviahealth.eviahealth.models.errors.DescripcionErrorGeneric;
import com.eviahealth.eviahealth.models.ihealth.am4.ErroresPulsera;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class view_dataAM4 extends BaseActivity implements View.OnClickListener {

    final String TAG = "AM4-VIEW ";
    final String FASE = "PULSERA ACTIVIDAD";
    Button btContinuar;
    Button btReintentar;
    ImageView imBateria;
    TextView txtMensaje;
    TextView txtTitulo;
    TextView txtBateria;
    int type_puls = 0;

    Datapulsera datos;
    Boolean _status_descarga= false;
    Boolean fgsaltar = false;

    CountDownTimer cTimer = null;
    TextToSpeechHelper textToSpeech;
    String mensage = "";
    String titulo = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulsera_viewdata_am4);
        EVLog.log(TAG,"onCreate()");

        Bundle extras = getIntent().getExtras();
        type_puls = extras.getInt("id_dispositivo");

        //region :: Views
        btContinuar = findViewById(R.id.btderecha);
        btReintentar = findViewById(R.id.btizquierda);
        imBateria = findViewById(R.id.igbateria);
        txtTitulo = findViewById(R.id.txtTitulo_pul);
        txtMensaje = findViewById(R.id.txtTexto_pul);
        txtBateria = findViewById(R.id.txtbateria);
        imBateria.setVisibility(View.INVISIBLE);
        txtBateria.setVisibility(View.INVISIBLE);
        //endregion

        // texto >> Voz
        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = Datapulsera.getInstance();
        mensage = "";
        titulo = "";
        fgsaltar = false;

        _status_descarga = datos.get_status_descarga();
        if (_status_descarga == true){
            btReintentar.setVisibility(View.INVISIBLE);
            EnsayoLog.log(FASE,TAG,"Se han descargado los datos de la Pulsera de Actividad");

            // Comprobación del nivel de batería
            if (datos.getBattery() <= 20){
                EVLog.log(TAG,"barrety_percent: " + datos.getBattery());
                EnsayoLog.log(FASE,TAG,"Batería Pulsera Actividad BAJA: " + datos.getBattery() +"%");
                imBateria.setVisibility(View.VISIBLE);
                txtBateria.setVisibility(View.VISIBLE);
            }

            datos.calculate_datos_actividad();

            titulo = "Sus datos de actividad se han descargado correctamente.";

            mensage = "Hoy ha realizado la siguiente actividad:\r\n";
            mensage += "\t\t\t\t\tPasos: " + datos.getPasos_total() + "\t\t\tCalorias: " + datos.getCalorias_total() + "\r\n";

            mensage += "\nDesde la última descarga ha realizado:\r\n";
            mensage += "\t\t\t\t\tPasos: " + datos.getPasos_ultimo() + "\t\t\tCalorias: " + datos.getCalorias_ultimo() + "\r\n";

            EnsayoLog.log(FASE,TAG,"DATOS DE ACTIVIDAD DESCARGADOS CORRECTAMENTE");
        //    EnsayoLog.log(FASE,TAG,"Pasos Totales: " + datos.getPasos_total() + ", Calorias Totales: " + datos.getCalorias_total());
        }
        else {
            // DESCARGA INCOMPLETA
            EVLog.log(TAG,"Descarga de datos incorrecta");
            EnsayoLog.log(FASE,TAG,"Ha fallado la descarga de datos de la Pulsera de Actividad");

            btReintentar.setVisibility(View.VISIBLE);

            titulo = "NO se ha podido descargar los datos de la Pulsera !!!\r\n";
            txtTitulo.setTextColor(Color.rgb(180,33,33)); //color rojizo

//            mensage = "\nMantenga la Pulsera cerca y compruebe que tiene batería.";

            DescripcionErrorGeneric desc = ErroresPulsera.getDescripcionErrorACT(datos.get_error_num());
            if (desc == null){
                desc = ErroresPulsera.getDescripcionErrorACT(-1);
            }

            String error = desc.getError();
            String solucion = desc.getSolucion();

            mensage = error + "\r\n\n";
            mensage += solucion;

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
        }
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

        datos.setFilesActivity();
    }

    @Override
    public void onClick(View view) {

        Log.e(TAG," onClick()");
        int viewId = view.getId();
        if (viewId == R.id.btizquierda) {
            //region >> Button Reintentar
            btReintentar.setEnabled(false);
            btContinuar.setEnabled(false);
            EVLog.log(TAG, "REINTENTAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "PACIENTE PULSA REINTENTAR");

            Intent in = new Intent(view_dataAM4.this, get_dataAM4.class);
            startActivity(in);
            finish();
            //endregion
        }
        else if (viewId == R.id.btderecha) {
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
                String frase = "Hoy lleva realizados la siguiente actividad: ";
                ;
                texto.add(frase);
                frase = "Pasos " + datos.getPasos_total() + ".";
                texto.add(frase);

                if (datos.getBattery() <= 20) {
                    frase = "Batería baja, porfavor ponga a cargar la pulsera.";
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
    protected void onStart() {
        super.onStart();
        EVLog.log(TAG,"onStart()");
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

    @Override
    protected void onStop() {
        super.onStop();
        EVLog.log(TAG,"onStop()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        EVLog.log(TAG,"onRestart()");
    }
}