package com.eviahealth.eviahealth.bluetooth.beurer.bf600;

import java.util.HashMap;
import java.util.UUID;

public class bf600GattAttributes {

    private static int pin = 1234;
    private static HashMap<String, String> attributes = new HashMap();

    public static String CHARACTERISTIC_2902 = "00002902-0000-1000-8000-00805f9b34fb";
    public static final UUID UUID_CHARACTERISTIC_2902 = UUID.fromString(CHARACTERISTIC_2902);

    // UUID Servicios
    public static final UUID GENERIC_ACCESS_SERVICE_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    public static final UUID GENERIC_ATTRIBUTE_SERVICE_UUID = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
    public static final UUID CURRENT_TIME_SERVICE_UUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
    public static final UUID USER_DATA_SERVICE_UUID = UUID.fromString("0000181c-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_LEVEL_SERVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    public static final UUID CUSTOM_SCALE_SETTINGS_SERVICE_UUID = UUID.fromString("000fff0-0000-1000-8000-00805f9b34fb");


    // UUIDs de las características de los servicios
    public static final UUID DEVICE_NAME_CHARACTERISTIC_UUID = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
    public static final UUID SERVICE_CHANGE_CHARACTERISTIC_UUID = UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb");
    public static final UUID CURRENT_TIME_CHARACTERISTIC_UUID = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb");
    public static final UUID USER_CONTROL_POINT_CHARACTERISTIC_UUID = UUID.fromString("00002a9f-0000-1000-8000-00805f9b34fb");

    public static final UUID DATE_OF_BIRTH_USER_CHARACTERISTIC_UUID = UUID.fromString("00002a85-0000-1000-8000-00805f9b34fb");
    public static final UUID GENDER_USER_CHARACTERISTIC_UUID = UUID.fromString("00002a8c-0000-1000-8000-00805f9b34fb");
    public static final UUID HEIGHT_USER_CHARACTERISTIC_UUID = UUID.fromString("00002a8e-0000-1000-8000-00805f9b34fb");
    public static final UUID ACTIVITY_LEVEL_USER_CHARACTERISTIC_UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb");
    public static final UUID DATABASE_CHANGE_INCREMENT_USER_CHARACTERISTIC_UUID = UUID.fromString("00002a99-0000-1000-8000-00805f9b34fb");
    public static final UUID USER_INDEX_CHARACTERISTIC_UUID = UUID.fromString("00002a9a-0000-1000-8000-00805f9b34fb");
    public static final UUID SCALE_SETTINGS_CHARACTERISTIC_UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");

    public static final UUID WEIGHT_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002a9d-0000-1000-8000-00805f9b34fb");
    public static final UUID BODY_COMPOSITION_CHARACTERISTIC_UUID = UUID.fromString("00002a9c-0000-1000-8000-00805f9b34fb");
    public static final UUID TAKE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb");
    public static final UUID USER_LIST_CHARACTERISTIC_UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");


    // Services OR Characteristics, Properties
    static {

        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "service");      // Generic Access
        attributes.put("00002a00-0000-1000-8000-00805f9b34fb", "read");         // Device Name
        attributes.put("00002a01-0000-1000-8000-00805f9b34fb", "read");         // Appearance
        attributes.put("00002a03-0000-1000-8000-00805f9b34fb", "write");        // Reconnection Address
        attributes.put("00002a02-0000-1000-8000-00805f9b34fb", "read");         // Peripheral Privacy Flag
        attributes.put("00002a04-0000-1000-8000-00805f9b34fb", "read");         // Peripheral Preferred Connection Parameters

        attributes.put("00001801-0000-1000-8000-00805f9b34fb", "service");          // Generic Attribute
        attributes.put("00002a05-0000-1000-8000-00805f9b34fb", "read,indicate");    // Service Change

        attributes.put("00002902-0000-1000-8000-00805f9b34fb", "descriptor");       // descriptor

        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "service");          // Device Information
        attributes.put("00002a25-0000-1000-8000-00805f9b34fb", "read");             // Serial Number String

        attributes.put("00002904-0000-1000-8000-00805f9b34fb", "descriptor");       // descriptor
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "read");             // Manufacturer Name String

//        attributes.put("00002904-0000-1000-8000-00805f9b34fb", "none");           // descriptor
        attributes.put("00002a23-0000-1000-8000-00805f9b34fb", "read");             // System ID
        attributes.put("00002a26-0000-1000-8000-00805f9b34fb", "read");             // Firmware Revision String

//        attributes.put("00002904-0000-1000-8000-00805f9b34fb", "none");           // descriptor
        attributes.put("00002a24-0000-1000-8000-00805f9b34fb", "read");             // Model Number String

//        attributes.put("00002904-0000-1000-8000-00805f9b34fb", "none");           // descriptor
        attributes.put("00002a27-0000-1000-8000-00805f9b34fb", "read");             // Hardware Revision String

//        attributes.put("00002904-0000-1000-8000-00805f9b34fb", "none");           // descriptor
        attributes.put("00002a28-0000-1000-8000-00805f9b34fb", "read");             // Software Revision String

//        attributes.put("00002904-0000-1000-8000-00805f9b34fb", "none");           // descriptor
        attributes.put("00002a50-0000-1000-8000-00805f9b34fb", "read");             // PnP ID
        attributes.put("00002a2a-0000-1000-8000-00805f9b34fb", "read");             // IEEE 11073-20601 Regulatory Certification Data List

        attributes.put("0000feba-0000-1000-8000-00805f9b34fb", "service");          // Custom service, Este servicio se utilizó para las actualizaciones de firmware durante el desarrollo y no es necesario para la comunicación con la báscula
        attributes.put("0000fa10-0000-1000-8000-00805f9b34fb", "notify,write");     // Custom characteristic

//        attributes.put("00002902-0000-1000-8000-00805f9b34fb", "none");             // descriptor
        attributes.put("00002901-0000-1000-8000-00805f9b34fb", "descriptor");         // descriptor
        attributes.put("0000fa11-0000-1000-8000-00805f9b34fb", "indicate,write");     // Custom characteristic

//        attributes.put("00002902-0000-1000-8000-00805f9b34fb", "none");           // descriptor
//        attributes.put("00002901-0000-1000-8000-00805f9b34fb", "none");           // descriptor
        attributes.put("0000fa13-0000-1000-8000-00805f9b34fb", "notify");           // Custom characteristic
//        attributes.put("00002902-0000-1000-8000-00805f9b34fb", "none");           // descriptor
//        attributes.put("00002901-0000-1000-8000-00805f9b34fb", "none");           // descriptor
        attributes.put("0000180f-0000-1000-8000-00805f9b34fb", "service");          // Battery Service
        attributes.put("00002a19-0000-1000-8000-00805f9b34fb", "notify,read");      // Battery Level
//        attributes.put("00002902-0000-1000-8000-00805f9b34fb", "none");           // descriptor
        attributes.put("0000fff0-0000-1000-8000-00805f9b34fb", "service");          // Custom service: Este servicio se utiliza para obtener y configurar ajustes e información de la báscula que no están cubiertos por los servicios Servicios Bluetooth SIG de báscula, composición corporal y datos de usuario
        attributes.put("0000fff1-0000-1000-8000-00805f9b34fb", "read,write");       // Custom characteristic: Scale settings
        attributes.put("0000fff2-0000-1000-8000-00805f9b34fb", "notify,write");     // Custom characteristic: User List
//        attributes.put("00002902-0000-1000-8000-00805f9b34fb", "none");           // descriptor
        attributes.put("0000fff3-0000-1000-8000-00805f9b34fb", "read,write");       // Custom characteristic: Activity level
        attributes.put("0000fff4-0000-1000-8000-00805f9b34fb", "notify,write");     // Custom characteristic: Take measurement
//        attributes.put("00002902-0000-1000-8000-00805f9b34fb", "none");           // descriptor
        attributes.put("0000fff5-0000-1000-8000-00805f9b34fb", "read");             // Custom characteristic: Reference Weight

        attributes.put("0000181b-0000-1000-8000-00805f9b34fb", "service");          // Body Composition Service
        attributes.put("00002a9b-0000-1000-8000-00805f9b34fb", "read");             // Body Composition Feature
        attributes.put("00002a9c-0000-1000-8000-00805f9b34fb", "indicate");         // Body Composition Measurement

//        attributes.put("00002902-0000-1000-8000-00805f9b34fb", "none");           // descriptor
        attributes.put("0000181d-0000-1000-8000-00805f9b34fb", "service");          // Weight Scale Service
        attributes.put("00002a9e-0000-1000-8000-00805f9b34fb", "read");             // Weight Scale Feature
        attributes.put("00002a9d-0000-1000-8000-00805f9b34fb", "indicate");         // Weight Scale Measurement

//        attributes.put("00002902-0000-1000-8000-00805f9b34fb", "none");           // descriptor
        attributes.put("00001805-0000-1000-8000-00805f9b34fb", "service");          // Current Time Service
        attributes.put("00002a2b-0000-1000-8000-00805f9b34fb", "notify,write,read");  // Current Time
//        attributes.put("00002902-0000-1000-8000-00805f9b34fb", "none");           // descriptor
        attributes.put("00002a0f-0000-1000-8000-00805f9b34fb", "read");             // Local Time Information
        attributes.put("00002a14-0000-1000-8000-00805f9b34fb", "read");             // Reference Time Information

        attributes.put("0000181c-0000-1000-8000-00805f9b34fb", "service");          // User data
        attributes.put("00002a85-0000-1000-8000-00805f9b34fb", "read,write");       // Date of Birth
        attributes.put("00002a8c-0000-1000-8000-00805f9b34fb", "read,write");       // Gender
        attributes.put("00002a8e-0000-1000-8000-00805f9b34fb", "read,write");       // Height
        attributes.put("00002a99-0000-1000-8000-00805f9b34fb", "notify,write,read");  // Database Change increment
//        attributes.put("00002902-0000-1000-8000-00805f9b34fb", "none");           // descriptor
        attributes.put("00002a9a-0000-1000-8000-00805f9b34fb", "read");             // User Index
        attributes.put("00002a9f-0000-1000-8000-00805f9b34fb", "indicate,write");   //User Control Point
//        attributes.put("00002902-0000-1000-8000-00805f9b34fb", "none");           // descriptor

//        attributes.put("00805f9b34fb", "notify,write");  //

    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    public static String getPropierties(String uuid){
        if (attributes.get(uuid) == null ) return "none";
        if (attributes.get(uuid).contains("indicate")){ return "indicate"; }
        if (attributes.get(uuid).contains("notify")){ return "notify"; }
        return "none";
    }
}

/*
gattService: 00001800-0000-1000-8000-00805f9b34fb
	gattCharacteristic: 00002a00-0000-1000-8000-00805f9b34fb
	gattCharacteristic: 00002a01-0000-1000-8000-00805f9b34fb
	gattCharacteristic: 00002a03-0000-1000-8000-00805f9b34fb
	gattCharacteristic: 00002a02-0000-1000-8000-00805f9b34fb
	gattCharacteristic: 00002a04-0000-1000-8000-00805f9b34fb

gattService: 00001801-0000-1000-8000-00805f9b34fb
	gattCharacteristic: 00002a05-0000-1000-8000-00805f9b34fb

descriptor: 00002902-0000-1000-8000-00805f9b34fb
gattService: 0000180a-0000-1000-8000-00805f9b34fb
	gattCharacteristic: 00002a25-0000-1000-8000-00805f9b34fb

descriptor: 00002904-0000-1000-8000-00805f9b34fb
	gattCharacteristic: 00002a29-0000-1000-8000-00805f9b34fb

descriptor: 00002904-0000-1000-8000-00805f9b34fb
	gattCharacteristic: 00002a23-0000-1000-8000-00805f9b34fb
	gattCharacteristic: 00002a26-0000-1000-8000-00805f9b34fb

descriptor: 00002904-0000-1000-8000-00805f9b34fb
	gattCharacteristic: 00002a24-0000-1000-8000-00805f9b34fb

descriptor: 00002904-0000-1000-8000-00805f9b34fb
	gattCharacteristic: 00002a27-0000-1000-8000-00805f9b34fb

descriptor: 00002904-0000-1000-8000-00805f9b34fb
	gattCharacteristic: 00002a28-0000-1000-8000-00805f9b34fb

descriptor: 00002904-0000-1000-8000-00805f9b34fb
	gattCharacteristic: 00002a50-0000-1000-8000-00805f9b34fb
	gattCharacteristic: 00002a2a-0000-1000-8000-00805f9b34fb

gattService: 0000feba-0000-1000-8000-00805f9b34fb
    gattCharacteristic: 0000fa10-0000-1000-8000-00805f9b34fb

descriptor: 00002902-0000-1000-8000-00805f9b34fb
descriptor: 00002901-0000-1000-8000-00805f9b34fb
	gattCharacteristic: 0000fa11-0000-1000-8000-00805f9b34fb

descriptor: 00002902-0000-1000-8000-00805f9b34fb
descriptor: 00002901-0000-1000-8000-00805f9b34fb
	gattCharacteristic: 0000fa13-0000-1000-8000-00805f9b34fb
descriptor: 00002902-0000-1000-8000-00805f9b34fb
descriptor: 00002901-0000-1000-8000-00805f9b34fb
	gattService: 0000180f-0000-1000-8000-00805f9b34fb
		gattCharacteristic: 00002a19-0000-1000-8000-00805f9b34fb
descriptor: 00002902-0000-1000-8000-00805f9b34fb
	gattService: 0000fff0-0000-1000-8000-00805f9b34fb
		gattCharacteristic: 0000fff1-0000-1000-8000-00805f9b34fb
		gattCharacteristic: 0000fff2-0000-1000-8000-00805f9b34fb

descriptor: 00002902-0000-1000-8000-00805f9b34fb
gattCharacteristic: 0000fff3-0000-1000-8000-00805f9b34fb
gattCharacteristic: 0000fff4-0000-1000-8000-00805f9b34fb

descriptor: 00002902-0000-1000-8000-00805f9b34fb
gattCharacteristic: 0000fff5-0000-1000-8000-00805f9b34fb
gattService: 0000181b-0000-1000-8000-00805f9b34fb
gattCharacteristic: 00002a9b-0000-1000-8000-00805f9b34fb
gattCharacteristic: 00002a9c-0000-1000-8000-00805f9b34fb

descriptor: 00002902-0000-1000-8000-00805f9b34fb
gattService: 0000181d-0000-1000-8000-00805f9b34fb
gattCharacteristic: 00002a9e-0000-1000-8000-00805f9b34fb
gattCharacteristic: 00002a9d-0000-1000-8000-00805f9b34fb

descriptor: 00002902-0000-1000-8000-00805f9b34fb
gattService: 00001805-0000-1000-8000-00805f9b34fb
gattCharacteristic: 00002a2b-0000-1000-8000-00805f9b34fb

descriptor: 00002902-0000-1000-8000-00805f9b34fb
gattCharacteristic: 00002a0f-0000-1000-8000-00805f9b34fb
gattCharacteristic: 00002a14-0000-1000-8000-00805f9b34fb
gattService: 0000181c-0000-1000-8000-00805f9b34fb
gattCharacteristic: 00002a85-0000-1000-8000-00805f9b34fb
gattCharacteristic: 00002a8c-0000-1000-8000-00805f9b34fb
gattCharacteristic: 00002a8e-0000-1000-8000-00805f9b34fb
gattCharacteristic: 00002a99-0000-1000-8000-00805f9b34fb

descriptor: 00002902-0000-1000-8000-00805f9b34fb
gattCharacteristic: 00002a9a-0000-1000-8000-00805f9b34fb
gattCharacteristic: 00002a9f-0000-1000-8000-00805f9b34fb

descriptor: 00002902-0000-1000-8000-00805f9b34fb

 */