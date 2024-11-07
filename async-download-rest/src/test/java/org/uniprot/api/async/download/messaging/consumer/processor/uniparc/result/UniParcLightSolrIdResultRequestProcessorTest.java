package org.uniprot.api.async.download.messaging.consumer.processor.uniparc.result;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.uniparc.UniParcDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniparc.UniParcHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.processor.SolrIdResultRequestProcessorTest;
import org.uniprot.api.async.download.messaging.consumer.processor.result.uniparc.UniParcLightSolrIdResultRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.uniparc.UniParcLightSolrIdResultStreamerFacade;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniparc.UniParcEntryLight;

@ExtendWith(MockitoExtension.class)
public class UniParcLightSolrIdResultRequestProcessorTest
        extends SolrIdResultRequestProcessorTest<
                UniParcDownloadRequest, UniParcDownloadJob, UniParcEntryLight> {
    @Mock private UniParcDownloadConfigProperties uniParcDownloadConfigProperties;
    @Mock private UniParcHeartbeatProducer uniParcHeartbeatProducer;
    @Mock private UniParcLightSolrIdResultStreamerFacade uniParcResultStreamerFacade;
    @Mock private UUWMessageConverterFactory uuwMessageConverterFactory;
    @Mock private UniParcDownloadRequest uniParcDownloadRequest;

    @BeforeEach
    void setUp() {
        downloadConfigProperties = uniParcDownloadConfigProperties;
        heartbeatProducer = uniParcHeartbeatProducer;
        solrIdResultStreamerFacade = uniParcResultStreamerFacade;
        messageConverterFactory = uuwMessageConverterFactory;
        solrIdResultRequestProcessor =
                new UniParcLightSolrIdResultRequestProcessor(
                        uniParcDownloadConfigProperties,
                        uniParcHeartbeatProducer,
                        uniParcResultStreamerFacade,
                        uuwMessageConverterFactory);
        request = uniParcDownloadRequest;
    }
}
