package com.eviahealth.eviahealth.models.transtek.mb6.models.setting;

public enum ENErrorCode {
    Success(0, "Success"),
    Unknown(-1, "Unknown"),
    Uninitialized(-2, "Uninitialized"),
    ParameterError(1, "ParameterError"),
    FileFormatError(2, "FileFormatError"),
    ManagerStateError(5, "ManagerStateError"),
    DeviceNotConnected(7, "DeviceNotConnected"),
    DeviceUnsupported(8, "DeviceUnsupported"),
    FileVerificationFailed(9, "FileVerificationFailed"),
    ConfirmTimeout(10, "ConfirmTimeout"),
    LowBattery(11, "LowBattery"),
    VersionNotMatch(12, "VersionNotMatch"),
    FileHeaderError(13, "FileHeaderError"),
    FlashSaveFailed(14, "FlashSaveFailed"),
    ScanTimeout(15, "ScanTimeout"),
    ConnectFailed(16, "ConnectFailed"),
    ConnectTimeout(21, "ConnectTimeout"),
    BluetoothUnavailable(23, "BluetoothUnavailable"),
    AbnormalDisconnect(24, "AbnormalDisconnect"),
    WriteDataTimeout(25, "WriteDataTimeout"),
    UserCancel(26, "UserCancel"),
    SettingTimeout(28, "SettingTimeout"),
    FileUpdating(30, "FileUpdating"),
    FileNotFound(41, "FileNotFound"),
    FileSystemBusy(42, "FileSystemBusy"),
    FileParameterError(43, "FileParameterError"),
    FileVerifyError(44, "FileVerifyError"),
    FileException(45, "FileException"),
    FileCancel(46, "FileCancel");

    private int code;
    private String msg;

    public int getCode() {
        return this.code;
    }

    public void setCode(int var1) {
        this.code = var1;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String var1) {
        this.msg = var1;
    }

    private ENErrorCode(int var3, String var4) {
        this.code = var3;
        this.msg = var4;
    }

    public static ENErrorCode toErrorCode(int var0) {
        ENErrorCode[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            ENErrorCode var4 = var1[var3];
            if (var4.getCode() == var0) {
                return var4;
            }
        }

        return Unknown;
    }
}
