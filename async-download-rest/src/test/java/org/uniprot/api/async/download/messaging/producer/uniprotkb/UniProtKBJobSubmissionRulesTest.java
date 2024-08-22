package org.uniprot.api.async.download.messaging.producer.uniprotkb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.producer.JobSubmissionRulesTest;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.service.uniprotkb.UniProtKBJobService;

@ExtendWith(MockitoExtension.class)
class UniProtKBJobSubmissionRulesTest
        extends JobSubmissionRulesTest<UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    @Mock private UniProtKBJobService uniProtKBJobService;
    @Mock private UniProtKBDownloadJob uniProtKBDownloadJob;
    @Mock private UniProtKBDownloadRequest uniProtKBDownloadRequest;

    @BeforeEach
    void setUp() {
        jobService = uniProtKBJobService;
        downloadJob = uniProtKBDownloadJob;
        downloadRequest = uniProtKBDownloadRequest;
        jobSubmissionRules =
                new UniProtKBJobSubmissionRules(
                        MAX_RETRY_COUNT, MAX_WAITING_TIME, uniProtKBJobService);
        mock();
    }
}
