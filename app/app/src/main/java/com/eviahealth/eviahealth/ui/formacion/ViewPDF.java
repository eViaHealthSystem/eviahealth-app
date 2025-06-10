package com.eviahealth.eviahealth.ui.formacion;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.github.barteksc.pdfviewer.PDFView;

public class ViewPDF extends BaseActivity {

    private PDFView pdfView;
    private Button btnSalir;

    String pdfPath = "POLITICADEPRIVACIDAD.pdf";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pdf);

        pdfView = findViewById(R.id.pdfView);
        btnSalir = findViewById(R.id.btnSalir);

        String doc = getIntent().getStringExtra("document");

        if (doc != null) { pdfPath = doc;
            // Carga el archivo PDF desde la carpeta Asset del proyecto
            pdfView.fromAsset(pdfPath)
                    .defaultPage(0) // Página inicial
                    .enableSwipe(true) // Permite deslizar entre páginas
                    .swipeHorizontal(false) // Desliza verticalmente
                    .enableDoubletap(true) // Permite hacer zoom al doble toque
                    .load();

            pdfView.setMinZoom(0.5f);
            pdfView.setMaxZoom(3.0f);
            pdfView.zoomTo(2.5f);
        }

        // Configura el click listener para el botón de salir
        btnSalir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Cierra la actividad actual
            }
        });
    }

}