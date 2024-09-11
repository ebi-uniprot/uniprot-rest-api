package org.uniprot.api.uniparc.common.service.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.core.uniparc.*;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;

/**
 * @@author sahmad
 *
 * @created 11/08/2020
 */
class UniParcDatabaseFilterTest {

    private static UniParcDatabaseFilter uniParcDatabaseFilter;
    private static final String UNIPARC_ID_PREFIX = "UPI0000083B";
    private UniParcEntry uniParcEntry;

    @BeforeAll
    static void setUp() {
        uniParcDatabaseFilter = new UniParcDatabaseFilter();
    }

    @BeforeEach
    void createTestData() {
        this.uniParcEntry = UniParcEntryMocker.createUniParcEntry(1, UNIPARC_ID_PREFIX);
        Assertions.assertNotNull(this.uniParcEntry);
        Assertions.assertTrue(
                this.uniParcEntry.getUniParcId().getValue().startsWith(UNIPARC_ID_PREFIX));
    }

    @Test
    void testFilterByDatabases() {
        verifyUniParcEntry(uniParcEntry);
        List<UniParcCrossReference> xrefs = uniParcEntry.getUniParcCrossReferences();
        Assertions.assertEquals(3, this.uniParcEntry.getUniParcCrossReferences().size());
        List<String> dbFilter = Collections.singletonList("UniProtKB/TrEMBL".toLowerCase());
        // filter by db
        UniParcEntry filteredEntry = uniParcDatabaseFilter.apply(this.uniParcEntry, dbFilter);
        // everything should be same except xrefs
        Assertions.assertEquals(1, filteredEntry.getUniParcCrossReferences().size());
        Assertions.assertEquals(
                UniParcDatabase.TREMBL.getDisplayName(),
                filteredEntry.getUniParcCrossReferences().get(0).getDatabase().getDisplayName());
        verifyUniParcEntry(filteredEntry);
        Assertions.assertNotEquals(xrefs, filteredEntry.getUniParcCrossReferences());
        verifyOriginalAndFilteredEntry(uniParcEntry, filteredEntry);
    }

    @Test
    void testFilterByEmptyDatabases() {
        verifyUniParcEntry(uniParcEntry);
        List<String> dbFilter = new ArrayList<>();
        // filter by db
        UniParcEntry filteredEntry = uniParcDatabaseFilter.apply(this.uniParcEntry, dbFilter);
        Assertions.assertEquals(uniParcEntry, filteredEntry);
    }

    @Test
    void testFilterByMatchingNonMatchingDatabases() {
        verifyUniParcEntry(uniParcEntry);
        List<UniParcCrossReference> xrefs = uniParcEntry.getUniParcCrossReferences();
        Assertions.assertEquals(3, this.uniParcEntry.getUniParcCrossReferences().size());
        List<String> dbFilter =
                Arrays.asList("RandomDB", UniParcDatabase.SWISSPROT.getDisplayName().toLowerCase());
        // filter by db
        UniParcEntry filteredEntry = uniParcDatabaseFilter.apply(this.uniParcEntry, dbFilter);
        // everything should be same except xrefs
        Assertions.assertEquals(1, filteredEntry.getUniParcCrossReferences().size());
        Assertions.assertEquals(
                UniParcDatabase.SWISSPROT.getDisplayName(),
                filteredEntry.getUniParcCrossReferences().get(0).getDatabase().getDisplayName());
        verifyUniParcEntry(filteredEntry);
        Assertions.assertNotEquals(xrefs, filteredEntry.getUniParcCrossReferences());
        verifyOriginalAndFilteredEntry(uniParcEntry, filteredEntry);
    }

    @Test
    void testFilterByNonMatchingDatabases() {
        verifyUniParcEntry(uniParcEntry);
        List<UniParcCrossReference> xrefs = uniParcEntry.getUniParcCrossReferences();
        Assertions.assertEquals(3, this.uniParcEntry.getUniParcCrossReferences().size());
        List<String> dbFilter = Collections.singletonList("RandomDB");
        // filter by db
        UniParcEntry filteredEntry = uniParcDatabaseFilter.apply(this.uniParcEntry, dbFilter);
        // everything should be same but no xrefs
        Assertions.assertTrue(filteredEntry.getUniParcCrossReferences().isEmpty());
        verifyUniParcEntry(filteredEntry);
        Assertions.assertNotEquals(xrefs, filteredEntry.getUniParcCrossReferences());
    }

    @Test
    void testFilterWithNoDatabases() {
        verifyUniParcEntry(uniParcEntry);
        UniParcEntryBuilder entryBuilder = UniParcEntryBuilder.from(uniParcEntry);
        entryBuilder.uniParcCrossReferencesSet(null);
        UniParcEntry entryWithoutXref = entryBuilder.build();
        List<String> dbFilter =
                Collections.singletonList(UniParcDatabase.SWISSPROT.getDisplayName());
        // filter by db
        UniParcEntry filteredEntry = uniParcDatabaseFilter.apply(entryWithoutXref, dbFilter);
        Assertions.assertEquals(entryWithoutXref, filteredEntry);
    }

    private void verifyUniParcEntry(UniParcEntry uniParcEntry) {
        Assertions.assertNotNull(uniParcEntry);
        Assertions.assertNotNull(uniParcEntry.getUniParcId());
        Assertions.assertTrue(uniParcEntry.getUniParcId().getValue().startsWith(UNIPARC_ID_PREFIX));
        Assertions.assertNotNull(uniParcEntry.getSequence());
        Assertions.assertNotNull(uniParcEntry.getSequenceFeatures());
    }

    private void verifyOriginalAndFilteredEntry(
            UniParcEntry uniParcEntry, UniParcEntry filteredEntry) {
        Assertions.assertEquals(uniParcEntry.getUniParcId(), filteredEntry.getUniParcId());
        Assertions.assertEquals(uniParcEntry.getSequence(), filteredEntry.getSequence());
        Assertions.assertEquals(
                uniParcEntry.getSequenceFeatures(), filteredEntry.getSequenceFeatures());
    }
}
