package com.jalbasri.squawk;

/**
 * Created by jalal on 23/08/13.
 */
public class MapRegion {


    private double southWestLongitude;
    private double southWestLatitude;
    private double northEastLongitude;
    private double northEastLatitude;

    public void MapRegion(double southWestLongitude, double southWestLatitude,
                             double northEastLongitude, double northEastLatitude) {
        this.southWestLongitude = southWestLongitude;
        this.southWestLatitude = southWestLatitude;
        this.northEastLongitude = northEastLongitude;
        this.northEastLatitude = northEastLatitude;
    }

    /**
     * Checks if the location passed in is within the map region for this device
     * @param lat The latitude of the location to check
     * @param lng The longitude of the location to check
     * @return true if the location is within the device's map region
     *
     */
    public boolean isInMapRegion(double lat, double lng) {
        return ((lng >= southWestLongitude && lng <= northEastLongitude) &&
                (lat >= southWestLatitude && lat <= northEastLatitude));
    }

    public double getSouthWestLongitude() {
        return southWestLongitude;
    }

    public double getSouthWestLatitude() {
        return southWestLatitude;
    }

    public double getNorthEastLongitude() {
        return northEastLongitude;
    }

    public double getNorthEastLatitude() {
        return northEastLatitude;
    }

    public void setSouthWestLongitude(double southWestLongitude) {
        this.southWestLongitude = southWestLongitude;
    }

    public void setSouthWestLatitude(double southWestLatitude) {
        this.southWestLatitude = southWestLatitude;
    }

    public void setNorthEastLongitude(double northEastLongitude) {
        this.northEastLongitude = northEastLongitude;
    }

    public void setNorthEastLatitude(double northEastLatitude) {
        this.northEastLatitude = northEastLatitude;
    }

}
