package org.uniprot.api.async.download.model.request;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.uniprot.api.async.download.model.request.IdMappingDownloadRequestToArrayConverter;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;

class IdMappingDownloadRequestToArrayConverterTest {

    @Test
    void canConvertFullRequest() {
        var converter = new IdMappingDownloadRequestToArrayConverter();
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
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
        char[] result = converter.apply(new IdMappingDownloadRequest());
        assertNotNull(result);
        assertArrayEquals(new char[0], result);
    }
}
