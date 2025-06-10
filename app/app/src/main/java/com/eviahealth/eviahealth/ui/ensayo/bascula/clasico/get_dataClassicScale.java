package com.eviahealth.eviahealth.ui.ensayo.bascula.clasico;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.models.manual.DataClassicScale;
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

public class get_dataClassicScale extends BaseActivity implements View.OnClickListener, NumberPicker.OnValueChangeListener {

    final static String TAG = "get_dataClassicScale";
    final static String FASE = "SCALE";
    NumberPicker pckCentenas,pckDecenas, pckUnidades, pckDecimal;
    Button btContinuar;
    TextToSpeechHelper textToSpeech;
    DataClassicScale datos;
    Boolean viewdata = false;               // Para evitar que se lance dos veces la siguiente actividad
    CountDownTimer cTimer = null;           // TimeOut para que se realice la medición de peso
    CountDownTimer cTimerContinuar = null;  // Timer para mostrar con retardo el button de continuar

    private Patient paciente = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_data_classic_scale);
        EVLog.log(TAG,"onCreate()");

        //region :: views
        int size = 90;
        pckCentenas = findViewById(R.id.scale_PesoCentenas);
        pckCentenas.setMaxValue(1);
        pckCentenas.setMinValue(0);
        pckCentenas.setValue(0);
        pckCentenas.setTextSize(size);
        pckCentenas.setOnValueChangedListener(this);

        pckDecenas = findViewById(R.id.scale_PesoDecenas);
        pckDecenas.setMaxValue(9);
        pckDecenas.setMinValue(0);
        pckDecenas.setValue(8);
        pckDecenas.setTextSize(size);
        pckDecenas.setOnValueChangedListener(this);

        pckUnidades = findViewById(R.id.scale_PesoUnidades);
        pckUnidades.setMaxValue(9);
        pckUnidades.setMinValue(0);
        pckUnidades.setValue(0);
        pckUnidades.setTextSize(size);
        pckUnidades.setOnValueChangedListener(this);

        NumberPicker pckPUNTO = findViewById(R.id.PUNTO);
        String[] valuesPUNTO= {"."};
        pckPUNTO.setDisplayedValues(valuesPUNTO);
        pckPUNTO.setTextSize(size);

        pckDecimal = findViewById(R.id.scale_PesoDecimal);
        pckDecimal.setMaxValue(9);
        pckDecimal.setMinValue(0);
        pckDecimal.setValue(0);
        pckDecimal.setTextSize(size);
        pckDecimal.setOnValueChangedListener(this);


        btContinuar = findViewById(R.id.scale_btderecha);
        setVisibleView(btContinuar,View.INVISIBLE);
        //endregion

        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = DataClassicScale.getInstance();
        datos.clear();

        viewdata = false;

        //region :: Carga datos del paciente de la DB
//        String idpaciente = Config.getInstance().getIdPacienteTablet();
        String idpaciente = Config.getInstance().getIdPacienteEnsayo();
        paciente = ApiMethods.loadCharacteristics(idpaciente);
        if (paciente.getBydefault()) {
            EVLog.log(TAG,"Datos de paciente cargados con valores por defecto");
        }
        //endregion

        //region :: Carga valores de peso
        int weight = paciente.getWeight();
        pckCentenas.setValue(weight / 100);
        pckDecenas.setValue((weight % 100) / 10);
        pckUnidades.setValue(weight % 10);
        pckDecimal.setValue(0);
        //endregion

        //region :: Timer para mostrar el botón de contuniar al minuto si no se toca el valor del peso
        cTimerContinuar = new CountDownTimer(1000 * 60, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimerContinuar.cancel();
                cTimerContinuar = null;
                setVisibleView(btContinuar,View.VISIBLE);
            }
        };
        cTimerContinuar.start();
        //endregion

    }

    @Override
    public void onClick(View view) {
        Log.e(TAG, "onClick()");

        int viewId = view.getId();
        if (viewId == R.id.imageButtonAudio_scale) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            texto.add("Realice la medida de peso con su báscula y a continuación seleccione el valor que ha obtenido.");
            textToSpeech.speak(texto);
            //endregion
        }
        else if (viewId == R.id.scale_btderecha) {
            //region :: CONTINUAR
            EVLog.log(TAG, "CONTINUAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "PACIENTE PULSA CONTINUAR PESO INTRODUCIDO");

            datos.setStatusDescarga(true);

            Double weight = (double) ((pckCentenas.getValue() * 100) + (pckDecenas.getValue() * 10) + pckUnidades.getValue() + (pckDecimal.getValue() * 0.1f));
            Integer height = paciente.getHeight();
            Double imc = weight / ((height/100.0f) * (height/100.0f));

            try {
                JSONObject obj = new JSONObject();
                obj.put("weight", util.roundDouble(weight));
                obj.put("imc", util.roundDouble(imc));
                datos.setWeightMeasurement(obj.toString());
            }
            catch (JSONException ex) {
                ex.printStackTrace();
                datos.setStatusDescarga(false);
            }

            textToSpeech.shutdown();

            viewResult();
            //endregion
        }
    }

    public void viewResult() {

        if (viewdata == false) {
            viewdata = true;
            EVLog.log(TAG, " ViewResult()");

            //region .. stop timers
            if (cTimer != null) cTimer.cancel();
            cTimer = null;

            if (cTimerContinuar != null) cTimerContinuar.cancel();
            cTimerContinuar = null;
            //endregion


            if (datos.getStatusDescarga()) {
                Intent intent = new Intent(this, view_dataClassicScale.class);
//                intent.putExtra("typeScale", TypeDevice);
                startActivity(intent);
            }
            else {
/*                Intent intent = new Intent(this, view_failBF600.class);
                intent.putExtra("typeScale", TypeDevice);*/
//                startActivity(intent);
            }

            Log.e(TAG,"datos.getStatusDescarga(): " + datos.getStatusDescarga());

            finish();
        }
    }

    //region :: Ciclo de vida
    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        EVLog.log(TAG, "onDestroy()");
        super.onDestroy();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){
    }
    @Override
    protected void onResume() {
        super.onResume();
        EVLog.log(TAG, "onResume()");

        //region :: Comprobación: si la fecha que se inició el ensayo en la actual
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
        //endregion

    }
    //endregion

    //region :: Eventos NumberPicker
    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        // Aquí puedes manejar el evento de cambio de valor
//        Toast.makeText(this, "Valor seleccionado: " + newVal, Toast.LENGTH_SHORT).show();
        Log.e(TAG,"Valor seleccionado: " + newVal);

        if (btContinuar.getVisibility() == View.INVISIBLE) {
            setVisibleView(btContinuar, View.VISIBLE);
            datos.setStatusDescarga(true);
        }

    }
    //endregion
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