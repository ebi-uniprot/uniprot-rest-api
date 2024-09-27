package org.uniprot.api.async.download.messaging.consumer.streamer.facade.map;

import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIREF;

import org.uniprot.api.async.download.messaging.consumer.streamer.batch.map.UniRefMapToSolrIdBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.SolrIdResultStreamerFacade;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.map.UniRefMapToListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.map.UniRefMapToRDFResultStreamer;
import org.uniprot.api.async.download.messaging.result.mapto.MapToFileHandler;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

public abstract class UniRefMapToSolrIdResultStreamerFacade<T extends MapToDownloadRequest>
        extends SolrIdResultStreamerFacade<T, MapToDownloadJob, UniRefEntryLight> {

    protected UniRefMapToSolrIdResultStreamerFacade(
            UniRefMapToRDFResultStreamer<T> rdfResultStreamer,
            UniRefMapToListResultStreamer<T> listResultStreamer,
            UniRefMapToSolrIdBatchResultStreamer<T> batchResultStreamer,
            MessageConverterContextFactory<UniRefEntryLight> converterContextFactory,
            MapToFileHandler fileHandler) {
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
