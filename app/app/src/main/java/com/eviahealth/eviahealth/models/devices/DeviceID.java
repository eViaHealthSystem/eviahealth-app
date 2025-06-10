package com.eviahealth.eviahealth.models.devices;

public enum DeviceID {
    None(0),
    Oximetro(1),
    Tensiometro(2),
    Actividad(3),
    Steps(4),
    Sleep(5),
    ViadoxFC(6),
    ViadoxAD(7),
    Cpap(8),
    Encuesta(9),
    Termometro(10),
    PeakFlow(11),
    Cat(12),
    Concentrador(13),
    Am5(14),
    Scale(15),
    Lung(16),
    Ecg(17),

    Unknown(100),
    ;

    private int value;

    DeviceID(int value) {
        this.value = value;
    }

    public int getValue(){
        return this.value;
    }

    public int size() { return DeviceID.values().length; }

    public static DeviceID getQuery(int var0) {
        DeviceID[] var1 = values();
        DeviceID[] var2 = var1;

        for(int var4 = 0; var4 < var1.length; ++var4) {
            DeviceID var5 = var2[var4];
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
