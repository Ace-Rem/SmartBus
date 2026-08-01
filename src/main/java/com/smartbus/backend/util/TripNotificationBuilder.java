package com.smartbus.backend.util;

import com.smartbus.backend.entity.Stop;

/**
 * Builds driver-facing GPS notifications. Business logic only; not persisted.
 */
public final class TripNotificationBuilder {

    private TripNotificationBuilder() {
    }

    public static String build(
            boolean withinThreshold,
            Stop currentStop,
            Stop nextStop,
            Stop nearestStop,
            Double nearestDistanceMeters,
            int passengersAlightingAtCurrentStop,
            int passengersAlightingAtNextStop
    ) {
        if (withinThreshold && currentStop != null) {
            if (passengersAlightingAtCurrentStop > 0) {
                return "Đã đến bến " + currentStop.getName()
                        + " — " + passengersAlightingAtCurrentStop + " khách xuống";
            }
            return "Đã đến bến " + currentStop.getName();
        }

        if (nextStop != null) {
            String distancePart = "";
            if (nearestStop != null
                    && nearestStop.getId() != null
                    && nearestStop.getId().equals(nextStop.getId())
                    && nearestDistanceMeters != null) {
                distancePart = " (còn " + Math.round(nearestDistanceMeters) + "m)";
            }
            if (passengersAlightingAtNextStop > 0) {
                return "Bến tiếp theo: " + nextStop.getName()
                        + distancePart
                        + " — " + passengersAlightingAtNextStop + " khách sẽ xuống";
            }
            return "Bến tiếp theo: " + nextStop.getName() + distancePart;
        }

        if (currentStop != null) {
            return "Đang tại bến cuối: " + currentStop.getName();
        }

        if (nearestStop != null && nearestDistanceMeters != null) {
            return "Đang di chuyển — bến gần nhất: " + nearestStop.getName()
                    + " (" + Math.round(nearestDistanceMeters) + "m)";
        }

        return "Đang di chuyển — chưa xác định bến";
    }
}
