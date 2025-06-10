package com.eviahealth.eviahealth.ui.inicio;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.models.devices.Config;

import org.json.JSONException;
import org.json.JSONObject;

public class Consentimiento extends DialogFragment {

    final String TAG = Consentimiento.class.getSimpleName(); //"CONSENTIMIENTO-DIALOG";
    private WebView webView;
    private Button btnAceptar, btCerrar, btnTecnico;
    private CheckBox checkBox;
    private static int TIMEOUT_CONSENTIMIENTO= 1000 * 60 * 2;  // 2 minutos
    private CountDownTimer cTimerConsentimiento = null; // Timeout para aceptar o cancelar consentimiento
    private CountDownTimer cTimer = null;               // Timeout de pulsación del botón tecnico
    private int secuencia = 0;
    private String htmlContent;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_navegador, null);

        webView = view.findViewById(R.id.webViewDialog);
        btnAceptar = view.findViewById(R.id.btnAceptar);
        btCerrar = view.findViewById(R.id.btnCerrar);
        btnTecnico = view.findViewById(R.id.btnTecnico);
        checkBox = view.findViewById(R.id.checkBoxAceptar);

        setupWebView();
        setupInteraction();


        AlertDialog builder = new AlertDialog.Builder(requireActivity(), R.style.CustomDialog_NoTitle)
                .setView(view)
                .create();

        builder.setCancelable(false);             // Previene cancelación por botón atrás
        builder.setCanceledOnTouchOutside(false); // Previene cancelación por toque fuera

        return builder;
    }

    private void setupInteraction() {
        btnAceptar.setEnabled(false);

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnAceptar.setEnabled(isChecked);
        });

        btnAceptar.setOnClickListener(v -> {
            Log.e(TAG, "Botón Aceptar pulsado.");
            if (listener != null) listener.onAceptar();
            dismiss(); // o realiza otra acción
        });

        btCerrar.setOnClickListener(v -> {
            if (listener != null) listener.onCerrar();
            dismiss();
        });

        btnTecnico.setOnClickListener(v -> {
            cTimer.cancel();
            if (secuencia == 0) {
                secuencia = 1;
                cTimer.start();
            }
            else {
                secuencia += 1;
                if (secuencia >= 4) {
                    if (listener != null) listener.onTecnico();
                    dismiss();
                }
                else { cTimer.start(); };
            }
            Log.e(TAG,"Pulsación: " + secuencia);
        });
    }

    private void setupWebView() {
        webView.setWebViewClient(new MyWebViewClient());
        webView.setWebChromeClient(null);                   // Quita soporte avanzado

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(false);            // Desactiva JavaScript
        webSettings.setAllowContentAccess(false);           // Previene acceso a contenido
        webSettings.setAllowFileAccess(false);              // Previene acceso a archivos
        webSettings.setDomStorageEnabled(false);            // Desactiva almacenamiento local
        webSettings.setDatabaseEnabled(false);              // Desactiva bases de datos

        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        webView.setVerticalScrollBarEnabled(true);      // Asegura que está habilitado
        webView.setScrollbarFadingEnabled(false);       // Evita que desaparezca tras un tiempo
        webView.setHorizontalScrollBarEnabled(false);   // Deshabilitado scroll horizontal
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET); //View.SCROLLBARS_INSIDE_INSET, View.SCROLLBARS_INSIDE_OVERLAY
        webView.clearCache(true);

        //region :: Carga texto de consentimiento en formato HTML
        Log.e(TAG, "setupWebView()");

        String paciente = Config.getInstance().getIdPacienteTablet();
        try {
            String json = ApiMethods.getTextConsentimiento(paciente);

            JSONObject texto = new JSONObject(json);
            if (texto.has("texto")) {
                htmlContent = texto.getString("texto");
                if (htmlContent.contains("<html") == false) {
                    htmlContent = "<html>" + '\n' + htmlContent + '\n' + "</html>";
                }
            }
            else { htmlContent = "about:blank"; }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        //endregion

//        Log.e(TAG, "TEXT CONCENTIMIENTO: " + htmlContent);
        // Cargar solo HTML básico
        webView.loadData(htmlContent, "text/html", "UTF-8");

        //region :: TimeOut Pulsaciones (5 seg x tecla)
        secuencia = 0;
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

        //region :: TimeOut CONSENTIMIENTO (2 min)
        cTimerConsentimiento = new CountDownTimer(TIMEOUT_CONSENTIMIENTO, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimerConsentimiento.cancel();
                if (listener != null) listener.onCerrar();
                dismiss();
            }
        };
        cTimerConsentimiento.start();
        //endregion
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            new AlertDialog.Builder(requireContext())
                    .setMessage("Certificado SSL no válido. ¿Desea continuar?")
                    .setPositiveButton("Continuar", (dialog, which) -> handler.proceed())
                    .setNegativeButton("Cancelar", (dialog, which) -> handler.cancel())
                    .create().show();

            Log.e(TAG, "Error SSL: " + error.toString());
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();

            if (ni != null && ni.isConnected()) {
                Log.e(TAG, "Error de carga. Código: " + errorCode + ", Desc: " + description);
            } else {
                Log.e(TAG, "Sin conexión a Internet.");
            }
        }
    }

    @Override
    public void onDestroy() {

        if (cTimerConsentimiento != null) {
            cTimerConsentimiento.cancel();
            cTimerConsentimiento = null;
        }

        if (cTimer!= null) {
            cTimer.cancel();
            cTimer = null;
        }

        super.onDestroy();
        Log.e(TAG,"onDestroy()");
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        Log.e(TAG,"onCancel()");
        if (listener != null) listener.onCerrar();
    }

    public interface OnDialogInteractionListener {
        void onAceptar();
        void onCerrar();
        void onTecnico();
    }

    private OnDialogInteractionListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnDialogInteractionListener) {
            listener = (OnDialogInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " debe implementar OnDialogInteractionListener");
        }
    }
}
