package org.uniprot.api.statistics.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.uniprot.api.statistics.entity.EntryType.SWISSPROT;
import static org.uniprot.api.statistics.repository.EntityGeneratorUtil.STATISTICS_CATEGORIES;
import static org.uniprot.api.statistics.repository.EntityGeneratorUtil.STATISTICS_ENTRIES;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class UniprotkbStatisticsEntryRepositoryIT {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UniprotkbStatisticsEntryRepository entryRepository;

    @BeforeEach
    void setUp() {
        Arrays.stream(STATISTICS_CATEGORIES).forEach(entityManager::persist);
        Arrays.stream(STATISTICS_ENTRIES).forEach(entityManager::persist);
    }

    @Test
    void findAllByReleaseNameAndEntryType() {
        List<UniprotkbStatisticsEntry> results = entryRepository.findAllByReleaseNameAndEntryType("rel0", SWISSPROT);

        assertThat(results, containsInAnyOrder(STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[1], STATISTICS_ENTRIES[3]));
    }

    @Test
    void findAllByReleaseNameAndEntryType_whenNoMatch() {
        List<UniprotkbStatisticsEntry> results = entryRepository.findAllByReleaseNameAndEntryType("rel1", SWISSPROT);

        assertThat(results, empty());
    }

    @Test
    void findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIdIn() {
        List<UniprotkbStatisticsEntry> results = entryRepository.findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIdIn("rel0", SWISSPROT,
                Arrays.asList(STATISTICS_CATEGORIES));

        assertThat(results, containsInAnyOrder(STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[1], STATISTICS_ENTRIES[3]));
    }

    @Test
    void findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIdIn_whenSingleCategoryPassed() {
        List<UniprotkbStatisticsEntry> results = entryRepository.findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIdIn("rel0", SWISSPROT,
                Collections.singletonList(STATISTICS_CATEGORIES[0]));

        assertThat(results, containsInAnyOrder(STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[1]));
    }

    @Test
    void findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIdIn_whenNoCategoryPassed() {
        List<UniprotkbStatisticsEntry> results = entryRepository.findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIdIn("rel0", SWISSPROT,
                Collections.emptyList());

        assertThat(results, empty());
    }
}
