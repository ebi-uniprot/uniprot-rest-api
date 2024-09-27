package org.uniprot.api.async.download.messaging.consumer.streamer.facade.map;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.map.UniProtKBToUniRefSolrIdBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.map.UniProtKBToUniReToListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.map.UniProtKBToUniRefRDFResultStreamer;
import org.uniprot.api.async.download.messaging.result.mapto.MapToFileHandler;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
public class UniProtKBToUniRefSolrIdResultStreamerFacade
        extends UniRefMapToSolrIdResultStreamerFacade<UniProtKBToUniRefDownloadRequest> {

    public UniProtKBToUniRefSolrIdResultStreamerFacade(
            UniProtKBToUniRefRDFResultStreamer rdfResultStreamer,
            UniProtKBToUniReToListResultStreamer listResultStreamer,
            UniProtKBToUniRefSolrIdBatchResultStreamer batchResultStreamer,
            MessageConverterContextFactory<UniRefEntryLight> converterContextFactory,
            MapToFileHandler fileHandler) {
        super(
                rdfResultStreamer,
                listResultStreamer,
                batchResultStreamer,
                converterContextFactory,
                fileHandler);
    }
}
