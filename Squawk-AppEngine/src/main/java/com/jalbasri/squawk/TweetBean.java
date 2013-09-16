package com.jalbasri.squawk;

import java.util.Arrays;

/**
 * Created by jalal on 11/09/13.
 */
public class TweetBean {

    String[] deviceIds;
    String id;
    String text;
    String created_at;
    String user_id;
    String user_name;
    String screen_name;
    String user_image;
    String user_url;
    String latitude;
    String longitude;

    public TweetBean() {

    }

    public String[] getDeviceIds() {
        return deviceIds;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getCreated_at() {
        return created_at;
    }

    public String getUser_id() {
        return user_id;
    }

    public String getUser_name() {
        return user_name;
    }

    public String getScreen_name() {
        return screen_name;
    }

    public String getUser_image() {
        return user_image;
    }

    public String getUser_url() {
        return user_url;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    @Override
    public String toString() {
        return "TweetBean{" +
                "deviceIds=" + Arrays.toString(deviceIds) +
                ", id='" + id + '\'' +
                ", text='" + text + '\'' +
                ", created_at='" + created_at + '\'' +
                ", user_id='" + user_id + '\'' +
                ", user_name='" + user_name + '\'' +
                ", screen_name='" + screen_name + '\'' +
                ", user_image='" + user_image + '\'' +
                ", user_url='" + user_url + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                '}';
    }
}
