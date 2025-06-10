package com.eviahealth.eviahealth.ui.admin.tecnico;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.eviahealth.eviahealth.utils.FileAccess.FileFuntions;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.utils.log.EVLogConfig;
import com.eviahealth.eviahealth.utils.PermissionUtils;

public class InsertIMEI extends BaseActivity implements View.OnClickListener  {

    public static String TAG = "LoginOption";
    TextView txtStatus, txtVersion;
    EditText editIMEI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert_imei);

        txtStatus = findViewById(R.id.txtStatusIMEI);
        txtVersion = findViewById(R.id.txtVersionIMEI);
        editIMEI = findViewById(R.id.editIMEI);
        setTextView(txtStatus,"");

        PermissionUtils.requestAll(this);

        if (FileFuntions.checkFileExist(FilePath.CONFIG_SERIAL.getNameFile())) {
            String imei = FileFuntions.readfile(FilePath.CONFIG_SERIAL.getNameFile());
            Log.e(TAG,"load imei: " + imei);
            setTextView(editIMEI,imei);
        }
    }

    // region :: ONCLICK
    @Override
    public void onClick(View view) {

        int viewId = view.getId();
        if (viewId == R.id.IMEI_imgAtras) {
            EVLogConfig.log(TAG, "onClick() >> SALIR");
            startActivity(new Intent(this, Inicio.class));
            finish();
        }
        else if (viewId == R.id.IMEI_btSave) {
            String imei = editIMEI.getText().toString();
            Log.e(TAG, "edit imei: " + imei);

            if (TextUtils.isEmpty(imei)) {
                setTextView(txtStatus, "IMEI empty");
                return;
            }
            EVLogConfig.log(TAG, "Nuevo IMEI: " + imei);

            FileFuntions.writefile(FilePath.CONFIG_SERIAL.getNameFile(), imei);

            startActivity(new Intent(this, Inicio.class));
            finish();
        }
    }
    //endregion

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume()");
//        editIMEI.requestFocus();
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