package org.uniprot.api.async.download.refactor.consumer.processor.result.idmapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.refactor.consumer.processor.result.IdMappingResultRequestProcessorTest;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.idmapping.UniRefIdMappingResultStreamerFacade;
import org.uniprot.api.idmapping.common.response.model.UniRefEntryPair;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@ExtendWith(MockitoExtension.class)
public class UniRefIdMappingResultRequestProcessorTest extends IdMappingResultRequestProcessorTest<UniRefEntryLight, UniRefEntryPair> {
    @Mock
    private IdMappingDownloadConfigProperties idMappingDownloadConfigProperties;
    @Mock private IdMappingHeartbeatProducer idMappingHeartbeatProducer;
    @Mock private UniRefIdMappingResultStreamerFacade uniRefResultStreamerFacade;
    @Mock private UUWMessageConverterFactory uuwMessageConverterFactory;

    @BeforeEach
    void setUp() {
        downloadConfigProperties = idMappingDownloadConfigProperties;
        heartbeatProducer = idMappingHeartbeatProducer;
        solrIdResultStreamerFacade = uniRefResultStreamerFacade;
        messageConverterFactory = uuwMessageConverterFactory;
        solrIdResultRequestProcessor =
                new UniRefIdMappingResultRequestProcessor(
                        idMappingDownloadConfigProperties,
                        idMappingHeartbeatProducer,
                        uniRefResultStreamerFacade,
                        uuwMessageConverterFactory);
    }
}
