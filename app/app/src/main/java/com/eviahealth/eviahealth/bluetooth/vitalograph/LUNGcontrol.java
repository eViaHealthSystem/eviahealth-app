package com.eviahealth.eviahealth.bluetooth.vitalograph;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.util.Log;

import com.eviahealth.eviahealth.bluetooth.models.Global;
import com.eviahealth.eviahealth.bluetooth.models.BleReferences;
import com.eviahealth.eviahealth.bluetooth.models.EStatusDevice;
import com.eviahealth.eviahealth.utils.log.EVLog;
import com.eviahealth.eviahealth.utils.util;
import com.telit.terminalio.TIOConnection;
import com.telit.terminalio.TIOConnectionCallback;
import com.telit.terminalio.TIOManager;
import com.telit.terminalio.TIOPeripheral;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class LUNGcontrol {

    private static String TAG = "LUNGCONTROL";
    private static final byte STX = 0x02;
    private static final byte ETX = 0x03;
    private static final byte ACK = 0x06;
    private static final byte NAK = 0x15;
    private static final int RSSI_INTERVAL 			  = 1670;
    private static final int MAX_RECEIVED_TEXT_LENGTH = 512;
    private int             TIO_DEFAULT_UART_DATA_SIZE = 20; // Ref. MAX_TX_DATA_SIZE
    private int             TIO_AUTOMATIC_SINGLE_TEST_DATA_SIZE = 71;
    private int             TIO_AUTOMATIC_GET_TIME_DATA_SIZE = 18;
    private Context mcontext;
    private TIOPeripheral mPeripheral;
    private TIOConnection 	mConnection;
    private String          mErrorMessage;
    private int             mRemoteUARTMtuSize;
    private int             mLocalUARTMtuSize;
    private String mDeviceAddress;
    private static  byte[] buffer = null;
    private static  byte[] bufferWRITE = null;
    private int index;
    private EStatusDevice myfunction = EStatusDevice.None;
    CountDownTimer cTimer = null;

    public void initialize(Context context, String address) {
        this.mcontext = context;
        this.mDeviceAddress = address;
        this.index = 0;
        buffer = null;

        EVLog.log(TAG,"mLUNGcontrol.initialize()");

        // Generamos servicio de Vitalograh
        connectPeripheral(address);

        if(mConnection != null) {
            mRemoteUARTMtuSize = mConnection.getRemoteMtuSize();
            mLocalUARTMtuSize  = mConnection.getLocalMtuSize();
        } else {
            mRemoteUARTMtuSize = TIO_DEFAULT_UART_DATA_SIZE;
            mLocalUARTMtuSize  = TIO_DEFAULT_UART_DATA_SIZE;
        }

        Log.e(TAG,"mRemoteUARTMtuSize = " + String.format("MTU %d", mRemoteUARTMtuSize));
        Log.e(TAG,"mLocalUARTMtuSize = " + String.format("MTU %d", mLocalUARTMtuSize));

        if ( mConnection != null ) {
            mConnection.setListener(ListenerCallback);
            startRssiListener();
        }
    }

    private void connectPeripheral(String address) {
        // extract peripheral id (address) from intent
        Log.e(TAG,"connectPeripheral(): " + address);

        // retrieve peripheral instance from TIOManager
        mPeripheral = TIOManager.getInstance().findPeripheral(address);

        if ( mPeripheral != null) {
            mConnection = mPeripheral.getConnection();

            // register callback
            try {

                // display peripheral properties
                Log.e(TAG,"mPeripheral.getName)= "+  mPeripheral.getName() + ", mPeripheral.getAddress(): " + mPeripheral.getAddress());

                if ( mPeripheral.getAdvertisement() != null ) {
                    Log.i(TAG,"mPeripheral.getAdvertisement(): " + mPeripheral.getAdvertisement().toString());
                }
            }
            catch (Exception ex ){
                Log.e(TAG,"! Connect to peripheral failed, "+ ex.toString());
            }
        }
        else {
            Log.e(TAG,"! Peripheral not found");
            Log.e(TAG,"! Peripheral not found");
            Log.e(TAG,"! Peripheral not found");
        }
    }

    public void connect() {
        Log.e(TAG, "connect()");
        try {
            mConnection = mPeripheral.connect(ListenerCallback);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        updateUIState();
    }

    public void destroy(){
        Log.e(TAG, "onDestroy()");
//        EVLog.log(TAG,"mPO60control.onDestroy()");
        if (cTimer != null) {
            cTimer.cancel();
            cTimer = null;
        }

        stopRssiListener();

        if ( mConnection != null ) {
            mConnection.setListener( null );
        }

    }

    public void disconnect(){
        Log.d(TAG, "onDisconnectButtonPressed");

        stopRssiListener();
        try {
            mConnection.disconnect();
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }

    //region :: Funtion TIOManager
    private void startRssiListener() {
        if (mPeripheral.getConnectionState() == TIOConnection.STATE_CONNECTED) {

            Log.d(TAG, "startRssiListener");

            try {
                mConnection.readRemoteRssi(RSSI_INTERVAL);
            } catch (Exception ex) {

            }
        }
    }

    private void stopRssiListener() {
        if ( mConnection != null) {

            Log.d(TAG, "stopRssiListener");

            try {
                mConnection.readRemoteRssi(0);
            }
            catch ( Exception ex) {

            }

        }
    }

    private void updateUIState() {
//        Log.e(TAG, "updateUIState()");
        if ( mConnection != null ) {
            Log.w(TAG,"mConnection.getConnectionState(): " + getIsStateDevice());
        }
    }

    private String getIsStateDevice() {
        if (mConnection.getConnectionState() == TIOConnection.STATE_CONNECTING) {
            return "Connecting";
        }
        else if (mConnection.getConnectionState() == TIOConnection.STATE_CONNECTED) {
            broadcastUpdate(BleReferences.ACTION_BLE_CONNECTED,"{ \"state\": \"Connecting\" }");
            return "Connected";
        }
        else if (mConnection.getConnectionState() == TIOConnection.STATE_DISCONNECTED) {
//            broadcastUpdate(BleReferences.ACTION_BLE_DISCONNECTED,"{ \"state\": \"Disconnected\" }");
            return "Disconnected";
        }
        else if (mConnection.getConnectionState() == TIOConnection.STATE_DISCONNECTING) { return "Disconnecting"; }
        else { return "Unknown"; }
    }
    //endregion

    //region :: TIOConnectionCallback
    private final TIOConnectionCallback ListenerCallback = new TIOConnectionCallback() {
        @Override
        public void onConnected(TIOConnection connection) {
            Log.w(TAG, "ListenerCallback.onConnected()");
            if (mPeripheral.getAdvertisement() != null) {
                Log.e(TAG, ">> onConnected " + mPeripheral.getAdvertisement().toString());
            }

            updateUIState();

            startRssiListener();

            if (!mPeripheral.shallBeSaved()) {
                // save if connected for the first time
                // guardar si es la primera vez que se conecta
                mPeripheral.setShallBeSaved(true);
                TIOManager.getInstance().savePeripherals();
            }
        }

        @Override
        public void onConnectFailed(TIOConnection connection, String errorMessage) {
            Log.w(TAG, "ListenerCallback.onConnectFailed " + errorMessage);
            broadcastUpdate(BleReferences.ACTION_BLE_CONNECT_FAILED,"{ \"state\": \"Unknown\", \"error\": \"" + errorMessage + "\" }");
        }

        @Override
        public void onDisconnected(TIOConnection connection, String errorMessage) {
            Log.w(TAG, "ListenerCallback.onDisconnected" + errorMessage);

            stopRssiListener();
            broadcastUpdate(BleReferences.ACTION_BLE_DISCONNECTED,"{ \"state\": \"Disconnected\", \"error\": \" + errorMessage + \" }");
            updateUIState();
        }

        @Override
        public void onDataReceived(TIOConnection connection, byte[] data) {
//            Log.e(TAG, "ListenerCallback.onDataReceived: " + util.getContentData(data));
//            Log.e(TAG, "onDataReceived len " + data.length);
//            Log.e(TAG, "ListenerCallback.onDataReceived: " + util.byteArrayInHexFormat(data));

            if (buffer == null) {
                buffer = data;
                EVLog.log(TAG,"(1): " + util.byteArrayInHexFormat(data));
            }
            else {
                buffer = util.addAll(buffer, data);
                EVLog.log(TAG, "(2): " + util.byteArrayInHexFormat(data));
            }

            if (buffer != null) {
                if (buffer[0] == STX) {
                    Log.e(TAG,"buffer.length = " + buffer.length);
                    // Single Test Data
                    if (TIO_AUTOMATIC_SINGLE_TEST_DATA_SIZE == buffer.length) {
                        broadcastUpdate(BleReferences.ACTION_BLE_EXTRA_DATA, buffer);
                        buffer = null;
                    }
                }
                else if (buffer[0] == NAK) {
                    Log.e(TAG,"NAK buffer.length = " + buffer.length);
                    buffer = null;
                }
                else if (buffer[0] == ACK) {
                    Log.e(TAG,"ACK buffer.length = " + buffer.length);

                    if (buffer.length == 1) {
                        // Solo se recibe un ACK
                        byte[] resdata = new byte[4];
                        if (myfunction == EStatusDevice.SetTime) {
                            resdata[0] = (byte) 0x47; // 'G'
                            resdata[1] = (byte) 0x56; // 'V'
                            resdata[2] = (byte) 0x53; // 'S'
                            resdata[3] = (byte) 0x54; // 'T'
                            broadcastUpdate(BleReferences.ACTION_BLE_CMD_DATA, resdata);
                        }
                        else if (myfunction == EStatusDevice.ClearMemory) {
                            resdata[0] = (byte) 0x47; // 'G'
                            resdata[1] = (byte) 0x56; // 'V'
                            resdata[2] = (byte) 0x43; // 'C'
                            resdata[3] = (byte) 0x4d; // 'M'
                            broadcastUpdate(BleReferences.ACTION_BLE_CMD_DATA, resdata);
                        }
                        else if (myfunction == EStatusDevice.ResetDefault) {
                            resdata[0] = (byte) 0x47; // 'G'
                            resdata[1] = (byte) 0x56; // 'V'
                            resdata[2] = (byte) 0x52; // 'R'
                            resdata[3] = (byte) 0x44; // 'D'
                            broadcastUpdate(BleReferences.ACTION_BLE_CMD_DATA, resdata);
                        }
                        else if (myfunction == EStatusDevice.ExitRemoteMode) {
                            resdata[0] = (byte) 0x47; // 'G'
                            resdata[1] = (byte) 0x56; // 'V'
                            resdata[2] = (byte) 0x58; // 'X'
                            resdata[3] = (byte) 0x52; // 'R'
                            broadcastUpdate(BleReferences.ACTION_BLE_CMD_DATA, resdata);
                        }

                        setTimeout(0);
                    }
                    else {
                        sendACK();
                        Log.e(TAG, "buffer: " + util.byteArrayInHexFormat(buffer));
                        byte[] datos = util.splitBytes(data, 2, buffer.length - 2);
                        Log.e(TAG, "datos: " + util.byteArrayInHexFormat(datos));
                        broadcastUpdate(BleReferences.ACTION_BLE_CMD_DATA, datos);
                        setTimeout(0);
                    }
                    buffer = null;
                }
                else {
                    buffer = null;
                    Log.e(TAG,"Clear buffer.length = " + buffer.length);
                }
            }

        }

        @Override
        public void onDataTransmitted(TIOConnection connection, int status, int bytesTransferred) {
            Log.e(TAG, "ListenerCallback.onDataTransmitted()");
        }

        @Override
        public void onReadRemoteRssi(TIOConnection connection, int status, int rssi) {
            Log.d(TAG, "ListenerCallback.onRemoteRssi(): " + rssi);
//        mRssi = rssi;
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                mRssiView.setText(Integer.toString(mRssi));
//            }
//        };
//        runOnUiThread(runnable);
        }

        //@Override
        public void onLocalUARTMtuSizeUpdated(TIOConnection connection, int mtuSize) {
            Log.e(TAG, "Local MTU Size = " + mtuSize);
//        mLocalUARTMtuSize = mtuSize;
//        mMtuReceiveSizeTextView.setText(String.format("MTU %d", mLocalUARTMtuSize));
        }

        //@Override
        public void onRemoteUARTMtuSizeUpdated(TIOConnection connection, int mtuSize) {
            Log.e(TAG, "Remote MTU Size = " + mtuSize);
//        mRemoteUARTMtuSize = mtuSize;
//        mMtuSendSizeTextView.setText(String.format("MTU %d", mRemoteUARTMtuSize));
        }
    };
    //endregion

    //region :: BROADCAST FUNCTION

    private void broadcastUpdate(final String action, String message) {
        setTimeout(Global.STATE_DISABLE);
        final Intent intent = new Intent(action);
        intent.putExtra(BleReferences.ACTION_BLE_EXTRA_MESSAGE, message);
        mcontext.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, byte[] values) {
        setTimeout(Global.STATE_DISABLE);
        final Intent intent = new Intent(action);
        intent.putExtra(BleReferences.ACTION_BLE_EXTRA_DATA,values);
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
                    EVLog.log(TAG,"COMMUNICATION_TIMEOUT_CMD");
                    broadcastUpdate(BleReferences.ACTION_BLE_COMMUNICATION_TIMEOUT, "{\"success\":\"fail\",\"function\":\"" + myfunction.toString() + "\"}");
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

    //region :: CONTROL FUNCTION
    public void setClearMemory() {
        Log.e(TAG, "setClearMemory()");

        try {
            myfunction = EStatusDevice.ClearMemory;
            byte[] data = convertTextData("GVCM");
            buffer = null;
            setTimeout(1);
            mConnection.transmit(data);
        } catch (Exception ex) {
            Log.e(TAG,ex.toString() );
        }
    }

    public void getTime() {
        Log.e(TAG, "getTime()");

        try {
            myfunction = EStatusDevice.GetTime;
            byte[] data = convertTextData("GVGT");
            buffer = null;
            setTimeout(1);
            mConnection.transmit(data);
        } catch (Exception ex) {
            Log.e(TAG,ex.toString() );
        }

    }

    public void setTime() {
        Log.e(TAG, "setTime()");

        try {
            myfunction = EStatusDevice.SetTime;
            String text = "GVCM" + getYY() + getMM() + getDD() + getHH() + getMIN() + getSS();
            Log.e(TAG,"text get date: " + text);
            byte[] data = convertTextData(text);
            buffer = null;
            setTimeout(1);
            mConnection.transmit(data);
        } catch (Exception ex) {
            Log.e(TAG,ex.toString() );
        }

    }

    public void ResetDefaults() {
        Log.e(TAG, "ResetDefaults()");

        try {
            myfunction = EStatusDevice.ResetDefault;
            byte[] data = convertTextData("GVRD");
            buffer = null;
            setTimeout(1);
            mConnection.transmit(data);
        } catch (Exception ex) {
            Log.e(TAG,ex.toString() );
        }
    }

    public void ExitRemoteMode() {
        Log.e(TAG, "ExitRemoteMode()");

        try {
            myfunction = EStatusDevice.ExitRemoteMode;
            byte[] data = convertTextData("GVXR");
            buffer = null;
            setTimeout(1);
            mConnection.transmit(data);
        } catch (Exception ex) {
            Log.e(TAG,ex.toString() );
        }
    }
    private byte[] convertTextData(String text) {
        try {
            String charsetName = "windows-1252";
            byte[] data = text.getBytes(charsetName);

            byte[] data2 = new byte[data.length + 3];
            data2[0] = 0x02;
            byte bcc0 = data2[0];

            for (int i=0; i<data.length; i++) {
                data2[i+1] = data[i];
                bcc0 ^= data2[i+1];
            }
            data2[data.length+1] = 0x03;
            bcc0 ^= data2[data.length+1];

            data2[data.length+2] = bcc0;

//            Log.e(TAG,"bcc0: " + bcc0);
            Log.e(TAG,"data: " + util.byteArrayInHexFormat(data2));

            return data2;
        } catch (Exception ex) {
            Log.e(TAG,ex.toString() );
            return null;
        }
    }

    private void sendACK() {
        try {
            mConnection.transmit(new byte[]{0x06});
        } catch (Exception ex) {
            Log.e(TAG,ex.toString() );
        }
    }
    //endregion

    //region :: PARSE DATAS

    public String parseGetTime(byte[] data) {
        String result = "20";

        result = result + (char)data[4] + (char)data[5] + "-"; // Year
        result = result + (char)data[6] + (char)data[7] + "-"; // Month
        result = result + (char)data[8] + (char)data[9] + " "; // Day

        result = result + (char)data[10] + (char)data[11] + ":"; // Hour
        result = result + (char)data[12] + (char)data[13] + ":"; // Minute
        result = result + (char)data[14] + (char)data[15]; // Second

        return result;
    }

    //endregion

    //region :: CALENDAR
    private String getYY(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        return dateFormat.format(calendar.getTime());
    }

    private String getMM(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        return dateFormat.format(calendar.getTime());
    }

    private String getDD(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        return dateFormat.format(calendar.getTime());
    }

    private String getHH(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        return dateFormat.format(calendar.getTime());
    }

    private String getMIN(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("mm", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        return dateFormat.format(calendar.getTime());
    }

    private String getSS(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("SS", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        return dateFormat.format(calendar.getTime());
    }
    //endregion
}
