package org.uniprot.api.async.download.messaging.consumer.streamer.facade.mapto;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.mapto.UniProtKBToUniRefSolrIdBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.mapto.UniProtKBToUniRefToListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.mapto.UniProtKBToUniRefRDFResultStreamer;
import org.uniprot.api.async.download.messaging.result.mapto.MapToFileHandler;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
public class UniProtKBToUniRefSolrIdResultStreamerFacade
        extends UniRefMapToSolrIdResultStreamerFacade<UniProtKBToUniRefDownloadRequest> {

    public UniProtKBToUniRefSolrIdResultStreamerFacade(
            UniProtKBToUniRefRDFResultStreamer rdfResultStreamer,
            UniProtKBToUniRefToListResultStreamer listResultStreamer,
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
