package com.eviahealth.eviahealth.bluetooth.models;

import java.util.HashMap;

public class Global {
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public static final int REQUEST_NOT_SUCCESS= 0;
    public static final int STATUS_SUCCESS = 1;

    public static HashMap<Integer, String> connectStatus = new HashMap();
    static {
        // Services OR Characteristics, Properties
        connectStatus.put(0, "STATE_DISCONNECTED");      // Service "Generic Access"
        connectStatus.put(1, "STATE_CONNECTING");         //"Device Name"
        connectStatus.put(2, "STATE_CONNECTED");         //"Appearance"
    }

    public static Integer getKey(String value){
        Integer key = null;
        for (Integer i : connectStatus.keySet()) {
            if (connectStatus.get(i).equals(value)) key = i;
        }
        return key;
    }

    public static final int STATE_DISABLE= 0;
    public static final int STATE_ENABLE = 1;
    public static final int STATE_RELOAD = 2;
}
