package org.uniprot.api.statistics.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uniprot.api.statistics.entity.EntryType;
import org.uniprot.api.statistics.entity.StatisticsCategory;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;

@Repository
public interface UniprotkbStatisticsEntryRepository
        extends JpaRepository<UniprotkbStatisticsEntry, Long> {
    List<UniprotkbStatisticsEntry> findAllByReleaseNameAndEntryType(
            String releaseName, EntryType entryType);

    List<UniprotkbStatisticsEntry> findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIdIn(
            String releaseName,
            EntryType entryType,
            Collection<StatisticsCategory> statisticsCategory);
}
