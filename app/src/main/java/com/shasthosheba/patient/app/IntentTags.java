package com.shasthosheba.patient.app;

public enum IntentTags {
    CALL_OBJ("call_object"),
    ACTION_ACCEPT("Jitsi_call_accept"),
    ACTION_REJECT("Jitsi_call_reject"),
    PATIENT_OBJ("patient_object"),
    PRESCRIPTION_OBJ("prescription_object"),
    ;
    public final String tag;

    private IntentTags(String tag) {
        this.tag = tag;
    }
}
