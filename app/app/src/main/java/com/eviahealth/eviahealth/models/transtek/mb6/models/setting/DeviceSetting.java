package com.eviahealth.eviahealth.models.transtek.mb6.models.setting;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.lifesense.plugin.ble.LSBluetoothManager;
import com.lifesense.plugin.ble.OnSettingListener;
import com.lifesense.plugin.ble.data.tracker.config.ATBloodOxygenMonitor;
import com.lifesense.plugin.ble.data.tracker.config.ATConfigItem;
import com.lifesense.plugin.ble.data.tracker.config.ATMeasureUnit;
import com.lifesense.plugin.ble.data.tracker.config.ATStrideInfo;
import com.lifesense.plugin.ble.data.tracker.config.ATTime;
import com.lifesense.plugin.ble.data.tracker.setting.ATConfigItemSetting;
import com.lifesense.plugin.ble.data.tracker.setting.ATDistanceFormatSetting;
import com.lifesense.plugin.ble.data.tracker.setting.ATFunctionSetting;
import com.lifesense.plugin.ble.data.tracker.setting.ATFunctionType;
import com.lifesense.plugin.ble.data.tracker.setting.ATHRDetectCycleSetting;
import com.lifesense.plugin.ble.data.tracker.setting.ATHeartRateDetectSetting;
import com.lifesense.plugin.ble.data.tracker.setting.ATLanguageSetting;
import com.lifesense.plugin.ble.data.tracker.setting.ATQuietModeSetting;
import com.lifesense.plugin.ble.data.tracker.setting.ATScreenPagesSetting;
import com.lifesense.plugin.ble.data.tracker.setting.ATSedentaryItem;
import com.lifesense.plugin.ble.data.tracker.setting.ATSedentarySetting;
import com.lifesense.plugin.ble.data.tracker.setting.ATTimeFormatSetting;
import com.lifesense.plugin.ble.data.tracker.setting.ATTimeSetting;
import com.lifesense.plugin.ble.data.tracker.setting.ATUserInfoSetting;
import com.lifesense.plugin.ble.data.tracker.setting.ATWatchPage;
import com.lifesense.plugin.ble.data.tracker.setting.ATWearingStyleSetting;
import com.lifesense.plugin.ble.data.tracker.setting.ATWeekDay;

import java.util.ArrayList;
import java.util.List;

public class DeviceSetting {
    private static final String TAG = "DeviceSetting";
    protected static Context baseContext;

    public static void setBaseContext(Context baseContext) {
        DeviceSetting.baseContext = baseContext;
    }

    public enum HBLanguage {

        English("en"),
        Japanese("ja"),
        French("fr"),
        German("de"),
        Italian("it"),
        Spanish("es"),
        ;

        public String languageCode = "";
        private HBLanguage(String languageCode){
            this.languageCode = languageCode;
        }

        public String getLanguageCode(){
            return languageCode;
        }

        /**
         * Abreviatura del idioma
         * @param languageCode
         */
        public void setLanguageCode(String languageCode){
            this.languageCode = languageCode;
        }
    }

    public static void initActivityContext(Activity activity) {
        baseContext = activity;
    }

    /**
     * Update DateTime
     */
    public static void setUpdateDateTime(String mac, final OnSettingListener listener) {
//        int utc = 1676026414; // "10 Feb 2023 10:53:34"
        int utc = (int)(System.currentTimeMillis()/1000l);  // Ahora
        ATTime time = new ATTime(utc,ATTime.getCurrentTimeZone());
        ATTimeSetting setting = new ATTimeSetting(time);
        LSBluetoothManager.getInstance().pushSyncSetting(mac, setting, listener);
    }


    /**
     * Configura Formato de Hora 24H
     * @param mac
     */
    public static void updateTimeFormat(String mac,final OnSettingListener listener){
        // 24H
        int index = ATTimeFormatSetting.TIME_FORMAT_24H;
        ATTimeFormatSetting modeSetting = new ATTimeFormatSetting(index);
        LSBluetoothManager.getInstance().pushSyncSetting(mac, modeSetting, listener);
    }


    /**
     * Configura la pulsera con el idioma indicado
     * @param mac
     * @param language
     */
    public static void setDeviceLanguage(String mac, String language, final OnSettingListener listener){
        HBLanguage idioma = HBLanguage.valueOf(language);
        ATLanguageSetting setting = new ATLanguageSetting(HBLanguage.English.getLanguageCode());
        setting.setLanguage(idioma.getLanguageCode()); // abreviatura del idioma
        LSBluetoothManager.getInstance().pushSyncSetting(mac, setting, listener);
    }


    /**
     * Configura el brazo donde se lleva la pulsera
     * @param mac
     * @param brazo Izquierdo o Derecho
     */
    public static void updateWearingStyles(String mac, String brazo, final OnSettingListener listener){
        int index = ATWearingStyleSetting.WEARING_STYLE_LEFT;
        if (brazo.equals("Derecho")) { index = ATWearingStyleSetting.WEARING_STYLE_RIGHT; }
        ATWearingStyleSetting modeSetting = new ATWearingStyleSetting(index);
        LSBluetoothManager.getInstance().pushSyncSetting(mac, modeSetting, listener);
    }


    /**
     * Actualizar la configuración del modo NO MOLESTAR en la pulsera
     *
     * @param deviceMac
     * @param startTime Hora de incio, formato "10:00"
     * @param endTime   Hora fin, formato "20:00"
     * @param enable    Enable/Disable Servicio NO MOLESTAR
     * @param listener  Callback OnSettingListener
     */
    public static void updateQuietModeSetting(final String deviceMac, String startTime, String endTime, boolean enable,  final OnSettingListener listener) {

//        String startTime = "00:00";
//        String endTime = "23:59";
        boolean autoState = enable;       // Habilita modo no molestas
        boolean state = enable;

        List<ATFunctionSetting> functionInfos = new ArrayList<ATFunctionSetting>();

        // Lista de funciones del dispositivo permitidas o desactivadas en el modo No molestar automático
        // ATFunctionSetting >> HeartbeatDataCollect, IncomingCall, LowBatteryReminder, ManualExerciseMode, MessageRemind, ScreenPowerOn, scrollDisplay, Unknown

        // disable Elevación de la muñeca para iluminar la pantalla
        ATFunctionSetting fun = new ATFunctionSetting(false, ATFunctionType.ScreenPowerOn);
        functionInfos.add(fun);

        // disable Alerta por vibración de batería baja
        fun = new ATFunctionSetting(false, ATFunctionType.LowBatteryReminder);
        functionInfos.add(fun);

        //to mode
        ATQuietModeSetting quietMode = new ATQuietModeSetting();
        quietMode.setStartTime(startTime);
        quietMode.setEndsTime(endTime);
        quietMode.setStatus(state);                 // Bit de estado del modo no molestar activo
        quietMode.setAutoState(false);              // Bit de indicador de estado activo del modo No molestar automático
        quietMode.setFunctions(functionInfos);
        quietMode.setCmd(0xFB);

        Log.e(TAG,"quiet mode setting >> " + quietMode.toString());
        //calling interface
        LSBluetoothManager.getInstance().pushSyncSetting(deviceMac, quietMode, listener);

    }

    /**
     * Ciclo de detección Heart Rate
     * @param deviceMac
     * @param number Periodo de detección: 0x00 Cada segundo, 5=5 minutos, 10=10 minutos, 20=20 minutos, 30=30 minutos
     * @param enable
     * @param listener
     */
    public static void setHeartRateDetect(final String deviceMac, String number, Boolean enable, final OnSettingListener listener) {
//        Arrays.asList("0", "5", "10", "20", "30"));
        ATHRDetectCycleSetting setting = new ATHRDetectCycleSetting();
        setting.setEnable(enable);
        setting.setDetectCycle(Integer.parseInt(number));
        LSBluetoothManager.getInstance().pushSyncSetting(deviceMac, setting, listener);
    }


    /**
     * Ajustar la visualización de la distancia de la pulsera
     *
     * @param deviceMac
     * @param type      0x00: Métrica, 0x01: Imperial
     * @param listener
     */
    public static void updateDistanceUnit(final String deviceMac, int type, final OnSettingListener listener) {
        ATDistanceFormatSetting modeSetting = new ATDistanceFormatSetting(type);
        LSBluetoothManager.getInstance().pushSyncSetting(deviceMac, modeSetting, listener);
    }


    /**
     * Configuración de la unidad de longitud
     * @param deviceMac
     * @param unidades      Unidades: 0x00 = Métrico (km/kg/cm) ATMeasureUnit.UNIT_METRIC, 0x01 = Imperial (millas/lb/ft)
     * @param listener
     */
    public static void measurementUnitSetting(final String deviceMac, int unidades, final OnSettingListener listener){
        ATMeasureUnit unit = new ATMeasureUnit(unidades);
        List<ATConfigItem> configItems = new ArrayList<ATConfigItem>();
        configItems.add(unit);

        ATConfigItemSetting setting = new ATConfigItemSetting(configItems);
        LSBluetoothManager.getInstance().pushSyncSetting(deviceMac, setting, listener);
    }


    /**
     * Ajustes de sincronización de datos de frecuencia cardiaca en tiempo real
     *
     * @param enable
     * @param deviceMac
     * @param listener
     * @return
     */
    public static void syncRealtimeHeartRate(String deviceMac, boolean enable, OnSettingListener listener) {
        ATHeartRateDetectSetting detectSetting = new ATHeartRateDetectSetting(enable);
        LSBluetoothManager.getInstance().pushSyncSetting(deviceMac, detectSetting, listener);
    }


    /**
     * Ajustes de sincronización de datos de frecuencia cardiaca en tiempo real
     *
     * @param enable
     * @param deviceMac
     * @param listener
     * @return
     */
    public static void syncRealtimeBloodOxygen(String deviceMac, boolean enable, OnSettingListener listener) {
        ATBloodOxygenMonitor monitor = new ATBloodOxygenMonitor(enable);

        List<ATConfigItem> configItems = new ArrayList<ATConfigItem>();
        configItems.add(monitor);

        ATConfigItemSetting setting = new ATConfigItemSetting(configItems);
        LSBluetoothManager.getInstance().pushSyncSetting(deviceMac, setting, listener);
    }


    /**
     * Ajustes del recordatorio de sedentarismo para la pulsera
     *
     * @param deviceMac
     * @param listener
     */
    public static void updateSedentaryInfo(final String deviceMac, ATSedentaryItem sedentary, final OnSettingListener listener) {

        boolean statusOfAll = sedentary.isEnable(); // Selector de control para la función de recordatorio de sedentarismo

        //add list
        List<ATSedentaryItem> sedentarys = new ArrayList<ATSedentaryItem>();
        sedentarys.add(sedentary);

        //calling interface
        ATSedentarySetting setting = new ATSedentarySetting(statusOfAll, sedentarys);
        LSBluetoothManager.getInstance().pushSyncSetting(deviceMac, setting, listener);
    }

    public static List<ATWeekDay> getWeekDays() {
        List<ATWeekDay> weekDays = new ArrayList<ATWeekDay>();//重复周期
        weekDays.add(ATWeekDay.Friday);
        weekDays.add(ATWeekDay.Monday);
        weekDays.add(ATWeekDay.Saturday);
        weekDays.add(ATWeekDay.Sunday);
        weekDays.add(ATWeekDay.Thursday);
        weekDays.add(ATWeekDay.Tuesday);
        weekDays.add(ATWeekDay.Wednesday);
        return weekDays;
    }


    /**
     * Actualización de la información de usuario de las balanzas
     *
     * @param deviceMac
     * @param listener
     * @return
     */
    public static void updateWristBandUserInfo(final String deviceMac, ATUserInfoSetting userInfo,final OnSettingListener listener) {

//        float userHeight = 1.65f;
//        float userWeight = 0f;
//        int userAge = 20;

        LSBluetoothManager.getInstance().pushSyncSetting(deviceMac, userInfo, listener);
    }


    /**
     * Ajuste del tamaño del paso
     */
    public static void strideSetting(final String deviceMac, ATStrideInfo stride,final OnSettingListener listener){

        List<ATConfigItem> configItems = new ArrayList<ATConfigItem>();
        configItems.add(stride);
        ATConfigItemSetting setting = new ATConfigItemSetting(configItems);
        LSBluetoothManager.getInstance().pushSyncSetting(deviceMac, setting, listener);
    }

    /**
     * Configurar la pulsera con la pantallas que queremos mostrar de forma
     *
     * @param deviceMac
     * @param listener
     */
    public static void updateCustomPage(final String deviceMac, final OnSettingListener listener) {

        // Según el orden que se añadan en pages salen en la pulsera
        List<Integer> pages = new ArrayList<Integer>();
        pages.add(ATWatchPage.TodayOverview.getCommand());  // Resumen de hoy
        pages.add(ATWatchPage.HeartRate.getCommand());    // Muestra medición de Heart Rate
        pages.add(ATWatchPage.BloodOxygen.getCommand());  // Muestra medición de Blood Oxygen
        pages.add(ATWatchPage.Setting.getCommand());

        ATScreenPagesSetting setting = new ATScreenPagesSetting(pages);
        LSBluetoothManager.getInstance().pushSyncSetting(deviceMac, setting, listener);

    }

}
