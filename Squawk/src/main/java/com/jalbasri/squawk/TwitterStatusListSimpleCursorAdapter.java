package com.jalbasri.squawk;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.TextView;

public class TwitterStatusListSimpleCursorAdapter extends SimpleCursorAdapter {

    Context context;
    int layout;

    public TwitterStatusListSimpleCursorAdapter(Context context, int layout, Cursor c,
                                        String[] from, int[] to) {
        super(context, layout, c, from, to);
        this.context = context;
        this.layout = layout;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
   /*     LinearLayout twitterStatusItemView;

        Cursor cursor = getCursor();

        String twitterUser = cursor.getString(cursor.getColumnIndex(TwitterStatusProvider.KEY_USER));
        String twitterStatus = cursor.getString(cursor.getColumnIndex(TwitterStatusProvider.KEY_STATUS));

        if (convertView == null) {
            twitterStatusItemView = new LinearLayout(context);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(layout, twitterStatusItemView, true);
        } else {
            twitterStatusItemView = (LinearLayout) convertView;
        }

        TextView twitterUserTextView = (TextView) twitterStatusItemView.findViewById(R.id.twitter_user);
        TextView twitterStatusTextView = (TextView) twitterStatusItemView.findViewById(R.id.twitter_status);

        twitterUserTextView.setText(twitterUser);
        twitterStatusTextView.setText(twitterStatus);

        return twitterStatusItemView;
    */
        return null;
    }
}
