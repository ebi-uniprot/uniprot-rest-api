package org.uniprot.api.support.data.statistics.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uniprot.api.support.data.statistics.entity.EntryType;
import org.uniprot.api.support.data.statistics.entity.StatisticsCategory;
import org.uniprot.api.support.data.statistics.entity.UniprotKBStatisticsEntry;

@Repository
@Primary
public interface UniprotkbStatisticsEntryRepository
        extends JpaRepository<UniprotKBStatisticsEntry, Long> {
    List<UniprotKBStatisticsEntry> findAllByReleaseNameAndEntryType(
            String releaseName, EntryType entryType);

    List<UniprotKBStatisticsEntry> findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIn(
            String releaseName,
            EntryType entryType,
            Collection<StatisticsCategory> statisticsCategory);
}
