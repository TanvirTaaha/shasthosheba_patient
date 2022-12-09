package com.shasthosheba.patient.model;

import java.util.List;

public class Intermediary extends BaseModel{
    private String name;
    private String id;
    private List<String> patients;

    public Intermediary(String name, String id, List<String> patients) {
        this.name = name;
        this.id = id;
        this.patients = patients;
    }

    public Intermediary() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getPatients() {
        return patients;
    }

    public void setPatients(List<String> patients) {
        this.patients = patients;
    }
}
