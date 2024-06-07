package org.uniprot.api.async.download.messaging.consumer.streamer.rdf.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;

@Component
public class UniParcIdMappingRDFResultStreamer extends IdMappingRDFStreamer {
    private static final String ID_MAPPING_DATA_TYPE = "uniparc";

    public UniParcIdMappingRDFResultStreamer(
            IdMappingHeartbeatProducer heartbeatProducer,
            IdMappingJobService jobService,
            RdfStreamer idMappingRdfStreamer) {
        super(heartbeatProducer, jobService, idMappingRdfStreamer);
    }

    @Override
    protected String getDataType() {
        return ID_MAPPING_DATA_TYPE;
    }
}
