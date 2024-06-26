package org.uniprot.api.async.download.service.idmapping;

import org.springframework.stereotype.Service;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.service.JobService;

@Service
public class IdMappingJobService extends JobService<IdMappingDownloadJob> {
    public IdMappingJobService(DownloadJobRepository<IdMappingDownloadJob> downloadJobRepository) {
        super(downloadJobRepository);
    }
}
