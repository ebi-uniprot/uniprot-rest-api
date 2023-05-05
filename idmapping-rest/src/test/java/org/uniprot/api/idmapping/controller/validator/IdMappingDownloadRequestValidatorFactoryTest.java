package org.uniprot.api.idmapping.controller.validator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.uniprot.api.common.exception.InvalidRequestException;

class IdMappingDownloadRequestValidatorFactoryTest {

    @Test
    void canCreateUniProtKB() {
        IdMappingDownloadRequestValidatorFactory factory =
                new IdMappingDownloadRequestValidatorFactory();
        IdMappingDownloadRequestValidator result = factory.create("uniprotkb");
        assertNotNull(result);
        assertTrue(result instanceof UniProtKBIdMappingDownloadRequestValidator);
    }

    @Test
    void canCreateUniParc() {
        IdMappingDownloadRequestValidatorFactory factory =
                new IdMappingDownloadRequestValidatorFactory();
        IdMappingDownloadRequestValidator result = factory.create("uniparc");
        assertNotNull(result);
        assertTrue(result instanceof UniParcIdMappingDownloadRequestValidator);
    }

    @Test
    void canCreateUniRef() {
        IdMappingDownloadRequestValidatorFactory factory =
                new IdMappingDownloadRequestValidatorFactory();
        IdMappingDownloadRequestValidator result = factory.create("uniref50");
        assertNotNull(result);
        assertTrue(result instanceof UniRefIdMappingDownloadRequestValidator);
    }

    @Test
    void canNotCreateInvalidThrowsError() {
        IdMappingDownloadRequestValidatorFactory factory =
                new IdMappingDownloadRequestValidatorFactory();
        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> factory.create("gene"));
        assertNotNull(exception);
        assertEquals(
                "The IdMapping 'to' parameter value is invalid. It should be 'uniprotkb', 'uniparc', 'uniref50', 'uniref90' or 'uniref100'.",
                exception.getMessage());
    }
}
