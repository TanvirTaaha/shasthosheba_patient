package com.shasthosheba.patient.app;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.shasthosheba.patient.BuildConfig;
import com.shasthosheba.patient.R;
import com.shasthosheba.patient.model.User;
import com.shasthosheba.patient.repo.Repository;
import com.shasthosheba.patient.util.TagTree;

import java.lang.ref.WeakReference;

import timber.log.Timber;

public class App extends Application {

    private static WeakReference<App> mReference;

    public static App getInstance() {
        return mReference.get();
    }

    public static Context getAppContext() {
        return mReference.get();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mReference = new WeakReference<>(this);
        Repository.initialize();
        if (BuildConfig.DEBUG) {
            Timber.plant(new TagTree(getString(R.string.app_name), true));
        }
        FirebaseApp.initializeApp(getApplicationContext());
        DatabaseReference conRef = FirebaseDatabase.getInstance(PublicVariables.FIREBASE_DB).getReference(".info/connected");
        DatabaseReference dataRef = FirebaseDatabase.getInstance(PublicVariables.FIREBASE_DB).getReference(PublicVariables.PATIENTS_KEY);
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        conRef.addValueEventListener(new ValueEventListener() { //Moved from Utils.setStatusOnline to here because need to be done once
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Timber.d(".info/connected:%s", snapshot.getValue());
                boolean connected = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                if (preferenceManager.getUser() != null && !TextUtils.isEmpty(preferenceManager.getUser().getuId())) {
                    User user = preferenceManager.getUser();
                    user.setStatus("offline");
                    dataRef.child(user.getuId()).onDisconnect().setValue(user);
                }
                preferenceManager.setConnected(connected);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Timber.e(error.toException());
            }
        });
    }
}
