package org.uniprot.api.async.download.controller.validator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.common.exception.InvalidRequestException;

class UniParcIdMappingDownloadRequestValidatorTest {

    @Test
    void canGetType() {
        UniParcIdMappingDownloadRequestValidator validator =
                new UniParcIdMappingDownloadRequestValidator();
        assertEquals("UniParc", validator.getType());
    }

    @Test
    void canValidateWithInvalidFormatNull() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();

        UniParcIdMappingDownloadRequestValidator validator =
                new UniParcIdMappingDownloadRequestValidator();
        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> validator.validate(request));
        assertNotNull(exception);
        assertEquals(
                "Invalid download format. Valid values are [text/plain;format=fasta, text/plain;format=tsv, application/json, application/xml, application/rdf+xml, text/turtle, application/n-triples, text/plain;format=list]",
                exception.getMessage());
    }

    @Test
    void canValidateWithFormatInvalid() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("html");
        UniParcIdMappingDownloadRequestValidator validator =
                new UniParcIdMappingDownloadRequestValidator();
        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> validator.validate(request));
        assertNotNull(exception);
        assertEquals(
                "Invalid download format. Valid values are [text/plain;format=fasta, text/plain;format=tsv, application/json, application/xml, application/rdf+xml, text/turtle, application/n-triples, text/plain;format=list]",
                exception.getMessage());
    }

    @Test
    void canValidateWithSimpleFormatValidate() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("json");

        UniParcIdMappingDownloadRequestValidator validator =
                new UniParcIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void canValidateWithFullFormatValidate() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("application/json");

        UniParcIdMappingDownloadRequestValidator validator =
                new UniParcIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void canValidateWithInValidFields() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("fasta");
        request.setFields("upid, identity");

        UniParcIdMappingDownloadRequestValidator validator =
                new UniParcIdMappingDownloadRequestValidator();
        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> validator.validate(request));
        assertNotNull(exception);
        assertEquals(
                "Invalid UniParc fields parameter values: [upid, identity].",
                exception.getMessage());
    }

    @Test
    void canValidateWithInValidField() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("fasta");
        request.setFields("invalid");

        UniParcIdMappingDownloadRequestValidator validator =
                new UniParcIdMappingDownloadRequestValidator();
        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> validator.validate(request));
        assertNotNull(exception);
        assertEquals("Invalid UniParc fields parameter value: [invalid].", exception.getMessage());
    }

    @Test
    void canValidateWithValidFields() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("fasta");
        request.setFields(" upi , checksum ");

        UniParcIdMappingDownloadRequestValidator validator =
                new UniParcIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void canValidateWithValidField() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("fasta");
        request.setFields(" organism_id ");

        UniParcIdMappingDownloadRequestValidator validator =
                new UniParcIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }
}
