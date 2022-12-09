package com.shasthosheba.patient.model;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

public abstract class BaseModel {
    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
