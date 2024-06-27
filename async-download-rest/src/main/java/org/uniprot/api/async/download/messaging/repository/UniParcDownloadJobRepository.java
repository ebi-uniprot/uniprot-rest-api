package org.uniprot.api.async.download.messaging.repository;

import org.springframework.stereotype.Repository;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;

@Repository
public interface UniParcDownloadJobRepository
        extends DownloadJobRepository<UniParcDownloadJob>,
                UniParcDownloadJobPartialUpdateRepository {}
