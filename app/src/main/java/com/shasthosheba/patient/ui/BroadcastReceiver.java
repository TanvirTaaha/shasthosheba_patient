package com.shasthosheba.patient.ui;

import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.Observer;

import com.google.gson.Gson;
import com.shasthosheba.patient.app.IntentTags;
import com.shasthosheba.patient.app.PublicVariables;
import com.shasthosheba.patient.model.Call;
import com.google.firebase.database.FirebaseDatabase;
import com.shasthosheba.patient.repo.DataOrError;
import com.shasthosheba.patient.repo.Repository;
import com.shasthosheba.patient.ui.chamber.ChamberWaitingService;
import com.shasthosheba.patient.ui.chamber.JitsiMeetCustomActivity;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import timber.log.Timber;

public class BroadcastReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(IntentTags.ACTION_ACCEPT_CALL.tag)) {
            Timber.v("ACTION_ACCEPT_CALL:");
            Call call = new Gson().fromJson(intent.getStringExtra(IntentTags.CALL_OBJ.tag), Call.class);
            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setRoom(call.getRoom())
                    .setVideoMuted(!call.isVideo())
                    .build();
            JitsiMeetCustomActivity.launch(context, options, () -> {
                Timber.d("Conference terminated callback in broadcast");
                Intent leaveIntent = new Intent(context, BroadcastReceiver.class)
                        .setAction(IntentTags.ACTION_LEAVE_CHAMBER.tag)
                        .putExtra(IntentTags.USER_ID.tag, call.getWants());
                context.sendBroadcast(leaveIntent);
            });
            FirebaseDatabase.getInstance(PublicVariables.FIREBASE_DB).getReference(PublicVariables.CALL_KEY).child(call.getRoom()).removeValue();
            NotificationManagerCompat.from(context).cancel(PublicVariables.CALL_NOTIFICATION_ID);
        }
        if (intent.getAction().equals(IntentTags.ACTION_REJECT_CALL.tag)) {
            Call call = new Gson().fromJson(intent.getStringExtra(IntentTags.CALL_OBJ.tag), Call.class);
            FirebaseDatabase.getInstance(PublicVariables.FIREBASE_DB).getReference(PublicVariables.CALL_KEY).child(call.getRoom()).removeValue();
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
