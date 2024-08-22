package org.uniprot.api.async.download.messaging.consumer.streamer.rdf.uniprotkb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.RDFResultStreamerTest;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.service.uniprotkb.UniProtKBJobService;

@ExtendWith(MockitoExtension.class)
class UniProtKBRDFResultStreamerTest
        extends RDFResultStreamerTest<UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    @Mock protected UniProtKBDownloadRequest uniProtKBDownloadRequest;
    @Mock protected UniProtKBDownloadJob uniProtKBDownloadJob;
    @Mock protected UniProtKBHeartbeatProducer uniProtKBHeartbeatProducer;
    @Mock protected UniProtKBJobService uniProtKBJobService;

    @BeforeEach
    void setUp() {
        request = uniProtKBDownloadRequest;
        job = uniProtKBDownloadJob;
        heartbeatProducer = uniProtKBHeartbeatProducer;
        jobService = uniProtKBJobService;
        rdfResultStreamer =
                new UniProtKBRDFResultStreamer(
                        uniProtKBHeartbeatProducer, uniProtKBJobService, rdfStreamer);
    }
}
