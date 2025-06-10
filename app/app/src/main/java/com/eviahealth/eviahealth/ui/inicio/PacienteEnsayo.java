package com.eviahealth.eviahealth.ui.inicio;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.utils.FileAccess.CarpetaEnsayo;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.models.SecuenciaActivity;
import com.eviahealth.eviahealth.models.devices.EquiposEnsayoPaciente;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.utils.PermissionUtils;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;

import org.json.JSONException;
import org.json.JSONObject;

public class PacienteEnsayo extends BaseActivity implements View.OnClickListener {

    final String TAG = "PacienteEnsayo";
    final String FASE = "ID PACIENTE ENSAYO";

    EditText editPaciente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paciente_ensayo);
        Log.e(TAG, "onCreate()");

        PermissionUtils.requestAll(this);

        editPaciente = findViewById(R.id.editPaciente);

    }

    @Override
    public void onClick(View view) {
        Log.e(TAG," onClick()");
        int viewId = view.getId();
        if (viewId == R.id.PAE_btCancelar) {
            //region >> Button Cancelar
            EVLog.log(TAG, "CANCELAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "PULSADO CANCELAR");
            startActivity(new Intent(this, Inicio.class));
            finish();
            //endregion
        }
        else if (viewId == R.id.PAE_btVerificar) {
            //region >> Button Continuar

            EVLog.log(TAG, "VERIFICAR >> onclick()");
            EnsayoLog.log(FASE, TAG, "VERIFICAE PACIENTE");

            String idpaciente = editPaciente.getText().toString();
            if (TextUtils.isEmpty(idpaciente)) {
                Toast.makeText(this, "El identificador del paciente no puede estar vacio.", Toast.LENGTH_LONG).show();
                return;
            }

            idpaciente = cleanString(idpaciente);
            Log.e(TAG, "ID PACIENTE: " + idpaciente);

            Boolean existe = ApiMethods.existeIdPaciente(idpaciente);
            if (existe) {
                try {
                    String name = ApiMethods.getNameIdPaciente(idpaciente);
                    JSONObject data = new JSONObject(name);
                    if (data.has("nombre") && data.has("apellidos")) {
                        // Mostrar cuadro de confirmación de id nombre paciente
                        dialogConfirm(idpaciente, data.getString("nombre"), data.getString("apellidos"));
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "No ha sido posible obtener la información del paciente.", Toast.LENGTH_LONG).show();
                }
            }
            else {
                Toast.makeText(this, "No ha sido posible obtener la información del paciente.", Toast.LENGTH_LONG).show();
            }
            //endregion
        }
    }

    public static String cleanString(String input) {
        // Usamos una expresión regular para reemplazar todo lo que no sea una letra o un número
        return input.replaceAll("[^a-zA-Z0-9]", "");
    }

    private void dialogConfirm(String idpaciente, String nombre, String apellidos) {

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.fragment_confirmar_paciente);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
//        dialog.setTitle("Title...");

        // set the custom dialog components - text, image and button
        TextView text = (TextView) dialog.findViewById(R.id.textIdPaciente);
        text.setText("" + idpaciente);

        TextView text2 = (TextView) dialog.findViewById(R.id.textNombre);
        text2.setText("" + nombre.toUpperCase() + "\n" + apellidos.toUpperCase());

        //region :: Button Aceptar
        Button buttonAceptar = (Button) dialog.findViewById(R.id.buttonAceptar);
        buttonAceptar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG,"Button Aceptar");
                dialog.dismiss();
                Config.getInstance().setIdPacienteEnsayo(idpaciente);

                // Actualiza dispositivos configurados actualmente al paciente establecido
                EquiposEnsayoPaciente dispositivos = new EquiposEnsayoPaciente(idpaciente);
                dispositivos.updateDispositivos();

                IniciarEnsayo();
                finish();
            }
        });
        //endregion

        //region :: Button Rechazar
        Button buttonRechazar = (Button) dialog.findViewById(R.id.buttonRechazar);
        buttonRechazar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG,"Button Rechazar");
                dialog.dismiss();
                PacienteEnsayo.this.startActivity(new Intent(PacienteEnsayo.this, Inicio.class));
                finish();
            }
        });
        //endregion
        dialog.show();
    }

    private void IniciarEnsayo() {
        CarpetaEnsayo.generarCarpetaEnsayo();
        EVLog.setFechaActual();
        EVLog.log(TAG, "onClick() >> Inicio Ensayo " + getVersionNameBuild()); // BuildConfig.VERSION_NAME
        EVLog.log(TAG, SecuenciaActivity.getInstance().toString());

        EnsayoLog.setFechaActual();
        EnsayoLog.log(TAG, "", "Inicio de Ensayo");

        FileAccess.writeDateEnsayo(FilePath.DATE_TEST);

        SecuenciaActivity.getInstance().next(PacienteEnsayo.this);
    }

    private String getVersionNameBuild() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            Log.e("Version Name: ", versionName);
            return versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}
}