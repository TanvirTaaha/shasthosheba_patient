package com.shasthosheba.patient.ui.chamber;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.core.Repo;
import com.shasthosheba.patient.R;
import com.shasthosheba.patient.app.IntentTags;
import com.shasthosheba.patient.app.PreferenceManager;
import com.shasthosheba.patient.app.PublicVariables;
import com.shasthosheba.patient.model.Call;
import com.shasthosheba.patient.model.ChamberMember;
import com.shasthosheba.patient.repo.Repository;
import com.shasthosheba.patient.ui.BroadcastReceiver;
import com.shasthosheba.patient.ui.MainActivity;

import timber.log.Timber;

public class ChamberWaitingService extends Service {
    private DatabaseReference callRef = Repository.getFirebaseDatabase().getReference("call");
    private ValueEventListener valueEventListener;
    private NotificationCompat.Builder waitingNotificationBuilder;
    private String uId;
    private ClientActivity mClientActivity;


    private void notifyCall(Call call) {
        Timber.d("Notify call called");
        Intent acceptIntent = new Intent(getApplicationContext(), BroadcastReceiver.class)
                .setAction(IntentTags.ACTION_ACCEPT_CALL.tag)
                .putExtra(IntentTags.CALL_OBJ.tag, call.toString());
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, acceptIntent, PendingIntent.FLAG_IMMUTABLE);
        Intent rejectIntent = new Intent(getApplicationContext(), BroadcastReceiver.class)
                .setAction(IntentTags.ACTION_REJECT_CALL.tag)
                .putExtra(IntentTags.CALL_OBJ.tag, call.toString());
        PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, rejectIntent, PendingIntent.FLAG_IMMUTABLE);


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), PublicVariables.CALL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(call.isVideo() ? "Video call" : "Audio call")
                .setContentText("Call from " + call.getDoctor())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .addAction(android.R.drawable.sym_action_call, "Accept", acceptPendingIntent)
                .addAction(R.drawable.ic_round_call_end_24, "Reject", rejectPendingIntent)
                .setOngoing(true);
        NotificationManagerCompat.from(this).notify(PublicVariables.CALL_NOTIFICATION_ID, notificationBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent == null) {
            Timber.wtf("Intent is null");
            return START_NOT_STICKY;
        }
        Timber.d("onStartCommand");
        uId = intent.getStringExtra(IntentTags.USER_ID.tag);
        Timber.d("got uid:%s", uId);
        buildNotification();
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Timber.d("Call dataset changed callback");
                Call call;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    try {
                        call = snap.getValue(Call.class);
                        if (call != null && call.getWants().equals(uId)) {
                            Timber.i("Match found..Launching jitsi");
                            notifyCall(call);
                            break;
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Timber.e(error.toException());
            }
        };
        Timber.d("starting foreground notification");
        startForeground(PublicVariables.WAITING_NOTIFICATION_ID, waitingNotificationBuilder.build());
        callRef.addValueEventListener(valueEventListener);
        new PreferenceManager(getApplicationContext()).setChamberRunning(true);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new WaitingServiceBinder();
    }

    private void buildNotification() {
        Timber.d("buildNotification called");
        Intent chamberIntent = new Intent(getApplicationContext(), ChamberActivityIntermediary.class);
        PendingIntent chamberPendingIntent = PendingIntent.getActivity(getApplicationContext(),
                0,
                chamberIntent,
                PendingIntent.FLAG_IMMUTABLE
        );
        Intent leaveActionIntent = new Intent(getApplicationContext(), BroadcastReceiver.class)
                .setAction(IntentTags.ACTION_LEAVE_CHAMBER.tag)
                .putExtra(IntentTags.USER_ID.tag, uId);
        PendingIntent leaveActionPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, leaveActionIntent, PendingIntent.FLAG_IMMUTABLE);

        waitingNotificationBuilder = new NotificationCompat.Builder(getApplicationContext(), PublicVariables.CHAMBER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Virtual chamber")
                .setContentText("Waiting for the call from doctor")
                .setContentIntent(chamberPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(R.drawable.ic_round_call_end_24, "Leave chamber", leaveActionPendingIntent)
                .setOngoing(true);
    }

    public class WaitingServiceBinder extends Binder {
        public void setClient(ClientActivity client) {
            ChamberWaitingService.this.mClientActivity = client;
        }
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        callRef.addValueEventListener(valueEventListener);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        callRef.removeEventListener(valueEventListener);
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("onDestroy:");
        if (mClientActivity != null) {
            Timber.d("onDestroy:finishing mClientActivity");
            mClientActivity.finishSelf();
        }
        new PreferenceManager(getApplicationContext()).setChamberRunning(false);
        Repository.getInstance().removeChamberMember(uId);
    }

    public interface ClientActivity {
        void finishSelf();
    }
}