package org.uniprot.api.support.data.statistics.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uniprot.api.support.data.statistics.entity.EntryType;
import org.uniprot.api.support.data.statistics.entity.StatisticsCategory;
import org.uniprot.api.support.data.statistics.entity.UniProtKBStatisticsEntry;
import org.uniprot.api.support.data.statistics.entity.UniProtRelease;

@Repository
@Primary
public interface UniProtKBStatisticsEntryRepository
        extends JpaRepository<UniProtKBStatisticsEntry, Long> {
    List<UniProtKBStatisticsEntry> findAllByAttributeNameIgnoreCaseAndEntryType(
            String attributeName, EntryType entryType);

    List<UniProtKBStatisticsEntry> findAllByAttributeNameIgnoreCase(String attributeName);

    List<UniProtKBStatisticsEntry> findAllByUniprotRelease(UniProtRelease uniProtRelease);

    List<UniProtKBStatisticsEntry> findAllByUniprotReleaseAndStatisticsCategoryIn(
            UniProtRelease releaseName, Collection<StatisticsCategory> statisticsCategory);
}
