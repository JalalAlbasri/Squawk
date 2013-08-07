package com.jalbasri.squawk;

import com.google.api.server.spi.response.CollectionResponse;
import twitter4j.Status;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * An entity for Android device information.
 * <p/>
 * Its associated endpoint, DeviceInfoEndpoint.java, was directly generated from
 * this class - the Google Plugin for Eclipse allows you to generate endpoints
 * directly from entities!
 * <p/>
 * DeviceInfoEndpoint.java will be used for registering devices with this App
 * Engine application. Registered devices will receive messages broadcast by
 * this application over Google Cloud Messaging (GCM). If you'd like to take a
 * look at the broadcasting code, check out MessageEndpoint.java.
 * <p/>
 * For more information, see
 * http://developers.google.com/eclipse/docs/cloud_endpoints.
 * <p/>
 * NOTE: This DeviceInfoEndpoint.java does not use any form of authorization or
 * authentication! If this app is deployed, anyone can access this endpoint! If
 * you'd like to add authentication, take a look at the documentation.
 */
@Entity
// DeviceInfoEndpoint has NO AUTHENTICATION - it is an OPEN ENDPOINT!
public class DeviceInfo {

    /**
     * Flag to indicate if this device is online and will collect Tweets
     *
     */
    private boolean online;

    /**
     * The map region this device is collecting tweets in
     *
     */
    private double[][] mapRegion;

    /**
     * Sets the online flag as true
     *
     */
    public void takeDeviceOnline() {
        this.online = true;
    }

    /**
     * Sets the online flag as false
     *
     */
    public void takeDeviceOffline() {
        this.online = false;
    }

    /**
     * Checks if the location passed in is within the map region for this device
     * @param lat The latitude of the location to check
     * @param lng The longitude of the location to check
     * @return true if the location is within the device's map region
     *
     */
    public boolean isInMapRegion(double lat, double lng) {
        return ((lng >= mapRegion[0][0] && lng <= mapRegion[1][0]) &&
                (lat >= mapRegion[0][1] && lat <= mapRegion[1][1]));
    }

    /**
     * @return the online status of the deivce
     */
    public boolean isOnline() {
        return this.online;
    }

    /*
     * The Google Cloud Messaging registration token for the device. This token
     * indicates that the device is able to receive messages sent via GCM.
     */
    @Id
    private String deviceRegistrationID;

    /*
     * Some identifying information about the device, such as its manufacturer
     * and product name.
     */
    private String deviceInformation;

    /*
     * Timestamp indicating when this device registered with the application.
     */
    private long timestamp;

    public String getDeviceRegistrationID() {
        return deviceRegistrationID;
    }

    public String getDeviceInformation() {
        return this.deviceInformation;
    }

    public void setDeviceRegistrationID(String deviceRegistrationID) {
        this.deviceRegistrationID = deviceRegistrationID;
    }

    public void setDeviceInformation(String deviceInformation) {
        this.deviceInformation = deviceInformation;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
