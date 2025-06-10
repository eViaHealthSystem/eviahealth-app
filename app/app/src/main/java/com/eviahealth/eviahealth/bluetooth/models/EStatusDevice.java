package com.eviahealth.eviahealth.bluetooth.models;

public enum EStatusDevice {
    None(0),
    Scanning(1),
    Detected(2),
    Connecting(3),
    Connected(4),
    Measurement(5),
    WaitMeasurement(6),
    Disconnecting(7),
    Disconnect(8),
    Reconnecting(9),
    Failed(10),
    Found(11),
    WaitTest(12),
    EndTest(13),
    ClearMemory(14),
    GetTime(15),
    SetTime(16),
    ResetDefault(17),
    ExitRemoteMode(18),
    NotFound(19),

    Pairing(20),
    BondBonded(21),
    BondBonding(22),
    BondNone(23),
    Unknown(100),
    ;

    private int value;

    EStatusDevice(int value) {
        this.value = value;
    }

    public int getValue(){
        return this.value;
    }

    public int size() { return EStatusDevice.values().length; }

    public static EStatusDevice getQuery(int var0) {
        EStatusDevice[] var1 = values();
        EStatusDevice[] var2 = var1;

        for(int var4 = 0; var4 < var1.length; ++var4) {
            EStatusDevice var5 = var2[var4];
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

/*
    EStatusDevice statusMeeting = EStatusDevice.None;
    if (statusMeeting == EStatusDevice.None) {
        Log.e(TAG,"if (statusMeeting == EStatusDevice.None): SI");
    } else {
        Log.e(TAG,"if (statusMeeting == EStatusDevice.None): NO");
    }
*/
}
