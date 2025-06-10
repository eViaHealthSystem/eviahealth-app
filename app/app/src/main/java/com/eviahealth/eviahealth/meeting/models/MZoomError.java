package com.eviahealth.eviahealth.meeting.models;

public enum MZoomError {

    ZOOM_ERROR_SUCCESS (0, "Api llama con éxito."),
    ZOOM_ERROR_INVALID_ARGUMENTS (1, "Las llamadas a la API fallaron debido a uno o más argumentos no válidos."),
    ZOOM_ERROR_ILLEGAL_APP_KEY_OR_SECRET (2, "Error de autenticación: clave o error secreto"),
    ZOOM_ERROR_NETWORK_UNAVAILABLE (3, "Las llamadas Api fallaron porque la red no está disponible."),
    ZOOM_ERROR_AUTHRET_CLIENT_INCOMPATIBLEE (4, "Las llamadas Api fallaron porque la versión del cliente de autenticación no es compatible."),
    ZOOM_ERROR_AUTHRET_TOKENWRONG (5, "El token jwt para autenticar es incorrecto"),
    ZOOM_ERROR_AUTHRET_KEY_OR_SECRET_ERROR (6, "Las llamadas Api fallaron porque la clave o el secreto es ilegal."),
    ZOOM_ERROR_AUTHRET_ACCOUNT_NOT_SUPPORT (7, "Error de autenticación: la cuenta no es compatible"),
    ZOOM_ERROR_AUTHRET_ACCOUNT_NOT_ENABLE_SDK (8, "Error de autenticación: la cuenta no habilita SDK"),
    ZOOM_ERROR_DEVICE_NOT_SUPPORTED (99, "El dispositivo no es compatible con ZOOM."),
    ZOOM_ERROR_UNKNOWN (1000, "Las llamadas Api fallaron debido a una razón desconocida.");

    private int code;
    private String msg;

    public int getCode() {
        return this.code;
    }

    public void setCode(int var1) {
        this.code = var1;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String var1) {
        this.msg = var1;
    }

    private MZoomError(int var3, String var4) {
        this.code = var3;
        this.msg = var4;
    }

    public static MZoomError toErrorCode(int var0) {
        MZoomError[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            MZoomError var4 = var1[var3];
            if (var4.getCode() == var0) {
                return var4;
            }
        }

        return ZOOM_ERROR_UNKNOWN;
    }

    public static String toString(int var0) {
        return toErrorCode(var0).toString();
    }
}
