package org.uniprot.api.uniparc.service.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.api.uniparc.controller.UniParcControllerITUtils;
import org.uniprot.core.uniparc.*;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;

/**
 * @author sahmad
 * @created 11/08/2020
 */
class UniParcDatabaseIdFilterTest {

    private static UniParcDatabaseIdFilter idFilter;
    private static final String UNIPARC_ID_PREFIX = "UPI0000083B";
    private UniParcEntry uniParcEntry;

    @BeforeAll
    static void setUp() {
        idFilter = new UniParcDatabaseIdFilter();
    }

    @BeforeEach
    void createTestData() {
        this.uniParcEntry = UniParcControllerITUtils.createEntry(1, UNIPARC_ID_PREFIX);
        Assertions.assertNotNull(this.uniParcEntry);
        Assertions.assertTrue(
                this.uniParcEntry.getUniParcId().getValue().startsWith(UNIPARC_ID_PREFIX));
    }

    @Test
    void testFilterByDatabases() {
        verifyUniParcEntry(uniParcEntry);
        List<UniParcCrossReference> xrefs = uniParcEntry.getUniParcCrossReferences();
        Assertions.assertEquals(2, this.uniParcEntry.getUniParcCrossReferences().size());
        List<String> dbIdFilter = Arrays.asList("P12301".toLowerCase());
        // filter by dbid
        UniParcEntry filteredEntry = idFilter.apply(this.uniParcEntry, dbIdFilter);
        // everything should be same except xrefs
        Assertions.assertEquals(1, filteredEntry.getUniParcCrossReferences().size());
        Assertions.assertEquals("P12301", filteredEntry.getUniParcCrossReferences().get(0).getId());
        verifyUniParcEntry(filteredEntry);
        Assertions.assertNotEquals(xrefs, filteredEntry.getUniParcCrossReferences());
        verifyOriginalAndFilteredEntry(uniParcEntry, filteredEntry);
    }

    @Test
    void testFilterByEmptyDatabases() {
        verifyUniParcEntry(uniParcEntry);
        List<String> dbIdFilter = new ArrayList<>();
        // filter by dbid
        UniParcEntry filteredEntry = idFilter.apply(this.uniParcEntry, dbIdFilter);
        Assertions.assertEquals(uniParcEntry, filteredEntry);
    }

    @Test
    void testFilterByMatchingNonMatchingDatabases() {
        verifyUniParcEntry(uniParcEntry);
        List<UniParcCrossReference> xrefs = uniParcEntry.getUniParcCrossReferences();
        Assertions.assertEquals(2, this.uniParcEntry.getUniParcCrossReferences().size());
        List<String> dbIdFilter = Arrays.asList("RandomDB".toLowerCase(), "P10001".toLowerCase());
        // filter by dbid
        UniParcEntry filteredEntry = idFilter.apply(this.uniParcEntry, dbIdFilter);
        // everything should be same except xrefs
        Assertions.assertEquals(1, filteredEntry.getUniParcCrossReferences().size());
        Assertions.assertEquals("P10001", filteredEntry.getUniParcCrossReferences().get(0).getId());
        verifyUniParcEntry(filteredEntry);
        Assertions.assertNotEquals(xrefs, filteredEntry.getUniParcCrossReferences());
        verifyOriginalAndFilteredEntry(uniParcEntry, filteredEntry);
    }

    @Test
    void testFilterByNonMatchingDatabases() {
        verifyUniParcEntry(uniParcEntry);
        List<UniParcCrossReference> xrefs = uniParcEntry.getUniParcCrossReferences();
        Assertions.assertEquals(2, this.uniParcEntry.getUniParcCrossReferences().size());
        List<String> dbIdFilter = Arrays.asList("RandomDB".toLowerCase());
        // filter by dbid
        UniParcEntry filteredEntry = idFilter.apply(this.uniParcEntry, dbIdFilter);
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
        List<String> dbIdFilter = Arrays.asList("P1001".toLowerCase());
        // filter by dbid
        UniParcEntry filteredEntry = idFilter.apply(entryWithoutXref, dbIdFilter);
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
        Assertions.assertEquals(uniParcEntry.getTaxonomies(), filteredEntry.getTaxonomies());
    }
}
