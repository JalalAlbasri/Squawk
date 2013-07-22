package com.jalbasri.squawk;

//import android.app.IntentService;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;


public class TwitterStatusUpdateService extends Service {
    private static final String TAG = TwitterStatusUpdateService.class.getSimpleName();

    private ConfigurationBuilder configurationBuilder;
    private TwitterStream twitterStream;
    private final IBinder mBinder = new TwitterServiceBinder();

    public class TwitterServiceBinder extends Binder {

        TwitterStatusUpdateService getService() {
            return TwitterStatusUpdateService.this;
        }
    }

    @Override
    public void onCreate() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setDebugEnabled(true)
                .setOAuthConsumerKey("JmPCgGdftlNXuh21WQ7hFA")
                .setOAuthConsumerSecret("drMLhPvOWs2Crol2LwQuqdKVRTFCVbQlkJQOCrV8uI")
                .setOAuthAccessToken("72023528-NFWdbv2h4vDVdZC1ML2jNT0gXt9fqZLpMdvtGDjnH")
                .setOAuthAccessTokenSecret("JW7Y2e8D086oDsU1wpNKgtsPZAwF1TQl5KkMdbHdnQ");
        twitterStream = new TwitterStreamFactory(configurationBuilder.build()).getInstance();

        StatusListener statusListener = new StatusListener() {

            @Override
            public void onException(Exception arg0) {
                Log.d(TAG, "onException: cause," + arg0.getCause() + " message," + arg0.getMessage());
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice arg0) {
                Log.d(TAG, "onDeletionNotice()");
            }

            @Override
            public void onScrubGeo(long arg0, long arg1) {
                Log.d(TAG, "onScrubGeo()");
            }

            @Override
            public void onStallWarning(StallWarning stallWarning) {
                Log.d(TAG, "onStallWarning()");
            }

            @Override
            public void onStatus(Status status) {
                Log.d(TAG,
                        "Twitter Status Received: Geolocation?:" +
                                (status.getGeoLocation() != null) + " / " +
                                status.getUser().getScreenName() + ": " + status.getText());
                if (status.getGeoLocation() != null) {
                    addNewTwitterStatus(status);
                }
            }

            @Override
            public void onTrackLimitationNotice(int arg0) {
                Log.d(TAG, "onTrackLimitationNotice()");
            }
        };
        twitterStream.addListener(statusListener);

        return mBinder;
    }

    public void updateTwitterStream(Location location, int radius) {
        Log.d(TAG, "updateTwitterStream()");
        double[][] mapRegion = GeoCalculationsHelper.getMapRegion(location, radius);
        FilterQuery filterQuery = new FilterQuery();
        filterQuery.locations(mapRegion);
        twitterStream.filter(filterQuery);
    }

    private void addNewTwitterStatus(Status status) {
        ContentResolver resolver = getContentResolver();

        //Make sure status doesn't alraedy exist
        String where = TwitterStatusContentProvider.KEY_STATUS_ID + " = " + status.getId();
        Cursor query = resolver.query(TwitterStatusContentProvider.CONTENT_URI, null, where, null, null);
        if (query.getCount() == 0) {
            ContentValues values = new ContentValues();
            values.put(TwitterStatusContentProvider
                    .KEY_STATUS_ID, status.getId());
            values.put(TwitterStatusContentProvider
                    .KEY_CREATED_AT, status.getCreatedAt().getTime());
            values.put(TwitterStatusContentProvider
                    .KEY_STATUS_TEXT, status.getText());
            values.put(TwitterStatusContentProvider
                    .KEY_USER_ID, status.getUser().getId());
            values.put(TwitterStatusContentProvider
                    .KEY_USER_NAME, status.getUser().getName());
            values.put(TwitterStatusContentProvider
                    .KEY_USER_SCREEN_NAME, status.getUser().getScreenName());
            values.put(TwitterStatusContentProvider
                    .KEY_USER_IMAGE, status.getUser().getMiniProfileImageURL());
            values.put(TwitterStatusContentProvider
                    .KEY_USER_URL, status.getUser().getURL());
            values.put(TwitterStatusContentProvider
                    .KEY_LATITUDE, status.getGeoLocation().getLatitude());
            values.put(TwitterStatusContentProvider
                    .KEY_LONGITUDE, status.getGeoLocation().getLongitude());
            resolver.insert(TwitterStatusContentProvider
                    .CONTENT_URI, values);
        }
        query.close();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service Destroyed.");
        super.onDestroy();
    }
}
