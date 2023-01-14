package com.shasthosheba.patient.app;

public enum IntentTags {
    CALL_OBJ("call_object"),
    PATIENT_OBJ("patient_object"),
    PRESCRIPTION_OBJ("prescription_object"),

    USER_ID("user_id"),

    ACTION_ACCEPT_CALL("Jitsi_call_accept"),
    ACTION_REJECT_CALL("Jitsi_call_reject"),
    ACTION_LEAVE_CHAMBER("leave_chamber_notification_action"),
    FCM_CALL_OBJ("call"),
    ;
    public final String tag;

    private IntentTags(String tag) {
        this.tag = tag;
    }
}
