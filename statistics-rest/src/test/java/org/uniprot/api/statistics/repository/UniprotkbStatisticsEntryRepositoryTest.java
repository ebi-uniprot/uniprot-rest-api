package org.uniprot.api.statistics.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.uniprot.api.statistics.TestEntityGeneratorUtil.STATISTICS_CATEGORIES;
import static org.uniprot.api.statistics.TestEntityGeneratorUtil.STATISTICS_ENTRIES;
import static org.uniprot.api.statistics.entity.EntryType.SWISSPROT;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@Import({HttpCommonHeaderConfig.class, RequestMappingHandlerMapping.class})
class UniprotkbStatisticsEntryRepositoryTest {
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
        List<UniprotkbStatisticsEntry> results =
                entryRepository.findAllByReleaseNameAndEntryType("rel0", SWISSPROT);

        assertThat(
                results,
                containsInAnyOrder(
                        STATISTICS_ENTRIES[0],
                        STATISTICS_ENTRIES[1],
                        STATISTICS_ENTRIES[3],
                        STATISTICS_ENTRIES[4]));
    }

    @Test
    void findAllByReleaseNameAndEntryType_whenNoMatch() {
        List<UniprotkbStatisticsEntry> results =
                entryRepository.findAllByReleaseNameAndEntryType("rel1", SWISSPROT);

        assertThat(results, empty());
    }

    @Test
    void findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIdIn() {
        List<UniprotkbStatisticsEntry> results =
                entryRepository.findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIn(
                        "rel0",
                        SWISSPROT,
                        List.of(STATISTICS_CATEGORIES[0], STATISTICS_CATEGORIES[1]));

        assertThat(
                results,
                containsInAnyOrder(
                        STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[1], STATISTICS_ENTRIES[3]));
    }

    @Test
    void findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIdIn_whenSingleCategoryPassed() {
        List<UniprotkbStatisticsEntry> results =
                entryRepository.findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIn(
                        "rel0", SWISSPROT, List.of(STATISTICS_CATEGORIES[0]));

        assertThat(results, containsInAnyOrder(STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[1]));
    }

    @Test
    void findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIdIn_whenNoCategoryPassed() {
        List<UniprotkbStatisticsEntry> results =
                entryRepository.findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIn(
                        "rel0", SWISSPROT, Collections.emptyList());

        assertThat(results, empty());
    }
}