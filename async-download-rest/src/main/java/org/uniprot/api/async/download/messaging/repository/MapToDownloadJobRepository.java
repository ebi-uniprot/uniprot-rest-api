package org.uniprot.api.async.download.messaging.repository;

import org.springframework.stereotype.Repository;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;

@Repository
public interface MapToDownloadJobRepository
        extends DownloadJobRepository<MapToDownloadJob>, MapToDownloadJobPartialUpdateRepository {}
