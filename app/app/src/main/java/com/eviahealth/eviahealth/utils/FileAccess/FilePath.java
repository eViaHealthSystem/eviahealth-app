package com.eviahealth.eviahealth.utils.FileAccess;

public enum FilePath {

    EVLOG("ev_app.log", FileType.LOG),
    REGISTROS_LOG("ev_log_device.log", FileType.LOG),
    REGISTROS_LOG_CONFIG("ev_config.log", FileType.LOG),

    CONFIG_SERIAL("serial.txt", FileType.NORMAL),
    CONFIG_ADMIN("confadmin.json", FileType.NORMAL),
    CONFIG_TOKEN("conftoken.json", FileType.NORMAL),
    CONFIG_ENCUESTA("encuesta.json", FileType.NORMAL),
    CONFIG_DISPOSITIVOS("dispositivos.json", FileType.NORMAL),
    CONFIG_PACIENTE("paciente.json", FileType.NORMAL),
    CONFIG_LOG("conflog.json", FileType.NORMAL),
    CONFIG_LAUNCHER("salirso.txt", FileType.NORMAL),

    DATE_TEST("dateTest.json", FileType.NORMAL),
    SLEEP_DESFASE("sleepDesfase.txt", FileType.NORMAL),

    REGISTROS_ACTIVIDAD_TOTAL_AM4("AM4_jsonactividadtotal.json", FileType.PERIODICO),
    REGISTROS_ACTIVIDAD_DIARIA_AM4("AM4_jsonactividaddiaria.json", FileType.PERIODICO),
    REGISTROS_ACTIVIDAD_SLEEP_AM4("AM4_jsonsuenyo.json", FileType.PERIODICO),

    REGISTROS_ACTIVIDAD_TOTAL_MAMBO6("MAMBO6_jsonactividadtotal.json", FileType.PERIODICO),
    REGISTROS_ACTIVIDAD_DIARIA_MAMBO6("MAMBO6_jsonactividaddiaria.json", FileType.PERIODICO),
    REGISTROS_ACTIVIDAD_SLEEP_MAMBO6("MAMBO6_jsonsuenyo.json", FileType.PERIODICO),
    REGISTROS_ACTIVIDAD_HEART_RATE_MAMBO6("MAMBO6_jsonHeartRate.json", FileType.PERIODICO),
    REGISTROS_ACTIVIDAD_BLODD_OXYGEN_MAMBO6("MAMBO6_jsonBloodOxygen.json", FileType.PERIODICO),
    REGISTROS_ACTIVIDAD_DATA_SLEEP_MAMBO6("MAMBO6_jsonDataSleep.json", FileType.PERIODICO),
    REGISTROS_ACTIVIDAD_DATA_STEPS_MAMBO6("MAMBO6_jsonDataSteps.json", FileType.PERIODICO),

    REGISTROS_BASCULA("jsonBascula.json", FileType.PERIODICO),
    REGISTROS_MONITOR_PULMONAR("jsonLung.json", FileType.PERIODICO),
    REGISTROS_ECG("jsonECG.json", FileType.PERIODICO),

    REGISTROS_ACTIVIDAD_REPORT("jsonactividadreport.json", FileType.PERIODICO),  // NO SE UTILIZA
    REGISTROS_TENSIOMETRO("tensiometro.json", FileType.PERIODICO),
    REGISTROS_OXIMETRO("pulsioximetro.json", FileType.PERIODICO),
    REGISTROS_ENCUESTA("registros_encuesta.json", FileType.PERIODICO),
    REGISTROS_CAT("registros_cat.json", FileType.PERIODICO),
    REGISTROS_TERMOMETRO("jsontermometro.json", FileType.PERIODICO),
    REGISTROS_PEAKFLOW("jsonpeakflow.json", FileType.PERIODICO),
    CARPETA_SUBIDO("_subido", FileType.FOLDER),
    CARPETA_ENSAYO("ensayo", FileType.FOLDER),
    REGISTRO_ENSAYO("ensayo.json", FileType.PERIODICO);

    private String path;
    // sera periodico = true si es un archivo de registros. Se pondra la fecha como prefijo
    private FileType type;
    FilePath(String path, FileType type ) { this.path = path; this.type = type; }
    
    String getPath() {
        return this.path;
    }
    FileType getFileType() { return this.type; }

    public String getNameFile() {
        return this.path;
    }
}
