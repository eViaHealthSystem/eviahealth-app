package com.eviahealth.eviahealth.ui.admin.fichapaciente;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FileFuntions;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.utils.dialogs.Alerts;
import com.eviahealth.eviahealth.utils.util;
import com.eviahealth.eviahealth.models.vitalograph.Patient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class CaracteristicasPaciente extends BaseActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    final static String TAG = "CaracteristicasPaciente";
    private Spinner cmbGenero;
    private EditText etFechaNacimiento;
    private NumberPicker pckCentenas,pckDecenas, pckUnidades, pckDecimal, pckAltura, pckEdad;
    private Integer weight = 0;
    private Integer height = 0;
    private Integer age = 0;
    private String birthday;
    private String gender = "Mujer";
    private String idpaciente = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caracteristicas_paciente);

        //region :: Views

        cmbGenero = findViewById(R.id.PA_cmbGenero);
        cmbGenero.setOnItemSelectedListener(this);

        etFechaNacimiento = findViewById(R.id.et_fecha_nacimiento);

        // View NumberPicker
        int size = 40;

        pckEdad = findViewById(R.id.PA_Edad);
        pckEdad.setMaxValue(99);
        pckEdad.setMinValue(20);
        pckEdad.setValue(65);
        pckEdad.setTextSize(size);

        pckCentenas = findViewById(R.id.PA_PesoCentenas);
        pckCentenas.setMaxValue(1);
        pckCentenas.setMinValue(0);
        pckCentenas.setValue(0);
        pckCentenas.setTextSize(size);

        pckDecenas = findViewById(R.id.PA_PesoDecenas);
        pckDecenas.setMaxValue(9);
        pckDecenas.setMinValue(0);
        pckDecenas.setValue(8);
        pckDecenas.setTextSize(size);

        pckUnidades = findViewById(R.id.PA_PesoUnidades);
        pckUnidades.setMaxValue(9);
        pckUnidades.setMinValue(0);
        pckUnidades.setValue(0);
        pckUnidades.setTextSize(size);

        NumberPicker pckKM = findViewById(R.id.PA_KG);
        String[] valuesKM = {"Kg"};
        pckKM.setDisplayedValues(valuesKM);
        pckKM.setTextSize(size);

        pckAltura = findViewById(R.id.PA_NAltura);
        pckAltura.setMaxValue(220);
        pckAltura.setMinValue(80);
        pckAltura.setValue(170);
        pckAltura.setTextSize(size);

        NumberPicker pckCM = findViewById(R.id.PA_CM);
        String[] valuesCM = {"cm"};
        pckCM.setDisplayedValues(valuesCM);
        pckCM.setTextSize(size);

        setRellenarGenero();
        //endregion

        EnableNumberPicker(false);

        //region :: Intent
        Intent intent = getIntent();
        Bundle b = intent.getExtras();
        if (b != null) {
            idpaciente = intent.getStringExtra("idpaciente");
            Patient paciente = ApiMethods.loadCharacteristics(idpaciente);

            weight = paciente.getWeight();
            height = paciente.getHeight();
            age = paciente.getAge();
            gender = paciente.getGender();
            birthday = paciente.getBirthday();

            Log.e(TAG, "gender: " + gender + ", age: " + age + ", weight: " + weight + ", height: " + height + ", Birthday: " + birthday);
        }
        //endregion

        pckEdad.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                int age = util.calcularEdad(etFechaNacimiento.getText().toString());
                if (newVal != age) {
                    String fechaCumple = util.obtenerFechaNacimiento(newVal);
                    etFechaNacimiento.setText(fechaCumple);
                }

            }
        });

        //region :: Establecer valores en los view
        int centenas = weight / 100;
        int decenas = (int) ((weight - centenas * 100.0) / 10.0);
        int unidades = (int) ((weight - centenas * 100.0 - decenas * 10.0) / 1.0);
        setNumberPicker(pckCentenas,centenas);
        setNumberPicker(pckDecenas,decenas);
        setNumberPicker(pckUnidades,unidades);

        setNumberPicker(pckEdad,age);
        setNumberPicker(pckAltura,height);
        setGenero(gender);
        etFechaNacimiento.setText(birthday);
        //endregion

        EnableNumberPicker(true);

    }

    // region :: ONCLICK
    @Override
    public void onClick(View view) {
        Log.e(TAG, " onClick()");

        int viewId = view.getId();
        if (viewId == R.id.PA_btSavePatient) {
            //region :: GUARDAR CONFIGURACION DE PACIENTE
            parsePatient();
            Patient patient = new Patient(gender, birthday, age, weight, height,false);
            Log.e(TAG, "patient guardar: " + patient.toString());
            if (ApiMethods.updateCharacteristics(idpaciente,patient)) {
                // Operación realizada correctamente
                updateFilePatient(patient);
                finish();
            }
            else { Alerts.mostrarAlert(this, "ERROR:\n\nNo se ha podido guardar los cambios."); }
            //endregion
        }
        else if (viewId == R.id.PA_imfAtras) {
            //region :: SALIR
            setEnableView(findViewById(R.id.PA_imfAtras),false);
            setEnableView(findViewById(R.id.PA_btSavePatient),false);

            Alerts.mostrarAlertSalir(this, "No se han guardado cambios.\n\n¿Que desea hacer?",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.e (TAG,"mostrarAlert i:" + i);
                            if (i == DialogInterface.BUTTON_POSITIVE) {
                                try {
                                    Log.e(TAG, "onClick() >> SALIR");
                                    finish();
                                } finally {
                                    dialogInterface.dismiss();
                                }
                            }
                            else if (i == DialogInterface.BUTTON_NEGATIVE){
                                try {
                                    Log.e(TAG, "onClick() >> Cancelar");
                                } finally {
                                    dialogInterface.dismiss();
                                }
                            }
                        }
                    }
            );

            //endregion
        }
        else if (viewId == R.id.et_fecha_nacimiento) {
            mostrarDatePicker(etFechaNacimiento.getText().toString());
        }
    }
    //endregion

    private void mostrarDatePicker(String fechaSeleccionada) {
        final Calendar calendario = Calendar.getInstance();
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        int anio, mes, dia;

        try {
            // Convertir la fecha de String a Calendar
            if (fechaSeleccionada != null && !fechaSeleccionada.isEmpty()) {
                calendario.setTime(formatoFecha.parse(fechaSeleccionada));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        anio = calendario.get(Calendar.YEAR);
        mes = calendario.get(Calendar.MONTH);
        dia = calendario.get(Calendar.DAY_OF_MONTH);

        // Restar 15 años a la fecha actual
        Calendar fechaMaximaCal = Calendar.getInstance();
        fechaMaximaCal.set(Calendar.YEAR, fechaMaximaCal.get(Calendar.YEAR) - 15);
        long fechaMaxima = fechaMaximaCal.getTimeInMillis();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        // Crear un calendario con la fecha seleccionada
                        Calendar calendarioSeleccionado = Calendar.getInstance();
                        calendarioSeleccionado.set(year, month, dayOfMonth);

                        // Formatear la fecha a "dd-MM-yyyy"
                        String cumple = formatoFecha.format(calendarioSeleccionado.getTime());
                        etFechaNacimiento.setText(cumple);
                        pckEdad.setValue(util.calcularEdad(cumple));
                    }
                },
                anio, mes, dia // Se inicializa el DatePicker con la fecha pasada por parámetro
        );

        // Establecer la fecha máxima (actual - 15 años)
        datePickerDialog.getDatePicker().setMaxDate(fechaMaxima);

        datePickerDialog.show();
    }

    //region :: LISTENER SPINNER
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//        if (adapterView.equals(cmbSelectQuery)){
//            typeQuery = (String) cmbSelectQuery.getSelectedItem();
//        }
//        else if (adapterView.equals(cmbLanguage)){
//            language = (String) cmbLanguage.getSelectedItem();
//        }
//        else if (adapterView.equals(cmbFunciones)){
//            setting = (String) cmbFunciones.getSelectedItem();
//        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    //endregion

    private void parsePatient() {
        weight = pckCentenas.getValue() * 100 + pckDecenas.getValue() * 10 + pckUnidades.getValue();
        height = pckAltura.getValue();
        age = pckEdad.getValue();
        birthday = etFechaNacimiento.getText().toString();
        gender = (String) cmbGenero.getSelectedItem();
        Log.e(TAG, "gender: " + gender + ", age: " + age + ", weight: " + weight + ", height: " + height);
    }

    //region :: Rellenado de view
    private void setEnableSpinners(boolean enable) {
        setEnableView(cmbGenero,enable);
    }

    private void setRellenarGenero(){
        ArrayList<String> typeList = new ArrayList<>();

        typeList.add("Hombre");
        typeList.add("Mujer");

//        ArrayAdapter types = new ArrayAdapter(this,android.R.layout.simple_spinner_item, typeList);
        ArrayAdapter types = new ArrayAdapter(this,R.layout.custom_spinner_item, typeList);
        types.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        cmbGenero.setAdapter(types);
    }

    private void EnableNumberPicker(boolean enable) {
        // weight
        setEnableView(pckCentenas,enable);
        setEnableView(pckDecenas,enable);
        setEnableView(pckUnidades,enable);
        setEnableView(pckDecimal,enable);

        // height
        setEnableView(pckAltura,enable);

        // age
        setEnableView(pckEdad,enable);

    }

    private void setNumberPicker(NumberPicker view, int number) {
        runOnUiThread(new Runnable() {
            public void run() {
                view.setValue(number);
            }
        });
    }

    private void setSelectionIdSpinner(Spinner spn, int id) {
        runOnUiThread(new Runnable() {
            public void run() {
                spn.setSelection(id);
            }
        });
    }

    private void setGenero(String genero) {
        if (genero.equals("Hombre")) {
            setSelectionIdSpinner(cmbGenero, 0);
        } else if (genero.equals("Mujer")) {
            setSelectionIdSpinner(cmbGenero, 1);
        }
    }
    //endregion

    //region :: Ciclo de vida
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG,"onResume()");
    }

    @Override
    protected  void onPause() {
        super.onPause();
        Log.e(TAG,"onPause()");
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG,"onDestroy()");
        super.onDestroy();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}
    //endregion

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
            }
        });
    }

    /***
     * <p>Añade al archivo paciente.json las características del paciente</p>
     * @param patient
     */
    private void updateFilePatient(Patient patient) {
        try {
            File pathPaciente = new File(FileAccess.getPATH_FILES(), FilePath.CONFIG_PACIENTE.getNameFile());
            Log.e(TAG,"pathFilePaciente Name: " + pathPaciente.getName());
            Log.e(TAG,"pathFilePaciente Path: " + pathPaciente.getPath());

            if (FileFuntions.checkFileExist(pathPaciente.getName())) {
                JSONObject json_paciente = util.readFileJSON(pathPaciente);
                Log.e(TAG,"read paciente.json: " + json_paciente.toString());

                // { .... "paciente":{"gender":"Hombre","age":50,"weight":96,"height":165,"birthday":"16-10-1974"}}
                // añadimos caracteristicas del paciente al fichero
                json_paciente.put("paciente",patient.toString());
                Log.e(TAG,"new paciente.json: " + json_paciente.toString());
                util.writeFileJSON(pathPaciente, json_paciente.toString());
            }
            else { Log.e(TAG, "Fichero paciente.json no existe"); }
        }
        catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"JSONException: " + e);
        }catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"Exception: " + e);
        }
    }
}