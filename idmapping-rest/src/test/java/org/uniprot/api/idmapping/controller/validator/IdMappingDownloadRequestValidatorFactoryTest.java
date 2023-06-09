package org.uniprot.api.idmapping.controller.validator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;

class IdMappingDownloadRequestValidatorFactoryTest {

    @Test
    void canCreateUniProtKB() {
        IdMappingDownloadRequestValidatorFactory factory =
                new IdMappingDownloadRequestValidatorFactory();
        IdMappingDownloadRequestValidator result =
                factory.create(IdMappingFieldConfig.UNIPROTKB_STR);
        assertNotNull(result);
        assertTrue(result instanceof UniProtKBIdMappingDownloadRequestValidator);
    }

    @Test
    void canCreateUniParc() {
        IdMappingDownloadRequestValidatorFactory factory =
                new IdMappingDownloadRequestValidatorFactory();
        IdMappingDownloadRequestValidator result = factory.create(IdMappingFieldConfig.UNIPARC_STR);
        assertNotNull(result);
        assertTrue(result instanceof UniParcIdMappingDownloadRequestValidator);
    }

    @Test
    void canCreateUniRef() {
        IdMappingDownloadRequestValidatorFactory factory =
                new IdMappingDownloadRequestValidatorFactory();
        IdMappingDownloadRequestValidator result =
                factory.create(IdMappingFieldConfig.UNIREF_50_STR);
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
                "The IdMapping 'to' parameter value is invalid. It should be 'UniProtKB', 'UniParc', 'UniRef50', 'UniRef90' or 'UniRef100'.",
                exception.getMessage());
    }
}
