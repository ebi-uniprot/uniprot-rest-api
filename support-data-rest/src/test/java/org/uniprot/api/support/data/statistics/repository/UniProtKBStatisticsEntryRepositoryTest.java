package org.uniprot.api.support.data.statistics.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.uniprot.api.support.data.statistics.TestEntityGeneratorUtil.*;
import static org.uniprot.api.support.data.statistics.entity.EntryType.SWISSPROT;
import static org.uniprot.api.support.data.statistics.entity.EntryType.TREMBL;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.support.data.statistics.entity.UniProtKBStatisticsEntry;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@Import({
    HttpCommonHeaderConfig.class,
    RequestMappingHandlerMapping.class,
    RequestMappingHandlerAdapter.class
})
@ActiveProfiles(profiles = "offline")
class UniProtKBStatisticsEntryRepositoryTest {
    @Autowired private TestEntityManager entityManager;

    @Autowired private UniProtKBStatisticsEntryRepository entryRepository;

    @BeforeEach
    void setUp() {
        Arrays.stream(STATISTICS_CATEGORIES).forEach(entityManager::persist);
        Arrays.stream(STATISTICS_ENTRIES).forEach(entityManager::persist);
        Arrays.stream(RELEASES).forEach(entityManager::persist);
    }

    @Test
    void findAllByReleaseNameAndEntryType() {
        List<UniProtKBStatisticsEntry> results =
                entryRepository.findAllByUniprotReleaseAndEntryType(RELEASES[0], SWISSPROT);

        assertThat(
                results,
                containsInAnyOrder(
                        STATISTICS_ENTRIES[0],
                        STATISTICS_ENTRIES[1],
                        STATISTICS_ENTRIES[3],
                        STATISTICS_ENTRIES[4]));
    }

    @Test
    void findAllByReleaseName() {
        List<UniProtKBStatisticsEntry> results = entryRepository.findAllByUniprotRelease(RELEASES[0]);

        assertThat(
                results,
                containsInAnyOrder(
                        STATISTICS_ENTRIES[0],
                        STATISTICS_ENTRIES[1],
                        STATISTICS_ENTRIES[3],
                        STATISTICS_ENTRIES[4],
                        STATISTICS_ENTRIES[5]));
    }

    @Test
    void findAllByReleaseNameAndEntryType_whenNoMatch() {
        List<UniProtKBStatisticsEntry> results =
                entryRepository.findAllByUniprotReleaseAndEntryType(RELEASES[2], TREMBL);

        assertThat(results, empty());
    }

    @Test
    void findAllByReleaseName_whenNoMatch() {
        List<UniProtKBStatisticsEntry> results = entryRepository.findAllByUniprotRelease(RELEASES[2]);

        assertThat(results, empty());
    }

    @Test
    void findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIdIn() {
        List<UniProtKBStatisticsEntry> results =
                entryRepository.findAllByUniprotReleaseAndEntryTypeAndStatisticsCategoryIn(
                        RELEASES[0],
                        SWISSPROT,
                        List.of(STATISTICS_CATEGORIES[0], STATISTICS_CATEGORIES[1]));

        assertThat(
                results,
                containsInAnyOrder(
                        STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[1], STATISTICS_ENTRIES[3]));
    }

    @Test
    void findAllByReleaseNameAndStatisticsCategoryIdIn() {
        List<UniProtKBStatisticsEntry> results =
                entryRepository.findAllByUniprotReleaseAndStatisticsCategoryIn(
                        RELEASES[0], List.of(STATISTICS_CATEGORIES[0], STATISTICS_CATEGORIES[1]));

        assertThat(
                results,
                containsInAnyOrder(
                        STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[1], STATISTICS_ENTRIES[3]));
    }

    @Test
    void findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIdIn_whenSingleCategoryPassed() {
        List<UniProtKBStatisticsEntry> results =
                entryRepository.findAllByUniprotReleaseAndEntryTypeAndStatisticsCategoryIn(
                        RELEASES[0], SWISSPROT, List.of(STATISTICS_CATEGORIES[0]));

        assertThat(results, containsInAnyOrder(STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[1]));
    }

    @Test
    void findAllByReleaseNameAndStatisticsCategoryIdIn_whenSingleCategoryPassed() {
        List<UniProtKBStatisticsEntry> results =
                entryRepository.findAllByUniprotReleaseAndStatisticsCategoryIn(
                        RELEASES[0], List.of(STATISTICS_CATEGORIES[0]));

        assertThat(results, containsInAnyOrder(STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[1]));
    }

    @Test
    void findAllByReleaseNameAndEntryTypeAndStatisticsCategoryIdIn_whenNoCategoryPassed() {
        List<UniProtKBStatisticsEntry> results =
                entryRepository.findAllByUniprotReleaseAndEntryTypeAndStatisticsCategoryIn(
                        RELEASES[0], SWISSPROT, Collections.emptyList());

        assertThat(results, empty());
    }

    @Test
    void findAllByReleaseNameAndStatisticsCategoryIdIn_whenNoCategoryPassed() {
        List<UniProtKBStatisticsEntry> results =
                entryRepository.findAllByUniprotReleaseAndStatisticsCategoryIn(
                        RELEASES[2], Collections.emptyList());

        assertThat(results, empty());
    }

    @Test
    void findAllByAttributeNameAndEntryType() {
        List<UniProtKBStatisticsEntry> result =
                entryRepository.findAllByAttributeNameIgnoreCaseAndEntryType("name0", SWISSPROT);

        assertThat(result, containsInAnyOrder(STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[3]));
    }

    @Test
    void findAllByAttributeNameAndEntryType_caseDiff() {
        List<UniProtKBStatisticsEntry> result =
                entryRepository.findAllByAttributeNameIgnoreCaseAndEntryType("Name0", SWISSPROT);

        assertThat(result, containsInAnyOrder(STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[3]));
    }

    @Test
    void findAllByAttributeNameAndEntryType_emptyResults() {
        List<UniProtKBStatisticsEntry> result =
                entryRepository.findAllByAttributeNameIgnoreCaseAndEntryType("name1", TREMBL);

        assertThat(result, empty());
    }

    @Test
    void findAllByAttributeName() {
        List<UniProtKBStatisticsEntry> result =
                entryRepository.findAllByAttributeNameIgnoreCase("name0");

        assertThat(
                result,
                containsInAnyOrder(
                        STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[2], STATISTICS_ENTRIES[3]));
    }

    @Test
    void findAllByAttributeName_caseDiff() {
        List<UniProtKBStatisticsEntry> result =
                entryRepository.findAllByAttributeNameIgnoreCase("Name0");

        assertThat(
                result,
                containsInAnyOrder(
                        STATISTICS_ENTRIES[0], STATISTICS_ENTRIES[2], STATISTICS_ENTRIES[3]));
    }

    @Test
    void findAllByAttributeName_emptyResult() {
        List<UniProtKBStatisticsEntry> result =
                entryRepository.findAllByAttributeNameIgnoreCase("name3");

        assertThat(result, empty());
    }
}
