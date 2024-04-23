package org.uniprot.api.async.download.refactor.consumer.streamer.rdf.uniprotkb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.async.download.messaging.listener.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.rdf.RDFResultStreamerTest;
import org.uniprot.api.async.download.refactor.consumer.streamer.rdf.uniprotkb.UniProtKBRDFResultStreamer;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.refactor.service.JobService;
import org.uniprot.api.async.download.refactor.service.uniprotkb.UniProtKBJobService;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;

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

    @Override
    protected String getDataType() {
        return "uniprotkb";
    }
}
