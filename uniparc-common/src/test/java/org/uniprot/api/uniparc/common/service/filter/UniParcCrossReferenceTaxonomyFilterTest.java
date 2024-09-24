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
    void testFilterByTaxonomyParamEmptyReturnTrue() {
        UniParcCrossReference xref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "AC12345", 9606, true);
        // filter by status
        boolean result = uniParcTaxonomyFilter.apply(xref, List.of());
        Assertions.assertTrue(result);
    }

    @Test
    void testFilterByTaxonomyParamNullReturnTrue() {
        UniParcCrossReference xref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "AC12345", 9606, false);
        // filter by status
        boolean result = uniParcTaxonomyFilter.apply(xref, null);
        Assertions.assertTrue(result);
    }

    @Test
    void testFilterByNullTaxonomyXrefReturnFalse() {
        UniParcCrossReference xref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(UniParcDatabase.EMBL);
        // filter by status
        boolean result = uniParcTaxonomyFilter.apply(xref, List.of("9000"));
        Assertions.assertFalse(result);
    }

    @Test
    void testFilterByTaxonomyNotFoundReturnFalse() {
        UniParcCrossReference xref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "AC12345", 9606, false);
        // filter by status
        boolean result = uniParcTaxonomyFilter.apply(xref, List.of("9000"));
        Assertions.assertFalse(result);
    }

    @Test
    void testFilterByTaxonomyFoundReturnTrue() {
        UniParcCrossReference xref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "AC12345", 9606, false);
        // filter by status
        boolean result = uniParcTaxonomyFilter.apply(xref, List.of("9606"));
        Assertions.assertTrue(result);
    }
}
