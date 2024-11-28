package org.uniprot.api.async.download.messaging.producer.mapto;

import org.mockito.Mock;
import org.uniprot.api.async.download.messaging.producer.JobSubmissionRulesTest;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;

public abstract class MapToJobSubmissionRulesTest<T extends MapToDownloadRequest>
        extends JobSubmissionRulesTest<T, MapToDownloadJob> {
    @Mock protected MapToJobService mapToJobService;
    @Mock protected MapToDownloadJob mapToDownloadJob;

    protected void init() {
        jobService = mapToJobService;
        downloadJob = mapToDownloadJob;
    }
}
