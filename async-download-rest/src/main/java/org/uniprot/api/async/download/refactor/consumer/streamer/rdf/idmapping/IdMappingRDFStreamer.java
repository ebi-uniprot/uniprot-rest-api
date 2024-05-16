package org.uniprot.api.async.download.refactor.consumer.streamer.rdf.idmapping;

import org.uniprot.api.async.download.messaging.listener.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.rdf.RDFResultStreamer;
import org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.refactor.service.idmapping.IdMappingJobService;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;

public abstract class IdMappingRDFStreamer extends RDFResultStreamer<IdMappingDownloadRequest, IdMappingDownloadJob> {
    protected IdMappingRDFStreamer(IdMappingHeartbeatProducer heartbeatProducer, IdMappingJobService jobService, RdfStreamer idMappingRdfStreamer) {
        super(heartbeatProducer, jobService, idMappingRdfStreamer);
    }
}
