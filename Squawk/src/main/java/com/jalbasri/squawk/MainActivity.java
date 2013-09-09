package com.jalbasri.squawk;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.app.Activity;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.jalbasri.squawk.deviceinfoendpoint.model.DeviceInfo;
import com.jalbasri.squawk.deviceinfoendpoint.model.MapRegion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends Activity implements
        StatusMapFragment.OnMapFragmentCreatedListener,
        LocationProvider.OnNewLocationListener {

    private static final String TAG = MainActivity.class.getName();

    private String mDeviceId;
    private int mRegisteredVersion;
    private long mDeviceIdExpirationTime;
    private int mRadius = 1;
    private boolean mBound = false;
    private long mTimestamp;
    private String mDeviceInformation;

    private static final int REGISTER_SUBACTIVITY = 1;
    private static final int SHOW_PREFERENCES = 2;
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_ACTION_BAR_INDEX = "action_bar_index";
    public static final String KEY_DEVICE_ID_EXPIRATION_TIME = "expiration_time";
    public static final String KEY_SERVER_DEVICE_INFORMATION = "server_device_information";
    public static final String KEY_SERVER_DEVICE_TIMESTAMP = "server_device_timestamp";
    public static final String KEY_APP_VERSION = "app_version";
    public static final String DEFAULT_DEVICE_ID = "";
    public static final String DEFAULT_PREF_RADIUS = "1";
    public static final String DEFAULT_DEVICE_INFORMATION = "";
    public static final long DEFAULT_DEVICE_TIMESTAMP = -1;
    public static final long DEFAULT_DEVICE_ID_EXPIRATION_TIME = -1;
    public static final int DEFAULT_APP_VERSION = -1;
    private static final String MAP_FRAGMENT_TAG = "map_fragment_tag";
    //Default Device Id expiration time set to one week.
    private static final long DEVICE_ID_EXPIRATION_TIME = 1000 * 3600 * 24 * 7;

    ContentResolver mContentResolver;
    private LocationProvider mLocationProvider;
    private TwitterStatusUpdateService mTwitterStatusUpdateService;
    private StatusMapFragment mStatusMapFragment;

    private TabListener<StatusListFragment> mListTabListener;
    private TabListener<StatusMapFragment> mMapTabListener;
    private ActionBar mActionBar;
    //TODO Remove the reload button.
    private Button mReloadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateFromPreferences();
        setContentView(R.layout.activity_main);
        mContentResolver = getContentResolver();
        //Initialize the ActionBar
        initActionBar();
        mLocationProvider = new LocationProvider(this);
        //TODO Check Wifi or GPS and prompt user to turn on if off.
        //TODO Check that Google Play Services exists on device. http://developer.android.com/google/gcm/client.html

        int appVersion = getAppVersion();

        /*
        If we have no device Id, the app version number has changed since registration or
        the registration Id has expired, acquire and new registration key.
         */
        Log.d(TAG, "[Registration] Checks DeviceId = " + mDeviceId +
                ", Current App Version = " + appVersion +
                ", Preferences App Version = " + mRegisteredVersion);

        Log.d(TAG, "[Registration] DeviceId Check: " + (mDeviceId.equals("")) +
                ", AppVersion Check: " + (appVersion != mRegisteredVersion) +
                ", ExpirationTime Check: " + (System.currentTimeMillis() > mDeviceIdExpirationTime));

        if (mDeviceId.equals("") || appVersion != mRegisteredVersion ||
                System.currentTimeMillis() > mDeviceIdExpirationTime) {

            Log.d(TAG, "[Registration] Device Id not found in Preferences.");
            Intent registerIntent = new Intent(this, RegisterActivity.class);
            startActivityForResult(registerIntent, REGISTER_SUBACTIVITY);

        } else {
            Date expirationDate = new Date(mDeviceIdExpirationTime);
            Log.d(TAG, "[Registration] Device Already Registered with Id: " + mDeviceId + ". " +
                    "Registration will expire on " + expirationDate);
        }

    }

    private int getAppVersion() {
        try {
            PackageInfo packageInfo = getPackageManager()
                    .getPackageInfo(getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private void updateFromPreferences() {
        Context context = getApplicationContext();
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        //Update Device Registration Id.
        mDeviceId = sharedPreferences.getString(KEY_DEVICE_ID, DEFAULT_DEVICE_ID);
        Log.d(TAG, "updateFromPreferences(), Device Registration Id: " + mDeviceId);

        //Update Device Registration Id. Expiration
        mDeviceIdExpirationTime = sharedPreferences
                .getLong(KEY_DEVICE_ID_EXPIRATION_TIME, DEFAULT_DEVICE_ID_EXPIRATION_TIME);

        //Update Registered App Version
        mRegisteredVersion = sharedPreferences.getInt(KEY_APP_VERSION, DEFAULT_APP_VERSION);

        mDeviceInformation = sharedPreferences
                .getString(KEY_SERVER_DEVICE_INFORMATION, DEFAULT_DEVICE_INFORMATION);
        mTimestamp = sharedPreferences
                .getLong(KEY_SERVER_DEVICE_TIMESTAMP, DEFAULT_DEVICE_TIMESTAMP);

        //Update Radius
        mRadius = Integer.parseInt(sharedPreferences
                .getString(SettingsActivity.PREF_RADIUS_LIST, DEFAULT_PREF_RADIUS));
        Log.d(TAG, "Radius: " + mRadius);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REGISTER_SUBACTIVITY:
                if (resultCode == RESULT_OK) {

                    Bundle deviceInfoBundle = data.getBundleExtra("deviceInfoBundle");

                    String deviceId = deviceInfoBundle.getString("deviceInfoId");
                    String deviceInformation = deviceInfoBundle.getString("deviceInformation");
                    Long deviceTimestamp =  deviceInfoBundle.getLong("deviceTimestamp");


                    if (deviceId != null) {
                        saveDeviceInfo(deviceId, deviceInformation, deviceTimestamp);
                        Log.d(TAG, "[Registration] onActivityResult: RegisterActivity Successful");
                        showDialog("Successfully registered with endpoint server." +
                                " Registration Id: " + deviceId);
                    }

                } else {
                    Log.d(TAG, "onActivityResult: RegisterActivity Failed");
                    showDialog("Error occurred registering with endpoint server.");
                }
            break;
            case SHOW_PREFERENCES:
                if (resultCode == RESULT_OK)
                    updateFromPreferences();
                break;
            default: break;
        }
    }

    private void saveDeviceInfo(String deviceId, String deviceInformation, Long deviceTimestamp) {
        Log.d(TAG, "[Registration] setDeviceId, DeviceId = " + deviceId);
        Context context = getApplicationContext();
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_DEVICE_ID, deviceId);
        editor.putString(KEY_SERVER_DEVICE_INFORMATION, deviceInformation);
        editor.putLong(KEY_SERVER_DEVICE_TIMESTAMP, deviceTimestamp);
        editor.putInt(KEY_APP_VERSION, getAppVersion());
        long expirationTime = System.currentTimeMillis() + DEVICE_ID_EXPIRATION_TIME;
        editor.putLong(KEY_DEVICE_ID_EXPIRATION_TIME, expirationTime);
        editor.commit();

    }

    /**
     * Update the deviceInfo taking the device online which in turn
     * explicitly starts the TwitterEndpointService to collect any existing tweets on the server.
     */

    public void updateDeviceInfo(DeviceInfo deviceInfo) {

//        if (location == null && mLocationProvider != null) {
//            Log.d(TAG, "reloadFeed()");
//            location = mLocationProvider.getLocation();
//            onNewLocation(location);
//        }
        updateFromPreferences();
        deviceInfo.setDeviceRegistrationID(mDeviceId)
                .setDeviceInformation(mDeviceInformation)
                .setTimestamp(mTimestamp);

        new UpdateDeviceInfoAsyncTask(this).execute(deviceInfo);

    }

    private void startTwitterEndpointService() {
        Intent twitterEndpointServiceIntent = new Intent(this, TwitterEndpointService.class);
        startService(twitterEndpointServiceIntent);
    }

    @Override
    public void onNewLocation(Location location) {

        if (location != null) {
            Log.d(TAG, "onNewLocation() loc: " + location.toString());
        } else {
            Log.d(TAG, "onNewLocation() location is null");
        }

        if (mStatusMapFragment == null) {
            View fragmentContainer = findViewById(R.id.fragment_container);
            boolean tabletLayout = fragmentContainer == null;

            if (!tabletLayout) {
                mStatusMapFragment = (StatusMapFragment) getFragmentManager()
                        .findFragmentByTag(StatusMapFragment.class.getName());
            } else {
                mStatusMapFragment = ((StatusMapFragment) getFragmentManager()
                        .findFragmentById(R.id.map_fragment));
            }
        }

        if (location != null) {
            Log.d(TAG, "onNewLocation(), map fragment null? " + (mStatusMapFragment == null) );
            if (mStatusMapFragment != null) {

                mStatusMapFragment.moveMaptoLocation(
                        new LatLng(location.getLatitude(), location.getLongitude()));

                double[][] mapRegion = mStatusMapFragment.getMapRegion();
                if (mapRegion != null) {
                    MapRegion deviceInfoMapRegion = new MapRegion();
                    deviceInfoMapRegion.setSouthWestLongitude(mapRegion[0][0])
                            .setSouthWestLatitude(mapRegion[0][1])
                            .setNorthEastLongitude(mapRegion[1][0])
                            .setNorthEastLatitude(mapRegion[1][1]);

                    DeviceInfo updatedDeviceInfo = new DeviceInfo();
                    updatedDeviceInfo.setMapRegion(deviceInfoMapRegion)
                            .setOnline(true);

                    updateDeviceInfo(updatedDeviceInfo);
                }
            }

        }
    }

    private void showDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        }).show();
    }

    @Override
    public void onResume() {
        super.onResume();

        View fragmentContainer = findViewById(R.id.fragment_container);
        boolean tabletLayout = fragmentContainer == null;

        if (!tabletLayout) {
            SharedPreferences pref = getPreferences(Activity.MODE_PRIVATE);
            int actionBarIndex = pref.getInt(KEY_ACTION_BAR_INDEX, 0);
            getActionBar().setSelectedNavigationItem(actionBarIndex);
            mStatusMapFragment = (StatusMapFragment) getFragmentManager()
                    .findFragmentByTag(StatusMapFragment.class.getName());
        } else {
            mStatusMapFragment = ((StatusMapFragment) getFragmentManager()
                    .findFragmentById(R.id.map_fragment));
        }

        startTwitterEndpointService();

//        if (!mBound) {
//            Start Twitter Update Service
//            Intent intent = new Intent(this, TwitterStatusUpdateService.class);
//            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
//
//        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent preferencesIntent = new Intent(this, SettingsActivity.class);
                startActivityForResult(preferencesIntent, SHOW_PREFERENCES);
                return true;

            case R.id.action_clear:
                mContentResolver.delete(TwitterStatusContentProvider.CONTENT_URI, null, null);
                if (mStatusMapFragment != null) {
                    mStatusMapFragment.clearMarkers();
                }
                return true;
            case R.id.action_reload:
                if (mStatusMapFragment != null) {
                    double[][] mapRegion = mStatusMapFragment.getMapRegion();
                    if (mapRegion != null) {
                        MapRegion deviceInfoMapRegion = new MapRegion();
                        deviceInfoMapRegion.setSouthWestLongitude(mapRegion[0][0])
                                .setSouthWestLatitude(mapRegion[0][1])
                                .setNorthEastLongitude(mapRegion[1][0])
                                .setNorthEastLatitude(mapRegion[1][1]);

                        DeviceInfo updatedDeviceInfo = new DeviceInfo();
                        updatedDeviceInfo.setMapRegion(deviceInfoMapRegion)
                                .setOnline(true);

                        updateDeviceInfo(updatedDeviceInfo);
                    }
                    else {
                        Log.d(TAG, "action_reload cancelled, could not obtain mapRegion");
                    }

                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        View fragmentContainer = findViewById(R.id.fragment_container);
        boolean tabletLayout = fragmentContainer == null;

        if (!tabletLayout) {
            mListTabListener.fragment = getFragmentManager()
                    .findFragmentByTag(StatusListFragment.class.getName());
            mMapTabListener.fragment = getFragmentManager()
                    .findFragmentByTag(MapFragment.class.getName());

            SharedPreferences pref = getPreferences(Activity.MODE_PRIVATE);
            int actionBarIndex = pref.getInt(KEY_ACTION_BAR_INDEX, 0);
            getActionBar().setSelectedNavigationItem(actionBarIndex);
        }
    }

    @Override
    protected void onPause() {
        if (mLocationProvider != null) {
            mLocationProvider.unregisterLocationListeners();
        }
        super.onPause();
    }

    private void initActionBar() {
        mActionBar = getActionBar();
        mActionBar.setDisplayUseLogoEnabled(false);
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        //TODO: Enable up navigation on icon in actionbar
        //mActionBar.setDisplayHomeAsUpEnabled(true);

        View fragmentContainer = findViewById(R.id.fragment_container);
        boolean tabletLayout = fragmentContainer == null;

        if (!tabletLayout) {
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

            Tab listTab = mActionBar.newTab();
            mListTabListener = new TabListener<StatusListFragment>
                    (this, StatusListFragment.class, R.id.fragment_container);
            listTab
//                    .setText("List")
                    .setIcon(R.drawable.collections_view_as_list)
                    .setContentDescription("List of Status Updates")
                    .setTabListener(mListTabListener);

            mActionBar.addTab(listTab);

            Tab mapTab = mActionBar.newTab();
            mMapTabListener = new TabListener<StatusMapFragment>
                    (this, StatusMapFragment.class, R.id.fragment_container);
            mapTab
//                    .setText("Map")
                    .setIcon(R.drawable.location_map)
                    .setContentDescription("Map of Status Updates")
                    .setTabListener(mMapTabListener);

            mActionBar.addTab(mapTab);
        }
    }

    /**
    Callback used to move the map camera to the last known location once
    the map is loaded.
     */
    @Override
    public void onMapFragmentCreated() {

        Location location = mLocationProvider.getLocation();
        if (mStatusMapFragment == null) {
            View fragmentContainer = findViewById(R.id.fragment_container);
            boolean tabletLayout = fragmentContainer == null;

            if (!tabletLayout) {

                mStatusMapFragment = (StatusMapFragment) getFragmentManager()
                        .findFragmentByTag(StatusMapFragment.class.getName());
            } else {
                mStatusMapFragment = ((StatusMapFragment) getFragmentManager()
                        .findFragmentById(R.id.map_fragment));
            }
        }
        Log.d(TAG, "onMapFragmentCreated() " + "location null? " + (location == null) + " map fragment null? " + (mStatusMapFragment == null));
        if (location != null && mStatusMapFragment != null) {
            mStatusMapFragment.moveMaptoLocation(
                    new LatLng(location.getLatitude(), location.getLongitude()));
        }
    }

    public class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private Fragment fragment;
        private Activity activity;
        private Class<T> fragmentClass;
        private int fragmentContainer;

        public TabListener(Activity activity, Class<T> fragmentClass, int fragmentContainer) {
            this.activity = activity;
            this.fragmentClass = fragmentClass;
            this.fragmentContainer = fragmentContainer;
        }

        public void onTabSelected(Tab tab, FragmentTransaction transaction) {

            if (fragment == null) {
                String fragmentName = fragmentClass.getName();
                fragment = Fragment.instantiate(activity, fragmentName);
                transaction.add(fragmentContainer, fragment, fragmentName);
            } else {
                transaction.attach(fragment);
            }

        }

        public void onTabUnselected(Tab tab, FragmentTransaction transaction) {
            if (fragment != null) {
                transaction.detach(fragment);
            }

        }

        public void onTabReselected(Tab tab, FragmentTransaction transaction) {
            if (fragment != null) {
                transaction.attach(fragment);
            }

        }
    }

//    private ServiceConnection mConnection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName className, IBinder service) {
//            Log.d(TAG, "Service Connected");
//            TwitterStatusUpdateService.TwitterServiceBinder binder =
//                    (TwitterStatusUpdateService.TwitterServiceBinder) service;
//            mTwitterStatusUpdateService = binder.getService();
//            mBound = true;
//            reloadFeed();
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName arg0) {
//            Log.d(TAG, "Service Disconnected");
//            mBound = false;
//        }
//    };
//
//    private void unbindFromTwitterService() {
//        if (mBound) {
//            unbindService(mConnection);
//            mBound = false;
//        }
//    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState()");
        View fragmentContainer = findViewById(R.id.fragment_container);
        boolean tabletLayout = fragmentContainer == null;

        if (!tabletLayout) {
            int actionBarIndex = getActionBar().getSelectedTab().getPosition();
            SharedPreferences.Editor editor = getPreferences(Activity.MODE_PRIVATE).edit();
            editor.putInt(KEY_ACTION_BAR_INDEX, actionBarIndex);
            editor.commit();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            if (mListTabListener.fragment != null) {
                transaction.detach(mListTabListener.fragment);
            }
            if (mMapTabListener.fragment != null) {
                transaction.detach(mMapTabListener.fragment);
            }
            transaction.commit();
        }

        updateDeviceInfo(new DeviceInfo().setOnline(false));

//        unbindFromTwitterService();
        super.onSaveInstanceState(outState);

    }

    @Override
    public void onDestroy() {
        //Unbind from Twitter Update Service
        Log.d(TAG, "onDestroy()");
//        unbindFromTwitterService();
        super.onDestroy();
    }

}
