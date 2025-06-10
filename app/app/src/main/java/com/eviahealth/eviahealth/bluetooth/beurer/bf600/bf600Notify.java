package com.eviahealth.eviahealth.bluetooth.beurer.bf600;

import java.util.HashMap;
import java.util.UUID;

public class bf600Notify {

    private static HashMap<String, String> attributes = new HashMap();

    // Characteristics, Description
    static {
        attributes.put("00002a05-0000-1000-8000-00805f9b34fb", "00002902-0000-1000-8000-00805f9b34fb");     // Indicate Service Changed
        attributes.put("0000fa10-0000-1000-8000-00805f9b34fb", "00002902-0000-1000-8000-00805f9b34fb");     // Notify Custom Cgarecteristic
        attributes.put("0000fa11-0000-1000-8000-00805f9b34fb", "00002902-0000-1000-8000-00805f9b34fb");     // Indicate
        attributes.put("0000fa13-0000-1000-8000-00805f9b34fb", "00002902-0000-1000-8000-00805f9b34fb");     // Notify

        attributes.put("00002a19-0000-1000-8000-00805f9b34fb", "00002902-0000-1000-8000-00805f9b34fb");     // Notify Battery Level

        attributes.put("0000fff2-0000-1000-8000-00805f9b34fb", "00002902-0000-1000-8000-00805f9b34fb");     // Notify User List
        attributes.put("0000fff4-0000-1000-8000-00805f9b34fb", "00002902-0000-1000-8000-00805f9b34fb");     // Notify Take measurement

        attributes.put("00002a9c-0000-1000-8000-00805f9b34fb", "00002902-0000-1000-8000-00805f9b34fb");     // Indicate Body Composition Measurement

        attributes.put("00002a9d-0000-1000-8000-00805f9b34fb", "00002902-0000-1000-8000-00805f9b34fb");     // Indicate Weight Scale Measurement

        attributes.put("00002a2b-0000-1000-8000-00805f9b34fb", "00002902-0000-1000-8000-00805f9b34fb");     // Notify Current Time

        attributes.put("00002a99-0000-1000-8000-00805f9b34fb", "00002902-0000-1000-8000-00805f9b34fb");     // Notify Database Change increment
        attributes.put("00002a9f-0000-1000-8000-00805f9b34fb", "00002902-0000-1000-8000-00805f9b34fb");     // Indicate User Control Point

//        attributes.put("Characteristics", "Description");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    public static UUID getDescription(String uuid){
        if (attributes.get(uuid) == null ) return null;
        return UUID.fromString(attributes.get(uuid));
    }
}
