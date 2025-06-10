package com.eviahealth.eviahealth.models.alivecor.models;

public enum EMethodUI {
    IntentUI(0),
    RecordEkgFragment(1),

    None(100),
    ;

    private int value;

    EMethodUI(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public int size() {
        return EMethodUI.values().length;
    }

    public static EMethodUI getQuery(int var0) {
        EMethodUI[] var1 = values();
        EMethodUI[] var2 = var1;

        for (int var4 = 0; var4 < var1.length; ++var4) {
            EMethodUI var5 = var2[var4];
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
