package com.jalbasri.squawk;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

/**
 * Created by jalal on 9/10/13.
 */
public class StatusListCursorAdapter extends CursorAdapter {
    private static final String TAG = StatusListCursorAdapter.class.getSimpleName();

    public StatusListCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LinearLayout statusListItemView = new LinearLayout(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.status_list_item, statusListItemView, true);

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

}
