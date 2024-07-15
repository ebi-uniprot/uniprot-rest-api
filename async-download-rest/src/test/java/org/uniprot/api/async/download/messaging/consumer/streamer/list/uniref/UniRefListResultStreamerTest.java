package org.uniprot.api.async.download.messaging.consumer.streamer.list.uniref;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniref.UniRefHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.ListResultStreamerTest;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;

@ExtendWith(MockitoExtension.class)
class UniRefListResultStreamerTest
        extends ListResultStreamerTest<UniRefDownloadRequest, UniRefDownloadJob> {
    @Mock private UniRefDownloadRequest uniRefDownloadRequest;
    @Mock private UniRefDownloadJob uniRefDownloadJob;
    @Mock private UniRefHeartbeatProducer uniRefHeartbeatProducer;
    @Mock private UniRefJobService uniRefJobService;

    @BeforeEach
    void setUp() {
        request = uniRefDownloadRequest;
        job = uniRefDownloadJob;
        heartbeatProducer = uniRefHeartbeatProducer;
        jobService = uniRefJobService;
        listResultStreamer =
                new UniRefListResultStreamer(uniRefHeartbeatProducer, uniRefJobService);
    }
}
