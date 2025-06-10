package com.eviahealth.eviahealth.ui.meeting;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.meeting.models.zutils;

import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FrameDialogEntrar#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FrameDialogEntrar extends Fragment {

    final String TAG = "FrameDialogEntrar";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // TOM:
    ProgressBar progressWait;
    TextView textFechaMeeting;
    Button buttonEntrar;
    CountDownTimer cTimer = null;
    String fechaMeeting = "";

    public FrameDialogEntrar() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FrameDialogEntrar.
     */
    // TODO: Rename and change types and number of parameters
    public static FrameDialogEntrar newInstance(String param1, String param2) {
        FrameDialogEntrar fragment = new FrameDialogEntrar();
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
                        Log.e(TAG, "(ahora >= fechaFinishMeeting) SUPERADA FECHA PARA PODER ENTRAR A LA VIDEOLLAMADA");
                        getActivity().finish();
                        return;
                    } else cTimer.start();
                }
                catch (NullPointerException  e) {
                    Log.e(TAG, "EXCEPTION >> NullPointerException  FrameDialogEntrar(): " + e.toString());
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
//        return inflater.inflate(R.layout.fragment_dialog_entrar, container, false);
        Log.e(TAG,"onCreateView()");
        View root = inflater.inflate(R.layout.fragment_dialog_entrar, container, false);

        textFechaMeeting = root.findViewById(R.id.textFechaMeeting);
        textFechaMeeting.setText(zutils.clearSecond(fechaMeeting));

        buttonEntrar = root.findViewById(R.id.buttonEntrar);
        progressWait = root.findViewById(R.id.progressWait);

        //region :: onClick >> buttonEntrar
        buttonEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG,"onClick >> buttonEntrar");
                progressWait.setVisibility(View.VISIBLE);
                buttonEntrar.setEnabled(false);
                Intent intent = new Intent(getActivity(), JoinMeetingActivity.class);
                startActivity(intent);
                getActivity().finish(); // Cierra actividad que llamó al frame
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