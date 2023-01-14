package com.shasthosheba.patient.ui.chamber;

import android.app.PendingIntent;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.shasthosheba.patient.R;
import com.shasthosheba.patient.app.IntentTags;
import com.shasthosheba.patient.app.PreferenceManager;
import com.shasthosheba.patient.app.PublicVariables;
import com.shasthosheba.patient.model.Call;
import com.shasthosheba.patient.repo.Repository;
import com.shasthosheba.patient.ui.BroadcastReceiver;

import timber.log.Timber;

public class FCMService extends FirebaseMessagingService {

    public FCMService() {
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        sendNotification(message);
    }

    private void sendNotification(RemoteMessage message) {
        Timber.d("message.getData():%s", message.getData().toString());
        if (message.getNotification() != null) {
            Timber.d("message.getNotification():%s", message.getNotification().toString());
        }
        Call call = new Gson().fromJson(message.getData().get("call"), Call.class);
        Intent callAcceptBroadcastIntent = new Intent(getApplicationContext(), BroadcastReceiver.class)
                .setAction(IntentTags.ACTION_ACCEPT_CALL.tag)
                .putExtra(IntentTags.CALL_OBJ.tag, message.getData().get("call"));
        PendingIntent callAcceptPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, callAcceptBroadcastIntent, PendingIntent.FLAG_IMMUTABLE);
        Uri defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_RINGTONE);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), PublicVariables.CALL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(call.isVideo() ? "Video call" : "Audio call")
                .setContentText("Call from " + call.getDoctor())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSound(defaultRingtoneUri, AudioManager.STREAM_RING)
                .addAction(android.R.drawable.sym_action_call, "Accept", callAcceptPendingIntent)
                .setOngoing(true);
        NotificationManagerCompat.from(this).notify(PublicVariables.CALL_NOTIFICATION_ID, notificationBuilder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Repository.getInstance().setNewTokenOnChamberIfExists(new PreferenceManager(getApplicationContext()).getUser().getuId(), token);
        Timber.d("onNewToken:%s", token);
    }
}