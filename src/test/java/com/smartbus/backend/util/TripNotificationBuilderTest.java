package com.smartbus.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.smartbus.backend.entity.Stop;
import org.junit.jupiter.api.Test;

class TripNotificationBuilderTest {

    @Test
    void arrivedWithPassengers_buildsAlightingNotification() {
        Stop current = stop(1L, "Ben A");
        String message = TripNotificationBuilder.build(
                true, current, stop(2L, "Ben B"), current, 12.0, 5, 0
        );
        assertEquals("Đã đến bến Ben A — 5 khách xuống", message);
    }

    @Test
    void approachingNextStop_includesDistanceAndAlighting() {
        Stop next = stop(2L, "Ben D");
        String message = TripNotificationBuilder.build(
                false, stop(1L, "Ben A"), next, next, 40.4, 0, 5
        );
        assertEquals("Gần nhất: Ben D (40m) — tiếp theo: Ben D — 5 khách sẽ xuống", message);
    }

    @Test
    void noStopYet_usesNearest() {
        Stop nearest = stop(1L, "Ben A");
        String message = TripNotificationBuilder.build(
                false, null, null, nearest, 120.0, 0, 0
        );
        assertEquals("Gần nhất: Ben A (120m) — đang ở bến cuối tuyến", message);
    }

    private static Stop stop(Long id, String name) {
        Stop stop = new Stop();
        stop.setId(id);
        stop.setName(name);
        return stop;
    }
}
