package org.uniprot.api.common.repository.stream.store.uniprotkb;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.common.repository.stream.store.uniprotkb.UniProtKBBatchStoreIterableUtil.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.uniprot.core.taxonomy.TaxonomyLineage;
import org.uniprot.core.taxonomy.impl.TaxonomyLineageBuilder;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;

class UniProtKBBatchStoreIterableUtilTest {

    @Test
    void canPopulateSingleEntry() {
        TaxonomyLineageService lineageService = Mockito.mock(TaxonomyLineageService.class);
        TaxonomyLineage lineage1 = new TaxonomyLineageBuilder().taxonId(9607L).build();
        TaxonomyLineage lineage2 = new TaxonomyLineageBuilder().taxonId(9608L).build();
        Mockito.when(lineageService.findByIds(Set.of(197221L)))
                .thenReturn(Map.of(197221L, List.of(lineage1, lineage2)));
        List<UniProtKBEntry> entries = new ArrayList<>();
        entries.add(UniProtEntryMocker.create(UniProtEntryMocker.Type.SP));
        List<UniProtKBEntry> result = populateLineageInEntry(lineageService, entries);
        assertNotNull(result);
        UniProtKBEntry resultEntry = result.get(0);
        assertNotNull(resultEntry);
        assertTrue(resultEntry.getLineages().contains(lineage1));
        assertTrue(resultEntry.getLineages().contains(lineage2));
    }

    @Test
    void canPopulateMultipleEntries() {
        TaxonomyLineageService lineageService = Mockito.mock(TaxonomyLineageService.class);
        TaxonomyLineage lineage1 = new TaxonomyLineageBuilder().taxonId(9607L).build();
        TaxonomyLineage lineage2 = new TaxonomyLineageBuilder().taxonId(9608L).build();
        Mockito.when(lineageService.findByIds(Set.of(197221L, 9615L)))
                .thenReturn(Map.of(197221L, List.of(lineage1), 9615L, List.of(lineage2)));
        List<UniProtKBEntry> entries = new ArrayList<>();
        entries.add(UniProtEntryMocker.create(UniProtEntryMocker.Type.SP));
        entries.add(UniProtEntryMocker.create(UniProtEntryMocker.Type.TR));
        List<UniProtKBEntry> result = populateLineageInEntry(lineageService, entries);
        assertNotNull(result);
        UniProtKBEntry resultEntry = result.get(0);
        assertNotNull(resultEntry);
        assertTrue(resultEntry.getLineages().contains(lineage1));

        resultEntry = result.get(1);
        assertNotNull(resultEntry);
        assertTrue(resultEntry.getLineages().contains(lineage2));
    }

    @Test
    void canPopulateMultipleEntriesFoundOneOrganism() {
        TaxonomyLineageService lineageService = Mockito.mock(TaxonomyLineageService.class);
        TaxonomyLineage lineage1 = new TaxonomyLineageBuilder().taxonId(9607L).build();
        TaxonomyLineage lineage2 = new TaxonomyLineageBuilder().taxonId(9608L).build();
        Mockito.when(lineageService.findByIds(Set.of(197221L, 9615L)))
                .thenReturn(Map.of(197221L, List.of(lineage1)));
        List<UniProtKBEntry> entries = new ArrayList<>();
        entries.add(UniProtEntryMocker.create(UniProtEntryMocker.Type.SP));
        entries.add(UniProtEntryMocker.create(UniProtEntryMocker.Type.TR));
        List<UniProtKBEntry> result = populateLineageInEntry(lineageService, entries);
        assertNotNull(result);
        UniProtKBEntry resultEntry = result.get(0);
        assertNotNull(resultEntry);
        assertTrue(resultEntry.getLineages().contains(lineage1));

        resultEntry = result.get(1);
        assertNotNull(resultEntry);
        assertTrue(resultEntry.getLineages().isEmpty());
    }
}
