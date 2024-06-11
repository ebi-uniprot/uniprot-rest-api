package org.uniprot.api.async.download.messaging.consumer.streamer.rdf.idmapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.RDFResultStreamerTest;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;

@ExtendWith(MockitoExtension.class)
class UnProtKBIdMappingRDFResultStreamerTest
        extends RDFResultStreamerTest<IdMappingDownloadRequest, IdMappingDownloadJob> {
    @Mock protected IdMappingDownloadRequest idMappingDownloadRequest;
    @Mock protected IdMappingDownloadJob idMappingDownloadJob;
    @Mock protected IdMappingHeartbeatProducer idMappingHeartbeatProducer;
    @Mock protected IdMappingJobService idMappingJobService;

    @BeforeEach
    void setUp() {
        request = idMappingDownloadRequest;
        job = idMappingDownloadJob;
        heartbeatProducer = idMappingHeartbeatProducer;
        jobService = idMappingJobService;
        rdfResultStreamer =
                new UniProtKBIdMappingRDFResultStreamer(
                        idMappingHeartbeatProducer, idMappingJobService, rdfStreamer);
    }
}
