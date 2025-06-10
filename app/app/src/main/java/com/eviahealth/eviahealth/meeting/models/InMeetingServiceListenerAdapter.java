package com.eviahealth.eviahealth.meeting.models;

import java.util.List;
import us.zoom.sdk.*;

public abstract class InMeetingServiceListenerAdapter implements InMeetingServiceListener {

    @Override
    public void onMeetingNeedPasswordOrDisplayName(boolean var1, boolean var2, InMeetingEventHandler var3) { }

    @Override public void onWebinarNeedRegister(String var1){ }

    @Override public void onJoinMeetingNeedUserInfo(IMeetingInputUserInfoHandler var1){ }

    @Override public void onJoinWebinarNeedUserNameAndEmail(InMeetingEventHandler var1){ }

    @Override public void onWebinarNeedInputScreenName(InMeetingEventHandler var1){ }

    @Override public void onMeetingNeedCloseOtherMeeting(InMeetingEventHandler var1){ }

    @Override public void onMeetingFail(int var1, int var2){ }

    @Override public void onMeetingLeaveComplete(long var1){ }

    @Override public void onMeetingUserJoin(List<Long> var1){ }

    @Override public void onMeetingUserLeave(List<Long> var1){ }

    /** @deprecated */
    @Deprecated
    @Override public void onMeetingUserUpdated(long var1){ }

    @Override public void onInMeetingUserAvatarPathUpdated(long var1){ }

    @Override public void onMeetingHostChanged(long var1){ }

    @Override public void onMeetingCoHostChange(long var1, boolean var3){ }

    @Override public void onActiveVideoUserChanged(long var1){ }

    @Override public void onActiveSpeakerVideoUserChanged(long var1){ }

    @Override public void onHostVideoOrderUpdated(List<Long> var1){ }

    @Override public void onFollowHostVideoOrderChanged(boolean var1){ }

    @Override public void onSpotlightVideoChanged(List<Long> var1){ }

    @Override public void onUserVideoStatusChanged(long var1, VideoStatus var3){ }

    @Override public void onSinkMeetingVideoQualityChanged(VideoQuality var1, long var2){ }

    @Override public void onMicrophoneStatusError(InMeetingAudioController.MobileRTCMicrophoneError var1){ }

    @Override public void onUserAudioStatusChanged(long var1, AudioStatus var3){ }

    @Override public void onHostAskUnMute(long var1){ }

    @Override public void onHostAskStartVideo(long var1){ }

    @Override public void onUserAudioTypeChanged(long var1){ }

    @Override public void onMyAudioSourceTypeChanged(int var1){ }

    @Override public void onLowOrRaiseHandStatusChanged(long var1, boolean var3){ }

    @Override public void onChatMessageReceived(InMeetingChatMessage var1){ }

    @Override public void onChatMsgDeleteNotification(String var1, ChatMessageDeleteType var2){ }

    @Override public void onShareMeetingChatStatusChanged(boolean var1){ }

    @Override public void onSilentModeChanged(boolean var1){ }

    @Override public void onFreeMeetingReminder(boolean var1, boolean var2, boolean var3){ }

    @Override public void onMeetingActiveVideo(long var1){ }

    @Override public void onSinkAttendeeChatPrivilegeChanged(int var1){ }

    @Override public void onSinkAllowAttendeeChatNotification(int var1){ }

    @Override public void onSinkPanelistChatPrivilegeChanged(InMeetingChatController.MobileRTCWebinarPanelistChatPrivilege var1){ }

    @Override public void onUserNamesChanged(List<Long> var1){ }

    @Override public void onFreeMeetingNeedToUpgrade(FreeMeetingNeedUpgradeType var1, String var2){ }

    @Override public void onFreeMeetingUpgradeToGiftFreeTrialStart(){ }

    @Override public void onFreeMeetingUpgradeToGiftFreeTrialStop(){ }

    @Override public void onFreeMeetingUpgradeToProMeeting(){ }

    @Override public void onClosedCaptionReceived(String var1, long var2){ }

    @Override public void onRecordingStatus(RecordingStatus var1){ }

    @Override public void onLocalRecordingStatus(long var1, RecordingStatus var3){ }

    @Override public void onInvalidReclaimHostkey(){ }

    @Override public void onPermissionRequested(String[] var1){ }

    @Override public void onAllHandsLowered(){ }

    @Override public void onLocalVideoOrderUpdated(List<Long> var1){ }

    @Override public void onLocalRecordingPrivilegeRequested(IRequestLocalRecordingPrivilegeHandler var1){ }

    @Override public void onSuspendParticipantsActivities(){ }

    @Override public void onAllowParticipantsStartVideoNotification(boolean var1){ }

    @Override public void onAllowParticipantsRenameNotification(boolean var1){ }

    @Override public void onAllowParticipantsUnmuteSelfNotification(boolean var1){ }

    @Override public void onAllowParticipantsShareWhiteBoardNotification(boolean var1){ }

    @Override public void onMeetingLockStatus(boolean var1){ }

    @Override public void onRequestLocalRecordingPrivilegeChanged(LocalRecordingRequestPrivilegeStatus var1){ }

    @Override public void onAICompanionActiveChangeNotice(boolean var1){ }

    @Override public void onParticipantProfilePictureStatusChange(boolean var1){ }

    @Override public void onCloudRecordingStorageFull(long var1){ }

    @Override public void onUVCCameraStatusChange(String var1, UVCCameraStatus var2){ }

    @Override public void onFocusModeStateChanged(boolean var1){ }

    @Override public void onFocusModeShareTypeChanged(MobileRTCFocusModeShareType var1){ }

    @Override public void onVideoAlphaChannelStatusChanged(boolean var1){ }

    @Override public void onAllowParticipantsRequestCloudRecording(boolean var1){ }

    @Override public void onSinkJoin3rdPartyTelephonyAudio(String var1){ }

    @Override public void onUserConfirmToStartArchive(IMeetingArchiveConfirmHandler var1){ }

    @Override public void onCameraControlRequestReceived(long var1, CameraControlRequestType var3, ICameraControlRequestHandler var4){ }

    /** @deprecated */
    @Deprecated
    @Override public void onCameraControlRequestResult(long var1, boolean var3){ }

    @Override public void onCameraControlRequestResult(long var1, CameraControlRequestResult var3){ }

    @Override public void onFileSendStart(ZoomSDKFileSender var1){ }

    @Override public void onFileReceived(ZoomSDKFileReceiver var1){ }

    @Override public void onFileTransferProgress(ZoomSDKFileTransferInfo var1){ }

    @Override public void onMuteOnEntryStatusChange(boolean var1){ }

    @Override public void onMeetingTopicChanged(String var1){ }

    @Override public void onMeetingFullToWatchLiveStream(String var1){ }

    @Override public void onRobotRelationChanged(long var1){ }

    @Override public void onVirtualNameTagStatusChanged(boolean var1, long var2){ }

    @Override public void onVirtualNameTagRosterInfoUpdated(long var1){ }

}