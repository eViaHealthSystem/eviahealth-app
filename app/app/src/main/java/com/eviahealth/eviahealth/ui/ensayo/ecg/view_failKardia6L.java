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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class view_failKardia6L extends BaseActivity implements View.OnClickListener {

    private String TAG = "VIEW_FAIL_K6L";
    final String FASE = "KARDIAMOBILE 6L";
    Button btContinuar, btReintentar;
    TextView txtCodeError, txtDetalles;
    KardiaData datos;
    Boolean fgsaltar = false;
    CountDownTimer cTimer = null;
    TextToSpeechHelper textToSpeech;
    List<String> texto = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_fail_kardia6_l);

        //region :: Referencia Views
        txtCodeError = findViewById(R.id.txt_code_error);
        txtDetalles = findViewById(R.id.txt_details);

        btContinuar = findViewById(R.id.buttonContinuar);
        btReintentar = findViewById(R.id.buttonReintentar);
        setVisibleView(btReintentar, View.INVISIBLE);
        setVisibleView(btContinuar, View.INVISIBLE);
        //endregion

        // texto >> Voz
        textToSpeech = new TextToSpeechHelper(getApplicationContext());
        texto.clear();

        texto.add("Error al realizar la grabación del ECG.");

        datos = KardiaData.getInstance();
        fgsaltar = false;
        EVLog.log(TAG,"Detectado error en la actividad de grabación del ecg.");
        EVLog.log(TAG,"datos fail: " + datos.getMsgfail());

        EnsayoLog.log(FASE,TAG,"REALIZADA GRABACIÓN DEL ECG");

        try {
            JSONObject msgfail = new JSONObject(datos.getMsgfail());
            if (msgfail.has("error")) {

                Integer err = msgfail.getInt("error");
                setTextView(txtCodeError,err.toString());

                EnsayoLog.log(FASE, TAG, "GRABACIÓN FALLIDA CODE-ERROR " + err.toString());

                texto.add("Code Error número " + err.toString() + ".");
            }

            if (msgfail.has("fail")) {
                Log.e(TAG,"fail ref: " + msgfail.getString("fail"));
            }

            if (msgfail.has("details")) {
                setTextView(txtDetalles,msgfail.getString("details"));
                texto.add("Detalles del error. ");
                texto.add(msgfail.getString("details") + ".");
            }

        } catch (JSONException e) {
            e.printStackTrace();

            texto.add("Code Error número no defindo.");
            setTextView(txtCodeError,"---");
            texto.add("Detalles del error no defindos.");
            setTextView(txtDetalles,"---");

        }

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

//                if (datos.getInverter() != null) {
//                    if (datos.getInverter() == true) { setVisibleView(btContinuar, View.INVISIBLE); }
//                    else { setVisibleView(btContinuar, View.VISIBLE); }
//                }
//                else { setVisibleView(btContinuar, View.VISIBLE); }
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
        if (viewId == R.id.buttonReintentar) {
            //region >> Button Reintentar
            btReintentar.setEnabled(false);
            btContinuar.setEnabled(false);
            EVLog.log(TAG, "REINTENTAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "PACIENTE PULSA REINTENTAR");
            startActivity(new Intent(this, get_data2Kardia6L.class));
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