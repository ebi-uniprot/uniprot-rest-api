package org.uniprot.api.async.download.messaging.consumer.streamer.facade.map;

import org.uniprot.api.async.download.messaging.consumer.streamer.batch.map.UniRefMapSolrIdBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.SolrIdResultStreamerFacade;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.map.UniRefMapListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.map.UniRefMapRDFResultStreamer;
import org.uniprot.api.async.download.messaging.result.map.MapFileHandler;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefFileHandler;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIREF;

public abstract class UniRefMapSolrIdResultStreamerFacade<T extends MapDownloadRequest>
        extends SolrIdResultStreamerFacade<
        T, MapDownloadJob, UniRefEntryLight> {

    protected UniRefMapSolrIdResultStreamerFacade(
            UniRefMapRDFResultStreamer<T> rdfResultStreamer,
            UniRefMapListResultStreamer<T> listResultStreamer,
            UniRefMapSolrIdBatchResultStreamer<T> batchResultStreamer,
            MessageConverterContextFactory<UniRefEntryLight> converterContextFactory,
            MapFileHandler fileHandler) {
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
