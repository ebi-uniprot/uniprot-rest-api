package org.uniprot.api.support.data.statistics.repository;

import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uniprot.api.support.data.statistics.entity.AttributeQuery;
import org.uniprot.api.support.data.statistics.entity.StatisticsCategory;

@Repository
@Primary
public interface AttributeQueryRepository extends JpaRepository<AttributeQuery, Long> {
    Optional<AttributeQuery> findByStatisticsCategoryAndAttributeNameIgnoreCase(
            StatisticsCategory statisticsCategory, String attributeName);
}
