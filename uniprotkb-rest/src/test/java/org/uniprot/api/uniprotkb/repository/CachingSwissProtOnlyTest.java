package org.uniprot.api.uniprotkb.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * Created 24/02/2020
 *
 * @author Edd
 */
// @SpringBootTest(classes = {CachingSwissProtOnlyTest.TestConfig.class})
// @Import(UniProtKBCacheConfig.class)
@SpringBootTest
@ContextConfiguration
@Import(CachingSwissProtOnlyTest.TestConfig.class)
class CachingSwissProtOnlyTest {

    @Autowired private FetchEntryService service;

    @Test
    void doIt() {
        for (int i = 0; i < 10; i++) {
            service.getEntry("accession" + i);
        }
        System.out.println("-------------");
        for (int i = 0; i < 10; i++) {
            service.getEntry("accession" + i);
        }
    }

    @TestConfiguration
    @EnableCaching
    public static class TestConfig {
        @Bean
        public FetchEntryService fetchEntryService(CacheManager cacheManager) {
            return new FetchEntryService(cacheManager);
        }
    }
}
