package com.shasthosheba.patient.ui.chamber;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseException;
import com.shasthosheba.patient.R;
import com.shasthosheba.patient.app.App;
import com.shasthosheba.patient.app.IntentTags;
import com.shasthosheba.patient.app.PreferenceManager;
import com.shasthosheba.patient.app.PublicVariables;
import com.shasthosheba.patient.databinding.ActivityChamberIntermediaryBinding;
import com.shasthosheba.patient.databinding.RcvChamberMemberListItemBinding;
import com.shasthosheba.patient.model.ChamberMember;
import com.shasthosheba.patient.repo.DataOrError;
import com.shasthosheba.patient.repo.Repository;
import com.shasthosheba.patient.ui.BroadcastReceiver;

import java.util.List;

import timber.log.Timber;

public class ChamberActivityIntermediary extends AppCompatActivity
        implements ChamberWaitingService.ClientActivity {

    private ActivityChamberIntermediaryBinding binding;
    private ChamberViewModel mViewModel;
    private ChamberMemberAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChamberIntermediaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mViewModel = new ViewModelProvider(this).get(ChamberViewModel.class);
        mViewModel.getAllChamberMembers().observe(this, dataOrError -> {
            int dataSize = 0;
            if (dataOrError.data != null) {
                mAdapter.submitList(dataOrError.data);
                dataSize = dataOrError.data.size();
                Timber.d("dataOrError.data:%s", dataOrError.data);
            }
            Timber.e(dataOrError.error);
            if (mAdapter.getItemCount() == 0 && dataSize == 0) {
                Timber.d("mAdapter.getCurrentList: %s", mAdapter.getCurrentList());
                binding.rcvChamberMemberList.setVisibility(View.GONE);
                binding.llEmpty.setVisibility(View.VISIBLE);
            } else {
                binding.rcvChamberMemberList.setVisibility(View.VISIBLE);
                binding.llEmpty.setVisibility(View.GONE);
            }
        });
        mViewModel.uId = new PreferenceManager(this).getUser().getuId();

        mAdapter = new ChamberMemberAdapter(
                mViewModel.uId,
                new DiffUtil.ItemCallback<ChamberMember>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull ChamberMember oldItem, @NonNull ChamberMember newItem) {
                        return oldItem.equals(newItem);
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull ChamberMember oldItem, @NonNull ChamberMember newItem) {
                        return oldItem.equals(newItem);
                    }
                });
        binding.rcvChamberMemberList.setLayoutManager(new LinearLayoutManager(this));
        binding.rcvChamberMemberList.setAdapter(mAdapter);

        binding.btnLeaveChamber.setOnClickListener(v -> {
            Timber.d("leave chamber button clicked");
            // stopService(new Intent(getApplicationContext(), ChamberWaitingService.class));
            // stopping doesn't work because this activity is bound to it and it is still running
            // instead send send broadcast
            Intent leaveChamberBroadcastIntent = new Intent(getApplicationContext(), BroadcastReceiver.class)
                    .setAction(IntentTags.ACTION_LEAVE_CHAMBER.tag)
                    .putExtra(IntentTags.USER_ID.tag, mViewModel.uId)
                    .addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            sendBroadcast(leaveChamberBroadcastIntent);
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        Timber.d("onStart");
        bindService(new Intent(getApplicationContext(), ChamberWaitingService.class), connection, Context.BIND_ABOVE_CLIENT);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Timber.d("onStop");
        unbindService(connection);
    }

    @Override
    public void finishSelf() {
        Timber.d("finishSelf");
        ChamberActivityIntermediary.this.finish();
    }

    public static class ChamberMemberAdapter extends ListAdapter<ChamberMember, ChamberMemberAdapter.ViewHolder> {
        private final String uId;

        protected ChamberMemberAdapter(String uId, @NonNull DiffUtil.ItemCallback<ChamberMember> diffCallback) {
            super(diffCallback);
            this.uId = uId;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(RcvChamberMemberListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChamberMember member = getItem(position);
            int backgroundDrawableId = R.drawable.rounded_rect_bg_selector;
            if (member.getIntermediaryId().equals(uId)) {
                backgroundDrawableId = R.drawable.rounded_rect_bg_selector_marked;
            }
            holder.binding.flRoot.setBackground(ContextCompat.getDrawable(holder.binding.getRoot().getContext(), backgroundDrawableId));
            holder.binding.tvName.setText(member.getName());
            if (member.isWithPayment()) {
                holder.binding.tvTransactionId.setVisibility(View.VISIBLE);
                holder.binding.tvBkashNo.setVisibility(View.VISIBLE);
                holder.binding.tvTransactionId.setText(App.getAppContext().getString(R.string.txn, member.getTransactionId()));
                holder.binding.tvBkashNo.setText(App.getAppContext().getString(R.string.bkash_with_param, member.getMemberBKashNo()));
            } else {
                holder.binding.tvTransactionId.setVisibility(View.GONE);
                holder.binding.tvBkashNo.setVisibility(View.GONE);
            }
        }

        private static class ViewHolder extends RecyclerView.ViewHolder {
            RcvChamberMemberListItemBinding binding;

            public ViewHolder(@NonNull RcvChamberMemberListItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ((ChamberWaitingService.WaitingServiceBinder) service).setClient(ChamberActivityIntermediary.this);
            Timber.d("onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Timber.d("onOptionsItemSelected: android.R.id.home");
            onBackPressed();
        }
        return true;
    }

    public static class ChamberViewModel extends ViewModel {
        private String uId;

        public LiveData<DataOrError<List<ChamberMember>, DatabaseException>> getAllChamberMembers() {
            return Repository.getInstance().getAllChamberMembers();
        }

        public LiveData<DataOrError<Boolean, Exception>> leaveChamber() {
            return Repository.getInstance().removeChamberMember(uId);
        }
    }
}