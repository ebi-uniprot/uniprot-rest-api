package org.uniprot.api.async.download.messaging.consumer.streamer.list.uniparc;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniparc.UniParcHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.ListResultStreamer;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;

@Component
public class UniParcListResultStreamer
        extends ListResultStreamer<UniParcDownloadRequest, UniParcDownloadJob> {

    public UniParcListResultStreamer(
            UniParcHeartbeatProducer heartbeatProducer, UniParcJobService jobService) {
        super(heartbeatProducer, jobService);
    }
}
