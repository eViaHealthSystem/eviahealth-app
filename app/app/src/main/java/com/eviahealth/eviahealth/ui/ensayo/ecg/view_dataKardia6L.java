package com.eviahealth.eviahealth.ui.ensayo.ecg;

import android.annotation.SuppressLint;
import android.content.Intent;
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
import com.eviahealth.eviahealth.models.alivecor.data.KardiaData;
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

public class view_dataKardia6L extends BaseActivity implements View.OnClickListener {

    private String TAG = "VIEW_DATA_K6L";
    final String FASE = "KARDIAMOBILE 6L";
    Button btContinuar, btReintentar;
    TextView txtBPM, txtResult;

    KardiaData datos;
    Boolean fgsaltar = false;
    Integer valor_bpm = null;
    CountDownTimer cTimer = null;
    TextToSpeechHelper textToSpeech;
    List<String> texto = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_view_kardia6_l);
        EVLog.log(TAG,"onCreate()");

        //region :: Referencia Views
        btContinuar = findViewById(R.id.k6l_btContinuar);
        btReintentar = findViewById(R.id.k6l_btReintentar);

        txtBPM = findViewById(R.id.txt_bpm);
        txtResult = findViewById(R.id.txt_result);

        setVisibleView(btContinuar, View.INVISIBLE);
        setVisibleView(btReintentar, View.INVISIBLE);
        //endregion

        // texto >> Voz
        texto.clear();
        textToSpeech = new TextToSpeechHelper(getApplicationContext());
        texto.add("Resultado obtenido en su ECG");


        fgsaltar = false;
        datos = KardiaData.getInstance();
        EVLog.log(TAG,"datos K6L: " + datos.toString());

        EnsayoLog.log(FASE,TAG,"REALIZADA GRABACIÓN DEL ECG");


        Float bpm = null;
        try {
            bpm = Float.parseFloat(datos.getBpm().replace(',','.'));
            Log.e(TAG, "bpm" + bpm.toString());
            if (bpm != null) {
                valor_bpm = bpm.intValue();
                Log.e(TAG, "valor" + valor_bpm);
            }
        } catch (NumberFormatException e) {
            System.out.println("El formato del número no es válido.");
            valor_bpm = 0;
        }

        if (bpm != null) { setTextView(txtBPM, valor_bpm.toString()); }
        else { setTextView(txtBPM, "---"); }

        if (valor_bpm != 0) { texto.add("Su frecuencia cardíaca es de " + valor_bpm.toString() + " latidos por minuto."); }
        else { texto.add("No se a podido establecer su frecuencia cardíaca."); }

        texto.add("Resumen de su ECG.");

        String resultado = "";

        String kaiResult = datos.getKaiResult();
        if (kaiResult.equals("too_short") || kaiResult.equals("too_long\"") ||
            kaiResult.equals("unclassified") || kaiResult.equals("unreadable") || kaiResult.equals("no_analysis")) {
            resultado = resultado + datos.getResultECG() + ".\n\n" + datos.getResultScreenDeterminationInfo();

            texto.add(datos.getResultECG());
            texto.add(datos.getResultScreenDeterminationInfo());

            EnsayoLog.log(FASE, TAG, "GRABACIÓN FALLIDA KAIRESULT " + kaiResult);
        }
        else {
            resultado = resultado + datos.getResultECG() + ".\n\n" + datos.getDetailsECG();
            texto.add(datos.getResultECG());
            texto.add(datos.getDetailsECG());

            EnsayoLog.log(FASE,TAG,"GRABACIÓN DEL ECG CORRECTA");
        }

        setTextView(txtResult,resultado);

        datos.setFilesActivity();

        texto.add("Eviageal ap no puede detectar signos de un ataque cardíaco. Si cree que está sufriendo una emergencia médica, llame a los servicios de urgencia. No cambie su medicación sin consultar con su médico.");

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
                setVisibleView(btContinuar, View.VISIBLE);
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
        if (viewId == R.id.k6l_btReintentar) {
            //region >> Button Reintentar
            btReintentar.setEnabled(false);
            btContinuar.setEnabled(false);
            EVLog.log(TAG, "REINTENTAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "PACIENTE PULSA REINTENTAR");
            startActivity(new Intent(this, get_data2Kardia6L.class));
            finish();
            //endregion
        }
        else if (viewId == R.id.k6l_btContinuar) {
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
            if (texto.size() > 0) {
                if (textToSpeech.isStart() == false) {
                    textToSpeech.speak(texto);
                }
            }
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
            return;
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
        runOnUiThread(new Runnable() {
            public void run() {
//                Log.e(TAG,"Status:" + texto);
                if (view instanceof TextView) {
                    TextView obj = (TextView) view;
                    obj.setText(texto);
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