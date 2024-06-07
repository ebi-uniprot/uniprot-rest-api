package org.uniprot.api.async.download.messaging.producer.uniref;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.producer.common.AsyncDownloadSubmissionRulesTest;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.refactor.service.uniref.UniRefJobService;

@ExtendWith(MockitoExtension.class)
class UniRefAsyncDownloadSubmissionRulesTest extends AsyncDownloadSubmissionRulesTest<UniRefDownloadRequest, UniRefDownloadJob> {
    @Mock
    private UniRefJobService uniRefJobService;
    @Mock
    private UniRefDownloadJob uniRefDownloadJob;
    @Mock
    private UniRefDownloadRequest uniRefDownloadRequest;

    @BeforeEach
    void setUp() {
        jobService = uniRefJobService;
        downloadJob = uniRefDownloadJob;
        downloadRequest = uniRefDownloadRequest;
        asyncDownloadSubmissionRules =
                new UniRefAsyncDownloadSubmissionRules(
                        MAX_RETRY_COUNT, MAX_WAITING_TIME, uniRefJobService);
        mock();
    }
}
