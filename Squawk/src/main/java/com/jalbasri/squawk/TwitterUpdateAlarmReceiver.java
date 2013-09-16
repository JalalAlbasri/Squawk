package com.jalbasri.squawk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Broadcast receiver to receive intents fired by the update alarms and start the update service.
 */

public class TwitterUpdateAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = TwitterUpdateAlarmReceiver.class.getSimpleName();

    public static final String ACTION_UPDATE_TWITTER_ALARM =
            "com.jalbasri.squawk.ACTION_UPDATE_TWITTER_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive, received alarm broadcast, starting update service....");
//        Intent twitterEndpointServiceIntent = new Intent(context, TwitterEndpointService.class);
//        context.startService(twitterEndpointServiceIntent);
    }

}
