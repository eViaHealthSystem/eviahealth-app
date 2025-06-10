package com.eviahealth.eviahealth.models.vitalograph;

import android.util.Log;

import com.eviahealth.eviahealth.utils.util;

import org.json.JSONException;
import org.json.JSONObject;

public class Patient {

    private String sex;
    private String birthday;
    private int age;
    private int weight;
    private int height;
    private Boolean bydefault;

    public Patient (String sex, int age, int weight, int height) {
        this.sex = sex;
        this.age = age;
        this.weight = weight;
        this.height = height;
        this.birthday = util.obtenerFechaNacimiento(age);
        this.bydefault = false;  // Clase creada con datos de paciente buenos
        Log.e("Patient", "Patient constructor 1");
    }

    public Patient (String sex, int age, int weight, int height, Boolean bydefault) {
        this.sex = sex;
        this.age = age;
        this.weight = weight;
        this.height = height;
        this.birthday = util.obtenerFechaNacimiento(age);
        this.bydefault = bydefault;  // false = Clase creada con datos de paciente buenos, true = Clase creada con datos de paciente por defecto
        Log.e("Patient", "Patient constructor 2");
    }

    public Patient (String sex, String birthday, int age, int weight, int height, Boolean bydefault) {
        this.sex = sex;
        this.age = age;
        this.weight = weight;
        this.height = height;
        this.birthday = birthday;
        this.bydefault = bydefault;  // false = Clase creada con datos de paciente buenos, true = Clase creada con datos de paciente por defecto
        Log.e("Patient", "Patient constructor 3");
    }

    public Patient (String strjson) {

        try {
            JSONObject obj = new JSONObject(strjson);
            this.sex = obj.getString("gender");
            this.age = obj.getInt("age");
            this.weight = obj.getInt("weight");
            this.height = obj.getInt("height");
            if (obj.has("birthday")) { this.birthday = obj.getString("birthday"); }
            else { this.birthday = util.obtenerFechaNacimiento(age); }
            this.bydefault = false;  // Clase creada con datos de paciente buenos
        }
        catch (JSONException ex) {
            ex.printStackTrace();
            this.sex = "Hombre";
            this.age = 65;
            this.weight = 70;
            this.height = 165;
            this.birthday = "01/01/1960";
            this.bydefault = true;  // Clase creada con datos de paciente por defecto
        }
        finally {
            Log.e("Patient", "Patient constructor 4");
        }
    }

    public JSONObject get() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("gender", this.sex);
            obj.put("age", this.age);
            obj.put("weight", this.weight);
            obj.put("height", this.height);
            obj.put("birthday", this.birthday);
            return obj;
        }
        catch (JSONException e){
            Log.e("Patient", "Exception: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String toString() {
        try {
            return get().toString();
        }
        catch (Exception e){
            Log.e("Patient", "Exception toString: " + e.getMessage());
            return "{}";
        }
    }

    public String getGender() {
        return sex;
    }

    public void setGender(String gender) {
        this.sex = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Boolean getBydefault() {
        return bydefault;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }
}
