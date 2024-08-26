package org.uniprot.api.async.download.messaging.consumer.streamer.list.map;

import org.uniprot.api.async.download.messaging.consumer.heartbeat.map.MapHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.ListResultStreamer;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.async.download.service.map.MapJobService;

public abstract class UniRefMapListResultStreamer<T extends MapDownloadRequest>
        extends ListResultStreamer<T, MapDownloadJob> {

    protected UniRefMapListResultStreamer(
            MapHeartbeatProducer heartbeatProducer, MapJobService jobService) {
        super(heartbeatProducer, jobService);
    }
}
