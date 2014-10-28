package com.jalbasri.squawk.amazon;

/**
 * Class enables communication with Amazon Web Server.
 */
public class Amazon {
    private static final String TAG = Amazon.class.getSimpleName();


//    public final static String AMAZON_HOST = "http://ec2-user@ec2-54-187-108-184.us-west-2.compute.amazonaws.com";
    public final static String AMAZON_HOST = "http://secure-ravine-8788.herokuapp.com";
    public final static String ADD_PATH = "/addDevice";
    public final static String REMOVE_PATH = "/removeDevice";

    public Amazon() {

    }

    /**
     * Add device to Amazon Server
     */
    public void addDevice(String deviceId, double[][] mapRegion) {
        if (mapRegion != null && deviceId != null) {
            new AddDeviceAsyncTask().execute(
                    deviceId,
                    Double.toString(mapRegion[0][0]),
                    Double.toString(mapRegion[0][1]),
                    Double.toString(mapRegion[1][0]),
                    Double.toString(mapRegion[1][1]));
        }
     }

    public void removeDevice(String deviceId) {
        if (deviceId != null) {
            new RemoveDeviceAsyncTask().execute(deviceId);
        }
    }



}
