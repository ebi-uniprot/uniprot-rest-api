package org.uniprot.api.mapto.common.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.model.MapToResult;

@Repository
public interface MapToResultRepository extends PagingAndSortingRepository<MapToResult, Long> {

    @Query("SELECT mTR.targetId FROM MapToResult mTR  WHERE mTR.mapToJob = :mapToJob")
    List<String> findTargetIdByMapToJob(MapToJob mapToJob, Pageable pageable);

    @Query("SELECT mTR.targetId FROM MapToResult mTR  WHERE mTR.mapToJob = :mapToJob")
    List<String> findTargetIdByMapToJob(MapToJob mapToJob);
}
