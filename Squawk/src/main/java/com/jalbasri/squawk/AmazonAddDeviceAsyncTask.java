package com.jalbasri.squawk;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Makes a POST Http Request to the Amazon server to add a new device Id and Map Region.
 */
public class AmazonAddDeviceAsyncTask  extends AsyncTask<String, Void, HttpResponse> {

    private static final String TAG = AmazonAddDeviceAsyncTask.class.getSimpleName();

    private MainActivity activity;
    private String mHost = "http://ec2-user@ec2-54-200-2-207.us-west-2.compute.amazonaws.com";

    public AmazonAddDeviceAsyncTask(Activity activity) {
        this.activity = (MainActivity) activity;
    }

    @Override
    protected void onPreExecute() {
        Log.d(TAG, "Starting Add to Amazon Server...");
    }

    @Override
    protected HttpResponse doInBackground(String... deviceInfo) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(mHost + deviceInfo[0]);
        HttpResponse response = null;
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
            nameValuePairs.add(new BasicNameValuePair("DeviceId", deviceInfo[1]));
            nameValuePairs.add(new BasicNameValuePair("swLat", deviceInfo[2]));
            nameValuePairs.add(new BasicNameValuePair("swLong", deviceInfo[3]));
            nameValuePairs.add(new BasicNameValuePair("neLat", deviceInfo[4]));
            nameValuePairs.add(new BasicNameValuePair("neLong", deviceInfo[5]));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            //Execute Http Post Request
            response = httpClient.execute(httpPost);

        } catch (ClientProtocolException e) {
            Log.e(TAG, "ClientProtocolException executing http request. " + e);
        } catch (IOException e) {
            Log.e(TAG, "IOException executing http request." + e);
        } catch (Exception e) {
            Log.e(TAG, "Unidentified Exception executing Http Post." + e);
        }
        return response;

    }

    @Override
    protected void onPostExecute(HttpResponse response) {
        if (response != null){
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                Log.d(TAG, "Device successfully added to Amazon server.");
            } else {
                Log.e(TAG, "Add device to Amazon server failed. Status Code: "
                        + statusCode);
            }
        }

    }
}