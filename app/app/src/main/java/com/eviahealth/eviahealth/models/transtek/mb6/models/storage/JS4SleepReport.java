package com.eviahealth.eviahealth.models.transtek.mb6.models.storage;

import android.util.Log;

import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.util;
import com.lifesense.plugin.ble.data.tracker.ATSleepReportData;
import com.lifesense.plugin.ble.data.tracker.ATSleepReportItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JS4SleepReport {

    final String TAG = "JS4SleepReport";

    private ArrayList<ATSleepReportData> recodsTotal;
    private ArrayList<ATSleepReportItem> itemSleep = new ArrayList<>();
    public MAPSleepDate mapSleepDate = new MAPSleepDate();
    private ArrayList<ATSleepReportData> records = new ArrayList<>();

    private ArrayList<CStepSleep> listSleep = new ArrayList<>();
    MAPDesfases desfases = new MAPDesfases();
    private List<String> tramos = new ArrayList<>();

    /***
     *
     * @param recodsTotal
     * @param desfases: Mapa de desfases generado en Step
     */
    public JS4SleepReport(ArrayList<ATSleepReportData>  recodsTotal, MAPDesfases desfases){

        this.recodsTotal = recodsTotal;
        this.desfases = desfases;

        //region :: Tramos sdk
        ArrayList<String> tramos = new ArrayList<>();
        int p = 0;
        for(ATSleepReportData obj: recodsTotal) {

            Integer totalNumberOfSleep = obj.getTotalNumberOfSleepItem();
            String t = "[" + util.toStrDateTime(obj.getBedTime()) + ", " + util.toStrDateTime(obj.getGetupTime()) + "], sleepType: " + obj.getSleepType() + ", TotalNumberOfSleep: " + obj.getTotalNumberOfSleepItem();
            p += obj.getCountOfSleepItem();
            if (tramos.contains(t) == false) {
                tramos.add(t);
                EVLog.log(TAG,"" + t);
                p = 0;
            }
            else {
                if (p == totalNumberOfSleep) {
                    tramos.add(t);
                    EVLog.log(TAG,"" + t);
                    p = 0;
                }
            }
        }
        // endregion

        //region :: Junta todos los registros de su trama en un único ATSleepReportData y genera una nueva lista "records"
        for(ATSleepReportData obj: recodsTotal) {
            // Miramos si el registro de sueño es definitivo o no
            if (obj.getSleepType() == 2) {
                // El tramo de sueño es completo y está finalizado
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
                    } else {
                        state -= 1;
                        if (state > 1) {
                            state = 2; // deep and rem >> deep
                        }
                    }

                    it.setState(state);
                    //endregion

                    String item = "" + util.toStrDateTime(it.getStartTime()) + ", " + util.toStrDateTime(it.getEndTime());
                    itemSleep.add(it);
                }
            }
        }
        //endregion

    }

    public int size() {
        return itemSleep.size();
    }

    //NEW FUNCTIONS
    public ArrayList<CStepSleep> getListSleepHour() {

        Log.e(TAG,"Acumula registros por fecha y hora el total de la actividad de sueño");
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
//            Log.e(TAG,"in white: startDate: " + util.DateToString(startDate,"yyyy-MM-dd HH:mm:SS") + ", endDate: " + util.DateToString(endDate,"yyyy-MM-dd HH:mm:SS"));

            // si endData >= startDate
            if (util.isEndDateAfterOrEqualStartDate(startDate,endDate)) {
                while (!startDate.equals(endDate)) {

                    String tmp = util.DateToString(startDate, "yyyy-MM-dd HH:mm:SS");
                    Date key = util.convertDateHour(tmp);                // Quita Minutos y segundos
                    mapSleepDate.add(key, type);

                    // Añadimos 1 minuto al registro de tiempo
                    startDate = util.addMinutesFecha(startDate, 1);
                }
            }
            else {
                // si endData < startDate, descartamos el registro
                EVLog.log(TAG, "ERROR: endData < startDate");
                EVLog.log(TAG,"startDate: " + util.DateToString(startDate,"yyyy-MM-dd HH:mm:SS") + ", endDate: " + util.DateToString(endDate,"yyyy-MM-dd HH:mm:SS"));
            }
//            Log.e(TAG,"out white");
        }
        //endregion

        listSleep.clear();
        List<String> totalActivitySleep = mapSleepDate.getListMapSleep();

        Log.e(TAG,"for totalActivitySleep.size()");
        for (int i=0;i < totalActivitySleep.size(); i++) {

            String actual = totalActivitySleep.get(i);
            String date = util.getStringValueJSON(actual, "date");
            Integer deep = util.getIntValueJSON(actual, "deep");
            Integer ligth = util.getIntValueJSON(actual, "ligth");
            Integer awake = util.getIntValueJSON(actual, "awake");

            Integer desfase = desfases.get(date);
//            Log.e("", "date: " + date + ", desfase: " + desfase);

            // registro de sueño fuera del mapa de desfase de steps desfase == null
            if (desfase != null ) {
                Log.e("", "********************************* sleep date: " + date + ", desfase: " + desfase);
                if (desfase != 0) {
                    date = addHourFecha(date, desfase);
                }
            }
            else {
                // desfase == null
                // le sumamos [GTM(CHINA) +8] - GMt(MADRID) [1 o 2] de desfase, debería existir desfase en paso pero si no existe es un desfase del dia anterior
                Log.e("", "????????????????????????????????? sleep date: " + date + ", desfase: " + desfase);
                //                date = addHourFecha(date, 8 - getGMTMadrid(date));
            }

            listSleep.add(new CStepSleep(date,deep,ligth,awake));
        }

        return listSleep;
    }

    private String addHourFecha(String fecha, int hour) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDateTime fechaActual = LocalDateTime.parse(fecha, formatter);
        fechaActual = fechaActual.plusHours(hour); // Añadir x horas a la fecha actual

        return fechaActual.format(formatter); // Convertir fechaActual a cadena con formato
    }

    public List<String> getListTramos() { return tramos; }

}

