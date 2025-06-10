package com.eviahealth.eviahealth.bluetooth.beurer.bc85;

import java.util.HashMap;
import java.util.UUID;

public class bc85GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();

    public static String CHARACTERISTIC_2A35 = "00002a35-0000-1000-8000-00805f9b34fb";
    public static final UUID UUID_CHARACTERISTIC_2A35 = UUID.fromString(CHARACTERISTIC_2A35);

    public static String DESCRIPTOR_CHARACTERISTIC_01 = "00002902-0000-1000-8000-00805f9b34fb";
    public static final UUID UUID_DESCRIPTOR_CHARACTERISTIC_01 = UUID.fromString(DESCRIPTOR_CHARACTERISTIC_01);

//    public static String DESCRIPTOR_CHARACTERISTIC_02 = "00002902-0000-1000-8000-00805f9b34fb";
//    public static final UUID UUID_DESCRIPTOR_CHARACTERISTIC_02 = UUID.fromString(DESCRIPTOR_CHARACTERISTIC_02);

    public static int LENGTH_BLOOD_PRESSURE_MEASUREMENT = 19;


    static {
        // Services OR Characteristics, Properties
        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "service");      // Service "Generic Access"
        attributes.put("00002a00-0000-1000-8000-00805f9b34fb", "read");         // Device Name
        attributes.put("00002a01-0000-1000-8000-00805f9b34fb", "read");         // Appearance
        attributes.put("00002a02-0000-1000-8000-00805f9b34fb", "read,write");   // Peripheral Privacy Flag
        attributes.put("00002a03-0000-1000-8000-00805f9b34fb", "write");        // Reconnection Address
        attributes.put("00002a04-0000-1000-8000-00805f9b34fb", "read");         // Peripheral Preferred Connection Parameters

        attributes.put("00001801-0000-1000-8000-00805f9b34fb", "service");      // Service "Generic Attribute"
        attributes.put("00002a05-0000-1000-8000-00805f9b34fb", "none");         // "Service Change", "indicate"

        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "service");      // Service "Device information"
        attributes.put("00002a23-0000-1000-8000-00805f9b34fb", "read");         // System ID >> response 0x00 00 00 00 00 00 00 00
        attributes.put("00002a24-0000-1000-8000-00805f9b34fb", "read");         // Model Number String "BC85"
        attributes.put("00002a25-0000-1000-8000-00805f9b34fb", "read");         // Serial Number String "MacAdress"
        attributes.put("00002a26-0000-1000-8000-00805f9b34fb", "read");         // Frimware Revision String "1.2"
        attributes.put("00002a27-0000-1000-8000-00805f9b34fb", "read");         // Hardware Revision String "1.0"
        attributes.put("00002a28-0000-1000-8000-00805f9b34fb", "read");         // Software Revision String "1.1"
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "read");         // Manufacturer Name String "Beurer GmbH"
        attributes.put("00002a2a-0000-1000-8000-00805f9b34fb", "read");         // Manufacturer Name String >> 0x00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        attributes.put("00002a50-0000-1000-8000-00805f9b34fb", "read");         // PnP ID >> 0x00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00

        attributes.put("00001810-0000-1000-8000-00805f9b34fb", "service");      // Service "Blood Pressure"
        attributes.put("00002a35-0000-1000-8000-00805f9b34fb", "indicate");     // Blood Presure Measurement
        attributes.put("00002a36-0000-1000-8000-00805f9b34fb", "none");         // Intermediate Cuff Pressure (not used by BC85) notify
        attributes.put("00002a49-0000-1000-8000-00805f9b34fb", "read");         //  Blood Pressure Feature

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
E/BluetoothLeService: gattService: 00001800-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a00-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a01-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a02-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a03-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a04-0000-1000-8000-00805f9b34fb

E/BluetoothLeService: gattService: 00001801-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a05-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: descriptor: 00002902-0000-1000-8000-00805f9b34fb

E/BluetoothLeService: gattService: 0000180a-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a29-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a24-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a25-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a27-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a26-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a28-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a23-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a2a-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a50-0000-1000-8000-00805f9b34fb

E/BluetoothLeService: gattService: 00001810-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a35-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: descriptor: 00002902-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a36-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: descriptor: 00002902-0000-1000-8000-00805f9b34fb
E/BluetoothLeService: gattCharacteristic: 00002a49-0000-1000-8000-00805f9b34fb
 */