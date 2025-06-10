package com.eviahealth.eviahealth.bluetooth.beurer.ft95;


public enum FT95Status {

    None(0),
    Search(1),
    Connecting(2),
    Disconnecting(3),
    Connected(4),
    Disconnected(5),
    Fail(6),
    Data(7),
    Bond(8),
    Unknown(11),
    ;

    private int value;

    FT95Status(int value) {
        this.value = value;
    }

    public int getValue(){
        return this.value;
    }

    public int size() { return FT95Status.values().length; }

    public static FT95Status getQuery(int var0) {
        FT95Status[] var1 = values();
        FT95Status[] var2 = var1;

        for(int var4 = 0; var4 < var1.length; ++var4) {
            FT95Status var5 = var2[var4];
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
