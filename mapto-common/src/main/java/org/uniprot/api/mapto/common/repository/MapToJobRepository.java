package org.uniprot.api.mapto.common.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.uniprot.api.mapto.common.model.MapToJob;

@Repository
public interface MapToJobRepository extends CrudRepository<MapToJob, String> {}
