package org.uniprot.api.statistics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uniprot.api.statistics.entity.EntryType;
import org.uniprot.api.statistics.entity.StatisticsCategory;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;

import java.util.Collection;
import java.util.List;

@Repository
public interface UniprotkbStatisticsEntryRepository
        extends JpaRepository<UniprotkbStatisticsEntry, Long> {
    List<UniprotkbStatisticsEntry> findAllByReleaseNameAndEntryType(
            String releaseName, EntryType entryType);

    List<UniprotkbStatisticsEntry> findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIn(
            String releaseName,
            EntryType entryType,
            Collection<StatisticsCategory> statisticsCategory);
}
