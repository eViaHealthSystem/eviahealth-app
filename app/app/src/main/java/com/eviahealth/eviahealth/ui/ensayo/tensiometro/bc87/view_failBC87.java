package com.eviahealth.eviahealth.ui.ensayo.tensiometro.bc87;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.beurer.bc87.bc87DataTensiometro;
import com.eviahealth.eviahealth.models.beurer.bc87.bc87Errors;
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

public class view_failBC87 extends BaseActivity implements View.OnClickListener {
    private String TAG = "VIEW_FAIL_BC87";
    final String FASE = "TENSIOMETRO BC87";

    Button btContinuar, btReintentar;
    TextView txtMensaje, txtTitulo;
    CountDownTimer cTimer = null;
    int type_puls = 0;
    bc87DataTensiometro datos;
    Boolean _status_descarga= false;
    Boolean fgsaltar = false;
    TextToSpeechHelper textToSpeech;
    String mensage = "";
    String titulo = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_fail_bc87);
        EVLog.log(TAG,"onCreate()");

        //region :: Referencia Views
        btContinuar = findViewById(R.id.btContinuar_bc87);
        btReintentar = findViewById(R.id.btReintentar_bc87);
        txtTitulo = findViewById(R.id.txtTitulo_bc87);
        txtMensaje = findViewById(R.id.txtTexto_bc87);
        //endregion

        // texto >> Voz
        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = bc87DataTensiometro.getInstance();
        mensage = "";
        titulo = "";
        fgsaltar = false;

        _status_descarga = datos.get_status_descarga();

        if (_status_descarga == true){

            EnsayoLog.log(FASE,TAG,"Se ha realizado la medición con el Tensiómetro");

            titulo = "Resultados obtenidos de la medición:";

            mensage =  "Sistólica:    " + datos.getHighPressure() + "  (mmHg)\n\n";
            mensage += "Diastólica:   " + datos.getLowPressure() + "  (mmHg)\n\n";
            mensage += "Pulso:            " + datos.getPulsaciones() + "  (latidos/min)\n";

            txtMensaje.setTextSize(32);
            txtMensaje.setPadding(txtMensaje.getPaddingLeft() + 30, txtMensaje.getPaddingTop(), txtMensaje.getPaddingRight(), txtMensaje.getPaddingBottom());

            EnsayoLog.log(FASE,TAG,"DATOS DEL TENSIOMETRO DESCARGADOS CORRECTAMENTE");
//            EnsayoLog.log(FASE,TAG,"Sistólica: " +datos.getHighPressure() + ", Diastólica: " + datos.getLowPressure() + ", pulso: " + datos.getPulsaciones());
        }
        else {
            // DESCARGA INCOMPLETA
            EVLog.log(TAG,"Descarga de datos incorrecta ERROR[" + datos.get_error_num()+"]");
            EnsayoLog.log(FASE,TAG,"Ha fallado la medición con el Tensiómetro");

            titulo = "La medida NO se ha realizado correctamente. !!!";

            DescripcionErrorGeneric desc = bc87Errors.getDescripcionError(datos.get_error_num());
            if (desc == null){
                desc = bc87Errors.getDescripcionError(-1);
            }

            String error = desc.getError();
            String solucion = desc.getSolucion();

            if (datos.get_error_num() != -1) {
                mensage = error + "\r\n\n";
                mensage += solucion;
            }
            else {
                titulo = error;
                mensage = solucion;

                Resources r = getResources();

                float height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, r.getDisplayMetrics());
                float top = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, r.getDisplayMetrics());
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) txtTitulo.getLayoutParams();
                params.height = (int)height;
//                params3.leftMargin = 100;
                params.topMargin = (int) top;
                txtTitulo.setLayoutParams(params);

                txtMensaje.setTextSize(22);
                height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 520, r.getDisplayMetrics());
                ViewGroup.MarginLayoutParams params2 = (ViewGroup.MarginLayoutParams) txtMensaje.getLayoutParams();
                params2.height = (int) height;
                txtMensaje.setLayoutParams(params2);
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
    }

    @Override
    public void onClick(View view) {
        Log.e(TAG," onClick()");
        int viewId = view.getId();
        if (viewId == R.id.btReintentar_bc87) {
            //region >> Button Reintentar
            btReintentar.setEnabled(false);
            btContinuar.setEnabled(false);
            EVLog.log(TAG, "REINTENTAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "PACIENTE PULSA REINTENTAR");
            startActivity(new Intent(this, get_dataBC87.class));
            finish();
            //endregion
        }
        else if (viewId == R.id.btContinuar_bc87) {
            //region >> Button Continuar
            btReintentar.setEnabled(false);
            btContinuar.setEnabled(false);

            if (fgsaltar) {
                EVLog.log(TAG, "SALTAR >> onclick()");
                EnsayoLog.log(FASE, TAG, "PACIENTE PULSA SALTAR");
            } else {
                EVLog.log(TAG, "CONTINUAR >> onclick()");
                EnsayoLog.log(FASE, TAG, "PACIENTE PULSA CONTINUAR");
                datos.setFilesActivity();   // Genera archivo datos
            }

            ir_siguiente_actividad();
            //endregion
        }
        else if (viewId == R.id.imageButtonAudio) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            texto.add(titulo);
            if (_status_descarga == true) {

                String frase = "Sistólica " + datos.getHighPressure().toString() + ", milímetros de mercurio.";
                texto.add(frase);
                frase = "Diastólica " + datos.getLowPressure().toString() + ", milímetros de mercurio.";
                texto.add(frase);
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