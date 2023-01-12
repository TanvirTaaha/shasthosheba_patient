package com.shasthosheba.patient.ui.patient;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shasthosheba.patient.app.PreferenceManager;
import com.shasthosheba.patient.app.PublicVariables;
import com.shasthosheba.patient.databinding.ActivityAddPatientBinding;
import com.shasthosheba.patient.model.Intermediary;
import com.shasthosheba.patient.model.Patient;
import com.shasthosheba.patient.util.Utils;

import java.util.ArrayList;
import java.util.Objects;

import timber.log.Timber;

public class AddPatientActivity extends AppCompatActivity {
    private FirebaseFirestore fireStoreDB = FirebaseFirestore.getInstance();
    private ActivityAddPatientBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddPatientBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(this);
        Utils.setStatusOnline(this);
        binding.btnAddPatient.setOnClickListener(v -> {
            Patient patient = new Patient();
            if (Objects.requireNonNull(binding.tietPatientName.getText()).toString().trim().isEmpty()) {
                binding.tilPatientName.setError("Please enter name");
                return;
            } else {
                binding.tilPatientName.setErrorEnabled(false);
                patient.setName(binding.tietPatientName.getText().toString().trim());
            }
            if (Objects.requireNonNull(binding.tietPatientBirthYear.getText()).toString().trim().isEmpty()) {
                binding.tilPatientBirthYear.setError("Please enter age");
                return;
            } else {
                binding.tilPatientBirthYear.setErrorEnabled(false);
                patient.setBirthYear(Integer.parseInt(binding.tietPatientBirthYear.getText().toString().trim()));
            }
            binding.tilPatientName.setEnabled(false);
            binding.tilPatientBirthYear.setEnabled(false);
            fireStoreDB.collection(PublicVariables.PATIENTS_KEY).add(patient).addOnSuccessListener(documentReference -> {
                String patientId = documentReference.getId();
                Timber.d("new patientId:%s", patientId);
                Timber.d("new patient doc path:%s", documentReference.getPath());
                Intermediary intermediary = preferenceManager.getIntermediary();
                intermediary.getPatients().add(patientId);
                patient.setId(patientId);
                documentReference.set(patient)
                        .addOnSuccessListener(unused -> {
//                            fireStoreDB.collection(PublicVariables.INTERMEDIARY_KEY).document(intermediary.getId()).set(intermediary)
//                                    .addOnSuccessListener(unused1 -> finish())
//                                    .addOnFailureListener(e -> {
//                                        binding.tilPatientName.setEnabled(true);
//                                        binding.tilPatientBirthYear.setEnabled(true);
//                                        Timber.e(e);
//                                    });
                            fireStoreDB.collection(PublicVariables.INTERMEDIARY_KEY).document(intermediary.getId())
                                    .update(PublicVariables.INTERMEDIARY_PATIENT_IDs, FieldValue.arrayUnion(patientId))
                                    .addOnSuccessListener(unused1 -> finish())
                                    .addOnFailureListener(e -> {
                                        binding.tilPatientName.setEnabled(true);
                                        binding.tilPatientBirthYear.setEnabled(true);
                                        Timber.e(e);
                                    });
                        })
                        .addOnFailureListener(e -> {
                            binding.tilPatientName.setEnabled(true);
                            binding.tilPatientBirthYear.setEnabled(true);
                            Timber.e(e);
                        });

            }).addOnFailureListener(e -> {
                binding.tilPatientName.setEnabled(true);
                binding.tilPatientBirthYear.setEnabled(true);
                Timber.e(e);
            });
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.setStatusOnline(this);
    }
}