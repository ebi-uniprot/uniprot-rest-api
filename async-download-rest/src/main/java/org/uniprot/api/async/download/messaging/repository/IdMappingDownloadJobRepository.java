package org.uniprot.api.async.download.messaging.repository;

import org.springframework.stereotype.Repository;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;

@Repository
public interface IdMappingDownloadJobRepository
        extends DownloadJobRepository<IdMappingDownloadJob>,
                IdMappingDownloadJobPartialUpdateRepository {}
