package org.uniprot.api.support.data.statistics.repository;

import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uniprot.api.support.data.statistics.entity.StatisticsCategory;

import java.util.Optional;

@Repository
@Primary
public interface StatisticsCategoryRepository extends JpaRepository<StatisticsCategory, Long> {
    Optional<StatisticsCategory> findByCategoryIgnoreCase(String category);
}
