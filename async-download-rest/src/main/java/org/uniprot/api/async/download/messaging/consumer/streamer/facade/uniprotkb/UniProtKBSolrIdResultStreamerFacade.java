package org.uniprot.api.async.download.messaging.consumer.streamer.facade.uniprotkb;

import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROTKB;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.uniprotkb.UniProtKBSolrIdBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.SolrIdResultStreamerFacade;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.uniprotkb.UniProtKBListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.uniprotkb.UniProtKBRDFResultStreamer;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@Component
public class UniProtKBSolrIdResultStreamerFacade
        extends SolrIdResultStreamerFacade<
                                UniProtKBDownloadRequest, UniProtKBDownloadJob, UniProtKBEntry> {

    public UniProtKBSolrIdResultStreamerFacade(
            UniProtKBRDFResultStreamer rdfResultStreamer,
            UniProtKBListResultStreamer listResultStreamer,
            UniProtKBSolrIdBatchResultStreamer batchResultStreamer,
            MessageConverterContextFactory<UniProtKBEntry> converterContextFactory, UniProtKBAsyncDownloadFileHandler fileHandler) {
        super(rdfResultStreamer, listResultStreamer, batchResultStreamer, converterContextFactory, fileHandler);
    }

    @Override
    protected Resource getResource() {
        return UNIPROTKB;
    }
}
