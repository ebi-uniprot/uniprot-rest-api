package org.uniprot.api.async.download.messaging.producer.map;

import org.mockito.Mock;
import org.uniprot.api.async.download.messaging.producer.JobSubmissionRulesTest;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.async.download.service.map.MapJobService;

public abstract class MapJobSubmissionRulesTest<T extends MapDownloadRequest>
        extends JobSubmissionRulesTest<T, MapDownloadJob> {
    @Mock protected MapJobService mapJobService;
    @Mock protected MapDownloadJob mapDownloadJob;

    protected void init() {
        jobService = mapJobService;
        downloadJob = mapDownloadJob;
    }
}
