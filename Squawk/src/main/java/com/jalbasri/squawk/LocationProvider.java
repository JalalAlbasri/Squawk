package com.jalbasri.squawk;

import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

public class LocationProvider {

    private static final String TAG = LocationProvider.class.getSimpleName();
    private LocationManager mLocationManager;
    private static int mMinUpdateTime = 5000;
    private static int mMinUpdateDistance = 1;
    private final Criteria mCriteria = new Criteria();
    private MainActivity mActivity;
    private OnNewLocationListener mOnNewLocationListener;
    private Location mLocation;

    public interface OnNewLocationListener {
        public void onNewLocation(Location location);
    }

    public LocationProvider(Activity activity) {
        mActivity = (MainActivity) activity;
        mOnNewLocationListener = mActivity;
        mLocationManager = (LocationManager)mActivity.getSystemService(Context.LOCATION_SERVICE);
        mCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        mCriteria.setPowerRequirement(Criteria.POWER_LOW);
        mCriteria.setCostAllowed(true);
        mCriteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        mCriteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
        mCriteria.setAltitudeRequired(true);
        mLocation = mLocationManager
                .getLastKnownLocation(mLocationManager.getBestProvider(mCriteria, true));
        registerLocationListeners();
    }

    public Location getLocation() {
        mLocation = mLocationManager
                .getLastKnownLocation(mLocationManager.getBestProvider(mCriteria, true));
        Log.d(TAG, "getLocation(), location = null: " + (mLocation == null));
        return mLocation;
    }

    private void registerLocationListeners() {
        Log.d(TAG, "registerLocationListeners()");
        String bestProvider = mLocationManager.getBestProvider(mCriteria, false);
        String bestAvailableProvider = mLocationManager.getBestProvider(mCriteria, true);

        Log.d(TAG, bestProvider + " / " + bestAvailableProvider);

        if (bestProvider == null) {
            //TODO: Message Could not load location provider
            Log.d(TAG, "No Location Provider on Device");
        } else if (bestProvider.equals(bestAvailableProvider)){
            mLocation = mLocationManager.getLastKnownLocation(bestAvailableProvider);
            if (mLocation != null) {
                Log.d(TAG, "New Location acquired from: " + bestAvailableProvider + " " + mLocation.toString());
                mOnNewLocationListener.onNewLocation(mLocation);
            }
            mLocationManager.requestLocationUpdates(bestAvailableProvider,
                    mMinUpdateTime, mMinUpdateDistance, mLocationListener);
        } else {
            mLocationManager.requestLocationUpdates(bestProvider,
                    mMinUpdateTime, mMinUpdateDistance, mLocationListener);

            if (bestAvailableProvider != null) {
                mLocation = mLocationManager.getLastKnownLocation(bestAvailableProvider);
                if (mLocation != null)
                    mOnNewLocationListener.onNewLocation(mLocation);
                mLocationManager.requestLocationUpdates(bestAvailableProvider,
                        mMinUpdateTime, mMinUpdateDistance, mLocationListener);
            } else {
                Log.d(TAG, "No Location Providers Available");
                List<String> allProviders = mLocationManager.getAllProviders();
                for (String provider : allProviders) {
                    mLocationManager.requestLocationUpdates(provider, 0, 0, mLocationListener);
                }
            }
        }
    }

    public void unregisterLocationListeners() {
        mLocationManager.removeUpdates(mLocationListener);
    }

    private LocationListener mLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            mOnNewLocationListener.onNewLocation(location);
        }

        public void onProviderDisabled(String provider) {

        }

        public void onProviderEnabled(String provider) {
            Log.d(TAG, "onProviderEnabled: " + provider);
            registerLocationListeners();
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

}
