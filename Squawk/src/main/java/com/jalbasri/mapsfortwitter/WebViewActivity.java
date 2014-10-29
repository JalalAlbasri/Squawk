package com.jalbasri.mapsfortwitter;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

/**
 * Display and reply to tweets in web view.
 */
public class WebViewActivity extends Activity {
    private static final String TAG = WebViewActivity.class.getSimpleName();

    private WebView mWebView;
    private Activity activity;

    @Override
    public void onCreate(Bundle bundle) {
        Log.d(TAG, "WebView onCreate()");
        super.onCreate(bundle);
        Intent intent = getIntent();
        String action = intent.getStringExtra("action");
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(action);
        actionBar.setDisplayHomeAsUpEnabled(true);
        activity = this;
        setContentView(R.layout.web_view);
        mWebView = (WebView) findViewById(R.id.web_view);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient());
        String url = intent.getStringExtra("url");
        Log.d(TAG,"onNewIntent() url: " + url);
        mWebView.loadUrl(url);

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.web_view_progress_bar);
        progressBar.animate().withLayer().yBy(-6.0f*getResources().getDisplayMetrics().density).setDuration(0).start();

        mWebView.setWebChromeClient(new WebChromeClient(){
            public void onProgressChanged(WebView view, int progress)
            {
                if(progress < 100 && progressBar.getVisibility() == ProgressBar.GONE){
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                }
                progressBar.setProgress(progress);
                if(progress == 100) {
                    progressBar.setProgress(progress);
                    progressBar.setVisibility(ProgressBar.GONE);
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }

}