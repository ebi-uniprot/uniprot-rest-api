package org.uniprot.api.idmapping.common.response.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.uniprot.api.rest.output.PredefinedAPIStatus;

class PredefinedIdMappingStatusTest {

    @Test
    void canGetParsedErrorMessage() {
        String errorMessage = PredefinedAPIStatus.LIMIT_EXCEED_ERROR.getErrorMessage(12);
        assertEquals(
                "Id Mapping API is not supported for mapping results with more than 12 \"mapped to\" IDs",
                errorMessage);
    }
}
