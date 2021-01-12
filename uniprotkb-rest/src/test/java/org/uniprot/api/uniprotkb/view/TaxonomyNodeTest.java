package org.uniprot.api.uniprotkb.view;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author sahmad
 * @created 09/12/2020
 */
public class TaxonomyNodeTest {

    @Test
    void testGetFullName() {
        TaxonomyNode node = new TaxonomyNode();
        String sName = "sample scientific name";
        String commonName = "sample common name";
        String syn = "sample synonym";
        node.setScientificName(sName);
        node.setCommonName(commonName);
        node.setSynonym(syn);
        String fullName = node.getFullName();
        Assertions.assertNotNull(fullName);
        Assertions.assertEquals(
                "sample scientific name (sample common name) (sample synonym)", fullName);
    }

    @Test
    void testGetFullNameWithoutCommonName() {
        TaxonomyNode node = new TaxonomyNode();
        String sName = "sample scientific name";
        String syn = "sample synonym";
        node.setScientificName(sName);
        node.setSynonym(syn);
        String fullName = node.getFullName();
        Assertions.assertNotNull(fullName);
        Assertions.assertEquals("sample scientific name (sample synonym)", fullName);
    }

    @Test
    void testGetFullNameWithoutSynonym() {
        TaxonomyNode node = new TaxonomyNode();
        String sName = "sample scientific name";
        String commonName = "sample common name";
        node.setScientificName(sName);
        node.setCommonName(commonName);
        String fullName = node.getFullName();
        Assertions.assertNotNull(fullName);
        Assertions.assertEquals("sample scientific name (sample common name)", fullName);
    }
}
