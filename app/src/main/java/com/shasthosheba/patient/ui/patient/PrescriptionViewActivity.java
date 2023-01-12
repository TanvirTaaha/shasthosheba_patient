package com.shasthosheba.patient.ui.patient;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.shasthosheba.patient.R;
import com.shasthosheba.patient.app.IntentTags;
import com.shasthosheba.patient.databinding.ActivityPrescriptionViewBinding;
import com.shasthosheba.patient.model.Prescription;

import timber.log.Timber;

public class PrescriptionViewActivity extends AppCompatActivity {
    private ActivityPrescriptionViewBinding binding;
    private Prescription prescription;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPrescriptionViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!getIntent().hasExtra(IntentTags.PRESCRIPTION_OBJ.tag)) {
            Snackbar.make(binding.getRoot(), "Something went wrong", Snackbar.LENGTH_LONG).show();
            finish();
        }

        prescription = new Gson().fromJson(getIntent().getStringExtra(IntentTags.PRESCRIPTION_OBJ.tag), Prescription.class);

        binding.tvDocName.setText(prescription.getDoctorName());
        binding.tvPatientName.setText(prescription.getPatientName());
        binding.tvIllnessDesc.setText((prescription.getIllnessDescription()));
        binding.lvMedicineList.setAdapter(new ArrayAdapter<>(this, R.layout.med_and_test_view_item, R.id.tv_text, prescription.getMedicines()));
        binding.lvTestsList.setAdapter(new ArrayAdapter<>(this, R.layout.med_and_test_view_item, R.id.tv_text, prescription.getTests()));
        binding.tvAdvice.setText(prescription.getAdvice());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Timber.d("onOptionsItemSelected: android.R.id.home");
            onBackPressed();
        }
        return true;
    }
}