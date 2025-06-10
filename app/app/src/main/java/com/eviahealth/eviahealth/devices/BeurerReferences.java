package com.eviahealth.eviahealth.devices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BeurerReferences {

    // Listado de los dispositivos aceptados
    public static List<String> device = new ArrayList<>(Arrays.asList("PO60","BC54","BC85","BC87","BM57","FT95","BF600"));

    // dispositivos que NECESITAN EMPAREJAR -------------------------------------------------------
    public static List<String> deviceOXI = new ArrayList<>(Arrays.asList("PO60"));
    public static List<String> deviceTEN = new ArrayList<>(Arrays.asList("BC54","BC85","BC87"));
    public static List<String> deviceACT = new ArrayList<>();
    public static List<String> deviceTHERMO = new ArrayList<>(Arrays.asList("FT95"));
    public static List<String> deviceSCALE = new ArrayList<>(Arrays.asList("BF600"));

    public static List<String> deviceMONPUL = new ArrayList<>();
    public static List<String> deviceECG = new ArrayList<>();
    // --------------------------------------------------------------------------------------------

    // dispositivos que necesitan configuración inicial -------------------------------------------
    public static List<String> deviceOXI_CNF = new ArrayList<>();
    public static List<String> deviceTEN_CNF = new ArrayList<>();
    public static List<String> deviceACT_CNF = new ArrayList<>();
    public static List<String> deviceTHERMO_CNF = new ArrayList<>(Arrays.asList("FT95"));
    public static List<String> deviceBAS_CNF = new ArrayList<>(Arrays.asList("BF600"));
    public static List<String> deviceMONPUL_CNF = new ArrayList<>();
    public static List<String> deviceECG_CNF = new ArrayList<>();
    // --------------------------------------------------------------------------------------------

    public final static String ACTION_PROCESS_ACTIVITY = "EV.ACTION_PROCESS_ACTIVITY";
    public final static String ACTION_BEURER_CONNECTED = "BEURER.ACTION_CONNECTED";
    public final static String ACTION_BEURER_DISCONNECTED = "BEURER.ACTION_DISCONNECTED";
    public final static String ACTION_BEURER_GET_OTHER = "BEURER.ACTION_BEURER_GET_OTHER";
    public final static String ACTION_BEURER_CMD_FAIL = "BEURER.ACTION_BEURER_CMD_FAIL";
    public final static String ACTION_BEURER_COMMUNICATION_TIMEOUT = "BEURER.ACTION_BEURER_COMMUNICATION_TIMEOUT";
    public final static String ACTION_BEURER_WRITE_FAIL = "BEURER.ACTION_BEURER_WRITE_FAIL";

    //region :: PO60
    public final static String ACTION_BEURER_PO60_DEVICE_VERSION = "BEURER.ACTION_BEURER_PO60_DEVICE_VERSION";
    public final static String ACTION_BEURER_PO60_UPDATE_TIME = "BEURER.ACTION_BEURER_PO60_UPDATE_TIME";
    public final static String ACTION_BEURER_PO60_RECORDS_STORAGE = "BEURER.ACTION_BEURER_PO60_RECORDS_STORAGE";
    public final static String ACTION_BEURER_PO60_DOWNLOAD_DATA = "BEURER.ACTION_BEURER_PO60_DOWNLOAD_DATA";
    public final static String ACTION_BEURER_PO60_DOWNLOAD_DATA_FINISHED = "BEURER.ACTION_BEURER_PO60_DOWNLOAD_DATA_FINISHED";
    public final static String ACTION_BEURER_PO60_DELETE_DATA_STORAGE = "BEURER.ACTION_BEURER_PO60_DELETE_DATA_STORAGE";
    //endregion

    //region :: BC54/BC85/BC87/BM57
    public final static String ACTION_BEURER_BC54_DOWNLOAD_DATA = "BEURER.ACTION_BEURER_BC54_DOWNLOAD_DATA";
    public final static String ACTION_BEURER_BC54_DOWNLOAD_DATA_FINISHED = "BEURER.ACTION_BEURER_BC54_DOWNLOAD_DATA_FINISHED";
    public final static String ACTION_BEURER_BC54_MEASUREMENT_ERROR = "BEURER.ACTION_BEURER_BC54_MEASUREMENT_ERROR";
    //endregion

    //region :: FT95
    public final static String ACTION_BEURER_FT95_DOWNLOAD_DATA = "BEURER.ACTION_BEURER_FT95_DOWNLOAD_DATA";
    public final static String ACTION_BEURER_FT95_DOWNLOAD_DATA_FINISHED = "BEURER.ACTION_BEURER_FT95_DOWNLOAD_DATA_FINISHED";
    //endregion

    //region :: BF600
    public final static String ACTION_BEURER_BF600_UPDATE_TIME = "BEURER.ACTION_BEURER_BF600_UPDATE_TIME";
    public final static String ACTION_BEURER_BF600_UPDATE_USER = "BEURER.ACTION_BEURER_BF600_GET_UPDATE_USER";
    public final static String ACTION_BEURER_BF600_USER_CONTROL_POINT = "BEURER.ACTION_BEURER_BF600_USER_CONTROL_POINT";
    public final static String ACTION_BEURER_BF600_WEIGHT_MEASUREMENT = "BEURER.ACTION_BEURER_BF600_WEIGHT_MEASUREMENT";
    public final static String ACTION_BEURER_BF600_BODY_COMPOSITION = "BEURER.ACTION_BEURER_BF600_BODY_COMPOSITION";
    public final static String ACTION_BEURER_BF600_TAKE_MEASUREMENT = "BEURER.ACTION_BEURER_BF600_TAKE_MEASUREMENT";
    public final static String ACTION_BEURER_BF600_USER_LIST = "BEURER.ACTION_BEURER_BF600_USER_LIST";
    public final static String ACTION_BEURER_BF600_BATTERY_LEVEL = "BEURER.ACTION_BEURER_BF600_BATTERY_LEVEL";

    //endregion





    // Constantes para intens
    public final static String ACTION_EXTRA_MESSAGE = "ACTION_EXTRA_MESSAGE";
    public final static String ACTION_EXTRA_DATA= "ACTION_EXTRA_DATA";
    public final static String ACTION_EXTRA_STATUS = "ACTION_EXTRA_STATUS";

    public final static int SLEEP_DEEP = 2;
    public final static int SLEEP_LIGHT = 1;
    public final static int SLEEP_AWAKE = 0;

}
