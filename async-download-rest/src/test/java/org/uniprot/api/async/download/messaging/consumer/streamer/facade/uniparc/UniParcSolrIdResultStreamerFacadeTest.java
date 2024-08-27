package org.uniprot.api.async.download.messaging.consumer.streamer.facade.uniparc;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.uniparc.UniParcSolrIdBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.SolrIdResultStreamerFacadeTest;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.uniparc.UniParcListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.uniparc.UniParcRDFResultStreamer;
import org.uniprot.api.async.download.messaging.result.uniparc.UniParcFileHandler;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniparc.UniParcEntryLight;

@ExtendWith(MockitoExtension.class)
class UniParcSolrIdResultStreamerFacadeTest
        extends SolrIdResultStreamerFacadeTest<
                UniParcDownloadRequest, UniParcDownloadJob, UniParcEntryLight> {
    @Mock private Stream<UniParcEntryLight> uniParcEntryStream;
    @Mock private MessageConverterContext<UniParcEntryLight> uniParcEntryMessageConverterContext;
    @Mock private UniParcDownloadRequest uniParcDownloadRequest;
    @Mock private UniParcRDFResultStreamer uniParcRDFResultStreamer;
    @Mock private UniParcListResultStreamer uniParcListResultStreamer;
    @Mock private UniParcSolrIdBatchResultStreamer uniParcBatchResultStreamer;
    @Mock private MessageConverterContextFactory<UniParcEntryLight> uniParcConverterContextFactory;
    @Mock private UniParcFileHandler uniParcAsyncDownloadFileHandler;

    @BeforeEach
    void setUp() {
        entryStream = uniParcEntryStream;
        messageConverterContext = uniParcEntryMessageConverterContext;
        downloadRequest = uniParcDownloadRequest;
        rdfResultStreamer = uniParcRDFResultStreamer;
        fileHandler = uniParcAsyncDownloadFileHandler;
        listResultStreamer = uniParcListResultStreamer;
        solrIdBatchResultStreamer = uniParcBatchResultStreamer;
        converterContextFactory = uniParcConverterContextFactory;
        solrIdResultStreamerFacade =
                new UniParcSolrIdResultStreamerFacade(
                        uniParcRDFResultStreamer,
                        uniParcListResultStreamer,
                        uniParcBatchResultStreamer,
                        uniParcConverterContextFactory,
                        uniParcAsyncDownloadFileHandler);
        mock();
    }
}
