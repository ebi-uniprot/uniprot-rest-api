package org.uniprot.api.async.download.messaging.consumer.streamer.rdf.mapto;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.mapto.MapToHeartbeatProducer;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;

@Component
public class UniProtKBToUniRefRDFResultStreamer
        extends UniRefMapToRDFResultStreamer<UniProtKBToUniRefDownloadRequest> {
    public UniProtKBToUniRefRDFResultStreamer(
            MapToHeartbeatProducer heartbeatProducer,
            MapToJobService jobService,
            RdfStreamer uniRefRdfStreamer) {
        super(heartbeatProducer, jobService, uniRefRdfStreamer);
    }
}
