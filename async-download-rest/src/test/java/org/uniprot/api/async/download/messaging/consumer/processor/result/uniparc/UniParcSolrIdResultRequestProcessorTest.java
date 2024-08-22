package org.uniprot.api.async.download.messaging.consumer.processor.result.uniparc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.config.uniparc.UniParcDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniparc.UniParcHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.processor.result.SolrIdResultRequestProcessorTest;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.uniparc.UniParcSolrIdResultStreamerFacade;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;
import org.uniprot.core.uniparc.UniParcEntry;

@ExtendWith(MockitoExtension.class)
public class UniParcSolrIdResultRequestProcessorTest
        extends SolrIdResultRequestProcessorTest<
                UniParcDownloadRequest, UniParcDownloadJob, UniParcEntry> {
    @Mock private UniParcDownloadConfigProperties uniParcDownloadConfigProperties;
    @Mock private UniParcHeartbeatProducer uniParcHeartbeatProducer;
    @Mock private UniParcSolrIdResultStreamerFacade uniParcResultStreamerFacade;
    @Mock private UUWMessageConverterFactory uuwMessageConverterFactory;
    @Mock private UniParcDownloadRequest uniParcDownloadRequest;

    @BeforeEach
    void setUp() {
        downloadConfigProperties = uniParcDownloadConfigProperties;
        heartbeatProducer = uniParcHeartbeatProducer;
        solrIdResultStreamerFacade = uniParcResultStreamerFacade;
        messageConverterFactory = uuwMessageConverterFactory;
        solrIdResultRequestProcessor =
                new UniParcSolrIdResultRequestProcessor(
                        uniParcDownloadConfigProperties,
                        uniParcHeartbeatProducer,
                        uniParcResultStreamerFacade,
                        uuwMessageConverterFactory);
        request = uniParcDownloadRequest;
    }
}
