package org.uniprot.api.async.download.service.idmapping;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.repository.IdMappingDownloadJobRepository;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.service.JobServiceTest;

@ExtendWith(MockitoExtension.class)
class IdMappingJobServiceTest extends JobServiceTest<IdMappingDownloadJob> {
    @Mock private IdMappingDownloadJob idMappingDownloadJob;
    @Mock private IdMappingDownloadJobRepository idMappingDownloadJobRepository;

    @BeforeEach
    void setUp() {
        downloadJob = idMappingDownloadJob;
        downloadJobOpt = Optional.of(downloadJob);
        downloadJobRepository = idMappingDownloadJobRepository;
        jobService = new IdMappingJobService(idMappingDownloadJobRepository);
    }
}
