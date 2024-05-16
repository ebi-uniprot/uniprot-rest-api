package org.uniprot.api.async.download.refactor.consumer.streamer.rdf.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.listener.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.refactor.service.idmapping.IdMappingJobService;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;

@Component
public class UniRefIdMappingRDFResultStreamer extends IdMappingRDFStreamer {
    private static final String ID_MAPPING_DATA_TYPE = "uniref";

    public UniRefIdMappingRDFResultStreamer(IdMappingHeartbeatProducer heartbeatProducer, IdMappingJobService jobService, RdfStreamer idMappingRdfStreamer) {
        super(heartbeatProducer, jobService, idMappingRdfStreamer);
    }

    @Override
    protected String getDataType() {
        return ID_MAPPING_DATA_TYPE;
    }
}
