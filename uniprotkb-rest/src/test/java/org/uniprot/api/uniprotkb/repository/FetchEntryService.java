package org.uniprot.api.uniprotkb.repository;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.core.uniprot.UniProtEntryType;
import org.uniprot.core.uniprot.builder.UniProtEntryBuilder;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created 24/02/20
 *
 * @author Edd
 */
public class FetchEntryService {
    private CacheManager cacheManager;
    private Cache cache;

    public FetchEntryService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @PostConstruct
    public void fetchEntryService() {
        cache = cacheManager.getCache("swissProtEntryCache");
    }

    public UniProtEntry getEntry(String id) {
        return getEntries(Collections.singletonList(id)).get(0);
    }

    public List<UniProtEntry> getEntries(List<String> ids) {
        List<UniProtEntry> entries = new ArrayList<>();
        List<String> idsToFetchFromStore = new ArrayList<>();
        for (String id : ids) {
            UniProtEntry cachedEntry = cache.get(id, UniProtEntry.class);
            if (cachedEntry != null) {
                System.out.println("Fetched cached entry: " + id);
                entries.add(cachedEntry);
            } else {
                idsToFetchFromStore.add(id);
            }
        }

        List<UniProtEntry> entriesFromStore = pretendFetchEntries(idsToFetchFromStore);
        for (UniProtEntry fetchedEntry : entriesFromStore) {
            if (fetchedEntry.getEntryType().equals(UniProtEntryType.SWISSPROT)) {
                cache.putIfAbsent(fetchedEntry.getPrimaryAccession().getValue(), fetchedEntry);
            }
        }

        entries.addAll(entriesFromStore);
        return entries;
    }

    private List<UniProtEntry> pretendFetchEntries(List<String> idsToFetchFromStore) {
        return idsToFetchFromStore.stream()
                .map(this::pretendFetchEntry)
                .collect(Collectors.toList());
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
