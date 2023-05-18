package org.uniprot.api.idmapping.controller.validator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.idmapping.controller.request.IdMappingDownloadRequestImpl;

class UniProtKBIdMappingDownloadRequestValidatorTest {

    @Test
    void canGetType() {
        UniProtKBIdMappingDownloadRequestValidator validator =
                new UniProtKBIdMappingDownloadRequestValidator();
        assertEquals("UniProtKB", validator.getType());
    }

    @Test
    void canValidateWithInvalidFormatNull() {
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();

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
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
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
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
        request.setFormat("json");

        UniProtKBIdMappingDownloadRequestValidator validator =
                new UniProtKBIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void canValidateWithFullFormatValidate() {
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
        request.setFormat("application/json");

        UniProtKBIdMappingDownloadRequestValidator validator =
                new UniProtKBIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void canValidateWithInValidFields() {
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
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
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
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
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
        request.setFormat("fasta");
        request.setFields(" accession , gene_names ");

        UniProtKBIdMappingDownloadRequestValidator validator =
                new UniProtKBIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void canValidateWithValidField() {
        IdMappingDownloadRequestImpl request = new IdMappingDownloadRequestImpl();
        request.setFormat("fasta");
        request.setFields(" organism_id ");

        UniProtKBIdMappingDownloadRequestValidator validator =
                new UniProtKBIdMappingDownloadRequestValidator();
        assertDoesNotThrow(() -> validator.validate(request));
    }
}
