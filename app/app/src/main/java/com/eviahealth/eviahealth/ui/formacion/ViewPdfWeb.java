package com.eviahealth.eviahealth.ui.formacion;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.eviahealth.eviahealth.R;

import com.github.barteksc.pdfviewer.PDFView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class ViewPdfWeb extends AppCompatActivity {

    private PDFView pdfView;
    private Button btnSalir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pdfweb);

        pdfView = findViewById(R.id.pdfView);
        btnSalir = findViewById(R.id.btnSalir);

        // Obtén la URL del PDF del Intent o de donde sea que la recibas
        String pdfUrl = getIntent().getStringExtra("pdf_url");

        // Carga el PDF directamente desde la URL
        if (pdfUrl != null) {
            Boolean exitFile = viewPdf(pdfUrl);
            // Si falla la carga carga la política por defecto
            if (exitFile == false) {
                Intent i = new Intent(this, ViewPDF.class);
                i.putExtra("document", "POLITICADEPRIVACIDAD.pdf");
                startActivity(i);
                finish();
            }
        }

        // Configura el click listener para el botón de salir
        btnSalir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Cierra la actividad actual
            }
        });
    }

    private Boolean viewPdf(String name) {
        try {
            InputStream inputStream = loadPDF(name);
            if (inputStream != null) {
                pdfView.fromStream(inputStream)
                        .defaultPage(0) // Página inicial
                        .enableSwipe(true) // Permite deslizar entre páginas
                        .swipeHorizontal(false) // Desliza verticalmente
                        .enableDoubletap(true) // Permite hacer zoom al doble toque
                        .load();

                pdfView.setMinZoom(0.5f);
                pdfView.setMaxZoom(3.0f);
                pdfView.zoomTo(2.5f);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("ViewPdfWeb","IOException: " + e.toString());
            Toast.makeText(this, "IOException: " + e.toString(), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private InputStream loadPDF(String pdfUrl) throws IOException {
        URL url = new URL(pdfUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        return new BufferedInputStream(connection.getInputStream());
    }

}