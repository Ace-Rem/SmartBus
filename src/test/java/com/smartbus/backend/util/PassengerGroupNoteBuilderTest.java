package com.smartbus.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.smartbus.backend.entity.Stop;
import org.junit.jupiter.api.Test;

class PassengerGroupNoteBuilderTest {

    @Test
    void build_groupFromAToD() {
        Stop boarding = stop("A");
        Stop alighting = stop("D");
        assertEquals("A -> D", PassengerGroupNoteBuilder.build(boarding, alighting, null));
    }

    @Test
    void build_keepsExplicitNote() {
        assertEquals(
                "Nhom hoc sinh",
                PassengerGroupNoteBuilder.build(stop("A"), stop("D"), "  Nhom hoc sinh  ")
        );
    }

    @Test
    void build_alightingOnly() {
        assertEquals("Xuống tại D", PassengerGroupNoteBuilder.build(null, stop("D"), null));
    }

    @Test
    void build_emptyWhenNoStops() {
        assertNull(PassengerGroupNoteBuilder.build(null, null, "   "));
    }

    private static Stop stop(String name) {
        Stop stop = new Stop();
        stop.setName(name);
        return stop;
    }
}
