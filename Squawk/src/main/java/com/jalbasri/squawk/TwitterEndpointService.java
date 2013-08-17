package com.jalbasri.squawk;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;
import com.jalbasri.squawk.deviceinfoendpoint.Deviceinfoendpoint;
import com.jalbasri.squawk.deviceinfoendpoint.model.Tweet;

import java.util.List;

/**
 * Android Serivce that connects to App Engine Backend to query for twitter status updates.
 */

public class TwitterEndpointService extends IntentService {
    private static final String TAG = TwitterEndpointService.class.getSimpleName();

    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;

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
        Context context = getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

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
        List<Tweet> tweets;

    }


}
