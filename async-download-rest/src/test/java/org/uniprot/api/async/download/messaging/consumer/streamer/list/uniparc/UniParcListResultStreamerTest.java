package org.uniprot.api.async.download.messaging.consumer.streamer.list.uniparc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniparc.UniParcHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.ListResultStreamerTest;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;

@ExtendWith(MockitoExtension.class)
class UniParcListResultStreamerTest
        extends ListResultStreamerTest<UniParcDownloadRequest, UniParcDownloadJob> {
    @Mock private UniParcDownloadRequest uniParcDownloadRequest;
    @Mock private UniParcDownloadJob uniParcDownloadJob;
    @Mock private UniParcHeartbeatProducer uniParcHeartbeatProducer;
    @Mock private UniParcJobService uniParcJobService;

    @BeforeEach
    void setUp() {
        request = uniParcDownloadRequest;
        job = uniParcDownloadJob;
        heartbeatProducer = uniParcHeartbeatProducer;
        jobService = uniParcJobService;
        listResultStreamer =
                new UniParcListResultStreamer(uniParcHeartbeatProducer, uniParcJobService);
    }
}
