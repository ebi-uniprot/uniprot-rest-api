package org.uniprot.api.async.download.controller.validator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.common.exception.InvalidRequestException;

class UniRefIdMappingDownloadRequestValidatorTest {

    @Test
    void canGetType() {
        UniRefIdMappingDownloadRequestValidator validator =
                new UniRefIdMappingDownloadRequestValidator();
        assertEquals("uniref", validator.getType());
    }

    @Test
    void canValidateWithInvalidFormatNull() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();

        UniRefIdMappingDownloadRequestValidator validator =
                new UniRefIdMappingDownloadRequestValidator();
        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> validator.validate(request));
        assertNotNull(exception);
        assertEquals(
                "Invalid download format. Valid values are [text/plain;format=fasta, text/plain;format=tsv, application/json, text/plain;format=list, application/rdf+xml, text/turtle, application/n-triples]",
                exception.getMessage());
    }

    @Test
    void canValidateWithFormatInvalid() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("xml");
        UniRefIdMappingDownloadRequestValidator validator =
                new UniRefIdMappingDownloadRequestValidator();
        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> validator.validate(request));
        assertNotNull(exception);
        assertEquals(
                "Invalid download format. Valid values are [text/plain;format=fasta, text/plain;format=tsv, application/json, text/plain;format=list, application/rdf+xml, text/turtle, application/n-triples]",
                exception.getMessage());
    }

    @Test
    void canValidateWithSimpleFormatValidate() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("json");

        UniRefIdMappingDownloadRequestValidator validator =
                new UniRefIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void canValidateWithFullFormatValidate() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("application/json");

        UniRefIdMappingDownloadRequestValidator validator =
                new UniRefIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void canValidateWithInValidFields() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("fasta");
        request.setFields("accession, checksum");

        UniRefIdMappingDownloadRequestValidator validator =
                new UniRefIdMappingDownloadRequestValidator();
        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> validator.validate(request));
        assertNotNull(exception);
        assertEquals(
                "Invalid uniref fields parameter values: [accession, checksum].",
                exception.getMessage());
    }

    @Test
    void canValidateWithInValidField() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("fasta");
        request.setFields("invalid");

        UniRefIdMappingDownloadRequestValidator validator =
                new UniRefIdMappingDownloadRequestValidator();
        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> validator.validate(request));
        assertNotNull(exception);
        assertEquals("Invalid uniref fields parameter value: [invalid].", exception.getMessage());
    }

    @Test
    void canValidateWithValidFields() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("fasta");
        request.setFields(" id , name ");

        UniRefIdMappingDownloadRequestValidator validator =
                new UniRefIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void canValidateWithValidField() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("fasta");
        request.setFields(" organism_id ");

        UniRefIdMappingDownloadRequestValidator validator =
                new UniRefIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }
}
