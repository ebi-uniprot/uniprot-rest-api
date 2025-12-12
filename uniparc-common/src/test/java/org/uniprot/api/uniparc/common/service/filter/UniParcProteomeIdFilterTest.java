package org.uniprot.api.uniparc.common.service.filter;

import static org.uniprot.core.uniparc.UniParcCrossReference.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.impl.ProteomeIdComponentBuilder;
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
        boolean result = uniParcProteomeFilter.test(xref, proteomeId);
        Assertions.assertTrue(result);
    }

    @Test
    void testFilterByProteomeIdParamNullReturnTrue() {
        UniParcCrossReference xref =
                new UniParcCrossReferenceBuilder()
                        .proteomeIdComponentsAdd(
                                new ProteomeIdComponentBuilder()
                                        .proteomeId("ANY_VALUE")
                                        .component("ANY_COMP")
                                        .build())
                        .build();
        // filter by proteome id
        boolean result = uniParcProteomeFilter.test(xref, null);
        Assertions.assertTrue(result);
    }

    @Test
    void testFilterByProteomeIdXrefFoundReturnTrue() {
        String proteomeId = "UP000001";
        UniParcCrossReference xref =
                new UniParcCrossReferenceBuilder()
                        .proteomeIdComponentsAdd(
                                new ProteomeIdComponentBuilder()
                                        .proteomeId(proteomeId)
                                        .component("COMP")
                                        .build())
                        .build();
        // filter by proteome id
        boolean result = uniParcProteomeFilter.test(xref, proteomeId);
        Assertions.assertTrue(result);
    }

    @Test
    void testFilterByProteomeIdNotFoundReturnFalse() {
        String proteomeId = "UP000001";
        UniParcCrossReference xref =
                new UniParcCrossReferenceBuilder()
                        .proteomeIdComponentsAdd(
                                new ProteomeIdComponentBuilder()
                                        .proteomeId(proteomeId)
                                        .component("COMP")
                                        .build())
                        .propertiesAdd(PROPERTY_SOURCES, "TEST:" + proteomeId)
                        .build();
        // filter by proteome id
        boolean result = uniParcProteomeFilter.test(xref, "UP000002");
        Assertions.assertFalse(result);
    }
}
