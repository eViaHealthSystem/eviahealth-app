package com.eviahealth.eviahealth.utils.FileAccess;

import android.util.Log;

import com.eviahealth.eviahealth.utils.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileFuntions {

    private FileFuntions() {}

    public static String readfile(String namefile){

        StringBuilder stringBuilder = new StringBuilder();
        String line;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(new File(FileAccess.getPATH_FILES(), namefile)));
            while ((line = in.readLine()) != null) stringBuilder.append(line);
        }
        catch (FileNotFoundException e) {
            Log.e("EXCEPCION","Exception readfile(" + namefile + "):" + e);
            return "";
        }
        catch (IOException e) {
            Log.e("EXCEPCION","Exception readfile():" + e);
            return "";
        }
        return stringBuilder.toString();
    }

    public static Boolean writefile(String namefile, String contenido){

        Log.e("FileFuntions","*************** writefile(" + namefile + "): " + contenido);

        try {
            File fichero = new File(FileAccess.getPATH_FILES(), namefile);
            FileWriter myWriter = new FileWriter(fichero);
            myWriter.write(contenido);
            myWriter.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
            Log.e("EXCEPCION","Exception writefile():" + e);
            return false;
        }
        return true;
    }

    public static boolean checkFileExist(String namefile){
        File fichero;
        fichero = new File(FileAccess.getPATH_FILES() , namefile);
        boolean existe = fichero.exists();
        Log.e("MAIN", "checkFileExist(" + namefile + "): " + Boolean.toString(existe));
        return existe;
    }

    public static String getSleepDesfaces() {
        if (FileFuntions.checkFileExist(FilePath.SLEEP_DESFASE.getNameFile())) {
            String content = util.readFile(new File(FileAccess.getPATH_FILES(), FilePath.SLEEP_DESFASE.getNameFile()));
            if (content != null) {
                Log.e("FileFuntions", ".................................................");
                Log.e("FileFuntions", "Read SleepDesfaces(): " + content);
                Log.e("FileFuntions", ".................................................");
            } else {
                Log.e("FileFuntions", "Read SleepDesfaces(): null");
            }
            return content;
        } else {
            Log.e("FileFuntions", "Read SleepDesfaces(): file not found");
            return null;
        }
    }

    public static void setSleepDesfaces(String content) {
        if (content == null) {
           FileAccess.deleteFile(FilePath.SLEEP_DESFASE);
        }
        else { FileFuntions.writefile(FilePath.SLEEP_DESFASE.getNameFile(), content); }
    }
}
