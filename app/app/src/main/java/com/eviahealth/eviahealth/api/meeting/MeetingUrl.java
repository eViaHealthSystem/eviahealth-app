package com.eviahealth.eviahealth.api.meeting;

public enum MeetingUrl {
    CONSULTA("/api/videocall/consulta"),
    PLANIFICADA("/api/videocall/planificada"),
    CONFIRMAR("/api/videocall/confirm"),
    RECHAZAR("/api/videocall/refused"),
    ;

    String URL;
    MeetingUrl(String url) { this.URL = url; }
    String getURL() { return this.URL; }
}

/*
    <string name="consulta">videocall/consulta</string>
    <string name="planificada">videocall/planificada</string>
    <string name="confirmar">videocall/confirm</string>
    <string name="rechazar">videocall/refused</string>
 */