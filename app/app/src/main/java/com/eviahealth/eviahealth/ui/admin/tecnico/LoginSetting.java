package com.eviahealth.eviahealth.ui.admin.tecnico;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.utils.log.EVLogConfig;

public class LoginSetting extends BaseActivity implements View.OnClickListener {

    public static String TAG = "LoginSetting";
    TextView txtStatus;
    EditText editUser;
    Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_setting);

        Log.e(TAG, "onCreate()");

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

        if (user.equals("1479")){
            // Abrimos Ajustes de la tablet
            Intent openWirelessSettings = new Intent(android.provider.Settings.ACTION_SETTINGS);
            startActivity(openWirelessSettings);
            finish();
        }
        else{
            setTextView(txtStatus,"incorrect PIN");
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume()");
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