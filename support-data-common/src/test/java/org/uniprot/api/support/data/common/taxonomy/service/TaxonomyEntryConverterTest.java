package org.uniprot.api.support.data.common.taxonomy.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.TaxonomyRank;
import org.uniprot.core.taxonomy.impl.TaxonomyEntryBuilder;
import org.uniprot.core.taxonomy.impl.TaxonomyLineageBuilder;
import org.uniprot.core.taxonomy.impl.TaxonomyStatisticsBuilder;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

class TaxonomyEntryConverterTest {

    @Test
    void canConvertTaxonomyWithSuccess() throws JsonProcessingException {
        TaxonomyEntry entry = getTaxonomyEntry();
        byte[] entryInBytes =
                TaxonomyJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry);
        TaxonomyDocument doc = getTaxonomyDocument(entryInBytes);

        TaxonomyEntryConverter taxonomyEntryConverter = new TaxonomyEntryConverter();
        TaxonomyEntry result = taxonomyEntryConverter.apply(doc);
        assertNotNull(result);
        assertEquals(entry, result);
    }

    @Test
    void canNotConvertTaxonomyWithSimpleMapper() throws JsonProcessingException {
        TaxonomyEntry entry = getTaxonomyEntry();
        byte[] entryInBytes =
                TaxonomyJsonConfig.getInstance().getSimpleObjectMapper().writeValueAsBytes(entry);
        TaxonomyDocument doc = getTaxonomyDocument(entryInBytes);

        TaxonomyEntryConverter taxonomyEntryConverter = new TaxonomyEntryConverter();
        TaxonomyEntry result = taxonomyEntryConverter.apply(doc);
        assertNull(result);
    }

    private TaxonomyEntry getTaxonomyEntry() {
        return new TaxonomyEntryBuilder()
                .taxonId(9606)
                .scientificName("scientific Name")
                .commonName("common name")
                .rank(TaxonomyRank.FAMILY)
                .lineagesAdd(new TaxonomyLineageBuilder().scientificName("l scientific").build())
                .statistics(new TaxonomyStatisticsBuilder().reviewedProteinCount(2).build())
                .linksAdd("KW link")
                .build();
    }

    private TaxonomyDocument getTaxonomyDocument(byte[] entryInBytes) {
        return TaxonomyDocument.builder().id("9606").taxonomyObj(entryInBytes).build();
    }
}
