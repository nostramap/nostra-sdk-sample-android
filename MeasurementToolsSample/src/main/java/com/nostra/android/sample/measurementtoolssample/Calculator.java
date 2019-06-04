package com.nostra.android.sample.measurementtoolssample;

public class Calculator {

    static double calDistance(double lat1,double lon1,double lat2,double lon2) {
        double r = 6371000.0;
        double d2r = Math.PI / 180.0;
        double rLat1 = lat1 * d2r;
        double rLat2 = lat2 * d2r;
        double dLat = (lat2 - lat1) * d2r;
        double dLon = (lon2 - lon1) * d2r;
        double a = (Math.sin(dLat / 2) * Math.sin(dLat / 2)) +
                (Math.cos(rLat1) * Math.cos(rLat2) * (Math.sin(dLon / 2) *
                        Math.sin(dLon / 2)));
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

}