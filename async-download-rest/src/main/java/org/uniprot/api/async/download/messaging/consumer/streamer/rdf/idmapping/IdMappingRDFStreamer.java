package org.uniprot.api.async.download.messaging.consumer.streamer.rdf.idmapping;

import org.uniprot.api.async.download.messaging.consumer.heartbeat.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.RDFResultStreamer;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;

public abstract class IdMappingRDFStreamer
        extends RDFResultStreamer<IdMappingDownloadRequest, IdMappingDownloadJob> {
    protected IdMappingRDFStreamer(
            IdMappingHeartbeatProducer heartbeatProducer,
            IdMappingJobService jobService,
            RdfStreamer idMappingRdfStreamer) {
        super(heartbeatProducer, jobService, idMappingRdfStreamer);
    }
}
