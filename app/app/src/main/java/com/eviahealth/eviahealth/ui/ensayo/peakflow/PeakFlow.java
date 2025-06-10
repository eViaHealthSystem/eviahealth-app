package com.eviahealth.eviahealth.ui.ensayo.peakflow;


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

public class PeakFlow extends BaseActivity implements View.OnClickListener {
    final String TAG = "PEAK-VIEW ";
    final String FASE = "PEAK FLOW";
    Button btnsiguientepeak;
    //
    NumberPicker picker;
    TextToSpeechHelper textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peakflow);
        EVLog.log(TAG,"onCreate()");

        btnsiguientepeak = findViewById(R.id.btderecha_peak);
        btnsiguientepeak.setVisibility(View.INVISIBLE);

        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        final NumberPicker picker = findViewById(R.id.peakflowPicker);
        this.picker = picker;
        picker.setWrapSelectorWheel(false);
        picker.setMaxValue(PeakFlowPickerValues.values.length - 1);
        picker.setMinValue(0);
        picker.setValue(PeakFlowPickerValues.values.length - 1);
        picker.setDisplayedValues(PeakFlowPickerValues.display_values);

        picker.setOnScrollListener(new NumberPicker.OnScrollListener() {
            @Override
            public void onScrollStateChange(NumberPicker numberPicker, int i) {
                btnsiguientepeak.setVisibility(View.VISIBLE);
                btnsiguientepeak.setEnabled(true);
            }
        });

        picker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnsiguientepeak.setVisibility(View.VISIBLE);
                btnsiguientepeak.setEnabled(true);
            }
        });

        picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int oldVal, int newVal) {
                Log.e("scroll","val: " + newVal);
                btnsiguientepeak.setVisibility(View.VISIBLE);
                btnsiguientepeak.setEnabled(true);
                int value = PeakFlowPickerValues.values[newVal];
                if( value < 250 ){
                    picker.setBackgroundResource(R.drawable.rojo2);
                }
                else if(value == 250){
                    picker.setBackgroundResource(R.drawable.rojonaranja);
                }
                else if( value < 450){
                    picker.setBackgroundResource(R.drawable.naranja3);
                }
                else if(value == 450){
                    picker.setBackgroundResource(R.drawable.amarilloverde);
                }
                else if(value > 450){
                    picker.setBackgroundResource(R.drawable.verdedegradado2);
                }
            }
        });

        picker.setBackgroundResource(R.drawable.marco_picker);

        EnsayoLog.log(FASE,TAG,"Medición del PEAK FLOW del PACIENTE");

    }

    // Boton avanzar actividad
    public void Siguiente(View v){
        btnsiguientepeak.setEnabled(false);
        picker.setEnabled(false);

        String date = Fecha.getFechaYHoraActual();
        JSONObject resultado = new JSONObject();
        try {
            EVLog.log(TAG,"onClick() >> VALOR: " + PeakFlowPickerValues.values[this.picker.getValue()]);

            EnsayoLog.log(FASE,TAG,"DATOS DEL PEAK FLOW INTRODUCIDOS");
//            EnsayoLog.log(FASE,TAG,"VALOR: " + PeakFlowPickerValues.values[this.picker.getValue()]);
            resultado.put("idpaciente", Config.getInstance().getIdPacienteEnsayo());
            resultado.put("fecha", date);
            resultado.put("rango", PeakFlowPickerValues.values[this.picker.getValue()]);

            FileAccess.escribirJSON(FilePath.REGISTROS_PEAKFLOW, resultado);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ir_siguiente_actividad();
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

    private void ir_siguiente_actividad() {
        SecuenciaActivity.getInstance().next(this);
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
            EVLog.log(TAG, "FILEACCESS READ: IOException: " + ex.toString());
            ir_siguiente_actividad();
        }
    }

    @Override
    protected  void onPause() {
        super.onPause();
        EVLog.log(TAG,"onPause()");
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.imageButtonAudio) {
            //region :: LEER TEXTO
            List<String> texto = new ArrayList<>();
            texto.add("Realice la medida del flujo espiratorio máximo y seleccione el valor que ha obtenido en la lista.");
            textToSpeech.speak(texto);
            //endregion
        }
    }
}
