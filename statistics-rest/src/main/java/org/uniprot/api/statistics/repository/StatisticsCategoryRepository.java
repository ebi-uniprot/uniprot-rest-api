package org.uniprot.api.statistics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uniprot.api.statistics.entity.StatisticsCategory;

import java.util.Optional;

public interface StatisticsCategoryRepository extends JpaRepository<StatisticsCategory, Long> {
    Optional<StatisticsCategory> findByCategoryIgnoreCase(String category);
}
