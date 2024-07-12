package org.uniprot.api.support.data;

import static org.mockito.Mockito.mock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.support.data.statistics.repository.StatisticsCategoryRepository;
import org.uniprot.api.support.data.statistics.repository.UniProtKBStatisticsEntryRepository;
import org.uniprot.api.support.data.statistics.repository.UniProtReleaseRepository;

@TestConfiguration
public class DataStoreTestConfig {

    @Bean("testUniprotkbStatisticsEntryRepository")
    @Profile("offline")
    public UniProtKBStatisticsEntryRepository uniprotkbStatisticsEntryRepository() {
        return mock(UniProtKBStatisticsEntryRepository.class);
    }

    @Bean("testStatisticsCategoryRepository")
    @Profile("offline")
    public StatisticsCategoryRepository statisticsCategoryRepository() {
        return mock(StatisticsCategoryRepository.class);
    }

    @Bean("testUniprotReleaseRepository")
    @Profile("offline")
    public UniProtReleaseRepository uniProtReleaseRepository() {
        return mock(UniProtReleaseRepository.class);
    }
}
