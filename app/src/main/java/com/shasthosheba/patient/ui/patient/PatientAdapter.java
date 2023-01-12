package com.shasthosheba.patient.ui.patient;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shasthosheba.patient.app.IntentTags;
import com.shasthosheba.patient.databinding.RcvPatientItemBinding;
import com.shasthosheba.patient.model.Patient;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import timber.log.Timber;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {
    private final List<Patient> mList;

    public PatientAdapter(List<Patient> list) {
        this.mList = list;
    }

    public List<Patient> getList() {
        return this.mList;
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PatientViewHolder(RcvPatientItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        Patient patient = mList.get(position);
        Calendar calendar = new GregorianCalendar();
        int age = calendar.get(Calendar.YEAR) - patient.getBirthYear();
        holder.binding.tvAge.setText(String.valueOf(age));
        holder.binding.tvName.setText(patient.getName());
        holder.binding.llRoot.setOnClickListener(v -> {
            v.getContext().startActivity(new Intent(v.getContext(), PatientDetailsActivity.class)
                    .putExtra(IntentTags.PATIENT_OBJ.tag, patient.toString()));
//            Toast.makeText(v.getContext(), patient.getId(), Toast.LENGTH_LONG).show();
            Timber.d("Patient clicked:%s", patient);
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    protected static class PatientViewHolder extends RecyclerView.ViewHolder {
        protected RcvPatientItemBinding binding;

        protected PatientViewHolder(@NonNull RcvPatientItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

        }
    }
}
