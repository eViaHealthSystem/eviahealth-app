package com.eviahealth.eviahealth.models.transtek.mb6.models.storage;

import com.eviahealth.eviahealth.utils.util;
import com.lifesense.plugin.ble.data.tracker.ATHeartRateData;

import java.util.ArrayList;
import java.util.List;

public class JSHeartRateData {

    private ArrayList<ATHeartRateData> records;
    private ArrayList<JSHeartRateItem> heartRates = new ArrayList<>();

    public JSHeartRateData(ArrayList<ATHeartRateData> records){
        this.records = records;

        // recoremos todos los registros para obtener las medidas individuales
        for(ATHeartRateData obj: records){

            long utc = obj.getUtc();            // fecha inicio capturas
            int offset = obj.getOffset();       // desplazacmiento entre fechas
            int dataSize = obj.getDataSize();   // nº de registros dentro del arraylist
            List<Integer> listHeartRates = obj.getHeartRates();

            for (int i=0; i<=dataSize-1; i++){
                long utcItem = utc + offset*i;
                JSHeartRateItem item = new JSHeartRateItem(obj.getType(),listHeartRates.get(i), util.toStrDateTime(utcItem));
                heartRates.add(item);
            }

        }
    }

    public String toString(String paciente) {


        String json = "{ \"identificador\": " + paciente + ", \"typeMeassure\": \"ContinuousHeartRates\", ";
        json += "\"meassures\": [";

        int size = heartRates.size() - 1;
        for (int i = 0;i<= size;i++) {
            json += heartRates.get(i).toString();
            if (i != size) {
                json += ", ";
            }
        }

        json += "]} ";

        return  json;
    }
}
