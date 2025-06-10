package com.eviahealth.eviahealth.ui.ensayo.ecg;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.utils.log.EVLog;

public class MyBrowsable extends BaseActivity implements View.OnClickListener  {

    final String TAG = "MyBrowsable";
    final String FASE = "MyBrowsable";
    private WebView webView;
    String home_url = "about:blank";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_browsable);

        Log.e(TAG,"onCreate()");

        // Configurar WebView
        webView = findViewById(R.id.webView2);
        setupWebView();

        // Obtener la URL desde el Intent
        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();

        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            home_url = data.toString(); // Capturar la URL que se intenta abrir
            Log.e(TAG, "URL recibida: " + home_url);
        }

        webView.setWebViewClient(new MyBrowsable.MyWebViewClient());
        Log.e(TAG, "url: " + home_url);

        if (home_url.contains("i-need-help-pre-recording-error-device-not-found")) {
            // web ecg no detectado
            home_url = "file:///android_asset/360026081453.html";
        }
        else  if (home_url.contains("i-need-help-recording-6l-single-lead")) {
            // ayuda single
            home_url = "file:///android_asset/360025943034.html";
        }
        else  if (home_url.contains("i-need-help-recording-6l-six-lead")) {
            // ayuda six
            home_url = "file:///android_asset/360025942974.html";
        }
        else  if (home_url.contains("i-need-help-too-short")) {
            // medición corta, (no debería ocurrir)
            home_url = "file:///android_asset/360025943394.html";
        }
        else  if (home_url.contains("115015801528")) {
            // información adicional se va desde la de medición cota (no deberia acurrir)
            home_url = "file:///android_asset/115015801528.html";
        }
        Log.e(TAG,"url: (2) " + home_url);
        webView.loadUrl(home_url);
    }

    /**
     * Configuración del WebView con mejores prácticas de seguridad y navegación.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.setWebViewClient(new MyWebViewClient());    // eventos como errores SSL y errores HTTP.
        webView.setWebChromeClient(new WebChromeClient());  //  eventos avanzados como alertas de JavaScript o carga de contenido multimedia.

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);             // Des/Activa JavaScript (necesario para la mayoría de los sitios web modernos).
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE); // No usa caché (puede consumir más datos y ser más lento).
        webSettings.setBuiltInZoomControls(true);           // Habilita el zoom en la página.
        webSettings.setDisplayZoomControls(false);          // Oculta los controles visuales del zoom (permite gestos de zoom sin botones).
        webSettings.setDomStorageEnabled(false);     // DesHabilita el almacenamiento local de sitios web, mejorando la compatibilidad con aplicaciones web modernas.
        webSettings.setAllowFileAccess(false);      // Evita accesos no autorizados a archivos del dispositivo.
        webSettings.setAllowContentAccess(false);   // Evita accesos no autorizados a archivos del dispositivo.
        webSettings.setLoadsImagesAutomatically(true);  // Carga imágenes automáticamente en la página web.
        webView.clearCache(true);     // Borra la caché para evitar cargar versiones antiguas de los sitios.
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            final SslErrorHandler sslErrorHandler = handler;
            Log.e(TAG, "onReceivedSslError()");

            AlertDialog.Builder builder = new AlertDialog.Builder(MyBrowsable.this);
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
            view.loadUrl("about:blank");
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            // Manejar el error de archivo no encontrado
            // Mostrar un mensaje de error, cargar una imagen de error alternativa, etc.
            Log.e(TAG, "onReceivedHttpError()");
            Log.e(TAG, "Error HTTP: " + errorResponse.getStatusCode());
            view.loadUrl("about:blank");
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Log.e(TAG,"shouldOverrideUrlLoading(): " + request.getUrl().toString());

            if (home_url.contains("115015801528")) {
                // web ecg no detectado
                String url = "file:///android_asset/115015801528.html";
                view.loadUrl(url);
            }
            else {
                view.loadUrl(request.getUrl().toString());
            }
            return true;
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