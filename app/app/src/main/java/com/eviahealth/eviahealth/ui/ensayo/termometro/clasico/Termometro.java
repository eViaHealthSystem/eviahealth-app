package com.eviahealth.eviahealth.ui.ensayo.termometro.clasico;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.models.SecuenciaActivity;
import com.eviahealth.eviahealth.utils.Fecha.Fecha;
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

public class Termometro extends BaseActivity implements View.OnClickListener {
    final String TAG = "TERMO-DATA ";
    final String FASE = "TERMÓMETRO";
    private NumberPicker picker;
    private Button btn;
    TextToSpeechHelper textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_termometro);
        EVLog.log(TAG,"onCreate()");

        btn =findViewById(R.id.continuarbbdd);
        btn.setVisibility(View.INVISIBLE);

        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        final NumberPicker picker = findViewById(R.id.termometroPicker);
        this.picker = picker;

        picker.setWrapSelectorWheel(false);
        picker.setMaxValue(TermometroPickerValues.values.length - 1);
        picker.setMinValue(0);
        picker.setValue(TermometroPickerValues.values.length - 1);
        Log.e("VALUES", "" + TermometroPickerValues.values.length);
        Log.e("VALUES", "" + TermometroPickerValues.values[109]);
        picker.setDisplayedValues(TermometroPickerValues.display_values);

        picker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btn.setVisibility(View.VISIBLE);
                btn.setEnabled(true);
            }
        });
        picker.setOnScrollListener(new NumberPicker.OnScrollListener() {
            @Override
            public void onScrollStateChange(NumberPicker numberPicker, int i) {
                btn.setVisibility(View.VISIBLE);
                btn.setEnabled(true);
            }
        });
        picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                btn.setVisibility(View.VISIBLE);
                btn.setEnabled(true);
            }
        });

        picker.setBackgroundResource(R.drawable.marco_picker);
        picker.setValue(100); // equivalente 35 oC

        EnsayoLog.log(FASE,TAG,"Medición de la TEMPERATURA del PACIENTE");
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.imageButtonAudio) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            texto.add("Realice la medida de temperatura y seleccione el valor que ha obtenido en la lista.");
            textToSpeech.speak(texto);
            //endregion
        }
    }

    public void Siguiente(View v){
        btn.setEnabled(false);
        picker.setEnabled(false);

        String date2 = Fecha.getFechaYHoraActual();
        JSONObject json = new JSONObject();

        try {
            EVLog.log(TAG,"onClick() >> VALOR: " + TermometroPickerValues.values[this.picker.getValue()]);
            EnsayoLog.log(FASE,TAG,"DATO DE TERMOMETRO INTRODUCIDO");
//            EnsayoLog.log(FASE,TAG,"VALOR: " + TermometroPickerValues.values[this.picker.getValue()]);

            json.put("idpaciente", Config.getInstance().getIdPacienteEnsayo());
            json.put("fecha", date2);
            json.put("temperatura", TermometroPickerValues.values[this.picker.getValue()]);
            FileAccess.escribirJSON(FilePath.REGISTROS_TERMOMETRO, json);

        } catch (JSONException | IOException err) {
            Log.d("Error", err.toString());
        }

        ir_siguiente_actividad();
    }

    private void ir_siguiente_actividad() {
        SecuenciaActivity.getInstance().next(this);
    }

    @Override
    protected void onDestroy() {
        EVLog.log(TAG,"onDestroy()");
        textToSpeech.shutdown();
        super.onDestroy();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}

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
