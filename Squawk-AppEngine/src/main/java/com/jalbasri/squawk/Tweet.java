package com.jalbasri.squawk;

import twitter4j.Status;

public class Tweet {
    Status status;
    String deviceId;

    public Tweet(Status status, String deviceId) {
        this.status = status;
        this.deviceId = deviceId;
    }

}
