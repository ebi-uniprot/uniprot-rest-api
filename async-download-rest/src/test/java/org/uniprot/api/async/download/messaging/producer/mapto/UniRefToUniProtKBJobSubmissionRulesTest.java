package org.uniprot.api.async.download.messaging.producer.mapto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.model.request.mapto.UniRefToUniProtKBDownloadRequest;

@ExtendWith(MockitoExtension.class)
class UniRefToUniProtKBJobSubmissionRulesTest
        extends MapToJobSubmissionRulesTest<UniRefToUniProtKBDownloadRequest> {
    @Mock private UniRefToUniProtKBDownloadRequest mapDownloadRequest;

    @BeforeEach
    void setUp() {
        init();
        downloadRequest = mapDownloadRequest;
        jobSubmissionRules =
                new UniRefToUniProtKBJobSubmissionRules(
                        MAX_RETRY_COUNT, MAX_WAITING_TIME, mapToJobService);
        mock();
    }
}
