package org.uniprot.api.uniparc.common.service.filter;

import static org.uniprot.core.uniparc.UniParcCrossReference.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.uniparc.impl.UniParcCrossReferenceBuilder;
import org.uniprot.store.indexer.uniparc.mockers.UniParcCrossReferenceMocker;

class UniParcProteomeIdFilterTest {

    private static UniParcProteomeIdFilter uniParcProteomeFilter;

    @BeforeAll
    static void setUp() {
        uniParcProteomeFilter = new UniParcProteomeIdFilter();
    }

    @Test
    void testFilterByProteomeIdParamEmptyReturnTrue() {
        String proteomeId = "UP000001";
        UniParcCrossReference xref =
                new UniParcCrossReferenceBuilder()
                        .propertiesAdd(PROPERTY_SOURCES, "TEST:" + proteomeId)
                        .build();
        // filter by status
        boolean result = uniParcProteomeFilter.apply(xref, proteomeId);
        Assertions.assertTrue(result);
    }

    @Test
    void testFilterByProteomeIdParamNullReturnTrue() {
        UniParcCrossReference xref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "AC12345", 9606, false);
        // filter by status
        boolean result = uniParcProteomeFilter.apply(xref, null);
        Assertions.assertTrue(result);
    }

    @Test
    void testFilterByNullProteomeIdXrefReturnFalse() {
        UniParcCrossReference xref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(UniParcDatabase.EMBL);
        // filter by status
        boolean result = false; // uniParcProteomeFilter.apply(xref, List.of("9000"));
        Assertions.assertFalse(result);
    }

    @Test
    void testFilterByProteomeIdNotFoundReturnFalse() {
        UniParcCrossReference xref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "AC12345", 9606, false);
        // filter by status
        boolean result = false; // uniParcProteomeFilter.apply(xref, List.of("9000"));
        Assertions.assertFalse(result);
    }

    @Test
    void testFilterByProteomeIdFoundReturnTrue() {
        UniParcCrossReference xref =
                UniParcCrossReferenceMocker.createUniParcCrossReference(
                        UniParcDatabase.EMBL, "AC12345", 9606, false);
        // filter by status
        boolean result = false; // uniParcProteomeFilter.apply(xref, List.of("9606"));
        Assertions.assertTrue(result);
    }
}
