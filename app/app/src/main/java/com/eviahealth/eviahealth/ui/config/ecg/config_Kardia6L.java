package com.eviahealth.eviahealth.ui.config.ecg;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.devices.BeurerReferences;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.utils.PermissionUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class config_Kardia6L extends BaseActivity implements View.OnClickListener {

    final static String TAG = "CONFIG K6L";
    Spinner spnLeadConfig, spnFiltreType, spnMaxDuration, spnFrequency;
    CheckBox checkLeadsButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_kardia6_l);
        Log.e(TAG,"onCreate()");

        PermissionUtils.requestAll(this);

        //region :: Views
        checkLeadsButtons = findViewById(R.id.check_leads_buttons);
        checkLeadsButtons.setChecked(false);

        spnLeadConfig = findViewById(R.id.spn_lead_config);
        spnFiltreType = findViewById(R.id.spn_filtre_type);
        spnMaxDuration = findViewById(R.id.spn_max_duration);
        spnFrequency = findViewById(R.id.spn_frequency);

        uploadLeadConfig();
        uploadFiltreType();
        uploadMaxDuration();
        uploadFrequency();
        //endregion

        //region :: BUNDLE
        Intent intent = getIntent();
        Bundle b = intent.getExtras();
        if (b != null) {
            String extra = intent.getStringExtra("k6l");
            try {
                JSONObject params = new JSONObject(extra);
                Log.e(TAG, "extra: " + params);

                if (params.has("k6l")) {
                    JSONObject k6l = params.getJSONObject("k6l");
                    Log.e(TAG, "k6l: " + k6l);

                    if (k6l.has("enableLeadsButtons")) {
                        checkLeadsButtons.setChecked(k6l.getBoolean("enableLeadsButtons"));
                    }

                    if (k6l.has("leadConfiguration")) {
                        setValueSpinner(spnLeadConfig,k6l.getString("leadConfiguration"),typeListLeadConfig);
                    }

                    if (k6l.has("filterType")) {
                        setValueSpinner(spnFiltreType,k6l.getString("filterType"),typeListFiltreType);
                    }

                    if (k6l.has("maxDuration")) {
                        setValueMaxDuration(k6l.getInt("maxDuration"));
                    }

                    if (k6l.has("mainsFrequency")) {
                        setValueFrequency(k6l.getInt("mainsFrequency"));
                    }
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG,"Exceprion: " + e.toString());
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }

        }
        //endregion
    }

    // region :: ONCLICK
    @Override
    public void onClick(View view) {
        Log.e(TAG, " onClick()");

        int viewId = view.getId();
        if (viewId == R.id.btn_save) {
            //region :: GUARDAR
            try {
                JSONObject message = new JSONObject();
                message.put("deviceinfo","k6l");

                JSONObject params = new JSONObject();
                params.put("enableLeadsButtons", checkLeadsButtons.isChecked());
                params.put("leadConfiguration", spnLeadConfig.getSelectedItem().toString());
                params.put("filterType", spnFiltreType.getSelectedItem().toString());
                params.put("maxDuration", getValueMaxDuration(spnMaxDuration.getSelectedItemPosition()));
                params.put("resetDuration", 10);
                params.put("mainsFrequency", getValueFrequency(spnFrequency.getSelectedItemPosition()));

                message.put("k6l",params);
                Log.e(TAG, "************* config k6l: " + message.toString());
                broadcastUpdate(BeurerReferences.ACTION_EXTRA_DATA, message.toString());

                finish();
            }
            catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG,"Exceprion: " + e.toString());
            }
            //endregion
        }
        else if (viewId == R.id.btn_cancel) {
            finish();
        }

    }
    //endregion

    private void broadcastUpdate(final String action, String message) {
        final Intent intent = new Intent(action);
        intent.putExtra(BeurerReferences.ACTION_EXTRA_MESSAGE, message);
        this.sendBroadcast(intent);
    }

    //region :: Relleno de Spinner
    private static final List<String> typeListLeadConfig = List.of("SINGLE", "SIX");
    private static final List<String> typeListFiltreType = List.of("ORIGINAL", "ENHANCED");
    private static final List<String> typeListMaxDuration = List.of("30 sec", "1 min", "2 min", "3 min", "4 min", "5 min");
    private static final List<Integer> valueListMaxDuration = List.of(30, 60, 120, 180, 240, 300);
    private static final List<String> typeListFrequency = List.of("50Hz", "60Hz");
    private static final List<Integer> valueListFrequency = List.of(50, 60);
    private void uploadLeadConfig(){
        ArrayAdapter types = new ArrayAdapter(this,R.layout.custom2_spinner_item, typeListLeadConfig);
        types.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnLeadConfig.setAdapter(types);
    }
    private void uploadFiltreType(){
        ArrayAdapter types = new ArrayAdapter(this,R.layout.custom2_spinner_item, typeListFiltreType);
        types.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnFiltreType.setAdapter(types);
    }
    private void uploadMaxDuration(){
        ArrayAdapter types = new ArrayAdapter(this,R.layout.custom2_spinner_item, typeListMaxDuration);
        types.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnMaxDuration.setAdapter(types);
    }
    private void uploadFrequency(){
        ArrayAdapter types = new ArrayAdapter(this,R.layout.custom2_spinner_item, typeListFrequency);
        types.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnFrequency.setAdapter(types);
    }
    //endregion

    //region :: Establecer valor al spinner
    private void setSelectionIdSpinner(Spinner spn, int id) {
        runOnUiThread(new Runnable() {
            public void run() {
                spn.setSelection(id);
            }
        });
    }

    private void setValueSpinner(Spinner spn, String value, List<String> list) {
        int position = list.indexOf(value); // Obtiene la posición de 'value' en la lista
        setSelectionIdSpinner(spn, position);
    }
    private void setValueMaxDuration(Integer value) {
            int position = 0;
            if (value == 60) { position = 1; }
            else if (value == 120) { position = 2; }
            else if (value == 180) { position = 3; }
            else if (value == 240) { position = 4; }
            else if (value == 300) { position = 5; }
        setSelectionIdSpinner(spnMaxDuration, position);
    }
    private Integer getValueMaxDuration(Integer position) {
        return valueListMaxDuration.get(position);
    }
    private void setValueFrequency(Integer value) {
        int position = 0;
        if (value == 60) { position = 1; }
        setSelectionIdSpinner(spnFrequency, position);
    }
    private Integer getValueFrequency(Integer position) {
        return valueListFrequency.get(position);
    }

    //endregion
}