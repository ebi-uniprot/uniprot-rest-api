package org.uniprot.api.async.download.messaging.consumer.streamer.list.uniprotkb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.ListResultStreamerTest;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.service.uniprotkb.UniProtKBJobService;

@ExtendWith(MockitoExtension.class)
class UniProtKBListResultStreamerTest
        extends ListResultStreamerTest<UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    @Mock private UniProtKBDownloadRequest uniProtKBRequest;
    @Mock private UniProtKBDownloadJob uniProtKBJob;
    @Mock private UniProtKBHeartbeatProducer uniProtKBHeartbeatProducer;
    @Mock private UniProtKBJobService uniProtKBJobService;

    @BeforeEach
    void setUp() {
        request = uniProtKBRequest;
        job = uniProtKBJob;
        heartbeatProducer = uniProtKBHeartbeatProducer;
        jobService = uniProtKBJobService;
        listResultStreamer =
                new UniProtKBListResultStreamer(uniProtKBHeartbeatProducer, uniProtKBJobService);
    }
}
