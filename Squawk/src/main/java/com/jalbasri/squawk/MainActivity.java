package com.jalbasri.squawk;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;
import com.jalbasri.squawk.amazon.Amazon;

public class MainActivity extends Activity implements
        LocationListener,
        StatusMapFragment.OnMapFragmentCreatedListener,
        StatusListFragment.OnListFragmentCreatedListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener

{
    private static final String TAG = MainActivity.class.getName();

    private static final int REGISTER_SUBACTIVITY = 1;
    private static final int SHOW_PREFERENCES = 2;
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_ACTION_BAR_INDEX = "action_bar_index";
    public static final String KEY_DEVICE_ID_EXPIRATION_TIME = "expiration_time";
    public static final String KEY_APP_VERSION = "app_version";
    public static final String KEY_MAP_VIEW = "map_view";
    public static final String DEFAULT_DEVICE_ID = "";
    public static final String DEFAULT_DEVICE_INFORMATION = "";
    public static final long DEFAULT_DEVICE_TIMESTAMP = -1;
    public static final long DEFAULT_DEVICE_ID_EXPIRATION_TIME = -1;
    public static final int DEFAULT_APP_VERSION = -1;
    //Default Device Id expiration time set to one week.
    private static final long DEVICE_ID_EXPIRATION_TIME = 1000 * 3600 * 3;
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final int LOCATION_UPDATE_INTERVAL = 20000;
    private static final int LOCATION_FASTEST_UPDATE_INTERVAL = 20000;
//    private static final String MAP_FRAGMENT_TAG = "map_fragment_tag";

    ContentResolver mContentResolver;
    //    private LocationProvider mLocationProvider;
    private Location mLocation;
    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    private StatusMapFragment mStatusMapFragment;
    private StatusListFragment mStatusListFragment;
    //    private TabListener<StatusListFragment> mListTabListener;
//    private TabListener<StatusMapFragment> mMapTabListener;
    private ActionBar mActionBar;
    private Amazon mAmazon;
    private String mDeviceId;
    private int mRegisteredVersion;
    private long mDeviceIdExpirationTime;
    private LatLng mMapTarget;
    private boolean mFirstLaunch = true;
    //    private int mActionBarIndex;
    private boolean mMapView;
    private Menu mActionBarMenu;
    private long mPendingInfoWindow = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "ONCREATE()");
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

        /*
        Initialize the UI
         */
        setContentView(R.layout.activity_main);
        initActionBar();
        if (System.currentTimeMillis() > mDeviceIdExpirationTime) {
            Intent registerIntent = new Intent(this, RegisterActivity.class);
            startActivityForResult(registerIntent, REGISTER_SUBACTIVITY);
        }
        /*
            Initialize Amazon server object
         */
        mAmazon = new Amazon();

        /*
            Check for GooglePlayServices and define Location Client
         */
        if(servicesConnected()) {
            // Create the LocationRequest object
            mLocationRequest = LocationRequest.create();
            // Use high accuracy
            mLocationRequest.setPriority(
                    LocationRequest.PRIORITY_HIGH_ACCURACY);
            // Set the update interval to 5 seconds
            mLocationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
            // Set the fastest update interval to 1 second
            mLocationRequest.setFastestInterval(LOCATION_FASTEST_UPDATE_INTERVAL);
        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
            mLocationClient = new LocationClient(this, this, this);

        }

        //TODO Check Wifi or GPS and prompt user to turn on if off.
        //TODO Check that Google Play Services exists on device.
        // http://developer.android.com/google/gcm/client.html
        //TODO Fix amazon update times
        //TODO Remove settings from actionbar
        //TODO Pull down list to reload tweets
        //TODO Swipe back and forth between list and map
        //TODO Loading Screen / Message
        //TODO About
        //TODO Tweet Icon
        //TODO Go through warnings
        //TODO Replace Icon
        //TODO Change name
        //DONE Fix Twitter Server
        //TODO Languages and tweet encoding
        //TODO Default graphic for unloaded pictures
        //TODO InfoWindow bug, remains onscreen after clear, reappears if new tweet arrives
        //TODO InfoWindow click to open in list
        //TODO List click to show tweet in map crashes on tablet
        //TODO Remove old location provider implementation
        //TODO Remove unused layouts and resources
        //TODO Check theme of twitter WebView
        //TODO Remove Toasts
        //TODO Add new toasts

    }

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        //Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        mLocation = mLocationClient.getLastLocation();
        mLocationClient.requestLocationUpdates(mLocationRequest, this);

    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected from location services. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }


    /*
         * Called by Location Services if the attempt to
         * Location Services fails.
         */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(servicesConnected())
            mLocationClient.connect();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REGISTER_SUBACTIVITY:
                if (resultCode == RESULT_OK) {
                    Bundle deviceInfoBundle =
                            data.getBundleExtra("deviceInfoBundle");
                    String deviceRegistrationId = data.getStringExtra("deviceRegistrationId");
                    if (deviceRegistrationId != null) {
                        saveDeviceInfo(deviceRegistrationId);
                        Log.d(TAG, "[Registration] onActivityResult:" +
                                " RegisterActivity Successful");
                    }

                } else {
                    Log.d(TAG, "[Registration] onActivityResult: RegisterActivity Failed");
                    showDialog("Error occurred registering with endpoint server.");
                }
                break;
            case SHOW_PREFERENCES:
                if (resultCode == RESULT_OK)
                    updateFromPreferences();
                break;
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                if (resultCode == RESULT_OK) {
                    //TODO Try the request again, ie Check for google play services again
                    Log.d(TAG, "CONNECTION_FAILURE_RESOLUTION_REQUEST, Result Ok");
                }
            default: break;
        }
    }

    /**
     * Callback called when a new location is acquired in the Location Provider
     * Moves the map to the new location.
     * Updates the Amazon Server with the new location.
     */
    // Define the callback method that receives location updates
    @Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
        /*
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        */
        if (mFirstLaunch) {
            mFirstLaunch = false;
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
                mStatusMapFragment.snapMaptoLocation(
                        new LatLng(location.getLatitude(), location.getLongitude()), 10);

//                double[][] mapRegion = mStatusMapFragment.getMapRegion();
//                if (mapRegion != null) {
//                    Log.d(TAG, "Amazon.addDevice - onNewLocation");
//                    mAmazon.addDevice(mDeviceId, mapRegion);
//                }
            }
        }
    }

    public void onProviderDisabled(String provider) {

    }

    public void onProviderEnabled(String provider) {
        Log.d(TAG, "onProviderEnabled: " + provider);
        if(servicesConnected())
            mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onResume() {
        super.onResume();
        //set mStatusMapFragment, and restore tab state.
        View fragmentContainer = findViewById(R.id.fragment_container);
        boolean tabletLayout = fragmentContainer == null;
        if (!tabletLayout) {

            SharedPreferences pref = getPreferences(Activity.MODE_PRIVATE);
/*
    Enable for tabbed browsing
    int actionBarIndex = pref.getInt(KEY_ACTION_BAR_INDEX, 1);
    mActionBar.setSelectedNavigationItem(actionBarIndex);
 */

            mMapView = pref.getBoolean(KEY_MAP_VIEW, true); //defaults to map view
            if (mMapView) {
                attachNavigationFragment(StatusMapFragment.class, false);
            } else {
                attachNavigationFragment(StatusListFragment.class, false);
            }

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
                mAmazon.addDevice(mDeviceId, mapRegion);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu()");
        mActionBarMenu = menu;
        SharedPreferences pref = getPreferences(Activity.MODE_PRIVATE);

        View fragmentContainer = findViewById(R.id.fragment_container);
        boolean tabletLayout = fragmentContainer == null;

    //IllegalStateException from this code. Should not attach fragments in this callback.

        if (!tabletLayout) {

            mMapView = pref.getBoolean(KEY_MAP_VIEW, true);
            if (mMapView) { //List fragment selected, or start new
                menu.add(Menu.NONE, 0, Menu.NONE, "navigation")
                        .setIcon(R.drawable.ic_action_view_as_list)
                        .setTitle("List")
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
//                attachNavigationFragment(StatusMapFragment.class, false);
            } else {
                menu.add(Menu.NONE, 0, Menu.NONE, "navigation")
                        .setIcon(R.drawable.ic_action_map)
                        .setTitle("Map")
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
//                attachNavigationFragment(StatusListFragment.class, false);
            }
        }

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

            case android.R.id.home:
                if(getFragmentManager().getBackStackEntryCount() > 0)
                    getFragmentManager().popBackStack();
                return true;
            case 0:
                Log.d(TAG, "onOptionsItemSelected(), Navigation Selected.");
                if (mMapView) {
                    detachNavigationFragment(StatusMapFragment.class, false);
                    attachNavigationFragment(StatusListFragment.class, false);
                    mMapView = false;
//                    attachMapOrListFragment();
                    mActionBarMenu.getItem(0).setIcon(R.drawable.ic_action_map);
                } else {
                    detachNavigationFragment(StatusListFragment.class, false);
                    attachNavigationFragment(StatusMapFragment.class, false);
                    mMapView = true;
//                    attachMapOrListFragment();
                    mActionBarMenu.getItem(0).setIcon(R.drawable.ic_action_view_as_list);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void attachNavigationFragment(Class fragmentClass, boolean addToBackStack) {
        String fragmentTag = fragmentClass.getName();
        Fragment fragment = getFragmentManager().findFragmentByTag(fragmentTag);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

        if (fragment == null) {
            fragment = Fragment.instantiate(this, fragmentTag);
            fragmentTransaction.add(R.id.fragment_container, fragment, fragmentTag);
        } else {
            Log.d(TAG, "onTabSelected, Attach Fragment: " + fragmentClass.getSimpleName());
            fragmentTransaction.attach(fragment);
        }
        if (addToBackStack)
            fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void detachNavigationFragment(Class fragmentClass, boolean addToBackStack) {
        String fragmentTag = fragmentClass.getName();
        Fragment fragment = getFragmentManager().findFragmentByTag(fragmentTag);
        if (fragment != null) {
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.detach(fragment);
            if (addToBackStack)
                fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }
    }

    /*
    Callback called when a Status List Item is clicked
    Set in the xml layout status_list_item.xml
     */
    public void onItemClicked(View statusListItemView) {
        Log.d(TAG, "onItemClicked");
        TextView statusIdTextView = (TextView) statusListItemView.findViewById(R.id.status_id);
        Long statusId = Long.parseLong(statusIdTextView.getText().toString());

        TextView userNameTextView = (TextView) statusListItemView.findViewById(R.id.user_name);
        TextView screenNameTextView = (TextView) statusListItemView.findViewById(R.id.screen_name);
        TextView createdAtTextView = (TextView) statusListItemView.findViewById(R.id.created_at);
        TextView statusTextView = (TextView) statusListItemView.findViewById(R.id.status_text);
        ImageButton replyImageButton = (ImageButton) statusListItemView.findViewById(R.id.reply);
        ImageButton retweetImageButton = (ImageButton) statusListItemView.findViewById(R.id.retweet);
        ImageButton favoriteImageButton = (ImageButton) statusListItemView.findViewById(R.id.favorite);


        statusListItemView.setBackgroundResource(R.drawable.card_background);
        retweetImageButton.setBackgroundResource(R.drawable.button_background);
        replyImageButton.setBackgroundResource(R.drawable.button_background);
        favoriteImageButton.setBackgroundResource(R.drawable.button_background);

        replyImageButton.setPadding(3, 3, 3, 3);
        retweetImageButton.setPadding(3, 3, 3, 3);
        favoriteImageButton.setPadding(5, 5, 5, 5);

        screenNameTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));
        statusTextView.setLinkTextColor(userNameTextView.getLinkTextColors().getDefaultColor());
        createdAtTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));
        userNameTextView.setTextColor(Color.BLACK);
        statusTextView.setTextColor(Color.BLACK);

        replyImageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_reply));
        retweetImageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_repeat));
        favoriteImageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_important));

        View fragmentContainer = findViewById(R.id.fragment_container);
        boolean tabletLayout = fragmentContainer == null;
        if (!tabletLayout) {
            Log.d(TAG, "onItemClicked2");
            detachNavigationFragment(StatusListFragment.class, false);
            attachNavigationFragment(StatusMapFragment.class, false);
            mMapView = true;
            mActionBarMenu.getItem(0).setIcon(R.drawable.ic_action_view_as_list);
        }

        if (mStatusMapFragment == null) {
            Log.d(TAG, "[InfoWindowDebug] Status Map Fragment is null");
            mPendingInfoWindow = statusId;
        } else {
            Log.d(TAG, "[InfoWindowDebug] select marker");
            mStatusMapFragment.selectMarker(statusId);
        }

    }

    @Override
    protected void onPause() {
//        if (mLocationProvider != null) {
//            mLocationProvider.unregisterLocationListeners();
//        }
        super.onPause();
    }

    private void setUpNavigation() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        } else {
            mActionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public void onListFragmentCreated() {
        if (mActionBarMenu != null)
            mActionBarMenu.getItem(0).setIcon(R.drawable.ic_action_map);
        setUpNavigation();
    }

    /**
     * Callback used to move the map camera to the last known location once
     * the map is loaded. Called from StatusMapFragment.
     */
    @Override
    public void onMapFragmentCreated() {
        Log.d(TAG, "onMapFragmentCreated()");
        if (mActionBarMenu != null)
            mActionBarMenu.getItem(0).setIcon(R.drawable.ic_action_view_as_list);
        setUpNavigation();
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

        if(mStatusMapFragment != null) {
            mMapTarget = mStatusMapFragment.getMapTarget();
            Log.d(TAG, "mMapTarget, location != null: " + (mLocation != null));
            Log.d(TAG, "mMapTarget, target != null: " + (mMapTarget != null));
            if (mMapTarget != null && mMapTarget.latitude !=0 && mMapTarget.longitude != 0) {
                Log.d(TAG, "Move Map to Target");
                mStatusMapFragment.snapMaptoLocation(mMapTarget);
            } else if (mLocation != null) {
                Log.d(TAG, "Move Map to Location");
                mStatusMapFragment.snapMaptoLocation(
                        new LatLng(mLocation.getLatitude(), mLocation.getLongitude()), 10);
            }
        }

        if (mPendingInfoWindow != 0) {
            mStatusMapFragment.selectMarker(mPendingInfoWindow);
            mPendingInfoWindow = 0;
        }
    }

    @Override
    public void onMapRegionChanged() {
        if (mStatusMapFragment != null) {
            double[][] mapRegion = mStatusMapFragment.getMapRegion();
            if (mapRegion != null) {
                Log.d(TAG, "Amazon.addDevice - onMapRegionChanged, : " +
                        mDeviceId + " MapRegion: " + mapRegion.toString());
                mAmazon.addDevice(mDeviceId, mapRegion);
            }
            mMapTarget = mStatusMapFragment.getMapTarget();
        }
    }

    @Override
    public void onInfoWindowClicked(long statusId) {
        Log.d(TAG, "[infowindowclick]");
        View fragmentContainer = findViewById(R.id.fragment_container);
        boolean tabletLayout = fragmentContainer == null;

        if (!tabletLayout) {
            detachNavigationFragment(StatusMapFragment.class, false);
            attachNavigationFragment(StatusListFragment.class, false);
            mMapView = false;
            mActionBarMenu.getItem(0).setIcon(R.drawable.ic_action_map);

            mStatusListFragment = (StatusListFragment) getFragmentManager()
                    .findFragmentByTag(StatusListFragment.class.getName());
        } else {
            mStatusListFragment = ((StatusListFragment) getFragmentManager()
                    .findFragmentById(R.id.status_list_fragment));
        }

        if (mStatusListFragment != null)
            mStatusListFragment.selectListItem(statusId);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState()");
        View fragmentContainer = findViewById(R.id.fragment_container);
        boolean tabletLayout = fragmentContainer == null;
        if (!tabletLayout) {
//            int actionBarIndex = mActionBar.getSelectedTab().getPosition();
            SharedPreferences.Editor editor = getPreferences(Activity.MODE_PRIVATE).edit();
//            editor.putInt(KEY_ACTION_BAR_INDEX, mActionBarIndex);
            editor.putBoolean(KEY_MAP_VIEW, mMapView);
            editor.commit();

            /*
            Detach both fragments
             */
            detachNavigationFragment(StatusMapFragment.class, false);
            detachNavigationFragment(StatusListFragment.class, false);

            /*
            Enable for tab navigation.
             */

//            FragmentTransaction transaction = getFragmentManager().beginTransaction();
//            if (mListTabListener.fragment != null) {
//                transaction.detach(mListTabListener.fragment);
//            }
//            if (mMapTabListener.fragment != null) {
//                transaction.detach(mMapTabListener.fragment);
//            }
//            transaction.commit();
        }
        mAmazon.removeDevice(mDeviceId);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        if(servicesConnected()) {
            mLocationClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        mAmazon.removeDevice(mDeviceId);
        super.onDestroy();
    }

    /*
     * Helper Functions
     *
     */
    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates",
                    "Google Play services is available.");
            // Continue
            return true;
        } else { // Google Play services was not available for some reason
            // Get the error code
            int errorCode = resultCode;
            // Get the error dialog from Google Play services
            showErrorDialog(errorCode);
            return false;
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

    private void saveDeviceInfo(String deviceId) {

        Log.d(TAG, "[Registration] saveDeviceId, DeviceId = " + deviceId);
        Context context = getApplicationContext();
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(context).edit();

        editor.putString(KEY_DEVICE_ID, deviceId);
        editor.putInt(KEY_APP_VERSION, getAppVersion());
        long expirationTime = System.currentTimeMillis() + DEVICE_ID_EXPIRATION_TIME;
        editor.putLong(KEY_DEVICE_ID_EXPIRATION_TIME, expirationTime);
        editor.commit();
        mDeviceId = deviceId;
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

    private void showErrorDialog(int errorCode) {
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                errorCode,
                this,
                CONNECTION_FAILURE_RESOLUTION_REQUEST);
        // If Google Play services can provide an error dialog
        if (errorDialog != null) {
            // Create a new DialogFragment for the error dialog
            ErrorDialogFragment errorFragment =
                    new ErrorDialogFragment();
            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);
            // Show the error dialog in the DialogFragment
            errorFragment.show(
                    getFragmentManager(),
                    "Location Updates");
        }
    }



    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;
        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }



    private void updateFromPreferences() {
        Context context = getApplicationContext();
        //Set Preferences to default value, only executes on first run.
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
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

    }

    private void initActionBar() {
        mActionBar = getActionBar();
//        mActionBar.setDisplayUseLogoEnabled(true);
//        mActionBar.setDisplayShowHomeEnabled(false);
//        mActionBar.setDisplayShowTitleEnabled(true);

//        mActionBar.setDisplayHomeAsUpEnabled(true);

//        /*
//        Initiate Tab Listeners.
//         */
//
//        View fragmentContainer = findViewById(R.id.fragment_container);
//        boolean tabletLayout = fragmentContainer == null;
//        if (!tabletLayout) {
//
//            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
//
//            Tab listTab = mActionBar.newTab();
//            mListTabListener = new TabListener<StatusListFragment>
//                    (this, StatusListFragment.class, R.id.fragment_container);
//            listTab
////                    .setText("List")
//                    .setIcon(R.drawable.ic_action_view_as_list)
//                    .setContentDescription("List of Status Updates")
//                    .setTabListener(mListTabListener);
//
//            mActionBar.addTab(listTab);
//
//            Tab mapTab = mActionBar.newTab();
//            mMapTabListener = new TabListener<StatusMapFragment>
//                    (this, StatusMapFragment.class, R.id.fragment_container);
//            mapTab
////                    .setText("Map")
//                    .setIcon(R.drawable.ic_action_map)
//                    .setContentDescription("Map of Status Updates")
//                    .setTabListener(mMapTabListener);
//
//            mActionBar.addTab(mapTab);
//        }
    }



//    public class TabListener<T extends Fragment> implements ActionBar.TabListener {
//        private Fragment fragment;
//        private Activity activity;
//        private Class<T> fragmentClass;
//        private int fragmentContainer;
//
//        public TabListener(Activity activity, Class<T> fragmentClass, int fragmentContainer) {
//            this.activity = activity;
//            this.fragmentClass = fragmentClass;
//            this.fragmentContainer = fragmentContainer;
//        }
//
//        public void onTabSelected(Tab tab, FragmentTransaction transaction) {
//            String fragmentTag = fragmentClass.getName();
////            getFragmentManager().executePendingTransactions();
//            fragment = getFragmentManager().findFragmentByTag(fragmentTag);
//            if (fragment == null) {
//                fragment = Fragment.instantiate(activity, fragmentTag);
//                transaction.add(fragmentContainer, fragment, fragmentTag);
//            } else {
//                Log.d(TAG, "onTabSelected, Attach Fragment: " + fragmentClass.getSimpleName());
//                transaction.attach(fragment);
//            }
//            mActionBarIndex = mActionBar.getSelectedTab().getPosition();
//            mActionBar.removeTab(tab);
//        }
//
//        public void onTabUnselected(Tab tab, FragmentTransaction transaction) {
//            if (fragment != null) {
//                Log.d(TAG, "onTabUnselected, Detach Fragment: " + fragmentClass.getSimpleName());
//                transaction.detach(fragment);
//            }
//            mActionBar.addTab(tab);
//        }
//
//        public void onTabReselected(Tab tab, FragmentTransaction transaction) {
//            if (fragment != null) {
//                Log.d(TAG, "onTabReselected, Attach Fragment: " + fragmentClass.getSimpleName());
//                transaction.attach(fragment);
//            }
//        }
//    }


}