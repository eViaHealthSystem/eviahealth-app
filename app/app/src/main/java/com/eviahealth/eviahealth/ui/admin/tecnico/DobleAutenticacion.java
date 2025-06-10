package com.eviahealth.eviahealth.ui.admin.tecnico;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.api.tecnico.EPService;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.admin.fichapaciente.DispositivosPaciente;
import com.eviahealth.eviahealth.utils.log.EVLogConfig;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.utils.PermissionUtils;
import com.eviahealth.eviahealth.utils.log.EVLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class DobleAutenticacion extends BaseActivity implements View.OnClickListener  {

    public static String TAG = DobleAutenticacion.class.getSimpleName();
    TextView txtStatus;
    EditText editCODE;
    String webtoken, usuario;
    private int intentos = 0;
    private static final int MAX_INTENTOS = 3;
    CountDownTimer cTimer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doble_autenticacion);

        txtStatus = findViewById(R.id.txtStatusCode);
        editCODE = findViewById(R.id.editCODE);
        setTextView(txtStatus,"");

        PermissionUtils.requestAll(this);

        webtoken = getIntent().getStringExtra("webtoken");
        usuario = getIntent().getStringExtra("user");

        Log.e(TAG,"user: " + usuario + ", token: " + webtoken);

        //region :: TimerOut verificación code (5 mint)
        cTimer = new CountDownTimer(1000 * 5 * 60, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimer.cancel();
                Log.e(TAG, "Timeout Code PIN");
                finish();
            }
        }.start();
        //endregion
    }

    @Override
    public void onClick(View view) {

        int viewId = view.getId();
        if (viewId == R.id.CODE_imgAtras) {
            EVLogConfig.log(TAG, "onClick() >> SALIR");
//            startActivity(new Intent(this, Inicio.class));
            finish();
        }
        else if (viewId == R.id.btVerificar) {
            setTextView(txtStatus,"");
            String code = editCODE.getText().toString();
            Log.e(TAG, "edit code: " + code);

            if (TextUtils.isEmpty(code)) {
                setTextView(txtStatus, "CODE empty");
                return;
            }
            EVLogConfig.log(TAG, "CODE DOBLE AUTENTIFICACIÓN: " + code);

            EPService service = new EPService();
            String msg = service.activateUser(webtoken, usuario, code);

            if (msg != null) {
                Log.e(TAG, "MSG CODE: " + msg);
                try {
                    JSONObject res = new JSONObject(msg);
                    if (res.has("httpcode")) {
                        setTextView(txtStatus,"Error Code Pin");
                        editCODE.setText("");

                        intentos += 1;
                        if (intentos > MAX_INTENTOS) {
                            Log.e(TAG, "Intentos de Code PIN superados.");
                            finish();
                        }
                    }
                    else {
                        setTextView(txtStatus,"Code Pin OK");
                        JSONObject json_paciente = FileAccess.leerJSON(FilePath.CONFIG_PACIENTE);
                        String idpaciente = json_paciente.getString("idpaciente");

                        Intent intent = new Intent(DobleAutenticacion.this, DispositivosPaciente.class);
                        intent.putExtra("key", "Login");
                        intent.putExtra("numeropaciente", idpaciente);
                        startActivity(intent);
                        finish();
                    }
                }
                catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
            else {
                setTextView(txtStatus,"Error server");
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        EVLog.log(TAG, "onDestroy()");
        if (cTimer != null) {
            cTimer.cancel();
            cTimer = null;
        }
        super.onDestroy();
    }

    private void setTextView(View view, String texto) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (view instanceof TextView) {
                    TextView txtView = (TextView) view;
                    txtView.setText(texto);
                }
                else if (view instanceof EditText) {
                    EditText txtView = (EditText) view;
                    txtView.setText(texto);
                }
            }
        });
    }
}