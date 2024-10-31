package org.uniprot.api.async.download.messaging.consumer.processor.idmapping;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.processor.ResultProcessingException;

@ExtendWith(MockitoExtension.class)
class IdMappingResultRequestProcessorFactoryTest {
    @Mock private UniParcIdMappingResultRequestProcessor uniParcIdMappingResultRequestProcessor;
    @Mock private UniRefIdMappingResultRequestProcessor uniRefIdMappingResultRequestProcessor;
    @Mock private UniProtKBMappingResultRequestProcessor uniProtKBMappingResultRequestProcessor;

    @InjectMocks
    private IdMappingResultRequestProcessorFactory idMappingResultRequestProcessorFactory;

    @Test
    void getRequestProcessor_uniProtKb() {
        assertSame(
                uniProtKBMappingResultRequestProcessor,
                idMappingResultRequestProcessorFactory.getRequestProcessor(UNIPROTKB_STR));
    }

    @Test
    void getRequestProcessor_uniParc() {
        assertSame(
                uniParcIdMappingResultRequestProcessor,
                idMappingResultRequestProcessorFactory.getRequestProcessor(UNIPARC_STR));
    }

    @Test
    void getRequestProcessor_uniRef50() {
        assertSame(
                uniRefIdMappingResultRequestProcessor,
                idMappingResultRequestProcessorFactory.getRequestProcessor(UNIREF_50_STR));
    }

    @Test
    void getRequestProcessor_uniRef90() {
        assertSame(
                uniRefIdMappingResultRequestProcessor,
                idMappingResultRequestProcessorFactory.getRequestProcessor(UNIREF_90_STR));
    }

    @Test
    void getRequestProcessor_uniRef100() {
        assertSame(
                uniRefIdMappingResultRequestProcessor,
                idMappingResultRequestProcessorFactory.getRequestProcessor(UNIREF_100_STR));
    }

    @Test
    void getRequestProcessor_inValidType() {
        assertThrows(
                ResultProcessingException.class,
                () -> idMappingResultRequestProcessorFactory.getRequestProcessor("randomType"));
    }
}
