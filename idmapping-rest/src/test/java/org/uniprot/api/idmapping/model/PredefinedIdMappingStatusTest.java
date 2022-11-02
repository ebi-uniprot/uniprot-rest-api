package org.uniprot.api.idmapping.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PredefinedIdMappingStatusTest {

    @Test
    void canGetParsedErrorMessage() {
        String errorMessage = PredefinedIdMappingStatus.LIMIT_EXCEED_ERROR.getErrorMessage(12);
        assertEquals(
                "Id Mapping API is not supported for mapping results with more than 12 \"mapped to\" IDs",
                errorMessage);
    }
}
