package org.uniprot.api.async.download.refactor.service.uniprotkb;

import org.springframework.stereotype.Service;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.service.JobService;

@Service
public class UniProtKBJobService extends JobService<UniProtKBDownloadJob> {
    public UniProtKBJobService(DownloadJobRepository<UniProtKBDownloadJob> downloadJobRepository) {
        super(downloadJobRepository);
    }
}
