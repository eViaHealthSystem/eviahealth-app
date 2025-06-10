package com.eviahealth.eviahealth.models.transtek.mb6.models.setting;

public enum PairMode {
    Random(0x03),
    Qrcode(0x04),
    Auto(0x05),
    ;
    private int value;

    PairMode(int value) {
        this.value = value;
    }

    public int getValue(){
        return this.value;
    }
}
