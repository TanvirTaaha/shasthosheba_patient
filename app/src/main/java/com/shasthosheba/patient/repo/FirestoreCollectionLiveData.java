package com.shasthosheba.patient.repo;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class FirestoreCollectionLiveData<T> extends LiveData<DataOrError<List<T>, FirebaseFirestoreException>>
        implements EventListener<QuerySnapshot> {
    private final CollectionReference collRef;
    private final Class<T> valueType;
    private ListenerRegistration listenerRegistration;


    public FirestoreCollectionLiveData(CollectionReference collRef, Class<T> valueType) {
        this.collRef = collRef;
        this.valueType = valueType;
    }

    @Override
    protected void onActive() {
        super.onActive();
        listenerRegistration = collRef.addSnapshotListener(this);
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot snapshot, @Nullable FirebaseFirestoreException error) {
        postValue(new DataOrError<>(
                (snapshot != null) ? snapshot.toObjects(valueType) : null,
                error));
    }
}
