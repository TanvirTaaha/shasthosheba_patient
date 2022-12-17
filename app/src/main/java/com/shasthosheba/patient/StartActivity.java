package com.shasthosheba.patient;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationManagerCompat;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shasthosheba.patient.app.PreferenceManager;
import com.shasthosheba.patient.app.PublicVariables;
import com.shasthosheba.patient.databinding.ActivityStartBinding;
import com.shasthosheba.patient.model.Intermediary;
import com.shasthosheba.patient.model.User;
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


    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            this::onSignInResult
    );

    private User mUser;
    private PreferenceManager preferenceManager;

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            //Successfully signed in
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                showConnectedProgress(true);
                preferenceManager.setUser(new User(firebaseUser.getUid(), firebaseUser.getDisplayName(), "online"));
            }
            Timber.d("calling handleAfterSignIn after signIn/signUp callback");
            handleAfterSignIn();
            Timber.d("Logged in");
        } else {
            if (response != null) {
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
                Timber.d(response.getError());
            }
        }
    }

    private FirebaseDatabase rtDB = FirebaseDatabase.getInstance(PublicVariables.FIREBASE_DB);
    private DatabaseReference dataRef = rtDB.getReference(PublicVariables.INTERMEDIARY_KEY);
    private DatabaseReference callRef = rtDB.getReference("call");
    private DatabaseReference conRef = rtDB.getReference(".info/connected");

    private ActivityStartBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(this);


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

        createNotificationChannel();

        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() == null) { // not signed in
                Timber.i("Launching sign in launcher");
                signInLauncher.launch(signInIntent);
            } else { // signed in
                showConnectedProgress(true);
                preferenceManager.setUser(
                        new User(firebaseAuth.getUid(),
                                firebaseAuth.getCurrentUser().getDisplayName(),
                                "offline"));
                Timber.d("calling handleAfterSignIn from onCreate");
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

    private boolean passed = false;
    private boolean retried = false;

    private void handleAfterSignIn() {
        if (!preferenceManager.isConnected()) {
            Timber.d("Not connected");
            passed = false;
            new Handler().postDelayed(() -> {
                if (!retried) {
                    retried = true;
                    Timber.d("retrying calling handleAfterSignIn after delay");
                    handleAfterSignIn();
                }
            }, 1000);
            showConnectedProgress(false);
            return;
        }
        showConnectedProgress(true);
        passed = true;
        Timber.d("inside handle sign in function");
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mUser = new User(firebaseUser.getUid(), firebaseUser.getDisplayName(), "online");
        dataRef.child(firebaseUser.getUid()).setValue(mUser);
//        Toast.makeText(StartActivity.this, "Signed in successfully", Toast.LENGTH_LONG).show();

        preferenceManager.setUser(mUser);
        conRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Timber.d(".info/connected:%s", snapshot.getValue());
                if (Boolean.FALSE.equals(snapshot.getValue(Boolean.class))) { //NOT CONNECTED
                    User usr = new User(mUser.getuId(), mUser.getName(), "offline");
                    dataRef.child(mUser.getuId()).onDisconnect().setValue(usr);
                }
                preferenceManager.setConnected(Boolean.TRUE.equals(snapshot.getValue(Boolean.class)));
                if (!passed) {
                    Timber.d("calling handleAfterSignIn from connection true callback");
                    handleAfterSignIn();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Timber.e(error.toException());
            }
        });

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection(PublicVariables.INTERMEDIARY_KEY).document(mUser.getuId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Timber.d("dddddddddddd");
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Timber.d("checking for existence of intermediary:%s", documentSnapshot.getData());
                        preferenceManager.setIntermediary(documentSnapshot.toObject(Intermediary.class));
                        Timber.d("checking whether intermediary at pref_Man is null:%s", preferenceManager.getIntermediary());
                        startActivity(new Intent(StartActivity.this, MainActivity.class));
                    } else {
                        //No data found
                        Timber.d("No data found");
                        Timber.d("[should be null]checking for existence of intermediary:%s", documentSnapshot.getData());
                        Intermediary intermediary = new Intermediary();
                        intermediary.setId(mUser.getuId());
                        intermediary.setName(mUser.getName());
                        intermediary.setPatients(new ArrayList<>());
                        firestore.collection(PublicVariables.INTERMEDIARY_KEY).document(mUser.getuId()).set(intermediary)
                                .addOnSuccessListener(unused -> {
                                    Timber.d("Added new data:%s", intermediary);
                                    preferenceManager.setIntermediary(intermediary);
                                    startActivity(new Intent(StartActivity.this, MainActivity.class));
                                }).addOnFailureListener(Timber::e);
                    }
                    Timber.d("Document:%s", documentSnapshot.getData());
                })
                .addOnFailureListener(Timber::e);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.setStatusOnline(this);
        if (isLoggedIn()) {
            Timber.d("calling handleAfterSignIn from onResume");
            handleAfterSignIn();
        }
    }

    private void createNotificationChannel() {
        Timber.v("Creating notification channel");
        CharSequence name = "Call";
        String description = "Audio and video call";
        int importance = NotificationManagerCompat.IMPORTANCE_MAX;
        NotificationChannelCompat channel = new NotificationChannelCompat.Builder(PublicVariables.CHANNEL_ID, importance)
                .setName(name)
                .setDescription(description)
                .build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(StartActivity.this);
        notificationManager.createNotificationChannel(channel);
    }
}