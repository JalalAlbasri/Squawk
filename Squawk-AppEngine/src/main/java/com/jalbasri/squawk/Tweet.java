package com.jalbasri.squawk;

import twitter4j.Status;

public class Tweet {
    private Status status;
    private String deviceId;

    public Tweet(Status status, String deviceId) {
        this.status = status;
        this.deviceId = deviceId;
    }

    public Status getStatus() {
        return this.status;
    }

}
