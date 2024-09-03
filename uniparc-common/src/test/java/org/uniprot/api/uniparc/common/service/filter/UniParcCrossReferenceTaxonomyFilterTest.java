package org.uniprot.api.uniparc.common.service.filter;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.store.indexer.uniparc.mockers.UniParcCrossReferenceMocker;

/**
 * @author lgonzales
 * @since 14/08/2020
 */
class UniParcCrossReferenceTaxonomyFilterTest {

    private static UniParcCrossReferenceTaxonomyFilter uniParcTaxonomyFilter;

    @BeforeAll
    static void setUp() {
        uniParcTaxonomyFilter = new UniParcCrossReferenceTaxonomyFilter();
    }

    @Test
    void testFilterByDatabaseStatus() {
        UniParcCrossReference activeXref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "AC12345", 9606, true);
        // filter by status
        boolean result = uniParcTaxonomyFilter.apply(activeXref, List.of(""));
        Assertions.assertTrue(result);
    }

    @Test
    void testFilterByDatabaseStatusFalse() {
        UniParcCrossReference activeXref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "AC12345", 9606, false);
        // filter by status
        boolean result = uniParcTaxonomyFilter.apply(activeXref, List.of(""));
        Assertions.assertFalse(result);
    }

    @Test
    void testFilterByNullStatus() {
        UniParcCrossReference activeXref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "AC12345", 9606, false);
        // filter by status
        boolean result = uniParcTaxonomyFilter.apply(activeXref, List.of());
        Assertions.assertTrue(result);
    }
}
