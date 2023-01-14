package com.shasthosheba.patient.model;

import java.io.Serializable;

public class User extends BaseModel implements Serializable {
    private String name;
    private String status;
    private String uId;

    public User(String uId, String name, String status) {
        this.uId = uId;
        this.name = name;
        this.status = status;
    }

    public User() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getuId() {
        return uId;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }
}
