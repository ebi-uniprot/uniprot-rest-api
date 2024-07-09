package org.uniprot.api.async.download.messaging.consumer.streamer.rdf.uniref;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniref.UniRefHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.RDFResultStreamerTest;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;

@ExtendWith(MockitoExtension.class)
class UniRefRDFResultStreamerTest
        extends RDFResultStreamerTest<UniRefDownloadRequest, UniRefDownloadJob> {
    @Mock protected UniRefDownloadRequest uniRefDownloadRequest;
    @Mock protected UniRefDownloadJob uniRefDownloadJob;
    @Mock protected UniRefHeartbeatProducer uniRefHeartbeatProducer;
    @Mock protected UniRefJobService uniRefJobService;

    @BeforeEach
    void setUp() {
        request = uniRefDownloadRequest;
        job = uniRefDownloadJob;
        heartbeatProducer = uniRefHeartbeatProducer;
        jobService = uniRefJobService;
        rdfResultStreamer =
                new UniRefRDFResultStreamer(uniRefHeartbeatProducer, uniRefJobService, rdfStreamer);
    }
}
