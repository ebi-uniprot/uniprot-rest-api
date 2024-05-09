package org.uniprot.api.async.download.messaging.repository;

import org.springframework.data.repository.CrudRepository;
import org.uniprot.api.async.download.model.common.DownloadJob;

/**
 * @author sahmad
 * @created 22/12/2022
 */
public interface DownloadJobRepository<T extends DownloadJob>
        extends CrudRepository<T, String>, DownloadJobPartialUpdateRepository {}
