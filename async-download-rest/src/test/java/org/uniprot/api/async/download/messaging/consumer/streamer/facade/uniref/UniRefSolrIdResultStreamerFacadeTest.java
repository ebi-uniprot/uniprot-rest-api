package org.uniprot.api.async.download.messaging.consumer.streamer.facade.uniref;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.uniref.UniRefSolrIdBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.SolrIdResultStreamerFacadeTest;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.uniref.UniRefListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.uniref.UniRefRDFResultStreamer;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefFileHandler;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@ExtendWith(MockitoExtension.class)
class UniRefSolrIdResultStreamerFacadeTest
        extends SolrIdResultStreamerFacadeTest<
                UniRefDownloadRequest, UniRefDownloadJob, UniRefEntryLight> {
    @Mock private Stream<UniRefEntryLight> uniRefEntryLightStream;
    @Mock private MessageConverterContext<UniRefEntryLight> uniRefEntryLightMessageConverterContext;
    @Mock private UniRefDownloadRequest uniRefDownloadRequest;
    @Mock private UniRefRDFResultStreamer uniRefRDFResultStreamer;
    @Mock private UniRefListResultStreamer uniRefListResultStreamer;
    @Mock private UniRefSolrIdBatchResultStreamer uniRefBatchResultStreamer;
    @Mock private MessageConverterContextFactory<UniRefEntryLight> uniRefConverterContextFactory;
    @Mock private UniRefFileHandler uniRefAsyncDownloadFileHandler;

    @BeforeEach
    void setUp() {
        entryStream = uniRefEntryLightStream;
        messageConverterContext = uniRefEntryLightMessageConverterContext;
        downloadRequest = uniRefDownloadRequest;
        rdfResultStreamer = uniRefRDFResultStreamer;
        fileHandler = uniRefAsyncDownloadFileHandler;
        listResultStreamer = uniRefListResultStreamer;
        solrIdBatchResultStreamer = uniRefBatchResultStreamer;
        converterContextFactory = uniRefConverterContextFactory;
        solrIdResultStreamerFacade =
                new UniRefSolrIdResultStreamerFacade(
                        uniRefRDFResultStreamer,
                        uniRefListResultStreamer,
                        uniRefBatchResultStreamer,
                        uniRefConverterContextFactory,
                        uniRefAsyncDownloadFileHandler);
        mock();
    }
}
