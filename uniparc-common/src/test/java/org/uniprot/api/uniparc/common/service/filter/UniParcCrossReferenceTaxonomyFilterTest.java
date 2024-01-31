package org.uniprot.api.uniparc.common.service.filter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;

/**
 * @author lgonzales
 * @since 14/08/2020
 */
class UniParcCrossReferenceTaxonomyFilterTest {

    private static UniParcCrossReferenceTaxonomyFilter uniParcTaxonomyFilter;
    private static final String UNIPARC_ID_PREFIX = "UPI0000083C";
    private UniParcEntry uniParcEntry;

    @BeforeAll
    static void setUp() {
        uniParcTaxonomyFilter = new UniParcCrossReferenceTaxonomyFilter();
    }

    @BeforeEach
    void createTestData() {
        this.uniParcEntry = UniParcEntryMocker.createEntry(1, UNIPARC_ID_PREFIX);
        assertNotNull(this.uniParcEntry);
        Assertions.assertTrue(
                this.uniParcEntry.getUniParcId().getValue().startsWith(UNIPARC_ID_PREFIX));
    }

    @Test
    void testFilterByTaxonomyIds() {
        verifyUniParcEntry(uniParcEntry);
        List<String> taxonFilter = Collections.singletonList("9606");
        // filter by db
        UniParcEntry filteredEntry = uniParcTaxonomyFilter.apply(this.uniParcEntry, taxonFilter);
        // everything should be same except xrefs
        Assertions.assertEquals(1, filteredEntry.getUniParcCrossReferences().size());
        Assertions.assertEquals(
                9606L, filteredEntry.getUniParcCrossReferences().get(0).getOrganism().getTaxonId());
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
        List<String> taxonFilter = Arrays.asList("10090", "9606");
        // filter by taxon
        UniParcEntry filteredEntry = uniParcTaxonomyFilter.apply(this.uniParcEntry, taxonFilter);
        // everything should be same except UniParcCrossReferences
        Assertions.assertEquals(1, filteredEntry.getUniParcCrossReferences().size());
        Assertions.assertEquals(
                9606L, filteredEntry.getUniParcCrossReferences().get(0).getOrganism().getTaxonId());
        verifyUniParcEntry(filteredEntry);
        Assertions.assertNotEquals(
                uniParcEntry.getUniParcCrossReferences(),
                filteredEntry.getUniParcCrossReferences());
        verifyOriginalAndFilteredEntry(this.uniParcEntry, filteredEntry);
    }

    @Test
    void testFilterByNonMatchingTaxon() {
        verifyUniParcEntry(uniParcEntry);
        List<UniParcCrossReference> xRef = uniParcEntry.getUniParcCrossReferences();
        Assertions.assertEquals(3, this.uniParcEntry.getUniParcCrossReferences().size());
        List<String> taxonFilter = Collections.singletonList("0");
        // filter by Cross Ref taxon
        UniParcEntry filteredEntry = uniParcTaxonomyFilter.apply(this.uniParcEntry, taxonFilter);
        // everything should be same but no Cross Ref taxon
        Assertions.assertTrue(filteredEntry.getUniParcCrossReferences().isEmpty());

        verifyUniParcEntry(filteredEntry);
        verifyOriginalAndFilteredEntry(this.uniParcEntry, filteredEntry);
        Assertions.assertNotEquals(xRef, filteredEntry.getUniParcCrossReferences());
    }

    @Test
    void testFilterWithNoTaxon() {
        verifyUniParcEntry(uniParcEntry);
        UniParcEntryBuilder entryBuilder = UniParcEntryBuilder.from(uniParcEntry);
        entryBuilder.uniParcCrossReferencesSet(null);
        UniParcEntry entryWithoutXref = entryBuilder.build();
        List<String> filter = Collections.singletonList("9606");
        // filter by taxon
        UniParcEntry filteredEntry = uniParcTaxonomyFilter.apply(entryWithoutXref, filter);
        Assertions.assertEquals(entryWithoutXref, filteredEntry);
    }

    private void verifyUniParcEntry(UniParcEntry uniParcEntry) {
        assertNotNull(uniParcEntry);
        assertNotNull(uniParcEntry.getUniParcId());
        Assertions.assertTrue(uniParcEntry.getUniParcId().getValue().startsWith(UNIPARC_ID_PREFIX));
        assertNotNull(uniParcEntry.getSequence());
        assertNotNull(uniParcEntry.getSequenceFeatures());
    }

    private void verifyOriginalAndFilteredEntry(
            UniParcEntry uniParcEntry, UniParcEntry filteredEntry) {
        Assertions.assertEquals(uniParcEntry.getUniParcId(), filteredEntry.getUniParcId());
        Assertions.assertEquals(uniParcEntry.getSequence(), filteredEntry.getSequence());
        Assertions.assertEquals(
                uniParcEntry.getSequenceFeatures(), filteredEntry.getSequenceFeatures());
    }
}
