package com.shasthosheba.patient.ui.patient;

import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.shasthosheba.patient.app.IntentTags;
import com.shasthosheba.patient.app.PreferenceManager;
import com.shasthosheba.patient.app.PublicVariables;
import com.shasthosheba.patient.databinding.ActivityPatientDetailsBinding;
import com.shasthosheba.patient.databinding.RcvPresTitleItemBinding;
import com.shasthosheba.patient.model.Intermediary;
import com.shasthosheba.patient.model.Patient;
import com.shasthosheba.patient.model.Prescription;
import com.shasthosheba.patient.model.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;

import dmax.dialog.SpotsDialog;
import timber.log.Timber;

public class PatientDetailsActivity extends AppCompatActivity {

    private ActivityPatientDetailsBinding binding;
    private Patient patient;
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private PrescriptionTitleAdapter adapter;

    private String intermediaryId;
    boolean fetchDone = true;
    private AlertDialog alertDialog;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPatientDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(this);
        alertDialog = new SpotsDialog.Builder().setContext(this).setMessage("Loading...").build();
        if (!getIntent().hasExtra(IntentTags.PATIENT_OBJ.tag)) {
            Timber.e("No patient data in intent");
            Snackbar.make(binding.getRoot(), "Something went wrong", Snackbar.LENGTH_LONG).show();
            finish();
        }
        patient = new Gson().fromJson(getIntent().getStringExtra(IntentTags.PATIENT_OBJ.tag), Patient.class);

        intermediaryId = preferenceManager.getIntermediary().getId();

        binding.tvPatientName.setText(patient.getName());
        int age = new GregorianCalendar().get(Calendar.YEAR) - patient.getBirthYear();
        binding.tvPatientAge.setText(String.valueOf(age));

        adapter = new PrescriptionTitleAdapter(new ArrayList<>());
        binding.rcvPrescription.setLayoutManager(new LinearLayoutManager(this));
        binding.rcvPrescription.setAdapter(adapter);

        fetchAllPrescriptions();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchAllPrescriptions() {
        alertDialog.show();
        firestore.collection(PublicVariables.PATIENTS_KEY).document(patient.getId()).get()
                .addOnSuccessListener(documentSnapshotPatient -> {
                    List<String> prescriptionIds = Objects.requireNonNull(documentSnapshotPatient.toObject(Patient.class)).getPrescriptionIds();
                    if (prescriptionIds == null || prescriptionIds.isEmpty()) {
                        Timber.d("prescription list empty");
                        return;
                    }
                    fetchDone = false;


                    adapter.mList.clear();
                    Timber.d("adapter cleared:%s", adapter.getItemCount());
                    for (String presId : prescriptionIds) {
                        Timber.d("prescription id list:%s", prescriptionIds);
                        firestore.collection(PublicVariables.PRESCRIPTION_KEY).document(presId).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    Prescription fetchedPrescription = documentSnapshot.toObject(Prescription.class);
                                    Timber.d("fetched prescription:%s", fetchedPrescription);
                                    if (!adapter.mList.contains(fetchedPrescription)) {
                                        adapter.mList.add(fetchedPrescription);
                                        adapter.notifyItemInserted(adapter.getItemCount() - 1);
                                        fetchDone = true;
                                        alertDialog.dismiss();
                                    }
                                })
                                .addOnFailureListener(Timber::e);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(Timber::e);
    }


    private class PrescriptionTitleAdapter extends RecyclerView.Adapter<PrescriptionTitleAdapter.PresTitleViewHolder> {
        private List<Prescription> mList;

        public PrescriptionTitleAdapter(List<Prescription> mList) {
            this.mList = mList;
        }

        public List<Prescription> getList() {
            return mList;
        }

        @NonNull
        @Override
        public PresTitleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PresTitleViewHolder(RcvPresTitleItemBinding.inflate(getLayoutInflater(), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PresTitleViewHolder holder, int position) {
            Prescription prescription = mList.get(position);
            holder.binding.tvPresTitle.setText(prescription.getPrescriptionTitle());
            holder.binding.llRoot.setOnClickListener(v -> {
                if (fetchDone) {
//                    Toast.makeText(v.getContext(), prescription.getPrescriptionTitle(), Toast.LENGTH_LONG).show();startActivity(new Intent(v.getContext(), PrescriptionViewActivity.class)
                    startActivity(new Intent(v.getContext(), PrescriptionViewActivity.class)
                            .putExtra(IntentTags.PRESCRIPTION_OBJ.tag, prescription.toString()));

                }
            });
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        public class PresTitleViewHolder extends RecyclerView.ViewHolder {
            RcvPresTitleItemBinding binding;

            public PresTitleViewHolder(@NonNull RcvPresTitleItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}