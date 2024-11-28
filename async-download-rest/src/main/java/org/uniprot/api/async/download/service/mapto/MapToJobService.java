package org.uniprot.api.async.download.service.mapto;

import org.springframework.stereotype.Service;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.service.JobService;

@Service
public class MapToJobService extends JobService<MapToDownloadJob> {
    public MapToJobService(DownloadJobRepository<MapToDownloadJob> downloadJobRepository) {
        super(downloadJobRepository);
    }
}
