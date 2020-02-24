package org.uniprot.api.uniprotkb.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.jcache.JCacheManagerFactoryBean;
import org.springframework.stereotype.Service;
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.core.uniprot.UniProtEntryType;
import org.uniprot.core.uniprot.builder.UniProtEntryBuilder;

import javax.annotation.PostConstruct;

/**
 * Created 24/02/20
 *
 * @author Edd
 */
@Service
public class FetchEntryService {
    @Autowired private CacheManager cacheManager;
    private Cache cache;

    @PostConstruct
    public void fetchEntryService() {
                cache = cacheManager.getCache("swissProtEntryCache");
//        System.out.println("hello world");
//        cache = null;
    }

    // https://stackoverflow.com/questions/21806298/compare-enums-in-spel
    // https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#cache
    //    @Cacheable(value = "swissProtEntryCache",
    //             unless = "#result.getEntryType().name() != 'SWISSPROT'"
    //    )
    public UniProtEntry getEntry(String id) {
        System.out.print("Fetching id=" + id + "... ");

        UniProtEntry toReturn;
        UniProtEntry cachedEntry = cache.get(id, UniProtEntry.class);
        if (cachedEntry != null) {
            toReturn = cachedEntry;
        } else {
            UniProtEntry fetchedEntry = pretendFetchEntry(id);
            if (fetchedEntry.getEntryType().equals(UniProtEntryType.SWISSPROT)) {
                cache.putIfAbsent(id, fetchedEntry);
            }
            toReturn = fetchedEntry;
        }

        System.out.println(" Done.");

        return toReturn;
    }

    public UniProtEntry pretendFetchEntry(String id) {
        double random = Math.random();
        UniProtEntryType entryType; // = UniProtEntryType.INACTIVE;
        if (random < 0.5) {
            entryType = UniProtEntryType.SWISSPROT;
        } else { // if (random < 0.6) {
            entryType = UniProtEntryType.TREMBL;
        }

        String uniProtId = "id-" + id;
        UniProtEntry entry = new UniProtEntryBuilder(id, uniProtId, entryType).build();

        System.out.print("[acc=" + id + ", id=" + uniProtId + ", type=" + entryType + "]");

        return entry;
    }
}
