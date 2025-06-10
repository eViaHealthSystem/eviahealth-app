package com.eviahealth.eviahealth.meeting.models;

import org.json.JSONException;
import org.json.JSONObject;

public class InfoMeeting {

    // {"idprescriptor":"admin","idpaciente":"40404040","fechaVideoCall":"2023-05-04 10:00:00","fechaConfirmado":null,"confirmado":1,"motivo":null,"meeting":"82656552491","password":"123456789"}
    private String idprescriptor;
    private String idpaciente;
    private String fechaVideoCall; //2023-05-04 10:00:00
    private String fechaConfirmado;
    private int confirmado;
    private String motivo;
    private String meeting;
    private String password; // default "123456789"

    public InfoMeeting() {
        setDefaultValues();
    }

    public InfoMeeting(JSONObject meeting) {

        try {
            this.idprescriptor = meeting.getString("idprescriptor");
            this.idpaciente = meeting.getString("idpaciente");
            this.fechaVideoCall = meeting.getString("fechaVideoCall");
            this.fechaConfirmado = meeting.getString("fechaConfirmado");
            this.confirmado = meeting.getInt("confirmado");
            this.motivo = meeting.getString("motivo");
            this.meeting = meeting.getString("meeting");
            this.password = meeting.getString("password");
        }
        catch (JSONException e) {
            setDefaultValues();
        }
    }

    private void setDefaultValues(){
        this.idprescriptor = "";
        this.idpaciente = "";
        this.fechaVideoCall = "";
        this.fechaConfirmado = "";
        this.confirmado = 0;
        this.motivo = "";
        this.meeting = "";
        this.password = "123456789";
    }

    public String getIdprescriptor() {
        return idprescriptor;
    }

    public void setIdprescriptor(String idprescriptor) {
        this.idprescriptor = idprescriptor;
    }

    public String getIdpaciente() {
        return idpaciente;
    }

    public void setIdpaciente(String idpaciente) {
        this.idpaciente = idpaciente;
    }

    public String getFechaVideoCall() {
        return fechaVideoCall;
    }

    public void setFechaVideoCall(String fechaVideoCall) {
        this.fechaVideoCall = fechaVideoCall;
    }

    public String getFechaConfirmado() {
        return fechaConfirmado;
    }

    public void setFechaConfirmado(String fechaConfirmado) {
        this.fechaConfirmado = fechaConfirmado;
    }

    public int getConfirmado() {
        return confirmado;
    }

    public void setConfirmado(int confirmado) {
        this.confirmado = confirmado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getMeeting() {
        return meeting;
    }

    public void setMeeting(String meeting) {
        this.meeting = meeting;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {

        try {
            JSONObject meeting = new JSONObject();
            meeting.put("idprescriptor",this.idprescriptor);
            meeting.put("idpaciente",this.idpaciente);
            meeting.put("fechaVideoCall",this.fechaVideoCall);
            meeting.put("fechaConfirmado",this.fechaConfirmado);
            meeting.put("confirmado",this.confirmado);
            meeting.put("motivo",this.motivo);
            meeting.put("meeting",this.meeting);
            meeting.put("password",this.password);
            return meeting.toString();
        }
        catch (JSONException e) {
            return "";
        }
    }
}
