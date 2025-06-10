package com.eviahealth.eviahealth.meeting.initsdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.eviahealth.eviahealth.meeting.models.InitAuthSDKCallback;

import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomSDKInitParams;
import us.zoom.sdk.ZoomSDKInitializeListener;
import us.zoom.sdk.ZoomSDKRawDataMemoryMode;

/**
 * Init and auth zoom sdk first before using SDK interfaces
 */
public class InitAuthSDKHelper implements ZoomSDKInitializeListener {

    private final static String TAG = "InitAuthSDKHelper";
    private final static String WEB_DOMAIN = "zoom.us";
    private static InitAuthSDKHelper mInitAuthSDKHelper;

    private ZoomSDK mZoomSDK;

    private InitAuthSDKCallback mInitAuthSDKCallback;

    private InitAuthSDKHelper() {
        mZoomSDK = ZoomSDK.getInstance();
    }

    public synchronized static InitAuthSDKHelper getInstance() {
        mInitAuthSDKHelper = new InitAuthSDKHelper();
        return mInitAuthSDKHelper;
    }

    /**
     * init sdk method
     */
    public void initSDK(Context context, InitAuthSDKCallback callback) {

        if (!mZoomSDK.isInitialized()) {
            mInitAuthSDKCallback = callback;

            Log.e(TAG,"initSDK()");

            SharedPreferences prefs = context.getSharedPreferences("VariableEntorno", Context.MODE_PRIVATE);
            String sdk_appkey = prefs.getString("sdk_appkey", "");
            String sdk_appsecret = prefs.getString("sdk_appsecret", "");

            Log.e(TAG,"sdk_appkey: "  + sdk_appkey + ", sdk_appsecret: " + sdk_appsecret);

            ZoomSDKInitParams initParams = new ZoomSDKInitParams();
            String jwttoken = JWTTokenGenerator.generateToken(sdk_appkey, sdk_appsecret);
            Log.e(TAG,"jwttoken: " + jwttoken);
            initParams.jwtToken = jwttoken;

            initParams.enableLog = false;               //true
            initParams.enableGenerateDump = false;      //true
            initParams.logSize = 5;
//            initParams.domain=AuthConstants.WEB_DOMAIN;
            initParams.domain = WEB_DOMAIN;
            initParams.videoRawDataMemoryMode = ZoomSDKRawDataMemoryMode.ZoomSDKRawDataMemoryModeStack;
            mZoomSDK.initialize(context, this, initParams);
        }
    }

    /**
     * init sdk callback
     *
     * @param errorCode         defined in {@link us.zoom.sdk.ZoomError}
     * @param internalErrorCode Zoom internal error code
     */
    @Override
    public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
        Log.i(TAG, "onZoomSDKInitializeResult, errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);

        if (mInitAuthSDKCallback != null) {
            mInitAuthSDKCallback.onZoomSDKInitializeResult(errorCode, internalErrorCode);
        }
    }

    // El token jwt de ZOOM ha caducado; genere un nuevo token jwt.
    @Override
    public void onZoomAuthIdentityExpired() {
        Log.e(TAG,"onZoomAuthIdentityExpired in init");
    }

    public void reset(){
        mInitAuthSDKCallback = null;
    }
}
