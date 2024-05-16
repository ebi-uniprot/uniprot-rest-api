package org.uniprot.api.async.download.refactor.consumer.streamer.list.uniref;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.listener.uniref.UniRefHeartbeatProducer;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.list.ListResultStreamer;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniref.UniRefJobService;

@Component
public class UniRefListResultStreamer
        extends ListResultStreamer<UniRefDownloadRequest, UniRefDownloadJob> {

    public UniRefListResultStreamer(
            UniRefHeartbeatProducer heartbeatProducer, UniRefJobService jobService) {
        super(heartbeatProducer, jobService);
    }
}
