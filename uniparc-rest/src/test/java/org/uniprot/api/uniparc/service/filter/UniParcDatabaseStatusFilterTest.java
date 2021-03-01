package org.uniprot.api.uniparc.service.filter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;
import org.uniprot.core.uniparc.UniParcEntry;

/**
 * @@author sahmad
 *
 * @created 11/08/2020
 */
class UniParcDatabaseStatusFilterTest {

    private static UniParcDatabaseStatusFilter uniParcDatabaseStatusFilter;
    private static final String UNIPARC_ID_PREFIX = "UPI0000083B";
    private UniParcEntry uniParcEntry;

    @BeforeAll
    static void setUp() {
        uniParcDatabaseStatusFilter = new UniParcDatabaseStatusFilter();
    }

    @BeforeEach
    void createTestData() {
        this.uniParcEntry = UniParcEntryMocker.createEntry(1, UNIPARC_ID_PREFIX);
        Assertions.assertNotNull(this.uniParcEntry);
        Assertions.assertTrue(
                this.uniParcEntry.getUniParcId().getValue().startsWith(UNIPARC_ID_PREFIX));
    }

    @Test
    void testFilterByDatabaseStatus() {
        verifyUniParcEntry(uniParcEntry);
        boolean status = true;
        // filter by status
        UniParcEntry filteredEntry = uniParcDatabaseStatusFilter.apply(this.uniParcEntry, status);
        Assertions.assertEquals(uniParcEntry, filteredEntry);
    }

    @Test
    void testFilterByDatabaseStatusFalse() {
        verifyUniParcEntry(uniParcEntry);
        boolean status = false;
        // filter by status
        UniParcEntry filteredEntry = uniParcDatabaseStatusFilter.apply(this.uniParcEntry, status);
        Assertions.assertNotEquals(uniParcEntry, filteredEntry);
        // everything should be same but no xrefs
        Assertions.assertTrue(filteredEntry.getUniParcCrossReferences().isEmpty());
        verifyUniParcEntry(filteredEntry);
    }

    @Test
    void testFilterByNullStatus() {
        verifyUniParcEntry(uniParcEntry);
        Boolean status = null;
        // filter by status
        UniParcEntry filteredEntry = uniParcDatabaseStatusFilter.apply(this.uniParcEntry, status);
        // everything should be same
        Assertions.assertEquals(uniParcEntry, filteredEntry);
    }

    private void verifyUniParcEntry(UniParcEntry uniParcEntry) {
        Assertions.assertNotNull(uniParcEntry);
        Assertions.assertNotNull(uniParcEntry.getUniParcId());
        Assertions.assertTrue(uniParcEntry.getUniParcId().getValue().startsWith(UNIPARC_ID_PREFIX));
        Assertions.assertNotNull(uniParcEntry.getSequence());
        Assertions.assertNotNull(uniParcEntry.getSequenceFeatures());
    }
}
