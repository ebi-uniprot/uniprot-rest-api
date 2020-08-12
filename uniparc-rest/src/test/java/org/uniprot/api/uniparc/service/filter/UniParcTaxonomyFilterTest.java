package org.uniprot.api.uniparc.service.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.api.uniparc.controller.UniParcControllerITUtils;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.core.uniprotkb.taxonomy.Taxonomy;

/**
 * @author sahmad
 * @created 11/08/2020
 */
class UniParcTaxonomyFilterTest {

    private static UniParcTaxonomyFilter uniParcTaxonomyFilter;
    private static final String UNIPARC_ID_PREFIX = "UPI0000083C";
    private UniParcEntry uniParcEntry;

    @BeforeAll
    static void setUp() {
        uniParcTaxonomyFilter = new UniParcTaxonomyFilter();
    }

    @BeforeEach
    void createTestData() {
        this.uniParcEntry = UniParcControllerITUtils.createEntry(1, UNIPARC_ID_PREFIX);
        Assertions.assertNotNull(this.uniParcEntry);
        Assertions.assertTrue(
                this.uniParcEntry.getUniParcId().getValue().startsWith(UNIPARC_ID_PREFIX));
    }

    @Test
    void testFilterByTaxonomyIds() {
        verifyUniParcEntry(uniParcEntry);
        Assertions.assertEquals(2, this.uniParcEntry.getTaxonomies().size());
        List<String> taxonFilter = Arrays.asList("9606");
        // filter by db
        UniParcEntry filteredEntry = uniParcTaxonomyFilter.apply(this.uniParcEntry, taxonFilter);
        // everything should be same except xrefs
        Assertions.assertEquals(1, filteredEntry.getTaxonomies().size());
        Assertions.assertEquals(9606L, filteredEntry.getTaxonomies().get(0).getTaxonId());
        verifyUniParcEntry(filteredEntry);
        verifyOriginalAndFilteredEntry(this.uniParcEntry, filteredEntry);
    }

    @Test
    void testFilterByEmptyTaxonFilter() {
        verifyUniParcEntry(uniParcEntry);
        List<String> taxonFilter = new ArrayList<>();
        // filter by db
        UniParcEntry filteredEntry = uniParcTaxonomyFilter.apply(this.uniParcEntry, taxonFilter);
        Assertions.assertEquals(uniParcEntry, filteredEntry);
    }

    @Test
    void testFilterByMatchingNonMatchingTaxons() {
        verifyUniParcEntry(uniParcEntry);
        Assertions.assertEquals(2, this.uniParcEntry.getTaxonomies().size());
        List<String> taxonFilter = Arrays.asList("0", "10090");
        // filter by taxon
        UniParcEntry filteredEntry = uniParcTaxonomyFilter.apply(this.uniParcEntry, taxonFilter);
        // everything should be same except taxon
        Assertions.assertEquals(1, filteredEntry.getTaxonomies().size());
        Assertions.assertEquals(10090L, filteredEntry.getTaxonomies().get(0).getTaxonId());
        verifyUniParcEntry(filteredEntry);
        Assertions.assertNotEquals(uniParcEntry.getTaxonomies(), filteredEntry.getTaxonomies());
        verifyOriginalAndFilteredEntry(this.uniParcEntry, filteredEntry);
    }

    @Test
    void testFilterByNonMatchingTaxon() {
        verifyUniParcEntry(uniParcEntry);
        List<Taxonomy> taxonomies = uniParcEntry.getTaxonomies();
        Assertions.assertEquals(2, this.uniParcEntry.getUniParcCrossReferences().size());
        List<String> taxonFilter = Arrays.asList("0");
        // filter by taxon
        UniParcEntry filteredEntry = uniParcTaxonomyFilter.apply(this.uniParcEntry, taxonFilter);
        // everything should be same but no taxon
        //        Assertions.assertTrue(filteredEntry.getTaxonomies().isEmpty());//FIXME see
        // implementation of getTaxonomies
        verifyUniParcEntry(filteredEntry);
        verifyOriginalAndFilteredEntry(this.uniParcEntry, filteredEntry);
        Assertions.assertNotEquals(taxonomies, filteredEntry.getTaxonomies());
    }

    @Test
    void testFilterWithNoTaxon() {
        verifyUniParcEntry(uniParcEntry);
        UniParcEntryBuilder entryBuilder = UniParcEntryBuilder.from(uniParcEntry);
        entryBuilder.taxonomiesSet(null);
        UniParcEntry entryWithoutXref = entryBuilder.build();
        List<String> filter = Arrays.asList("9606");
        // filter by taxon
        UniParcEntry filteredEntry = uniParcTaxonomyFilter.apply(entryWithoutXref, filter);
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
        Assertions.assertEquals(
                uniParcEntry.getUniParcCrossReferences(),
                filteredEntry.getUniParcCrossReferences());
    }
}
