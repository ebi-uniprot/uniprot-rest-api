package org.uniprot.api.async.download.messaging.consumer.streamer.list.idmapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.ListResultStreamerTest;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;

@ExtendWith(MockitoExtension.class)
class IdMappingListResultStreamerTest
        extends ListResultStreamerTest<IdMappingDownloadRequest, IdMappingDownloadJob> {
    @Mock private IdMappingDownloadRequest idMappingDownloadRequest;
    @Mock private IdMappingDownloadJob idMappingDownloadJob;
    @Mock private IdMappingHeartbeatProducer idMappingHeartbeatProducer;
    @Mock private IdMappingJobService idMappingJobService;

    @BeforeEach
    void setUp() {
        request = idMappingDownloadRequest;
        job = idMappingDownloadJob;
        heartbeatProducer = idMappingHeartbeatProducer;
        jobService = idMappingJobService;
        listResultStreamer =
                new IdMappingListResultStreamer(idMappingHeartbeatProducer, idMappingJobService);
    }
}
