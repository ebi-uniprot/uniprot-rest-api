package org.uniprot.api.mapto.common.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uniprot.api.mapto.common.model.MapToJob;

@Repository
public interface MapToJobRepository extends JpaRepository<MapToJob, Long> {
    Optional<MapToJob> findByJobId(String jobId);

    void deleteByJobId(String jobId);

    boolean existsByJobId(String jobId);
}
