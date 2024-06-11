package org.uniprot.api.async.download.messaging.consumer.streamer.facade.uniref;

import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIREF;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.uniref.UniRefSolrIdBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.SolrIdResultStreamerFacade;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.uniref.UniRefListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.uniref.UniRefRDFResultStreamer;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
public class UniRefSolrIdResultStreamerFacade
        extends SolrIdResultStreamerFacade<
                UniRefDownloadRequest, UniRefDownloadJob, UniRefEntryLight> {

    public UniRefSolrIdResultStreamerFacade(
            UniRefRDFResultStreamer rdfResultStreamer,
            UniRefListResultStreamer listResultStreamer,
            UniRefSolrIdBatchResultStreamer batchResultStreamer,
            MessageConverterContextFactory<UniRefEntryLight> converterContextFactory,
            UniRefAsyncDownloadFileHandler fileHandler) {
        super(
                rdfResultStreamer,
                listResultStreamer,
                batchResultStreamer,
                converterContextFactory,
                fileHandler);
    }

    @Override
    protected Resource getResource() {
        return UNIREF;
    }
}
