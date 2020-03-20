package org.uniprot.api.taxonomy.output;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.TaxonomyRank;
import org.uniprot.core.taxonomy.impl.TaxonomyEntryBuilder;
import org.uniprot.core.taxonomy.impl.TaxonomyLineageBuilder;
import org.uniprot.core.taxonomy.impl.TaxonomyStatisticsBuilder;
import org.uniprot.core.taxonomy.impl.TaxonomyStrainBuilder;
import org.uniprot.core.uniprot.taxonomy.Taxonomy;
import org.uniprot.core.uniprot.taxonomy.impl.TaxonomyBuilder;
import org.uniprot.store.search.field.TaxonomyField;

/** @author lgonzales */
class TaxonomyEntryFilterTest {

    @Test
    void parseWithoutFieldsReturnDefaultFields() {
        List<String> fieldList = TaxonomyEntryFilter.parse(null);
        assertNotNull(fieldList);
        assertFalse(fieldList.isEmpty());
        assertEquals(fieldList.size(), 10);

        assertTrue(fieldList.contains("id"));
        assertTrue(fieldList.contains("mnemonic"));
        assertTrue(fieldList.contains("scientific_name"));
        assertTrue(fieldList.contains("common_name"));
        assertTrue(fieldList.contains("other_names"));
        assertTrue(fieldList.contains("reviewed"));
        assertTrue(fieldList.contains("rank"));
        assertTrue(fieldList.contains("lineage"));
        assertTrue(fieldList.contains("parent"));
        assertTrue(fieldList.contains("host"));
    }

    @Test
    void parseFieldsReturnFieldList() {
        List<String> fieldList = TaxonomyEntryFilter.parse("id,mnemonic");
        assertNotNull(fieldList);
        assertFalse(fieldList.isEmpty());
        assertEquals(fieldList.size(), 2);

        assertTrue(fieldList.contains("id"));
        assertTrue(fieldList.contains("mnemonic"));
    }

    @Test
    void filterWithoutFieldsEntry() {
        TaxonomyEntry entry = getCompleteTaxonomyEntry();
        TaxonomyEntry filterEntry = TaxonomyEntryFilter.filterEntry(entry, null);
        assertNotNull(filterEntry);
        assertEquals(entry, filterEntry);
    }

    @Test
    void filterWithAllFieldsEntry() {
        TaxonomyEntry entry = getCompleteTaxonomyEntry();
        List<String> allFields =
                Arrays.stream(TaxonomyField.ResultFields.values())
                        .map(TaxonomyField.ResultFields::name)
                        .collect(Collectors.toList());

        TaxonomyEntry filterEntry = TaxonomyEntryFilter.filterEntry(entry, allFields);
        assertNotNull(filterEntry);
        assertEquals(entry.getTaxonId(), filterEntry.getTaxonId());
        assertEquals(entry.getParentId(), filterEntry.getParentId());
        assertEquals(entry.getMnemonic(), filterEntry.getMnemonic());
        assertEquals(entry.getScientificName(), filterEntry.getScientificName());
        assertEquals(entry.getCommonName(), filterEntry.getCommonName());
        assertEquals(entry.getSynonyms(), filterEntry.getSynonyms());
        assertEquals(entry.getOtherNames(), filterEntry.getOtherNames());
        assertEquals(entry.getRank(), filterEntry.getRank());
        assertEquals(entry.getLineages(), filterEntry.getLineages());
        assertEquals(entry.getStrains(), filterEntry.getStrains());
        assertEquals(entry.getHosts(), filterEntry.getHosts());
        assertEquals(entry.getLinks(), filterEntry.getLinks());
        assertEquals(entry.getStatistics(), filterEntry.getStatistics());
        assertFalse(filterEntry.isHidden());
        assertFalse(filterEntry.isActive());
    }

    @Test
    void filterWithIdAndScientificNameFieldEntry() {
        TaxonomyEntry entry = getCompleteTaxonomyEntry();
        List<String> allFields = Arrays.asList("id", "scientific_name");

        TaxonomyEntry filterEntry = TaxonomyEntryFilter.filterEntry(entry, allFields);
        assertNotNull(filterEntry);
        assertEquals(entry.getTaxonId(), filterEntry.getTaxonId());
        assertNull(filterEntry.getParentId());
        assertNull(filterEntry.getMnemonic());
        assertEquals(entry.getScientificName(), filterEntry.getScientificName());
        assertTrue(filterEntry.getCommonName().isEmpty());
        assertTrue(filterEntry.getSynonyms().isEmpty());
        assertTrue(filterEntry.getOtherNames().isEmpty());
        assertNull(filterEntry.getRank());
        assertTrue(filterEntry.getLineages().isEmpty());
        assertTrue(filterEntry.getStrains().isEmpty());
        assertTrue(filterEntry.getHosts().isEmpty());
        assertTrue(filterEntry.getLinks().isEmpty());
        assertNull(filterEntry.getStatistics());
        assertFalse(filterEntry.isHidden());
        assertFalse(filterEntry.isActive());
    }

    private TaxonomyEntry getCompleteTaxonomyEntry() {
        TaxonomyEntryBuilder builder = new TaxonomyEntryBuilder();
        builder.taxonId(9606L);
        builder.scientificName("scientificName");
        builder.commonName("commonName");
        builder.mnemonic("mnemonic");
        builder.parentId(9605L);
        builder.rank(TaxonomyRank.KINGDOM);
        builder.hidden(true);
        builder.active(true);
        builder.statistics(new TaxonomyStatisticsBuilder().reviewedProteinCount(10).build());

        builder.synonymsAdd("synonym");
        builder.otherNamesAdd("otherName");
        builder.lineagesAdd(new TaxonomyLineageBuilder().scientificName("scientificName").build());
        builder.strainsAdd(new TaxonomyStrainBuilder().name("name").build());
        builder.hostsAdd(getCompleteTaxonomy());
        builder.linksAdd("link");

        return builder.build();
    }

    private Taxonomy getCompleteTaxonomy() {
        return new TaxonomyBuilder()
                .taxonId(9606)
                .scientificName("Homo sapiens")
                .commonName("Human")
                .synonymsSet(Collections.singletonList("Some name"))
                .mnemonic("HUMAN")
                .build();
    }
}
