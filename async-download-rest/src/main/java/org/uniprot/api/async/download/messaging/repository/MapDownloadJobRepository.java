package org.uniprot.api.async.download.messaging.repository;

import org.springframework.stereotype.Repository;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;

@Repository
public interface MapDownloadJobRepository
        extends DownloadJobRepository<MapDownloadJob>, MapDownloadJobPartialUpdateRepository {}
