package org.uniprot.api.async.download.messaging.consumer.streamer.list.mapto;

import org.uniprot.api.async.download.messaging.consumer.heartbeat.mapto.MapToHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.ListResultStreamer;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;

public abstract class UniRefMapToListResultStreamer<T extends MapToDownloadRequest>
        extends ListResultStreamer<T, MapToDownloadJob> {

    protected UniRefMapToListResultStreamer(
            MapToHeartbeatProducer heartbeatProducer, MapToJobService jobService) {
        super(heartbeatProducer, jobService);
    }
}
