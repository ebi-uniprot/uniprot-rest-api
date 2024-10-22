package org.uniprot.api.async.download.messaging.repository;

import lombok.Getter;

@Getter
public enum JobFields {
    RETRIED("retried"),
    STATUS("status"),
    UPDATE_COUNT("updateCount"),
    UPDATED("updated"),
    PROCESSED_ENTRIES("processedEntries"),
    TOTAL_ENTRIES("totalEntries"),
    RESULT_FILE("resultFile"),
    TOTAL_FROM_IDS("totalFromIds");

    private final String name;

    JobFields(String name) {
        this.name = name;
    }
}
