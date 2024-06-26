package org.uniprot.api.async.download.controller.validator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.common.exception.InvalidRequestException;

class UniProtKBIdMappingDownloadRequestValidatorTest {

    @Test
    void canGetType() {
        UniProtKBIdMappingDownloadRequestValidator validator =
                new UniProtKBIdMappingDownloadRequestValidator();
        assertEquals("UniProtKB", validator.getType());
    }

    @Test
    void canValidateWithInvalidFormatNull() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();

        UniProtKBIdMappingDownloadRequestValidator validator =
                new UniProtKBIdMappingDownloadRequestValidator();
        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> validator.validate(request));
        assertNotNull(exception);
        assertEquals(
                "Invalid download format. Valid values are [text/plain;format=fasta, text/plain;format=tsv, application/json, application/xml, application/rdf+xml, text/turtle, application/n-triples, text/plain;format=flatfile, text/plain;format=gff, text/plain;format=list]",
                exception.getMessage());
    }

    @Test
    void canValidateWithFormatInvalid() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("html");
        UniProtKBIdMappingDownloadRequestValidator validator =
                new UniProtKBIdMappingDownloadRequestValidator();
        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> validator.validate(request));
        assertNotNull(exception);
        assertEquals(
                "Invalid download format. Valid values are [text/plain;format=fasta, text/plain;format=tsv, application/json, application/xml, application/rdf+xml, text/turtle, application/n-triples, text/plain;format=flatfile, text/plain;format=gff, text/plain;format=list]",
                exception.getMessage());
    }

    @Test
    void canValidateWithSimpleFormatValidate() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("json");

        UniProtKBIdMappingDownloadRequestValidator validator =
                new UniProtKBIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void canValidateWithFullFormatValidate() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("application/json");

        UniProtKBIdMappingDownloadRequestValidator validator =
                new UniProtKBIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void canValidateWithInValidFields() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("fasta");
        request.setFields("upi, identity");

        UniProtKBIdMappingDownloadRequestValidator validator =
                new UniProtKBIdMappingDownloadRequestValidator();
        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> validator.validate(request));
        assertNotNull(exception);
        assertEquals(
                "Invalid UniProtKB fields parameter values: [upi, identity].",
                exception.getMessage());
    }

    @Test
    void canValidateWithInValidField() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("fasta");
        request.setFields("invalid");

        UniProtKBIdMappingDownloadRequestValidator validator =
                new UniProtKBIdMappingDownloadRequestValidator();
        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> validator.validate(request));
        assertNotNull(exception);
        assertEquals(
                "Invalid UniProtKB fields parameter value: [invalid].", exception.getMessage());
    }

    @Test
    void canValidateWithValidFields() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("fasta");
        request.setFields(" accession , gene_names ");

        UniProtKBIdMappingDownloadRequestValidator validator =
                new UniProtKBIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void canValidateWithValidField() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setFormat("fasta");
        request.setFields(" organism_id ");

        UniProtKBIdMappingDownloadRequestValidator validator =
                new UniProtKBIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }
}
