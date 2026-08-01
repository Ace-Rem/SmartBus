package com.smartbus.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GeoUtilsTest {

    @Test
    void distanceMeters_samePoint_isZero() {
        BigDecimal lat = new BigDecimal("10.762622");
        BigDecimal lon = new BigDecimal("106.660172");
        double distance = GeoUtils.distanceMeters(lat, lon, lat, lon);
        assertEquals(0.0, distance, 0.001);
    }

    @Test
    void distanceMeters_nearbyPoints_withinExpectedRange() {
        // Roughly ~111 meters per 0.001 degree latitude
        BigDecimal lat1 = new BigDecimal("10.762000");
        BigDecimal lon1 = new BigDecimal("106.660000");
        BigDecimal lat2 = new BigDecimal("10.763000");
        BigDecimal lon2 = new BigDecimal("106.660000");
        double distance = GeoUtils.distanceMeters(lat1, lon1, lat2, lon2);
        assertTrue(distance > 100 && distance < 130, "distance=" + distance);
    }
}
