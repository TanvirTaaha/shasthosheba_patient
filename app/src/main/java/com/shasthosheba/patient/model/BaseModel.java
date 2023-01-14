package com.shasthosheba.patient.model;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.Serializable;

public abstract class BaseModel implements Serializable {
    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
