package org.uniprot.api.async.download.messaging.producer.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.model.request.map.UniProtKBToUniRefMapDownloadRequest;

@ExtendWith(MockitoExtension.class)
class UniProtKBMapJobSubmissionRulesTest
        extends MapJobSubmissionRulesTest<UniProtKBToUniRefMapDownloadRequest> {
    @Mock private UniProtKBToUniRefMapDownloadRequest mapDownloadRequest;

    @BeforeEach
    void setUp() {
        init();
        downloadRequest = mapDownloadRequest;
        jobSubmissionRules =
                new UniProtKBMapJobSubmissionRules(
                        MAX_RETRY_COUNT, MAX_WAITING_TIME, mapJobService);
        mock();
    }
}
