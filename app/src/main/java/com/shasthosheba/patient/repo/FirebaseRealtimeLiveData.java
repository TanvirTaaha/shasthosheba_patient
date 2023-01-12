package com.shasthosheba.patient.repo;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class FirebaseRealtimeLiveData<T> extends LiveData<DataOrError<T, DatabaseException>> implements ValueEventListener {
    private final DatabaseReference databaseReference;
    private final Class<T> valueType;

    public FirebaseRealtimeLiveData(DatabaseReference databaseReference, Class<T> valueType) {
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
            postValue(new DataOrError<>(snapshot.getValue(valueType), null));
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {
        postValue(new DataOrError<>(null, error.toException()));
    }
}
