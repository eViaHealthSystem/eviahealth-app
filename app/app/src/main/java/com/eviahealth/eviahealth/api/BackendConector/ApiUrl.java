package com.eviahealth.eviahealth.api.BackendConector;

public enum ApiUrl {

    LOGIN("/api/login"),
    PACIENTE_EXISTE("/api/paciente/existe"),
    PACIENTE_GET_DATOS("/api/paciente/datos"),
    PACIENTE_GET_ID_ENCUESTA("/api/encuestas/paciente/get_id"),
    PACIENTE_GET_ENCUESTA("/api/encuestas/paciente/get"),
    PACIENTE_UPDATE_ENCUESTA("/api/encuestas/paciente/set"),
    PACIENTE_UPDATE_HORA_ENSAYO("/api/paciente/update_hora_ensayo"),

    ENCUESTAS_GET("/api/encuestas/get"),

    PACIENTE_EQUIPO_UPDATE("/api/paciente/equipo/update"),
    PACIENTE_EQUIPO_GET("/api/paciente/equipos/get"),
    PACIENTE_EQUIPO_GET_EXTRAS("/api/paciente/equipos/get_extra"),

    DISPOSITIVOS_GET("/api/paciente/dispositivos"),

    // subir datos dispositivos
    SUBIR_REGISTRO_TERMOMETRO("/api/paciente/termometro/insertar"),
    SUBIR_REGISTRO_PEAKFLOW("/api/paciente/flujo_resp/insertar"),
    SUBIR_REGISTRO_ENCUESTA("/api/paciente/respuesta/insertar"),
    SUBIR_REGISTRO_CAT("/api/paciente/cat/insertar"),
    SUBIR_REGISTRO_TENSIOMETRO("/api/paciente/datostensiometro"),
    SUBIR_REGISTRO_ACTIVIDAD("/api/paciente/datosactividadtotal"),
    SUBIR_REGISTRO_SUENYO("/api/paciente/datosactividadsuenyo"),
    SUBIR_REGISTRO_OXIMETRO("/api/paciente/datosoximetro"),
    SUBIR_REGISTRO_MEASSURE("/api/paciente/datameassures"),
    SUBIR_SLEEP("/api/paciente/datasleep"),
    SUBIR_STEPS("/api/paciente/datasteps"),
    SUBIR_SURVEYS("/api/paciente/respuesta/dataencuestas"),

    PROCESAR_INCIDENCIAS("/api/incidencias/procesar"),
    SUBIR_LOG("/api/subir_archivo"),

    // PDF ECG PACIENTE
    ECG_PACIENTE("/api/paciente/upload_ECG"),

    //Foto paciente
    FOTO_PACIENTE("/api/admin_pacientes/upload_foto"),
    CARACTERISTICAS_PACIENTE("/api/admin_pacientes/characteristics"),
    SET_CARACTERISTICAS_PACIENTE("/api/admin_pacientes/set_characteristics"),
    CALCULATE_FEV1PB("/api/paciente/calculate_fev1pb"),

    // FORMACION
    FORMACION("/api/formacion/listado"),

    //ensayo
    CREAR_ENSAYO("/api/ensayo/crear"),
    SUBIR_LOG_ENSAYO("/api/ensayo/insert_logs"),

    // CONSENTIMIENTO
    GET_CONSENTIMIENTO("/api/paciente/is_consentimiento_inicial_aceptado"),
    CONSENTIMIENTO_OK("/api/paciente/consentimiento_inicial_aceptado"),
    GET_TEXT_CONSENTIMIENTO("/api/textos/get"),

    // KARDIA
    JWT_KARDIA("/kardia/token"),
    ;

    String URL;
    ApiUrl(String url) { this.URL = url; }
    String getURL() { return this.URL; }
}
