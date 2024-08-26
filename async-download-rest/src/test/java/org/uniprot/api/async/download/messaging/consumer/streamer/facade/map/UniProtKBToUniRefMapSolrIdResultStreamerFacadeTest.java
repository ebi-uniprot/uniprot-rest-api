package org.uniprot.api.async.download.messaging.consumer.streamer.facade.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.map.UniProtKBToUniRefMapSolrIdBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.map.UniProtKBToUniRefMapListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.map.UniProtKBToUniRefMapRDFResultStreamer;
import org.uniprot.api.async.download.model.request.map.UniProtKBMapDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
public class UniProtKBToUniRefMapSolrIdResultStreamerFacadeTest extends MapSolrIdResultStreamerFacadeTest<UniProtKBMapDownloadRequest, UniRefEntryLight> {
    @Mock
    private UniProtKBMapDownloadRequest uniProtKBMapDownloadRequest;
    @Mock
    private UniProtKBToUniRefMapRDFResultStreamer uniProtKBToUniRefMapRDFResultStreamer;
    @Mock
    private UniProtKBToUniRefMapListResultStreamer uniProtKBToUniRefMapListResultStreamer;
    @Mock
    private UniProtKBToUniRefMapSolrIdBatchResultStreamer uniProtKBToUniRefMapSolrIdBatchResultStreamer;
    @Mock
    private Stream<UniRefEntryLight> refEntryLightStream;
    @Mock
    private MessageConverterContext<UniRefEntryLight> uniRefEntryLightMessageConverterContext;
    @Mock
    private MessageConverterContextFactory<UniRefEntryLight> uniRefEntryLightMessageConverterContextFactory;

    @BeforeEach
    void setUp() {
        init();
        entryStream = refEntryLightStream;
        messageConverterContext = uniRefEntryLightMessageConverterContext;
        converterContextFactory = uniRefEntryLightMessageConverterContextFactory;
        downloadRequest = uniProtKBMapDownloadRequest;
        rdfResultStreamer = uniProtKBToUniRefMapRDFResultStreamer;
        listResultStreamer = uniProtKBToUniRefMapListResultStreamer;
        solrIdBatchResultStreamer = uniProtKBToUniRefMapSolrIdBatchResultStreamer;
        solrIdResultStreamerFacade =
                new UniProtKBToUniRefMapSolrIdResultStreamerFacade(
                        uniProtKBToUniRefMapRDFResultStreamer,
                        uniProtKBToUniRefMapListResultStreamer,
                        uniProtKBToUniRefMapSolrIdBatchResultStreamer,
                        uniRefEntryLightMessageConverterContextFactory,
                        mapFileHandler);
        mock();
    }
}
