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

import javax.annotation.PostConstruct;

import static java.util.Arrays.asList;

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
        //        for (int i = 0; i < 10; i++) {
        //            service.getEntry("accession" + i);
        //        }
        //        System.out.println("-------------");
        //        for (int i = 0; i < 10; i++) {
        //            service.getEntry("accession" + i);
        //        }

        service.getEntries(asList("a1", "a2", "a3", "a4", "a5", "a6"));
        service.getEntries(asList("a1", "a2", "a3", "a4", "a5", "a6"));
    }

    @TestConfiguration
    @EnableCaching
    public static class TestConfig {
                @Autowired private CacheManager cacheManager;

                @PostConstruct
                public void setEntryServiceCache() {
                    entryService().setCache(cacheManager.getCache("swissProtEntryCache"));
                }

        //        @Bean
        //        public Cache cache(CacheManager cacheManager) {
        //            return cacheManager.getCache("swissProtEntryCache");
        //        }

        @Bean
        public FetchEntryService entryService() {
            return new FetchEntryService();
        }
    }

//    @Component
//    public static class MyCache {
//        @Autowired
//        private CacheManager cacheManager;
//
//        public Cache cache() {
//            return cache;
//        }
//
//        private Cache cache;
//
//        @PostConstruct
//        public void init() {
//            this.cache = cacheManager.getCache("swissProtEntryCache");
//        }
//    }
}
