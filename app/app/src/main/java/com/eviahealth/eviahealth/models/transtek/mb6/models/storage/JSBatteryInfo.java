package com.eviahealth.eviahealth.models.transtek.mb6.models.storage;

import com.lifesense.plugin.ble.data.tracker.ATBatteryInfo;

public class JSBatteryInfo {

    private ATBatteryInfo batteryInfo;
    private int battery;

    public JSBatteryInfo(ATBatteryInfo batteryInfo){
        this.batteryInfo = batteryInfo;
        this.battery = batteryInfo.getBattery();
    }

    public int getBattery() {
        return this.battery;
    }
    public ATBatteryInfo getBatteryInfo() { return this.batteryInfo; }

    @Override
    public String toString() {
        return "{ \"battery\":" + this.battery + " }";
    }

}
