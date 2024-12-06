package org.uniprot.api.uniparc.common.service.filter;

import static org.uniprot.core.uniparc.UniParcCrossReference.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.impl.UniParcCrossReferenceBuilder;

class UniParcProteomeIdFilterTest {

    private static UniParcProteomeIdFilter uniParcProteomeFilter;

    @BeforeAll
    static void setUp() {
        uniParcProteomeFilter = new UniParcProteomeIdFilter();
    }

    @Test
    void testFilterByProteomeIdParamEmptyAndValidPropertyReturnTrue() {
        String proteomeId = "UP000001";
        UniParcCrossReference xref =
                new UniParcCrossReferenceBuilder()
                        .propertiesAdd(PROPERTY_SOURCES, "TEST:" + proteomeId)
                        .build();
        // filter by proteome id
        boolean result = uniParcProteomeFilter.apply(xref, proteomeId);
        Assertions.assertTrue(result);
    }

    @Test
    void testFilterByProteomeIdParamNullReturnTrue() {
        UniParcCrossReference xref =
                new UniParcCrossReferenceBuilder().proteomeId("ANY_VALUE").build();
        // filter by proteome id
        boolean result = uniParcProteomeFilter.apply(xref, null);
        Assertions.assertTrue(result);
    }

    @Test
    void testFilterByProteomeIdXrefFoundReturnTrue() {
        String proteomeId = "UP000001";
        UniParcCrossReference xref =
                new UniParcCrossReferenceBuilder().proteomeId(proteomeId).build();
        // filter by proteome id
        boolean result = uniParcProteomeFilter.apply(xref, proteomeId);
        Assertions.assertTrue(result);
    }

    @Test
    void testFilterByProteomeIdNotFoundReturnFalse() {
        String proteomeId = "UP000001";
        UniParcCrossReference xref =
                new UniParcCrossReferenceBuilder()
                        .proteomeId(proteomeId)
                        .propertiesAdd(PROPERTY_SOURCES, "TEST:" + proteomeId)
                        .build();
        // filter by proteome id
        boolean result = uniParcProteomeFilter.apply(xref, "UP000002");
        Assertions.assertFalse(result);
    }
}
