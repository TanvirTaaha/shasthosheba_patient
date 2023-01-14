package com.shasthosheba.patient.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AlertDialogLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shasthosheba.patient.R;
import com.shasthosheba.patient.app.IntentTags;
import com.shasthosheba.patient.app.PreferenceManager;
import com.shasthosheba.patient.app.PublicVariables;
import com.shasthosheba.patient.databinding.ActivityMainBinding;
import com.shasthosheba.patient.databinding.ChamberAlertDialogBinding;
import com.shasthosheba.patient.model.Call;
import com.shasthosheba.patient.model.ChamberMember;
import com.shasthosheba.patient.model.Intermediary;
import com.shasthosheba.patient.model.Patient;
import com.shasthosheba.patient.model.User;
import com.shasthosheba.patient.repo.DataOrError;
import com.shasthosheba.patient.repo.Repository;
import com.shasthosheba.patient.ui.chamber.ChamberActivityIntermediary;
import com.shasthosheba.patient.ui.chamber.ChamberWaitingService;
import com.shasthosheba.patient.ui.patient.AddPatientActivity;
import com.shasthosheba.patient.ui.patient.PatientAdapter;
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
    private MainActivityViewModel mViewModel;

    private FirebaseFirestore fireStoreDB = FirebaseFirestore.getInstance();
    private ActivityResultLauncher<String> requestPermissionLauncher;

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

        // called from waiting service
        // setCallListener();

        binding.btnGoToChamber.setOnClickListener(v -> askNotificationPermission());


        binding.ibSignOut.setOnClickListener(v -> signOut(mUser));
        mViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        mViewModel.getNetStatus().observe(this, netAvailable -> {
            Snackbar snackbar;
            snackbar = Snackbar.make(binding.getRoot(), "Check your internet connection", Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("Ok", v -> snackbar.dismiss());
            if (netAvailable) {
                snackbar.dismiss();
            } else {
                snackbar.show();
            }
        });
    }

    private void setCallListener() {
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
    }

    private void launchChamber() {
        ChamberMember member = new ChamberMember();
        member.setIntermediaryId(preferenceManager.getUser().getuId());
        member.setName(preferenceManager.getUser().getName());

        ChamberAlertDialogBinding chamberAlertDialogBinding = ChamberAlertDialogBinding.inflate(getLayoutInflater());
        chamberAlertDialogBinding.llPaymentInfo.setVisibility(View.GONE);
        chamberAlertDialogBinding.radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_without_payment) {
                member.setWithPayment(false);
                chamberAlertDialogBinding.llPaymentInfo.setVisibility(View.GONE);
            } else if (checkedId == R.id.radio_with_payment) {
                member.setWithPayment(true);
                chamberAlertDialogBinding.llPaymentInfo.setVisibility(View.VISIBLE);
            }
        });
        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogStyle);
        materialAlertDialogBuilder.setView(chamberAlertDialogBinding.getRoot())
                .setTitle("Join chamber")
                .setMessage("Select payment options")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    if (member.isWithPayment()) {
                        int amount = Integer.parseInt(chamberAlertDialogBinding.tietAmount.getText().toString().trim());
                        member.setAmount(amount);
                        member.setMemberBKashNo(chamberAlertDialogBinding.tietBkashNo.getText().toString().trim());
                        member.setTransactionId(chamberAlertDialogBinding.tietTxnId.getText().toString().trim());
                    }
                    mViewModel.addChamberMember(member).observe(this, booleanOrError -> {
                        if (booleanOrError.data) {
                            Timber.d("Successfully added chamber_member");
                            waitAtChamber(member.getIntermediaryId());
                        } else {
                            Timber.e(booleanOrError.error);
                        }
                    });
                })
                .setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss()).show();
    }

    private void waitAtChamber(String uId) {
        Intent waitForegroundService = new Intent(getApplicationContext(), ChamberWaitingService.class)
                .putExtra(IntentTags.USER_ID.tag, uId);
        startService(waitForegroundService);
        Intent chamberActivity = new Intent(getApplicationContext(), ChamberActivityIntermediary.class);
        startActivity(chamberActivity);
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

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    proceedAfterNotificationPermission();
                } else {
                    Snackbar notPermittedSnack = Snackbar.make(binding.getRoot(), "Calling will not work without notification", Snackbar.LENGTH_INDEFINITE);
                    notPermittedSnack.setAction("Permit", v -> {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    }).show();
                }
            });
        }
    }


    private void askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                proceedAfterNotificationPermission();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // display an educational UI explaining to the user the features that will be enabled
                // by them granting the POST_NOTIFICATION permission. This UI should provide the user
                // "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                // If the user selects "No thanks," allow the user to continue without notifications.
                new AlertDialog.Builder(this)
                        .setTitle("Grant Notification")
                        .setMessage("The calling feature with doctor requires notifications to show. Grant the permission to use call feature")
                        .setNeutralButton("Later", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Ok", (dialog, which) -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                        .create().show();
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            proceedAfterNotificationPermission();
        }
    }

    private void proceedAfterNotificationPermission() {
        if (!preferenceManager.isChamberRunning()) {
            if (Repository.getInstance().isConnected()) {
                launchChamber();
            }
        } else {
            Timber.v("chamber waiting service is running. not show dialog. direct go to chamber");
            waitAtChamber(preferenceManager.getUser().getuId());
        }
    }

    private void notifyCall(Call call) {
        Intent acceptIntent = new Intent(MainActivity.this, BroadcastReceiver.class)
                .setAction(IntentTags.ACTION_ACCEPT_CALL.tag)
                .putExtra(IntentTags.CALL_OBJ.tag, call);
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, acceptIntent, PendingIntent.FLAG_IMMUTABLE);
        Intent rejectIntent = new Intent(MainActivity.this, BroadcastReceiver.class)
                .setAction(IntentTags.ACTION_REJECT_CALL.tag)
                .putExtra(IntentTags.CALL_OBJ.tag, call);
        PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, rejectIntent, PendingIntent.FLAG_IMMUTABLE);


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MainActivity.this, PublicVariables.CALL_CHANNEL_ID)
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

    public static class MainActivityViewModel extends ViewModel {
        public LiveData<Boolean> getNetStatus() {
            return Repository.getInstance().getNetStatus();
        }

        public LiveData<DataOrError<Boolean, Exception>> addChamberMember(ChamberMember chamberMember) {
            return Repository.getInstance().addChamberMember(chamberMember);
        }
    }
}