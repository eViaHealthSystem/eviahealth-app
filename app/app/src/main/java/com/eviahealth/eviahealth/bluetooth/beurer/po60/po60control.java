package com.eviahealth.eviahealth.bluetooth.beurer.po60;

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
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class po60control {
    private static String TAG = "PO60CONTROL";

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
    private Integer nrecords = null;
    private Integer lendownload = null;

    public void initialize(Context context, String address) {
        this.mcontext = context;
        this.mDeviceAddress = address;
        this.index = 0;
        buffer = null;

        EVLog.log(TAG,"mPO60control.initialize()");

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
            EVLog.log(TAG,"Connecting: " + mDeviceAddress);
            boolean isconnect = mBluetoothLeService.connect(mDeviceAddress);
            EVLog.log(TAG,"isconnect: " + isconnect);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            isconnect = Global.STATE_DISCONNECTED;
        }

    };

    public void destroy(){
        Log.e(TAG, "onDestroy()");
        EVLog.log(TAG,"mPO60control.onDestroy()");
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
            EVLog.log(TAG,"mPO60control.disconnect()");
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
        String propierties = po60GattAttributes.getPropierties(characteristic.getUuid().toString());

        // Solicitamos activación
        mBluetoothLeService.setActivatePropiertiesCharacteristic(characteristic,propierties, po60GattAttributes.UUID_DESCRIPTOR_CHARACTERISTIC_02);
    }

    //region :: BROADCAST RECEIVER
    private  BroadcastReceiver mReceiverControl = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            String uuid;

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
                    if (isconnect == Global.STATE_CONNECTING) {
                        broadcastUpdate(BeurerReferences.ACTION_BEURER_DISCONNECTED,"");
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
                    //endregion
                    break;

                case BluetoothLeService.ACTION_WRITE_CHARACTERISTIC:
                    //region :: ACTION_WRITE_CHARACTERISTIC
                    byte[] dataW = extras.getByteArray(mBluetoothLeService.EXTRA_DATA_BYTES_WRITE);
                    EVLog.log(TAG,"WRITE_CHARACTERISTIC_DATA: " + util.byteArrayInHexFormat(dataW));

                    if (Arrays.equals(dataW, po60GattAttributes.CMD_SET_DELETE_DATA_STORAGE)) {
                        EVLog.log(TAG,"PO60_SUCCESS_DELETE_DATA_STORAGE");
                        broadcastUpdate(BeurerReferences.ACTION_BEURER_PO60_DELETE_DATA_STORAGE, "{\"success\":\"ok\",\"function\":\"setDeleteDataStorage\"}");
                    }

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

                    // Comprobación de Respuesta recibida
                    command = buffer[0];
                    if (command == po60GattAttributes.RESPONSE_CMD_GET_DEVICE_VERSION) {
                        if (po60GattAttributes.LENGTH_DEVICE_VERSION == buffer.length) {
                            broadcastUpdate(BluetoothLeService.ACTION_DATA, buffer, uuid);
                        }
                        else {
                            EVLog.log(TAG, "FAIL: RESPONSE_CMD_GET_DEVICE_VERSION");
                            broadcastUpdate(BeurerReferences.ACTION_BEURER_CMD_FAIL, "{\"success\":\"fail\",\"function\":\"getDeviceVersion\"}");
                        }
                    }
                    else if (command == po60GattAttributes.RESPONSE_CMD_SET_TIME) {
                        if (po60GattAttributes.LENGTH_SET_TIME == buffer.length) {
                            broadcastUpdate(BluetoothLeService.ACTION_DATA, buffer, uuid);
                        }
                        else {
                            EVLog.log(TAG, "FAIL: RESPONSE_CMD_SET_TIME");
                            broadcastUpdate(BeurerReferences.ACTION_BEURER_CMD_FAIL, "{\"success\":\"fail\",\"function\":\"setTime\"}");
                        }
                    }
                    else if (command == po60GattAttributes.RESPONSE_CMD_GET_TIME) {
                        if (po60GattAttributes.LENGTH_GET_TIME == buffer.length) {
                            broadcastUpdate(BluetoothLeService.ACTION_DATA, buffer, uuid);
                        }
                        else {
                            EVLog.log(TAG, "FAIL: RESPONSE_CMD_GET_TIME");
                            broadcastUpdate(BeurerReferences.ACTION_BEURER_CMD_FAIL, "{\"success\":\"fail\",\"function\":\"getTime\"}");
                        }
                    }
                    else if (command == po60GattAttributes.RESPONSE_SET_TYPE_STORAGE_DATA) {
                        if (po60GattAttributes.LENGTH_SET_TYPE_STORAGE_DATA == buffer.length) {
                            broadcastUpdate(BluetoothLeService.ACTION_DATA, buffer, uuid);
                        }
                        else {
                            EVLog.log(TAG, "FAIL: RESPONSE_SET_TYPE_STORAGE_DATA");
                            broadcastUpdate(BeurerReferences.ACTION_BEURER_CMD_FAIL, "{\"success\":\"fail\",\"function\":\"setTypeStorageData\"}");
                        }
                    }
                    else if (command == po60GattAttributes.RESPONSE_DOWNLOAD_DATA) {
                        EVLog.log(TAG,"lenght: " +lendownload * po60GattAttributes.LENGTH_RECORD_DATA + ",buffer.length:" + buffer.length);
                        if ((lendownload * po60GattAttributes.LENGTH_RECORD_DATA) == buffer.length) {
                            nrecords -= 10;
                            lendownload = (nrecords > 10) ? 10 : nrecords;
                            broadcastUpdate(BluetoothLeService.ACTION_DATA, buffer, uuid);
                        }
                    }
                    //endregion
                    break;

                case BluetoothLeService.ACTION_DATA:
                    // region :: ACTION_DATA
                    setTimeout(Global.STATE_DISABLE);
                    // Obtenemos datos enviados por el intent
                    byte[] data = extras.getByteArray(mBluetoothLeService.EXTRA_DATA_BYTES);
                    uuid = extras.getString(mBluetoothLeService.EXTRA_UUID);
                    buffer = null; // limpiamos buffer de recepcion

                    if (uuid.equals(po60GattAttributes.CHARACTERISTIC_FF02)) {
                        command = data[0];

                        //region :: RESPONSE
                        if (command == po60GattAttributes.RESPONSE_CMD_GET_DEVICE_VERSION) {
                            EVLog.log(TAG,"RESPONSE_CMD_GET_DEVICE_VERSION");
                            String message = po60manager.msgDeviceVersion(data);
                            EVLog.log(TAG,"MESSAGE: " + message);

                            broadcastUpdate(BeurerReferences.ACTION_BEURER_PO60_DEVICE_VERSION, message);
                        }
                        else if (command == po60GattAttributes.RESPONSE_CMD_SET_TIME) {
                            EVLog.log(TAG,"RESPONSE_CMD_SET_TIME");
                            int status = util.toUnsignedInt(data[1]);
                            if (status == 0) {
                                broadcastUpdate(BeurerReferences.ACTION_BEURER_PO60_UPDATE_TIME, "{\"success\":\"ok\",\"function\":\"setTime\"}");
                            }
                            else {
                                broadcastUpdate(BeurerReferences.ACTION_BEURER_CMD_FAIL, "{\"success\":\"fail\",\"function\":\"setTime\"}");
                            }
                        }
                        else if (command == po60GattAttributes.RESPONSE_CMD_GET_TIME) {
                            EVLog.log(TAG,"RESPONSE_CMD_GET_TIME");
                            String message = po60manager.msgGetTime(data);
                            EVLog.log(TAG,"MESSAGE: " + message);

                            broadcastUpdate(BeurerReferences.ACTION_BEURER_GET_OTHER, "{\"success\":\"ok\",\"function\":\"getTime\"}");

                        }
                        else if (command == po60GattAttributes.RESPONSE_SET_TYPE_STORAGE_DATA) {
                            EVLog.log(TAG,"RESPONSE_SET_TYPE_STORAGE_DATA");
                            String message = po60manager.msgGetRecords(data);
                            nrecords = util.getIntValueJSON(message,"records");
                            lendownload = (nrecords > 10) ? 10 : nrecords;

                            EVLog.log(TAG,"MESSAGE: " + message);
                            broadcastUpdate(BeurerReferences.ACTION_BEURER_PO60_RECORDS_STORAGE, message);
                        }
                        else if (command == po60GattAttributes.RESPONSE_DOWNLOAD_DATA) {
                            EVLog.log(TAG,"RESPONSE_DOWNLOAD_DATA");
                            int size = data.length;
                            String message = "";
                            for (int i=0; i < size; i += po60GattAttributes.LENGTH_RECORD_DATA) {
                                byte[] registro = util.splitBytes(data, i, i+po60GattAttributes.LENGTH_RECORD_DATA);
                                message = po60manager.msgGetRecordData(registro);
                                EVLog.log(TAG,"MESSAGE: " + message);
                            }
                            if (nrecords > 0)
                                broadcastUpdate(BeurerReferences.ACTION_BEURER_PO60_DOWNLOAD_DATA, message);
                            else
                                broadcastUpdate(BeurerReferences.ACTION_BEURER_PO60_DOWNLOAD_DATA_FINISHED, message);
                        }
                        else {
                            EVLog.log(TAG,"RECEIVED COMMAND NOT IMPLEMENTED");
                            String message = util.byteArrayInHexFormat(buffer);
                            EVLog.log(TAG,"MESSAGE: " + message);
                            broadcastUpdate(BeurerReferences.ACTION_BEURER_GET_OTHER, message);
                        }
                        //endregion

                    }
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

    public boolean setWriteCommand(byte[] data) {
        EVLog.log(TAG,"setWriteCommand()");

        BluetoothGattCharacteristic gattCharacteristic = mBluetoothLeService.getSearchCharacteristic(po60GattAttributes.UUID_CHARACTERISTIC_FF01);
        if (gattCharacteristic == null) {
            EVLog.log(TAG,"UUID_CHARACTERISTIC_FF01");
            return false;
        }

        setTimeout(Global.STATE_ENABLE); // Habilitamos Timeout de recepción
        bufferWRITE = data;
        mBluetoothLeService.setWriteCharacteristic(gattCharacteristic, data);
        return true;
    }

    public byte calculateChecksum(byte[] data) {
        EVLog.log(TAG,"calculateChecksum()");
        int checksum = 0;
        int len = data.length - 1;
        for (int i = 0; i < len; i++) { checksum += util.toUnsignedInt(data[i]); }

        byte result = (byte)(checksum & (byte)0x7f);
        Log.e(TAG,"Checksum: " + result);
        return result;
    }


    public Integer getNrecords() { return this.nrecords; }

    public boolean getDeviceVersion() {
        lastFunction = "getDeviceVersion()";
        EVLog.log(TAG,lastFunction);
        setWriteCommand(po60GattAttributes.CMD_GET_DEVICE_VERSION);
        return true;
    }

    public boolean getTime() {
        lastFunction = "getTime()";
        EVLog.log(TAG,lastFunction);
        setWriteCommand(po60GattAttributes.CMD_GET_TIME);
        return true;
    }

    public boolean setTime() {
        lastFunction = "setTime()";
        EVLog.log(TAG,lastFunction);

        Calendar instance = Calendar.getInstance();
        int year = instance.get(Calendar.YEAR) - 2000;      // year
        int month = instance.get(Calendar.MONTH) + 1;       // month
        int day = instance.get(Calendar.DAY_OF_MONTH);      // day
        int hour = instance.get(Calendar.HOUR_OF_DAY);      // hour
        int minute = instance.get(Calendar.MINUTE);         // minute
        int second = instance.get(Calendar.SECOND);         // second

        // Set Characteristic and command and values
        byte[] values = new byte[10];
        values[0] = po60GattAttributes.CMD_SET_TIME;  //Commando 0x83

        values[1] = (byte)(year & 0x7f);    // year
        values[2] = (byte)(month & 0x0f);   // Month
        values[3] = (byte)(day & 0x1f);     // Day
        values[4] = (byte)(hour & 0x1f);    // Hour
        values[5] = (byte)(minute & 0x3f);  // Minute
        values[6] = (byte)(second & 0x3f);  // Second

        values[7] = 0x00; values[8] = 0x00;     // Milliseconds

        values[9] = calculateChecksum(values);  // Checksum

        buffer = null;
        setWriteCommand(values);
        return true;
    }

    public boolean setTypeStorageData() {
        lastFunction = "setTypeStorageData()";
        EVLog.log(TAG,lastFunction);
        nrecords = 0;
        buffer = null;
        setWriteCommand(po60GattAttributes.CMD_SET_TYPE_STORAGE_DATA);
        return true;
    }

    public boolean getStartDownloadData() {
        lastFunction = "getStartDownloadData()";
        EVLog.log(TAG,lastFunction);
        buffer = null;
        setWriteCommand(po60GattAttributes.CMD_GET_START_DOWNLOAD_DATA);
        return true;
    }

    public boolean getContinueDownloadData() {
        lastFunction = "getContinueDownloadData()";
        EVLog.log(TAG,lastFunction);
        buffer = null;
        setWriteCommand(po60GattAttributes.CMD_GET_CONTINUE_DOWNLOAD_DATA);
        return true;
    }

    public boolean setDeleteDataStorage() {
        lastFunction = "setDeleteDataStorage()";
        EVLog.log(TAG,lastFunction);
        buffer = null;
        setWriteCommand(po60GattAttributes.CMD_SET_DELETE_DATA_STORAGE);
        return true;
    }
    //endregion
}
