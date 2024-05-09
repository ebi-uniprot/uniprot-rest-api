package org.uniprot.api.async.download.messaging.repository;

import org.springframework.stereotype.Repository;
import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadJob;

@Repository
public interface IdMappingDownloadJobRepository
        extends DownloadJobRepository<IdMappingDownloadJob>,
                IdMappingDownloadJobPartialUpdateRepository {}
