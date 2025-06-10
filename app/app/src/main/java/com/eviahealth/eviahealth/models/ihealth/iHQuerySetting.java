package com.eviahealth.eviahealth.models.ihealth;

public enum iHQuerySetting {

    None(0),
    DeviceInfo(1),
    UserInfo(2),
    RestoreFactory(3),
    Unit(4),
    SpecifyOnlineUsers(5),
    CreateUserInfo(6),
    UpdateUserInfo(7),
    AnonymousDataCount(8),
    AnonymousData(9),
    OfflineDataCount(10),
    OfflineData(11),
    DeleteHistoryData(12),
    WaitMeasure(13),
    OnlineRealTime(14),
    OnlineResult(15),
    BodyFatResult(16),
    MeasureFinishAtCritical(17),
    ErrorHS(18),

    Fisnish(100),
    ;

    private int value;

    iHQuerySetting(int value) {
        this.value = value;
    }

    public int getValue(){
        return this.value;
    }

    public int size() { return iHQuerySetting.values().length; }

    public String getQuerySetting() {
        return getQuery(this.value).toString();
    }

    public static iHQuerySetting getQuery(int var0) {
        iHQuerySetting[] var1 = values();
        iHQuerySetting[] var2 = var1;

        for(int var4 = 0; var4 < var1.length; ++var4) {
            iHQuerySetting var5 = var2[var4];
//            Log.e("Prueba","" + var5.toString()); // Lista de nombres de la enumeracion
            if (var5.getValue() == var0) {
                return var5;
            }
        }
        return None;
    }

    /*
        iHQuerySetting querySetting = iHQuerySetting.None;

        Log.e(TAG,"querySetting.size(): " + querySetting.size());
        Log.e(TAG,"querySetting.getValue(): " + querySetting.getValue());
        Log.e(TAG,"querySetting.getQuerySetting(): " + querySetting.getQuerySetting());
        if (querySetting == iHQuerySetting.None) {
            Log.e(TAG,"if (querySetting == iHQuerySetting.None): SI");
        } else {
            Log.e(TAG,"if (querySetting == iHQuerySetting.None): NO");
        }

        Log.e(TAG,"iHQuerySetting.getQuery(3)");
        querySetting = iHQuerySetting.getQuery(3);

        Log.e(TAG,"querySetting.getValue(): " + querySetting.getValue());
        Log.e(TAG,"querySetting.getQuerySetting(): " + querySetting.getQuerySetting());
        if (querySetting == iHQuerySetting.None) {
            Log.e(TAG,"if (querySetting == iHQuerySetting.None): SI");
        } else {
            Log.e(TAG,"if (querySetting == iHQuerySetting.None): NO");
        }
     */
}
