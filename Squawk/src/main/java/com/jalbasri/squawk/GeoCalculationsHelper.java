package com.jalbasri.squawk;

import android.location.Location;

import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GlobalCoordinates;

public class GeoCalculationsHelper {

    private static double mNorthEastBearing = 45;
    private static double mSouthWestBearing = 225;

    //Calculates a mapregion for twitter streamig api
    //double {{swlong, sqlat},{nelong, nelat}}
    public static double[][] getMapRegion(Location location, int radius) {
        int radiusMeters = radius*1000;
        GlobalCoordinates start =
                new GlobalCoordinates(location.getLatitude(), location.getLongitude());
        GeodeticCalculator geoCalc = new GeodeticCalculator();
        Ellipsoid reference = Ellipsoid.WGS84;
        GlobalCoordinates southWest = geoCalc.calculateEndingGlobalCoordinates(
                reference,
                start,
                mSouthWestBearing,
                radiusMeters);
        GlobalCoordinates northEast = geoCalc.calculateEndingGlobalCoordinates(
                reference,
                start,
                mNorthEastBearing,
                radiusMeters);
        double[][] mapRegion = {{southWest.getLongitude(), southWest.getLatitude()},
                {northEast.getLongitude(), northEast.getLatitude()}};
        return mapRegion;
    }

}
