package com.eviahealth.eviahealth.bluetooth.beurer.ft95;

import static android.content.Context.BIND_AUTO_CREATE;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import com.eviahealth.eviahealth.bluetooth.BluetoothLeService;
import com.eviahealth.eviahealth.bluetooth.models.Global;
import com.eviahealth.eviahealth.devices.BeurerReferences;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.util;

import java.util.ArrayList;
import java.util.List;

public class ft95control {
    private static String TAG = "FT95CONTROL";

    private int isconnect = Global.STATE_DISCONNECTED;
    private Context mcontext;
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;

    List<BluetoothGattCharacteristic> ListCharacteristics = new ArrayList<>();
    private int index;
    private static  byte[] buffer = null;
    private static  byte[] bufferWRITE = null;
    private String lastFunction = "";

    CountDownTimer cTimer = null;
    private Integer nrecords = 0;

    public void initialize(Context context, String address) {
        this.mcontext = context;
        this.mDeviceAddress = address;
        this.index = 0;
        buffer = null;
        nrecords = 0;

        EVLog.log(TAG,"ft95control.initialize()");

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
        EVLog.log(TAG,"ft95control.onDestroy()");
        if (cTimer != null) {
            cTimer.cancel();
            cTimer = null;
        }

        if (isconnect != Global.STATE_DISCONNECTED) {
            if (mBluetoothLeService != null) { mBluetoothLeService.disconnect(); }
        }

        mcontext.unregisterReceiver(mReceiverControl);
        mcontext.unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    public void disconnect(){
        if (mBluetoothLeService != null) {
            EVLog.log(TAG,"ft95control.disconnect()");
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

    public BluetoothLeService getBluetoothConected(){
        return mBluetoothLeService;
    }

    public void setIndicateNotificate(int i) {
        BluetoothGattCharacteristic characteristic = ListCharacteristics.get(i);
        // Obtenemos Propiertes
        String propierties = ft95GattAttributes.getPropierties(characteristic.getUuid().toString());

        // Solicitamos activación
        mBluetoothLeService.setActivatePropiertiesCharacteristic(characteristic,propierties, ft95GattAttributes.UUID_DESCRIPTOR_CHARACTERISTIC_01);
    }

    //region :: BROADCAST RECEIVER
    private BroadcastReceiver mReceiverControl = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            String uuid = "";

//            Log.e(TAG, "mReceiverControl(): " + action);
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
                    if (isconnect == Global.STATE_CONNECTED) {

                        if (buffer != null) {
                            if (buffer.length >= ft95GattAttributes.LENGTH_RECORD_DATA) {
                                broadcastUpdate(BluetoothLeService.ACTION_DATA, buffer,null);
                            }
                            else {
                                // Se a desconectado pero no se han recibido un buffer correcto
                                isconnect = Global.STATE_DISCONNECTED;
                                broadcastUpdate(BeurerReferences.ACTION_BEURER_DISCONNECTED, "");
                            }
                        }
                        else {
                            // Se a desconectado pero no se han recibido datos
                            isconnect = Global.STATE_DISCONNECTED;
                            broadcastUpdate(BeurerReferences.ACTION_BEURER_DISCONNECTED, "");
                        }
                    }
                    else {
                        isconnect = Global.STATE_DISCONNECTED;
                        broadcastUpdate(BeurerReferences.ACTION_BEURER_DISCONNECTED, "");
                    }
                    //endregion
                    break;

                case BluetoothLeService.ACTION_ENABLE_INDICATE_NOTIFY:
                    //region :: ACTION_ENABLE_INDICATE_NOTIFY
                    index += 1;
                    if (index < ListCharacteristics.size()){ setIndicateNotificate(index); }
                    else {
                        EVLog.log(TAG,"Complete connection");
                        Log.e(TAG,"Complete connection");
                        broadcastUpdate(BeurerReferences.ACTION_BEURER_CONNECTED,"");
                    }
                    //endregion
                    break;

                case BluetoothLeService.ACTION_DATA_AVAILABLE_READ:
                    //region :: ACTION_DATA_AVAILABLE_READ
                    // Obtenemos datos enviados por el intent
                    byte[] dataRD = extras.getByteArray(mBluetoothLeService.EXTRA_DATA_BYTES);
                    String uuidRead = extras.getString(mBluetoothLeService.EXTRA_UUID);
                    EVLog.log(TAG,"READ_CHARACTERISTIC_DATA_BYTES: " + util.byteArrayInHexFormat(dataRD));
                    Log.e(TAG,"READ_CHARACTERISTIC_DATA_BYTES: " + util.byteArrayInHexFormat(dataRD));
                    //endregion
                    break;

                case BluetoothLeService.ACTION_WRITE_CHARACTERISTIC:
                    //region :: ACTION_WRITE_CHARACTERISTIC
                    byte[] dataW = extras.getByteArray(mBluetoothLeService.EXTRA_DATA_BYTES_WRITE);
                    EVLog.log(TAG,"WRITE_CHARACTERISTIC_DATA: " + util.byteArrayInHexFormat(dataW));
                    Log.e(TAG,"WRITE_CHARACTERISTIC_DATA: " + util.byteArrayInHexFormat(dataW));

//                    if (Arrays.equals(dataW, ft95GattAttributes.CMD_SET_DELETE_DATA_STORAGE)) {
//                        EVLog.log(TAG,"FT95_SUCCESS_DELETE_DATA_STORAGE");
//                        broadcastUpdate(BeurerReferences.ACTION_BEURER_FT95_DELETE_DATA_STORAGE, "{\"success\":\"ok\",\"function\":\"setDeleteDataStorage\"}");
//                    }

                    //endregion
                    break;

                case BluetoothLeService.ACTION_CHARACTERISTIC_CHANGED:
                    //region :: ACTION_DATA_AVAILABLE
                    byte[] dataRx = extras.getByteArray(mBluetoothLeService.EXTRA_DATA_BYTES);
                    uuid = extras.getString(mBluetoothLeService.EXTRA_UUID);
                    byte command;

                    setTimeout(Global.STATE_RELOAD);

                    if (buffer == null) {
                        buffer = dataRx;
                        EVLog.log(TAG,"(1): " + util.byteArrayInHexFormat(dataRx));
                    }
                    else {
                        buffer = util.addAll(buffer,dataRx);
                        EVLog.log(TAG,"(2): " + util.byteArrayInHexFormat(dataRx));
                    }

                    if (nrecords == 3) {
                        broadcastUpdate(BeurerReferences.ACTION_BEURER_FT95_DOWNLOAD_DATA, "");
                    }
                    nrecords += 1;

                    //endregion
                    break;

                case BluetoothLeService.ACTION_DATA:
                    // region :: ACTION_DATA
                    setTimeout(Global.STATE_DISABLE);
                    // Obtenemos datos enviados por el intent
                    byte[] data = extras.getByteArray(mBluetoothLeService.EXTRA_DATA_BYTES);
                    buffer = null; // limpiamos buffer de recepcion

                    // Nota este termometro almacena hasta 60 registro de medidas y los envia todos
                    // el más actual es siempre el último registro.
                    Integer rg = data.length / ft95GattAttributes.LENGTH_RECORD_DATA;
                    Log.e(TAG, "buffer.length: " + data.length + ", registros: " + rg);

                    if (data.length >=  ft95GattAttributes.LENGTH_RECORD_DATA) {
                        EVLog.log(TAG, "RESPONSE_DOWNLOAD_DATA");
                        String message = "{}";

                        if ((data.length % ft95GattAttributes.LENGTH_RECORD_DATA) == 0) {
                            byte[][] temperatures = divideArray(data);

                            for (byte[] obj : temperatures) {
                                rg -= 1;
                                if (rg == 0) {
                                    // Mostramos solo el útlimo registro que es el medido ahora
                                    message = ft95manager.msgTemperature(obj);
//                                    EVLog.log(TAG, "MESSAGE: " + message);
                                }
                            }
                        }

                        broadcastUpdate(BeurerReferences.ACTION_BEURER_FT95_DOWNLOAD_DATA_FINISHED, message);
                    }
                    else {

                        EVLog.log(TAG, "RECEIVED COMMAND NOT IMPLEMENTED");
                        String message = util.byteArrayInHexFormat(buffer);
                        EVLog.log(TAG, "MESSAGE: " + message);
                        broadcastUpdate(BeurerReferences.ACTION_BEURER_GET_OTHER, message);
                    }
                    isconnect = Global.STATE_DISCONNECTED;
                    //endregion
                    break;

                case BluetoothLeService.ACTION_WRITE_FAIL:
                    EVLog.log(TAG,"ACTION_WRITE_FAIL");
                    broadcastUpdate(BeurerReferences.ACTION_BEURER_CMD_FAIL, "{\"success\":\"fail\",\"function\":\"actionWriteFail\"}");
                    break;

                default:
                    EVLog.log(TAG,"ACTION_DEFAULT");
                    broadcastUpdate(BeurerReferences.ACTION_BEURER_CMD_FAIL, "{\"success\":\"fail\",\"function\":\"setDateTime()\"}");
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
        intentFilter.addAction(BluetoothLeService.ACTION_CHARACTERISTIC_CHANGED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE_READ);
        intentFilter.addAction(BluetoothLeService.ACTION_WRITE_FAIL);
        return intentFilter;
    }

    private void broadcastUpdate(final String action, String message) {
        setTimeout(Global.STATE_DISABLE);
        final Intent intent = new Intent(action);
        intent.putExtra(BeurerReferences.ACTION_EXTRA_MESSAGE, message);
        mcontext.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, byte[] values, String uuid) {
        setTimeout(Global.STATE_DISABLE);
        final Intent intent = new Intent(action);
        intent.putExtra(BluetoothLeService.EXTRA_DATA_BYTES,values);
        intent.putExtra(BluetoothLeService.EXTRA_UUID,uuid);
        mcontext.sendBroadcast(intent);
    }

    private void setTimeout(int action) {

        Integer timeout = 1000 * 4;
        if (action == 1) {
            // Start Timeout
            cTimer = new CountDownTimer(timeout, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {}

                @Override
                public void onFinish() {
                    cTimer.cancel();
                    cTimer = null;
                    buffer = null;
                    EVLog.log(TAG,"COMMUNICATION_TIMEOUT");
                    broadcastUpdate(BeurerReferences.ACTION_BEURER_COMMUNICATION_TIMEOUT, "{\"success\":\"fail\",\"function\":\"" + lastFunction + "\"}");
                }
            };
            EVLog.log(TAG,"Timeout: Start");
            cTimer.start();
        }
        else if (action > 1) {
            // Reload
            if (cTimer == null) return;
            cTimer.cancel();
            EVLog.log(TAG,"Timeout: Reload");
            cTimer.start();
        }
        else {
            // Finish
            if (cTimer == null) return;
            cTimer.cancel();
            cTimer = null;
            EVLog.log(TAG,"Timeout: stop");
        }

    }

    //endregion

    //region :: Funciones

    public byte[][] divideArray(byte[] source) {
        int size = 13;
        byte[][] result = new byte[(int)Math.ceil(source.length / (double)size)][size];

        int start = 0;

        for(int i = 0; i < result.length; i++) {
            if(start + size > source.length) {
                System.arraycopy(source, start, result[i], 0, source.length - start);
            } else {
                System.arraycopy(source, start, result[i], 0, size);
            }
            start += size ;
        }

        return result;
    }

    //endregion
}
