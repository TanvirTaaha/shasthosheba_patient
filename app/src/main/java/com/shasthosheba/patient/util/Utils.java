package com.shasthosheba.patient.util;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.shasthosheba.patient.app.PreferenceManager;
import com.shasthosheba.patient.app.PublicVariables;
import com.shasthosheba.patient.model.User;

import timber.log.Timber;

public class Utils {
    public static void setStatusOnline(Context context) {
        DatabaseReference dataRef = FirebaseDatabase.getInstance(PublicVariables.FIREBASE_DB).getReference(PublicVariables.INTERMEDIARY_KEY);
        DatabaseReference conRef = FirebaseDatabase.getInstance(PublicVariables.FIREBASE_DB).getReference(".info/connected");
        User user = new PreferenceManager(context).getUser();
        if (user == null || user.getuId() == null ||user.getuId().isEmpty()) {
            Timber.d("User is null");
            Timber.d("returning from setStatusOnline...");
            return;
        }
        Timber.d("User is not null");
        user.setStatus("online");
        dataRef.child(user.getuId()).setValue(user)
                .addOnSuccessListener(unused ->
                        Timber.i("util:updated status online"))
                .addOnFailureListener(Timber::e);

//        conRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                Timber.d(".info/connected:%s", snapshot.getValue());
//                if (Boolean.FALSE.equals(snapshot.getValue(Boolean.class))) { //NOT CONNECTED
//                    user.setStatus("offline");
//                    dataRef.child(user.getuId()).onDisconnect().setValue(user);
//                }
//                new PreferenceManager(context).setConnected(Boolean.TRUE.equals(snapshot.getValue(Boolean.class)));
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Timber.e(error.toException());
//            }
//        });
    }
}
