package com.jalbasri.squawk;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.maps.model.LatLng;
import com.jalbasri.squawk.amazon.Amazon;

import java.util.Date;

public class MainActivity extends Activity implements
        StatusMapFragment.OnMapFragmentCreatedListener,
        LocationProvider.OnNewLocationListener {

    private static final String TAG = MainActivity.class.getName();

    private String mDeviceId;
    private int mRegisteredVersion;
    private long mDeviceIdExpirationTime;
    private int mRadius = 1;
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
    //Default Device Id expiration time set to one week.
    private static final long DEVICE_ID_EXPIRATION_TIME = 1000 * 3600 * 24 * 7;
//    private static final String MAP_FRAGMENT_TAG = "map_fragment_tag";

    ContentResolver mContentResolver;
    private LocationProvider mLocationProvider;
    private StatusMapFragment mStatusMapFragment;
    private TabListener<StatusListFragment> mListTabListener;
    private TabListener<StatusMapFragment> mMapTabListener;
    private ActionBar mActionBar;
    private Amazon mAmazon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContentResolver = getContentResolver();
        int appVersion = getAppVersion();
        updateFromPreferences();
        Log.d(TAG, "[Registration] Checks DeviceId = " + mDeviceId +
                ", Current App Version = " + appVersion +
                ", Preferences App Version = " + mRegisteredVersion +
                ", Expiration Time = " + mDeviceIdExpirationTime +
                ", Current Time = " + System.currentTimeMillis()
        );

        if (mDeviceId.equals("") || appVersion != mRegisteredVersion) {

            Log.d(TAG, "[Registration] Device Id not found in Preferences.");
            Intent registerIntent = new Intent(this, RegisterActivity.class);
            startActivityForResult(registerIntent, REGISTER_SUBACTIVITY);

        } else {
            Date expirationDate = new Date(mDeviceIdExpirationTime);
            Log.d(TAG, "[Registration] Device Already Registered with Id: " + mDeviceId + ". " +
                    "Registration will expire on " + expirationDate);
        }
        setContentView(R.layout.activity_main);

        //Initialize the ActionBar
        initActionBar();
        mLocationProvider = new LocationProvider(this);
        mAmazon = new Amazon();
        //TODO Check Wifi or GPS and prompt user to turn on if off.
        //TODO Check that Google Play Services exists on device. http://developer.android.com/google/gcm/client.html
        //TODO Remove radius
        //TODO Update Amazon when the map view changes not just the location
        /*
        If we have no device Id, the app version number has changed since registration or
        the registration Id has expired, acquire and new registration key.
         */


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REGISTER_SUBACTIVITY:
                if (resultCode == RESULT_OK) {
                    Bundle deviceInfoBundle = data.getBundleExtra("deviceInfoBundle");
                    Log.d(TAG, "[Registration] onActivityResult OK, " + (deviceInfoBundle != null) );
                    if (deviceInfoBundle != null) {
                            saveDeviceInfo(deviceInfoBundle);
                            Log.d(TAG, "[Registration] onActivityResult: RegisterActivity Successful");
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

    /**
     * Callback called when a new location is acquired in the Location Provider
     * Moves the map to the new location.
     * Updates the Amazon Server with the new location.
     */

    @Override
    public void onNewLocation(Location location) {
        //set mStatusMapFragment
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
        if (location != null && mStatusMapFragment != null && mDeviceId != null) {
            mStatusMapFragment.moveMaptoLocation(
                    new LatLng(location.getLatitude(), location.getLongitude()));

            double[][] mapRegion = mStatusMapFragment.getMapRegion();
            if (mapRegion != null) {
                Log.d(TAG, "Amazon.addDevice - onNewLocation");
                mAmazon.addDevice(mDeviceId, mapRegion);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //set mStatusMapFragment, and restore tab state.
        View fragmentContainer = findViewById(R.id.fragment_container);
        boolean tabletLayout = fragmentContainer == null;
        if (!tabletLayout) {
            SharedPreferences pref = getPreferences(Activity.MODE_PRIVATE);
            int actionBarIndex = pref.getInt(KEY_ACTION_BAR_INDEX, 0);
            mActionBar.setSelectedNavigationItem(actionBarIndex);
            mStatusMapFragment = (StatusMapFragment) getFragmentManager()
                    .findFragmentByTag(StatusMapFragment.class.getName());
        } else {
            mStatusMapFragment = ((StatusMapFragment) getFragmentManager()
                    .findFragmentById(R.id.map_fragment));
        }
        if (mStatusMapFragment != null) {
            double[][] mapRegion = mStatusMapFragment.getMapRegion();
            if (mapRegion != null) {
                Log.d(TAG, "Amazon.addDevice - onResume");
                mAmazon.addDevice(mDeviceId,mapRegion);
            }
        }
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
                        Log.d(TAG, "Amazon.addDevice - ActionReload");
                        mAmazon.addDevice(mDeviceId,mapRegion);
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
    protected void onPause() {
        if (mLocationProvider != null) {
            mLocationProvider.unregisterLocationListeners();
        }
        super.onPause();
    }

    /**
     * Callback used to move the map camera to the last known location once
     * the map is loaded.
     */
    @Override
    public void onMapFragmentCreated() {
        Log.d(TAG, "on map fragment created");
        Location location = mLocationProvider.getLocation();
        //set mStatusMapFragment
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

        if (location != null && mStatusMapFragment != null) {
            mStatusMapFragment.moveMaptoLocation(
                    new LatLng(location.getLatitude(), location.getLongitude()));
        }
    }

    @Override
    public void onMapRegionChanged() {
        if (mStatusMapFragment != null) {
            double[][] mapRegion = mStatusMapFragment.getMapRegion();
            if (mapRegion != null) {
                Log.d(TAG, "Amazon.addDevice - onMapRegionChanged");
                mAmazon.addDevice(mDeviceId,mapRegion);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState()");
        View fragmentContainer = findViewById(R.id.fragment_container);
        boolean tabletLayout = fragmentContainer == null;
        if (!tabletLayout) {
            int actionBarIndex = mActionBar.getSelectedTab().getPosition();
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
        mAmazon.removeDevice(mDeviceId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        mAmazon.removeDevice(mDeviceId);
        super.onDestroy();
    }

    /*
     * Helper Functions
     *
     */

    private int getAppVersion() {
        try {
            PackageInfo packageInfo = getPackageManager()
                    .getPackageInfo(getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private void saveDeviceInfo(Bundle deviceInfoBundle) {

        String deviceId = deviceInfoBundle.getString("deviceInfoId");
        String deviceInformation = deviceInfoBundle.getString("deviceInformation");
        Long deviceTimestamp =  deviceInfoBundle.getLong("deviceTimestamp");

        Log.d(TAG, "[Registration] saveDeviceId, DeviceId = " + deviceId);
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

        showDialog("Successfully registered with endpoint server." +
                " Registration Id: " + deviceId);
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

        //Update Server Registration Information
        mDeviceInformation = sharedPreferences
                .getString(KEY_SERVER_DEVICE_INFORMATION, DEFAULT_DEVICE_INFORMATION);
        mTimestamp = sharedPreferences
                .getLong(KEY_SERVER_DEVICE_TIMESTAMP, DEFAULT_DEVICE_TIMESTAMP);

        //Update Radius
        mRadius = Integer.parseInt(sharedPreferences
                .getString(SettingsActivity.PREF_RADIUS_LIST, DEFAULT_PREF_RADIUS));
        Log.d(TAG, "Radius: " + mRadius);

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
            String fragmentTag = fragmentClass.getName();
//            getFragmentManager().executePendingTransactions();
            fragment = getFragmentManager().findFragmentByTag(fragmentTag);
            if (fragment == null) {
                fragment = Fragment.instantiate(activity, fragmentTag);
                transaction.add(fragmentContainer, fragment, fragmentTag);
            } else {
                Log.d(TAG, "onTabSelected, Attach Fragment: " + fragmentClass.getSimpleName());
                transaction.attach(fragment);
            }
        }

        public void onTabUnselected(Tab tab, FragmentTransaction transaction) {
            if (fragment != null) {
                Log.d(TAG, "onTabUnselected, Detach Fragment: " + fragmentClass.getSimpleName());
                transaction.detach(fragment);
            }

        }

        public void onTabReselected(Tab tab, FragmentTransaction transaction) {
            if (fragment != null) {
                Log.d(TAG, "onTabReselected, Attach Fragment: " + fragmentClass.getSimpleName());
                transaction.attach(fragment);
            }
        }
    }
}