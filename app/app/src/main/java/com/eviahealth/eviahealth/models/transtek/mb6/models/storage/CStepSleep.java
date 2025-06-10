package com.eviahealth.eviahealth.models.transtek.mb6.models.storage;

public class CStepSleep {

    private String date;
    private Integer steps;
    private Integer calories;
    private Integer deep;
    private Integer ligth;
    private Integer awake;

    public CStepSleep(String date, Integer steps, Integer calories, Integer deep, Integer ligth, Integer awake) {
        this.date = date;
        this.steps = steps;
        this.calories = calories;
        this.deep = deep;
        this.ligth = ligth;
        this.awake = awake;
    }

    public CStepSleep(String date, Integer steps, Integer calories) {
        this.date = date;
        this.steps = steps;
        this.calories = calories;
        this.deep = 0;
        this.ligth = 0;
        this.awake = 0;
    }

    public CStepSleep(String date, Integer deep, Integer ligth, Integer awake) {
        this.date = date;
        this.steps = 0;
        this.calories = 0;
        this.deep = deep;
        this.ligth = ligth;
        this.awake = awake;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Integer getSteps() {
        return steps;
    }

    public void setSteps(Integer steps) {
        this.steps = steps;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public Integer getDeep() {
        return deep;
    }

    public void setDeep(Integer deep) {
        this.deep = deep;
    }

    public Integer getLigth() {
        return ligth;
    }

    public void setLigth(Integer ligth) {
        this.ligth = ligth;
    }

    public Integer getAwake() {
        return awake;
    }

    public void setAwake(Integer awake) {
        this.awake = awake;
    }

    @Override
    public String toString() {

        String json = "{ \"time\":\"" + getDate() + "\",\t";
        json += "\"steps\":" + getSteps() + ",\t";
        if (getSteps() <= 99) json += "\t";
        json += "\"calories\":" + getCalories() + ",\t\t";
//        if (getCalories() <= 99) json += "\t";
        json += "\"deep\":" + getDeep() + ",\t";
        if (getDeep() <= 99) json += "\t";
        json += "\"ligt\":" + getLigth() + ",\t";
        if (getLigth() <= 99) json += "\t";
        json += "\"awake\":" + getAwake() + " }";

        return  json;
    }

}
