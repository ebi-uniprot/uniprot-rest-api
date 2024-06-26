package org.uniprot.api.async.download.messaging.consumer.streamer.list.uniref;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniref.UniRefHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.ListResultStreamer;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;

@Component
public class UniRefListResultStreamer
        extends ListResultStreamer<UniRefDownloadRequest, UniRefDownloadJob> {

    public UniRefListResultStreamer(
            UniRefHeartbeatProducer heartbeatProducer, UniRefJobService jobService) {
        super(heartbeatProducer, jobService);
    }
}
