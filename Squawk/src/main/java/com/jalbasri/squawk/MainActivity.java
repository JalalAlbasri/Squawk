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

public class MainActivity extends Activity implements
        StatusMapFragment.OnMapFragmentCreatedListener,
        LocationProvider.OnNewLocationListener {

    private static final String TAG = MainActivity.class.getName();

    private String mDeviceId;
    private int mRadius = 1;
    private boolean mBound = false;

    private static final int REGISTER_SUBACTIVITY = 1;
    private static final int SHOW_PREFERENCES = 2;
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_ACTION_BAR_INDEX = "action_bar_index";
    private static final String DEFAULT_DEVICE_ID = "";
    private static final String DEFAULT_PREF_RADIUS = "1";
    private static final String MAP_FRAGMENT_TAG = "map_fragment_tag";

    ContentResolver mContentResolver;
    private LocationProvider mLocationProvider;
    private TwitterStatusUpdateService mTwitterStatusUpdateService;
    private StatusMapFragment mStatusMapFragment;

    private TabListener<StatusListFragment> mListTabListener;
    private TabListener<StatusMapFragment> mMapTabListener;
    private ActionBar mActionBar;
    private Button mReloadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateFromPreferences();
        if (mDeviceId.equals(DEFAULT_DEVICE_ID)) { //Device is not registered.
            Intent registerIntent = new Intent(this, RegisterActivity.class);
            startActivityForResult(registerIntent, REGISTER_SUBACTIVITY);
        }

        //TODO Check Wifi or GPS and prompt user to turn on if off.

    }

    private void updateFromPreferences() {
        Context context = getApplicationContext();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        //Update Device Registration Id.
        mDeviceId = sharedPreferences.getString(KEY_DEVICE_ID, DEFAULT_DEVICE_ID);
        Log.d(TAG, "updateFromPreferences(), Device Registration Id: " + mDeviceId);
        //Update Radius
        mRadius = Integer.parseInt(sharedPreferences.getString(SettingsActivity.PREF_RADIUS_LIST, DEFAULT_PREF_RADIUS));
        Log.d(TAG, "Radius: " + mRadius);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REGISTER_SUBACTIVITY:
                if (resultCode == RESULT_OK) {
                        showDialog("Successfully registered with endpoint server.");
                        startMainActivity();
                } else {
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

    private void startMainActivity() {
        setContentView(R.layout.activity_main);
        mContentResolver = getContentResolver();
        //Initialize the ActionBar
        initActionBar();
        mLocationProvider = new LocationProvider(this);
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

        if (!mBound) {
            //Start Twitter Update Service
            Intent intent = new Intent(this, TwitterStatusUpdateService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
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
                reloadFeed();
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
        mLocationProvider.unregisterLocationListeners();
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

    public void reloadFeed() {
        if (mLocationProvider != null) {
            Log.d(TAG, "reloadFeed()");
            Location location;
            location = mLocationProvider.getLocation();
            Log.d(TAG, "reloadFeed() loc: " + location.toString());
            onNewLocation(location);
        }
    }

    @Override
    public void onNewLocation(Location location) {
//        Log.d(TAG, "onNewLocation() loc: " + location.toString());

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
            if (mStatusMapFragment != null)
                mStatusMapFragment.moveMaptoLocation(
                        new LatLng(location.getLatitude(), location.getLongitude()));
            if (mTwitterStatusUpdateService != null)
                mTwitterStatusUpdateService.updateTwitterStream(location, mRadius);
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

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "Service Connected");
            TwitterStatusUpdateService.TwitterServiceBinder binder =
                    (TwitterStatusUpdateService.TwitterServiceBinder) service;
            mTwitterStatusUpdateService = binder.getService();
            mBound = true;
            reloadFeed();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG, "Service Disconnected");
            mBound = false;
        }
    };

    private void unbindFromTwitterService() {
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState()");
        View fragmentContainer = findViewById(R.id.fragment_container);
        boolean tabletLayout = fragmentContainer == null;

        if (!tabletLayout) {
            int actionBarIndex = getActionBar().getSelectedTab().getPosition();
            SharedPreferences.Editor editor = getPreferences(Activity.MODE_PRIVATE).edit();
            editor.putInt(KEY_ACTION_BAR_INDEX, actionBarIndex);
            editor.apply();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            if (mListTabListener.fragment != null) {
                transaction.detach(mListTabListener.fragment);
            }
            if (mMapTabListener.fragment != null) {
                transaction.detach(mMapTabListener.fragment);
            }
            transaction.commit();
        }
        unbindFromTwitterService();
        super.onSaveInstanceState(outState);

    }

    @Override
    public void onDestroy() {
        //Unbind from Twitter Update Service
        Log.d(TAG, "onDestroy()");
        unbindFromTwitterService();
        super.onDestroy();
    }

}
