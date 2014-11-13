package com.jalbasri.mapsfortwitter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.bitmap.Transform;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import twitter4j.Twitter;

/**
 * Created by jalal on 9/10/13.
 */
public class StatusListCursorAdapter extends CursorAdapter {
    private static final String TAG = StatusListCursorAdapter.class.getSimpleName();
    private final long ONE_MINUTE = 60*1000;
    private final long ONE_HOUR = 60*ONE_MINUTE;
    private final long ONE_DAY = 24*ONE_HOUR;
    private Context mContext;

    public StatusListCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor);
        mContext = context;
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

        /*
        Collect tweet data from the cursor
         */
        final Long statusId = getCursor().getLong(getCursor().getColumnIndex(TwitterStatusContentProvider.KEY_STATUS_ID));
        Log.d(TAG, "bindView(), Status ID: " + statusId);
        String userName = getCursor().getString(getCursor().getColumnIndex(TwitterStatusContentProvider.KEY_USER_NAME));
        String statusText = getCursor().getString(getCursor().getColumnIndex(TwitterStatusContentProvider.KEY_STATUS_TEXT));
        String userImageUrl = getCursor().getString(getCursor().getColumnIndex(TwitterStatusContentProvider.KEY_USER_IMAGE));
        String screenName = getCursor().getString(getCursor().getColumnIndex(TwitterStatusContentProvider.KEY_USER_SCREEN_NAME));
        Long createdAtLong = getCursor().getLong(getCursor().getColumnIndex(TwitterStatusContentProvider.KEY_CREATED_AT));
        String createdAtString = formatCreatedAt(createdAtLong);
        Log.d(TAG, "bindView(): " + userName);

        /*
        Get references to UI elements
         */
        final RelativeLayout itemRelativeLayout = (RelativeLayout) view.findViewById(R.id.tweet_item);
        final TextView userNameTextView = (TextView) view.findViewById(R.id.user_name);
        final TextView statusTextView = (TextView) view.findViewById(R.id.status_text);
        final TextView screenNameTextView = (TextView) view.findViewById(R.id.screen_name);
        ImageView userImageView = (ImageView) view.findViewById(R.id.user_image);
        final TextView createdAtTextView = (TextView) view.findViewById(R.id.created_at);
        TextView statusIdTextView = (TextView) view.findViewById(R.id.status_id);
        final ImageButton replyImageButton = (ImageButton) view.findViewById(R.id.reply);
        final ImageButton retweetImageButton = (ImageButton) view.findViewById(R.id.retweet);
        final ImageButton favoriteImageButton = (ImageButton) view.findViewById(R.id.favorite);

        /*
        Update Views with tweet data
         */
        statusIdTextView.setText(statusId.toString());
        userNameTextView.setText(userName);
        screenNameTextView.setText("@"+screenName);
        statusTextView.setText(statusText);
//        UrlImageViewHelper.setUrlDrawable(userImageView, userImageUrl, R.drawable.user_image_placeholder);
//        Ion.with(userImageView)
//                .placeholder(R.drawable.user_image_placeholder)
//                .load(userImageUrl);

        Ion.with(mContext)
                .load(userImageUrl)
                .withBitmap()
                .placeholder(R.drawable.user_image_placeholder)
                .intoImageView(userImageView);

        createdAtTextView.setText(createdAtString);


        /*
        Linkify status text urls, @mentions and #hastags.
         */
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

        final Uri userUri = Uri.parse("http://www.twitter.com/" + screenName);

        View.OnClickListener userOnCickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContext.startActivity(new Intent(Intent.ACTION_VIEW, userUri));
            }
        };

        userNameTextView.setOnClickListener(userOnCickListener);
        screenNameTextView.setOnClickListener(userOnCickListener);
        userImageView.setOnClickListener(userOnCickListener);

        Pattern mentionPattern = Pattern.compile("@([A-Za-z0-9_-]+)");
        String mentionScheme = "http://www.twitter.com/";
        Linkify.addLinks(statusTextView, mentionPattern, mentionScheme, null, filter);

        Pattern hashtagPattern = Pattern.compile("#([A-Za-z0-9_-]+)");
        String hashtagScheme = "http://www.twitter.com/search/";
        Linkify.addLinks(statusTextView, hashtagPattern, hashtagScheme, null, hashtagFilter);

        Pattern urlPattern = Patterns.WEB_URL;
        Linkify.addLinks(statusTextView, urlPattern, null, null, filter);

        /*
        Define button listeners
         */


        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                statusTextView.setTextColor(Color.WHITE);
                screenNameTextView.setTextColor(Color.WHITE);
                userNameTextView.setTextColor(Color.WHITE);
                createdAtTextView.setTextColor(Color.WHITE);
                itemRelativeLayout.setBackgroundResource(R.drawable.card_background_pressed);
                replyImageButton.setBackgroundResource(R.drawable.button_background_pressed);
                retweetImageButton.setBackgroundResource(R.drawable.button_background_pressed);
                favoriteImageButton.setBackgroundResource(R.drawable.button_background_pressed);
                replyImageButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_reply_dark));
                retweetImageButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_repeat_dark));
                favoriteImageButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_important_dark));
                statusTextView.setLinkTextColor(Color.WHITE);

                replyImageButton.setPadding(3, 3, 3, 3);
                retweetImageButton.setPadding(3, 3, 3, 3);
                favoriteImageButton.setPadding(5, 5, 5, 5);
            }
        };

        itemRelativeLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    handler.postDelayed(runnable, 100);

                }
                if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_CANCEL
                        ) {
                    handler.removeCallbacks(runnable);
                    statusTextView.setTextColor(Color.BLACK);
                    screenNameTextView.setTextColor(mContext.getResources().getColor(android.R.color.darker_gray));
                    userNameTextView.setTextColor(Color.BLACK);
                    createdAtTextView.setTextColor(mContext.getResources().getColor(android.R.color.darker_gray));

                    itemRelativeLayout.setBackgroundResource(R.drawable.card_background);
                    replyImageButton.setBackgroundResource(R.drawable.button_background);
                    retweetImageButton.setBackgroundResource(R.drawable.button_background);
                    favoriteImageButton.setBackgroundResource(R.drawable.button_background);
                    replyImageButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_reply));
                    retweetImageButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_repeat));
                    favoriteImageButton.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_important));
                    statusTextView.setLinkTextColor(userNameTextView.getLinkTextColors().getDefaultColor());

                    replyImageButton.setPadding(3, 3, 3, 3);
                    retweetImageButton.setPadding(3, 3, 3, 3);
                    favoriteImageButton.setPadding(5, 5, 5, 5);
                }
                return false;
            }
        });

        replyImageButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    replyImageButton.setBackgroundResource(R.drawable.button_background_pressed);
                    replyImageButton.setPadding(3, 3, 3, 3);
                }
                if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_CANCEL
                        ) {
                    replyImageButton.setBackgroundResource(R.drawable.button_background);
                    replyImageButton.setPadding(3, 3, 3, 3);
                }
                return false;
            }
        });

        replyImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, WebViewActivity.class);
                intent.putExtra("action", "Reply");
                intent.putExtra("url", "https://www.twitter.com/intent/tweet?in_reply_to="  + statusId);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                mContext.startActivity(intent);
            }
        });

        retweetImageButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    retweetImageButton.setBackgroundResource(R.drawable.button_background_pressed);
                    retweetImageButton.setPadding(3, 3, 3, 3);
                }
                if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_CANCEL
                        ) {
                    retweetImageButton.setBackgroundResource(R.drawable.button_background);
                    retweetImageButton.setPadding(3, 3, 3, 3);
                }
                return false;
            }
        });

        retweetImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, WebViewActivity.class);
                intent.putExtra("action", "Retweet");
                intent.putExtra("url", "https://www.twitter.com/intent/retweet?tweet_id=" + statusId);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                mContext.startActivity(intent);
            }
        });

        favoriteImageButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    favoriteImageButton.setBackgroundResource(R.drawable.button_background_pressed);
                    favoriteImageButton.setPadding(5, 5, 5, 5);
                }
                if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_CANCEL
                        ) {
                    favoriteImageButton.setBackgroundResource(R.drawable.button_background);
                    favoriteImageButton.setPadding(5, 5, 5, 5);
                }
                return false;
            }
        });

        favoriteImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, WebViewActivity.class);
                intent.putExtra("action", "Favorite");
                intent.putExtra("url", "https://www.twitter.com/intent/favorite?tweet_id=" + statusId);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                mContext.startActivity(intent);

            }
        });

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
