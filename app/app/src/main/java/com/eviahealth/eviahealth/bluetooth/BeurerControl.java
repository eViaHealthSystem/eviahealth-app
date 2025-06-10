package com.eviahealth.eviahealth.bluetooth;

import static android.content.Context.BIND_AUTO_CREATE;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.eviahealth.eviahealth.bluetooth.models.Global;
import com.eviahealth.eviahealth.devices.BeurerReferences;
import com.eviahealth.eviahealth.bluetooth.beurer.bf600.bf600GattAttributes;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BeurerControl {
    private static String TAG = "BEURER-CONTROL";

    private int isconnect = Global.STATE_DISCONNECTED;
    private Context mcontext;
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;

    List<BluetoothGattCharacteristic> ListCharacteristics = new ArrayList<>();
    private int index;
    private static  byte[] buffer = null;

    public void initialize(Context context, String address) {
        this.mcontext = context;
        this.mDeviceAddress = address;
        this.index = 0;

        EVLog.log(TAG,"mBeurerControl.initialize()");

        // registramos broadcast
        context.registerReceiver(mReceiverControl, makeGattUpdateIntentFilter(), context.RECEIVER_EXPORTED);

        // Generamos servicio de bluetooth
        Intent gattServiceIntent = new Intent(mcontext, BluetoothLeService.class);
        mcontext.bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
//                Log.e(TAG, "Unable to initialize Bluetooth");
                EVLog.log(TAG,"No se ha podido inicializar el servicio de Bluetooth");
                return;
            }

            // Automatically connects to the device upon successful start-up initialization.
            isconnect = Global.STATE_CONNECTING;
            EVLog.log(TAG,"Connect: " + mDeviceAddress);
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            isconnect = Global.STATE_DISCONNECTED;
        }

    };

    public void destroy(){
        Log.e(TAG, "onDestroy()");
        EVLog.log(TAG,"onDestroy()");

        if (isconnect != Global.STATE_DISCONNECTED) {
            if (mBluetoothLeService != null) {
                Log.e(TAG,"disconnect()");
                mBluetoothLeService.disconnect();
            }
        }

        if( mReceiverControl.isOrderedBroadcast() ){
            // receiver object is registered
            mcontext.unregisterReceiver(mReceiverControl);
        } else {
            // receiver object is not registered
        }

        mcontext.unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    public void disconnect(){
        if (mBluetoothLeService != null) {
            EVLog.log(TAG,"mBeurerControl.disconnect()");
            mBluetoothLeService.disconnect();
        }
    }

    public void close(){
        if (mBluetoothLeService != null) {
            mBluetoothLeService.close();
        }
    }

    public int getIsconnect(){
        return this.isconnect;
    }

    @SuppressLint("MissingPermission")
    public void setIndicateNotificate(int i) {
        BluetoothGattCharacteristic characteristic = ListCharacteristics.get(i);
        // Obtenemos Propiertes
        final int charaProp = characteristic.getProperties();

        UUID uuid = characteristic.getUuid();
        List<BluetoothGattDescriptor> listDescriptor = characteristic.getDescriptors();

        if (listDescriptor .size() > 0) {
            BluetoothGattDescriptor descriptor = listDescriptor.get(0);

            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//                Log.e(TAG, "ENABLE NOTIFICATION: " + characteristic.getUuid().toString());
//                Log.e(TAG, "DESCRIPTOR NOTIFICATION: " + descriptor.getUuid().toString());
                Log.e(TAG, "SOLICITUD EMPAREJAMIENTO (NOTIFY)");
                mBluetoothLeService.setCharacteristicNotification(characteristic);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                // Solicitamos activación
                mBluetoothLeService.writeDescriptor(descriptor);
            }
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
//                Log.e(TAG, "ENABLE INDICATE: " + characteristic.getUuid().toString());
//                Log.e(TAG, "DESCRIPTOR INDICATE: " + descriptor.getUuid().toString());
                Log.e(TAG, "SOLICITUD EMPAREJAMIENTO (INDICATE)");
                mBluetoothLeService.setCharacteristicNotification(characteristic);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                // Solicitamos activación
                mBluetoothLeService.writeDescriptor(descriptor);
            }
        }

    }

    //region :: BROADCAST RECEIVER
    private BroadcastReceiver mReceiverControl = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            String uuid;
            EVLog.log(TAG,"RECEIVER ACTION: " + action);

            switch(action) {
                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    //region :: ACTION_GATT_CONNECTED
                    isconnect = Global.STATE_CONNECTED;
                    EVLog.log(TAG,"isconnect: " + Global.connectStatus.get(isconnect));

                    ListCharacteristics.clear();
                    ListCharacteristics = mBluetoothLeService.getListCharacteristicDescriptor();

                    if (ListCharacteristics.size() > 0){
                        index = 0;
                        setIndicateNotificate(index);
                    }
                    else {
                        EVLog.log(TAG,"No se ha encontrado ningún servicio en el dispositivo");
                    }
                    //endregion
                    break;

                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    //region :: ACTION_GATT_DISCONNECTED
                    EVLog.log(TAG,"isconnect: " + Global.connectStatus.get(isconnect));
                    broadcastUpdate(BeurerReferences.ACTION_BEURER_DISCONNECTED, Global.connectStatus.get(isconnect));
                    //endregion
                    break;

                case BluetoothLeService.ACTION_ENABLE_INDICATE_NOTIFY:
                    //region :: ACTION_ENABLE_INDICATE_NOTIFY
                    index += 1;
                    if (index < ListCharacteristics.size()){ setIndicateNotificate(index); }
                    else {
                        EVLog.log(TAG,"Complete connection");
                        broadcastUpdate(BeurerReferences.ACTION_BEURER_CONNECTED,"");
                    }
                    //endregion
                    break;

                case BluetoothLeService.ACTION_WRITE_CHARACTERISTIC:
                    //region :: ACTION_WRITE_CHARACTERISTIC
                    byte[] dataW = extras.getByteArray(mBluetoothLeService.EXTRA_DATA_BYTES_WRITE);
                    EVLog.log(TAG,"WRITE_CHARACTERISTIC_DATA: " + util.byteArrayInHexFormat(dataW));
                    //endregion
                    break;

                default:
                    EVLog.log(TAG,"ACTION_DEFAULT");
                    break;
            };

        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_ENABLE_INDICATE_NOTIFY);
        intentFilter.addAction(BluetoothLeService.ACTION_WRITE_CHARACTERISTIC);
        return intentFilter;
    }

    private void broadcastUpdate(final String action, String message) {
        final Intent intent = new Intent(action);
        intent.putExtra(BeurerReferences.ACTION_EXTRA_MESSAGE, message);
        mcontext.sendBroadcast(intent);
    }

    //endregion

    public boolean setWriteCommand(UUID uuid, byte[] data) {
        BluetoothGattCharacteristic gattCharacteristic = mBluetoothLeService.getSearchCharacteristic(uuid);
        if (gattCharacteristic == null) {
            EVLog.log(TAG,"setWriteCommand uuid: " + uuid.toString());
            return false;
        }

        mBluetoothLeService.setWriteCharacteristic(gattCharacteristic, data);
        return true;
    }

    public void UserActive(int user_index) {
        buffer = null;
        byte[] values = new byte[4];
        values[0] = 0x02;               // OP 0x02 Procedimiento de consentimiento
        values[1] = (byte) user_index;  // user index
        values[2] = (byte) 0xd2;        // Consent Code 1234 0x04d2
        values[3] = (byte) 0x04;        // parte baja
        setWriteCommand(bf600GattAttributes.USER_CONTROL_POINT_CHARACTERISTIC_UUID, values);
    }
}
