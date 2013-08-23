package com.jalbasri.squawk;

import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;
import com.jalbasri.squawk.deviceinfoendpoint.Deviceinfoendpoint;
import com.jalbasri.squawk.deviceinfoendpoint.model.DeviceInfo;

import java.io.IOException;

/**
 * AsyncTask connected to deviceInfoEndpoint and updates the device Info.
 *
 * Called to initially set the device as online, take the device offline and change the registered mapRegion.
 *
 */
public class UpdateDeviceInfoAsyncTask extends AsyncTask<DeviceInfo, Void, DeviceInfo> {
    private static final String TAG = UpdateDeviceInfoAsyncTask.class.getSimpleName();

    private MainActivity activity;
    private Deviceinfoendpoint deviceinfoendpoint;

    public UpdateDeviceInfoAsyncTask(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    protected void onPreExecute() {
        //Set up the deviceinfoendpoint
        super.onPreExecute();

        Deviceinfoendpoint.Builder deviceinfoendpointBuilder = new Deviceinfoendpoint.Builder(
                AndroidHttp.newCompatibleTransport(), new JacksonFactory(),
                new HttpRequestInitializer() {
                    public void initialize(HttpRequest httpRequest) {
                    }
                });
        deviceinfoendpoint = CloudEndpointUtils.updateBuilder(deviceinfoendpointBuilder).build();

    }

    @Override
    protected DeviceInfo doInBackground(DeviceInfo... params) {
        Log.d(TAG, "doInBackground...");
        DeviceInfo deviceInfo = params[0];
        DeviceInfo resultDeviceInfo = null;
        if (deviceInfo != null) {
            try {
                resultDeviceInfo = deviceinfoendpoint.updateDeviceInfo(deviceInfo).execute();
            } catch (IOException e) {
                Log.e(TAG, "Error occured updating device info: " + e);
            }
        }
        return resultDeviceInfo;
    }

    @Override
    protected void onPostExecute(DeviceInfo result) {
        if (result != null) {
            Log.d(TAG, "Update Device Info Successful");
        }
    }

}
