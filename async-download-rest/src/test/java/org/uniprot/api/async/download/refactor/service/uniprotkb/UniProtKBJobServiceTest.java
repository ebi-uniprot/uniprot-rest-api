package org.uniprot.api.async.download.refactor.service.uniprotkb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.repository.UniProtKBDownloadJobRepository;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.service.JobServiceTest;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class UniProtKBJobServiceTest extends JobServiceTest<UniProtKBDownloadJob> {
    @Mock
    private UniProtKBDownloadJob uniProtKBDownloadJob;
    @Mock private UniProtKBDownloadJobRepository uniProtKBDownloadJobRepository;

    @BeforeEach
    void setUp() {
        downloadJob = uniProtKBDownloadJob;
        downloadJobOpt = Optional.of(downloadJob);
        downloadJobRepository = uniProtKBDownloadJobRepository;
        jobService = new UniProtKBJobService(uniProtKBDownloadJobRepository);
    }
}