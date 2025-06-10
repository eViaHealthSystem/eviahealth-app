package com.eviahealth.eviahealth.bluetooth.models;

public class BleDevices {
    private String name;
    private String address;

    public BleDevices() {
    }

    public BleDevices(String name, String address) {
        setName(name);
        setAddress(address);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

}
