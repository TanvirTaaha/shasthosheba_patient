package com.shasthosheba.patient.repo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.shasthosheba.patient.app.App;

public class Repository {
    private static Repository mInstance;

    public static void initialize() {
        mInstance = new Repository();
    }

    private Repository() {
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
     * @param context Application context
     * @return  0: No Internet available (maybe on airplane mode, or in the process of joining an wi-fi).
     *          1: Cellular (mobile data, 3G/4G/LTE whatever).
     *          2: Wi-fi.
     *          3: VPN
     */
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
}
