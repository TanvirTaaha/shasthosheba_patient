package com.shasthosheba.patient.app;

import com.shasthosheba.patient.R;

public class PublicVariables {

    public static final int CALL_NOTIFICATION_ID = 99;
    public static final int WAITING_NOTIFICATION_ID = 69;
    public static final String CALL_CHANNEL_ID = App.getAppContext().getString(R.string.call_notification_channel);
    public static final String CHAMBER_CHANNEL_ID = "chamber_notification_channel";

    public static final String FIREBASE_DB = "https://shasthosheba-fe3e1-default-rtdb.asia-southeast1.firebasedatabase.app";

    public static final String INTERMEDIARY_KEY = "intermediaries";
    public static final String PATIENTS_KEY = "patients";
    public static final String CALL_KEY = "call";
    public static final String PRESCRIPTION_KEY = "prescriptions";
    public static final String INTERMEDIARY_PATIENT_IDs = "patients";
    public static final String CHAMBER_KEY = "chamber";
    public static final String SERVER_TIME_STAMP_NODE = "server_time_stamp_node";
}
