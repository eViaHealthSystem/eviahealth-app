package com.eviahealth.eviahealth.api.BackendConector;

public class ApiException extends Exception {
    private int code;
    public ApiException(String msg, int code) {
        super(msg);
        this.code = code;
    }
    public int getCode() {
        return this.code;
    }
}
