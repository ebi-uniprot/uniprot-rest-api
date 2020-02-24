package org.uniprot.api.uniprotkb.repository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Import;
import org.uniprot.api.uniprotkb.configuration.UniProtKBCacheConfig;
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.core.uniprot.UniProtEntryType;
import org.uniprot.core.uniprot.builder.UniProtEntryBuilder;

/**
 * Created 24/02/2020
 *
 * @author Edd
 */
class CachingSwissProtOnlyTest {

    @Test
    void doIt() {
        FetchEntryService service = new FetchEntryService();
        for (int i = 0; i < 10; i++) {
            service.getEntry("accession"+i);
        }
        for (int i = 0; i < 10; i++) {
            service.getEntry("accession"+i);
        }
    }

    @TestConfiguration
    @Import(UniProtKBCacheConfig.class)
    static class TestConfig {}

    private static class FetchEntryService {
        // https://stackoverflow.com/questions/21806298/compare-enums-in-spel
        // https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#cache
        @Cacheable(
                cacheNames = "taxonomyCache",
                unless = "#result.getEntryType().name() != 'SWISSPROT'")
        public UniProtEntry getEntry(String id) {
            System.out.print("Fetching ... ");
            UniProtEntry uniProtEntry = pretendFetchEntry(id);
            System.out.println(" Done.");

            return uniProtEntry;
        }

        private UniProtEntry pretendFetchEntry(String id) {
            double random = Math.random();
            UniProtEntryType entryType;// = UniProtEntryType.INACTIVE;
            if (random < 0.5) {
                entryType = UniProtEntryType.SWISSPROT;
            } else {//if (random < 0.6) {
                entryType = UniProtEntryType.TREMBL;
            }

            String uniProtId = "id-" + id;
            UniProtEntry entry = new UniProtEntryBuilder(id, uniProtId, entryType).build();

            System.out.print("[acc=" + id + ", id=" + uniProtId + ", type=" + entryType+"]");

            return entry;
        }
    }
}
