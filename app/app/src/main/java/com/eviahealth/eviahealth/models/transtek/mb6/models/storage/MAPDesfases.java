package com.eviahealth.eviahealth.models.transtek.mb6.models.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MAPDesfases {
    private static String TAG = "MAPStepSleep";
    private Map<String, Integer> map = new HashMap<>();

    public MAPDesfases() {
        map.clear();
    }

    public void add(String fecha, Integer desfase){

        String key = fecha;
        if (!containsKey(key)) {
            // No existe >> inserta
            map.put(key,desfase);
        }
//        else {
//            // Existe sumanos al actual
//            Log.e("", "Desface en esta fecha ya existe");
//        }
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
    public Integer get(String key){ return map.get(key); }

    public List<String> getListMapDesfases(){
        List<String> listado = new ArrayList<>();

        TreeMap<String, Integer> map1 = new TreeMap<>(map);
        for (String s : map1.keySet()) {
            listado.add("" + s + ";" + map.get(s));
//            EVLog.log(TAG,"MAP SLEEP SLEEP: " + map.get(s));
        }
        return listado;
    }
}
