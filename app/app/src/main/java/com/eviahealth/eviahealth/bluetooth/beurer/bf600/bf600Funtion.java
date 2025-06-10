package com.eviahealth.eviahealth.bluetooth.beurer.bf600;

public enum bf600Funtion {
    None(0),
    GetCurrentTime(1),
    SetCurrentTime(2),
    CreateUser(3),
    UserActive(4),
    DeleteUser(5),
    SetDateOfBirth(6),
    SetGender(7),
    SetHeight(8),
    ActivityLevel(9),
    GetDatabase(10),
    SetDatabase(11),
    TakeMeasurement(12),
    GetDateOfBirth(13),
    GetGender(14),
    GetHeight(15),
    GetUserIndex(16),
    OperateDeleteUser(17),
    OperateCreateUser(18),
    SetScaleSetting(19),
    GetScaleSetting(20),
    OperateUserSetting(21),
    OperateTest(22),
    OperateUserActivate(23),
    OperateActivateCreate(24),
    OperateActivateUserDefault(25),
    UserList(26),
    GetServiceChange(27),
    GetBatteryLevel(28),
    Unknown(100),
    ;

    private int value;

    bf600Funtion(int value) {
        this.value = value;
    }

    public int getValue(){
        return this.value;
    }

    public int size() { return bf600Funtion.values().length; }

    public static bf600Funtion getQuery(int var0) {
        bf600Funtion[] var1 = values();
        bf600Funtion[] var2 = var1;

        for(int var4 = 0; var4 < var1.length; ++var4) {
            bf600Funtion var5 = var2[var4];
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
