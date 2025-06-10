package com.eviahealth.eviahealth.ui.formacion;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.eviahealth.eviahealth.api.BackendConector.ApiMethods;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class EducationTraining extends BaseActivity {

    final String TAG = "EducationTraining";

//    private LinkItem[] linkItems = {
//            new LinkItem("Formación EPOC", "https://escuelapacientes.riojasalud.es/enfermedades-respiratorias/epoc/educacion-pacientes", R.drawable.doctor2,"web"),
//            new LinkItem("Formación Manual instrucciones","https://www.google.com", R.drawable.formacion,"web"),
//            new LinkItem("Video web","https://joy1.videvo.net/videvo_files/video/free/2020-10/large_watermarked/elizabethschummer_11929588430682337400698424756070231523524052n_1602015842_preview.mp4", R.drawable.zoom2,"video"),
//            new LinkItem("Política de privacidad","POLITICADEPRIVACIDAD.pdf", R.drawable.politica_calidad,"pdf")
//    };
    private LinkItem[] linkItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_education_training);

        // Obtener LinkItem de la db
        int regFormacion = 0;
        try {
            JSONObject vOb = ApiMethods.getFormacion(getApplicationContext());
            Log.e(TAG, "vOb: " + vOb.toString());

            if (vOb.getInt("httpCode") != HttpsURLConnection.HTTP_OK) {
                vOb = new JSONObject();
                // Crear el JSONArray "formacion"
                JSONArray formacionArray = new JSONArray();
                // Agregar el JSONArray al JSON principal
                vOb.put("formacion", formacionArray);
            }

            JSONArray arrayFormacion = vOb.getJSONArray("formacion");
            regFormacion = arrayFormacion.length();
            Log.e(TAG,"len: " + regFormacion);
            List<LinkItem> listado = new ArrayList<>();

            if (regFormacion != 0) {
                for (int i = 0; i < arrayFormacion.length(); i++) {
                    JSONObject objeto = arrayFormacion.getJSONObject(i);

                    int idIcon = selectIcon(objeto.getInt("icon"));
                    LinkItem nuevo = new LinkItem(objeto.getString("title"), objeto.getString("url"), idIcon, objeto.getString("type"));

                    // Añadimos al listado de formación todos los tipos exepto type="politica"
                    if (!objeto.getString("type").equals("politica")) {
                       listado.add(nuevo);
                    }
                }
            }

            regFormacion = listado.size();
            if (regFormacion != 0) {
                linkItems = new LinkItem[regFormacion];
                for (int i = 0; i < listado.size(); i++) {
                    linkItems[i] = listado.get(i);
                }
            }

        }
        catch (JSONException e) {
            Log.e(TAG, "EXCEPTION >> JSONException queryFormacion(): " + e.toString());
            e.printStackTrace();
        }

        if (regFormacion == 0) {
            Toast.makeText(this, "Actualmente no hay formación disponible.", Toast.LENGTH_LONG).show();
        }

        Button salir = findViewById(R.id.exitButton);
        salir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        ListView linkListView = findViewById(R.id.linkListView);

        if (linkItems != null) {
            LinkAdapter adapter = new LinkAdapter(this, linkItems);
            linkListView.setAdapter(adapter);

            linkListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String url = linkItems[position].getUrl();
                    String type = linkItems[position].getType();

                    if (type.equals("web")) {
                        Intent i = new Intent(EducationTraining.this, Navegador.class);
                        i.putExtra("home_url", url);
                        startActivity(i);
                    } else if (type.equals("pdf")) {
                        Intent i = new Intent(EducationTraining.this, ViewPDF.class);
                        i.putExtra("document", url);
                        startActivity(i);
                    }
                    else if (type.equals("pdfweb")) {
                            Intent i = new Intent(EducationTraining.this, ViewPdfWeb.class);
                            i.putExtra("pdf_url", url);
                            startActivity(i);
                    } else if (type.equals("video")) {
                        Intent i = new Intent(EducationTraining.this, VideoActivity.class);
                        i.putExtra("url", url);
                        startActivity(i);
                    }


//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//                startActivity(intent);
                }
            });
        }
    }

    private class LinkItem {
        private String url;
        private String title;
        private int imageRes;
        private String type;

        public LinkItem(String title, String url, int imageRes, String type) {
            this.url = url;
            this.title = title;
            this.imageRes = imageRes;
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public int getImageRes() {
            return imageRes;
        }

        public String getType() {
            return type;
        }
    }

    private class LinkAdapter extends ArrayAdapter<LinkItem> {

        private Context context;
        private LinkItem[] items;

        public LinkAdapter(Context context, LinkItem[] items) {
            super(context, 0, items);
            this.context = context;
            this.items = items;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.link_item, parent, false);
            }

            LinkItem item = items[position];

            ImageView imageView = convertView.findViewById(R.id.linkImage);
            imageView.setImageResource(item.getImageRes());

            TextView textView = convertView.findViewById(R.id.linkText);
            textView.setText(item.getTitle());

            return convertView;
        }
    }

    private Integer selectIcon(int icon) {
        Integer idIcon = 0;

        if (icon == 0) idIcon = R.drawable.doctor2;
        else if (icon == 1) idIcon = R.drawable.formacion;
        else if (icon == 2) idIcon = R.drawable.politica_calidad;
        else if (icon == 3) idIcon = R.drawable.zoom2;
        else if (icon == 4) idIcon = R.drawable.manual;
        else idIcon = R.drawable.formacion;

        return idIcon;
    }
}