package com.eviahealth.eviahealth.bluetooth.beurer.po60;

public enum PO60Status {
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

    PO60Status(int value) {
        this.value = value;
    }

    public int getValue(){
        return this.value;
    }

    public int size() { return PO60Status.values().length; }

    public static PO60Status getQuery(int var0) {
        PO60Status[] var1 = values();
        PO60Status[] var2 = var1;

        for(int var4 = 0; var4 < var1.length; ++var4) {
            PO60Status var5 = var2[var4];
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
