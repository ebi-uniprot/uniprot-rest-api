package org.uniprot.api.mapto.common.repository;

import org.springframework.data.repository.CrudRepository;
import org.uniprot.api.mapto.common.model.MapToJob;

public interface MapToJobRepository extends CrudRepository<MapToJob, String> {}
