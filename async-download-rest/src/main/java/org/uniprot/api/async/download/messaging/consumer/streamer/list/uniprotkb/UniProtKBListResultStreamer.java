package org.uniprot.api.async.download.messaging.consumer.streamer.list.uniprotkb;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.ListResultStreamer;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.service.uniprotkb.UniProtKBJobService;

@Component
public class UniProtKBListResultStreamer
        extends ListResultStreamer<UniProtKBDownloadRequest, UniProtKBDownloadJob> {

    public UniProtKBListResultStreamer(
            UniProtKBHeartbeatProducer heartbeatProducer, UniProtKBJobService jobService) {
        super(heartbeatProducer, jobService);
    }
}
