package org.uniprot.api.async.download.messaging.consumer.streamer.rdf.map;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.mapto.MapToHeartbeatProducer;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;

@Component
public class UniProtKBToUniRefRDFResultStreamer
        extends UniRefMapToRDFResultStreamer<UniProtKBToUniRefDownloadRequest> {
    private static final String UNIREF_DATA_TYPE = "uniref";

    public UniProtKBToUniRefRDFResultStreamer(
            MapToHeartbeatProducer heartbeatProducer,
            MapToJobService jobService,
            RdfStreamer uniRefRdfStreamer) {
        super(heartbeatProducer, jobService, uniRefRdfStreamer);
    }

    @Override
    protected String getDataType() {
        return UNIREF_DATA_TYPE;
    }
}
