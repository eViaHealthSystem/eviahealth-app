package com.eviahealth.eviahealth.bluetooth.beurer.ft95;

import java.util.HashMap;
import java.util.UUID;

public class ft95GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();

    public static String CHARACTERISTIC_2A1C = "00002a1c-0000-1000-8000-00805f9b34fb";
    public static final UUID UUID_CHARACTERISTIC_2A1C = UUID.fromString(CHARACTERISTIC_2A1C);

    public static String DESCRIPTOR_CHARACTERISTIC_01 = "00002902-0000-1000-8000-00805f9b34fb";
    public static final UUID UUID_DESCRIPTOR_CHARACTERISTIC_01 = UUID.fromString(DESCRIPTOR_CHARACTERISTIC_01);

    // RESPONSE COMMAND
    // este dispositivo no dispone de comandos

    // RESPONSE LENGTH DATA
    public static int LENGTH_RECORD_DATA = 13;

    static {
        // Services OR Characteristics, Properties
        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "service");      // Service "Generic Access"
        attributes.put("00002a00-0000-1000-8000-00805f9b34fb", "read");         // Device Name
        attributes.put("00002a01-0000-1000-8000-00805f9b34fb", "read");         // Appearance
        attributes.put("00002a02-0000-1000-8000-00805f9b34fb", "read,write");   // Peripheral Privacy Flag
        attributes.put("00002a03-0000-1000-8000-00805f9b34fb", "write");        // Reconnection Address
        attributes.put("00002a04-0000-1000-8000-00805f9b34fb", "read");         // Peripheral Preferred (Connection Parameters)

        attributes.put("00001801-0000-1000-8000-00805f9b34fb", "service");      // Service "Generic Attribute"
        attributes.put("00002a05-0000-1000-8000-00805f9b34fb", "none");         // "Service Change", "indicate"

        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "service");      // Service "Device information"
        attributes.put("00002a23-0000-1000-8000-00805f9b34fb", "read");         // System ID
        attributes.put("00002a24-0000-1000-8000-00805f9b34fb", "read");         // MOdel Number String
        attributes.put("00002a25-0000-1000-8000-00805f9b34fb", "read");         // Serial NUmber String
        attributes.put("00002a26-0000-1000-8000-00805f9b34fb", "read");         // Firmware Revision String
        attributes.put("00002a27-0000-1000-8000-00805f9b34fb", "read");         // Hardware Revision String
        attributes.put("00002a28-0000-1000-8000-00805f9b34fb", "read");         // Software Revision String
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "read");         // Manufacturer Name String
        attributes.put("00002a2a-0000-1000-8000-00805f9b34fb", "read");         // IEEE
        attributes.put("00002a50-0000-1000-8000-00805f9b34fb", "read");         // PIP ID

        attributes.put("00001809-0000-1000-8000-00805f9b34fb", "service");      // Service "Health Thermometer"
        attributes.put("00002a1c-0000-1000-8000-00805f9b34fb", "indicate");     // Temperature Measurement

        attributes.put("0000180f-0000-1000-8000-00805f9b34fb", "service");      // Service "Battery Service"
        attributes.put("00002a19-0000-1000-8000-00805f9b34fb", "none");         // Baterry Level (notify,read)

        attributes.put("00002a21-0000-1000-8000-00805f9b34fb", "read");         // Measurement interval
        attributes.put("0000180f-0000-1000-8000-00805f9b34fb-", "none");        //


//        attributes.put("00805f9b34fb", "notify,write");  //
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    public static String getPropierties(String uuid){
        if (attributes.get(uuid) == null ) return "node";
        if (attributes.get(uuid).contains("indicate")){ return "indicate"; }
        if (attributes.get(uuid).contains("notify")){ return "notify"; }
        return "none";
    }
}

/*
gattService: 00001800-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a00-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a01-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a02-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a03-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a04-0000-1000-8000-00805f9b34fb

gattService: 00001801-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a05-0000-1000-8000-00805f9b34fb

descriptor: 00002902-0000-1000-8000-00805f9b34fb

gattService: 0000180a-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a23-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a24-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a25-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a26-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a27-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a28-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a29-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a2a-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a50-0000-1000-8000-00805f9b34fb

gattService: 00001809-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a1c-0000-1000-8000-00805f9b34fb

descriptor: 00002902-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a21-0000-1000-8000-00805f9b34fb

descriptor: 00002906-0000-1000-8000-00805f9b34fb

gattService: 0000180f-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 00002a19-0000-1000-8000-00805f9b34fb
descriptor: 00002902-0000-1000-8000-00805f9b34fb
 */