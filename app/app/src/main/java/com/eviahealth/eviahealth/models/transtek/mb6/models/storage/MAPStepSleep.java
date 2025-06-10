package com.eviahealth.eviahealth.models.transtek.mb6.models.storage;

import com.eviahealth.eviahealth.utils.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MAPStepSleep {

    private static String TAG = "MAPStepSleep";
    private Map<String, CStepSleep> map = new HashMap<>();

    public MAPStepSleep() {
        map.clear();
    }

    public void add(String fecha, CStepSleep registro){

        String key = fecha;
        if (!containsKey(key)) {
            // No existe >> inserta
//            Log.e(TAG,"INSERTA key: " + key + ", value: " + registro.toString());
            map.put(key,registro);
        }
        else {
            // Existe sumanos al actual
            CStepSleep actual = map.get(key);   // Obtenemos valores actuales
//            Log.e(TAG,"ADD actual: " + actual + ", type: " + type);
            CStepSleep message = addRegister(actual, registro);
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
    public CStepSleep get(String key){ return map.get(key); }

    public CStepSleep addRegister(CStepSleep actual, CStepSleep registro){

        CStepSleep registerAdd = new CStepSleep(actual.getDate(),
                actual.getSteps() + registro.getSteps(),
                actual.getCalories() + registro.getCalories(),
                actual.getDeep() + registro.getDeep(),
                actual.getLigth() + registro.getLigth(), actual.getAwake() + registro.getAwake()
        );

        return registerAdd;
    }

    public List<CStepSleep> getListMapStepSleep(){
        List<CStepSleep> listado = new ArrayList<>();

        TreeMap<String, CStepSleep> map1 = new TreeMap<>(map);
        for (String s : map1.keySet()) {
            listado.add(map.get(s));
//            EVLog.log(TAG,"MAP SLEEP SLEEP: " + map.get(s));
        }
        return listado;
    }

    /***
     * fracciona los registro de sleep en registros de 5 minutos con el tipo de sleep
     * @return
     */
    public List<String> fraccionar5Mint(){

        /*
        List<String> totalActivitySleep = mapSleepDate.getListMapSleep();
        message += "\"date\":\"" + date + "\",";
        message += "\"deep\":" + deep + ",";
        message += "\"ligth\":" + ligth + ",";
        message += "\"awake\":" + awake + "";
        return message + " }";
        */

        List<String> totalActivitySleep = new ArrayList<>();

        for (CStepSleep registro: getListMapStepSleep()) {
//            Log.e("", "    " + registro.toString());
            String message = "{ ";
            message += "\"date\":\"" + registro.getDate() + "\",";
            message += "\"deep\":" + registro.getDeep() + ",";
            message += "\"ligth\":" + registro.getLigth() + ",";
            message += "\"awake\":" + registro.getAwake() + "";
            message += " }";
            totalActivitySleep.add(message);
        }

        //region :: transformamos a registros de 5 minutos por fecha/hora tipo
        List<String> activity = new ArrayList<>();

        for (String mTotalActivitySleep: totalActivitySleep) {

            String date = util.getStringValueJSON(mTotalActivitySleep,"date");
            Integer deep = util.getIntValueJSON(mTotalActivitySleep,"deep");
            Integer ligth = util.getIntValueJSON(mTotalActivitySleep,"ligth");
            Integer awake = util.getIntValueJSON(mTotalActivitySleep,"awake");

            Date fecha = util.toDate(date);

            //region >> DEEP en fraciones de 5 mint
            Integer deep5 = deep / 5;
            Integer resto = deep % 5;
            if (resto > 3) deep5++;

            for (int i=0 ;i < deep5; i++) {
                date = util.DateToString(fecha,"yyyy-MM-dd HH:mm:SS");
                fecha = util.addMinutesFecha(fecha,5);

                String newvalue = "{ ";
                newvalue += "\"time\":\"" + date + "\",";
                newvalue += "\"level\":\"2\"";
                newvalue += " }";

                activity.add(newvalue);
            }
            //endregion

            //region >> LIGTH en fraciones de 5 mint
            Integer ligth5 = ligth / 5;
            resto = ligth % 5;
            if (resto > 3) ligth5++;

            for (int i=0 ;i < ligth5; i++) {
                date = util.DateToString(fecha,"yyyy-MM-dd HH:mm:SS");
                fecha = util.addMinutesFecha(fecha,5);

                String newvalue = "{ ";
                newvalue += "\"time\":\"" + date + "\",";
                newvalue += "\"level\":\"1\"";
                newvalue += " }";

                activity.add(newvalue);
            }
            //endregion

            //region >> AWAKE en fraciones de 5 mint
            Integer awake5 = awake / 5;
            resto = awake % 5;
            if (resto > 3) awake5++;

            for (int i=0 ;i < awake5; i++) {
                date = util.DateToString(fecha,"yyyy-MM-dd HH:mm:SS");
                fecha = util.addMinutesFecha(fecha,5);

                String newvalue = "{ ";
                newvalue += "\"time\":\"" + date + "\",";
                newvalue += "\"level\":\"0\"";
                newvalue += " }";

                activity.add(newvalue);
            }
            //endregion
        }
        //endregion

        return activity;
    }

    /***
     * String con el json adaptado para sueño en fracciones de 5 minutos
     * @return
     */
    public String getRecordsSleep(){

        List<String> records = fraccionar5Mint();

        String message = "{\"sleep\":[ {\"sleep_each_data\":[";
        int i = 1;
        for (String registro : records){
            // { "time":"2022-07-15 06:50:00","level":"1" }
            message += registro;
            if (i != records.size()) {
                i++;
                message += ",";
            }
        }
        message += "]} ]}";
        return message;
    }

    /***
     * Saca una lista con los registros de hora de step
     * @return
     */
    public List<String> listadoSteps() {

        List<String> totalActivity = new ArrayList<>();

        for (CStepSleep registro : getListMapStepSleep()) {
            String message = "{ ";

            message += "\"time\":\"" + registro.getDate() + "\"," +
                    "\"step\":" + registro.getSteps() + "," +
                    "\"calorie\":" + registro.getCalories() ;

            message += " }";
            totalActivity.add(message);
        }

        return totalActivity;
    }

    /***
     * String con el json adaptado para actividad por horas
     * @return
     */
    public String getRecordsSteps() {

        List<String> records = listadoSteps();

        String message = "{ \"activity\": [{ ";
        message += "\"activity_step_data\": [ ";
        int i = 1;
        for (String registro : records){
            // { "time":"2022-07-15 06:50:00","level":"1" }
            message += registro;
            if (i != records.size()) {
                i++;
                message += ",";
            }
        }
        message += "]} ]}";
        return message;
    }

    /***
     * Saca una lista con los registros de hora de step, cambio de time a fecha en el json
     * @return
     */
    public List<String> listado2Steps() {

        List<String> totalActivity = new ArrayList<>();

        for (CStepSleep registro : getListMapStepSleep()) {
            String message = "{ ";

            message += "\"fecha\":\"" + registro.getDate() + "\"," +
                    "\"step\":" + registro.getSteps() + "," +
                    "\"calorie\":" + registro.getCalories() ;

            message += " }";
            totalActivity.add(message);
        }

        return totalActivity;
    }

    /***
     * String con el json adaptado para actividad por horas
     * @return
     */
    public String getRecordsSteps(String paciente,Integer dispositivo) {

        List<String> records = listado2Steps();

        String message = "{ \"idpaciente\": \""+ paciente + "\", \"identificadorpaciente\": \"" + paciente + "\", \"dispositivo\": " + dispositivo.toString() + ", ";
        message += "\"activity\": [{ ";
        message += "\"activity_step_data\": [ ";
        int i = 1;
        for (String registro : records){
            // { "time":"2022-07-15 06:50:00","level":"1" }
            message += registro;
            if (i != records.size()) {
                i++;
                message += ",";
            }
        }
        message += "]} ]}";
        return message;
    }


    /***
     * fracciona los registro de sleep en registros de 5 minutos con el tipo de sleep
     * @return
     */
    public List<String> fraccionar5Mint2(){

        /*
        List<String> totalActivitySleep = mapSleepDate.getListMapSleep();
        message += "\"date\":\"" + date + "\",";
        message += "\"deep\":" + deep + ",";
        message += "\"ligth\":" + ligth + ",";
        message += "\"awake\":" + awake + "";
        return message + " }";
        */

        List<String> totalActivitySleep = new ArrayList<>();

        for (CStepSleep registro: getListMapStepSleep()) {
//            Log.e("", "    " + registro.toString());
            String message = "{ ";
            message += "\"date\":\"" + registro.getDate() + "\",";
            message += "\"deep\":" + registro.getDeep() + ",";
            message += "\"ligth\":" + registro.getLigth() + ",";
            message += "\"awake\":" + registro.getAwake() + "";
            message += " }";
            totalActivitySleep.add(message);
        }

        //region :: transformamos a registros de 5 minutos por fecha/hora tipo
        List<String> activity = new ArrayList<>();

        for (String mTotalActivitySleep: totalActivitySleep) {

            String date = util.getStringValueJSON(mTotalActivitySleep,"date");
            Integer deep = util.getIntValueJSON(mTotalActivitySleep,"deep");
            Integer ligth = util.getIntValueJSON(mTotalActivitySleep,"ligth");
            Integer awake = util.getIntValueJSON(mTotalActivitySleep,"awake");

            Date fecha = util.toDate(date);

            //region >> DEEP en fraciones de 5 mint
            Integer deep5 = deep / 5;
            Integer resto = deep % 5;
            if (resto > 3) deep5++;

            for (int i=0 ;i < deep5; i++) {
                date = util.DateToString(fecha,"yyyy-MM-dd HH:mm:SS");
                fecha = util.addMinutesFecha(fecha,5);

                String newvalue = "{ ";
                newvalue += "\"fecha\":\"" + date + "\",";
                newvalue += "\"nivel\":\"2\"";
                newvalue += " }";

                activity.add(newvalue);
            }
            //endregion

            //region >> LIGTH en fraciones de 5 mint
            Integer ligth5 = ligth / 5;
            resto = ligth % 5;
            if (resto > 3) ligth5++;

            for (int i=0 ;i < ligth5; i++) {
                date = util.DateToString(fecha,"yyyy-MM-dd HH:mm:SS");
                fecha = util.addMinutesFecha(fecha,5);

                String newvalue = "{ ";
                newvalue += "\"fecha\":\"" + date + "\",";
                newvalue += "\"nivel\":\"1\"";
                newvalue += " }";

                activity.add(newvalue);
            }
            //endregion

            //region >> AWAKE en fraciones de 5 mint
            Integer awake5 = awake / 5;
            resto = awake % 5;
            if (resto > 3) awake5++;

            for (int i=0 ;i < awake5; i++) {
                date = util.DateToString(fecha,"yyyy-MM-dd HH:mm:SS");
                fecha = util.addMinutesFecha(fecha,5);

                String newvalue = "{ ";
                newvalue += "\"fecha\":\"" + date + "\",";
                newvalue += "\"nivel\":\"0\"";
                newvalue += " }";

                activity.add(newvalue);
            }
            //endregion
        }
        //endregion

        return activity;
    }

    /***
     * String con el json adaptado para sueño en fracciones de 5 minutos
     * @return
     */
    public String getRecordsSleep(String paciente,Integer dispositivo){

        List<String> records = fraccionar5Mint2();

        String message = "{ \"idpaciente\": \""+ paciente + "\", \"identificadorpaciente\": \"" + paciente + "\", \"dispositivo\": " + dispositivo.toString() + ", ";
        message += "\"sleep\":[ {\"sleep_each_data\":[";
        int i = 1;
        for (String registro : records){
            // { "time":"2022-07-15 06:50:00","level":"1" }
            message += registro;
            if (i != records.size()) {
                i++;
                message += ",";
            }
        }
        message += "]} ]}";
        return message;
    }
}
