package com.eviahealth.eviahealth.utils.FileAccess;


import android.content.Context;
import android.util.Log;

import com.eviahealth.eviahealth.utils.Fecha.Fecha;
import com.eviahealth.eviahealth.utils.log.EVLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class FileAccess {

    //Nota: getExternalFilesDir(null): /storage/emulated/0/Android/data/com.eviahealth.eviahealth/files
    private static Context context;
    private static File PATH;
    private static String PATH_FILES;

    /**
     * Inicializar PATH y PERMISOS
     * @param ctx
     */
    public static void init(Context ctx) {
        context = ctx.getApplicationContext(); // Almacenar contexto global

        PATH = new File(context.getExternalFilesDir(null).toString());
        PATH_FILES = context.getExternalFilesDir(null).toString();
    }

    public static File getPATH() {
        return PATH;
    }
    public static String getPATH_FILES() {
        return PATH_FILES;
    }

    // FUNCIONES ESCRITURA
    public static void escribir(FilePath path, String contenido) throws IOException {
        String ruta = path.getPath();
        File carpeta = PATH;
        if(path.getFileType() == FileType.PERIODICO) {
            ruta = Fecha.getFechaParaFile() + ruta;
            carpeta = CarpetaEnsayo.getCarpeta();
        }
        File fichero = new File(carpeta , ruta);
        Log.e("FileAccess", "escribir: " + fichero.toString());
        FileWriter myWriter = new FileWriter(fichero, false);
        myWriter.write(contenido);
        myWriter.close();
    }

    public static void escribirJSON(FilePath path, JSONObject json) throws IOException {
        escribir(path, json.toString());
    }

    public static void escribirJSONArray(FilePath path, JSONArray jsonarray) throws IOException {
        escribir(path, jsonarray.toString());
    }

    public static void appendToLogFile(File carpeta, FilePath path, String fecha_log, Object obj) throws IOException {
        if(path.getFileType() != FileType.LOG)
            throw new IOException("Debe ser un fichero FileType.LOG");
        if(fecha_log == null)
            throw new IOException("Debe establecerse una fecha antes de usarse.");

        File fichero = new File(carpeta , fecha_log + path.getPath());
        FileWriter myWriter = new FileWriter(fichero, true);
        myWriter.write(obj.toString());
        myWriter.close();
    }

    // FUNCIONES LECTURA

    public static String leer(File file) throws IOException {
        Scanner sc = new Scanner(file);
        StringBuffer contenido = new StringBuffer();
        while (sc.hasNextLine()) {
            contenido.append(sc.nextLine());
            contenido.append("\n");
        }
        sc.close();
        return contenido.toString();
    }

    private static JSONObject leerJSON(File file) throws IOException, JSONException {
        return new JSONObject(leer(file));
    }

    public static Scanner getScanner(FilePath path) throws FileNotFoundException {
        File fichero = new File(PATH , path.getPath());
        return new Scanner(fichero);
    }

    public static String leer(FilePath path) throws IOException {
        Scanner sc = getScanner(path);
        StringBuffer contenido = new StringBuffer();
        while (sc.hasNextLine())
            contenido.append(sc.nextLine());

//        Log.e("LEER STR", "PATH: " + path.name());
        Log.e("LEER STR", "CONTENIDO: " + contenido.toString());

        sc.close();
        return contenido.toString();
    }

    public static JSONObject leerJSON(FilePath path) throws IOException, JSONException {
        return new JSONObject(leer(path));
    }

    public static File[] getFileRegistros(File carpeta, final FilePath path) {
        File[] files = carpeta.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return name.endsWith(path.getPath());
            }
        });
        return files;
    }

    public static File[] getFileCarpetaEnsayo() {
        File[] dirs = PATH.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return name.endsWith(FilePath.CARPETA_ENSAYO.getPath());
            }
        });
        return dirs;
    }

    public static JSONObject[] leerFicherosRegistros(File carpeta, FilePath path) throws IOException, JSONException {
        if(path.getFileType() != FileType.PERIODICO)
            throw new IOException("Debe ser un fichero de registros");

        File[] files = getFileRegistros(carpeta, path);
        Log.e("FileAccess", "files.length: " + files.length);

        JSONObject[] array_jsons = new JSONObject[files.length];
        for(int i = 0; i < files.length; i++) {
            array_jsons[i] = leerJSON(files[i]);
        }
        return array_jsons;
    }

    public static File[] getFilesSDKLog() {
        File directorio = new File(PATH, "ihealth_sdk");
        File[] files = directorio.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
//                Log.e("getFilesSDKLog()","Checking file: " + name);
                return name.endsWith("SDK_Debug.txt");
            }
        });
        return files;
    }

    private static File[] addFileToArray(File[] originalArray, File newFile) {
        // Crear un nuevo array con un tamaño mayor
        File[] newArray = new File[originalArray.length + 1];

        // Copiar los elementos del array original al nuevo array
        System.arraycopy(originalArray, 0, newArray, 0, originalArray.length);

        // Añadir el nuevo elemento al final del nuevo array
        newArray[originalArray.length] = newFile;

        return newArray;
    }

    public static boolean checkIfFileExist(FilePath filePath, boolean location){
        File fichero;
        if (location) {
            fichero = new File(PATH , filePath.getPath());
        }else {
            fichero = new File(PATH_FILES , filePath.getPath());
        }

        return fichero.exists();
    }

    public static boolean checkFileExist(String namefile){
        File fichero;
        fichero = new File(PATH_FILES , namefile);
        boolean existe = fichero.exists();
//        Log.e("MAIN","checkFileExist(" + namefile + "): " +  Boolean.toString(existe));
        return existe;
    }

    public static FilePath[] getConfigFiles(){
        FilePath files[] = {FilePath.CONFIG_SERIAL,FilePath.CONFIG_TOKEN, FilePath.CONFIG_PACIENTE, FilePath.CONFIG_LOG, FilePath.CONFIG_ENCUESTA, FilePath.CONFIG_DISPOSITIVOS, FilePath.CONFIG_ADMIN};
        return files;
    }

    public static final SimpleDateFormat DATE_FORMAT_TEST = new SimpleDateFormat("yyyy-MM-dd");

    public static void writeDateEnsayo(FilePath path) {
        try {
            String ruta = path.getPath();
            File carpeta = PATH;
            File fichero = new File(carpeta, ruta);
            FileWriter myWriter = new FileWriter(fichero,false);
//            FileWriter myWriter = new FileWriter(fichero);

            String contenido = "{ \"datetest\": \"" + DATE_FORMAT_TEST.format(new Date()) + "\" }";
            EVLog.log("FILEACCESS WRITE", contenido);
            myWriter.write(contenido);
            myWriter.close();
        }
        catch (IOException ex) {

        }
    }

    //region :: DELETE FILEs
    public static Boolean deleteFile(FilePath path){
        String ruta = path.getPath();
        File carpeta = PATH;
        File f = new File(carpeta , ruta);

        if (f.isFile()){ return f.delete(); }

        return false;
    }
    public static boolean deleteFile(File file) {
        try {
            if (file == null) {
                Log.e("FileAccess","Error: El archivo es nulo.");
                return false;
            }
            if (!file.exists()) {
                Log.e("FileAccess","Error: El archivo no existe.");
                return false;
            }
            if (file.isDirectory()) {
                Log.e("FileAccess","Error: No se puede borrar un directorio.");
                return false;
            }
            return file.delete();
        } catch (SecurityException e) {
            Log.e("FileAccess","Error: Permisos insuficientes para borrar el archivo. " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e("FileAccess","Error inesperado al borrar el archivo: " + e.getMessage());
            return false;
        }
    }
    public static boolean deleteFile(File[] files) {
        if (files == null || files.length == 0) {
            Log.e("FileAccess","Error: La lista de archivos es nula o vacía.");
            return false;
        }

        boolean allDeleted = true;

        for (File file : files) {
            if (file == null) {
                Log.e("FileAccess","Error: Uno de los archivos en la lista es nulo.");
                allDeleted = false;
                continue;
            }
            if (!file.exists()) {
                Log.e("FileAccess","Error: El archivo '" + file.getAbsolutePath() + "' no existe.");
                allDeleted = false;
                continue;
            }
            if (file.isDirectory()) {
                Log.e("FileAccess","Error: '" + file.getAbsolutePath() + "' es un directorio, no un archivo.");
                allDeleted = false;
                continue;
            }
            try {
                if (!file.delete()) {
                    Log.e("FileAccess","Error: No se pudo eliminar el archivo '" + file.getAbsolutePath() + "'.");
                    allDeleted = false;
                }
            } catch (SecurityException e) {
                Log.e("FileAccess","Error: Permisos insuficientes para borrar '" + file.getAbsolutePath() + "'. " + e.getMessage());
                allDeleted = false;
            }
        }

        return allDeleted;
    }

    public static boolean deleteFile(File carpeta, final FilePath path) {
        File[] files = getFileRegistros(carpeta, path);
        return deleteFile(files);
    }

    //endregion
}
