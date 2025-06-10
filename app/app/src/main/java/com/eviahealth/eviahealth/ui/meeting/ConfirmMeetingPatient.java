package com.eviahealth.eviahealth.ui.meeting;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.eviahealth.eviahealth.api.meeting.APIServiceImpl;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.admin.tecnico.LoginOption;
import com.eviahealth.eviahealth.utils.log.EVLogConfig;
import com.eviahealth.eviahealth.utils.log.EVLog;

import org.json.JSONException;
import org.json.JSONObject;

public class ConfirmMeetingPatient extends BaseActivity implements View.OnClickListener{

    final String TAG = "ConfirmMeetingPatient";
    int secuencia = 0;
    CountDownTimer cTimer = null;               // Timeout de pulsación de botones para acceder a tecnico
    CountDownTimer cTimerCheckMeeting = null;   // Gestion si existe todavia VideoLLamada (1 minuto)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_meeting_patient);

        //region :: TimerOut Pulsaciones (5 seg x tecla)
        cTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimer.cancel();
                secuencia = 0;
            }
        };
        //endregion

        //region :: TimerOut Sigue Planificada VideoLlamada ? (1 minuto)
        cTimerCheckMeeting = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimerCheckMeeting.cancel();
                isActiveMeeting();
            }
        };
        cTimerCheckMeeting.start();
        //endregion

        loadFragmentConfirmMeeting();

//        dialogConfirm();
    }

    private void loadFragmentConfirmMeeting() {

        FrameDialogConfirmar fm = new FrameDialogConfirmar();
        this.getSupportFragmentManager().beginTransaction()
                .replace(R.id.FrameConfirmMeeting, fm)
                .addToBackStack(null)
                .commit();
    }

    private void dialogConfirm() {

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_zoom_confirmar);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
//        dialog.setTitle("Title...");

        // set the custom dialog components - text, image and button
        TextView text = (TextView) dialog.findViewById(R.id.dlgTextFechaMeeting);
        text.setText("2023-01-01 00:00");

        //region :: Button Aceptar
        Button buttonAceptar = (Button) dialog.findViewById(R.id.dlgButtonAceptar);
        buttonAceptar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG,"Button Aceptar");
                dialog.dismiss();
                finish();
            }
        });
        //endregion

        //region :: Button Rechazar
        Button buttonRechazar = (Button) dialog.findViewById(R.id.dlgButtonRechazar);
        buttonRechazar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG,"Button Rechazar");
                dialog.dismiss();
                finish();
            }
        });
        //endregion
        dialog.show();
    }

    private void isActiveMeeting() {
        APIServiceImpl serviceZoom = new APIServiceImpl();

        SharedPreferences prefs = this.getSharedPreferences("VariableEntorno", Context.MODE_PRIVATE);
        String idpaciente = prefs.getString("idpaciente", "");

        try {
            JSONObject isActive = serviceZoom.isActivePatientMeeting(getApplicationContext(),idpaciente);
            Log.e(TAG, "isActive: " + isActive);

            if (isActive.has("status")) {
                int status = isActive.getInt("status");
                if (status == 0) {
                    Log.e(TAG,"Paciente ya no tiene ninguna videollamada planificada");
                    finish();
                }
                else cTimerCheckMeeting.start();
            }

        }
        catch (JSONException e) {
            Log.e(TAG, "EXCEPTION >> JSONException plannedMeeting(): " + e.toString());
            e.printStackTrace();
            cTimerCheckMeeting.start();
        }
    }

    @Override
    protected void onDestroy() {
        EVLog.log(TAG,"onDestroy()");

        if (cTimerCheckMeeting != null) {
            cTimerCheckMeeting.cancel();
            cTimerCheckMeeting = null;
        }
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
    }

    @Override
    protected  void onPause() {
        super.onPause();
        EVLog.log(TAG,"onPause()");
    }

    @Override
    public void onClick(View view) {
        Log.e(TAG,"onClick()");
        int viewId = view.getId();
        if (viewId == R.id.btnConfig1) {
            cTimer.cancel();
            if (secuencia == 0) {
                secuencia = 1;
                cTimer.start();
            }
        }
        else if (viewId == R.id.btnConfig2) {
            cTimer.cancel();
            if (secuencia == 1) {
                secuencia = 2;
                cTimer.start();
            } else secuencia = 0;
        }
        else if (viewId == R.id.btnConfig3) {
            cTimer.cancel();
            if (secuencia == 2) {
                secuencia = 3;
                cTimer.start();
            } else secuencia = 0;
        }
        else if (viewId == R.id.btnConfig4) {
            cTimer.cancel();
            if (secuencia == 3) {
                // Ir a login
                EVLogConfig.setFechaActual();
                EVLogConfig.log(TAG, "IrALoginAdmin()");

                Intent intent = new Intent(this, LoginOption.class);
                startActivity(intent);
                finish();
            }
            secuencia = 0;
        }
    }
}