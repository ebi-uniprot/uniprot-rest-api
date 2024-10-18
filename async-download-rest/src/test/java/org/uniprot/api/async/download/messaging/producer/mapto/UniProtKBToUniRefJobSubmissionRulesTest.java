package org.uniprot.api.async.download.messaging.producer.mapto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;

@ExtendWith(MockitoExtension.class)
class UniProtKBToUniRefJobSubmissionRulesTest
        extends MapToJobSubmissionRulesTest<UniProtKBToUniRefDownloadRequest> {
    @Mock private UniProtKBToUniRefDownloadRequest mapDownloadRequest;

    @BeforeEach
    void setUp() {
        init();
        downloadRequest = mapDownloadRequest;
        jobSubmissionRules =
                new UniProtKBToUniRefJobSubmissionRules(
                        MAX_RETRY_COUNT, MAX_WAITING_TIME, mapToJobService);
        mock();
    }
}
