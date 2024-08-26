package org.uniprot.api.async.download.messaging.consumer.streamer.rdf.map;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.map.MapHeartbeatProducer;
import org.uniprot.api.async.download.model.request.map.UniProtKBMapDownloadRequest;
import org.uniprot.api.async.download.service.map.MapJobService;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;

@Component
public class UniProtKBToUniRefMapRDFResultStreamer
        extends UniRefMapRDFResultStreamer<UniProtKBMapDownloadRequest> {
    private static final String UNIREF_DATA_TYPE = "uniref";

    public UniProtKBToUniRefMapRDFResultStreamer(
            MapHeartbeatProducer heartbeatProducer,
            MapJobService jobService,
            RdfStreamer uniRefRdfStreamer) {
        super(heartbeatProducer, jobService, uniRefRdfStreamer);
    }

    @Override
    protected String getDataType() {
        return UNIREF_DATA_TYPE;
    }
}
