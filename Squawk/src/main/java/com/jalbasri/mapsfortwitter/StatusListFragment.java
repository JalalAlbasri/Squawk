package com.jalbasri.mapsfortwitter;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.content.Loader;
import android.content.CursorLoader;
import android.database.Cursor;

//import twitter4j.Twitter;

public class StatusListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = StatusListFragment.class.getSimpleName();

    public interface OnListFragmentCreatedListener {
        public void onListFragmentCreated();
    };

    private static final int TWITTER_STATUS_LOADER = 0;
    private static final String mSortOrder = TwitterStatusContentProvider.KEY_CREATED_AT + " DESC";
    CursorAdapter mCursorAdapter;
    private OnListFragmentCreatedListener mOnListFragmentCreatedListener;
    private MainActivity mActivity;
    private int mPendingItem;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach()");
        this.mOnListFragmentCreatedListener = (OnListFragmentCreatedListener) activity;
        this.mActivity = (MainActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.status_list_listview, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(TWITTER_STATUS_LOADER, null, this);
        mOnListFragmentCreatedListener.onListFragmentCreated();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "[infowindow] onResume()");
        super.onResume();
        if (mPendingItem != 0) {
//            ListView listView = getListView();
//            View listItemView;
//            TextView statusIdTextView;
//            for (int i = 0; i < listView.getCount(); i ++) {
//                listItemView = listView.getChildAt(i);
//                statusIdTextView = (TextView) listItemView.findViewById(R.id.status_id);
//                long viewStatusId = Long.parseLong(statusIdTextView.getText().toString());
//                if (viewStatusId == mPendingItem) {
//
//                }
//
//            }

            Log.d(TAG, "[infowindow] set selection " + mPendingItem);
            setSelection(mPendingItem);
            mPendingItem = 0;
        }
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

    private void setCursorAdapter(Cursor cursor) {
        Log.d(TAG, "setCursorAdapter()");
        mCursorAdapter = new StatusListCursorAdapter(getActivity(), cursor);
        setListAdapter(mCursorAdapter);

    }

    public void selectListItem(long statusId) {
        Log.d(TAG, "[infowindow] selectListItem, statusId: " + statusId);
        if (mCursorAdapter != null) {
            Cursor cursor = mCursorAdapter.getCursor();
            if (cursor != null) {
                int initialPosition = cursor.getPosition();
                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++) {
                    if (statusId == cursor.getLong(cursor.getColumnIndex(TwitterStatusContentProvider.KEY_STATUS_ID))) {

//                View listView = getListView();
//                if (listView != null)
//                    setSelection(cursor.getPosition());
//                else
                        mPendingItem = cursor.getPosition();
                        Log.d(TAG, "[infowindow] mPendingItem" + mPendingItem);
                        break;
                    }
                    cursor.moveToNext();
                }
                cursor.move(initialPosition);
            }
        }
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
        //CursorLoader(mActivity, content uri,
        // columns to return, selection, selection args, sort order)
        return new CursorLoader(mActivity,
                TwitterStatusContentProvider.CONTENT_URI, projection, null, null, mSortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (mCursorAdapter == null) {
            Log.d(TAG, "onLoadFinished: setCursorAdapter()");
            setCursorAdapter(cursor);
        } else {
            Log.d(TAG, "onLoadFinished: swapCursor()");
            mCursorAdapter.changeCursor(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        Log.d(TAG, "onLoaderReset()");
//        mCursorAdapter.swapCursor(null);
    }

}