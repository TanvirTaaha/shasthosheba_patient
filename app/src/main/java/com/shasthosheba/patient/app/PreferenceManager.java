package com.shasthosheba.patient.app;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.shasthosheba.patient.model.Intermediary;
import com.shasthosheba.patient.model.User;

public class PreferenceManager {
    private static final String PREFERENCE_STORAGE = "shasthosheba_pref_storage";

    public static enum PreferenceKey {
        USER("user"),
        INTERMEDIARY("intermediary"),
        CONNECTED("is_connected"),

        SERVER_TIMESTAMP_OF_CHAMBER("server_timestamp_of_chamber"),
        CHAMBER_RUNNING("chamber_is_running");

        private final String key;

        PreferenceKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    private Context context;
    private SharedPreferences preferences;
    private Gson mGson = new Gson();

    public PreferenceManager(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(PREFERENCE_STORAGE, Context.MODE_PRIVATE);
    }

    public void setUser(User user) {
        preferences.edit().putString(PreferenceKey.USER.key, mGson.toJson(user)).apply();
    }

    public User getUser() {
        return mGson.fromJson(preferences.getString(PreferenceKey.USER.key, ""), User.class);
    }

    public void setIntermediary(Intermediary intermediary) {
        preferences.edit().putString(PreferenceKey.INTERMEDIARY.key, mGson.toJson(intermediary)).apply();
    }

    public Intermediary getIntermediary() {
        return mGson.fromJson(preferences.getString(PreferenceKey.INTERMEDIARY.key, ""), Intermediary.class);
    }

    public void setConnected(boolean connected) {
        preferences.edit().putBoolean(PreferenceKey.CONNECTED.key, connected).apply();
    }

    public boolean isConnected() {
        return preferences.getBoolean(PreferenceKey.CONNECTED.key, false);
    }

    public void setChamberRunning(boolean isRunning) {
        preferences.edit().putBoolean(PreferenceKey.CHAMBER_RUNNING.key, isRunning).apply();
    }

    public boolean isChamberRunning() {
        return preferences.getBoolean(PreferenceKey.CHAMBER_RUNNING.key, false);
    }
}
