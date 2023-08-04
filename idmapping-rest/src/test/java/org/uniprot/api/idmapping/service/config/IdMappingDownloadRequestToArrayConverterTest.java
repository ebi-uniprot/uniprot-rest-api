package org.uniprot.api.idmapping.service.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.uniprot.api.idmapping.controller.request.IdMappingDownloadRequestImpl;

class IdMappingDownloadRequestToArrayConverterTest {

    @Test
    void canConvertFullRequest() {
        var converter = new IdMappingDownloadRequestToArrayConverter();
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
        request.setJobId("JOBID ");
        request.setFormat(" format");
        request.setFields(" fieldValue ");
        char[] result = converter.apply(request);
        assertNotNull(result);
        assertArrayEquals("jobidformatfieldvalue".toCharArray(), result);
    }

    @Test
    void canConvertEmptyRequest() {
        var converter = new IdMappingDownloadRequestToArrayConverter();
        char[] result = converter.apply(new IdMappingDownloadRequestImpl());
        assertNotNull(result);
        assertArrayEquals(new char[0], result);
    }
}
