package org.uniprot.api.async.download.service.uniparc;

import org.springframework.stereotype.Service;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.service.JobService;

@Service
public class UniParcJobService extends JobService<UniParcDownloadJob> {
    public UniParcJobService(DownloadJobRepository<UniParcDownloadJob> downloadJobRepository) {
        super(downloadJobRepository);
    }
}
