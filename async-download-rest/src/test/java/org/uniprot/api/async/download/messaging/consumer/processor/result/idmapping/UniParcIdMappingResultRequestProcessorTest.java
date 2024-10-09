package org.uniprot.api.async.download.messaging.consumer.processor.result.idmapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.processor.result.IdMappingResultRequestProcessorTest;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.idmapping.UniParcLightIdMappingResultStreamerFacade;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryLightPair;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniparc.UniParcEntryLight;

@ExtendWith(MockitoExtension.class)
public class UniParcIdMappingResultRequestProcessorTest
        extends IdMappingResultRequestProcessorTest<UniParcEntryLight, UniParcEntryLightPair> {
    @Mock private IdMappingDownloadConfigProperties idMappingDownloadConfigProperties;
    @Mock private IdMappingHeartbeatProducer idMappingHeartbeatProducer;

    @Mock
    private UniParcLightIdMappingResultStreamerFacade uniParcLightIdMappingResultStreamerFacade;

    @Mock private UUWMessageConverterFactory uuwMessageConverterFactory;

    @BeforeEach
    void setUp() {
        downloadConfigProperties = idMappingDownloadConfigProperties;
        heartbeatProducer = idMappingHeartbeatProducer;
        solrIdResultStreamerFacade = uniParcLightIdMappingResultStreamerFacade;
        messageConverterFactory = uuwMessageConverterFactory;
        solrIdResultRequestProcessor =
                new UniParcIdMappingResultRequestProcessor(
                        idMappingDownloadConfigProperties,
                        idMappingHeartbeatProducer,
                        uniParcLightIdMappingResultStreamerFacade,
                        uuwMessageConverterFactory);
    }
}
