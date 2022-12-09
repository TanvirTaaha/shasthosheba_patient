package com.shasthosheba.patient;

import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

import com.shasthosheba.patient.app.IntentTags;
import com.shasthosheba.patient.app.PublicVariables;
import com.shasthosheba.patient.model.Call;
import com.google.firebase.database.FirebaseDatabase;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

public class BroadcastReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(IntentTags.ACTION_ACCEPT.tag)) {
            Call call = (Call) intent.getSerializableExtra(IntentTags.CALL_OBJ.tag);
            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setRoom(call.getRoom())
                    .setVideoMuted(!call.isVideo())
                    .build();
            JitsiMeetActivity.launch(context, options);
            FirebaseDatabase.getInstance(PublicVariables.FIREBASE_DB).getReference("call").child(call.getRoom()).removeValue();
            NotificationManagerCompat.from(context).cancel(PublicVariables.CALL_NOTIFICATION_ID);
        }
        if (intent.getAction().equals(IntentTags.ACTION_REJECT.tag)) {
            Call call = (Call) intent.getSerializableExtra(IntentTags.CALL_OBJ.tag);
            FirebaseDatabase.getInstance(PublicVariables.FIREBASE_DB).getReference("call").child(call.getRoom()).removeValue();
            NotificationManagerCompat.from(context).cancel(PublicVariables.CALL_NOTIFICATION_ID);
        }
    }
}
