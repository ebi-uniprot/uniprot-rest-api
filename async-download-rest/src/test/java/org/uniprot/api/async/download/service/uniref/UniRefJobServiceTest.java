package org.uniprot.api.async.download.service.uniref;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.repository.UniRefDownloadJobRepository;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.service.JobServiceTest;

@ExtendWith(MockitoExtension.class)
public class UniRefJobServiceTest extends JobServiceTest<UniRefDownloadJob> {
    @Mock private UniRefDownloadJob uniRefDownloadJob;
    @Mock private UniRefDownloadJobRepository uniRefDownloadJobRepository;

    @BeforeEach
    void setUp() {
        downloadJob = uniRefDownloadJob;
        downloadJobOpt = Optional.of(downloadJob);
        downloadJobRepository = uniRefDownloadJobRepository;
        jobService = new UniRefJobService(uniRefDownloadJobRepository);
    }
}
