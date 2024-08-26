package org.uniprot.api.async.download.service.map;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.repository.MapDownloadJobRepository;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.service.JobServiceTest;

@ExtendWith(MockitoExtension.class)
public class MapJobServiceTest extends JobServiceTest<MapDownloadJob> {
    @Mock private MapDownloadJob mapDownloadJob;
    @Mock private MapDownloadJobRepository mapDownloadJobRepository;

    @BeforeEach
    void setUp() {
        downloadJob = mapDownloadJob;
        downloadJobOpt = Optional.of(downloadJob);
        downloadJobRepository = mapDownloadJobRepository;
        jobService = new MapJobService(mapDownloadJobRepository);
    }
}
