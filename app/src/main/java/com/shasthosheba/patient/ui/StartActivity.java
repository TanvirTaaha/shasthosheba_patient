package com.shasthosheba.patient.ui;

import android.app.PendingIntent;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.Observer;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.shasthosheba.patient.R;
import com.shasthosheba.patient.app.IntentTags;
import com.shasthosheba.patient.app.PreferenceManager;
import com.shasthosheba.patient.app.PublicVariables;
import com.shasthosheba.patient.databinding.ActivityStartBinding;
import com.shasthosheba.patient.model.Call;
import com.shasthosheba.patient.model.Intermediary;
import com.shasthosheba.patient.model.User;
import com.shasthosheba.patient.repo.Repository;
import com.shasthosheba.patient.ui.chamber.FCMService;
import com.shasthosheba.patient.util.Utils;

import org.jitsi.meet.sdk.JitsiMeet;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

public class StartActivity extends AppCompatActivity {


    private ActivityResultLauncher<Intent> signInLauncher;
    private boolean signInLauncherRegistered = false;

    private User mUser;
    private PreferenceManager preferenceManager;

    private FirebaseDatabase rtDB = FirebaseDatabase.getInstance(PublicVariables.FIREBASE_DB);
    private DatabaseReference dataRef = rtDB.getReference(PublicVariables.INTERMEDIARY_KEY);
    private DatabaseReference callRef = rtDB.getReference("call");
    private DatabaseReference conRef = rtDB.getReference(".info/connected");

    private ActivityStartBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        binding = ActivityStartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(this);


        createNotificationChannel();


        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() == null) { // not signed in
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.EmailBuilder().build(),
                        new AuthUI.IdpConfig.GoogleBuilder().build()
                );
                Intent signInIntent = AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAlwaysShowSignInMethodScreen(true)
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(providers)
                        .build();
                if (signInLauncherRegistered) {
                    Timber.v("Launching sign in launcher");
                    signInLauncher.launch(signInIntent);
                } else {
                    Timber.v("Ignoring because activity is in background");
                }
            } else { // signed in
                showConnectedProgress(true);
                preferenceManager.setUser(
                        new User(firebaseAuth.getUid(),
                                firebaseAuth.getCurrentUser().getDisplayName(),
                                "offline"));
                Timber.d("calling handleAfterSignIn from authStateListener");
                handleAfterSignIn();
            }
        });


        URL serverUrl;
        try {
            serverUrl = new URL("https://meet.jit.si");
            JitsiMeetConferenceOptions defaultOptions = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(serverUrl)
                    .setFeatureFlag("welcomepage.enabled", false)
                    .build();
            JitsiMeet.setDefaultConferenceOptions(defaultOptions);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (getIntent().getExtras() != null) {
            Timber.d("call:%s", getIntent().getStringExtra(IntentTags.FCM_CALL_OBJ.tag));
            Call call = new Gson().fromJson(getIntent().getStringExtra(IntentTags.FCM_CALL_OBJ.tag), Call.class);
            Intent callAcceptBroadcastIntent = new Intent(getApplicationContext(), BroadcastReceiver.class)
                    .setAction(IntentTags.ACTION_ACCEPT_CALL.tag)
                    .putExtra(IntentTags.CALL_OBJ.tag, getIntent().getStringExtra(IntentTags.FCM_CALL_OBJ.tag));
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
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            //Successfully signed in
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                showConnectedProgress(true);
                preferenceManager.setUser(new User(firebaseUser.getUid(), firebaseUser.getDisplayName(), "online"));
            }
            Timber.d("calling handleAfterSignIn from signInResult");
            Timber.d("Logged in");
            handleAfterSignIn();
        } else {
            if (response != null) {
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
                Timber.d(response.getError());
            }
        }
    }

    private boolean isLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    private void showConnectedProgress(boolean connected) {
        if (connected) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.tvConnecting.setText(R.string.connecting);
        } else { // Show connection lost
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.tvConnecting.setText(R.string.connection_lost);
        }
    }

    private void handleAfterSignIn() {
        Timber.d("inside handle sign in function");
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        assert firebaseUser != null;
        mUser = new User(firebaseUser.getUid(), firebaseUser.getDisplayName(), "online");
        preferenceManager.setUser(mUser);
        Timber.d("User:%s", mUser);

        if (Repository.getInstance().getNetStatus().hasActiveObservers()) {
            // this is because AuthStateListener somehow gets callbacks multiple times.
            // this is to prevent multiple observer being registered as this on must be the first in this app.
            return;
        }
        showConnectedProgress(Repository.getInstance().isConnected());
        Repository.getInstance().getNetStatus().observe(StartActivity.this, netAvailable -> {
            Timber.d("In ConMan network callbacks:netAvailable:%s", netAvailable);
            if (netAvailable) {
                Timber.d("Got connection, checking for data, setting if not already exists and loading otherwise");
                showConnectedProgress(true);
                dataRef.child(firebaseUser.getUid()).setValue(mUser)
                        .addOnSuccessListener(unused -> {
                            Timber.d("Intermediary status set");
                        })
                        .addOnFailureListener(Timber::e);
                FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                firestore.collection(PublicVariables.INTERMEDIARY_KEY).document(mUser.getuId()).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            Timber.d("dddddddddddd");
                            if (documentSnapshot != null && documentSnapshot.exists()) {
                                Timber.d("checking for existence of intermediary:%s", documentSnapshot.getData());
                                preferenceManager.setIntermediary(documentSnapshot.toObject(Intermediary.class));
                                Timber.d("checking whether intermediary at pref_Man is null:%s", preferenceManager.getIntermediary());
                                startActivity(new Intent(StartActivity.this, MainActivity.class));
                                finish();
                            } else {
                                //No data found
                                Timber.d("No data found");
                                Timber.d("[should be null as no data found]checking for existence of intermediary:%s", documentSnapshot.getData());
                                Intermediary intermediary = new Intermediary();
                                intermediary.setId(mUser.getuId());
                                intermediary.setName(mUser.getName());
                                intermediary.setPatients(new ArrayList<>());
                                firestore.collection(PublicVariables.INTERMEDIARY_KEY).document(mUser.getuId()).set(intermediary)
                                        .addOnSuccessListener(unused -> {
                                            Timber.d("Added new data:%s", intermediary);
                                            preferenceManager.setIntermediary(intermediary);
                                            startActivity(new Intent(StartActivity.this, MainActivity.class));
                                            finish();
                                        }).addOnFailureListener(Timber::e);
                            }
                            Timber.d("Document:%s", documentSnapshot.getData());
                        })
                        .addOnFailureListener(Timber::e);
            } else {
                showConnectedProgress(false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.setStatusOnline(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        signInLauncher = registerForActivityResult(
                new FirebaseAuthUIActivityResultContract(),
                this::onSignInResult
        );
        signInLauncherRegistered = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        signInLauncherRegistered = false;
    }

    private void createNotificationChannel() {
        Timber.v("Creating notification channel");
        CharSequence name = "Call";
        String description = "Audio and video call";
        Uri defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_RINGTONE);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build();
        NotificationChannelCompat channel = new NotificationChannelCompat.Builder(PublicVariables.CALL_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_MAX)
                .setName(name)
                .setDescription(description)
                .setSound(defaultRingtoneUri, audioAttributes)
                .build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(StartActivity.this);
        notificationManager.createNotificationChannel(channel);
        // channel for waiting in chamber notification
        NotificationChannelCompat chamberChannel = new NotificationChannelCompat.Builder(PublicVariables.CHAMBER_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("Chamber")
                .setDescription("Chamber waiting notification")
                .build();
        notificationManager.createNotificationChannel(chamberChannel);
    }
}