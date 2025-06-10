package com.eviahealth.eviahealth.models.transtek.mb6.models.storage;

import android.util.Log;

import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.util;
import com.lifesense.plugin.ble.data.tracker.ATStepItem;
import com.lifesense.plugin.ble.data.tracker.ATStepSummary;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;									 
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class JS2StepSummary {
    final String TAG = "JS2StepSummary";
    private ArrayList<ATStepSummary> stepSummary;
    private ArrayList<ATStepItem> itemStep = new ArrayList<>();

    private ArrayList<CStepSleep> tmpSteps = new ArrayList<>();
    private ArrayList<CStepSleep> listSteps = new ArrayList<>();
    private MAPDesfases desfases = new MAPDesfases();

    // Constructor
    public JS2StepSummary(ArrayList<ATStepSummary>  stepSummary){

        this.stepSummary = stepSummary;

        EVLog.log(TAG,"DOWNLOAD SDK INFO STEPS");
        int id = 0;
        for(ATStepSummary obj: stepSummary) {
//            Log.e("STEPS",""+ obj.toString());
            ArrayList<ATStepItem> steps = new ArrayList<>(obj.getSteps());
            for (ATStepItem item : steps) {
                itemStep.add(item);
                EVLog.log(TAG,"ATStepItem \""+ util.toStrDateTime(item.getUtc()) + ", steps: " + item.getStep());
                id += 1;			
            }
        }

        EVLog.log(TAG,"itemStep.size(): " + itemStep.size());

        //region :: Poner minutos y segundos >> 00
        for (int i=0;i<=itemStep.size()-1;i++) {
            String f = itemStep.get(i).getMeasureTime();
            f = clearDateHour(f);
            itemStep.get(i).setMeasureTime(f);
        }
        //endregion

        //region :: Rellena los huecos de horas por dejar en carga
        EVLog.log(TAG,"Relleno y desfase ----------------------------------");
        tmpSteps.clear();
        if (itemStep.size() > 0) {

            tmpSteps.add(new CStepSleep(itemStep.get(0).getMeasureTime(),itemStep.get(0).getStep(),(int)itemStep.get(0).getCalories()));

            if (itemStep.size() > 1) {
                for (int i = 1; i <= itemStep.size() - 2; i++) {
                    String datePrevious = itemStep.get(i - 1).getMeasureTime();
                    String dateNow = itemStep.get(i).getMeasureTime();

                    long des = obtenerDiferenciaEnHoras(datePrevious, dateNow);
//                Log.e("", "datePrevious: " + datePrevious + ", dateNow: " + dateNow);
//                Log.e("", "i: " + i + "\t" + des);

                    if (des == 1) {
                        tmpSteps.add(new CStepSleep(itemStep.get(i).getMeasureTime(), itemStep.get(i).getStep(), (int) itemStep.get(i).getCalories()));
                    } else if (des < 1) {
                        EVLog.log(TAG, "i: " + i + "\t " + datePrevious + " - " + dateNow + "\t" + des);
                        tmpSteps.add(new CStepSleep(itemStep.get(i).getMeasureTime(), itemStep.get(i).getStep(), (int) itemStep.get(i).getCalories()));
                    } else {
                        // des > 1 RELLENAR
                        String dateTocaria = addHourDate(itemStep.get(0).getMeasureTime(), i);
                        if (dateNow.equals(dateTocaria) == false) {

                            EVLog.log(TAG, "i: " + i + "\t " + datePrevious + " - " + dateNow + "\t" + des);
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                            LocalDateTime fechaA = LocalDateTime.parse(datePrevious, formatter);
                            LocalDateTime fechaB = LocalDateTime.parse(dateNow, formatter);

                            LocalDateTime fechaActual = fechaA;
                            fechaActual = fechaActual.plusHours(1); // Añadir 1 hora a la fecha actual

                            while (fechaActual.isBefore(fechaB)) {

                                String fechaActualString = fechaActual.format(formatter); // Convertir fechaActual a cadena con formato
                                EVLog.log(TAG, "Rellena: " + fechaActualString); // Imprimir la fecha actual

                                tmpSteps.add(new CStepSleep(fechaActualString, 0, 0));
                                fechaActual = fechaActual.plusHours(1); // Añadir 1 hora a la fecha actual
                            }
                        } else {
                            EVLog.log(TAG, "i: " + i + "\t >>> " + dateTocaria + " = " + dateNow + "\t" + des);
                        }
                        tmpSteps.add(new CStepSleep(itemStep.get(i).getMeasureTime(), itemStep.get(i).getStep(), (int) itemStep.get(i).getCalories()));
                    }
                }

                tmpSteps.add(new CStepSleep(itemStep.get(itemStep.size() - 1).getMeasureTime(), itemStep.get(itemStep.size() - 1).getStep(), (int) itemStep.get(itemStep.size() - 1).getCalories()));
            }

//            for (CStepSleep item : tmpSteps) {
//                Log.e(""," **** \""+ item.getDate() + ", steps: " + item.getSteps());
//            }

        }
        EVLog.log(TAG,"----------------------------------------------------");
        //endregion

    }

    public int size() {
        return itemStep.size();
    }

    public ArrayList<CStepSleep> getListSteps() {

        listSteps.clear();
        long desfase = 1;

        for (int i=0;i<=tmpSteps.size()-1;i++) {

            String date = tmpSteps.get(i).getDate();
            Integer pasos = tmpSteps.get(i).getSteps();
            Integer calories = (int) tmpSteps.get(i).getCalories();

//            Log.e("", "i: " +i);
            if (i == 0 || i == tmpSteps.size()-1) {
                desfases.add(date,0);
            }

			if (i > 0 && i< tmpSteps.size()-1) {

                String dateBase = tmpSteps.get(0).getDate();
//                String dateTocaria = addHourFecha(dateBase, i);
                String dateTocaria = addHourDate(dateBase,i);

                Log.e("","dateTocaria: " + dateTocaria + ", date: " + date);
                long des = obtenerDiferenciaEnHoras2(dateTocaria,date);
                desfase = (-1) * des;
//                Log.e("", "" + dateTocaria + "-" + date + " : " + desfase);

                if (desfase != 0) {
//                    date = addHourFecha(date, (int) desfase);
                    date = addHourDate(date, (int) desfase);
                    desfases.add(date, (int) desfase);
                }
                else{
                    desfases.add(date, 0);
                }

            }

            listSteps.add(new CStepSleep(date,pasos,calories));
        }

        return listSteps;
    }

    /***
     * Pone minutos y segundos a 0
     * @param date
     * @return
     */
    private String clearDateHour(String date) {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH");
        try {
            Date ndate = format.parse(date);
            String tmp = "" + format2.format(ndate) + ":00:00";
            ndate = format.parse(tmp);
            return "" + format.format(ndate);

        } catch (ParseException e) {
            e.printStackTrace();
            Log.e("UTIL", "convertDateHourMinute >> null");
            return null;
        }
    }

    private long obtenerDiferenciaEnHoras2(String fecha1, String fecha2) {
        try {
            SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date1 = formato.parse(fecha1);
            Date date2 = formato.parse(fecha2);

            long diferenciaMillis = date2.getTime() - date1.getTime();
            long diferenciaHoras = TimeUnit.MILLISECONDS.toHours(diferenciaMillis);

            return diferenciaHoras;
        }
        catch (ParseException e) {
            Log.e("obtenerDiferenciaEnHoras", "ParseException: " + e);
            System.out.println("Error al parsear las fechas: " + e.getMessage());
            return 0;
        }
    }

    private long obtenerDiferenciaEnHoras(String fecha1, String fecha2) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            LocalDateTime fechaA = LocalDateTime.parse(fecha1, formatter);
            LocalDateTime fechaB = LocalDateTime.parse(fecha2, formatter);

            return ChronoUnit.HOURS.between(fechaA, fechaB);
        }
        catch (Exception e) {
            Log.e("obtenerDiferenciaEnHoras", "Exception: " + e);
            System.out.println("Error al parsear las fechas: " + e.getMessage());
            return 0;
        }
    }

    private String addHourDate(String fecha, int hour) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDateTime fechaActual = LocalDateTime.parse(fecha, formatter);
        fechaActual = fechaActual.plusHours(hour); // Añadir x horas a la fecha actual

        return fechaActual.format(formatter); // Convertir fechaActual a cadena con formato
    }

    public MAPDesfases getDesfases() { return desfases; }

}
