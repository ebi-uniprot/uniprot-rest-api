package org.uniprot.api.async.download.service.mapto;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.repository.MapToDownloadJobRepository;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.service.JobServiceTest;

@ExtendWith(MockitoExtension.class)
public class MapToJobServiceTest extends JobServiceTest<MapToDownloadJob> {
    @Mock private MapToDownloadJob mapToDownloadJob;
    @Mock private MapToDownloadJobRepository mapDownloadJobRepository;

    @BeforeEach
    void setUp() {
        downloadJob = mapToDownloadJob;
        downloadJobOpt = Optional.of(downloadJob);
        downloadJobRepository = mapDownloadJobRepository;
        jobService = new MapToJobService(mapDownloadJobRepository);
    }
}
