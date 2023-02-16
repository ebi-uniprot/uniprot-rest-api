package org.uniprot.api.statistics.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uniprot.api.statistics.entity.StatisticsCategory;

public interface StatisticsCategoryRepository extends JpaRepository<StatisticsCategory, Long> {
    Optional<StatisticsCategory> findByCategory(String category);
}
