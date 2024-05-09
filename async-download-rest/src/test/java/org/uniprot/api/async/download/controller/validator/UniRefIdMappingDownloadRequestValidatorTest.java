package org.uniprot.api.async.download.controller.validator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadRequestImpl;
import org.uniprot.api.common.exception.InvalidRequestException;

class UniRefIdMappingDownloadRequestValidatorTest {

    @Test
    void canGetType() {
        UniRefIdMappingDownloadRequestValidator validator =
                new UniRefIdMappingDownloadRequestValidator();
        assertEquals("UniRef", validator.getType());
    }

    @Test
    void canValidateWithInvalidFormatNull() {
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();

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
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
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
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
        request.setFormat("json");

        UniRefIdMappingDownloadRequestValidator validator =
                new UniRefIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void canValidateWithFullFormatValidate() {
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
        request.setFormat("application/json");

        UniRefIdMappingDownloadRequestValidator validator =
                new UniRefIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void canValidateWithInValidFields() {
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
        request.setFormat("fasta");
        request.setFields("accession, checksum");

        UniRefIdMappingDownloadRequestValidator validator =
                new UniRefIdMappingDownloadRequestValidator();
        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> validator.validate(request));
        assertNotNull(exception);
        assertEquals(
                "Invalid UniRef fields parameter values: [accession, checksum].",
                exception.getMessage());
    }

    @Test
    void canValidateWithInValidField() {
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
        request.setFormat("fasta");
        request.setFields("invalid");

        UniRefIdMappingDownloadRequestValidator validator =
                new UniRefIdMappingDownloadRequestValidator();
        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> validator.validate(request));
        assertNotNull(exception);
        assertEquals("Invalid UniRef fields parameter value: [invalid].", exception.getMessage());
    }

    @Test
    void canValidateWithValidFields() {
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
        request.setFormat("fasta");
        request.setFields(" id , name ");

        UniRefIdMappingDownloadRequestValidator validator =
                new UniRefIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void canValidateWithValidField() {
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
        request.setFormat("fasta");
        request.setFields(" organism_id ");

        UniRefIdMappingDownloadRequestValidator validator =
                new UniRefIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }
}
