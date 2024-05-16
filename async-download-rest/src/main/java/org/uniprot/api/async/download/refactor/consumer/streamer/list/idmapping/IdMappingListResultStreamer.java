package org.uniprot.api.async.download.refactor.consumer.streamer.list.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.listener.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.list.ListResultStreamer;
import org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.refactor.service.idmapping.IdMappingJobService;

@Component
public class IdMappingListResultStreamer extends ListResultStreamer<IdMappingDownloadRequest, IdMappingDownloadJob> {

    public IdMappingListResultStreamer(IdMappingHeartbeatProducer heartbeatProducer, IdMappingJobService jobService) {
        super(heartbeatProducer, jobService);
    }
}
