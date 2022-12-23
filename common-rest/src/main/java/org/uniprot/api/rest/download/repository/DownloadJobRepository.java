package org.uniprot.api.rest.download.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.uniprot.api.rest.download.model.DownloadJob;

/**
 * @author sahmad
 * @created 22/12/2022
 */
@Repository
public interface DownloadJobRepository extends CrudRepository<DownloadJob, String> {}
