package com.jalbasri.squawk;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

//TODO set a default map zoom
//TODO move the camera to last location when switching tabs and changing orientation
//TODO add map marker limit.

/*
    Warning, com.google.android.gms.maps.model.LatLng is used
    instead of android.location.Location for map functions.
 */
public class StatusMapFragment extends MapFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = StatusMapFragment.class.getName();

    private OnMapFragmentCreatedListener mOnMapFragmentCreatedListener;
    private static final int TWITTER_STATUS_LOADER = 0;
    private MainActivity mActivity;
    private Cursor mCursor;
    private GoogleMap mGoogleMap;
    private Marker mLastMarker;
    private String mSortOrder = TwitterStatusContentProvider.KEY_CREATED_AT + " DESC";
    private final int MARKER_LIMIT = 10;

    public interface OnMapFragmentCreatedListener {
        public void onMapFragmentCreated();
        public void onMapRegionChanged();
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
        Log.d(TAG, "initGoogleMap finished " + (mGoogleMap != null));

    }

    public void moveMaptoLocation(LatLng latLng, float zoom) {
        Log.d(TAG, "move map to location " + (mGoogleMap != null));
        if (mGoogleMap != null) {
            CameraPosition cameraPosition = new CameraPosition(latLng, zoom, 0, 0);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
            mGoogleMap.moveCamera(cameraUpdate);

        }

    }

    public void moveMaptoLocation(LatLng latLng) {
        Log.d(TAG, "move map to location " + (mGoogleMap != null));
        if (mGoogleMap != null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        }

    }

    public void clearMarkers() {
        if (mGoogleMap != null) {
            mGoogleMap.clear();
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

    private void refreshMapMarkers() {
        if (mCursor != null) {
            clearMarkers();
            for (int i = 0; i < MARKER_LIMIT && mCursor.moveToNext(); i++) {
                LatLng latLng = new LatLng(
                        mCursor.getDouble(mCursor
                                .getColumnIndex(TwitterStatusContentProvider.KEY_LATITUDE)),
                        mCursor.getDouble(mCursor
                                .getColumnIndex(TwitterStatusContentProvider.KEY_LONGITUDE)));
                String title = mCursor.getString(mCursor
                        .getColumnIndex(TwitterStatusContentProvider.KEY_USER_SCREEN_NAME));
                String snippet = mCursor
                        .getString(mCursor.getColumnIndex(TwitterStatusContentProvider.KEY_STATUS_TEXT));
                drawMarker(latLng, title, snippet);

            }
            if(mLastMarker != null) {
                mLastMarker.showInfoWindow();
            }
        }

//        if (cursor != null && cursor.moveToNext()) {
//            int i = 0;
//            do {
//                i++;
//                LatLng latLng = new LatLng(
//                        cursor.getDouble(cursor
//                                .getColumnIndex(TwitterStatusContentProvider.KEY_LATITUDE)),
//                        cursor.getDouble(cursor
//                                .getColumnIndex(TwitterStatusContentProvider.KEY_LONGITUDE)));
//                String title = cursor.getString(cursor
//                        .getColumnIndex(TwitterStatusContentProvider.KEY_USER_SCREEN_NAME));
//                String snippet = cursor
//                        .getString(cursor.getColumnIndex(TwitterStatusContentProvider.KEY_STATUS_TEXT));
//                drawMarker(latLng, title, snippet);
//                cursor.moveToNext();
//            } while (i < MARKER_LIMIT && cursor.moveToNext());
//        }
    }

    private void drawMarker(LatLng latLng, String title, String snippet) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title)
                .draggable(false)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.tweet_light))
//                .anchor(24, 2)
                .snippet(snippet);

        if (mGoogleMap != null && latLng != null) {
            mGoogleMap.addMarker(markerOptions);
        }
    }

    GoogleMap.OnMarkerClickListener onMapMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            if (mLastMarker != null) {
                mLastMarker.hideInfoWindow();
                if (mLastMarker.equals(marker)) {
                    mLastMarker = null;
                    return true;
                }
            }

            marker.showInfoWindow();
            mLastMarker = marker;

            return true;
        }
    };

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