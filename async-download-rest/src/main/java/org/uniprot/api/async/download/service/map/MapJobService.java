package org.uniprot.api.async.download.service.map;

import org.springframework.stereotype.Service;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.service.JobService;

@Service
public class MapJobService extends JobService<MapDownloadJob> {
    public MapJobService(DownloadJobRepository<MapDownloadJob> downloadJobRepository) {
        super(downloadJobRepository);
    }
}
