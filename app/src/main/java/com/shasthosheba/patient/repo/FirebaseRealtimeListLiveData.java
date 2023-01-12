package com.shasthosheba.patient.repo;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class FirebaseRealtimeListLiveData<T> extends LiveData<DataOrError<List<T>, DatabaseException>> implements ValueEventListener {
    private final DatabaseReference databaseReference;
    private final Class<T> valueType;

    public FirebaseRealtimeListLiveData(DatabaseReference databaseReference, Class<T> valueType) {
        this.databaseReference = databaseReference;
        this.valueType = valueType;
    }

    @Override
    protected void onActive() {
        super.onActive();
        databaseReference.addValueEventListener(this);
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        databaseReference.removeEventListener(this);
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot snapshot) {
        if (snapshot.exists()) {
            List<T> list = new ArrayList<>();
            for (DataSnapshot snap : snapshot.getChildren()) {
                Timber.d("onDataChange:in for loop:item:%s", snap.getValue());
                try {
                    T item = snap.getValue(valueType);
                    list.add(item);
                } catch (Exception e) {
                    if ((e instanceof DatabaseException) != false) {
                        Timber.w("Cannot convert:key:%s", snap.getKey());
                    } else {
                        Timber.e(e);
                    }
                }
            }
            postValue(new DataOrError<>(list, null));
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {
        postValue(new DataOrError<>(null, error.toException()));
    }

    //    {
//        FirebaseDatabase.getInstance().getReference()
//    }
}
