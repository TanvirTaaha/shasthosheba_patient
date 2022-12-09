package com.shasthosheba.patient.app;

import android.app.Application;

import com.shasthosheba.patient.BuildConfig;
import com.shasthosheba.patient.R;
import com.shasthosheba.patient.TagTree;

import timber.log.Timber;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new TagTree(getString(R.string.app_name), true));
        }
    }
}
