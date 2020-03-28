package org.uniprot.api.subcell.output;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.uniprot.core.Statistics;
import org.uniprot.core.cv.go.impl.GeneOntologyEntryBuilder;
import org.uniprot.core.cv.keyword.impl.KeywordIdBuilder;
import org.uniprot.core.cv.subcell.SubcellLocationCategory;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.core.cv.subcell.impl.SubcellularLocationEntryBuilder;
import org.uniprot.core.impl.StatisticsBuilder;
import org.uniprot.store.search.field.SubcellularLocationField;

/**
 * @author lgonzales
 * @since 2019-08-30
 */
class SubcellularLocationEntryFilterTest {

    @Test
    void parseWithoutFieldsReturnDefaultFields() {
        List<String> fieldList = SubcellularLocationEntryFilter.parse(null);
        assertNotNull(fieldList);
        assertFalse(fieldList.isEmpty());
        assertEquals(fieldList.size(), 4);

        assertTrue(fieldList.contains("id"));
        assertTrue(fieldList.contains("accession"));
        assertTrue(fieldList.contains("definition"));
        assertTrue(fieldList.contains("category"));
    }

    @Test
    void parseFieldsReturnFieldList() {
        List<String> fieldList = SubcellularLocationEntryFilter.parse("id,definition");
        assertNotNull(fieldList);
        assertFalse(fieldList.isEmpty());
        assertEquals(fieldList.size(), 2);

        assertTrue(fieldList.contains("id"));
        assertTrue(fieldList.contains("definition"));
    }

    @Test
    void filterWithoutFieldsEntry() {
        SubcellularLocationEntry entry = getCompleteSubcellularLocationEntry(true);
        SubcellularLocationEntry filterEntry =
                SubcellularLocationEntryFilter.filterEntry(entry, null);
        assertNotNull(filterEntry);
        assertEquals(entry, filterEntry);
    }

    @Test
    void filterWithAllFieldsEntry() {
        SubcellularLocationEntry entry = getCompleteSubcellularLocationEntry(true);
        List<String> allFields =
                Arrays.stream(SubcellularLocationField.ResultFields.values())
                        .map(SubcellularLocationField.ResultFields::name)
                        .collect(Collectors.toList());

        SubcellularLocationEntry filterEntry =
                SubcellularLocationEntryFilter.filterEntry(entry, allFields);
        assertNotNull(filterEntry);
        assertEquals(entry, filterEntry);
    }

    private SubcellularLocationEntry getCompleteSubcellularLocationEntry(boolean hasChild) {
        Statistics statistics =
                new StatisticsBuilder().reviewedProteinCount(10).unreviewedProteinCount(20).build();
        SubcellularLocationEntryBuilder entry = new SubcellularLocationEntryBuilder();
        entry.accession("accession");
        entry.content("content");
        entry.definition("definition");
        entry.geneOntologiesAdd(new GeneOntologyEntryBuilder().id("goId").name("goTerm").build());
        entry.id("id");
        entry.keyword(new KeywordIdBuilder().id("keywordId").accession("keywordAccession").build());
        entry.linksAdd("link");
        entry.note("note");
        entry.referencesAdd("synonym");
        entry.statistics(statistics);
        entry.synonymsAdd("synonym");
        entry.category(SubcellLocationCategory.LOCATION);
        if (hasChild) {
            entry.isAAdd(getCompleteSubcellularLocationEntry(false));
            entry.partOfAdd(getCompleteSubcellularLocationEntry(false));
        }
        return entry.build();
    }
}
