package com.shasthosheba.patient.model;

import java.io.Serializable;

public class Call extends BaseModel implements Serializable {
    private String wants;
    private boolean video;
    private String room;
    private String doctor;

    public Call() {
    }

    public Call(String wants, boolean video, String room, String doctor) {
        this.wants = wants;
        this.video = video;
        this.room = room;
        this.doctor = doctor;
    }

    public boolean isVideo() {
        return video;
    }

    public void setVideo(boolean video) {
        this.video = video;
    }

    public String getWants() {
        return wants;
    }

    public void setWants(String wants) {
        this.wants = wants;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getDoctor() {
        return doctor;
    }

    public void setDoctor(String doctor) {
        this.doctor = doctor;
    }
}
