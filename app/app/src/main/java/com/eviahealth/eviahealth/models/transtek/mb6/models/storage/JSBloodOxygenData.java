package com.eviahealth.eviahealth.models.transtek.mb6.models.storage;

import com.eviahealth.eviahealth.utils.util;
import com.lifesense.plugin.ble.data.tracker.ATBloodOxygenData;
import com.lifesense.plugin.ble.data.tracker.ATBloodOxygenItem;

import java.util.ArrayList;

public class JSBloodOxygenData {

    ArrayList<ATBloodOxygenData> records;
    ArrayList<JSBloodOxygenItem> dataBloodOxygen = new ArrayList<>();

    public JSBloodOxygenData(ArrayList<ATBloodOxygenData> records){
        this.records = records;
        for (ATBloodOxygenData obj: records) {
            for (ATBloodOxygenItem m: obj.getBloodOxygens()) {
                dataBloodOxygen.add(new JSBloodOxygenItem(m.getType(), m.getBloodOxygen(), util.toStrDateTime(m.getUtc())));
            }
        }
    }

    public String toString(String paciente) {

        String json = "{ \"identificador\": " + paciente + ", \"typeMeassure\": \"ContinuousBloodOxygen\", ";
        json += "\"meassures\": [";

        int size = dataBloodOxygen.size() - 1;
        for (int i = 0;i<= size;i++) {
            json += dataBloodOxygen.get(i).toString();
            if (i != size) {
                json += ", ";
            }
        }

        json += "]} ";

        return  json;
    }
}
