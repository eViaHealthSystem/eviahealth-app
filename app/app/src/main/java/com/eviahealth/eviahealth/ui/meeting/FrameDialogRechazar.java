package com.eviahealth.eviahealth.ui.meeting;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.eviahealth.eviahealth.api.meeting.APIServiceImpl;
import com.eviahealth.eviahealth.R;

import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FrameDialogRechazar#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FrameDialogRechazar extends Fragment {

    final String TAG = "FrameDialogRechazar";
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // TOM:
    TextView textFechaMeeting;
    Button buttonCancelar, buttonRechazar;

    public FrameDialogRechazar() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FrameDialogRechazar.
     */
    // TODO: Rename and change types and number of parameters
    public static FrameDialogRechazar newInstance(String param1, String param2) {
        FrameDialogRechazar fragment = new FrameDialogRechazar();
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_dialog_rechazar, container, false);

        Log.e(TAG,"onCreateView()");
        View root = inflater.inflate(R.layout.fragment_dialog_rechazar, container, false);

        buttonCancelar= root.findViewById(R.id.buttonCancelar);
        buttonRechazar = root.findViewById(R.id.buttonRechazar);

        //region :: onClick >> buttonRechazar
        buttonRechazar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG,"onClick >> buttonRechazar");

                SharedPreferences prefs = getActivity().getSharedPreferences("VariableEntorno", Context.MODE_PRIVATE);
                String idpaciente = prefs.getString("idpaciente", "");
                String idprescriptor = prefs.getString("idprescriptor", "");
                String motivo = "";

                RadioButton radioButtonNoDisponible = root.findViewById(R.id.radioButtonNoDisponible);
                RadioButton radioButtonHospitalizado = root.findViewById(R.id.radioButtonHospitalizado);
//                RadioButton radioButtonOtro = root.findViewById(R.id.radioButtonOtro);

                if (radioButtonNoDisponible.isChecked()) {
                    motivo = getString(R.string.nodisponible);
                }
                else if (radioButtonHospitalizado.isChecked()) {
                    motivo = getString(R.string.hospitalizado);
                } else {
                    // Otro
                    EditText editMotivo = root.findViewById(R.id.editMotivo);
                    motivo = editMotivo.getText().toString();

                    if (motivo.length() == 0 || motivo == null) {
                        Toast.makeText(getActivity(), "No se ha especificado motivo", Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                // CONSULTA Backend
                APIServiceImpl serviceZoom = new APIServiceImpl();
                JSONObject action = serviceZoom.refusedMeeting(getActivity(),idpaciente,idprescriptor,motivo);

                try {
                    if (!action.has("httpCode")) {
                        // error de comunicación
                        Log.e(TAG,"refusedMeeting() not param httpCode");
                    }
                    else if (action.getInt("httpCode") != HttpsURLConnection.HTTP_OK ) {
                        // error de comunicación
                        Log.e(TAG,"refusedMeeting() ERROR httpCode: " + action.getInt("httpCode"));
                    }
                    else {
                        Log.e(TAG,"refusedMeeting() OK");
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

        //region :: onClick >> buttonCancelar
        buttonCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG,"onClick >> buttonCancelar");
                FrameDialogConfirmar fm = new FrameDialogConfirmar();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.FrameConfirmMeeting, fm)
                        .addToBackStack(null)
                        .commit();
            }
        });
        //endregion

        return root;
    }
}