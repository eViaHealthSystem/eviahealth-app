package com.eviahealth.eviahealth.models.transtek.mb6.models.storage;

public class JSHeartRateItem {

    private int type;
    private int heartRates;
    private String meassureTime;

    public JSHeartRateItem (int type, int heartRates, String meassureTime) {
        this.type = type;
        this.heartRates = heartRates;
        this.meassureTime = meassureTime;
    }

    public int getType() {
        return type;
    }

    public int getHeartRates() {
        return heartRates;
    }

    public String getMeassureTime() {
        return meassureTime;
    }

    @Override
    public String toString() {
        String jsonItem = "{ ";
        jsonItem += "\"time\":\"" + this.meassureTime + "\"," +
                "\"heartRates\":" + this.heartRates + "," +
                "\"type\":" + this.type;
        jsonItem += " }";
        return jsonItem;
    }

}
