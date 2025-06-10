package com.eviahealth.eviahealth.utils.FileAccess;

import com.eviahealth.eviahealth.models.devices.Config;
import com.eviahealth.eviahealth.utils.Fecha.Fecha;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class CarpetaEnsayo {

    private static File CARPETA;
    private static String FECHA = null;

    // generar una nueva carpeta de ensayo al empezar un ensayo
    public static void generarCarpetaEnsayo() {
        String fecha = Fecha.getFechaParaFile();
        FECHA = fecha;
        CARPETA = new File(FileAccess.getPATH(), fecha + FilePath.CARPETA_ENSAYO.getPath());
        CARPETA.mkdirs();

        try {
            JSONObject json = new JSONObject();
            json.put("fecha", Fecha.getFechaYHoraActual());
            // añadimos el id del paciente en el fichero del ensayo para que cuando se envie el ensayo se envíe
            // al paciente que se lo hizo
            json.put("idpaciente", Config.getInstance().getIdPacienteEnsayo());
            FileAccess.escribirJSON(FilePath.REGISTRO_ENSAYO, json);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

    }

    public String getFecha() { return this.FECHA;}

    public static File getCarpeta() {
        return CARPETA;
    }
}
