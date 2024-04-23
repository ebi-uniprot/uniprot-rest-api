package org.uniprot.api.async.download.refactor.service.idmapping;

import org.springframework.stereotype.Service;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.refactor.service.JobService;

@Service
public class IdMappingJobService extends JobService<IdMappingDownloadJob> {
    public IdMappingJobService(DownloadJobRepository<IdMappingDownloadJob> downloadJobRepository) {
        super(downloadJobRepository);
    }
}
