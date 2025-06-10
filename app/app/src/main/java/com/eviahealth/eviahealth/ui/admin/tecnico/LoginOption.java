package com.eviahealth.eviahealth.ui.admin.tecnico;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FileConfig;
import com.eviahealth.eviahealth.utils.FileAccess.FileFuntions;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.admin.fichapaciente.DispositivosPaciente;
import com.eviahealth.eviahealth.utils.log.EVLogConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class LoginOption extends BaseActivity implements View.OnClickListener {

    public static String TAG = "LoginOption";
    TextView txtStatus;
    EditText editUser;
    Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_user);

        txtStatus = findViewById(R.id.txtUserStatus);
        editUser = findViewById(R.id.editUser);
        buttonLogin = findViewById(R.id.USER_btLogin);

        setTextView(txtStatus,"");

    }

    // region :: ONCLICK
    @Override
    public void onClick(View view) {

        int viewId = view.getId();
        if (viewId == R.id.USER_imgAtras) {
            EVLogConfig.log(TAG, "onClick() >> SALIR");
//                startActivity(new Intent(this, Inicio.class));
            finish();
        }
        else if (viewId == R.id.USER_btLogin) {
            checkUser();
        }

    }
    //endregion

    private void checkUser() {

        String user = editUser.getText().toString();

        if(TextUtils.isEmpty(user)) {
            setTextView(txtStatus,"User empty");
            return;
        }

        user = user.toLowerCase(); // pasamos texto a minusculas

        if (user.equals("tecnico")){

            // Comprobamos que exista el archivo de serial.txt
            if (FileFuntions.checkFileExist(FilePath.CONFIG_SERIAL.getNameFile()) == false) {
                EVLogConfig.log(TAG,"No existe archivo: " + FilePath.CONFIG_SERIAL.getNameFile());

                Intent intent = new Intent(this, InsertIMEI.class);
                startActivity(intent);
                finish();
                return;
            }

            // Comprobamos si existen todos los archivos de configuración
            boolean exiten = true;
            List<String> namesFile = FileConfig.getAllPaths();
            for (String name: namesFile) {
                if (FileFuntions.checkFileExist(name) == false) {
                    exiten = false;
                }
            }

            if (exiten == false) {
                EVLogConfig.log(TAG,"No existen todos los archivos de configuración >> QRReader");
                Intent intent = new Intent(this, QRReader.class);
                intent.putExtra("key", "Login");
                intent.putExtra("numeropaciente", "0");
                startActivity(intent);
                finish();
            }
            else {
                startActivity(new Intent(this,LoginTecnico.class));
                finish();
            }
        }
        else if (user.equals("t")){

            // Comprobamos que exista el archivo de serial.txt
            if (FileFuntions.checkFileExist(FilePath.CONFIG_SERIAL.getNameFile()) == false) {
                EVLogConfig.log(TAG,"No existe archivo: " + FilePath.CONFIG_SERIAL.getNameFile());

                Intent intent = new Intent(this, InsertIMEI.class);
                startActivity(intent);
                finish();
                return;
            }

            // Comprobamos si existen todos los archivos de configuración
            boolean exiten = true;
            List<String> namesFile = FileConfig.getAllPaths();
            for (String name: namesFile) {
                if (FileFuntions.checkFileExist(name) == false) {
                    exiten = false;
                }
            }

            if (exiten == false) {
                EVLogConfig.log(TAG,"No existen todos los archivos de configuración >> QRReader");
                Intent intent = new Intent(this, QRReader.class);
                intent.putExtra("key", "Login");
                intent.putExtra("numeropaciente", "0");
                startActivity(intent);
                finish();
            }
            else {
                try {
                    JSONObject json_paciente = FileAccess.leerJSON(FilePath.CONFIG_PACIENTE);
                    String idpaciente = json_paciente.getString("idpaciente");

                    Intent intent = new Intent(this, DispositivosPaciente.class);
                    intent.putExtra("key", "Login");
                    intent.putExtra("numeropaciente", idpaciente);
                    startActivity(intent);
                    finish();
                }
                catch (IOException ioException) {
                    ioException.printStackTrace();
                    EVLogConfig.log(TAG,"IOException: " + ioException.toString());
                }
                catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                    EVLogConfig.log(TAG,"JSONException: " + jsonException.toString());
                }
            }
        }
        else if (user.equals("admin") || user.equals("imei")){
            EVLogConfig.log(TAG,"Configuración de IMEI");

            Intent intent = new Intent(this, InsertIMEI.class);
            startActivity(intent);
            finish();
        }
        else if (user.equals("play") || user.equals("p")){
            /* Para que funcione en la Polítia se debe añadir
            el package de play store: com.android.vending
            */
            EVLogConfig.log(TAG,"Configuración de Play Store");

            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=")));
            } catch (android.content.ActivityNotFoundException anfe) {
                Log.e(TAG,"Exception: Play Store");
            }
            editUser.setText("");
//            finish();
        }
        else{
            setTextView(txtStatus,"incorrect user");
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}

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