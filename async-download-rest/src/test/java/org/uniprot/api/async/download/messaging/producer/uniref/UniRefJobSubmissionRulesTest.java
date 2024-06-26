package org.uniprot.api.async.download.messaging.producer.uniref;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.producer.JobSubmissionRulesTest;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;

@ExtendWith(MockitoExtension.class)
class UniRefJobSubmissionRulesTest
        extends JobSubmissionRulesTest<UniRefDownloadRequest, UniRefDownloadJob> {
    @Mock private UniRefJobService uniRefJobService;
    @Mock private UniRefDownloadJob uniRefDownloadJob;
    @Mock private UniRefDownloadRequest uniRefDownloadRequest;

    @BeforeEach
    void setUp() {
        jobService = uniRefJobService;
        downloadJob = uniRefDownloadJob;
        downloadRequest = uniRefDownloadRequest;
        jobSubmissionRules =
                new UniRefJobSubmissionRules(MAX_RETRY_COUNT, MAX_WAITING_TIME, uniRefJobService);
        mock();
    }
}
