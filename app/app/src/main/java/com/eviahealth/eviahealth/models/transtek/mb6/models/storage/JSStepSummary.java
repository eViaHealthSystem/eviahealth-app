package com.eviahealth.eviahealth.models.transtek.mb6.models.storage;

import com.eviahealth.eviahealth.utils.util;
import com.lifesense.plugin.ble.data.tracker.ATStepItem;
import com.lifesense.plugin.ble.data.tracker.ATStepSummary;

import java.util.ArrayList;

public class JSStepSummary {

    private ArrayList<ATStepSummary> stepSummary;
    private ArrayList<ATStepItem> itemStep = new ArrayList<>();

    public JSStepSummary(ArrayList<ATStepSummary>  stepSummary){

        this.stepSummary = stepSummary;

        for(ATStepSummary obj: stepSummary){
            ArrayList<ATStepItem> steps = new ArrayList<>(obj.getSteps());
            for (ATStepItem item: steps){
                itemStep.add(item);
            }
        }
    }

    /**
     * Pone los segundos = 0 en todas los registros
     * "2023-01-20 23:59:59" >> "2023-01-20 23:59:00"
     */
    public void adapterDateHours() {
        for (int i=0;i<=itemStep.size()-1;i++) {
            String f = itemStep.get(i).getMeasureTime();
            f = util.clearSecond(f);
            itemStep.get(i).setMeasureTime(f);
        }
    }

    public int size() {
        return itemStep.size();
    }

    public ATStepItem getId(int id){
        return itemStep.get(id);
    }

    public String getStepToday() {
        String today = "{}";
//        String hoy = "2023-01-23";
        String hoy = util.getToday();
        for (ATStepItem item: itemStep) {
            String measureTime = util.getDay(item.getMeasureTime());
            if (measureTime.equals(hoy)) {

                today = "{ ";
                today += "\"time\":\"" + item.getMeasureTime() + "\"," +
                        "\"steps\":" + item.getStep() + "," +
                        "\"calories\":" + item.getCalories();
                today += " }";
                break;
            }
        }
        return today;
    }


    public String getJsonItem(ATStepItem item) {
        String jsonItem = "{ ";

        jsonItem += "\"time\":\"" + item.getMeasureTime() + "\"," +
                "\"step\":" + item.getStep() + "," +
                "\"calorie\":" + item.getCalories();

        jsonItem += " }";

        return jsonItem;
    }

    @Override
    public String toString() {

        String json = "{ \"activity\": [{ ";
        json += "\"activity_step_data\": [ ";

        int size = itemStep.size() - 1;
        for (int i = 0;i<= size;i++) {
            json += getJsonItem(itemStep.get(i));
            if (i != size) {
                json += ", ";
            }
        }
        json += "]} ";
        json += "]}";

        return  json;
    }
}
