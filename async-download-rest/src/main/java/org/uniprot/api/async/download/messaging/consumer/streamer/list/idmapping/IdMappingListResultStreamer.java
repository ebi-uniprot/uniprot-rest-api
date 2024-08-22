package org.uniprot.api.async.download.messaging.consumer.streamer.list.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.ListResultStreamer;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;

@Component
public class IdMappingListResultStreamer
        extends ListResultStreamer<IdMappingDownloadRequest, IdMappingDownloadJob> {

    public IdMappingListResultStreamer(
            IdMappingHeartbeatProducer heartbeatProducer, IdMappingJobService jobService) {
        super(heartbeatProducer, jobService);
    }
}
