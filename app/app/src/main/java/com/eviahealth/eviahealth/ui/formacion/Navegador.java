package com.eviahealth.eviahealth.ui.formacion;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.utils.log.EVLog;

public class Navegador extends BaseActivity implements View.OnClickListener {

    final String TAG = "NAVEGADOR-VIEW ";
    final String FASE = "NAVEGADOR";
    private WebView webView;
    String home_url = "https://escuelapacientes.riojasalud.es/enfermedades-respiratorias/epoc/educacion-pacientes";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navegador);

        Log.e(TAG,"onCreate()");

        // Configurar WebView
        webView = findViewById(R.id.webView2);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Habilitar el zoom de pantalla
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // Borrar la caché
        webView.clearCache(true);

        // Cargar URL
        String url = getIntent().getStringExtra("home_url");
        if (url != null) {
            home_url = url;
        }

        webView.setWebViewClient(new MyWebViewClient());
        Log.e(TAG, "url: " + home_url);
        webView.loadUrl(home_url);
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            final SslErrorHandler sslErrorHandler = handler;
            Log.e(TAG, "onReceivedSslError()");

            AlertDialog.Builder builder = new AlertDialog.Builder(Navegador.this);
            builder.setMessage("El sitio web tiene un certificado SSL no válido. ¿Desea continuar?");
            builder.setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Permite la carga de la página a pesar del error SSL
                    sslErrorHandler.proceed();
                }
            });
            builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Cancela la carga de la página
                    sslErrorHandler.cancel();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();

            // Registra un comentario en el log
            Log.e(TAG, "Error SSL: " + error.toString());
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            // Verificar el estado de la red
            Log.e(TAG, "onReceivedError()");
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.isConnected()) {
                // Error de carga de página
                Log.e(TAG, "Error de carga de página. Código: " + errorCode + ", Descripción: " + description);
            } else {
                // Error de red, el dispositivo no está conectado a Internet
                Log.e(TAG, "Error de red, el dispositivo no está conectado a Internet.");
            }
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            // Manejar el error de archivo no encontrado
            // Mostrar un mensaje de error, cargar una imagen de error alternativa, etc.
            Log.e(TAG, "onReceivedHttpError()");
            Log.e(TAG, "Error HTTP: " + errorResponse.getStatusCode());
        }

    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.btnHomeWeb) {
            // Botón Home
            webView.clearCache(true);
            webView.loadUrl(home_url);
        }
        else if (viewId == R.id.btnBackWeb) {
            // Botón Retroceder
            if (webView.canGoBack()) {
                webView.goBack();
            }
        }
        else if (viewId == R.id.btnExitWeb) {
            // Botón Salir
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        EVLog.log(TAG,"onDestroy()");
        super.onDestroy();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}
}
