package com.eviahealth.eviahealth.ui.meeting;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.eviahealth.eviahealth.api.meeting.APIServiceImpl;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.meeting.models.zutils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FrameDialogConfirmar#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FrameDialogConfirmar extends Fragment {

    final String TAG = "FrameDialogConfirmar";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // TOM:
    TextView textFechaMeeting;
    Button buttonAceptar, buttonRechazar;
    CountDownTimer cTimer = null;
    String fechaMeeting = "";

    public FrameDialogConfirmar() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FrameDialogConfirmar.
     */
    // TODO: Rename and change types and number of parameters
    public static FrameDialogConfirmar newInstance(String param1, String param2) {
        FrameDialogConfirmar fragment = new FrameDialogConfirmar();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        Log.e(TAG,"onCreate()");

        SharedPreferences prefs = getActivity().getSharedPreferences("VariableEntorno", Context.MODE_PRIVATE);
        fechaMeeting = prefs.getString("fechaMeeting", "");

        //region :: TimerOut Comprovación de fecha actual (1 minuto)
        cTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimer.cancel();

                try {
                    Date ahora = zutils.toDate(zutils.getDateNow());
                    Date fechaFinishMeeting = zutils.addMinutesFecha(fechaMeeting, 45);

                    if (zutils.compareTo(ahora, fechaFinishMeeting) >= 0) {

                        // ahora >= fechaFinishMeeting
                        Log.e(TAG, "(ahora >= fechaFinishMeeting) SUPERADA FECHA PARA PODER ACEPTAR A LA VIDEOLLAMADA");
                        getActivity().finish();
                        return;
                    } else cTimer.start();
                }
                catch (NullPointerException  e) {
                    Log.e(TAG, "EXCEPTION >> NullPointerException  FrameDialogConfirmar(): " + e.toString());
                    e.printStackTrace();
                    cTimer.start();
                }
            }
        };
        //endregion
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_zoom_dialog_confirmar, container, false);

        Log.e(TAG,"onCreateView()");
        View root = inflater.inflate(R.layout.fragment_zoom_dialog_confirmar, container, false);

        textFechaMeeting = root.findViewById(R.id.textFechaMeeting);
        textFechaMeeting.setText(zutils.clearSecond(fechaMeeting));

        buttonAceptar= root.findViewById(R.id.buttonAceptar);
        buttonRechazar = root.findViewById(R.id.buttonRechazar);

        //region :: onClick >> buttonAceptar
        buttonAceptar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG,"onClick >> buttonAceptar");

                SharedPreferences prefs = getActivity().getSharedPreferences("VariableEntorno", Context.MODE_PRIVATE);
                String idpaciente = prefs.getString("idpaciente", "");
                String idprescriptor = prefs.getString("idprescriptor", "");

                // CONSULTA Backend
                APIServiceImpl serviceZoom = new APIServiceImpl();
                JSONObject action = serviceZoom.confirmarMeeting(getActivity(),idpaciente,idprescriptor);

                try {
                    if (!action.has("httpCode")) {
                        // error de comunicación
                        Log.e(TAG,"confirmarMeeting() not param httpCode");
                    }
                    else if (action.getInt("httpCode") != HttpsURLConnection.HTTP_OK ) {
                        // error de comunicación
                        Log.e(TAG,"confirmarMeeting() ERROR httpCode: " + action.getInt("httpCode"));
                    }
                    else {
                        Log.e(TAG,"confirmarMeeting() OK");
                    }

                    getActivity().finish();
                }
                catch (JSONException e) {
                    Log.e(TAG,"JSONException: " + e.toString());
                    getActivity().finish();
                }

            }
        });
        //endregion

        //region :: onClick >> buttonRechazar
        buttonRechazar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG,"onClick >> buttonRechazar");
                FrameDialogRechazar fm = new FrameDialogRechazar();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.FrameConfirmMeeting, fm)
                        .addToBackStack(null)
                        .commit();
            }
        });
        //endregion

        cTimer.start();
        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy()");

        if (cTimer != null) {
            cTimer.cancel();
            cTimer = null;
        }
    }
}