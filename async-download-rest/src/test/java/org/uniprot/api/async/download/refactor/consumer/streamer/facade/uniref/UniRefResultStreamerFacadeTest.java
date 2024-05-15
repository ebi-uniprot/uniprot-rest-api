package org.uniprot.api.async.download.refactor.consumer.streamer.facade.uniref;

import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIREF;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.uniref.UniRefBatchResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.ResultStreamerFacadeTest;
import org.uniprot.api.async.download.refactor.consumer.streamer.list.uniref.UniRefListResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.rdf.uniref.UniRefRDFResultStreamer;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@ExtendWith(MockitoExtension.class)
class UniRefResultStreamerFacadeTest
        extends ResultStreamerFacadeTest<
                UniRefDownloadRequest, UniRefDownloadJob, UniRefEntryLight> {
    @Mock private Stream<UniRefEntryLight> uniRefEntryLightStream;
    @Mock private MessageConverterContext<UniRefEntryLight> uniRefEntryLightMessageConverterContext;
    @Mock private UniRefDownloadRequest uniRefDownloadRequest;
    @Mock private UniRefRDFResultStreamer uniRefRDFResultStreamer;
    @Mock private UniRefListResultStreamer uniRefListResultStreamer;
    @Mock private UniRefBatchResultStreamer uniRefBatchResultStreamer;
    @Mock private MessageConverterContextFactory<UniRefEntryLight> uniRefConverterContextFactory;

    @BeforeEach
    void setUp() {
        entryStream = uniRefEntryLightStream;
        messageConverterContext = uniRefEntryLightMessageConverterContext;
        downloadRequest = uniRefDownloadRequest;
        rdfResultStreamer = uniRefRDFResultStreamer;
        listResultStreamer = uniRefListResultStreamer;
        batchResultStreamer = uniRefBatchResultStreamer;
        converterContextFactory = uniRefConverterContextFactory;
        resultStreamerFacade =
                new UniRefResultStreamerFacade(
                        uniRefRDFResultStreamer,
                        uniRefListResultStreamer,
                        uniRefBatchResultStreamer,
                        uniRefConverterContextFactory);
        mock();
    }

    @Override
    protected Resource getResource() {
        return UNIREF;
    }
}
