package org.uniprot.api.async.download.service.uniprotkb;

import org.springframework.stereotype.Service;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.service.JobService;

@Service
public class UniProtKBJobService extends JobService<UniProtKBDownloadJob> {
    public UniProtKBJobService(DownloadJobRepository<UniProtKBDownloadJob> downloadJobRepository) {
        super(downloadJobRepository);
    }
}
