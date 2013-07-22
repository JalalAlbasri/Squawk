package com.jalbasri.squawk;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.content.Loader;
import android.content.CursorLoader;
import android.database.Cursor;

public class StatusListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = StatusListFragment.class.getSimpleName();

    private static final int TWITTER_STATUS_LOADER = 0;
    private static final String sortOrder = TwitterStatusContentProvider.KEY_CREATED_AT + " DESC";
    SimpleCursorAdapter cursorAdapter;
    MainActivity activity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (MainActivity) activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        int layoutId = R.layout.twitter_status_list;
        getLoaderManager().initLoader(TWITTER_STATUS_LOADER, null, this);

        String fromColumns[] = new String[] {
                TwitterStatusContentProvider.KEY_STATUS_ID,
                TwitterStatusContentProvider.KEY_STATUS_TEXT,
                TwitterStatusContentProvider.KEY_CREATED_AT,
                TwitterStatusContentProvider.KEY_USER_ID,
                TwitterStatusContentProvider.KEY_USER_NAME,
                TwitterStatusContentProvider.KEY_USER_SCREEN_NAME,
                TwitterStatusContentProvider.KEY_USER_IMAGE,
                TwitterStatusContentProvider.KEY_USER_URL,
                TwitterStatusContentProvider.KEY_LATITUDE,
                TwitterStatusContentProvider.KEY_LONGITUDE,
        };
        int toLayoutIds[] = new int[] {
                R.id.twitter_user,
                R.id.twitter_status
        };

        //TODO: Use custom simplecursoradapter
//        cursorAdapter = new TwitterStatusListSimpleCursorAdapter(
        cursorAdapter = new SimpleCursorAdapter (
                getActivity(),
                layoutId,
                null,
                fromColumns,
                toLayoutIds,
                0
        );
        setListAdapter(cursorAdapter);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        getLoaderManager().restartLoader(TWITTER_STATUS_LOADER, null, this);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onSaveInstanceState()");
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String projection[] = new String[] {
                TwitterStatusContentProvider.KEY_STATUS_ID,
                TwitterStatusContentProvider.KEY_STATUS_TEXT,
                TwitterStatusContentProvider.KEY_CREATED_AT,
                TwitterStatusContentProvider.KEY_USER_ID,
                TwitterStatusContentProvider.KEY_USER_NAME,
                TwitterStatusContentProvider.KEY_USER_SCREEN_NAME,
                TwitterStatusContentProvider.KEY_USER_IMAGE,
                TwitterStatusContentProvider.KEY_USER_URL,
                TwitterStatusContentProvider.KEY_LATITUDE,
                TwitterStatusContentProvider.KEY_LONGITUDE,
        };
        return new CursorLoader(activity,
                TwitterStatusContentProvider.CONTENT_URI, projection, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        cursorAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        cursorAdapter.changeCursor(null);
    }
}