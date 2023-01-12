package com.shasthosheba.patient.repo;

public class DataOrError<T, E extends Exception> {
    public final T data;
    public final E error;

    public DataOrError(T data, E error) {
        this.data = data;
        this.error = error;
    }
}
