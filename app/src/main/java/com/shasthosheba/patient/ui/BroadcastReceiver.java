package com.shasthosheba.patient.ui;

import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.Observer;

import com.shasthosheba.patient.app.IntentTags;
import com.shasthosheba.patient.app.PublicVariables;
import com.shasthosheba.patient.model.Call;
import com.google.firebase.database.FirebaseDatabase;
import com.shasthosheba.patient.repo.DataOrError;
import com.shasthosheba.patient.repo.Repository;
import com.shasthosheba.patient.ui.chamber.ChamberWaitingService;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import timber.log.Timber;

public class BroadcastReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(IntentTags.ACTION_ACCEPT_CALL.tag)) {
            Call call = (Call) intent.getSerializableExtra(IntentTags.CALL_OBJ.tag);
            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setRoom(call.getRoom())
                    .setVideoMuted(!call.isVideo())
                    .build();
            JitsiMeetActivity.launch(context, options);
            FirebaseDatabase.getInstance(PublicVariables.FIREBASE_DB).getReference("call").child(call.getRoom()).removeValue();
            NotificationManagerCompat.from(context).cancel(PublicVariables.CALL_NOTIFICATION_ID);
        }
        if (intent.getAction().equals(IntentTags.ACTION_REJECT_CALL.tag)) {
            Call call = (Call) intent.getSerializableExtra(IntentTags.CALL_OBJ.tag);
            FirebaseDatabase.getInstance(PublicVariables.FIREBASE_DB).getReference("call").child(call.getRoom()).removeValue();
            NotificationManagerCompat.from(context).cancel(PublicVariables.CALL_NOTIFICATION_ID);
        }
        if (intent.getAction().equals(IntentTags.ACTION_LEAVE_CHAMBER.tag)) {
            String uId = intent.getStringExtra(IntentTags.USER_ID.tag);
            Timber.d("onReceive: Action Leave chamber for uid:%s", uId);
            Repository.getInstance().removeChamberMember(uId).observeForever(booleanOrError -> {
                if (booleanOrError.data) {
                    Timber.d("chamber removed");
                    NotificationManagerCompat.from(context).cancel(PublicVariables.WAITING_NOTIFICATION_ID);
                } else {
                    Timber.d("chamber remove failed");
                    Timber.e(booleanOrError.error);
                }
            });
            context.stopService(new Intent(context.getApplicationContext(), ChamberWaitingService.class));
//            NotificationManagerCompat.from(context).cancel(PublicVariables.WAITING_NOTIFICATION_ID);
        }
    }
}
