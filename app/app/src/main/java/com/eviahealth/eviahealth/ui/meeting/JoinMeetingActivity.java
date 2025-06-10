package com.eviahealth.eviahealth.ui.meeting;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Gravity;							
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;							   							
import android.widget.ProgressBar;
import android.widget.TextView;

import com.eviahealth.eviahealth.R;
import com.eviahealth.eviahealth.meeting.models.InMeetingServiceListenerAdapter;
import com.eviahealth.eviahealth.meeting.models.MErrorCodes;
import com.eviahealth.eviahealth.meeting.models.MStatusMeeting;
import com.eviahealth.eviahealth.meeting.models.MZoomError;
import com.eviahealth.eviahealth.meeting.models.InitAuthSDKCallback;
import com.eviahealth.eviahealth.meeting.initsdk.InitAuthSDKHelper;
import com.eviahealth.eviahealth.ui.BaseActivity;
import com.eviahealth.eviahealth.utils.PermissionUtils;

import java.util.List;

import us.zoom.sdk.InMeetingNotificationHandle;
import us.zoom.sdk.InMeetingService;
import us.zoom.sdk.InMeetingServiceListener;
import us.zoom.sdk.JoinMeetingOptions;
import us.zoom.sdk.JoinMeetingParams;
import us.zoom.sdk.MeetingParameter;
import us.zoom.sdk.MeetingServiceListener;
import us.zoom.sdk.MeetingStatus;

import us.zoom.sdk.MeetingViewsOptions;
import us.zoom.sdk.ZoomApiError;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.ZoomSDK;

public class JoinMeetingActivity extends BaseActivity implements InitAuthSDKCallback, MeetingServiceListener {

    final String TAG = "JoinMeetingActivity";
    private ZoomSDK mZoomSDK;
    MStatusMeeting statusMeeting = MStatusMeeting.None;
    String textfail = "";

    ProgressBar circulodescarga;
    TextView txtStatus;
    Button buttonAceptar;
    ImageView imageZoom, imageZoomProgress;							   
    CountDownTimer cTimer = null;            // timeout de conexión con zoom
    CountDownTimer cTimerMeeting = null;     // timerOut para finalizar una reunión en curso antes de que se finalicen los 40 minutos gratuitos de zoom
    CountDownTimer cTimerEndMeeting = null;  // timerOut para finalizar una reunión máximo 51 minutos
    CountDownTimer cTimerFinish = null;     //
    Boolean finishMeeting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_join_meeting);
        Log.e(TAG,"onCreate()");

        PermissionUtils.requestAll(this);

        //region :: views
        txtStatus = findViewById(R.id.textStatusJoin);
        txtStatus.setTextSize(28);
        txtStatus.setVisibility(View.VISIBLE);
        setTextSatus("Iniciando videollamada\n\nEspere unos segundos", Gravity.CENTER_HORIZONTAL);

        circulodescarga = findViewById(R.id.loadProgressBar);
        setVisibleView(circulodescarga, View.VISIBLE);

        imageZoom = findViewById(R.id.imageZoom);
        imageZoom.setVisibility(View.INVISIBLE);

        imageZoomProgress = findViewById(R.id.imageZoomProgress);
        imageZoomProgress.setVisibility(View.VISIBLE);

        buttonAceptar = findViewById(R.id.buttonAceptar);
        buttonAceptar.setVisibility(View.INVISIBLE);
        buttonAceptar.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   finish();
               }
        });
        //endregion

        //region :: TimerOut Conexción con ZOOM (30 segudos)
        cTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimer.cancel();
                cTimer = null;
                setVisibleView(circulodescarga, View.INVISIBLE);
                setVisibleView(imageZoomProgress, View.INVISIBLE);
                setVisibleView(imageZoom, View.VISIBLE);
                setVisibleView(buttonAceptar,View.VISIBLE);
                setTextSatus("TimeOut de conexión con servidor.", Gravity.LEFT);
                Log.e(TAG,"TimeOut de conexión con servidor.");
            }
        };
        cTimer.start();
        //endregion

        //region :: TimerOut Conexión con ZOOM (9 MINUTOS Y 50 segudos = 9*60+50 = 590)
        // Se utiliza para cuando una reunión está a punto de expirar finalizarla para que no salga un catertel de zoom.
        // Este Timer no debería saltar nunca ya que el equivalente de prescriptor salta antes.
        long timeout = 1000 * 590;
        cTimerMeeting = new CountDownTimer(timeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimerMeeting.cancel();
                cTimerMeeting = null;
                Log.e(TAG,"TIMEOIUT TIME MEETING");

                // Obtenemos el servicio de reuniones
                InMeetingService inMeetingService = ZoomSDK.getInstance().getInMeetingService();
                // Salimos de la reunión actual para todos
                if (inMeetingService != null) {
                    inMeetingService.leaveCurrentMeeting(false);
                }
            }
        };
        //endregion

        //region :: TimerOut Meeting Máximo con ZOOM (10 min de entrada a la reunión + 40 máximo de reunión + offset)
        long timeoutEndMeeting = 1000 * 60 * 51; // 51 minutos (10 min de entrada a la reunión + 40 máximo de reunión + offset)
        cTimerEndMeeting = new CountDownTimer(timeoutEndMeeting, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimerEndMeeting.cancel();
                cTimerEndMeeting = null;
                Log.e(TAG,"TIMEOUT END MEETING");

                // Obtenemos el servicio de reuniones
                InMeetingService inMeetingService = ZoomSDK.getInstance().getInMeetingService();
                // Salimos de la reunión actual para todos
                if (inMeetingService != null) {
                    inMeetingService.leaveCurrentMeeting(false);
                }
            }
        };
        cTimerEndMeeting.start();
        //endregion

        //region :: Contador para salir de la actividad (6 segudos)
        long tfin = 1000 * 6;
        cTimerFinish = new CountDownTimer(tfin, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                cTimerFinish.cancel();
                cTimerFinish = null;
                Log.e(TAG,"ADIOS");
                finish();
            }
        };
        //endregion

        mZoomSDK = ZoomSDK.getInstance();
        if (mZoomSDK.isLoggedIn()) {
            Log.e(TAG,"mZoomSDK.isLoggedIn(): true");
            errDetectZoom("Fallo iniciando el SDK de ZoomS");
            finish();
            return;
        }

        InitAuthSDKHelper.getInstance().initSDK(this, this);

        if (mZoomSDK.isInitialized()) {
            Log.e(TAG,"mZoomSDK.isInitialized(): true");
            lanzarMeeting();
        } else {
            Log.e(TAG,"mZoomSDK.isInitialized(): false");
        }
    }

    InMeetingNotificationHandle handle=new InMeetingNotificationHandle() {

        @Override
        public boolean handleReturnToConfNotify(Context context, Intent intent) {
            Log.e(TAG,"handleReturnToConfNotify()");

//            intent = new Intent(context, MyMeetingActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//            if(!(context instanceof Activity)) {
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            }
//            intent.setAction(InMeetingNotificationHandle.ACTION_RETURN_TO_CONF);
//            context.startActivity(intent);
            return true;
        }
    };

    private void lanzarMeeting() {

//        ZoomSDK.getInstance().getZoomUIService().setNewMeetingUI(CustomNewZoomUIActivity.class);

        ZoomSDK.getInstance().getZoomUIService().disablePIPMode(true);
        ZoomSDK.getInstance().getZoomUIService().hideMeetingInviteUrl(true);

        ZoomSDK.getInstance().getMeetingSettingsHelper().enable720p(false);

        ZoomSDK.getInstance().getMeetingSettingsHelper().setAutoConnectVoIPWhenJoinMeeting(true);
        // Deshabilitar/Habilitar el cuadro de diálogo de vista previa de video al unirse a una reunión de video
        ZoomSDK.getInstance().getMeetingSettingsHelper().disableShowVideoPreviewWhenJoinMeeting(true);
        // Habilitar/Deshabilitar mostrar el tiempo transcurrido de la reunión
        ZoomSDK.getInstance().getMeetingSettingsHelper().enableShowMyMeetingElapseTime(false);
        // Establezca si habilitar la opción MOSTRAR SIEMPRE LOS CONTROLES DE LA REUNIÓN.
        ZoomSDK.getInstance().getMeetingSettingsHelper().setAlwaysShowMeetingToolbarEnabled(true);

        ZoomSDK.getInstance().getMeetingService().addListener(this);
        // Agregar listener a ZoomSDK
        ZoomSDK.getInstance().getInMeetingService().addListener(meetingServiceListener);

        ZoomSDK.getInstance().getMeetingSettingsHelper().setCustomizedNotificationData(null, handle);

//        Toast.makeText(this, "Initialize Zoom SDK successfully.", Toast.LENGTH_LONG).show();
        Log.e(TAG, "sendJoin()");

        if (mZoomSDK.tryAutoLoginZoom() == ZoomApiError.ZOOM_API_ERROR_SUCCESS) {
            Log.e(TAG, "ZoomApiError.ZOOM_API_ERROR_SUCCESS");
        } else {
            Log.e(TAG, "onZoomSDKInitializeResult >> sendJoin()");
//            sendJoinHost();
            finishMeeting = false;
            sendJoinInvite();
        }
    }

    //region :: implements ZoomSDKInitializeListener
    @Override
    public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
        Log.e(TAG, "onZoomSDKInitializeResult, errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);

        if (errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
//            Toast.makeText(this, "Failed to initialize Zoom SDK. Error: " + errorCode + ", internalErrorCode=" + internalErrorCode, Toast.LENGTH_LONG).show();
            String text = "Fallo iniciando ZoomSDK\n\n" +
                        "FAIL: " + MZoomError.toString(errorCode) + "\n\n" +
                        "CODE ERROR " + errorCode;;

            finishTimer();
            setVisibleView(circulodescarga, View.INVISIBLE);
            setVisibleView(imageZoomProgress, View.INVISIBLE);
            setTextSatus(text, Gravity.LEFT);
            setVisibleView(imageZoom,View.VISIBLE);
            setVisibleView(buttonAceptar,View.VISIBLE);										   
        }
        else {
            Log.e(TAG,"lanzarMeeting() mZoomSDK initializate");
            lanzarMeeting();
        }
    }

    @Override
    public void onZoomAuthIdentityExpired() {
        Log.e(TAG,"onZoomAuthIdentityExpired()");
        if (mZoomSDK.isLoggedIn()) {
            mZoomSDK.logoutZoom();
        }
    }
    //endregion

    //region :: implements MeetingServiceListener
    @Override
    public void onMeetingParameterNotification(MeetingParameter meetingParameter) {
        Log.e(TAG, "onMeetingParameterNotification "+meetingParameter);
    }

    @Override
    public void onMeetingStatusChanged(MeetingStatus meetingStatus, int errorCode, int internalErrorCode) {
        Log.e(TAG,"onMeetingStatusChanged >> "+meetingStatus+" "+errorCode+":"+internalErrorCode);

        if (meetingStatus == MeetingStatus.MEETING_STATUS_CONNECTING) {
            // La reunión está lista y en proceso.
            setTextSatus("Conéctese al servidor", Gravity.CENTER_HORIZONTAL);
            statusMeeting = MStatusMeeting.Connecting;
        }
        else if (meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING) {
            // La reunión está lista y en proceso.
            setTextSatus("", Gravity.CENTER_HORIZONTAL);
            finishTimer();
            statusMeeting = MStatusMeeting.InMeeting;
        }
        else if (meetingStatus == MeetingStatus.MEETING_STATUS_DISCONNECTING) {
            // Desconectando delservidor de reuniones.
//            statusMeeting = MStatusMeeting.Disconnecting;
        }
        else if (meetingStatus == MeetingStatus.MEETING_STATUS_FAILED) {
            // No se pudo conectar el servidor de reuniones.
            textfail = "No se pudo conectar el servidor de reuniones\n\n";
            textfail += MErrorCodes.toErrorCode(errorCode).toString() + "\n\n";
            textfail += "CODE ERROR " + errorCode;
            statusMeeting = MStatusMeeting.Failed;
        }
        else if (meetingStatus == MeetingStatus.MEETING_STATUS_IDLE) {
            // No se está ejecutando ninguna reunión.
            // Este estado siempre es el último

            if (statusMeeting == MStatusMeeting.Reconnecting) {
                setTextSatus("Conectando con servidor de videollamda.", Gravity.LEFT);
                finishTimer();
//                visibleFinishStatus();
            }
            else if (statusMeeting != MStatusMeeting.InMeeting) {

                if (statusMeeting == MStatusMeeting.Failed) {
                    setTextSatus(textfail, Gravity.LEFT);
                    visibleFinishStatus();
                }
                else if (statusMeeting == MStatusMeeting.Connecting) {
                    setTextSatus("No se ha podido realizar la conexión con servidor de videollamada.", Gravity.LEFT);
                    visibleFinishStatus();
                }

            }
            else {
                Log.e(TAG, "@@ Finish()");
                finishMeeting = true;
                setVisibleView(circulodescarga, View.VISIBLE);
                setVisibleView(imageZoomProgress, View.VISIBLE);
                setVisibleView(imageZoom, View.INVISIBLE);
                setVisibleView(buttonAceptar, View.INVISIBLE);
                setTextSatus("\n\nFinalizando videollamada\n\nEspere unos segundos", Gravity.CENTER_HORIZONTAL);
                cTimerFinish.start();

//                finish();
            }

            Log.e(TAG,  "statusMeeting: " + statusMeeting.toString());

        }
        else if (meetingStatus == MeetingStatus.MEETING_STATUS_IN_WAITING_ROOM) {
            // Los participantes que se unen a la reunión antes del inicio están en la sala de espera.
            statusMeeting = MStatusMeeting.InWaitingRoom;
            Log.e(TAG,"Esperando que el anfitrión nos admita en la reunión");
        }
        else if (meetingStatus == MeetingStatus.MEETING_STATUS_RECONNECTING) {
            // Reconectando el servidor de reuniones.
            statusMeeting = MStatusMeeting.Reconnecting;
        }
        else if (meetingStatus == MeetingStatus.MEETING_STATUS_UNKNOWN) {
            // Unknown status.
            statusMeeting = MStatusMeeting.Unknown;
        }
        else if (meetingStatus == MeetingStatus.MEETING_STATUS_WAITINGFORHOST) {
            // Hemos conectado al servidor de zoom y esperando a que el anfitrión inicie la reunión.
            Log.e(TAG,"Esperado en la Sala de Espera");
            cTimer.cancel();
        }
        else if (meetingStatus == MeetingStatus.MEETING_STATUS_WEBINAR_DEPROMOTE) {
            // Demote the attendees from the panelist.
        }
        else if (meetingStatus == MeetingStatus.MEETING_STATUS_WEBINAR_PROMOTE) {
            // Upgrade the attendees to panelist in webinar.
        }

        if (ZoomSDK.getInstance().getMeetingSettingsHelper().isCustomizedMeetingUIEnabled()) {
            if (meetingStatus == MeetingStatus.MEETING_STATUS_CONNECTING) {
//                showMeetingUi();
                Log.e(TAG,"isCustomizedMeetingUIEnabled()");
            }
        }
//
//        refreshUI();
    }
    //endregion

    //region :: implements InMeetingServiceListener, se implementan solo los necesarios
    private final InMeetingServiceListener meetingServiceListener = new InMeetingServiceListenerAdapter() {
        @Override
        // Informe al usuario que la reunión gratuita finalizará en 10 minuto
        public void onFreeMeetingReminder(boolean isHost, boolean canUpgrade, boolean isFirstGift){
            Log.e(TAG,"onFreeMeetingReminder() >> isHost: " + isHost + ", canUpgrade: " + canUpgrade +"isFirstGift: " + isFirstGift);
            Log.e(TAG,"onFreeMeetingReminder(): la reunión gratuita finalizará en 10 minuto");
            cTimerMeeting.start(); // Activamos timer para finalizar la reunión antes de que expire
        }

        // evento de que un NUEVO USUARIO SE UNE a la reunión.
        @Override
        public void onMeetingUserJoin(List<Long> userList) {
            Log.e(TAG,"onMeetingUserJoin(" + userList.toString() + "): nuevo usuario se une a la reunión");
        }

        // evento en que el usuario va a abandonar la reunión
        @Override
        public void onMeetingUserLeave(List<Long> userList) {
            Log.e(TAG,"onMeetingUserLeave(" + userList.toString() + "): usuario va a abandonar la reunión");
        }

        // evento en el que el usuario abandonó la reunión.
        @Override
        public void onMeetingLeaveComplete(long ret) {
            Log.e(TAG,"onMeetingLeaveComplete(" + ret + "): el usuario abandonó la reunión");
        }

        //Receptor en caso de que se actualice la información del usuario en la reunión.
        @Override
        public void onMeetingUserUpdated(long userID){ Log.e(TAG,"onMeetingUserUpdated(" + userID + "): actualizada información del usuario en la reunión"); }
    };
    //endregion

    public void sendJoinInvite() {

        if(!mZoomSDK.isInitialized()) {
            Log.e(TAG,"Init SDK First");
//            Toast.makeText(this,"Init SDK First",Toast.LENGTH_SHORT).show();
            InitAuthSDKHelper.getInstance().initSDK(this, this);
//            errDetectZoom("Fallo iniciando el SDK de ZoomS");
            return;
        }

        if (ZoomSDK.getInstance().getMeetingSettingsHelper().isCustomizedMeetingUIEnabled()) {
            ZoomSDK.getInstance().getSmsService().enableZoomAuthRealNameMeetingUIShown(false);
        }
        else {
            ZoomSDK.getInstance().getSmsService().enableZoomAuthRealNameMeetingUIShown(true);
        }

        SharedPreferences prefs = this.getSharedPreferences("VariableEntorno", Context.MODE_PRIVATE);
        String displayName = prefs.getString("idpaciente", "paciente");
        String meetingNo = prefs.getString("meeting", "");
        String password = "123456789";
        String fechaMeeting = prefs.getString("fechaMeeting", null);

        Log.e(TAG,"meetingNo: " + meetingNo);
        Log.e(TAG,"fechaMeeting: " + fechaMeeting);

        JoinMeetingParams params = new JoinMeetingParams();
        params.displayName = displayName; // "paciente"     ***
        params.meetingNo = meetingNo;     // "81182397362"  ***
        params.password = password;       // "123456789"    ***

        JoinMeetingOptions options = new JoinMeetingOptions();
        options.no_disconnect_audio = true;
        options.custom_meeting_id = "videollamada";

        //region :: Opciones de vista de reunión.
        JoinMeetingOptions meetingOptions = new JoinMeetingOptions();

        options.meeting_views_options = meetingOptions.meeting_views_options;
//        options.meeting_views_options ^= MeetingViewsOptions.NO_BUTTON_AUDIO;                      // Elimina Botón de Audio
//        options.meeting_views_options ^= MeetingViewsOptions.NO_BUTTON_LEAVE;                      // No hay botón: SALIR.
        options.meeting_views_options ^= MeetingViewsOptions.NO_BUTTON_MORE;                       // No hay botón: MÁS.
        options.meeting_views_options ^= MeetingViewsOptions.NO_BUTTON_PARTICIPANTS;               // No hay botón: PARTICIPANTES.
        options.meeting_views_options ^= MeetingViewsOptions.NO_BUTTON_SHARE;                      // No hay botón: COMPARTIR.
        options.meeting_views_options ^= MeetingViewsOptions.NO_BUTTON_SWITCH_AUDIO_SOURCE;        // No hay botón: CAMBIAR FUENTE DE AUDIO.
        options.meeting_views_options ^= MeetingViewsOptions.NO_BUTTON_SWITCH_CAMERA;              // No hay botón: CAMBIAR CÁMARA.
//        options.meeting_views_options ^= MeetingViewsOptions.NO_BUTTON_VIDEO;                      // No hay botón: VIDEO.
        options.meeting_views_options ^= MeetingViewsOptions.NO_TEXT_MEETING_ID;                   // No hay texto: ID DE LA REUNIÓN.
        options.meeting_views_options ^= MeetingViewsOptions.NO_TEXT_PASSWORD;                     // No hay texto: CONTRASEÑA.
        //endregion

//        ZoomSDK.getInstance().getMeetingService().joinMeetingWithParams(this, params,ZoomMeetingUISettingHelper.getJoinMeetingOptions());
        ZoomSDK.getInstance().getMeetingService().joinMeetingWithParams(this, params,options);

    }

    @Override
    protected void onResume() {
        super.onResume();
		Log.e(TAG,"onResume()");

       if (finishMeeting) {
//            Intent intent = new Intent(this, Inicio.class);
//            startActivity(intent);
            finish();
       }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG,"onDestroy()");

        if (cTimer != null) {
            cTimer.cancel();
            cTimer = null;
        }
        if (cTimerMeeting != null) {
            cTimerMeeting.cancel();
            cTimerMeeting = null;
        }
        if (cTimerEndMeeting != null) {
            cTimerEndMeeting.cancel();
            cTimerEndMeeting = null;
        }
        if (cTimerFinish != null) {
            cTimerFinish.cancel();
            cTimerFinish = null;
        }

        if (mZoomSDK.isLoggedIn()) {
            mZoomSDK.logoutZoom();
            Log.e(TAG, "mZoomSDK.logoutZoom()");
        }

        if(null!= ZoomSDK.getInstance().getMeetingService()) {
            ZoomSDK.getInstance().getMeetingService().removeListener(this);
            ZoomSDK.getInstance().getInMeetingService().removeListener(meetingServiceListener);
        }
        if(null!=ZoomSDK.getInstance().getMeetingSettingsHelper()){
            ZoomSDK.getInstance().getMeetingSettingsHelper().setCustomizedNotificationData(null, null);
        }
        InitAuthSDKHelper.getInstance().reset();
        ZoomSDK.getInstance().uninitialize();

    }

    @Override
    public void onBackPressed() {
        if (null == ZoomSDK.getInstance().getMeetingService()) {
            super.onBackPressed();
            return;
        }
        MeetingStatus meetingStatus = ZoomSDK.getInstance().getMeetingService().getMeetingStatus();
        if (meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING) {
            moveTaskToBack(true);
        } else {
            super.onBackPressed();
        }
    }

    private void setTextSatus(String texto, int justify) {
        runOnUiThread(new Runnable() {
            public void run() {
                txtStatus.setText(texto);
                txtStatus.setGravity(justify);
            }
        });
    }

    private void setVisibleView(View view, int state) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (view instanceof TextView) {
                    TextView obj = (TextView) view;
                    obj.setVisibility(state);
                }
                else if (view instanceof EditText) {
                    EditText obj = (EditText) view;
                    obj.setVisibility(state);
                }
                else if (view instanceof Button) {
                    Button obj = (Button) view;
                    obj.setVisibility(state);
                }
                else if (view instanceof ImageView) {
                    ImageView obj = (ImageView) view;
                    obj.setVisibility(state);
                }
                else if (view instanceof ProgressBar) {
                    ProgressBar obj = (ProgressBar) view;
                    obj.setVisibility(state);
                }
            }
        });
    }

    private void finishTimer(){
        if (cTimer != null) {
            cTimer.cancel();
            cTimer = null;
        }
    }

    private void visibleFinishStatus() {
        finishTimer();
        setVisibleView(circulodescarga, View.INVISIBLE);
        setVisibleView(imageZoomProgress, View.INVISIBLE);
        setVisibleView(imageZoom, View.VISIBLE);
        setVisibleView(buttonAceptar, View.VISIBLE);
    }

    private void errDetectZoom(String txtError) {
        finishTimer();
        setVisibleView(circulodescarga, View.INVISIBLE);
        setVisibleView(imageZoomProgress, View.INVISIBLE);
        setTextSatus(txtError, Gravity.LEFT);
        setVisibleView(imageZoom,View.VISIBLE);
        setVisibleView(buttonAceptar,View.VISIBLE);
    }

}