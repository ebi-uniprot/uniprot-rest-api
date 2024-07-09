package org.uniprot.api.async.download.messaging.consumer.streamer.rdf.uniparc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniparc.UniParcHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.RDFResultStreamerTest;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;

@ExtendWith(MockitoExtension.class)
class UniParcRDFResultStreamerTest
        extends RDFResultStreamerTest<UniParcDownloadRequest, UniParcDownloadJob> {
    @Mock protected UniParcDownloadRequest uniParcDownloadRequest;
    @Mock protected UniParcDownloadJob uniParcDownloadJob;
    @Mock protected UniParcHeartbeatProducer uniParcHeartbeatProducer;
    @Mock protected UniParcJobService uniParcJobService;

    @BeforeEach
    void setUp() {
        request = uniParcDownloadRequest;
        job = uniParcDownloadJob;
        heartbeatProducer = uniParcHeartbeatProducer;
        jobService = uniParcJobService;
        rdfResultStreamer =
                new UniParcRDFResultStreamer(
                        uniParcHeartbeatProducer, uniParcJobService, rdfStreamer);
    }
}
