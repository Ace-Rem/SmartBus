package com.smartbus.backend.util;

import com.smartbus.backend.entity.Stop;

/**
 * Builds a group note such as "A -> D" for PassengerRecord.
 * One record = one group of passengers, never individuals.
 */
public final class PassengerGroupNoteBuilder {

    private PassengerGroupNoteBuilder() {
    }

    public static String build(Stop boardingStop, Stop alightingStop, String existingNote) {
        if (existingNote != null && !existingNote.isBlank()) {
            return existingNote.trim();
        }
        if (boardingStop != null && alightingStop != null) {
            return boardingStop.getName() + " -> " + alightingStop.getName();
        }
        if (alightingStop != null) {
            return "Xuống tại " + alightingStop.getName();
        }
        if (boardingStop != null) {
            return "Lên tại " + boardingStop.getName();
        }
        return null;
    }
}
