package com.smartbus.backend.util;

import java.math.BigDecimal;

public final class GeoUtils {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private GeoUtils() {
    }

    /**
     * Haversine distance in meters between two WGS84 coordinates.
     */
    public static double distanceMeters(
            BigDecimal lat1,
            BigDecimal lon1,
            BigDecimal lat2,
            BigDecimal lon2
    ) {
        double latitude1 = Math.toRadians(lat1.doubleValue());
        double longitude1 = Math.toRadians(lon1.doubleValue());
        double latitude2 = Math.toRadians(lat2.doubleValue());
        double longitude2 = Math.toRadians(lon2.doubleValue());

        double deltaLat = latitude2 - latitude1;
        double deltaLon = longitude2 - longitude1;

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(latitude1) * Math.cos(latitude2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
