package com.eviahealth.eviahealth.ui.ensayo.bascula.hs2s;

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
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.models.ihealth.hs2s.DataBascula;
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

public class view_dataHS2S extends BaseActivity implements View.OnClickListener{

    private String TAG = "VIEW_HS2S";
    final String FASE = "BÁSCULA HS2S";
    Button btContinuar, btReintentar;
    ImageView imBateria;
    TextView txtBateria;
    TextView txtGrasaCorporal, txtMasaMagra, txtAguaCorporal, txtBMR, txtMasaMuscular, txtTGV, txtCCI, txtMasaOsea, txtHitorico;
    TextView valTxtPeso, valTxtIMC, valTxtGrasaCorporal, valTxtMasaMagra, valTxtAguaCorporal, valTxtBMR, valTxtMasaMuscular, valTxtTGV, valTxtCCI, valTxtMasaOsea;

    int type_puls = 0;
    JSONObject bodyfat = null;
    DataBascula datos;
    Boolean _status_descarga= false;
    Boolean fgsaltar = false;
    CountDownTimer cTimer = null;
    TextToSpeechHelper textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data_hs2s);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Mantener la pantala activa

        EVLog.log(TAG,"onCreate()");
        Bundle extras = getIntent().getExtras();
        type_puls = extras.getInt("id_dispositivo");

        //region :: Referencia Views
        btContinuar = findViewById(R.id.HS2S_btderecha);
        btReintentar = findViewById(R.id.HS2S_btizquierda);

        imBateria = findViewById(R.id.HS2S_igbateria);
        imBateria.setVisibility(View.INVISIBLE);
        txtBateria = findViewById(R.id.HS2S_txtbateria);
        txtBateria.setVisibility(View.INVISIBLE);

        // labels
        txtHitorico = findViewById(R.id.hs2s_txt_hitorico);
        txtHitorico.setVisibility(View.INVISIBLE);

        txtGrasaCorporal = findViewById(R.id.hs2s_txt_grasa_corporal);
        txtMasaMagra = findViewById(R.id.hs2s_txt_masa_magra);
        txtAguaCorporal = findViewById(R.id.hs2s_txt_agua_corporal);
        txtBMR = findViewById(R.id.hs2s_txt_bmr);
        txtMasaMuscular = findViewById(R.id.hs2s_txt_masa_muscular);
        txtTGV = findViewById(R.id.hs2s_txt_tgv);
        txtCCI = findViewById(R.id.hs2s_txt_cci);
        txtMasaOsea = findViewById(R.id.hs2s_txt_masa_osea);

        // valores
        valTxtPeso = findViewById(R.id.hs2s_weight);
        valTxtIMC = findViewById(R.id.hs2s_imc  );
        valTxtGrasaCorporal = findViewById(R.id.hs2s_grasa_corporal);
        valTxtMasaMagra = findViewById(R.id.hs2s_masa_magra);
        valTxtAguaCorporal = findViewById(R.id.hs2s_agua_corporal);
        valTxtBMR = findViewById(R.id.hs2s_bmr);
        valTxtMasaMuscular = findViewById(R.id.hs2s_masa_muscular);
        valTxtTGV = findViewById(R.id.hs2s_tgv);
        valTxtCCI = findViewById(R.id.hs2s_cci);
        valTxtMasaOsea = findViewById(R.id.hs2s_masa_osea);

        setVisibleView(txtCCI,View.INVISIBLE);
        setVisibleView(valTxtCCI,View.INVISIBLE);
        setVisibleBodyFat(View.INVISIBLE);
        //endregion

        // texto >> Voz
        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = DataBascula.getInstance();
        fgsaltar = false;

        EnsayoLog.log(FASE,TAG,"Se ha descargado la medición de su báscula.");

        //region :: Comprobación del nivel de batería
        Integer battery = datos.getBattery();
        EVLog.log(TAG,"barrety_percent: " + datos.getBattery());

        if (battery == null) {
            EnsayoLog.log(FASE,TAG,"Batería Báscula BAJA");
            imBateria.setVisibility(View.VISIBLE);
            txtBateria.setVisibility(View.VISIBLE);
        }
        else if (battery < 20){
            EnsayoLog.log(FASE,TAG,"Batería Báscula BAJA: " + battery +"%");
            imBateria.setVisibility(View.VISIBLE);
            txtBateria.setVisibility(View.VISIBLE);
        }
        //endregion

        Boolean statusOnline = datos.getStatusOnline();
        Boolean statusBodyFat = datos.getStatusBodyFat();
        Boolean statusDataHistory = datos.getStatusDataHistory();
        Log.e(TAG, "Status Flags: " + datos.getStatusFlags());

        String data = "{}";
        if (statusBodyFat == true) {
            data = datos.getBodyFatResult();
            Log.e(TAG, "data: " + data);
        }
        else if (statusOnline == true) {
            data = datos.getOnlineResul();
            Log.e(TAG, "data: " + data);
        }

        // Carga datos
        String measure = "";
        String fecha = "";
        try {
            if (statusOnline || statusBodyFat) { measure = datos.generateResult(data); }
            else { measure = datos.generateResultHistorico(); }

            bodyfat = new JSONObject(measure);

            if (bodyfat.has("weight")) {
                setTextView(valTxtPeso, "" + bodyfat.getDouble("weight"));
            } else {
                setTextView(valTxtPeso, "---");
            }

            if (bodyfat.has("imc")) {
                setTextView(valTxtIMC, "" + bodyfat.getDouble("imc"));
            } else {
                setTextView(valTxtIMC, "---");
            }

            // Si la medicion corresponde a un multipaciente no se visualiza IMC
            if (Config.getInstance().getMultipaciente() == true) {
                setVisibleView(valTxtIMC,View.INVISIBLE);
                TextView txtLabelIMC = findViewById(R.id.hs2s_label_imc);
                setVisibleView(txtLabelIMC,View.INVISIBLE);
            }

            if (bodyfat.has("grasa_corporal")) {
                setTextView(valTxtGrasaCorporal, bodyfat.getDouble("grasa_corporal") + " %");
            } else {
                setTextView(valTxtGrasaCorporal, "---");
            }

            if (bodyfat.has("masa_magra")) {
                setTextView(valTxtMasaMagra, bodyfat.getDouble("masa_magra") + " kg");
            } else {
                setTextView(valTxtMasaMagra, "---");
            }

            if (bodyfat.has("agua_corporal")) {
                setTextView(valTxtAguaCorporal, bodyfat.getDouble("agua_corporal") + " %");
            } else {
                setTextView(valTxtAguaCorporal, "---");
            }

            if (bodyfat.has("bmr")) {
                setTextView(valTxtBMR, "" + bodyfat.getInt("bmr"));
            } else {
                setTextView(valTxtBMR, "---");
            }

            if (bodyfat.has("masa_muscular")) {
                setTextView(valTxtMasaMuscular, bodyfat.getDouble("masa_muscular") + " kg");
            } else {
                setTextView(valTxtMasaMuscular, "---");
            }

            if (bodyfat.has("tgv")) {
                setTextView(valTxtTGV, "" + bodyfat.getInt("tgv"));
            } else {
                setTextView(valTxtTGV, "---");
            }

            if (bodyfat.has("masa_osea")) {
                setTextView(valTxtMasaOsea, bodyfat.getDouble("masa_osea") + " kg");
            } else {
                setTextView(valTxtMasaOsea, "---");
            }

            // esta solo se pone en histórico
            if (bodyfat.has("fecha")) {
                fecha = bodyfat.getString("fecha");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Si la medicion corresponde a un multipaciente no se visualiza la grasa corporal
        if (statusBodyFat == true && Config.getInstance().getMultipaciente() == false) {
            if (datos.getInstructionType()) {
                // muestra labels de medición de grasa corporal
                setVisibleBodyFat(View.VISIBLE);
            }
        }
        else if (statusDataHistory == true && statusOnline == false && statusBodyFat == false) {
            setVisibleView(txtHitorico,View.VISIBLE);
            setTextView(txtHitorico,"Los datos que se muestran son del día: " + fecha);
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
        if (viewId == R.id.HS2S_btizquierda) {

            //region >> Button Reintentar
            btReintentar.setEnabled(false);
            btContinuar.setEnabled(false);
            EVLog.log(TAG, "REINTENTAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "PACIENTE PULSA REINTENTAR");
            startActivity(new Intent(this, get_dataHS2S.class));
            finish();
            //endregion
        }
        else if (viewId == R.id.HS2S_btderecha) {
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
        else if (viewId == R.id.imageButtonAudio_hs2s) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            texto.add("Los datos obtenidos de su báscula son:");

            try {

                Log.e(TAG,"datos: " + bodyfat.toString());

                //region >> PESO
                Double peso = bodyfat.getDouble("weight");
                String tmp = peso.toString();
                String[] dev = tmp.split("\\.");
                String frase = "Su peso actual es de " + dev[0].toString() + " con ";
                if (dev.length > 1) { frase = frase + dev[1].toString() ; }
                frase = frase + " kg.";
                texto.add(frase);
                //endregion

                //region >> IMC
                if (Config.getInstance().getMultipaciente() == false) {
                    Double imc = bodyfat.getDouble("imc");
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

    private void setVisibleBodyFat(int state) {
        //labels
        setVisibleView(txtGrasaCorporal,state);
        setVisibleView(txtMasaMagra,state);
        setVisibleView(txtAguaCorporal,state);
        setVisibleView(txtBMR,state);
        setVisibleView(txtMasaMuscular,state);
        setVisibleView(txtTGV,state);
//        setVisibleView(txtCCI,state);
        setVisibleView(txtMasaOsea,state);

        // valores
        setVisibleView(valTxtGrasaCorporal,state);
        setVisibleView(valTxtMasaMagra,state);
        setVisibleView(valTxtAguaCorporal,state);
        setVisibleView(valTxtBMR,state);
        setVisibleView(valTxtMasaMuscular,state);
        setVisibleView(valTxtTGV,state);
//        setVisibleView(valTxtCCI,state);
        setVisibleView(valTxtMasaOsea,state);
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

    private void setEnableView(View view, boolean state) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (view instanceof TextView) {
                    TextView obj = (TextView) view;
                    obj.setEnabled(state);
                }
                else if (view instanceof EditText) {
                    EditText obj = (EditText) view;
                    obj.setEnabled(state);
                }
                else if (view instanceof Button) {
                    Button obj = (Button) view;
                    obj.setEnabled(state);
                }
                else if (view instanceof ImageView) {
                    ImageView obj = (ImageView) view;
                    obj.setEnabled(state);
                }
                else if (view instanceof ProgressBar) {
                    ProgressBar obj = (ProgressBar) view;
                    obj.setEnabled(state);
                }
                else if (view instanceof NumberPicker) {
                    NumberPicker obj = (NumberPicker) view;
                    obj.setEnabled(state);
                }
                else if (view instanceof Spinner) {
                    Spinner obj = (Spinner) view;
                    obj.setEnabled(state);
                }
            }
        });
    }
}


