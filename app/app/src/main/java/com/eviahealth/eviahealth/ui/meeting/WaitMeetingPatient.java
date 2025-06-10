package com.eviahealth.eviahealth.ui.meeting;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;

import com.eviahealth.eviahealth.api.meeting.APIServiceImpl;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.admin.tecnico.LoginOption;
import com.eviahealth.eviahealth.utils.log.EVLogConfig;
import com.eviahealth.eviahealth.utils.log.EVLog;

import org.json.JSONException;
import org.json.JSONObject;

public class WaitMeetingPatient extends BaseActivity implements View.OnClickListener{

    final String TAG = "WaitMeetingPatient";
    int secuencia = 0;
    CountDownTimer cTimer = null;       // Timeout de pulsación de botones para acceder a tecnico
    CountDownTimer cTimerCheckMeeting = null;   // Gestion si existe todavia VideoLLamada (1 minuto)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wait_meeting_patient);

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

        loadFragmentEnterMeeting();
    }

    private void loadFragmentEnterMeeting() {

        FrameDialogEntrar fm = new FrameDialogEntrar();
        this.getSupportFragmentManager().beginTransaction()
                .replace(R.id.FrameEnterMeeting, fm)
                .addToBackStack(null)
                .commit();
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
        secuencia = 0;
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
        if (viewId == R.id.bttnConfig1) {
            cTimer.cancel();
            if (secuencia == 0) {
                secuencia = 1;
                cTimer.start();
            }
        }
        else if (viewId == R.id.bttnConfig2) {
            cTimer.cancel();
            if (secuencia == 1) {
                secuencia = 2;
                cTimer.start();
            } else secuencia = 0;
        }
        else if (viewId == R.id.bttnConfig3) {
            cTimer.cancel();
            if (secuencia == 2) {
                secuencia = 3;
                cTimer.start();
            } else secuencia = 0;
        }
        else if (viewId == R.id.bttnConfig4) {
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