package org.uniprot.api.statistics.repository;

import java.util.List;
import java.util.Optional;

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

    List<UniprotkbStatisticsEntry> findAllByReleaseNameAndEntryTypeAndStatisticsCategoryId(
            String releaseName, EntryType entryType, StatisticsCategory statisticsCategory);

    Optional<UniprotkbStatisticsEntry>
            findByReleaseNameAndEntryTypeAndStatisticsCategoryIdAndAttributeName(
                    String releaseName,
                    EntryType entryType,
                    StatisticsCategory statisticsCategory,
                    String attributeName);
}
