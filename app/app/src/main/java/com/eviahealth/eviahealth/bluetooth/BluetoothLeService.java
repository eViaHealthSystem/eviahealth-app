package com.eviahealth.eviahealth.bluetooth;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private final static Boolean logViewService = false; // Muestra las uuid de servicie, characteristic and descriptor en log.e

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    private BluetoothGattService mServerService;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private byte[] buffer;
    private int limitBytes;

    protected int retries;
    private int reintentos;
    private String mDeviceAddress;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "EV.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "EV.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "EV.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_ENABLE_INDICATE_NOTIFY = "EV.ACTION_ENABLE_INDICATE_NOTIFY";
    public final static String ACTION_WRITE_CHARACTERISTIC = "EV.ACTION_WRITE_CHARACTERISTIC";
    public final static String ACTION_CHARACTERISTIC_CHANGED = "EV.ACTION_CHARACTERISTIC_CHANGED";
    public final static String ACTION_DATA = "EV.ACTION_DATA";
    public final static String ACTION_DATA_AVAILABLE_READ = "EV.ACTION_DATA_AVAILABLE_READ";
    public final static String ACTION_WRITE_FAIL = "EV.ACTION_WRITE_FAIL";

    public final static String EXTRA_DATA = "EV.EXTRA_DATA";
    public final static String EXTRA_DATA_BYTES = "EV.EXTRA_DATA_BYTES";
    public final static String EXTRA_DATA_BYTES_WRITE = "EV.EXTRA_DATA_BYTES";
    public final static String EXTRA_UUID = "EV.EXTRA_UUID";

    private static final long SCAN_PERIOD = 10000;  // Stops scanning after 10 seconds.
    private List<BluetoothGattCharacteristic> listCharacteristic = new ArrayList<>();           // listado de todas las characteristics
    private List<BluetoothGattCharacteristic> listCharacteristicDescriptor = new ArrayList<>(); // listado de todas las characteristics con descriptor

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    mConnectionState = STATE_CONNECTED;
//                    broadcastUpdate(ACTION_GATT_CONNECTED);
                    Log.e(TAG, "Connected to GATT server.");
                    Log.e(TAG, "Detección de servicios:" + mBluetoothGatt.discoverServices());
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    mConnectionState = STATE_DISCONNECTED;
                    Log.e(TAG, "Disconnected from GATT server.");
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);
                    break;

                case BluetoothProfile.STATE_CONNECTING:
                    EVLog.log(TAG,"gattCallback >> STATE_CONNECTING");
                    break;

                case BluetoothProfile.STATE_DISCONNECTING:
                    EVLog.log(TAG,"gattCallback >> STATE_DISCONNECTING");
                    break;

                default:
                    EVLog.log(TAG,"gattCallback >> STATE_OTHER Status: " + status);
                    break;
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.e(TAG, "onServicesDiscovered() Status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {

                if(gatt.getServices().size()<4 && retries<5) {
                    retries++;
                    Log.e(TAG,"found only "+gatt.getServices().size()+" services, retrying discoverServices...");
                    try {Thread.sleep(50);} catch(InterruptedException ioe){}
                    gatt.discoverServices();
                    return;
                }

                // Detectados servicios, obtenemos un listado de los GattCharacteristic()
                listCharacteristic.clear();                 // listado de todas las characteristics
                listCharacteristicDescriptor.clear();       // listado de todas las characteristics con descriptor
                listCharacteristic = getBluetoothGattCharacteristic();

                broadcastUpdate(ACTION_GATT_CONNECTED);
//                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.e(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.e(TAG, "onCharacteristicRead() Status: " + status);
            byte[] messageBytes = characteristic.getValue();
            Log.e(TAG,"onCharacteristicChanged Read: " + util.byteArrayInHexFormat(messageBytes));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE_READ, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
//            byte[] messageBytes = characteristic.getValue();
//            Log.e(TAG, "onCharacteristicChanged(): " + characteristic.getUuid().toString() + StringUtils.byteArrayInHexFormat(messageBytes));
            Log.e(TAG, "onCharacteristicChanged(): " + characteristic.getUuid().toString());
            broadcastUpdate(ACTION_CHARACTERISTIC_CHANGED, characteristic);
        }

        // LEVEL API 33
//        @Override
//        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] values) {
//            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
//        }

        /*
        status = 0 >> A GATT operation completed successfully
        status = 3 >> GATT write operation is not permitted
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            Log.e(TAG, "onCharacteristicWrite() Status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdateWrite(ACTION_WRITE_CHARACTERISTIC, characteristic);
            } else {
                Log.e(TAG, "Status: GATT write operation is not permitted");
                broadcastUpdate(ACTION_WRITE_FAIL);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.e(TAG, "onDescriptorWrite() Status: " + status);
            Log.e(TAG,"Read value descriptor: " + util.byteArrayInHexFormat(descriptor.getValue()));
            broadcastUpdate(ACTION_ENABLE_INDICATE_NOTIFY);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.e(TAG, "onReliableWriteCompleted() Status: " + status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
            Log.e(TAG, "onDescriptorRead() Status: " + status);
        }

    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
//        sendBroadcast(intent);
        sendOrderedBroadcast(intent,null);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA_BYTES,characteristic.getValue());
        intent.putExtra(EXTRA_UUID,characteristic.getUuid().toString());

//        Log.e(TAG, "DATA: " +StringUtils.byteArrayInHexFormat(characteristic.getValue()));
//        sendBroadcast(intent);
        sendOrderedBroadcast(intent,null);
    }

    private void broadcastUpdateWrite(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA_BYTES_WRITE,characteristic.getValue());
        intent.putExtra(EXTRA_UUID,characteristic.getUuid().toString());

//        sendBroadcast(intent);
        sendOrderedBroadcast(intent,null);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        reintentos = 0;
        retries = 0;
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    @SuppressLint("MissingPermission")
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            EVLog.log(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.e(TAG, "Trying to use an existing mBluetoothGatt for connection.");

            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            EVLog.log(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback,BluetoothDevice.TRANSPORT_LE);
        Log.e(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            EVLog.log(TAG, "BluetoothAdapter not initialized");
            return;
        }
        EVLog.log(TAG, "mBluetoothGatt.disconnect()");
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    @SuppressLint("MissingPermission")
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    @SuppressLint("MissingPermission")
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }

//        Log.e(TAG,"readCharacteristic()");
        mBluetoothGatt.setCharacteristicNotification(characteristic,true);
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    @SuppressLint("MissingPermission")
    public void setWriteCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
        BluetoothGattCharacteristic mCharacteristic = characteristic;

        Log.e(TAG,"setWriteCharacteristic(): " + mCharacteristic.getUuid().toString() + " CMD: " + util.byteArrayInHexFormat(value));

        mCharacteristic.setValue(value);
//        mCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        mBluetoothGatt.setCharacteristicNotification(mCharacteristic, true);
        mBluetoothGatt.writeCharacteristic(mCharacteristic);
    }

    @SuppressLint("MissingPermission")
    public void setWriteCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value, int limitbytes) {
        BluetoothGattCharacteristic mCharacteristic = characteristic;

        Log.e(TAG,"setWriteCharacteristic(): " + mCharacteristic.getUuid().toString() + " CMD: " + util.byteArrayInHexFormat(value));

        buffer = value;
        limitBytes = limitbytes;
        if (buffer.length <= limitbytes) { mCharacteristic.setValue(value);}
        else {
            byte[] data = util.splitBytes(buffer,0,limitbytes);
        }

        mCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        mBluetoothGatt.setCharacteristicNotification(mCharacteristic, true);
        mBluetoothGatt.writeCharacteristic(mCharacteristic);
    }

    @SuppressLint("MissingPermission")
    public void setActivatePropiertiesCharacteristic(BluetoothGattCharacteristic characteristic, String type, UUID uuid) {

        if (type.contains("none")) {
            broadcastUpdate(ACTION_ENABLE_INDICATE_NOTIFY);
            return;
        }

        if (uuid == null) {
            broadcastUpdate(ACTION_ENABLE_INDICATE_NOTIFY);
            return;
        }

        Log.e(TAG,"UUID: " + uuid.toString() + ", type: " + type);

        BluetoothGattCharacteristic mCharacteristic = characteristic;
        mBluetoothGatt.setCharacteristicNotification(mCharacteristic, true);
        BluetoothGattDescriptor descriptor = mCharacteristic.getDescriptor(uuid);
        if (type.equals("indicate")) {
            Log.e(TAG,"ENABLE INDICATION: " + mCharacteristic.getUuid().toString());
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        }
        else if (type.equals("notify")) {
            Log.e(TAG,"ENABLE NOTIFICATION: " + mCharacteristic.getUuid().toString());
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        }
        else{
            Log.e(TAG,"ERROR setActivatePropiertiesCharacteristic: " + type);
            broadcastUpdate(ACTION_ENABLE_INDICATE_NOTIFY);
            return;
        }
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    @SuppressLint("MissingPermission")
    public void writeDescriptor(BluetoothGattDescriptor descriptor) {
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    public List<BluetoothGattCharacteristic> getBluetoothGattCharacteristic() {

        List<BluetoothGattCharacteristic> _characteritics = new ArrayList<BluetoothGattCharacteristic>();
        if (mBluetoothGatt == null) return _characteritics;

        List<BluetoothGattService> gattServices = mBluetoothGatt.getServices();
        if (gattServices == null) return _characteritics;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            String uuid = gattService.getUuid().toString();
            if (logViewService) { Log.e(TAG,"gattService: " + uuid); }

            List<BluetoothGattCharacteristic> GattCharacteristics = gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic characteristic : GattCharacteristics) {
                _characteritics.add(characteristic);
                uuid = characteristic.getUuid().toString();
                if (logViewService) { Log.e(TAG,"gattCharacteristic: " + uuid);}

                // Obtenemos la lista de descriptores que tiene la characteristic
                List<BluetoothGattDescriptor> listDescriptor = characteristic.getDescriptors();
                for (BluetoothGattDescriptor descri : listDescriptor) {
                    if (logViewService) { Log.e(TAG,"descriptor: " + descri.getUuid().toString()); }
                    listCharacteristicDescriptor.add(characteristic);
                }
            }
        }
        return _characteritics;
    }

    public List<BluetoothGattCharacteristic> getListCharacteristic() { return listCharacteristic; }

    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic) {
        mBluetoothGatt.setCharacteristicNotification(characteristic, true);
    }
    public List<BluetoothGattCharacteristic> getListCharacteristicDescriptor() { return listCharacteristicDescriptor; }

    public BluetoothGattCharacteristic getSearchCharacteristic(UUID uuid){

        for (BluetoothGattCharacteristic gattchar : listCharacteristic){
            if (uuid.equals(gattchar.getUuid())) { return gattchar; }
        };
        return null;
    }

    /**
     * @return (STATE_DISCONNECTED = 0, STATE_CONNECTING = 1, STATE_CONNECTED = 2
     **/
    public int isConnect() { return mConnectionState; }

    @SuppressLint("MissingPermission")
    public void enableNotifications(UUID serviceUuid, UUID characteristicUuid) {
        BluetoothGattService currentTimeService = getService(serviceUuid);
        if (currentTimeService == null) {
            Log.e(TAG, "Servicio NO encontrado");
            return;
        }

        BluetoothGattCharacteristic currentTimeCharacteristic = currentTimeService.getCharacteristic(characteristicUuid);
        if (currentTimeCharacteristic != null) {
            setCharacteristicNotification(currentTimeCharacteristic);
        } else {
            Log.e(TAG, "Característica No encontrada");
        }
    }

    //region :: Leer

    // Método para obtener un servicio específico
    @SuppressLint("MissingPermission")
    public BluetoothGattService getService(UUID serviceUuid) {
        if (mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothGatt no inicializado");
            return null;
        }
        return mBluetoothGatt.getService(serviceUuid);
    }

    // Método para leer una característica específica de un servicio
    @SuppressLint("MissingPermission")
    public void readCharacteristicFromService(UUID serviceUuid, UUID characteristicUuid) {
        BluetoothGattService service = getService(serviceUuid);
        if (service == null) {
            Log.e(TAG, "Servicio no encontrado: " + serviceUuid.toString());
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
        if (characteristic != null) {
            readCharacteristic(characteristic);
        } else {
            Log.e(TAG, "Característica no encontrada: " + characteristicUuid.toString());
        }
    }

    //endregion
}
