package org.uniprot.api.async.download.messaging.consumer.streamer.facade.uniparc;

import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPARC;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.uniparc.UniParcSolrIdBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.SolrIdResultStreamerFacade;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.uniparc.UniParcListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.uniparc.UniParcRDFResultStreamer;
import org.uniprot.api.async.download.messaging.result.uniparc.UniParcFileHandler;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniparc.UniParcEntry;

@Component
public class UniParcSolrIdResultStreamerFacade
        extends SolrIdResultStreamerFacade<
                UniParcDownloadRequest, UniParcDownloadJob, UniParcEntry> {

    public UniParcSolrIdResultStreamerFacade(
            UniParcRDFResultStreamer rdfResultStreamer,
            UniParcListResultStreamer listResultStreamer,
            UniParcSolrIdBatchResultStreamer batchResultStreamer,
            MessageConverterContextFactory<UniParcEntry> converterContextFactory,
            UniParcFileHandler fileHandler) {
        super(
                rdfResultStreamer,
                listResultStreamer,
                batchResultStreamer,
                converterContextFactory,
                fileHandler);
    }

    @Override
    protected Resource getResource() {
        return UNIPARC;
    }
}
