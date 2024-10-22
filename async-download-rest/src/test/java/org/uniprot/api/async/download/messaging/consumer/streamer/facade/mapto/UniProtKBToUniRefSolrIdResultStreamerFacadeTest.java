package org.uniprot.api.async.download.messaging.consumer.streamer.facade.mapto;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.mapto.UniProtKBToUniRefSolrIdBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.mapto.UniProtKBToUniRefToListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.mapto.UniProtKBToUniRefRDFResultStreamer;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@ExtendWith(MockitoExtension.class)
public class UniProtKBToUniRefSolrIdResultStreamerFacadeTest
        extends MapSolrIdResultStreamerFacadeTest<
                UniProtKBToUniRefDownloadRequest, UniRefEntryLight> {
    @Mock private UniProtKBToUniRefDownloadRequest uniProtKBToUniRefMapDownloadRequest;
    @Mock private UniProtKBToUniRefRDFResultStreamer uniProtKBToUniRefRDFResultStreamer;
    @Mock private UniProtKBToUniRefToListResultStreamer uniProtKBToUniReListResultStreamer;

    @Mock
    private UniProtKBToUniRefSolrIdBatchResultStreamer uniProtKBToUniRefSolrIdBatchResultStreamer;

    @Mock private Stream<UniRefEntryLight> refEntryLightStream;
    @Mock private MessageConverterContext<UniRefEntryLight> uniRefEntryLightMessageConverterContext;

    @Mock
    private MessageConverterContextFactory<UniRefEntryLight>
            uniRefEntryLightMessageConverterContextFactory;

    @BeforeEach
    void setUp() {
        init();
        entryStream = refEntryLightStream;
        messageConverterContext = uniRefEntryLightMessageConverterContext;
        converterContextFactory = uniRefEntryLightMessageConverterContextFactory;
        downloadRequest = uniProtKBToUniRefMapDownloadRequest;
        rdfResultStreamer = uniProtKBToUniRefRDFResultStreamer;
        listResultStreamer = uniProtKBToUniReListResultStreamer;
        solrIdBatchResultStreamer = uniProtKBToUniRefSolrIdBatchResultStreamer;
        solrIdResultStreamerFacade =
                new UniProtKBToUniRefSolrIdResultStreamerFacade(
                        uniProtKBToUniRefRDFResultStreamer,
                        uniProtKBToUniReListResultStreamer,
                        uniProtKBToUniRefSolrIdBatchResultStreamer,
                        uniRefEntryLightMessageConverterContextFactory,
                        mapToFileHandler);
        mock();
    }
}
