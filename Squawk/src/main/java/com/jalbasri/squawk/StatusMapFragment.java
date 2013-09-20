package com.jalbasri.squawk;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import java.util.ArrayList;

//TODO set a default map zoom
//TODO move the camera to last location when switching tabs and changing orientation


/*
    Warning, com.google.android.gms.maps.model.LatLng is used
    instead of android.location.Location for map functions.
 */
public class StatusMapFragment extends MapFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = StatusMapFragment.class.getName();

    private OnMapFragmentCreatedListener mOnMapFragmentCreatedListener;
    private static final int TWITTER_STATUS_LOADER = 0;
    private MainActivity activity;
    private Cursor cursor;
    private GoogleMap mGoogleMap;

    public interface OnMapFragmentCreatedListener {
        public void onMapFragmentCreated();
        public void onMapRegionChanged();
    }

    @Override
    public void onAttach(Activity activity){
        Log.d(TAG, "map onAttach()");
        super.onAttach(activity);
        this.activity = (MainActivity) activity;
        try {
            mOnMapFragmentCreatedListener = (OnMapFragmentCreatedListener) activity;
        } catch (ClassCastException e){
            Log.d(TAG, e.getMessage());
        }
        getLoaderManager().initLoader(TWITTER_STATUS_LOADER, null, this);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceBundle) {
        Log.d(TAG, "map onActivityCreated() " + (mGoogleMap != null));
        super.onActivityCreated(savedInstanceBundle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        View v = super.onCreateView(inflater, container, savedInstanceState);
        initGoogleMap();
        mOnMapFragmentCreatedListener.onMapFragmentCreated();
        return v;
    }

    @Override
    public void onResume() {
        Log.d(TAG, "map onResume()");
        super.onResume();
        if (mGoogleMap == null) {
            initGoogleMap();
        }
        //TODO Set Zoom Level Here.
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
        Log.d(TAG, "initGoogleMap finished " + (mGoogleMap != null));
    }

    public void moveMaptoLocation(LatLng latLng) {
        Log.d(TAG, "move map to location " + (mGoogleMap != null));
        if (mGoogleMap != null)
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
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

    private void refreshMapMarkers() {
//        Log.d(TAG, "cursor size: " + cursor.getCount());
        if (cursor != null && cursor.moveToNext()) {
            do {
                LatLng latLng = new LatLng(
                        cursor.getDouble(cursor
                                .getColumnIndex(TwitterStatusContentProvider.KEY_LATITUDE)),
                        cursor.getDouble(cursor
                                .getColumnIndex(TwitterStatusContentProvider.KEY_LONGITUDE)));
                String title = cursor.getString(cursor
                        .getColumnIndex(TwitterStatusContentProvider.KEY_USER_SCREEN_NAME));
                String snippet = cursor
                        .getString(cursor.getColumnIndex(TwitterStatusContentProvider.KEY_STATUS_TEXT));
                drawMarker(latLng, title, snippet);
                cursor.moveToNext();
            } while (cursor.moveToNext());
        }
    }

    private void drawMarker(LatLng latLng, String title, String snippet) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title)
                .draggable(false)
//        .icon(BitmapDescriptorFactory.fromResource(R.drawable.twitter_bird))
                .snippet(snippet);
        if (mGoogleMap != null && latLng != null) {
            mGoogleMap.addMarker(markerOptions);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(activity,
                TwitterStatusContentProvider.CONTENT_URI, null, null, null, null);
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
        this.cursor = cursor;
        refreshMapMarkers();
    }

}