package com.jalbasri.squawk;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;
import com.jalbasri.squawk.deviceinfoendpoint.Deviceinfoendpoint;
import com.jalbasri.squawk.deviceinfoendpoint.model.Tweet;
import com.jalbasri.squawk.deviceinfoendpoint.model.Status;

//import twitter4j.Status;

import java.io.IOException;
import java.util.List;

/**
 * Android Serivce that connects to App Engine Backend to query for twitter status updates.
 */

public class TwitterEndpointService extends IntentService {
    private static final String TAG = TwitterEndpointService.class.getSimpleName();

    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;

    private SharedPreferences prefs;
    private String mDeviceId;

    private Deviceinfoendpoint deviceinfoendpoint;

    public TwitterEndpointService() {
        super("TwitterEndpointService");
    }

    public TwitterEndpointService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Twitter Endpoint Service Started.....");
        super.onCreate();

        //Set up the alarms
        alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        String ALARM_ACTION = TwitterUpdateAlarmReceiver.ACTION_UPDATE_TWITTER_ALARM;
        Intent intentToFire = new Intent(ALARM_ACTION);
        alarmIntent = PendingIntent.getBroadcast(this, 0, intentToFire, 0);

        prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        mDeviceId = prefs.getString(MainActivity.KEY_DEVICE_ID, MainActivity.DEFAULT_DEVICE_ID);

        //Set up the endpoint
        Deviceinfoendpoint.Builder stockpricealertendpointBuilder = new Deviceinfoendpoint.Builder(
                AndroidHttp.newCompatibleTransport(), new JacksonFactory(),
                new HttpRequestInitializer() {
                    public void initialize(HttpRequest httpRequest) {
                    }
                });
        deviceinfoendpoint = CloudEndpointUtils.updateBuilder(stockpricealertendpointBuilder).build();
    }

    @Override
    public void onHandleIntent(Intent intent) {
        //Register the alarms
        boolean autoUpdate = prefs.getBoolean(SettingsActivity.PREF_AUTO_UPDATE, false);
        int updateFrequency = prefs.getInt(SettingsActivity.PREF_UPDATE_FREQUENCY, 0);
        if (autoUpdate) {
            int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
            long timeToRefresh = SystemClock.elapsedRealtime() + updateFrequency;
            alarmManager
                    .setInexactRepeating(alarmType, timeToRefresh, updateFrequency, alarmIntent);
        } else
            alarmManager.cancel(alarmIntent);

        getNewTweets();

        //TODO Connect to endpoint, retrieve new tweets and add them to the database

    }

    /**
     * Retrieve new tweets for this device from the TwitterEndpoint.
     */

    private void getNewTweets() {
        if (!mDeviceId.equals(MainActivity.DEFAULT_DEVICE_ID)) {
            List<Tweet> tweets;
            try {
                tweets = deviceinfoendpoint.getNewTweets(mDeviceId).execute().getItems();

                for (Tweet tweet : tweets) {
                    addNewTwitterStatus(tweet.getStatus());
                }

            } catch (IOException e) {
                Log.e(TAG, "IOException occurred when trying to retrieve tweets from the" +
                        "endpoint server: " + e);
            }
        }

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
                    .KEY_CREATED_AT, status.getCreatedAt().getValue());
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
                    .KEY_USER_URL, status.getUser().getUrl());
            values.put(TwitterStatusContentProvider
                    .KEY_LATITUDE, status.getGeoLocation().getLatitude());
            values.put(TwitterStatusContentProvider
                    .KEY_LONGITUDE, status.getGeoLocation().getLongitude());
            resolver.insert(TwitterStatusContentProvider
                    .CONTENT_URI, values);
        }
        query.close();
    }

}
