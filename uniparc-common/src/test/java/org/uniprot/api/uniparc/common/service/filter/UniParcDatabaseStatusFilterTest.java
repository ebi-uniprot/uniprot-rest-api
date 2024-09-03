package org.uniprot.api.uniparc.common.service.filter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.store.indexer.uniparc.mockers.UniParcCrossReferenceMocker;

/**
 * @@author sahmad
 *
 * @created 11/08/2020
 */
class UniParcDatabaseStatusFilterTest {

    private static UniParcDatabaseStatusFilter uniParcDatabaseStatusFilter;

    @BeforeAll
    static void setUp() {
        uniParcDatabaseStatusFilter = new UniParcDatabaseStatusFilter();
    }

    @Test
    void testFilterByDatabaseStatus() {
        UniParcCrossReference activeXref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "AC12345", 9606, true);
        // filter by status
        boolean result = uniParcDatabaseStatusFilter.apply(activeXref, true);
        Assertions.assertTrue(result);
    }

    @Test
    void testFilterByDatabaseStatusFalse() {
        UniParcCrossReference activeXref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "AC12345", 9606, false);
        // filter by status
        boolean result = uniParcDatabaseStatusFilter.apply(activeXref, true);
        Assertions.assertFalse(result);
    }

    @Test
    void testFilterByNullStatus() {
        UniParcCrossReference activeXref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "AC12345", 9606, false);
        // filter by status
        boolean result = uniParcDatabaseStatusFilter.apply(activeXref, null);
        Assertions.assertTrue(result);
    }
}
