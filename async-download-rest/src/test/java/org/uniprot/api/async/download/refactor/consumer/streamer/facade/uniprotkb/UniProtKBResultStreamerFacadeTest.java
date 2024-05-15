package org.uniprot.api.async.download.refactor.consumer.streamer.facade.uniprotkb;

import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROTKB;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.uniprotkb.UniProtKBBatchResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.ResultStreamerFacadeTest;
import org.uniprot.api.async.download.refactor.consumer.streamer.list.uniprotkb.UniProtKBListResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.rdf.uniprotkb.UniProtKBRDFResultStreamer;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@ExtendWith(MockitoExtension.class)
class UniProtKBResultStreamerFacadeTest
        extends ResultStreamerFacadeTest<
                UniProtKBDownloadRequest, UniProtKBDownloadJob, UniProtKBEntry> {
    @Mock private Stream<UniProtKBEntry> uniProtKBEntryStream;
    @Mock private MessageConverterContext<UniProtKBEntry> uniProtKBEntryMessageConverterContext;
    @Mock private UniProtKBDownloadRequest uniProtKBDownloadRequest;
    @Mock private UniProtKBRDFResultStreamer uniProtKBRDFResultStreamer;
    @Mock private UniProtKBListResultStreamer uniProtKBListResultStreamer;
    @Mock private UniProtKBBatchResultStreamer uniProtKBBatchResultStreamer;
    @Mock private MessageConverterContextFactory<UniProtKBEntry> uniProtKBConverterContextFactory;

    @BeforeEach
    void setUp() {
        entryStream = uniProtKBEntryStream;
        messageConverterContext = uniProtKBEntryMessageConverterContext;
        downloadRequest = uniProtKBDownloadRequest;
        rdfResultStreamer = uniProtKBRDFResultStreamer;
        listResultStreamer = uniProtKBListResultStreamer;
        batchResultStreamer = uniProtKBBatchResultStreamer;
        converterContextFactory = uniProtKBConverterContextFactory;
        resultStreamerFacade =
                new UniProtKBResultStreamerFacade(
                        uniProtKBRDFResultStreamer,
                        uniProtKBListResultStreamer,
                        uniProtKBBatchResultStreamer,
                        uniProtKBConverterContextFactory);
        mock();
    }

    @Override
    protected Resource getResource() {
        return UNIPROTKB;
    }
}
