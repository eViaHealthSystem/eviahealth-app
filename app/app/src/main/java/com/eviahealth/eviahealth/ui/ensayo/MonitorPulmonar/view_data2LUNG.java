package com.eviahealth.eviahealth.ui.ensayo.MonitorPulmonar;

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
import com.eviahealth.eviahealth.models.SecuenciaActivity;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;
import com.eviahealth.eviahealth.models.vitalograph.LungData;
import com.eviahealth.eviahealth.models.vitalograph.SigleTestData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class view_data2LUNG extends BaseActivity implements View.OnClickListener {

    private String TAG = "VIEW_DATA_LUNG";
    final String FASE = "M. PULMONAR LUNG";
    Button btContinuar, btReintentar;
    TextView txtPEF,txtFEV1, txtFEV6, txtFEV1FEV6, txtTitulo, txtfail;
    TextView txt_pef,txt_fev1, txt_fev6, txt_ratio;
    TextView txtuni_pef,txtuni_fev1, txtuni_fev6, txtuni_ratio;
    String titulo = "";

    LungData datos;
    Boolean fgsaltar = false;

    CountDownTimer cTimer = null;
    TextToSpeechHelper textToSpeech;

    private Integer id_test = 0;
    private int semaforo = 0;
    SigleTestData test;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data2_lung);
        EVLog.log(TAG,"onCreate()");

        //region :: Referencia Views
        btContinuar = findViewById(R.id.lung_btContinuar);
        btReintentar = findViewById(R.id.lung_btReintentar);

        txtTitulo = findViewById(R.id.lung_txtTitulo);

        txtPEF = findViewById(R.id.lung_pef);
        txtFEV1 = findViewById(R.id.lung_fev1);
        txtFEV6 = findViewById(R.id.lung_fev6);
        txtFEV1FEV6 = findViewById(R.id.lung_fev1fev6);

        txt_pef = findViewById(R.id.txt_pef);
        txt_fev1 = findViewById(R.id.txt_fev1);
        txt_fev6 = findViewById(R.id.txt_fev6);
        txt_ratio = findViewById(R.id.txt_ratio);

        txtuni_pef = findViewById(R.id.txt_uni_pef);
        txtuni_fev1 = findViewById(R.id.txt_uni_fev1);
        txtuni_fev6 = findViewById(R.id.txt_uni_fev6);
        txtuni_ratio = findViewById(R.id.txt_uni_ratio);

        txtfail = findViewById(R.id.lung_fail);

        setVisibleView(btContinuar, View.INVISIBLE);
        setVisibleView(btReintentar, View.INVISIBLE);
        //endregion

        // texto >> Voz
        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        datos = LungData.getInstance();
        id_test = datos.getIdTest();
        fgsaltar = false;

        EnsayoLog.log(FASE,TAG,"Se ha descargado el test del monitor pulmonar");

        titulo = "Resultado de su Test";
        setTextView(txtTitulo,titulo);

        test = datos.getSingleTestData();

        if (test.isGoodTest()) {
            // Si el test es correcto
            setTextView(txtPEF,"" + test.getPEF());
            setTextView(txtFEV1,"" +  test.getFEV1());
            setTextView(txtFEV6,"" +  test.getFEV6());
            setTextView(txtFEV1FEV6,"" +  test.getFEV1FEV6());
            EnsayoLog.log(FASE, TAG, "TEST REALIZADO CORRECTAMENTE");

            //region :: Semáforo

            double pb = test.getFEV1_PersonalBest();
            if (pb == 0) pb = test.getFEV1();

            double tmp = test.getFEV1() / pb;
            semaforo = (int)(tmp * 100);
            Log.e(TAG,"pb: " + pb + ", FEV1: " + test.getFEV1() + ", semaforo: " + semaforo + "%");

            if (semaforo >= test.getGreenZone()) {
                Log.e(TAG,"verde");

                // la zona verde la divido en 4 trozos para que sea más visual el nivel verde
                int trozo = 100 - test.getGreenZone();
                int tramo = trozo / 4;

                if (semaforo >= (test.getGreenZone() + (3*tramo))) {
                    changeSemaforo("verde4");
                }
                else if (semaforo >= test.getGreenZone() + (2*tramo)) {
                    changeSemaforo("verde3");
                }
                else if (semaforo >= test.getGreenZone() + tramo) {
                    changeSemaforo("verde2");
                }
                else {
                    changeSemaforo("verde1");
                }
            }
            else if (semaforo >= test.getYellowZone()) {
                Log.e(TAG,"amarillo");
                changeSemaforo("amarrillo");
            }
            else if (semaforo >= test.getOrangeZone()) {
                Log.e(TAG,"naranja");
                changeSemaforo("naranja");
            }
            else {
                Log.e(TAG,"rojo");

                // la zona roja la divido en 4 trozos para que sea más visual el nivel rojo
                int trozo = test.getOrangeZone();
                int tramo = trozo / 4;

                if (semaforo >= (test.getOrangeZone() - tramo)) {
                    changeSemaforo("rojo1");
                }
                else if (semaforo >= test.getOrangeZone() - (2*tramo)) {
                    changeSemaforo("rojo2");
                }
                else if (semaforo >= (test.getOrangeZone() - (3*tramo))) {
                    changeSemaforo("rojo3");
                }
                else {
                    changeSemaforo("rojo4");
                }
            }

            //endregion

            // Crea un nuevo fichero si el test ha sido correcto
            datos.setFilesActivity();
            id_test = 0;
            datos.setIdTest(id_test);

        }
        else {
            // test incorrecto

            //region :: Ocultar Views
            setVisibleView(txt_pef, View.INVISIBLE);
            setVisibleView(txt_fev1, View.INVISIBLE);
            setVisibleView(txt_fev6, View.INVISIBLE);
            setVisibleView(txt_ratio, View.INVISIBLE);

            setVisibleView(txtuni_pef, View.INVISIBLE);
            setVisibleView(txtuni_fev1, View.INVISIBLE);
            setVisibleView(txtuni_fev6, View.INVISIBLE);
            setVisibleView(txtuni_ratio, View.INVISIBLE);

            setVisibleView(txtPEF, View.INVISIBLE);
            setVisibleView(txtFEV1, View.INVISIBLE);
            setVisibleView(txtFEV6, View.INVISIBLE);
            setVisibleView(txtFEV1FEV6, View.INVISIBLE);

            //endregion

            changeSemaforo("fail");

            EnsayoLog.log(FASE, TAG, "EL TEST NO SE HA REALIZADO CORRECTAMENTE, INTENTOS: " + id_test);

            id_test += 1;
            datos.setIdTest(id_test);

            String mensage = "Medida incorrecta\n\n" +
                            "Por favor repita\nla maniobra\n\n" +
                            "INTENTO " + id_test + " de 3";

            setVisibleView(txtfail, View.VISIBLE);
            setTextView(txtfail,mensage);

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

                if (test.isGoodTest()) {
                    setVisibleView(btContinuar, View.VISIBLE);
                }
                else {
                    if (id_test >= 3) {
                        setVisibleView(btContinuar, View.VISIBLE);
                    }
                    else {
                        setVisibleView(btContinuar, View.INVISIBLE);
                    }
                }
                setVisibleView(btReintentar, View.VISIBLE);

            }
        };
        cTimer.start();
        //endregion

    }

    private void changeSemaforo(String image) {
        Log.e(TAG,"changeSemaforo()" + image);

        if (image.equals("verde4")) {
            datos.setColorSemaforo("green");
        }
        else if (image.equals("verde3")) {
            datos.setColorSemaforo("green");
        }
        else if (image.equals("verde2")) {
            datos.setColorSemaforo("green");
        }
        else if (image.equals("verde1")) {
            datos.setColorSemaforo("green");
        }
        else if (image.equals("amarrillo")) {
            datos.setColorSemaforo("yellow");
        }
        else if (image.equals("naranja")) {
            datos.setColorSemaforo("orange");
        }
        else if (image.equals("rojo1")) {
            datos.setColorSemaforo("red");
        }
        else if (image.equals("rojo2")) {
            datos.setColorSemaforo("red");
        }
        else if (image.equals("rojo3")) {
            datos.setColorSemaforo("red");
        }
        else if (image.equals("rojo4")) {
            datos.setColorSemaforo("red");
        }
        else {
            datos.setColorSemaforo("none");
        }
    }

    @Override
    public void onClick(View view) {
        Log.e(TAG," onClick()");
        Intent intent;
        int viewId = view.getId();
        if (viewId == R.id.lung_btReintentar) {
            //region >> Button Reintentar
            btReintentar.setEnabled(false);
            btContinuar.setEnabled(false);
            EVLog.log(TAG, "REINTENTAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "PACIENTE PULSA REINTENTAR");
            startActivity(new Intent(this, get_dataLUNG.class));
            finish();
            //endregion
        }
        else if (viewId == R.id.lung_btContinuar) {
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
            String mensage = "";
            List<String> texto = new ArrayList<>();
            texto.add(titulo);

            if (test.isGoodTest()) {
                mensage = "El test se ha realizado correctamente.";
                texto.add(mensage);

                mensage = "Su Pico de Flujo Espiratorio se encuentra a " + test.getPEF() + " litros, minuto.";
                texto.add(mensage);

                mensage = "Su Volumen Espiratorio Forzado en el primer segundo se encuentra a ";
                String[] fev1 = convertFloatToVoz(String.valueOf(test.getFEV1()));
                if (fev1.length == 2) { mensage = mensage + fev1[0] + " con " + fev1[1]; }
                else { mensage = mensage + fev1[0]; }
                mensage = mensage + " litros.";
                texto.add(mensage);

                mensage = "Su Volumen Espiratorio Forzado en seis segundos se encuentra a ";
                String[] fev6 = convertFloatToVoz(String.valueOf(test.getFEV6()));
                if (fev6.length == 2) { mensage = mensage + fev6[0] + " con " + fev6[1]; }
                else { mensage = mensage + fev6[0]; }
                mensage = mensage + " litros.";
                texto.add(mensage);

                mensage = "Pulse continuar para seguir con el ensayo.";
                texto.add(mensage);
            }
            else {
                mensage = "El test no se ha realizado correctamente.";
                texto.add(mensage);
                mensage = "Medida incorrecta.";
                texto.add(mensage);
                mensage = "Por favor repita la maniobra.";
                texto.add(mensage);
            }

            textToSpeech.speak(texto);
            //endregion
        }
    }

    private String[] convertFloatToVoz(String tp) {
        String[] dev = tp.split("\\.");
        return dev;
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
                Log.e(TAG,"Status:" + texto);
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