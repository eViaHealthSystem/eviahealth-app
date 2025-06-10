package com.eviahealth.eviahealth.ui.ensayo.bascula.gbs2012b;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.models.transtek.gbs2012b.DataGBS2012B;
import com.eviahealth.eviahealth.models.SecuenciaActivity;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;
import com.eviahealth.eviahealth.models.vitalograph.Patient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class view_dataGBS2012B extends BaseActivity implements View.OnClickListener {

    private String TAG = "VIEW_DATA_GBS2012B";
    final String FASE = "GBS2012B SCALE";
    Button btContinuar, btReintentar;
    TextView valTxtPeso, valTxtIMC;
    DataGBS2012B datos;
    Boolean fgsaltar = false;
    CountDownTimer cTimer = null;
    private Patient paciente = null;
    JSONObject weightMeasure = null;
    TextToSpeechHelper textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data_gbs2012_b);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Mantener la pantala activa
        EVLog.log(TAG,"onCreate()");

        //region :: Carga datos del paciente de la DB
        String idpaciente = Config.getInstance().getIdPacienteTablet();
        paciente = ApiMethods.loadCharacteristics(idpaciente);
        if (paciente.getBydefault()) {
            EVLog.log(TAG,"Datos de paciente cargados con valores por defecto");
        }
        //endregion

        //region :: Referencia Views
        btContinuar = findViewById(R.id.gbs2012b_btderecha);
        btReintentar = findViewById(R.id.gbs2012b_btizquierda);

        // valores
        valTxtPeso = findViewById(R.id.gbs2012b_weight);
        valTxtIMC = findViewById(R.id.gbs2012b_imc);
        //endregion

        // texto >> Voz
        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = DataGBS2012B.getInstance();
        fgsaltar = false;

        EnsayoLog.log(FASE,TAG,"Su medición de peso es:");

        // Carga datos
        try {

            weightMeasure = new JSONObject(datos.getWeightMeasurement());

            if (weightMeasure.has("weight")) {
                setTextView(valTxtPeso, "" + weightMeasure.getDouble("weight"));
            } else {
                setTextView(valTxtPeso, "---");
            }

            if (weightMeasure.has("imc")) {
                setTextView(valTxtIMC, "" + weightMeasure.getDouble("imc"));
            } else {
                setTextView(valTxtIMC, "---");
            }

            // Si la medicion corresponde a un multipaciente no se visualiza IMC
            if (Config.getInstance().getMultipaciente() == true) {
                setVisibleView(valTxtIMC,View.INVISIBLE);
                TextView txtLabelIMC = findViewById(R.id.gbs2012b_label_imc);
                setVisibleView(txtLabelIMC,View.INVISIBLE);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

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
        if (viewId == R.id.gbs2012b_btizquierda) {

            //region >> Button Reintentar
            btReintentar.setEnabled(false);
            btContinuar.setEnabled(false);
            EVLog.log(TAG, "REINTENTAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "PACIENTE PULSA REINTENTAR");
            startActivity(new Intent(this, get_dataGBS2012B.class));
            finish();
            //endregion
        }
        else if (viewId == R.id.gbs2012b_btderecha) {
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

            datos.setFilesActivity();

            ir_siguiente_actividad();

            //endregion
        }
        else if (viewId == R.id.imageButtonAudio_gbs2012b) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            texto.add("Los datos obtenidos de su báscula son:");

            try {

//                Log.e(TAG,"datos: " + weightMeasure.toString());

                //region >> PESO
                Double peso = weightMeasure.getDouble("weight");
                String tmp = peso.toString();
                String[] dev = tmp.split("\\.");
                String frase = "Su peso actual es de " + dev[0].toString() + " con ";
                if (dev.length > 1) { frase = frase + dev[1].toString() ; }
                frase = frase + " kg.";
                texto.add(frase);
                //endregion

                //region >> IMC
                if (Config.getInstance().getMultipaciente() == false) {
                    Double imc = weightMeasure.getDouble("imc");
                    tmp = imc.toString();
                    String[] dev2 = tmp.split("\\.");
                    frase = "Su indice de masa corporal actual es de " + dev2[0].toString() + " con ";
                    if (dev2.length > 1) {
                        frase = frase + dev2[1].toString();
                    }
                    texto.add(frase);
                }
                //endregion
            }
            catch (JSONException e) {
                e.printStackTrace();
                texto.add("No tengo datos disponibles");
            }
            finally {
                textToSpeech.speak(texto);
            }
            //endregion
        }
    }

    private void ir_siguiente_actividad() {
        SecuenciaActivity.getInstance().next(this);
        finish();
    }

    //region :: Activity LifeCycle
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
    //endregion

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