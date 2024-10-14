package org.uniprot.api.async.download.messaging.consumer.streamer.rdf.mapto;

import org.uniprot.api.async.download.messaging.consumer.heartbeat.mapto.MapToHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.RDFResultStreamer;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;

public abstract class UniRefMapToRDFResultStreamer<T extends MapToDownloadRequest>
        extends RDFResultStreamer<T, MapToDownloadJob> {
    private static final String UNIREF_DATA_TYPE = "uniref";

    protected UniRefMapToRDFResultStreamer(
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
