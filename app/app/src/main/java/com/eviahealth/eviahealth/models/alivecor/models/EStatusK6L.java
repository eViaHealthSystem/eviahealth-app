package com.eviahealth.eviahealth.models.alivecor.models;

public enum EStatusK6L {
    None(0),
    SdkInitialized(1),
    Scanning(2),
    Detected(3),
    WaitBPM(4),
    Recording(5),
    RecordComplete(6),
    CreateReport(7),
    ViewEcg(8),
    ErrorJWT(9),
    JWTOk(10),
    SdkFail(11),


    NotFound(19),
    Pairing(20),
    BondBonded(21),
    BondBonding(22),
    BondNone(23),
    Unknown(100),
    ;

    private int value;

    EStatusK6L(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public int size() {
        return EStatusK6L.values().length;
    }

    public static EStatusK6L getQuery(int var0) {
        EStatusK6L[] var1 = values();
        EStatusK6L[] var2 = var1;

        for (int var4 = 0; var4 < var1.length; ++var4) {
            EStatusK6L var5 = var2[var4];
            if (var5.getValue() == var0) {
                return var5;
            }
        }

        return None;
    }

    public static String toString(int var0) {
        return getQuery(var0).toString();
    }
}
