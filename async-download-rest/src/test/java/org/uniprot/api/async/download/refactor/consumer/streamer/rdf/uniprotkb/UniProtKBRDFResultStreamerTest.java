package org.uniprot.api.async.download.refactor.consumer.streamer.rdf.uniprotkb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.listener.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.rdf.RDFResultStreamerTest;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniprotkb.UniProtKBJobService;

@ExtendWith(MockitoExtension.class)
class UniProtKBRDFResultStreamerTest extends RDFResultStreamerTest<UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    @Mock
    protected UniProtKBDownloadRequest uniProtKBDownloadRequest;
    @Mock
    protected UniProtKBDownloadJob uniProtKBDownloadJob;
    @Mock
    protected UniProtKBHeartbeatProducer uniProtKBHeartbeatProducer;
    @Mock
    protected UniProtKBJobService uniProtKBJobService;

    @BeforeEach
    void setUp() {
        request = uniProtKBDownloadRequest;
        job = uniProtKBDownloadJob;
        heartbeatProducer = uniProtKBHeartbeatProducer;
        jobService = uniProtKBJobService;
        rdfResultStreamer = new UniProtKBRDFResultStreamer(uniProtKBHeartbeatProducer, uniProtKBJobService, rdfStreamer);
    }

}