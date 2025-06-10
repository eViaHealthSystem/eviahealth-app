package com.eviahealth.eviahealth.ui.ensayo.encuesta;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.eviahealth.eviahealth.api.BackendConector.ApiConnector;
import com.eviahealth.eviahealth.api.BackendConector.ApiException;
import com.eviahealth.eviahealth.api.BackendConector.ApiUrl;
import com.eviahealth.eviahealth.utils.FileAccess.FileAccess;
import com.eviahealth.eviahealth.utils.FileAccess.FilePath;
import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.ui.inicio.Inicio;
import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.models.SecuenciaActivity;
import com.eviahealth.eviahealth.models.devices.Dispositivo;
import com.eviahealth.eviahealth.models.devices.EquipoPaciente;
import com.eviahealth.eviahealth.devices.NombresDispositivo;
import com.eviahealth.eviahealth.models.manual.encuesta.Pregunta;
import com.eviahealth.eviahealth.models.manual.encuesta.Respuesta;
import com.eviahealth.eviahealth.models.manual.encuesta.TipoPregunta;
import com.eviahealth.eviahealth.utils.Fecha.Fecha;
import com.eviahealth.eviahealth.utils.dialogs.Alerts;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.log.log_ensayo.EnsayoLog;
import com.eviahealth.eviahealth.utils.textvoz.TextToSpeechHelper;
import com.eviahealth.eviahealth.utils.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class  Encuesta extends BaseActivity {

    final String TAG = "ENCUESTA";
    final String FASE = "ENCUESTA";

    List<Pregunta> preguntas;
    LinearLayout parentLayout;
    LinearLayout parentLayout1;
    LinearLayout parentLayout2;
    LinearLayout parentLayout3;
    TextToSpeechHelper textToSpeech;
    Map<NombresDispositivo, Dispositivo> MAP_DISP_ID;
    Map<Integer, NombresDispositivo> MAP_ID_DISP;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.encuestaprueba);
        EVLog.log(TAG,"onCreate()");

        textToSpeech = new TextToSpeechHelper(getApplicationContext());

        parentLayout = findViewById(R.id.encuestaprueba1);
        calcularSize(parentLayout, 3);
        parentLayout1 = findViewById(R.id.encuestaprueba2);
        calcularSize(parentLayout1, 3);
        parentLayout2 = findViewById(R.id.encuestaprueba3);
        calcularSize(parentLayout2, 3);
        parentLayout3 = findViewById(R.id.encuestaprueba4);
        calcularSize(parentLayout3, 3);

        EnsayoLog.log(FASE,TAG,"PACIENTE en Encuesta");

        //region :: Descarga Encuestas de la DB y cagar preguntas del fichero
        try {
            // Descarga encuestas y dispositivos actuales de la db
            getRemoteData();
            // Carga las preguntas de archivo en List<Pregunta> preguntas
            obtenerPreguntas();
        }
        catch (IOException | JSONException | ApiException e) {
            e.printStackTrace();
            EVLog.log(TAG,"ApiException " + e.toString());
            EVLog.log(TAG,"Carga las preguntas de archivo");
            // Carga las preguntas de archivo
            obtenerPreguntas();
        }
        //endregion

    }

    private void obtenerPreguntas() {
        Log.e(TAG,"obtenerPreguntas()");
        try {
            this.preguntas = this.leerFicheroPreguntas();
            this.construirInterfaz(0);
        }
        catch (IOException | JSONException e) {
            e.printStackTrace();
            EVLog.log(TAG,"Exception obtenerPreguntas(): " + e.toString());
            ir_siguiente_actividad();
        }
    }

    /**
     * Solicitud api: Obtiene listado de dispositivos de la instalación >> "SELECT id, nombre, params, display FROM device"
     * @throws IOException
     * @throws ApiException
     * @throws JSONException
     */
    private void getRemoteData() throws IOException, ApiException, JSONException {

        Log.e(TAG,"getRemoteData()");

        JSONObject params = new JSONObject();
        params.put("idpaciente", Config.getInstance().getIdPacienteEnsayo());
//        JSONObject respuesta = ApiConnector.peticionJSONObject(ApiUrl.DISPOSITIVOS_GET, new JSONObject());
        JSONObject respuesta = ApiConnector.peticionJSONObject(ApiUrl.DISPOSITIVOS_GET, params);

        JSONArray dispositivos = respuesta.getJSONArray("devices");

        Map<NombresDispositivo, Dispositivo> map = new HashMap<>();
        Map<Integer, NombresDispositivo> id_disp_map = new HashMap<>();

        for(int i = 0; i < dispositivos.length(); i++) {
            JSONObject dispositivo = dispositivos.getJSONObject(i);

            int id = dispositivo.getInt("id");
            String nombre = dispositivo.getString("nombre");
            NombresDispositivo cual = NombresDispositivo.fromName(nombre);

            if(cual != null) {
                map.put(cual, new Dispositivo(id));
                id_disp_map.put(id, cual);
            }
        }

        MAP_DISP_ID = map;              // <NombreDispositivo(enum), Dispositivo(Class)>
        MAP_ID_DISP = id_disp_map;      // <Integer, NombreDispositivo(enum)>

        Log.e(TAG,"Genera el fichero de \"dispositivos.json\" y \"encuesta.json\"");
        escribirFicheros(getEquiposPaciente());
    }

    private Map<NombresDispositivo, EquipoPaciente> getEquiposPaciente() throws JSONException, ApiException, IOException {

        Log.e(TAG,"getEquiposPaciente()");
//        String idpaciente = Config.getInstance().getIdPacienteTablet();
        String idpaciente = Config.getInstance().getIdPacienteEnsayo();
        Log.e(TAG,"idpaciente: " + idpaciente);

        JSONObject params = new JSONObject();
        params.put("idpaciente", idpaciente);
        JSONArray dispositivos = ApiConnector.peticionJSONArray(ApiUrl.PACIENTE_EQUIPO_GET, params);

        Map<NombresDispositivo, EquipoPaciente> equipos_paciente = new HashMap<>();

        for(int i = 0; i < dispositivos.length(); i++) {
            JSONObject dispositivo = dispositivos.getJSONObject(i);
            int id = dispositivo.getInt("id_device");
            boolean enable = dispositivo.getInt("enable") == 1;
            String mac = dispositivo.getString("desc");
            String extra = dispositivo.getString("extra");

            NombresDispositivo cual = MAP_ID_DISP.get(id);
            if (cual != null) {
                equipos_paciente.put(cual, new EquipoPaciente(new Dispositivo(id), enable, mac, extra));
            }
        }
        return equipos_paciente;
    }

    /**
     * Genera el fichero de "dispositivos.json" y "encuesta.json"
     * @param equipos_paciente
     * @throws IOException
     * @throws JSONException
     */
    private void escribirFicheros(Map<NombresDispositivo, EquipoPaciente> equipos_paciente) throws IOException, JSONException {

        Log.e(TAG,"escribirFicheros()");
        JSONObject jsonequipos = new JSONObject();
        for(NombresDispositivo ndisp: equipos_paciente.keySet()) {

            if (ndisp.getNombre() != "ConcentradorO2" && ndisp.getNombre() != "CPAP") {
                EquipoPaciente equipo = equipos_paciente.get(ndisp);

                JSONObject jsoneq = new JSONObject();
                jsoneq.put("id", equipo.getDispositivo().getId());
                jsoneq.put("enable", equipo.isEnabled());
                jsoneq.put("desc", equipo.getDesc());
                jsoneq.put("extra", equipo.getExtra());
                jsonequipos.put(ndisp.getNombre(), jsoneq);

                //region >> WRITE Encuestas
                if (ndisp.getNombre() == "Encuesta"){
                    Log.e(TAG, "Write file encuesta.json");

                    JSONObject jsonencuesta = new JSONObject();
                    jsonencuesta.put("id_encuesta", Integer.parseInt(equipo.getDesc()));

                    try {
                        JSONArray jencuesta = getEncuesta();    // >> API GET
                        jsonencuesta.put("arrayencuesta", jencuesta);
                        FileAccess.escribirJSON(FilePath.CONFIG_ENCUESTA, jsonencuesta);
                    }
                    catch (JSONException | ApiException | IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "JSONException | ApiException | IOException: " + e.toString());
                        Alerts.mostrarAlert(this, "ERROR al generar el fichero de encuestas");
                    }

                    String contenido = readfile("encuesta.json");
                    Log.e(TAG,"DB ENCUESTAS: " + contenido);

                }
                //endregion
            }
        }

        Log.e(TAG,"DB dispositivos.json: " + jsonequipos.toString());
        FileAccess.escribirJSON(FilePath.CONFIG_DISPOSITIVOS, jsonequipos);
        
//        String content = jsonequipos.toString();
//        UriAccess.updateUriContent(this, MAPFileUri.get("dispositivos"),content);

    }

    private JSONArray getEncuesta() throws IOException, ApiException, JSONException {
        Log.e(TAG,"getEncuesta()");
//        String idpaciente = Config.getInstance().getIdPacienteTablet();
        String idpaciente = Config.getInstance().getIdPacienteEnsayo();
        JSONObject params = new JSONObject();
        params.put("idpaciente", idpaciente);
        JSONObject respuesta = ApiConnector.peticionJSONObject(ApiUrl.PACIENTE_GET_ENCUESTA, params);
        return respuesta.getJSONArray("preguntas");
    }

    private String readfile(String namefile){

        StringBuilder stringBuilder = new StringBuilder();
        String line;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(new File(FileAccess.getPATH_FILES(), namefile)));
            while ((line = in.readLine()) != null) stringBuilder.append(line);
        } catch (FileNotFoundException e) {
            Log.e("EXCEPCION","Exception readfile():" + e.toString());
            return "";
        } catch (IOException e) {
            Log.e("EXCEPCION","Exception readfile():" + e.toString());
            return "";
        }
        return stringBuilder.toString();
    }

    private List<Pregunta> leerFicheroPreguntas() throws IOException, JSONException {
        JSONObject config = FileAccess.leerJSON(FilePath.CONFIG_ENCUESTA);
        JSONArray preguntasjson = config.getJSONArray("arrayencuesta");
        List<Pregunta> preguntas = new ArrayList<>();

        int length = preguntasjson.length();
        for(int i = 0; i < length; i++) {
            JSONObject pregunta = preguntasjson.getJSONObject(i);
            Log.e(TAG, "Pregunta("+i+"): " + pregunta.toString());

            // construir lista de respuestas
            List<Respuesta> respuestas = new ArrayList<>();
            JSONArray posibles_respuestas = pregunta.getJSONArray("respuestas");
            int length_respuestas = posibles_respuestas.length();
            for(int j = 0; j < length_respuestas; j++) {
                JSONObject resp = posibles_respuestas.getJSONObject(j);
                respuestas.add(new Respuesta(resp.getString("texto"), resp.getInt("valor")));
            }

            // ver si tiene cat
            //Integer cat = pregunta.has("cat") ? (Integer) pregunta.get("cat") : null;
            Integer cat = null;
            try {
                cat = pregunta.getInt("cat");
            } catch(JSONException e) {}

            Log.e(TAG, "CAT: " + cat);
            Integer tipo_pregunta = pregunta.getInt("tipo_pregunta");
            if (cat == null && tipo_pregunta != 2) {
                // anyadir pregunta a la lista que se va a retornar
                preguntas.add(new Pregunta(
                        pregunta.getInt("id"),
                        pregunta.getString("Pregunta"),
                        cat,
                        TipoPregunta.fromInt(pregunta.getInt("tipo_pregunta")),
                        respuestas
                ));
            }

            // anyadir pregunta a la lista que se va a retornar
//            preguntas.add(new Pregunta(
//                    pregunta.getInt("id"),
//                    pregunta.getString("Pregunta"),
//                    cat,
//                    TipoPregunta.fromInt(pregunta.getInt("tipo_pregunta")),
//                    respuestas
//            ));

        }

        return preguntas;
    }

    private View crearViewChild(int layout) {
        LayoutInflater inflater = this.getLayoutInflater();
        View v = inflater.inflate(layout, null);
        return v;
    }

    private void construirInterfaz(final int indice) {

        Log.e(TAG,"construirInterfaz(" + indice + ")");
        parentLayout.removeAllViewsInLayout();
        parentLayout1.removeAllViewsInLayout();
        parentLayout2.removeAllViewsInLayout();
        parentLayout3.removeAllViewsInLayout();

        for(int i = indice; i < indice + 3; i++) {
            if(i < this.preguntas.size()) {

                Pregunta pregunta = this.preguntas.get(i);
                String texto_pregunta = pregunta.getTexto();
                TipoPregunta tipo_pregunta = pregunta.getTipo_pregunta();
                EVLog.log(TAG, "Construye pregunta >> [?]: " + texto_pregunta +" [Tipo?]: " + tipo_pregunta.getInt());

                // elegir tipo layout
                View childLayout = null;
                if(tipo_pregunta == TipoPregunta.DOS_RESPUESTAS) {
                    childLayout = crearViewChild(R.layout.activity_sino);
                }
                else if(tipo_pregunta == TipoPregunta.TRES_RESPUESTAS) {
                    childLayout = crearViewChild(R.layout.activity_tres);
                }
                else if(tipo_pregunta == TipoPregunta.CUATRO_RESPUESTAS) {
                    childLayout = crearViewChild(R.layout.activity_cuatro);
                }
                else if(tipo_pregunta == TipoPregunta.CINCO_RESPUESTAS) {
                    childLayout = crearViewChild(R.layout.activity_cinco);
                }

                // establecer textos de las preguntas
                if(tipo_pregunta == TipoPregunta.CINCO_RESPUESTAS) {
                    String[] partes_pregunta = texto_pregunta.split("/");
                    ((TextView) childLayout.findViewById(R.id.mensajeizquierda)).setText(partes_pregunta[0]);
                    ((TextView) childLayout.findViewById(R.id.mensajederecha)).setText(partes_pregunta[1]);
                } else {
                    TextView tx = childLayout.findViewById(R.id.mensajepregunta);
                    tx.setText(texto_pregunta);
                }

                RadioButton[] buttons = {
                        childLayout.findViewById(R.id.opcion0),
                        childLayout.findViewById(R.id.opcion1),
                        childLayout.findViewById(R.id.opcion2),
                        childLayout.findViewById(R.id.opcion3),
                        childLayout.findViewById(R.id.opcion4),
                        childLayout.findViewById(R.id.opcion5)
                };

                ImageButton imageButtonAudio = childLayout.findViewById(R.id.imageButtonAudio);
                imageButtonAudio.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(tipo_pregunta == TipoPregunta.DOS_RESPUESTAS)
                        {
                            List<String> texto = new ArrayList<>();
                            // Pregunta
                            String frase = texto_pregunta;
                            texto.add(frase);

                            texto.add("Marque la respuesta que más se acerque.");

                            // Respuestas posibles
                            List<Respuesta> res = pregunta.getPosibles_respuestas();
                            for (Respuesta n: res) {
                                texto.add(n.getTexto());
                            }

                            textToSpeech.speak(texto);
                        }
                        else if(tipo_pregunta == TipoPregunta.TRES_RESPUESTAS)
                        {
                            List<String> texto = new ArrayList<>();
                            // Pregunta
                            String frase = texto_pregunta;
                            texto.add(frase);

                            texto.add("Marque la respuesta que más se acerque.");

                            // Respuestas posibles
                            List<Respuesta> res = pregunta.getPosibles_respuestas();
                            for (Respuesta n: res) {
                                texto.add(n.getTexto());
                            }
                            textToSpeech.speak(texto);
                        }
                        else if(tipo_pregunta == TipoPregunta.CUATRO_RESPUESTAS)
                        {
                            List<String> texto = new ArrayList<>();
                            // Pregunta
                            String frase = texto_pregunta;
                            texto.add(frase);

                            texto.add("Marque la respuesta que más se acerque.");

                            // Respuestas posibles
                            List<Respuesta> res = pregunta.getPosibles_respuestas();
                            for (Respuesta n: res) {
                                texto.add(n.getTexto());
                            }
                            textToSpeech.speak(texto);
                        }
                        else if(tipo_pregunta == TipoPregunta.CINCO_RESPUESTAS)
                        {
                            String[] partes_pregunta = texto_pregunta.split("/");
                            List<String> texto = new ArrayList<>();
                            // Pregunta
                            String frase = partes_pregunta[0];
                            texto.add(frase);

                            // Respuesta
                            frase = partes_pregunta[1];
                            texto.add(frase);
                            textToSpeech.speak(texto);

                            texto.add("Marque la respuesta que más se acerque.");
                            // Respuestas posibles
                            List<Respuesta> res = pregunta.getPosibles_respuestas();
                            for (Respuesta n: res) {
                                texto.add(n.getTexto());
                            }


                        }
                    }
                });

                // establecer el button checked para la respuesta si ya esta contestada
                Integer valor_respuesta_actual = pregunta.getRespuesta();
                // Recorrer las posibles respuestas para asociarle el texto al boton
                // y el onclick para que modifique el valor de la respuesta de la pregunta
                final int finalI = i;
                for(int j = 0; j < pregunta.getPosibles_respuestas().size(); j++) {
                    final Respuesta resp = pregunta.getPosibles_respuestas().get(j);
                    // checkear el button si es la respuesta
                    if(valor_respuesta_actual != null && valor_respuesta_actual == resp.getValor()) {
                        buttons[j].setChecked(true);
                    }
                    // texto botones y onclick
                    buttons[j].setText(resp.getTexto());
                    buttons[j].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Pregunta p = preguntas.get(finalI);
                            p.setRespuesta(
                                    resp.getValor()
                            );
                            setContinueVisibility(indice);
                        }
                    });
                }

                if (i == indice) {
                    this.parentLayout.addView(childLayout);
                }else if (i == indice + 1){
                    this.parentLayout1.addView(childLayout);
                }else if (i == indice + 2){
                    this.parentLayout2.addView(childLayout);
                }
            }
        }
        // BOTONES ATRAS Y CONTINUAR
        View layout_botones = crearViewChild(R.layout.layout_boton);
        // BOTON ATRAS
        final Button atras_button = layout_botones.findViewById(R.id.backButton);
        // invisible si es es la pantalla de las primeras preguntas
        if(indice == 0) atras_button.setVisibility(View.INVISIBLE);
        // retroceder 3 al pinchar
        atras_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EVLog.log(TAG,"BOTON ATRAS >> setContinueVisibility(indice)");
                textToSpeech.stop();
                construirInterfaz(indice - 3);
            }
        });

        // BOTON CONTINUAR
        final Button continue_button = layout_botones.findViewById(R.id.continueButton);
        // si aun quedas preguntas
        if(indice + 3 < this.preguntas.size())
        {
            // avanzar 3 al pinchar
            continue_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EVLog.log(TAG,"BOTON CONTINUAR >> setContinueVisibility(indice)");
                    textToSpeech.stop();
                    construirInterfaz(indice + 3);
                }
            });
        }
        else
        {
            // terminar encuesta
            continue_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    continue_button.setEnabled(false);
                    atras_button.setEnabled(false);
                    textToSpeech.stop();
                    terminar();
                }
            });
        }

        this.parentLayout3.addView(layout_botones);
        setContinueVisibility(indice);
    }

    private boolean deberiaVerCotinuar(int indice) {
        int aux = indice;
        Log.e(TAG, "deberiaVerCotinuar(" + indice + ")");
        if (this.preguntas.size() - indice >= 3) {
            aux = 3;
        }else {
            aux = this.preguntas.size() - indice;
        }

        for (int i = indice; i < indice + aux; i++)
            if (this.preguntas.get(i).getRespuesta() == null)
                return false;

        return true;
    }

    private void setContinueVisibility(int indice) {
        Log.e(TAG,"setContinueVisibility()");
        Button continue_button = findViewById(R.id.continueButton);
        if(deberiaVerCotinuar(indice)) {
            continue_button.setVisibility(View.VISIBLE);

        } else {
            continue_button.setVisibility(View.INVISIBLE);
        }
    }

    private void terminar() {
        EVLog.log(TAG,"onClick(Terminar)");
        EnsayoLog.log(FASE,TAG,"PACIENTE ha terminado la Encuenta");
        try {
            String fecha = Fecha.getFechaYHoraActual();
            JSONArray array_resultados = new JSONArray();

            // map idcat > sumacat
            Map<Integer, Integer> cats = new HashMap<>();

            for(Pregunta pregunta: this.preguntas) {
                // meter registros de encuesta
                JSONObject json_pregunta = new JSONObject();
                json_pregunta.put("id_pregunta", pregunta.getId());
                json_pregunta.put("valor_respuesta", pregunta.getRespuesta());
                json_pregunta.put("fecha", fecha);
                array_resultados.put(json_pregunta);

                //comprobar si es cat
                Integer idcat = pregunta.getCAT();
                if(idcat != null) {
                    Integer value = cats.get(idcat);

                    if(value == null) {
                        // aun no hemos anyadido este idcat
                        cats.put(idcat, pregunta.getRespuesta());
                    } else {
                        // sumar valor de este idcat con lo nuevo
                        cats.put(idcat, value + pregunta.getRespuesta());
                    }
                }
            }

            JSONArray array_cat = new JSONArray();

            for(Integer key: cats.keySet()) {
                JSONObject json_cat = new JSONObject();
                json_cat.put("fecha", fecha);
                json_cat.put("id_cat", key);
                json_cat.put("suma", cats.get(key));
                array_cat.put(json_cat);
            }

            JSONObject json_encuesta = new JSONObject();
            json_encuesta.put("resultado_encuesta", array_resultados);
            FileAccess.escribirJSON(FilePath.REGISTROS_ENCUESTA, json_encuesta);

//            if(Config.getInstance().getDispositivos().get(NombresDispositivo.CAT).isEnabled()) {
//                JSONObject json_cat = new JSONObject();
//                json_cat.put("resultado_cat", array_cat);
//                EVLog.log(TAG,"CAT: " + json_cat.toString());
//                FileAccess.escribirJSON(FilePath.REGISTROS_CAT, json_cat);
//            }

        }
        catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        ir_siguiente_actividad();
    }

    public void calcularSize(LinearLayout linearLayout, int numPreguntas){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int height = displayMetrics.heightPixels / (numPreguntas + 1);
        int width = displayMetrics.widthPixels;
        int weight = height * 100 / displayMetrics.heightPixels;

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) linearLayout.getLayoutParams();

        if (linearLayout.equals(findViewById(R.id.encuestaprueba4))) {
            layoutParams.weight = weight - 6;
        }else {

            layoutParams.weight = weight + 2;
            layoutParams.height = height;
            layoutParams.width = width;
        }


        linearLayout.setLayoutParams(layoutParams);
    }

    private void ir_siguiente_actividad() {
        SecuenciaActivity.getInstance().next(this);
//        finish();
    }

    @Override
    protected void onDestroy() {
        EVLog.log(TAG,"onDestroy()");
        textToSpeech.shutdown();
        super.onDestroy();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed(){}

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume()");

        //region :: Comprobación: si la fecha que se inició el ensayo en la actual
        try {
            String contenido = FileAccess.leer(FilePath.DATE_TEST);
            String dateTestFile = util.getStringValueJSON(contenido,"datetest");
            String dateTest = FileAccess.DATE_FORMAT_TEST.format(new Date());
            EVLog.log(TAG, "FILEACCESS READ: dateTestFile: " + dateTestFile + ", dateTest: " + dateTest);

            if (dateTestFile.equals(dateTest) == false) {
                EnsayoLog.log(TAG,"", "ENSAYO ANTERIOR NO FINALIZADO");
                EVLog.log(TAG, "ENSAYO ANTERIOR NO FINALIZADO");
                startActivity(new Intent(this, Inicio.class));
                finish();
            }
        }
        catch (IOException ex) {
            EVLog.log(TAG, "FILEACCESS READ: IOException: " + ex.toString());
            ir_siguiente_actividad();
        }
        //endregion

    }
}
