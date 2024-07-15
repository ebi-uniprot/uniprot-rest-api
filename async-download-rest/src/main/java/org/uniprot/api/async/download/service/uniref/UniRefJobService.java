package org.uniprot.api.async.download.service.uniref;

import org.springframework.stereotype.Service;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.model.job.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.service.JobService;

@Service
public class UniRefJobService extends JobService<UniRefDownloadJob> {
    public UniRefJobService(DownloadJobRepository<UniRefDownloadJob> downloadJobRepository) {
        super(downloadJobRepository);
    }
}
