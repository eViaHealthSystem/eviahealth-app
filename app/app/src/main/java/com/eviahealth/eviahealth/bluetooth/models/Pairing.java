package com.eviahealth.eviahealth.bluetooth.models;

public enum Pairing {
    None(0),
    bf600(1),
    Unknown(100),
    ;

    private int value;

    Pairing(int value) {
        this.value = value;
    }

    public int getValue(){
        return this.value;
    }

    public int size() { return Pairing.values().length; }

    public static Pairing getQuery(int var0) {
        Pairing[] var1 = values();
        Pairing[] var2 = var1;

        for(int var4 = 0; var4 < var1.length; ++var4) {
            Pairing var5 = var2[var4];
//            Log.e("Prueba","" + var5.toString()); // Lista de nombres de la enumeracion
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
