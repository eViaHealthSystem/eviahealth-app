package com.eviahealth.eviahealth.meeting.models;

public enum MStatusMeeting {

    None(0),
    Connecting(1),
    InMeeting(2),
    Disconnecting(3),
    Idle(4),
    WaitingForHost(5),
    Reconnecting(6),
    Failed(7),
    InWaitingRoom(8),
    WebinarPromote(9),
    WebinarDePromote(10),
    Unknown(11),
            ;

    private int value;

    MStatusMeeting(int value) {
        this.value = value;
    }

    public int getValue(){
        return this.value;
    }

    public int size() { return MStatusMeeting.values().length; }

    public static MStatusMeeting getQuery(int var0) {
        MStatusMeeting[] var1 = values();
        MStatusMeeting[] var2 = var1;

        for(int var4 = 0; var4 < var1.length; ++var4) {
            MStatusMeeting var5 = var2[var4];
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
    MStatusMeeting statusMeeting = MStatusMeeting.None;
    if (statusMeeting == MStatusMeeting.None) {
        Log.e(TAG,"if (statusMeeting == MStatusMeeting.None): SI");
    } else {
        Log.e(TAG,"if (statusMeeting == MStatusMeeting.None): NO");
    }
*/
}
