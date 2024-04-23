package org.uniprot.api.async.download.refactor.consumer.streamer.list.uniprotkb;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.listener.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniprotkb.UniProtKBJobService;
import org.uniprot.api.async.download.refactor.consumer.streamer.list.ListResultStreamer;

@Component
public class UniProtKBListResultStreamer extends ListResultStreamer<UniProtKBDownloadRequest, UniProtKBDownloadJob> {

    public UniProtKBListResultStreamer(UniProtKBHeartbeatProducer heartbeatProducer, UniProtKBJobService jobService) {
        super(heartbeatProducer, jobService);
    }
}
