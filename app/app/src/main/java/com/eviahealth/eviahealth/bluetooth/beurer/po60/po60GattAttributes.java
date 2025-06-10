package com.eviahealth.eviahealth.bluetooth.beurer.po60;

import java.util.HashMap;
import java.util.UUID;

public class po60GattAttributes {

    private static HashMap<String, String> attributes = new HashMap();

    public static String CHARACTERISTIC_FF01 = "0000ff01-0000-1000-8000-00805f9b34fb";
    public static final UUID UUID_CHARACTERISTIC_FF01 = UUID.fromString(CHARACTERISTIC_FF01);

    public static String CHARACTERISTIC_FF02 = "0000ff02-0000-1000-8000-00805f9b34fb";
    public static final UUID UUID_CHARACTERISTIC_FF02 = UUID.fromString(CHARACTERISTIC_FF02);

    public static String DESCRIPTOR_CHARACTERISTIC_01 = "00002901-0000-1000-8000-00805f9b34fb";
    public static final UUID UUID_DESCRIPTOR_CHARACTERISTIC_01 = UUID.fromString(DESCRIPTOR_CHARACTERISTIC_01);

    public static String DESCRIPTOR_CHARACTERISTIC_02 = "00002902-0000-1000-8000-00805f9b34fb";
    public static final UUID UUID_DESCRIPTOR_CHARACTERISTIC_02 = UUID.fromString(DESCRIPTOR_CHARACTERISTIC_02);

    //region :: COMMANDS
    public static byte[] CMD_GET_DEVICE_VERSION = new byte[]{(byte) 0x82, (byte) 0x02};
    public static byte[] CMD_GET_TIME= new byte[]{(byte) 0x89, (byte) 0x09};
    public static byte CMD_SET_TIME = (byte) 0x83;
    public static byte[] CMD_SET_TYPE_STORAGE_DATA = new byte[]{(byte) 0x90, (byte) 0x05, (byte) 0x15};

    public static byte[] CMD_GET_START_DOWNLOAD_DATA = new byte[]{(byte) 0x99, (byte) 0x00, (byte) 0x19};
    public static byte[] CMD_GET_CONTINUE_DOWNLOAD_DATA = new byte[]{(byte) 0x99, (byte) 0x01, (byte) 0x1a};
    public static byte[] CMD_GET_RESEND_DOWNLOAD_DATA = new byte[]{(byte) 0x99, (byte) 0x02, (byte) 0x1b};
    public static byte[] CMD_SET_SAVE_RECORD_DATA = new byte[]{(byte) 0x99, (byte) 0x7e, (byte) 0x17};
    public static byte[] CMD_SET_DELETE_DATA_STORAGE = new byte[]{(byte) 0x99, (byte) 0x7f, (byte) 0x18};

    //endregion

    //region :: RESPONSE COMMAND
    public static byte RESPONSE_CMD_GET_DEVICE_VERSION  = (byte)0xf2;
    public static byte RESPONSE_CMD_SET_TIME  = (byte)0xf3;
    public static byte RESPONSE_CMD_GET_TIME  = (byte)0xf9;
    public static byte RESPONSE_SET_TYPE_STORAGE_DATA  = (byte)0xe0;
    public static byte RESPONSE_DOWNLOAD_DATA  = (byte)0xe9;

    //endregion

    //region :: RESPONSE COMMAND LENGTH
    public static int LENGTH_DEVICE_VERSION  = 8;
    public static int LENGTH_SET_TIME  = 3;
    public static int LENGTH_GET_TIME  = 10;
    public static int LENGTH_SET_TYPE_STORAGE_DATA = 7;
    public static int LENGTH_RECORD_DATA = 24;
//    public static int LENGTH_DOWNLOAD_DATA = 24 * 10; // 23 BYTES por registros x 10 registros por petición
    //endregion


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

        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "service");      // Service "Custom service PO60Device information
        attributes.put("00002a23-0000-1000-8000-00805f9b34fb", "read");         // System ID >> response 0xF6 8A 1C 00 00 B8 5F D0
        attributes.put("00002a24-0000-1000-8000-00805f9b34fb", "read");         // Model Number String
        attributes.put("00002a25-0000-1000-8000-00805f9b34fb", "read");         // Serial Number String
        attributes.put("00002a26-0000-1000-8000-00805f9b34fb", "read");         // Frimware Revision String
        attributes.put("00002a27-0000-1000-8000-00805f9b34fb", "read");         // Hardware Revision String
        attributes.put("00002a28-0000-1000-8000-00805f9b34fb", "read");         // Software Revision String
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "read");         // Manufacturer Name String "Manufacturer Name"
        attributes.put("00002a50-0000-1000-8000-00805f9b34fb", "read");         // PnP ID >> response 0x00 0D

        attributes.put("0000ff12-0000-1000-8000-00805f9b34fb", "service");      // Service "Custom service PO60"
        attributes.put("0000ff01-0000-1000-8000-00805f9b34fb", "write");        // Unknown characteristic "Send write commands to this characteristic (Data in)" write (no resp.)
        attributes.put("0000ff02-0000-1000-8000-00805f9b34fb", "notify");       // Unknown characteristic "Provides the requested data via notifications (Data out)"

        attributes.put("0000ff00-0000-1000-8000-00805f9b34fb", "service");      // ?? NO sale en la documentacion
        attributes.put("0000ff03-0000-1000-8000-00805f9b34fb", "read,write");   // Unknown characteristic "Baud rate"


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
    gattCharacteristic: 00002a50-0000-1000-8000-00805f9b34fb
gattService: 0000ff12-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 0000ff01-0000-1000-8000-00805f9b34fb
        descriptor: 00002902-0000-1000-8000-00805f9b34fb
        descriptor: 00002901-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 0000ff02-0000-1000-8000-00805f9b34fb
        descriptor: 00002902-0000-1000-8000-00805f9b34fb
        descriptor: 00002901-0000-1000-8000-00805f9b34fb
gattService: 0000ff00-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 0000ff03-0000-1000-8000-00805f9b34fb
        descriptor: 00002902-0000-1000-8000-00805f9b34fb
        descriptor: 00002901-0000-1000-8000-00805f9b34fb
 */