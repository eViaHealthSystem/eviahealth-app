package com.eviahealth.eviahealth.models.transtek.mb6.models.setting;

public enum CmdTranstek {
    SetpOfHour(87),
    StepOfDay(27),
    Other(0),
    ;
    private int value;

    CmdTranstek(int value) {
        this.value = value;
    }

    public int getValue(){
        return this.value;
    }

    public static CmdTranstek getCmd(int var0) {
        CmdTranstek[] var1 = values();
        CmdTranstek[] var2 = var1;
        int var3 = var1.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            CmdTranstek var5 = var2[var4];
            if (var5.getValue() == var0) {
                return var5;
            }
        }

        return Other;
    }
}
