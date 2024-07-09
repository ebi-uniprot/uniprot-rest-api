package org.uniprot.api.async.download.messaging.repository;

import org.springframework.data.repository.CrudRepository;
import org.uniprot.api.async.download.model.job.DownloadJob;

/**
 * @author sahmad
 * @created 22/12/2022
 */
public interface DownloadJobRepository<R extends DownloadJob>
        extends CrudRepository<R, String>, DownloadJobPartialUpdateRepository {}
