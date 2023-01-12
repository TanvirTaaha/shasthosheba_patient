package com.shasthosheba.patient.repo;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

public class FirestoreDocumentLiveData<T> extends LiveData<DataOrError<T, FirebaseFirestoreException>>
        implements EventListener<DocumentSnapshot> {
    private final DocumentReference docRef;
    private final Class<T> valueType;
    private ListenerRegistration listenerRegistration;

    public FirestoreDocumentLiveData(DocumentReference docRef, Class<T> valueType) {
        this.docRef = docRef;
        this.valueType = valueType;
    }

    @Override
    protected void onActive() {
        super.onActive();
        listenerRegistration = docRef.addSnapshotListener(this);
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    @Override
    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
        postValue(new DataOrError<>(
                (snapshot != null && snapshot.exists()) ? snapshot.toObject(valueType) : null,
                error));
    }
}
