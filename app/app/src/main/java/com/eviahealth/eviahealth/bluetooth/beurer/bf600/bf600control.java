package com.eviahealth.eviahealth.bluetooth.beurer.bf600;

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
import com.eviahealth.eviahealth.bluetooth.beurer.po60.po60GattAttributes;
import com.eviahealth.eviahealth.bluetooth.beurer.po60.po60manager;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.util;
import com.eviahealth.eviahealth.models.vitalograph.Patient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class bf600control {
    private static String TAG = "BF600CONTROL";

    private int isconnect = Global.STATE_DISCONNECTED;
    private Context mcontext;
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;

    List<BluetoothGattCharacteristic> ListCharacteristics = new ArrayList<>();
    private int index;
    private static  byte[] buffer = null;
    private static  byte[] bufferWRITE = null;

    CountDownTimer cTimer = null;
    private Integer nrecords = null;
    private Integer lendownload = null;
    Patient patient = null;
    private bf600Funtion manager =  bf600Funtion.None;

    private Integer age = 0;
    private String gender = "";
    private Integer height = 0;
    private Integer activitylevel = 1;
    private Integer database = 0;
    private Double weight;
    private Integer impedance;

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
        String propierties = bf600GattAttributes.getPropierties(characteristic.getUuid().toString());

        Log.e("setIndicateNotificate", "characteristic: " + characteristic.getUuid().toString() + ", propierties: " + propierties);

        UUID uuidDescriptor = bf600Notify.getDescription(characteristic.getUuid().toString());
        if (uuidDescriptor != null) {
            Log.e("setIndicateNotificate", "uuidDescriptor: " + uuidDescriptor.toString());
        }

        // Solicitamos activación
//        mBluetoothLeService.setActivatePropiertiesCharacteristic(characteristic,propierties, po60GattAttributes.UUID_DESCRIPTOR_CHARACTERISTIC_02);
        mBluetoothLeService.setActivatePropiertiesCharacteristic(characteristic,propierties, uuidDescriptor);
    }

    public void enableNotifications(UUID serviceUuid, UUID characteristicUuid) {
        mBluetoothLeService.enableNotifications(serviceUuid, characteristicUuid);
    }

    public boolean setWriteCommand(UUID uuid, byte[] data) {
//        EVLog.log(TAG,"setWriteCommand()");

        BluetoothGattCharacteristic gattCharacteristic = mBluetoothLeService.getSearchCharacteristic(uuid);
        if (gattCharacteristic == null) {
            EVLog.log(TAG,"setWriteCommand uuid: " + uuid.toString());
            return false;
        }

        setTimeout(Global.STATE_ENABLE); // Habilitamos Timeout de recepción
        bufferWRITE = data;
        mBluetoothLeService.setWriteCharacteristic(gattCharacteristic, data);
        return true;
    }

    public bf600Funtion getFuntionManager() { return this.manager; }

    //region :: BROADCAST RECEIVER
    private BroadcastReceiver mReceiverControl = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            String uuid;

//            Log.e(TAG, "mReceiverControl(): " + action);
//            EVLog.log(TAG,"RECEIVER ACTION: " + action);

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

                    if (uuidRead.equals(bf600GattAttributes.CURRENT_TIME_CHARACTERISTIC_UUID.toString())) {
                        broadcastUpdate(BeurerReferences.ACTION_BEURER_BF600_UPDATE_TIME, bf600ParseData.ParseCurrentTime(dataRD));
                    }
                    else if (uuidRead.equals(bf600GattAttributes.DATE_OF_BIRTH_USER_CHARACTERISTIC_UUID.toString())) {
                        Log.e(TAG, "DATE_OF_BIRTH_USER_CHARACTERISTIC_UUID");
                        age = bf600ParseData.calculateAge(dataRD);
                        getGender();
                    }
                    else if (uuidRead.equals(bf600GattAttributes.GENDER_USER_CHARACTERISTIC_UUID.toString())) {
                        Log.e(TAG, "GENDER_USER_CHARACTERISTIC_UUID");
                        gender = bf600ParseData.ParseGender(dataRD);
                        getHeight();
                    }
                    else if (uuidRead.equals(bf600GattAttributes.HEIGHT_USER_CHARACTERISTIC_UUID.toString())) {
                        Log.e(TAG, "HEIGHT_USER_CHARACTERISTIC_UUID");
                        height = bf600ParseData.ParseHeight(dataRD);
                        broadcastUpdate(BeurerReferences.ACTION_BEURER_BF600_UPDATE_USER, bf600ParseData.ParseUserInfo(age, gender, height));
                    }
                    else if (uuidRead.equals(bf600GattAttributes.DATABASE_CHANGE_INCREMENT_USER_CHARACTERISTIC_UUID.toString())) {
                        database = util.BytesToInt(dataRD);
                        Log.e(TAG, "DATABASE_CHANGE_INCREMENT_USER: " + database);
                        database += 1;
                        setDataBaseIncrementChanged(database);
                    }
                    else if (uuidRead.equals(bf600GattAttributes.BATTERY_LEVEL_CHARACTERISTIC_UUID.toString())) {
                        Log.e(TAG, "BATTERY_LEVEL: " + util.byteArrayInHexFormat(dataRD));
                        broadcastUpdate(BeurerReferences.ACTION_BEURER_BF600_BATTERY_LEVEL, bf600ParseData.ParseBatteryLevel(dataRD));
                    }
                    //endregion
                    break;

                case BluetoothLeService.ACTION_WRITE_CHARACTERISTIC:
                    //region :: ACTION_WRITE_CHARACTERISTIC
                    byte[] dataW = extras.getByteArray(mBluetoothLeService.EXTRA_DATA_BYTES_WRITE);
                    EVLog.log(TAG,"WRITE_CHARACTERISTIC_DATA: " + util.byteArrayInHexFormat(dataW));

                    if (manager == bf600Funtion.SetCurrentTime) {
                        EVLog.log(TAG,"ACTION_BEURER_BF600_UPDATE_TIME");
                        getCurrentTime();
                    }
                    else if (manager == bf600Funtion.SetDateOfBirth) {
                        EVLog.log(TAG,"ACTION_BF600_DATA_OF_BIRTH");
                        setGender(patient.getGender());
                    }
                    else if (manager == bf600Funtion.SetGender) {
                        EVLog.log(TAG,"ACTION_BF600_GENDER_USER");
                        setHeight(patient.getHeight());
                    }
                    else if (manager == bf600Funtion.SetHeight) {
                        EVLog.log(TAG,"ACTION_BF600_HEIGHT_USER");
                        setActivityLevel(0);
                    }
                    else if (manager == bf600Funtion.ActivityLevel) {
                        EVLog.log(TAG,"ACTION_BF600_ACTIVITY_LEVEL");
//                        ScaleSetting();
                        getDatabaseIncrementChanged();
                    }
                    else if (manager == bf600Funtion.SetDatabase) {
                        ScaleSetting();
//                        broadcastUpdate(BeurerReferences.ACTION_BEURER_BF600_UPDATE_USER, bf600ParseData.ParseUserInfo(age, gender, height));
                    }
                    else if (manager == bf600Funtion.SetScaleSetting) {
                        EVLog.log(TAG,"ACTION_BF600_SCALE_SETTING");
                        getDateOfBirthday();
                    }


                    //endregion
                    break;

                case BluetoothLeService.ACTION_CHARACTERISTIC_CHANGED:
                    //region :: ACTION_CHARACTERISTIC_CHANGED
                    byte[] dataRx = extras.getByteArray(mBluetoothLeService.EXTRA_DATA_BYTES);
                    uuid = extras.getString(mBluetoothLeService.EXTRA_UUID);
                    byte command;

                    Log.e(TAG, "ACTION_CHARACTERISTIC_CHANGED uuid: " + uuid);

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
                    if (buffer.length > 0) {
                        if (uuid.equals(bf600GattAttributes.USER_CONTROL_POINT_CHARACTERISTIC_UUID.toString())) {
                            // { 0x20, 0x01, 0x01, 0x01 }
                            String message = bf600ParseData.ResponseUserControlPoint(buffer);
                            buffer = null;
                            broadcastUpdate(BeurerReferences.ACTION_BEURER_BF600_USER_CONTROL_POINT, message);
                        }
                        else if (uuid.equals(bf600GattAttributes.USER_LIST_CHARACTERISTIC_UUID.toString())) {
                            // LISTA USUARIOS OBTENIDA
                            Log.e(TAG, "USUARIOS: " + (buffer.length / 12));
                            if ((buffer.length % 12) != 0) {
                                String message = bf600ParseData.ParseUserList(buffer);
                                buffer = null;
                                broadcastUpdate(BeurerReferences.ACTION_BEURER_BF600_USER_LIST, message);
                            }
                        }
                        else if (uuid.equals(bf600GattAttributes.WEIGHT_MEASUREMENT_CHARACTERISTIC_UUID.toString())) {
                            // MEDIA DE PESO OBTENIDA
                            String message = bf600ParseData.ParseWeightMeasurement(buffer);
                            buffer = null;
                            weight = util.getDoubleValueJSON(message,"weight");
                            height = util.getIntValueJSON(message,"height");
                            broadcastUpdate(BeurerReferences.ACTION_BEURER_BF600_WEIGHT_MEASUREMENT, message);
                        }
                        else if (uuid.equals(bf600GattAttributes.BODY_COMPOSITION_CHARACTERISTIC_UUID.toString())) {
                            // MEDIDA DE LA COMPOSICIÓN CORPORAL OBTENIDA
                            String message = bf600ParseData.ParseBodyComposition(buffer, weight.floatValue());
                            buffer = null;
                            impedance = util.getIntValueJSON(message, "impedance");
                            broadcastUpdate(BeurerReferences.ACTION_BEURER_BF600_BODY_COMPOSITION, message);
                        }
                        else if (uuid.equals(bf600GattAttributes.TAKE_MEASUREMENT_CHARACTERISTIC_UUID.toString())) {
                            // MEDICIÓN FINALIZADA
                            String message = bf600ParseData.ParseTakeMeasurement(buffer);
                            Log.e(TAG, "message: " + message);
                            buffer = null;
                            broadcastUpdate(BeurerReferences.ACTION_BEURER_BF600_TAKE_MEASUREMENT, message);
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
                    broadcastUpdate(BeurerReferences.ACTION_BEURER_WRITE_FAIL, "{\"success\":\"fail\",\"function\":\""+ getFuntionManager().toString() + "\"}");
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

//    private void setTimeout(int action) {
//
//        Integer timeout = 1000 * 4;
//        if (action == 1) {
//            // Start Timeout
//            cTimer = new CountDownTimer(timeout, 1000) {
//                @Override
//                public void onTick(long millisUntilFinished) {}
//
//                @Override
//                public void onFinish() {
//                    cTimer.cancel();
//                    cTimer = null;
//                    buffer = null;
//                    EVLog.log(TAG,"COMMUNICATION_TIMEOUT");
//                    broadcastUpdate(BeurerReferences.ACTION_BEURER_COMMUNICATION_TIMEOUT, "{\"success\":\"fail\",\"function\":\"" + lastFunction + "\"}");
//                }
//            };
//            EVLog.log(TAG,"Timeout: Start");
//            cTimer.start();
//        }
//        else if (action > 1) {
//            // Reload
//            if (cTimer == null) return;
//            cTimer.cancel();
//            EVLog.log(TAG,"Timeout: Reload");
//            cTimer.start();
//        }
//        else {
//            // Finish
//            if (cTimer == null) return;
//            cTimer.cancel();
//            cTimer = null;
//            EVLog.log(TAG,"Timeout: stop");
//        }
//
//    }

    private void setTimeout(int action) {
       return;
    }

    //endregion

    //region :: Funciones

    //region >> Time
    public void getCurrentTime() {
        manager = bf600Funtion.GetCurrentTime;
        EVLog.log(TAG,manager.toString());
        mBluetoothLeService.readCharacteristicFromService(bf600GattAttributes.CURRENT_TIME_SERVICE_UUID, bf600GattAttributes.CURRENT_TIME_CHARACTERISTIC_UUID);
    }

    public void setCurrentTime() {
        manager = bf600Funtion.SetCurrentTime;
        EVLog.log(TAG,manager.toString());

        Calendar instance = Calendar.getInstance();
        int year = instance.get(Calendar.YEAR);      // year
        int month = instance.get(Calendar.MONTH) + 1;       // month
        int day = instance.get(Calendar.DAY_OF_MONTH);      // day
        int hour = instance.get(Calendar.HOUR_OF_DAY);      // hour
        int minute = instance.get(Calendar.MINUTE);         // minute
        int second = instance.get(Calendar.SECOND);         // second
        int dayOfWeek = instance.get(Calendar.DAY_OF_WEEK); // day of week  (domingo = 0, lunes = 1, etc)

        if (dayOfWeek == 0) { dayOfWeek = 7; }

        byte[] any = util.intToBytes(year);

        // Set Characteristic and command and values
        byte[] values = new byte[10];
        values[0] = any[1];                     // year parte alta
        values[1] = any[0];                     // year parte baja
        values[2] = (byte)(month & 0x0f);       // Month
        values[3] = (byte)(day & 0x1f);         // Day
        values[4] = (byte)(hour & 0x1f);        // Hour
        values[5] = (byte)(minute & 0x3f);      // Minute
        values[6] = (byte)(second & 0x3f);      // Second
        values[7] = (byte)(dayOfWeek & 0x0f);   // Day of week
        values[8] = 0x00;                       //
        values[9] = 0x00;                       //

        buffer = null;
        setWriteCommand(bf600GattAttributes.CURRENT_TIME_CHARACTERISTIC_UUID, values);
    }
    //endregion

    public void CreateUser() {
        manager = bf600Funtion.CreateUser;
        EVLog.log(TAG,manager.toString());
        buffer = null;

        byte[] values = new byte[3];
        values[0] = 0x01;               // OP 0x01 Crear nuevo usuario
        values[1] = (byte) 0xd2;        // Consent Code 1234 0x04d2
        values[2] = (byte) 0x04;        // parte baja
        setWriteCommand(bf600GattAttributes.USER_CONTROL_POINT_CHARACTERISTIC_UUID, values);
    }

    public void UserActive(int user_index) {
        manager = bf600Funtion.UserActive;
        EVLog.log(TAG,manager.toString() + ", user_index: " + user_index);
        buffer = null;

        byte[] values = new byte[4];
        values[0] = 0x02;               // OP 0x02 Procedimiento de consentimiento
        values[1] = (byte) user_index;  // user index
        values[2] = (byte) 0xd2;        // Consent Code 1234 0x04d2
        values[3] = (byte) 0x04;        // parte baja
        setWriteCommand(bf600GattAttributes.USER_CONTROL_POINT_CHARACTERISTIC_UUID, values);
    }

    // Borra el usuario asignado actualmente
    public void DeleteUser() {
        manager = bf600Funtion.DeleteUser;
        EVLog.log(TAG,manager.toString());
        buffer = null;

        byte[] values = new byte[1];
        values[0] = 0x03;               // OP 0x03 eliminar datos de usuario
        setWriteCommand(bf600GattAttributes.USER_CONTROL_POINT_CHARACTERISTIC_UUID, values);
    }

    //region :: USER DATA
    public void setDateOfBirth(int age) {
        manager = bf600Funtion.SetDateOfBirth;
        EVLog.log(TAG,manager.toString());
        buffer = null;

        byte[] values = calcularFechaNacimiento(age);
        setWriteCommand(bf600GattAttributes.DATE_OF_BIRTH_USER_CHARACTERISTIC_UUID, values);
    }
    public byte[] calcularFechaNacimiento(int edad) {
        // Obtener la fecha actual
        Calendar fechaActual = Calendar.getInstance();

        // Calcular el año de nacimiento
        int añoNacimiento = fechaActual.get(Calendar.YEAR) - edad;

        // Obtener el mes y el día actuales
        int mesActual = fechaActual.get(Calendar.MONTH) + 1; // Los meses en Calendar empiezan desde 0
        int diaActual = fechaActual.get(Calendar.DAY_OF_MONTH);

        // Crear el array de bytes
        byte[] fechaNacimiento = new byte[4];

        // Asignar los valores al array de bytes
        fechaNacimiento[0] = (byte) (añoNacimiento);      // Parte baja del año
        fechaNacimiento[1] = (byte) (añoNacimiento >> 8); // Parte alta del año
        fechaNacimiento[2] = (byte) mesActual;            // Byte del mes
        fechaNacimiento[3] = (byte) diaActual;            // Byte del día

        return fechaNacimiento;
    }
    public void setGender(String genero) {
        manager = bf600Funtion.SetGender;
        EVLog.log(TAG,manager.toString());
        buffer = null;

        int gender = (genero.equals("Hombre")) ? 0 : 1;

        byte[] values = new byte[1];
        values[0] = (byte) gender;
        setWriteCommand(bf600GattAttributes.GENDER_USER_CHARACTERISTIC_UUID, values);
    }
    public void setHeight(int height) {
        manager = bf600Funtion.SetHeight;
        EVLog.log(TAG,manager.toString());
        buffer = null;

        byte[] values = new byte[1];
        values[0] = (byte) height;
        setWriteCommand(bf600GattAttributes.HEIGHT_USER_CHARACTERISTIC_UUID, values);
    }
    public void setActivityLevel(int active) {
        manager = bf600Funtion.ActivityLevel;
        EVLog.log(TAG,manager.toString());
        buffer = null;

        byte[] values = new byte[1];
        values[0] = (byte) active;
        setWriteCommand(bf600GattAttributes.ACTIVITY_LEVEL_USER_CHARACTERISTIC_UUID, values);
    }
    public void getDateOfBirthday() {
        manager = bf600Funtion.GetDateOfBirth;
        EVLog.log(TAG,manager.toString());
        buffer = null;
        mBluetoothLeService.readCharacteristicFromService(bf600GattAttributes.USER_DATA_SERVICE_UUID, bf600GattAttributes.DATE_OF_BIRTH_USER_CHARACTERISTIC_UUID);
    }
    public void getGender() {
        manager = bf600Funtion.GetGender;
        EVLog.log(TAG,manager.toString());
        buffer = null;
        mBluetoothLeService.readCharacteristicFromService(bf600GattAttributes.USER_DATA_SERVICE_UUID, bf600GattAttributes.GENDER_USER_CHARACTERISTIC_UUID);
    }
    public void getHeight() {
        manager = bf600Funtion.GetHeight;
        EVLog.log(TAG,manager.toString());
        buffer = null;
        mBluetoothLeService.readCharacteristicFromService(bf600GattAttributes.USER_DATA_SERVICE_UUID, bf600GattAttributes.HEIGHT_USER_CHARACTERISTIC_UUID);
    }
    public void getUserIndex() {
        manager = bf600Funtion.GetUserIndex;
        EVLog.log(TAG, manager.toString());
        buffer = null;
        mBluetoothLeService.readCharacteristicFromService(bf600GattAttributes.USER_DATA_SERVICE_UUID, bf600GattAttributes.USER_INDEX_CHARACTERISTIC_UUID);
    }
    //endregion

    //region :: DATABASE INCREMENT
    public void setDataBaseIncrementChanged(int valor) {
        manager = bf600Funtion.SetDatabase;
        EVLog.log(TAG,manager.toString());
        buffer = null;

        byte[] values = intToByteArray(valor);
        setWriteCommand(bf600GattAttributes.DATABASE_CHANGE_INCREMENT_USER_CHARACTERISTIC_UUID, values);
    }
    public void getDatabaseIncrementChanged() {
        manager = bf600Funtion.GetDatabase;
        EVLog.log(TAG,manager.toString());
        buffer = null;
        mBluetoothLeService.readCharacteristicFromService(bf600GattAttributes.USER_DATA_SERVICE_UUID, bf600GattAttributes.DATABASE_CHANGE_INCREMENT_USER_CHARACTERISTIC_UUID);
    }
    private byte[] intToByteArray(int valor) {
        byte[] bytes = new byte[4];

        bytes[0] = (byte) (valor);         // LSO
        bytes[1] = (byte) (valor >> 8);
        bytes[2] = (byte) (valor >> 16);
        bytes[3] = (byte) (valor >> 24);   // MSO

        return bytes;
    }
    //endregion

    public void TakeMeasurement() {
        manager = bf600Funtion.TakeMeasurement;
        EVLog.log(TAG,manager.toString());
        buffer = null;

        byte[] values = new byte[1];
        values[0] = (byte) 0x00;
        setWriteCommand(bf600GattAttributes.TAKE_MEASUREMENT_CHARACTERISTIC_UUID, values);
    }

    //region :: SCALE SETTING
    public void ScaleSetting() {
        manager = bf600Funtion.SetScaleSetting;
        EVLog.log(TAG,manager.toString());
        buffer = null;

        byte[] values = new byte[8];
        values[0] = (byte) 0xFF;    // Language (no compatible con bf600 valor 0xff)
        values[1] = (byte) 0x00;    // 0x00=Kg, 0x01 libras, 0x02=St
        values[2] = (byte) 0xFF;    // Formato Hora (no compatible con bf600 valor 0xff)
        values[3] = (byte) 0xFF;    // Guest Mode (no compatible con bf600 valor 0xff)
        values[4] = (byte) 0x32;    // Umbral de peso para asignar al usuario. Resolucion 0.1Kg, por defecto +-3Kg(0x1E) (parte baja) pongo +-5kg
        values[5] = (byte) 0x00;    // (parte alta)
        values[6] = (byte) 0xFF;    // Umbral grasa corporal (no se usa en bf600) (parte baja)
        values[7] = (byte) 0xFF;    // (parte alta)
        setWriteCommand(bf600GattAttributes.SCALE_SETTINGS_CHARACTERISTIC_UUID, values);
    }
    public void getScaleSetting() {
        manager = bf600Funtion.GetScaleSetting;
        EVLog.log(TAG, manager.toString());
        buffer = null;
        mBluetoothLeService.readCharacteristicFromService(bf600GattAttributes.CUSTOM_SCALE_SETTINGS_SERVICE_UUID, bf600GattAttributes.SCALE_SETTINGS_CHARACTERISTIC_UUID);
    }
    //endregion

    public void UserList() {
        manager = bf600Funtion.UserList;
        EVLog.log(TAG,manager.toString());
        buffer = null;

        byte[] values = new byte[1];
        values[0] = (byte) 0x00;
        setWriteCommand(bf600GattAttributes.USER_LIST_CHARACTERISTIC_UUID, values);
    }
    public void UpdateUserData(Patient patient) {
        this.patient = patient;
        setDateOfBirth(patient.getAge()); // Inicia secuencia
    }

    public void getServiceChange() {
        manager = bf600Funtion.GetBatteryLevel;
        EVLog.log(TAG,manager.toString());
        buffer = null;
        mBluetoothLeService.readCharacteristicFromService(bf600GattAttributes.GENERIC_ATTRIBUTE_SERVICE_UUID, bf600GattAttributes.SERVICE_CHANGE_CHARACTERISTIC_UUID);
    }

    public void getBatteryLevel() {
        manager = bf600Funtion.GetServiceChange;
        EVLog.log(TAG,manager.toString());
        buffer = null;
        mBluetoothLeService.readCharacteristicFromService(bf600GattAttributes.BATTERY_LEVEL_SERVICE_UUID, bf600GattAttributes.BATTERY_LEVEL_CHARACTERISTIC_UUID);
    }

    //endregion
}
