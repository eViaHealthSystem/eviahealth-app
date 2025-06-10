package com.eviahealth.eviahealth.bluetooth.beurer.bm57;

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

public class bm57control {
    private static String TAG = "bm57-CONTROL";

    private int isconnect = Global.STATE_DISCONNECTED;
    private Context mcontext;
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;

    List<BluetoothGattCharacteristic> ListCharacteristics = new ArrayList<>();
    private int index;
    private static  byte[] buffer = null;

    CountDownTimer cTimer = null;
    static List<String> listado = new ArrayList<>();
    static List<String> listUser1 = new ArrayList<>();
    static List<String> listUser2 = new ArrayList<>();

    public void initialize(Context context, String address) {
        this.mcontext = context;
        this.mDeviceAddress = address;
        this.index = 0;
        listado.clear();
        listUser1.clear();
        listUser2.clear();

        EVLog.log(TAG,"mRC87control.initialize()");

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
        EVLog.log(TAG,"mRC87control.onDestroy()");
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
            EVLog.log(TAG,"mRC87control.disconnect()");
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
        String propierties = bm57GattAttributes.getPropierties(characteristic.getUuid().toString());

        // Solicitamos activación
        mBluetoothLeService.setActivatePropiertiesCharacteristic(characteristic,propierties, bm57GattAttributes.UUID_DESCRIPTOR_CHARACTERISTIC_01);
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
                    //endregion
                    break;

                case BluetoothLeService.ACTION_CHARACTERISTIC_CHANGED:
                    //region :: ACTION_DATA_AVAILABLE
                    byte[] dataRx = extras.getByteArray(mBluetoothLeService.EXTRA_DATA_BYTES);
                    uuid = extras.getString(mBluetoothLeService.EXTRA_UUID);

                    if (buffer == null) {
                        buffer = dataRx;
                        EVLog.log(TAG,"(1): " + util.byteArrayInHexFormat(dataRx));
                    }
                    else {
                        buffer = util.addAll(buffer,dataRx);
                        EVLog.log(TAG,"(2): " + util.byteArrayInHexFormat(dataRx));
                    }

                    // Comprobación de Respuesta recibida
                    if (buffer.length >= bm57GattAttributes.LENGTH_BLOOD_PRESSURE_MEASUREMENT) {
                        broadcastUpdate(BluetoothLeService.ACTION_DATA, buffer, uuid);
                    }
                    else {
                        EVLog.log(TAG, "FAIL: BLOOD_PRESSURE_MEASUREMENT");
                        broadcastUpdate(BeurerReferences.ACTION_BEURER_CMD_FAIL, "{\"success\":\"fail\",\"function\":\"Blood Pressure Measurement\"}");
                    }

                    //endregion
                    break;

                case BluetoothLeService.ACTION_DATA:
                    // region :: ACTION_DATA

                    // Obtenemos datos enviados por el intent
                    byte[] data = extras.getByteArray(mBluetoothLeService.EXTRA_DATA_BYTES);
                    uuid = extras.getString(mBluetoothLeService.EXTRA_UUID);
                    buffer = null; // limpiamos buffer de recepcion

                    if (uuid.equals(bm57GattAttributes.CHARACTERISTIC_2A35)) {
                        //region :: RESPONSE
                        if (data.length == bm57GattAttributes.LENGTH_BLOOD_PRESSURE_MEASUREMENT) {
                            EVLog.log(TAG,"RESPONSE_BLOOD_PRESSURE_MEASUREMENT");
                            String message = msgGetRecordData(data);
                            EVLog.log(TAG,"MESSAGE: " + message);
                            listado.add(message);

                            Integer userID = util.getIntValueJSON(message,"userID");
                            if (userID == 1) { listUser1.add(message); }
                            else { listUser2.add(message); }

                            if (listado.size() == 1) {
                                // Indica que ya se ha iniciado la descarga
                                broadcastUpdate(BeurerReferences.ACTION_BEURER_BC54_DOWNLOAD_DATA, "");
                            }
                        }
                        else {
                            EVLog.log(TAG,"RECEIVED COMMAND NOT IMPLEMENTED");
                            String message = util.byteArrayInHexFormat(buffer);
                            EVLog.log(TAG,"MESSAGE: " + message);
                            broadcastUpdate(BeurerReferences.ACTION_BEURER_GET_OTHER, message);
                        }
                        //endregion
                    }
                    else {
                        Log.e(TAG,"******************************** UUID: " + uuid);
                    }
                    //endregion
                    break;

                case BluetoothLeService.ACTION_WRITE_FAIL:
                    EVLog.log(TAG,"ACTION_WRITE_FAIL");
                    broadcastUpdate(BeurerReferences.ACTION_BEURER_CMD_FAIL, "{\"success\":\"fail\",\"function\":\"actionWriteFail\"}");
                    break;

                default:
                    EVLog.log(TAG,"ACTION_DEFAULT");
                    broadcastUpdate(BeurerReferences.ACTION_BEURER_CMD_FAIL, "{\"success\":\"fail\",\"function\":\"actiondefault()\"}");
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
        final Intent intent = new Intent(action);
        intent.putExtra(BeurerReferences.ACTION_EXTRA_MESSAGE, message);
        mcontext.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, byte[] values, String uuid) {
        final Intent intent = new Intent(action);
        intent.putExtra(BluetoothLeService.EXTRA_DATA_BYTES,values);
        intent.putExtra(BluetoothLeService.EXTRA_UUID,uuid);
        mcontext.sendBroadcast(intent);
    }

    //endregion

    // Devuelve el últomo valor de la lista
    public static String getRecordData() {
        String message = "";
        if (listado.size() > 0) {

            int idUser1 = listUser1.size() - 1;
            int idUser2 = listUser2.size() - 1;

            Log.e(TAG, "size1: " + listUser1.size() + ", size2: " + listUser2.size());
            if (listUser1.size() > 0 && listUser2.size() > 0) {
                /*
                Obtenemos la fecha mayor del último registro de cada usuario,
                el de mayor fecha se considera que es la última medición realizada.
                NOTA IMPORTANTE, el ternsiómetro tiene que estar en fecha y hora correcta.
                 */
                String messageUser1 = listUser1.get(idUser1);
                String messageUser2 = listUser2.get(idUser2);

//                Log.e(TAG, "iumsg1: " + messageUser1);
//                Log.e(TAG, "iumsg2: " + messageUser2);

                String dateUser1 = util.getStringValueJSON(messageUser1,"timestamp");
                String dateUser2 = util.getStringValueJSON(messageUser2,"timestamp");

//                Log.e(TAG, "date1: " + dateUser1);
//                Log.e(TAG, "date2: " + dateUser2);

                long tUser1 = util.getTimestamp(dateUser1);
                long tUser2 = util.getTimestamp(dateUser2);

//                Log.e(TAG, "tUser1: " + tUser1);
//                Log.e(TAG, "tUser2: " + tUser2);

                if (tUser1 >= tUser2) { message = messageUser1; }
                else { message = messageUser2; }

            }
            else if (listUser1.size() == 0) {
                message = listUser2.get(idUser2);
            }
            else { message = listUser1.get(idUser1); }

        }
        return message;
    }

    public static String msgGetRecordData(byte[] values){
        String message = "{ ";

        Integer flags = util.toUnsignedInt(values[0]);
        Integer systolic = util.BytesToInt(new byte[]{values[2], values[1]});
        Integer diastolic = util.BytesToInt(new byte[]{values[4], values[3]});
        Integer pulse = util.BytesToInt(new byte[]{values[15], values[14]});
        Integer userID = util.toUnsignedInt(values[16]) + 1;

        //region :: timestamp
        Integer year = util.BytesToInt(new byte[]{values[8], values[7]});
        Integer month = util.toUnsignedInt(values[9]);
        Integer day = util.toUnsignedInt(values[10]);
        Integer hour = util.toUnsignedInt(values[11]);
        Integer minute = util.toUnsignedInt(values[12]);
        Integer second = util.toUnsignedInt(values[13]);

        String sYear = "" + String.format("%04d", year);
        String sMonth = "" + String.format("%02d", month);
        String sDay = "" + String.format("%02d", day);
        String sHour = "" + String.format("%02d", hour);
        String sMinute = "" + String.format("%02d", minute);
        String sSecond = "" + String.format("%02d", second);

        String timestamp = "" + sYear + "-" + sMonth + "-" + sDay + " " + sHour+ ":" + sMinute+ ":" + sSecond;
        //endregion

        Integer status = util.BytesToInt(new byte[]{values[18], values[17]});

        message += "\"timestamp\":\"" + timestamp + "\",";
        message += "\"systolic\":" + systolic + ",";
        message += "\"diastolic\":" + diastolic  + ",";
        message += "\"pulse\":" + pulse + ",";
        message += "\"userID\":" + userID + ",";
        message += "\"status\":" + status + "";

        return message + " }";
    }

    public static String msgGetMeasurementStatus(String value) {
        String message = "{ ";

        Integer status = util.getIntValueJSON(value,"status") & 0x3f;

        Integer bodyMovement = 0;            // 0 = sin movimiento del cuerpo
        Integer cuffFit = 0;                 // 0 = el manguito se ajusta correctamente
        Integer irregularPulse = 0;          // 0 = pulso regular
        Integer pulseRateRange = 0;          // 00 = "ien rango"
        Integer measurementPosition = 0;     // 0 = posición correcta de la medición

        if ((status & 0x01) == 0x01) {
            bodyMovement = 1;               // 1 = Movimiento del cuerpo
        }
        if ((status & 0x02) == 0x02) {
            cuffFit = 1;                    // 1 = el manguito está demasiado suelto
        }
        if ((status & 0x04) == 0x04) {
            irregularPulse = 1;             // 1 = Pulso irregular
        }

        Integer statusPulse = status & 0x18;    // bit 3,4
        if (statusPulse > 0) {
            if (statusPulse == 0x08) { // 01
                pulseRateRange = 1;  // "exceeds upper limit"
            }
            else if (statusPulse == 0x10) { // 10
                pulseRateRange = 2;  // "below lower limit"
            }
            else { // 11
                pulseRateRange = 3;  // "reserved future use"
            }
        }

        if ((status & 0x20) == 0x20) {
            measurementPosition = 1;        // 1 = Posición incorrecta de la medición
        }

        message += "\"bodyMovement\":" + bodyMovement + ",";
        message += "\"cuffFit\":" + cuffFit  + ",";
        message += "\"irregularPulse\":" + irregularPulse + ",";
        message += "\"pulseRateRange\":" + pulseRateRange + ",";
        message += "\"measurementPosition\":" + measurementPosition  + ",";
        message += "\"status\":" + status + "";

        return message + " }";
    }

    public static String msgGetMeasurementStatus(Integer value) {
        String message = "{ ";

        Integer status = value & 0x3f;

        Integer bodyMovement = 0;            // 0 = sin movimiento del cuerpo
        Integer cuffFit = 0;                 // 0 = el manguito se ajusta correctamente
        Integer irregularPulse = 0;          // 0 = pulso regular
        Integer pulseRateRange = 0;          // 00 = "ien rango"
        Integer measurementPosition = 0;     // 0 = posición correcta de la medición

        if ((status & 0x01) == 0x01) {
            bodyMovement = 1;               // 1 = Movimiento del cuerpo
        }
        if ((status & 0x02) == 0x02) {
            cuffFit = 1;                    // 1 = el manguito está demasiado suelto
        }
        if ((status & 0x04) == 0x04) {
            irregularPulse = 1;             // 1 = Pulso irregular
        }

        Integer statusPulse = status & 0x18;    // bit 3,4
        if (statusPulse > 0) {
            if (statusPulse == 0x08) { // 01
                pulseRateRange = 1;  // "exceeds upper limit"
            }
            else if (statusPulse == 0x10) { // 10
                pulseRateRange = 2;  // "below lower limit"
            }
            else { // 11
                pulseRateRange = 3;  // "reserved future use"
            }
        }

        if ((status & 0x20) == 0x20) {
            measurementPosition = 1;        // 1 = Posición incorrecta de la medición
        }

        message += "\"bodyMovement\":" + bodyMovement + ",";
        message += "\"cuffFit\":" + cuffFit  + ",";
        message += "\"irregularPulse\":" + irregularPulse + ",";
        message += "\"pulseRateRange\":" + pulseRateRange + ",";
        message += "\"measurementPosition\":" + measurementPosition  + ",";
        message += "\"status\":" + status + "";

        return message + " }";
    }

}
