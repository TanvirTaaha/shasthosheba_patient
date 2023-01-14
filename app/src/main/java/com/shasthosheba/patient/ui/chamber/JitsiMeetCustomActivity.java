package com.shasthosheba.patient.ui.chamber;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import java.util.HashMap;

import timber.log.Timber;

public class JitsiMeetCustomActivity extends JitsiMeetActivity {

    private static ConferenceEndCallback endCallback;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static void launch(Context context, JitsiMeetConferenceOptions options, ConferenceEndCallback callback) {
        Intent intent = new Intent(context, JitsiMeetCustomActivity.class);
        intent.setAction("org.jitsi.meet.CONFERENCE");
        intent.putExtra("JitsiMeetConferenceOptions", options);
        if (!(context instanceof Activity)) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        context.startActivity(intent);
        endCallback = callback;
    }

    @Override
    protected void onConferenceTerminated(HashMap<String, Object> extraData) {
        super.onConferenceTerminated(extraData);
        Timber.d("Conference terminated callback");
        endCallback.terminated();
    }

    public interface ConferenceEndCallback{
        void terminated();
    }
}