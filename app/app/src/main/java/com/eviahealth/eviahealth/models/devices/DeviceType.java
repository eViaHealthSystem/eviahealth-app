package com.eviahealth.eviahealth.models.devices;

import org.json.JSONException;
import org.json.JSONObject;

public class DeviceType {
    private Integer id_device;
    private String name;
    private String params;

    public DeviceType(Integer id_device, String name, String params) {
        this.id_device = id_device;
        this.name = name;
        this.params = params;
    }

    // {"id":1,"nombre":"Oximetro","params":"[{\"col\":\"bloodoxygen\",\"name\":\"Saturación de oxígeno\"},{\"col\":\"heartrate\",\"name\":\"Ritmo cardíaco\"}]","display":"Oxímetro"}
    public DeviceType(String data) {
        try {
            JSONObject obj = new JSONObject(data);
            this.id_device = obj.getInt("id");
            this.name = obj.getString("nombre");
            this.params = obj.getString("params");
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String toString() {
        return "DeviceType: " +
                "id_device=" + getIdDevice() +
                ", nombre='" + getName() + '\'' +
                ", params='" + getParams() + '\'' ;
    }

    //region :: GETTERS & SETTERS
    public Integer getIdDevice() {
        return id_device;
    }

    public void setIdDevice(Integer id_device) {
        this.id_device = id_device;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }
    //endregion
}
