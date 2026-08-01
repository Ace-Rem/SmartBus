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

        if (nearestStop != null && nearestDistanceMeters != null) {
            String nearPart = "Gần nhất: " + nearestStop.getName()
                    + " (" + Math.round(nearestDistanceMeters) + "m)";
            if (nextStop != null) {
                String alighting = passengersAlightingAtNextStop > 0
                        ? " — " + passengersAlightingAtNextStop + " khách sẽ xuống"
                        : "";
                return nearPart + " — tiếp theo: " + nextStop.getName() + alighting;
            }
            return nearPart + " — đang ở bến cuối tuyến";
        }

        if (nextStop != null) {
            if (passengersAlightingAtNextStop > 0) {
                return "Bến tiếp theo: " + nextStop.getName()
                        + " — " + passengersAlightingAtNextStop + " khách sẽ xuống";
            }
            return "Bến tiếp theo: " + nextStop.getName();
        }

        if (currentStop != null) {
            return "Đang tại bến: " + currentStop.getName();
        }

        return "Đang di chuyển — chưa xác định bến";
    }
}
