package com.eviahealth.eviahealth.models.transtek.mb6.models.storage;

import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MAPSleepDate {

    private static String TAG = "MAPSleepDate";
    private Map<String, String> map = new HashMap<>();

    public MAPSleepDate() {
        map.clear();
    }

    public void add(Date fecha, Integer type){

        String key = util.DateToString(fecha,"yyyy-MM-dd HH:mm:SS");
        if (!containsKey(key)) {
            // No existe >> inserta
//            Log.e(TAG,"INSERTA key: " + key + ", type: " + type);
            String message = newRegister(key,type);
//            Log.e(TAG,"INSERTA key: " + message);
            map.put(key,message);
        }
        else {
            // Existe sumanos al actual
            String actual = map.get(key);   // Obtenemos valores actuales
//            Log.e(TAG,"ADD actual: " + actual + ", type: " + type);
            String message = addRegister(actual, type);
//            Log.e(TAG,"ADD message: " + message);
            map.remove(key);        // eliminamos key
            map.put(key,message);   // volvemos a insertarla con los datos actualizados
        }
    }

    public void clear(){
        map.clear();
    }

    // Borra el par clave/valor de la clave que se le pasa como parámetro
    public void remove(String key){ map.remove(key); }

    public int size(){ return map.size(); }

    // Devuelve true si no hay elementos en el Map y false si si los hay
    public boolean isEmpty(){ return map.isEmpty(); }

    // Devuelve true si en el map hay una clave que coincide con K
    public boolean containsKey(String key){
        return map.containsKey(key);
    }

    // Devuelve el valor de la clave que se le pasa como parámetro o 'null' si la clave no existe
    public String get(String key){ return map.get(key); }

    public String newRegister(String date, Integer type){
        String message = "{ ";

        Integer deep = 0;
        Integer ligth = 0;
        Integer awake = 0;

        if (type == 2) deep++;
        else if (type == 1) ligth++;
        else awake++;

        message += "\"date\":\"" + date + "\",";
        message += "\"deep\":" + deep + ",";
        message += "\"ligth\":" + ligth + ",";
        message += "\"awake\":" + awake + "";
        return message + " }";
    }

    public String addRegister(String actual, Integer type){
        String message = "{ ";

        String date = util.getStringValueJSON(actual,"date");
        Integer deep = util.getIntValueJSON(actual,"deep");
        Integer ligth = util.getIntValueJSON(actual,"ligth");
        Integer awake = util.getIntValueJSON(actual,"awake");

        if (type == 2) deep++;
        else if (type == 1) ligth++;
        else awake++;

        message += "\"date\":\"" + date + "\",";
        message += "\"deep\":" + deep + ",";
        message += "\"ligth\":" + ligth + ",";
        message += "\"awake\":" + awake + "";
        return message + " }";
    }

    public List<String> getListMapSleep(){
        List<String> listado = new ArrayList<>();

        TreeMap<String, String> map1 = new TreeMap<>(map);
        for (String s : map1.keySet()) {
            listado.add(map.get(s));
            EVLog.log(TAG,"MAP SLEEP: " + map.get(s));
        }
        return listado;
    }
}
