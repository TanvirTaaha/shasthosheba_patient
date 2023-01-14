package com.shasthosheba.patient.repo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.shasthosheba.patient.R;
import com.shasthosheba.patient.app.App;
import com.shasthosheba.patient.app.PreferenceManager;
import com.shasthosheba.patient.app.PublicVariables;
import com.shasthosheba.patient.model.ChamberMember;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import timber.log.Timber;

public class Repository {
    private static Repository mInstance;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseFirestore fireStore;

    public static void initialize() {
        mInstance = new Repository();
    }

    private Repository() {
        firebaseDatabase = FirebaseDatabase.getInstance(PublicVariables.FIREBASE_DB);
        fireStore = FirebaseFirestore.getInstance();

        ConnectivityManager conMan = (ConnectivityManager) App.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            conMan.registerDefaultNetworkCallback(networkCallback);
        } else {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
            conMan.registerNetworkCallback(request, networkCallback);
        }
    }

    public static Repository getInstance() {
        return mInstance;
    }

    public static FirebaseFirestore getFireStore() {
        return mInstance.fireStore;
    }

    public static FirebaseDatabase getFirebaseDatabase() {
        return mInstance.firebaseDatabase;
    }

    private final MutableLiveData<Boolean> netAvailable = new MutableLiveData<>();

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        //https://stackoverflow.com/q/25678216
        @Override
        public void onAvailable(@NonNull Network network) {
            netAvailable.postValue(true);
        }

        @Override
        public void onLost(@NonNull Network network) {
            // https://stackoverflow.com/q/70324348
            netAvailable.postValue(false);
        }

        @Override
        public void onUnavailable() {
            netAvailable.postValue(false);
        }
    };

    public LiveData<Boolean> getNetStatus() {
        return netAvailable;
    }

    public boolean isConnected() {
        return getConnectionType(App.getAppContext()) != 0;
    }

    /**
     * https://stackoverflow.com/a/53243938
     *
     * @param context Application context
     * @return 0: No Internet available (maybe on airplane mode, or in the process of joining an wi-fi).
     * 1: Cellular (mobile data, 3G/4G/LTE whatever).
     * 2: Wi-fi.
     * 3: VPN
     */
    @SuppressLint("ObsoleteSdkInt")
    @IntRange(from = 0, to = 3)
    public static int getConnectionType(Context context) {
        int result = 0; // Returns connection type. 0: none; 1: mobile data; 2: wifi; 3: vpn
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (cm != null) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        result = 2;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        result = 1;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        result = 3;
                    }
                }
            }
        } else {
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null) {
                    // connected to the internet
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        result = 2;
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                        result = 1;
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_VPN) {
                        result = 3;
                    }
                }
            }
        }
        return result;
    }

    private MutableLiveData<DataOrError<Boolean, Exception>> addChamberMemberLD;

    public LiveData<DataOrError<Boolean, Exception>> addChamberMember(ChamberMember chamberMember) {
        addChamberMemberLD = new MutableLiveData<>();
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Timber.e(task.getException(), "Fetching FCM registration token failed");
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    Timber.d("addChamberMember:New FCM token:%s", token);

                    chamberMember.setCallDeviceToken(token);
                    addChamberMember_Impl(chamberMember);
                });
        return addChamberMemberLD;
    }

    private LiveData<DataOrError<Boolean, Exception>> addChamberMember_Impl(ChamberMember chamberMember) {
        Map<String, Object> valueOfTime = new HashMap<>();
        valueOfTime.put("timestamp", ServerValue.TIMESTAMP);
        String uId = chamberMember.getIntermediaryId();

        OnCompleteListener<Void> chamberAddCompleteListener = task -> {
            if (task.isSuccessful()) {
                Timber.d("chamber member add successfull:%s", chamberMember);
                addChamberMemberLD.postValue(new DataOrError<>(true, null));
            } else {
                Timber.d("chamber member add failed:%s", chamberMember);
                addChamberMemberLD.postValue(new DataOrError<>(false, task.getException()));
            }
        };

        firebaseDatabase.getReference(PublicVariables.SERVER_TIME_STAMP_NODE)
                .child(uId).setValue(valueOfTime)
                .addOnSuccessListener(unused -> firebaseDatabase.getReference(PublicVariables.SERVER_TIME_STAMP_NODE).child(uId).get()
                        .addOnCompleteListener(task -> {
                            Timber.d("server time set successful and get callback");
                            Long timestamp = null;
                            if (task.isSuccessful()) {
                                timestamp = task.getResult().child("timestamp").getValue(Long.class);
                                Timber.d("server time set success and get successful. timestamp:%s", timestamp);
                            }
                            if (timestamp == null) {
                                timestamp = System.currentTimeMillis();
                                Timber.d("server time set success but get failed. timestamp:%s", timestamp);
                            }
                            Timber.d("timestamp:%s", timestamp);
                            chamberMember.setTimestamp(timestamp);
                            firebaseDatabase.getReference(PublicVariables.CHAMBER_KEY).child(Long.toString(chamberMember.getTimestamp())).setValue(chamberMember)
                                    .addOnCompleteListener(chamberAddCompleteListener);
                            deleteTimeStamp(uId);
                        }))
                .addOnFailureListener(e -> {
                    long timestamp = System.currentTimeMillis();
                    Timber.d("server time set failed:%s", timestamp);
                    chamberMember.setTimestamp(timestamp);
                    firebaseDatabase.getReference(PublicVariables.CHAMBER_KEY).child(Long.toString(chamberMember.getTimestamp())).setValue(chamberMember)
                            .addOnCompleteListener(chamberAddCompleteListener);
                });
        return addChamberMemberLD;
    }

    private void deleteTimeStamp(String key) {
        firebaseDatabase.getReference(PublicVariables.SERVER_TIME_STAMP_NODE).child(key).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Timber.i("Server timestamp cache remove success");
                    } else {
                        Timber.i("Server timestamp cache remove failed");
                        Timber.e(task.getException());
                    }
                });
    }

    public LiveData<DataOrError<Boolean, Exception>> removeChamberMember(String uId) {
        MutableLiveData<DataOrError<Boolean, Exception>> dataOrErrorLD = new MutableLiveData<>();
        firebaseDatabase.getReference(PublicVariables.CHAMBER_KEY).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DataSnapshot snap : task.getResult().getChildren()) {
                    try {
                        ChamberMember chamMem = snap.getValue(ChamberMember.class);
                        if (chamMem != null && chamMem.getIntermediaryId().equals(uId)) {
                            deleteChamberMember(
                                    Long.toString(chamMem.getTimestamp()),
                                    task1 -> dataOrErrorLD.postValue(
                                            new DataOrError<>(task1.isSuccessful(), task1.getException())));
                        }
                    } catch (Exception e) {
                        if ((e instanceof DatabaseException) != false) {
                            Timber.w("Cannot convert:key:%s", snap.getKey());
                        } else {
                            Timber.e(e);
                        }
                    }
                }
            }
        });
        return dataOrErrorLD;
    }

    private void deleteChamberMember(String timestamp, OnCompleteListener<Void> completeListener) {
        firebaseDatabase.getReference(PublicVariables.CHAMBER_KEY).child(timestamp).removeValue()
                .addOnCompleteListener(completeListener);
    }

    private FirebaseRealtimeListLiveData<ChamberMember> allChamberMembersLD;

    public LiveData<DataOrError<List<ChamberMember>, DatabaseException>> getAllChamberMembers() {
        if (allChamberMembersLD == null) {
            Timber.d("setting livedata with dataReference");
            allChamberMembersLD = new FirebaseRealtimeListLiveData<>(firebaseDatabase.getReference(PublicVariables.CHAMBER_KEY), ChamberMember.class);
        }
        return allChamberMembersLD;
    }

    public void setNewTokenOnChamberIfExists(String uId, String newToken) {
        firebaseDatabase.getReference(PublicVariables.CHAMBER_KEY).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DataSnapshot snap : task.getResult().getChildren()) {
                    try {
                        ChamberMember chamMem = snap.getValue(ChamberMember.class);
                        if (chamMem != null && chamMem.getIntermediaryId().equals(uId)) {
                            String timestamp = Long.toString(chamMem.getTimestamp());
                            chamMem.setCallDeviceToken(newToken);
                            firebaseDatabase.getReference(PublicVariables.CHAMBER_KEY).child(timestamp).setValue(chamMem).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Timber.v("Added new token on uid:%s", uId);
                                    } else {
                                        Timber.w(task.getException(), "tried to update token but shit happened");
                                    }
                                }
                            });
                            return;
                        }
                    } catch (Exception e) {
                        if ((e instanceof DatabaseException) != false) {
                            Timber.w("Cannot convert:key:%s", snap.getKey());
                        } else {
                            Timber.e(e);
                        }
                    }
                }
            }
        });
    }
}
