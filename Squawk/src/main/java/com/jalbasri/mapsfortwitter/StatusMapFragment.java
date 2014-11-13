package com.jalbasri.mapsfortwitter;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import com.koushikdutta.ion.Ion;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

//import org.apache.commons.validator.routines.UrlValidator;

//import twitter4j.Twitter;

/*
    Warning, com.google.android.gms.maps.model.LatLng is used
    instead of android.location.Location for map functions.
 */
public class StatusMapFragment extends MapFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = StatusMapFragment.class.getName();

    private OnMapFragmentCreatedListener mOnMapFragmentCreatedListener;
    private static final int TWITTER_STATUS_LOADER = 0;
    private static final String PREF_MOVE_MAP = "pref_center_marker_checkbox";
    private MainActivity mActivity;
    private Cursor mCursor;
    private GoogleMap mGoogleMap;
    private Marker mLastMarker;
    private String mSortOrder = TwitterStatusContentProvider.KEY_CREATED_AT + " DESC";
    //    private final int MARKER_LIMIT = 200;
    private LinkedHashMap<Long, Marker> mMarkers;
    private long mPendingInfoWindow = 0;
    private boolean mCenterSelectedMarker;

    public interface OnMapFragmentCreatedListener {
        public void onMapFragmentCreated();
        public void onMapRegionChanged();
        public void onInfoWindowClicked(long statusId);
    }

    @Override
    public void onAttach(Activity activity){
        Log.d(TAG, "map onAttach()");
        super.onAttach(activity);
        this.mActivity = (MainActivity) activity;
        try {
            mOnMapFragmentCreatedListener = (OnMapFragmentCreatedListener) activity;
        } catch (ClassCastException e){
            Log.d(TAG, e.getMessage());
        }

        getLoaderManager().initLoader(TWITTER_STATUS_LOADER, null, this);
        setRetainInstance(true);
        mMarkers = new LinkedHashMap<Long, Marker>() {
//            @Override
//            protected boolean removeEldestEntry(Map.Entry eldest) {
//                if (size() > MARKER_LIMIT) {
//                    Marker eldestMarker = (Marker) eldest.getValue();
//                    eldestMarker.remove();
//                    return true;
//                }
//                return false;
//            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        LatLng lastMapTarget = null;
        View v = super.onCreateView(inflater, container, savedInstanceState);
        initGoogleMap();
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceBundle) {
        Log.d(TAG, "map onActivityCreated() " + (mGoogleMap != null));
        mOnMapFragmentCreatedListener.onMapFragmentCreated();
        super.onActivityCreated(savedInstanceBundle);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "map onResume()");
        super.onResume();
        updateFromPreferences();
        if (mGoogleMap == null) {
            initGoogleMap();
        }
        getLoaderManager().restartLoader(TWITTER_STATUS_LOADER, null, this);
    }

    private void initGoogleMap() {
        Log.d(TAG, "initGoogleMap()");
        mGoogleMap = getMap();
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mGoogleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                mOnMapFragmentCreatedListener.onMapRegionChanged();
            }
        });
        mGoogleMap.setOnMarkerClickListener(onMapMarkerClickListener);
        mGoogleMap.setOnMapClickListener(onMapClickListener);
        mGoogleMap.setOnInfoWindowClickListener(onInfoWindowClickListener);

        /*
        Move Location controls to bottom
        **Hack, High potential of failing**
        View locationControls = getView().findViewById(0x2);
        if (locationControls != null
                && locationControls.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) locationControls.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

            final int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30,
                    getResources().getDisplayMetrics());
            params.setMargins(margin, margin, margin, margin);
        }
        */

       /*
       Custom Info Window Adapter
        */
        mGoogleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                View infoWindowView = mActivity.getLayoutInflater()
                        .inflate(R.layout.status_map_info_window, null);
                ImageView userImageView = (ImageView) infoWindowView.findViewById(R.id.info_window_user_image);
                TextView userNameTextView = (TextView) infoWindowView.findViewById(R.id.info_window_user_name);
                TextView screenNameTextView = (TextView) infoWindowView.findViewById(R.id.info_window_screen_name);
                TextView statusTextView = (TextView) infoWindowView.findViewById(R.id.info_window_status_text);

                String markerIdString = marker.getTitle();
                long markerId = Long.parseLong(markerIdString);

                ContentResolver contentResolver = mActivity.getContentResolver();
                Uri uri = Uri.withAppendedPath(TwitterStatusContentProvider.CONTENT_URI, markerIdString);
                Cursor cursor = contentResolver.query(uri, null, null, null, null);

                if (cursor.getCount() > 0 && cursor.moveToNext()) {
                    String userImageUrl = cursor.getString(cursor.getColumnIndex(TwitterStatusContentProvider.KEY_USER_IMAGE));

//                    UrlImageViewHelper.setUrlDrawable(userImageView, userImageUrl, R.drawable.user_image_placeholder);
                    Log.d(TAG, "[IMAGE LOAD] " + userImageUrl);
                    Ion.with(mActivity)
                            .load(userImageUrl)
                            .withBitmap()
                            .placeholder(R.drawable.user_image_placeholder)
                            .intoImageView(userImageView);

//                    Ion.with(userImageView)
//                            .placeholder(R.drawable.user_image_placeholder)
//                            .load(userImageUrl);

                    String userName = cursor.getString(cursor.getColumnIndex(TwitterStatusContentProvider.KEY_USER_NAME));
                    String statusText = cursor.getString(cursor.getColumnIndex(TwitterStatusContentProvider.KEY_STATUS_TEXT));
                    String screenName = cursor.getString(cursor.getColumnIndex(TwitterStatusContentProvider.KEY_USER_SCREEN_NAME));

                    userNameTextView.setText(userName);
                    screenNameTextView.setText("@"+screenName);
                    statusTextView.setText(statusText);
                }
                return infoWindowView;
            }
        });
        Log.d(TAG, "initGoogleMap finished " + (mGoogleMap != null));
    }

    public void snapMaptoLocation(LatLng latLng, float zoom) {
        Log.d(TAG, "move map to location " + (mGoogleMap != null));
        if (mGoogleMap != null) {
            CameraPosition cameraPosition = new CameraPosition(latLng, zoom, 0, 0);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
            mGoogleMap.moveCamera(cameraUpdate);
        }
    }

    public void snapMaptoLocation(LatLng latLng) {
        Log.d(TAG, "move map to location " + (mGoogleMap != null));
        if (mGoogleMap != null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    public void clearMarkers() {
        if (mLastMarker != null) {
            mLastMarker.hideInfoWindow();
        }
        if (mGoogleMap != null) {
            mGoogleMap.clear();
        }
        if (mMarkers != null) {
            mMarkers.clear();
        }
    }

    public double[][] getMapRegion() {
        if (mGoogleMap != null) {
            VisibleRegion visibleRegion = mGoogleMap.getProjection().getVisibleRegion();
            return new double[][] {{visibleRegion.nearLeft.latitude, visibleRegion.nearLeft.longitude},
                    {visibleRegion.farRight.latitude, visibleRegion.farRight.longitude}};
        }
        return null;
    }

    public LatLng getMapTarget() {
        if (mGoogleMap != null) {
            return mGoogleMap.getCameraPosition().target;
        }
        return null;
    }

    public void selectMarker(long statusId) {

        Log.d(TAG, "selectMarker()");
        for (Map.Entry<Long, Marker> marker: mMarkers.entrySet()) {
            if (marker.getKey() == statusId) {
                clickMarker(marker.getValue());
                break;
            }
        }

        if (mCursor != null) {
            while (mCursor.moveToNext()) {
                if (statusId ==
                        (mCursor.getLong(mCursor.getColumnIndex(TwitterStatusContentProvider.KEY_STATUS_ID)))) {
                    LatLng latLng = new LatLng(
                            mCursor.getDouble(mCursor
                                    .getColumnIndex(TwitterStatusContentProvider.KEY_LATITUDE)),
                            mCursor.getDouble(mCursor
                                    .getColumnIndex(TwitterStatusContentProvider.KEY_LONGITUDE)));
                    String title = mCursor.getString(mCursor
                            .getColumnIndex(TwitterStatusContentProvider.KEY_STATUS_ID));
                    Marker marker = drawMarker(latLng, title);
                    if (marker != null)
                        clickMarker(marker);
                    break;
                }
            }
        }
        else {
            mPendingInfoWindow = statusId;
        }

    }

    private void refreshMapMarkers() {
        Log.d(TAG, "Refresh Map Markers");
        boolean lastMarkerGone = true;
        long lastMarkerId = 0;
        if (mLastMarker != null)
            lastMarkerId = Long.parseLong(mLastMarker.getTitle());

        if (mCursor != null) {
            //clearMarkers();
//            for (int i = 0; i < MARKER_LIMIT && mCursor.moveToNext(); i++) {
            while(mCursor.moveToNext()) {
                LatLng latLng = new LatLng(
                        mCursor.getDouble(mCursor
                                .getColumnIndex(TwitterStatusContentProvider.KEY_LATITUDE)),
                        mCursor.getDouble(mCursor
                                .getColumnIndex(TwitterStatusContentProvider.KEY_LONGITUDE)));
                String title = mCursor.getString(mCursor
                        .getColumnIndex(TwitterStatusContentProvider.KEY_STATUS_ID));
                Long statusId = mCursor.getLong(mCursor.getColumnIndex(TwitterStatusContentProvider.KEY_STATUS_ID));

                if(!mMarkers.containsKey(statusId)) {
//                    Log.d(TAG, "Refresh Map Markers: Add new Marker");
                    mMarkers.put(statusId, drawMarker(latLng, title));
                }

                /*
                Preload images for infowindow
                 */
                String imageString = mCursor.getString(mCursor.getColumnIndex(TwitterStatusContentProvider.KEY_USER_IMAGE));
                Log.d(TAG, "[IMAGE PRE-LOAD] " + imageString);
                Ion.with(mActivity.getApplicationContext())
                        .load(imageString)
                        .withBitmap()
                        .asBitmap();

//                try {
//                    URL imageUrl = new URL(imageString);
//                    URI imageUri = imageUrl.toURI();
//                    String validImageString = imageUri.toString();
//                    if(validImageString != null){
////                        Log.d(TAG, validImageString);
////                        UrlImageViewHelper.loadUrlDrawable(mActivity, validImageString);
//                        Ion.with(mActivity).load(validImageString)
//                                .withBitmap()
//                                .placeholder(R.drawable.user_image_placeholder);
//                    }
//                } catch (MalformedURLException e) {
//                    Log.d(TAG, "User Image URL Malformed");
//                } catch (URISyntaxException e) {
//                    Log.d(TAG, "URI Syntax Exception");
//                }

                if (statusId == lastMarkerId) {
                    lastMarkerGone = false;
                }

            }

            if (mPendingInfoWindow != 0) {
                selectMarker(mPendingInfoWindow);
                mPendingInfoWindow = 0;
            }

            if (!lastMarkerGone && mLastMarker != null) {
                mLastMarker.showInfoWindow();
            } else {
//                mLastMarker = null;
            }
        }
    }

    private Marker drawMarker(LatLng latLng, String title) {

        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title)
                .draggable(false)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker));
//                .anchor(24, 2)

        if (mGoogleMap != null && latLng != null) {
            return mGoogleMap.addMarker(markerOptions);
        }
        return null;
    }

    GoogleMap.OnMarkerClickListener onMapMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            return clickMarker(marker);
        }
    };

    GoogleMap.OnMapClickListener onMapClickListener = new GoogleMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) {
            Log.d(TAG, "onMapClick");
            if (mLastMarker != null) {
                mLastMarker.hideInfoWindow();
                mLastMarker = null;
            }
        }
    };

    GoogleMap.OnInfoWindowClickListener onInfoWindowClickListener = new GoogleMap.OnInfoWindowClickListener() {
        @Override
        public void onInfoWindowClick(Marker marker) {
            long statusId = Long.parseLong(marker.getTitle());
            mOnMapFragmentCreatedListener.onInfoWindowClicked(statusId);
        }
    };

    private boolean clickMarker(Marker marker) {
        if (mLastMarker != null) {
            mLastMarker.hideInfoWindow();
            if (mLastMarker.equals(marker)) {
                mLastMarker = null;
                return true;
            }
        }

        marker.showInfoWindow();
        mLastMarker = marker;
        if (mCenterSelectedMarker) {
            return false;
        }
        //returning true disables default onMarkerClick behaviour which centers the marker
        return true;
    }

    private void updateFromPreferences() {
        Context context = mActivity;
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        mCenterSelectedMarker = sharedPreferences.getBoolean(PREF_MOVE_MAP, true);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(mActivity,
                TwitterStatusContentProvider.CONTENT_URI, null, null, null,
//                    mSortOrder + " limit " + mLimit);
                mSortOrder);
//                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        swapCursor(null);
    }

    private void swapCursor(Cursor cursor) {
        this.mCursor = cursor;
        refreshMapMarkers();
    }

}