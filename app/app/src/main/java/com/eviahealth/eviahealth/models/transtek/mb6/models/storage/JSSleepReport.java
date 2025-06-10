package com.eviahealth.eviahealth.models.transtek.mb6.models.storage;

import com.eviahealth.eviahealth.utils.util;
import com.lifesense.plugin.ble.data.tracker.ATSleepReportData;
import com.lifesense.plugin.ble.data.tracker.ATSleepReportItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JSSleepReport {
    private ArrayList<ATSleepReportData> recodsTotal;
    private ArrayList<ATSleepReportItem> itemSleep = new ArrayList<>();
    public MAPSleepDate mapSleepDate = new MAPSleepDate();
    private ArrayList<ATSleepReportData> records = new ArrayList<>();

    public JSSleepReport(ArrayList<ATSleepReportData>  recodsTotal){

        this.recodsTotal = recodsTotal;

        int acumulados = 0;
        List<ATSleepReportItem> items = new ArrayList<>();

        //region :: Junta todos los registros de su trama en un único ATSleepReportData y genera una nueva lista "records"
        for(ATSleepReportData obj: recodsTotal) {
            int totalNUmberOfSleepItem = obj.getTotalNumberOfSleepItem();
            ArrayList<ATSleepReportItem> reportItems = new ArrayList<>(obj.getReportItems());

            for (ATSleepReportItem it : reportItems) {

                //region :: Adaptación valores estado sueño a los de la AM4
                /* State MAMBO 6
                    0x01 awake
                    0x02 light
                    0x03 deep
                    0x04 rem
                */
                /* AM4
                    0x00 awake
                    0x01 light
                    0x02 deep
                */

                // Adaptamos a 3 únicos estados de sueño como la AM4
                int state = it.getState();
                if (state <= 1) {
                    state = 0; // awake >> AM4
                }
                else{
                    state -= 1;
                    if (state > 1){
                        state = 2; // deep and rem >> deep
                    }
                }

                it.setState(state);
                //endregion

                items.add(it);
                acumulados += 1;
            }

            if (totalNUmberOfSleepItem == acumulados) {
                obj.setReportItems(new ArrayList<>(items));
                records.add(obj);
                acumulados = 0;
                items.clear();
            }

        }
        //endregion

        // tramos duplicados
        List<List<ATSleepReportData>> m = agrupaDuplicados(records);

        records.clear();
        records = agrupaMayorTiempo(m);

        //region :: Genera una Lista con todos los ATSleepReportItem de todos los tramos
        for(ATSleepReportData obj: records){
            ArrayList<ATSleepReportItem> sleep = new ArrayList<>(obj.getReportItems());
            for (ATSleepReportItem item: sleep){
                itemSleep.add(item);
            }
        }
        //endregion

//        oldData();
    }

    private void oldData() {
        for(ATSleepReportData obj: recodsTotal){
            ArrayList<ATSleepReportItem> sleep = new ArrayList<>(obj.getReportItems());
            for (ATSleepReportItem item: sleep){

                //region :: Adaptación valores estado sueño a los de la AM4
                /* State MAMBO 6
                    0x01 awake
                    0x02 light
                    0x03 deep
                    0x04 rem
                */
                /* AM4
                    0x00 awake
                    0x01 light
                    0x02 deep
                */

                // Adaptamos a 3 únicos estados de sueño como la AM4
                int state = item.getState();
                if (state <= 1) {
                    state = 0; // awake >> AM4
                }
                else{
                    state -= 1;
                    if (state > 1){
                        state = 2; // deep and rem >> deep
                    }
                }

                item.setState(state);
                //endregion

                itemSleep.add(item);
            }
        }
    }

    public int size() {
        return itemSleep.size();
    }

    public ATSleepReportItem getId(int id){
        return itemSleep.get(id);
    }

    public String getJsonItem(ATSleepReportItem item) {
        String jsonItem = "{ ";

        jsonItem += "\"startTime\":\"" + util.toStrDateTime(item.getStartTime()) + "\"," +
                "\"endTime\":\"" + util.toStrDateTime(item.getEndTime()) + "\"," +
                "\"type\":" + item.getState();

        jsonItem += " }";

        return jsonItem;
    }

    @Override
    public String toString() {

        String json = "{ \"sleep\": [{ ";
        json += "\"sleep_each_data\": [ ";

        for (ATSleepReportItem item: itemSleep) {
            json += getJsonItem(item) + ", ";
        }
        json += "]} ";
        json += "]}";

        return  json;
    }

    public List<String> fraccionar5MintSleep(){

        //region :: Acumula registros por fecha y hora el total de la actividad de sueño { "date":"2022-07-07 16:00:00","deep":2,"ligth":14,"awake":31 }
        for (ATSleepReportItem item: itemSleep){
            //{ "startDate":"2022-07-07 15:43:00","endDate":"2022-07-07 16:11:00","type":1 }

            String startTime = util.toStrDateTime(item.getStartTime());   // UTC LONG >> STRING FECHA
            String endTime = util.toStrDateTime(item.getEndTime());       // UTC LONG >> STRING FECHA

            startTime = util.clearSecond(startTime);   // Quita segundos >> "startDate":"2022-07-07 23:52:00"
            endTime = util.clearSecond(endTime);       // Quita segundos >> "endDate":"2022-07-08 00:07:00"

            Integer type = item.getState();

            Date startDate = util.toDate(startTime);    // Quita segundos >> "startDate":"2022-07-07 23:52:00"
            Date endDate = util.toDate(endTime);        // Quita segundos >> "endDate":"2022-07-08 00:07:00"

//            Log.e("itemSleep: ","startTime: " + startTime +", endTime: " + endTime + ", tipo: " + item.getState());

            while (!startDate.equals(endDate)) {

                String tmp = util.DateToString(startDate,"yyyy-MM-dd HH:mm:SS");
                Date key = util.convertDateHour(tmp);                // Quita Minutos y segundos
                mapSleepDate.add(key,type);

                // Añadimos 1 minuto al registro de tiempo
                startDate = util.addMinutesFecha(startDate,1);
            }

        }

        List<String> totalActivitySleep = mapSleepDate.getListMapSleep();
        //endregion

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

    public String getRecordsSleep(){

        List<String> records = fraccionar5MintSleep();

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
//        EVLog.log(TAG,"MESSAGE: " + message);
        return message;
    }

    //region :: RECOPILA EN LISTA DE DATOS DEL MISMO INICIO (bedTime)
    private List<List<ATSleepReportData>> agrupaDuplicados(List<ATSleepReportData> lista) {
        List<List<ATSleepReportData>> m = new ArrayList<>();
        ArrayList<Integer> tratados = new ArrayList<Integer>();

        for (int i = 0; i <= lista.size() - 1; i++) {

            List<ATSleepReportData> l = new ArrayList<>();
            if (tratados.contains(i) == false) {
                for (int j = i + 1; j <= lista.size() - 1; j++) {
                    if (lista.get(i).getBedTime() == lista.get(j).getBedTime()) {
                        if (l.contains(lista.get(i)) == false) {
                            l.add(lista.get(i));
                            tratados.add(i);
                        }
                        if (l.contains(lista.get(j)) == false) {
                            l.add(lista.get(j));
                            tratados.add(j);
                        }
                    }
                }
            }

            if (l.size() > 0) {
                m.add(l);
            } else {
                if (tratados.contains(i) == false) {
                    l.add(lista.get(i));
                    m.add(l);
                }
            }

        }

//        Log.e("agrupaDuplicados", "----------LISTAS----------");
//        for (List<ATSleepReportData> x : m) {
//            for (ATSleepReportData y : x) {
//                Log.e("agrupaDuplicados", "A: " + y.getBedTime() + ", B: " + y.getGetupTime());
//            }
//            Log.e("agrupaDuplicados", "___");
//        }
        //endregion

        return m;
    }

    //region :: RECOPILA EN UNA List<ATSleepReportData> LOS REGISTROS DE MAYOR TIEMPO
    private ArrayList<ATSleepReportData> agrupaMayorTiempo(List<List<ATSleepReportData>> m) {
        ArrayList<ATSleepReportData> l = new ArrayList<>();
        for (List<ATSleepReportData> x: m) {
            if (x.size() > 0) {
                if (x.size() == 1) {
                    l.add(x.get(0));
                } else {
                    l.add(mayorItems(x));
                }
            }
        }
        return l;
    }

    private ATSleepReportData mayorItems(List<ATSleepReportData> lista) {
        ATSleepReportData reg = lista.get(0);

        for (int i = 1; i <= lista.size()-1; i++) {

            if (lista.get(i).getGetupTime() > reg.getGetupTime()) {
                reg = lista.get(i);
            }
        }
        return reg;
    }
}
