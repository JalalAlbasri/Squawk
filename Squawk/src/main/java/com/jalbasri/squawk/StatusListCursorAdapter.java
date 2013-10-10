package com.jalbasri.squawk;

import android.content.Context;
import android.database.Cursor;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jalal on 9/10/13.
 */
public class StatusListCursorAdapter extends CursorAdapter {
    private static final String TAG = StatusListCursorAdapter.class.getSimpleName();
    private final long ONE_MINUTE = 60*1000;
    private final long ONE_HOUR = 60*ONE_MINUTE;
    private final long ONE_DAY = 24*ONE_HOUR;


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
        Long createdAtLong = getCursor().getLong(getCursor().getColumnIndex(TwitterStatusContentProvider.KEY_CREATED_AT));
        String createdAtString = formatCreatedAt(createdAtLong);
        Log.d(TAG, "bindView(): " + userName);

        TextView userNameTextView = (TextView) view.findViewById(R.id.user_name);
        TextView statusTextView = (TextView) view.findViewById(R.id.status_text);
        TextView screenNameTextView = (TextView) view.findViewById(R.id.screen_name);
        ImageView userImageView = (ImageView) view.findViewById(R.id.user_image);
        TextView createdAtTextView = (TextView) view.findViewById(R.id.created_at);

        Linkify.TransformFilter filter = new Linkify.TransformFilter() {
            public final String transformUrl(final Matcher match, String url) {
                return match.group();
            }
        };

        Linkify.TransformFilter hashtagFilter = new Linkify.TransformFilter() {
            @Override
            public String transformUrl(Matcher matcher, String s) {
                Log.d(TAG, "Transform hashtag before: " + s);
                s = matcher.group();
                s = s.replace("#", "%23");
                Log.d(TAG, "Transform hashtag after: " + s);
                return s;
            }
        };

        userNameTextView.setText(userName);
        screenNameTextView.setText("@"+screenName);
        statusTextView.setText(statusText);
        UrlImageViewHelper.setUrlDrawable(userImageView, userImageUrl);
        createdAtTextView.setText(createdAtString);

        Pattern mentionPattern = Pattern.compile("@([A-Za-z0-9_-]+)");
        String mentionScheme = "http://www.twitter.com/";
        Linkify.addLinks(statusTextView, mentionPattern, mentionScheme, null, filter);

        Pattern hashtagPattern = Pattern.compile("#([A-Za-z0-9_-]+)");
        String hashtagScheme = "http://www.twitter.com/search/";
        Linkify.addLinks(statusTextView, hashtagPattern, hashtagScheme, null, hashtagFilter);

        Pattern urlPattern = Patterns.WEB_URL;
        Linkify.addLinks(statusTextView, urlPattern, null, null, filter);

    }

    private String formatCreatedAt(Long createdAt) {
        long since = System.currentTimeMillis() - createdAt;
        if (since < ONE_MINUTE) {
            return "Just now";
        }
        else if (since < ONE_HOUR) {
            return (int) since/ONE_MINUTE + "m";
        } else if (since > ONE_HOUR && since < ONE_DAY) {
            return (int) since/ONE_HOUR +"h";
        } else {
            return (int) since/ONE_DAY + "d";
        }
    }

}
