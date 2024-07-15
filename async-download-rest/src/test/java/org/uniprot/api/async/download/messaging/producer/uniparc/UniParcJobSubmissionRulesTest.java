package org.uniprot.api.async.download.messaging.producer.uniparc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.producer.JobSubmissionRulesTest;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;

@ExtendWith(MockitoExtension.class)
class UniParcJobSubmissionRulesTest
        extends JobSubmissionRulesTest<UniParcDownloadRequest, UniParcDownloadJob> {
    @Mock private UniParcJobService uniParcJobService;
    @Mock private UniParcDownloadJob uniParcDownloadJob;
    @Mock private UniParcDownloadRequest uniParcDownloadRequest;

    @BeforeEach
    void setUp() {
        jobService = uniParcJobService;
        downloadJob = uniParcDownloadJob;
        downloadRequest = uniParcDownloadRequest;
        jobSubmissionRules =
                new UniParcJobSubmissionRules(MAX_RETRY_COUNT, MAX_WAITING_TIME, uniParcJobService);
        mock();
    }
}
