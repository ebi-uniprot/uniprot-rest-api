package org.uniprot.api.uniparc.common.service.filter;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.core.uniparc.*;
import org.uniprot.store.indexer.uniparc.mockers.UniParcCrossReferenceMocker;

/**
 * @@author sahmad
 *
 * @created 11/08/2020
 */
class UniParcDatabaseFilterTest {

    private static UniParcDatabaseFilter uniParcDatabaseFilter;

    @BeforeAll
    static void setUp() {
        uniParcDatabaseFilter = new UniParcDatabaseFilter();
    }

    @Test
    void testFilterByDatabaseStatus() {
        UniParcCrossReference activeXref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "AC12345", 9606, true);
        // filter by status
        boolean result = uniParcDatabaseFilter.apply(activeXref, List.of(""));
        Assertions.assertTrue(result);
    }

    @Test
    void testFilterByDatabaseStatusFalse() {
        UniParcCrossReference activeXref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "AC12345", 9606, false);
        // filter by status
        boolean result = uniParcDatabaseFilter.apply(activeXref, List.of(""));
        Assertions.assertFalse(result);
    }

    @Test
    void testFilterByNullStatus() {
        UniParcCrossReference activeXref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "AC12345", 9606, false);
        // filter by status
        boolean result = uniParcDatabaseFilter.apply(activeXref, List.of());
        Assertions.assertTrue(result);
    }
}
