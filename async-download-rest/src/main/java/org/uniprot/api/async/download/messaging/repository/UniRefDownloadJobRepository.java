package org.uniprot.api.async.download.messaging.repository;

import org.springframework.stereotype.Repository;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;

@Repository
public interface UniRefDownloadJobRepository
        extends DownloadJobRepository<UniRefDownloadJob>,
                UniRefDownloadJobPartialUpdateRepository {}
