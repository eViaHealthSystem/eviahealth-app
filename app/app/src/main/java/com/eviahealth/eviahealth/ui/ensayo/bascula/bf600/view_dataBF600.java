package com.eviahealth.eviahealth.ui.ensayo.bascula.bf600;

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

import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.models.SecuenciaActivity;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.beurer.bf600.DataBF600;
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

public class view_dataBF600 extends BaseActivity implements View.OnClickListener {

    private String TAG = "VIEW_BF600";
    final String FASE = "BÁSCULA BF600";
    Button btContinuar, btReintentar;
    ImageView imBateria;
    TextView txtBateria;
    TextView txtGrasaCorporal, txtMasaMagra, txtAguaCorporal, txtBMR, txtMasaMuscular, txtTGV, txtCCI, txtMasaOsea;
    TextView valTxtPeso, valTxtIMC, valTxtGrasaCorporal, valTxtMasaMagra, valTxtAguaCorporal, valTxtBMR, valTxtMasaMuscular, valTxtTGV, valTxtCCI, valTxtMasaOsea;

    int type_puls = 0;
    DataBF600 datos;
    Boolean _status_descarga= false;
    Boolean fgsaltar = false;
    CountDownTimer cTimer = null;
    private Patient paciente = null;
    JSONObject weightMeasure = null;
    TextToSpeechHelper textToSpeech;    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data_bf600);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Mantener la pantala activa
        EVLog.log(TAG,"onCreate()");

        Bundle extras = getIntent().getExtras();
        type_puls = extras.getInt("id_dispositivo");

        //region :: Carga datos del paciente de la DB
        String idpaciente = Config.getInstance().getIdPacienteTablet();
        paciente = ApiMethods.loadCharacteristics(idpaciente);
        if (paciente.getBydefault()) {
            EVLog.log(TAG,"Datos de paciente cargados con valores por defecto");
        }
        //endregion

        //region :: Referencia Views
        btContinuar = findViewById(R.id.BF600_btderecha);
        btReintentar = findViewById(R.id.BF600_btizquierda);

        imBateria = findViewById(R.id.bf600_igbateria);
        imBateria.setVisibility(View.INVISIBLE);
        txtBateria = findViewById(R.id.bf600_txtbateria);
        txtBateria.setVisibility(View.INVISIBLE);

        // labels
        txtGrasaCorporal = findViewById(R.id.bf600_txt_grasa_corporal);
        txtMasaMagra = findViewById(R.id.bf600_txt_masa_magra);
        txtAguaCorporal = findViewById(R.id.bf600_txt_agua_corporal);
        txtBMR = findViewById(R.id.bf600_txt_bmr);
        txtMasaMuscular = findViewById(R.id.bf600_txt_masa_muscular);
        txtTGV = findViewById(R.id.bf600_txt_tgv);
        txtCCI = findViewById(R.id.bf600_txt_cci);
        txtMasaOsea = findViewById(R.id.bf600_txt_masa_osea);

        // valores
        valTxtPeso = findViewById(R.id.bf600_weight);
        valTxtIMC = findViewById(R.id.bf600_imc);
        valTxtGrasaCorporal = findViewById(R.id.bf600_grasa_corporal);
        valTxtMasaMagra = findViewById(R.id.bf600_masa_magra);
        valTxtAguaCorporal = findViewById(R.id.bf600_agua_corporal);
        valTxtBMR = findViewById(R.id.bf600_bmr);
        valTxtMasaMuscular = findViewById(R.id.bf600_masa_muscular);
        valTxtTGV = findViewById(R.id.bf600_tgv);


        valTxtCCI = findViewById(R.id.bf600_cci);
        valTxtMasaOsea = findViewById(R.id.bf600_masa_osea);

        setVisibleView(valTxtTGV,View.INVISIBLE);
        setVisibleView(txtTGV,View.INVISIBLE);
        setVisibleView(txtCCI,View.INVISIBLE);
        setVisibleView(valTxtCCI,View.INVISIBLE);

        setVisibleBodyFat(View.INVISIBLE);
        //endregion

        // texto >> Voz
        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = DataBF600.getInstance();
        fgsaltar = false;

        EnsayoLog.log(FASE,TAG,"Se ha descargado la medición de su báscula.");

        //region :: Comprobación del nivel de batería
        Integer battery = datos.getBattery();
        EVLog.log(TAG,"barrety_percent: " + datos.getBattery().toString());

        if (battery == null) {
            EnsayoLog.log(FASE,TAG,"Batería Báscula BAJA");
            imBateria.setVisibility(View.VISIBLE);
            txtBateria.setVisibility(View.VISIBLE);
        }
        else if (battery <10){
            EnsayoLog.log(FASE,TAG,"Batería Báscula BAJA: " + battery +"%");
            imBateria.setVisibility(View.VISIBLE);
            txtBateria.setVisibility(View.VISIBLE);
        }
        //endregion

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
                TextView txtLabelIMC = findViewById(R.id.bf600_label_imc);
                setVisibleView(txtLabelIMC,View.INVISIBLE);
            }

            JSONObject bodyComposition = new JSONObject(datos.getBodyComposition());

            // Si la medicion corresponde a un multipaciente no se visualiza la grasa corporal
            if (Config.getInstance().getMultipaciente() == false) {
                if (bodyComposition.has("impedance")) {
                    if (bodyComposition.getInt("impedance") == 0) {
                        setVisibleBodyFat(View.INVISIBLE);
                    } else {
                        setVisibleBodyFat(View.VISIBLE);

                        // Grasa Corporal
                        if (bodyComposition.has("body_fat")) {
                            setTextView(valTxtGrasaCorporal, bodyComposition.getDouble("body_fat") + " %");
                        } else {
                            setTextView(valTxtGrasaCorporal, "---");
                        }

                        // BRM y Masa Magra
                        if (bodyComposition.has("bmr")) {
                            Integer bmr = bodyComposition.getInt("bmr");
                            setTextView(valTxtBMR, "" + bmr);
                            setTextView(valTxtMasaMagra, datos.calculateMasaMagra(bmr) + " kg");
                        } else {
                            setTextView(valTxtBMR, "---");
                            setTextView(valTxtMasaMagra, "---");
                        }

                        // Agua Corporal
                        if (bodyComposition.has("water")) {
                            setTextView(valTxtAguaCorporal, bodyComposition.getDouble("water") + " %");
                        } else {
                            setTextView(valTxtAguaCorporal, "---");
                        }

                        // Masa Muscular
                        if (bodyComposition.has("muscle_percentage")) {
                            setTextView(valTxtMasaMuscular, bodyComposition.getDouble("muscle_percentage") + " %");
                        } else {
                            setTextView(valTxtMasaMuscular, "---");
                        }

                        // Masa Osea
                        if (bodyComposition.has("boneMass")) {
                            setTextView(valTxtMasaOsea, bodyComposition.getDouble("boneMass") + " kg");
                        } else {
                            setTextView(valTxtMasaOsea, "---");
                        }

                        // TGV >> Tejido Graso Visceral o Grasa visceral
//                    Double weight = weightMeasure.getDouble("weight");
//                    Double height = (double)paciente.getHeight() / 100.0f;
//                    int sex = (paciente.getGender().equals("Hombre")) ? 0 : 1;
//                    float visceral_fat_grade = datos.getVisceralFat(weight.floatValue(),height.floatValue(),paciente.getAge(),sex);
//                    Double tgv = datos.getTGV(visceral_fat_grade, bodyComposition.getDouble("boneMass"));
//                    setTextView(valTxtTGV, "" + tgv.intValue());

                    }
                } else {
                    setVisibleBodyFat(View.INVISIBLE);
                }
            }
            else {
                setVisibleBodyFat(View.INVISIBLE);
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
        if (viewId == R.id.BF600_btizquierda) {

            //region >> Button Reintentar
            btReintentar.setEnabled(false);
            btContinuar.setEnabled(false);
            EVLog.log(TAG, "REINTENTAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "PACIENTE PULSA REINTENTAR");
            startActivity(new Intent(this, get_dataBF600.class));
            finish();
            //endregion
        }
        else if (viewId == R.id.BF600_btderecha) {
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
        else if (viewId == R.id.imageButtonAudio_bf600) {
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

    private void setVisibleBodyFat(int state) {
        //labels
        setVisibleView(txtGrasaCorporal,state);
        setVisibleView(txtMasaMagra,state);
        setVisibleView(txtAguaCorporal,state);
        setVisibleView(txtBMR,state);
        setVisibleView(txtMasaMuscular,state);
//        setVisibleView(txtTGV,state);
//        setVisibleView(txtCCI,state);
        setVisibleView(txtMasaOsea,state);

        // valores
        setVisibleView(valTxtGrasaCorporal,state);
        setVisibleView(valTxtMasaMagra,state);
        setVisibleView(valTxtAguaCorporal,state);
        setVisibleView(valTxtBMR,state);
        setVisibleView(valTxtMasaMuscular,state);
//        setVisibleView(valTxtTGV,state);
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