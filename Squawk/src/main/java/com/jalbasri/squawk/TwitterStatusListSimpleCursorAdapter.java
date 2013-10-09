package com.jalbasri.squawk;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.TextView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

public class TwitterStatusListSimpleCursorAdapter extends SimpleCursorAdapter {
    private static final String TAG = TwitterStatusListSimpleCursorAdapter.class.getSimpleName();


    Context mContext;
    int mLayout;

    public TwitterStatusListSimpleCursorAdapter(Context context, int layout, Cursor cursor,
                                        String[] from, int[] to) {
        super(context, layout, cursor, from, to);
        this.mContext = context;
        this.mLayout = layout;

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
//        LayoutInflater inflater = LayoutInflater.from(context);
//        View view = inflater.inflate(R.layout.status_list_item, parent, false);

//        View itemView =
//        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        inflater.inflate(viewGroup, viewGroup, true);

        LinearLayout statusListItemView = new LinearLayout(mContext);
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(mLayout, statusListItemView, true);

        bindView(view, context, cursor);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String userName = getCursor().getString(getCursor().getColumnIndex(TwitterStatusContentProvider.KEY_USER_NAME));
        String statusText = getCursor().getString(getCursor().getColumnIndex(TwitterStatusContentProvider.KEY_STATUS_TEXT));
        String userImageUrl = getCursor().getString(getCursor().getColumnIndex(TwitterStatusContentProvider.KEY_USER_IMAGE));
        String screenName = getCursor().getString(getCursor().getColumnIndex(TwitterStatusContentProvider.KEY_USER_SCREEN_NAME));
        Log.d(TAG, "bindView(): " + userName);

        TextView userNameTextView = (TextView) view.findViewById(R.id.user_name);
        TextView statusTextView = (TextView) view.findViewById(R.id.status_text);
        TextView screenNameTextView = (TextView) view.findViewById(R.id.screen_name);
        ImageView userImageView = (ImageView) view.findViewById(R.id.user_image);

        userNameTextView.setText(userName);
        screenNameTextView.setText("@"+screenName);
        statusTextView.setText(statusText);
        UrlImageViewHelper.setUrlDrawable(userImageView, userImageUrl);

    }
//
//    @Override
//    public View getView(int position, View convertView, ViewGroup parent) {
//        LinearLayout statusListItemView;
//
//
//        if (getCursor().isBeforeFirst()) {
//            Log.d(TAG, "cursor before first- move to next");
//            getCursor().moveToNext();
//        }
//
//        String userName = getCursor().getString(getCursor().getColumnIndex(TwitterStatusContentProvider.KEY_USER_NAME));
//        String statusText = getCursor().getString(getCursor().getColumnIndex(TwitterStatusContentProvider.KEY_STATUS_TEXT));
//        String userImageUrl = getCursor().getString(getCursor().getColumnIndex(TwitterStatusContentProvider.KEY_USER_IMAGE));
//        String screenName = getCursor().getString(getCursor().getColumnIndex(TwitterStatusContentProvider.KEY_USER_SCREEN_NAME));
//        Log.d(TAG, "getView(): " + userName);
//
//        if (convertView == null) {
//
//            statusListItemView = new LinearLayout(mContext);
//            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            inflater.inflate(mLayout, statusListItemView, true);
//        } else {
//            statusListItemView = (LinearLayout) convertView;
//        }
//
//        TextView userNameTextView = (TextView) statusListItemView.findViewById(R.id.user_name);
//        TextView statusTextView = (TextView) statusListItemView.findViewById(R.id.status_text);
//        TextView screenNameTextView = (TextView) statusListItemView.findViewById(R.id.screen_name);
//        ImageView userImageView = (ImageView) statusListItemView.findViewById(R.id.user_image);
//
//        userNameTextView.setText(userName);
//        screenNameTextView.setText("@"+screenName);
//        statusTextView.setText(statusText);
//        UrlImageViewHelper.setUrlDrawable(userImageView, userImageUrl);
//
//        return statusListItemView;
//
//    }
}