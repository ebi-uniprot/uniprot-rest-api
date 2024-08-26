package org.uniprot.api.async.download.messaging.consumer.streamer.facade.map;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.map.UniProtKBToUniRefMapSolrIdBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.map.UniProtKBToUniRefMapListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.map.UniProtKBToUniRefMapRDFResultStreamer;
import org.uniprot.api.async.download.messaging.result.map.MapFileHandler;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefFileHandler;
import org.uniprot.api.async.download.model.request.map.UniProtKBMapDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
public class UniProtKBToUniRefMapSolrIdResultStreamerFacade
        extends UniRefMapSolrIdResultStreamerFacade<UniProtKBMapDownloadRequest> {

    public UniProtKBToUniRefMapSolrIdResultStreamerFacade(
            UniProtKBToUniRefMapRDFResultStreamer rdfResultStreamer,
            UniProtKBToUniRefMapListResultStreamer listResultStreamer,
            UniProtKBToUniRefMapSolrIdBatchResultStreamer batchResultStreamer,
            MessageConverterContextFactory<UniRefEntryLight> converterContextFactory,
            MapFileHandler fileHandler) {
        super(
                rdfResultStreamer,
                listResultStreamer,
                batchResultStreamer,
                converterContextFactory,
                fileHandler);
    }
}
