package org.uniprot.api.async.download.messaging.repository;

import org.springframework.stereotype.Repository;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;

@Repository
public interface UniProtKBDownloadJobRepository
        extends DownloadJobRepository<UniProtKBDownloadJob>,
                UniProtKBDownloadJobPartialUpdateRepository {}
