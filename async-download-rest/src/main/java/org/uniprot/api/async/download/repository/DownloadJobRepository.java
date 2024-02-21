package org.uniprot.api.async.download.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.uniprot.api.rest.download.model.DownloadJob;

/**
 * @author sahmad
 * @created 22/12/2022
 */
@Repository
@Profile({"asyncDownload"})
public interface DownloadJobRepository
        extends CrudRepository<DownloadJob, String>, DownloadJobPartialUpdateRepository {}
