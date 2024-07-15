package org.uniprot.api.async.download.service.uniparc;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.repository.UniParcDownloadJobRepository;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.service.JobServiceTest;

@ExtendWith(MockitoExtension.class)
public class UniParcJobServiceTest extends JobServiceTest<UniParcDownloadJob> {
    @Mock private UniParcDownloadJob uniParcDownloadJob;
    @Mock private UniParcDownloadJobRepository uniParcDownloadJobRepository;

    @BeforeEach
    void setUp() {
        downloadJob = uniParcDownloadJob;
        downloadJobOpt = Optional.of(downloadJob);
        downloadJobRepository = uniParcDownloadJobRepository;
        jobService = new UniParcJobService(uniParcDownloadJobRepository);
    }
}
