package org.uniprot.api.async.download.messaging.consumer.streamer.rdf.map;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.map.MapHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniref.UniRefHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.RDFResultStreamer;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.map.MapJobService;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;

@Component
public class UniRefMapRDFResultStreamer<T extends MapDownloadRequest>
        extends RDFResultStreamer<T, MapDownloadJob> {
    private static final String UNIREF_DATA_TYPE = "uniref";

    public UniRefMapRDFResultStreamer(
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
