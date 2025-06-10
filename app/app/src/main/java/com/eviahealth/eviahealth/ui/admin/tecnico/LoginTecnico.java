package com.eviahealth.eviahealth.ui.admin.tecnico;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.api.tecnico.EPService;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.admin.fichapaciente.DispositivosPaciente;
import com.eviahealth.eviahealth.utils.log.EVLogConfig;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class LoginTecnico extends BaseActivity implements View.OnClickListener {

    static final String TAG = LoginTecnico.class.getSimpleName();

    EditText editUSer, editPassword;
    Button loginButton;
    TextView txtStatus;
    LinearLayout linearlayout;
    ProgressBar cargando;
    private int intentos = 0;
    private static final int MAX_INTENTOS = 3;
    private static final boolean DEBUG_MODE = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_tecnico);

        //region :: View
        txtStatus = findViewById(R.id.txtStatus_tecnico);
        setTextView(txtStatus,"");
        linearlayout = findViewById(R.id.linearLayout);
        cargando=findViewById(R.id.progressBarTecnico);
        cargando.setVisibility(View.GONE);
        editUSer = findViewById(R.id.editUser);
        editPassword = findViewById(R.id.editPassword);
        loginButton = findViewById(R.id.buttonLogin);
        //endregion

    }

    // region :: ONCLICK
    @Override
    public void onClick(View view) {

        int viewId = view.getId();
        if (viewId == R.id.tecnico_imgAtras) {
            EVLogConfig.log(TAG, "onClick() >> SALIR");
            finish();
        }
        else if (viewId == R.id.buttonLogin) {
            if (editUSer.getText().length()==0||editPassword.getText().length()==0||editUSer.getText()  == null || editPassword.getText() == null) {
                Toast.makeText(getApplicationContext(), "El usuario o la contraseña  no pueden estar vacios", Toast.LENGTH_SHORT).show();
            }else{

                setTextView(txtStatus,"");
                cargando.setVisibility(View.VISIBLE);
                linearlayout.setEnabled(false);
                loginButton.setEnabled(false);

                Log.e(TAG,"BUTTON LOGIN");

                Runnable R = new Hilo1();
                new Thread(R).start();
            }
        }

    }
    //endregion

    class Hilo1 extends Thread {

        public Hilo1( ){

        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(loginButton.getWindowToken(), 0);
                    String usuario = editUSer.getText().toString();
                    String password = editPassword.getText().toString();

                    EPService service = new EPService();
                    String msg = service.login(usuario, password);
                    if (msg == null) {
                        Log.e(TAG, "TOKEN WEB: null" );
                        setErrorLogin();
                    }
                    else {
                        Log.e(TAG, "TOKEN WEB: " + msg);
                        try {
                            JSONObject token = new JSONObject(msg);
                            if (token.getString("token").length() != 0) {
                                cargando.setVisibility(View.GONE);
                                boolean dobleAuth = false;
                                if (token.has("user")) {
                                    JSONObject user = token.getJSONObject("user");
                                    if (user.has("twofa_activated")) {
                                        int doble = user.getInt("twofa_activated");
                                        dobleAuth = (doble == 1)? true : false;
                                    }
                                }

                                if (dobleAuth) {
                                    String webtoken = token.getString("token");

                                    Intent intent = new Intent(LoginTecnico.this, DobleAutenticacion.class);
                                    intent.putExtra("webtoken", webtoken);
                                    intent.putExtra("user", usuario);
                                    startActivity(intent);
                                }
                                else {
                                    JSONObject json_paciente = FileAccess.leerJSON(FilePath.CONFIG_PACIENTE);
                                    String idpaciente = json_paciente.getString("idpaciente");

                                    Intent intent = new Intent(LoginTecnico.this, DispositivosPaciente.class);
                                    intent.putExtra("key", "Login");
                                    intent.putExtra("numeropaciente", idpaciente);
                                    startActivity(intent);
                                }
                                finish();
                            }
                            else {
                                setErrorLogin();

                                intentos += 1;
                                if (intentos > MAX_INTENTOS) {
                                    Log.e(TAG, "Intentos de Code PIN superados.");
                                    finish();
                                }
                            }
                        }
                        catch (IOException | JSONException e) {
                            Log.e(TAG, "IOException or JSONException: " + e.toString());
                            setErrorLogin();
                        }
                    }

                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (DEBUG_MODE) {
        editPassword.setText("_Aeviala@987");
        editUSer.setText("admin");
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}

    private void setErrorLogin()  {
        cargando.setVisibility(View.GONE);
        linearlayout.setEnabled(true);
        loginButton.setEnabled(true);
        setTextView(txtStatus,"Usuario y contraseña incorrectos");
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