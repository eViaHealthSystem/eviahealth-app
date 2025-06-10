package com.eviahealth.eviahealth.models.transtek.mb6.models.setting;

public enum LSQuerySetting {

    None(0),
    Language(1),
    Arm(2),
    TimeFormat(3),
    DistanceUnit(4),
    MeasurementUnit(5),
    QuietMode(6),
    RealtimeHeartRate(7),
    RealtimeBloodOxygen(8),
    SedentaryInfo(9),
    UpdateDatetime(10),
    WatchPage(11),
    fisnish(12),
    ;

    private int value;

    LSQuerySetting(int value) {
        this.value = value;
    }

    public int getValue(){
        return this.value;
    }

    public int size() { return LSQuerySetting.values().length; }

    public String getQuerySetting() {
        return getQuery(this.value).toString();
    }

    public static LSQuerySetting getQuery(int var0) {
        LSQuerySetting[] var1 = values();
        LSQuerySetting[] var2 = var1;

        for(int var4 = 0; var4 < var1.length; ++var4) {
            LSQuerySetting var5 = var2[var4];
//            Log.e("Prueba","" + var5.toString()); // Lista de nombres de la enumeracion
            if (var5.getValue() == var0) {
                return var5;
            }
        }

        return None;
    }

    /*
        LSQuerySetting querySetting = LSQuerySetting.None;

        Log.e(TAG,"querySetting.size(): " + querySetting.size());
        Log.e(TAG,"querySetting.getValue(): " + querySetting.getValue());
        Log.e(TAG,"querySetting.getQuerySetting(): " + querySetting.getQuerySetting());
        if (querySetting == LSQuerySetting.None) {
            Log.e(TAG,"if (querySetting == LSQuerySetting.None): SI");
        } else {
            Log.e(TAG,"if (querySetting == LSQuerySetting.None): NO");
        }

        Log.e(TAG,"LSQuerySetting.getQuery(3)");
        querySetting = LSQuerySetting.getQuery(3);

        Log.e(TAG,"querySetting.getValue(): " + querySetting.getValue());
        Log.e(TAG,"querySetting.getQuerySetting(): " + querySetting.getQuerySetting());
        if (querySetting == LSQuerySetting.None) {
            Log.e(TAG,"if (querySetting == LSQuerySetting.None): SI");
        } else {
            Log.e(TAG,"if (querySetting == LSQuerySetting.None): NO");
        }
     */
}
