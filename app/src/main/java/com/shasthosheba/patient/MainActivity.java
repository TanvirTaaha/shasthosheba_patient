package com.shasthosheba.patient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shasthosheba.patient.app.IntentTags;
import com.shasthosheba.patient.app.PreferenceManager;
import com.shasthosheba.patient.app.PublicVariables;
import com.shasthosheba.patient.databinding.ActivityMainBinding;
import com.shasthosheba.patient.model.Call;
import com.shasthosheba.patient.model.Intermediary;
import com.shasthosheba.patient.model.Patient;
import com.shasthosheba.patient.model.User;
import com.shasthosheba.patient.patient.AddPatientActivity;
import com.shasthosheba.patient.patient.PatientAdapter;
import com.shasthosheba.patient.util.Utils;

import java.util.ArrayList;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private FirebaseDatabase rtDB = FirebaseDatabase.getInstance(PublicVariables.FIREBASE_DB);
    private DatabaseReference callRef = rtDB.getReference("call");
    private DatabaseReference dataRef = rtDB.getReference(PublicVariables.INTERMEDIARY_KEY);

    private ActivityMainBinding binding;
    private User mUser;
    private PreferenceManager preferenceManager;
    private PatientAdapter adapter;

    private FirebaseFirestore fireStoreDB = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(this);
        mUser = preferenceManager.getUser();

        Utils.setStatusOnline(this);

        adapter = new PatientAdapter(new ArrayList<>());
        binding.rcvPatientList.setLayoutManager(new LinearLayoutManager(this));
        binding.rcvPatientList.setAdapter(adapter);

        binding.fabAddPatient.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddPatientActivity.class)));

        callRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Timber.d("Call dataset changed callback");
                Call call;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    try {
                        call = snap.getValue(Call.class);
                        if (call != null && call.getWants().equals(mUser.getuId())) {
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
        });

        fetchPatients(mUser.getuId());

        binding.ibSignOut.setOnClickListener(v -> signOut(mUser));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchPatients(String intermediaryId) {
        Timber.v("inside fetch patients");
        fireStoreDB.collection(PublicVariables.INTERMEDIARY_KEY).document(intermediaryId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Timber.e(error);
                        return;
                    }
                    if (value != null && value.exists()) {
                        Intermediary intermediary = value.toObject(Intermediary.class);
                        preferenceManager.setIntermediary(intermediary);
                        //fetch all patients
                        Timber.d("onSnapshot method:got value:%s", intermediary);
                        assert intermediary != null;
                        if (intermediary.getPatients() != null && !intermediary.getPatients().isEmpty()) {
                            Timber.i("patient list is not empty:%s, contents:%s", intermediary.getPatients().size(), intermediary.getPatients());
                            adapter.getList().clear();
                            Timber.i("adapter cleared:%s", adapter.getItemCount());
                            for (String id : intermediary.getPatients()) {
                                Timber.d("fetching for patient id:%s", id);
                                fireStoreDB.collection(PublicVariables.PATIENTS_KEY).document(id).get()
                                        .addOnSuccessListener(documentSnapshot1 -> {
                                            Patient fetchedPatient = documentSnapshot1.toObject(Patient.class);
                                            Timber.d("fetched fetchedPatient:%s", fetchedPatient);
                                            if (!adapter.getList().contains(fetchedPatient)) {
                                                adapter.getList().add(fetchedPatient);
                                                adapter.notifyItemInserted(adapter.getItemCount() - 1);
                                            }
                                        }).addOnFailureListener(Timber::e);
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPatients(mUser.getuId());
        Utils.setStatusOnline(this);
    }

    private void notifyCall(Call call) {
        Intent acceptIntent = new Intent(MainActivity.this, BroadcastReceiver.class)
                .setAction(IntentTags.ACTION_ACCEPT.tag)
                .putExtra(IntentTags.CALL_OBJ.tag, call);
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, acceptIntent, PendingIntent.FLAG_IMMUTABLE);
        Intent rejectIntent = new Intent(MainActivity.this, BroadcastReceiver.class)
                .setAction(IntentTags.ACTION_REJECT.tag)
                .putExtra(IntentTags.CALL_OBJ.tag, call);
        PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, rejectIntent, PendingIntent.FLAG_IMMUTABLE);


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MainActivity.this, PublicVariables.CHANNEL_ID)
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_sign_out) {
            signOut(mUser);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOut(User user) {
        if (user == null) preferenceManager.getUser();
        assert user != null;
        user.setStatus("offline");
        dataRef.child(user.getuId()).setValue(user)
                .addOnCompleteListener(task -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(MainActivity.this, StartActivity.class));
                });

    }
}