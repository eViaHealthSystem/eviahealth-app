package com.eviahealth.eviahealth.models.transtek.mb6.models.storage;

public class JSBloodOxygenItem {
    private int type;
    private int bloodOxygen ;
    private String meassureTime;

    public JSBloodOxygenItem (int type, int bloodOxygen , String meassureTime) {
        this.type = type;
        this.bloodOxygen  = bloodOxygen ;
        this.meassureTime = meassureTime;
    }

    public int getType() {
        return type;
    }

    public int getBloodOxygen () {
        return bloodOxygen ;
    }

    public String getMeassureTime() {
        return meassureTime;
    }

    @Override
    public String toString() {
        String jsonItem = "{ ";
        jsonItem += "\"time\":\"" + this.meassureTime + "\"," +
                "\"bloodOxygen\":" + this.bloodOxygen + "," +
                "\"type\":" + this.type;
        jsonItem += " }";
        return jsonItem;
    }

}
