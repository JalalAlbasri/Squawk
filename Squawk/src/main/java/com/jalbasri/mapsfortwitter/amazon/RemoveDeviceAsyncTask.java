package com.jalbasri.mapsfortwitter.amazon;

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
 * Created by jalal on 16/09/13.
 */
public class RemoveDeviceAsyncTask extends AsyncTask<String, Void, HttpResponse> {

    private static final String TAG = RemoveDeviceAsyncTask.class.getSimpleName();

    public RemoveDeviceAsyncTask() {

    }

    @Override
    protected void onPreExecute() {
        Log.d(TAG, "Starting Remove to Amazon Server...");
    }

    @Override
    protected HttpResponse doInBackground(String... deviceInfo) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(Amazon.AMAZON_HOST + Amazon.REMOVE_PATH);
        HttpResponse response = null;
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("DeviceId", deviceInfo[0]));
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
                Log.d(TAG, "Device successfully removed from Amazon server.");
            } else {
                Log.e(TAG, "Remove device to Amazon server failed. Status Code: "
                        + statusCode);
            }
        }

    }
}
