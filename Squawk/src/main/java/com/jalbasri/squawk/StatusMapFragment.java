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
import android.widget.SimpleCursorAdapter;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

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
    }

    public void moveMaptoLocation(LatLng latLng) {
        if (mGoogleMap != null)
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        this.activity = (MainActivity) activity;
        try {
            mOnMapFragmentCreatedListener = (OnMapFragmentCreatedListener) activity;
        } catch (ClassCastException e){
            Log.d(TAG, e.getMessage());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceBundle) {
        super.onActivityCreated(savedInstanceBundle);
        getLoaderManager().initLoader(TWITTER_STATUS_LOADER, null, this);
        initGoogleMap();
        mOnMapFragmentCreatedListener.onMapFragmentCreated();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleMap == null) {
            initGoogleMap();
        }
        //TODO Set Zoon Level Here.
    }

    private void initGoogleMap() {
        mGoogleMap = getMap();
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
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

    public void clearMarkers() {
        if (mGoogleMap != null) {
            mGoogleMap.clear();
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