package org.uniprot.api.subcell.output;

import org.junit.jupiter.api.Test;
import org.uniprot.core.cv.keyword.impl.GeneOntologyImpl;
import org.uniprot.core.cv.keyword.impl.KeywordImpl;
import org.uniprot.core.cv.subcell.SubcellLocationCategory;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.core.cv.subcell.impl.SubcellularLocationEntryImpl;
import org.uniprot.core.cv.subcell.impl.SubcellularLocationStatisticsImpl;
import org.uniprot.store.search.field.SubcellularLocationField;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

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
        SubcellularLocationEntry filterEntry = SubcellularLocationEntryFilter.filterEntry(entry, null);
        assertNotNull(filterEntry);
        assertEquals(entry, filterEntry);
    }

    @Test
    void filterWithAllFieldsEntry() {
        SubcellularLocationEntry entry = getCompleteSubcellularLocationEntry(true);
        List<String> allFields = Arrays.stream(SubcellularLocationField.ResultFields.values())
                .map(SubcellularLocationField.ResultFields::name)
                .collect(Collectors.toList());

        SubcellularLocationEntry filterEntry = SubcellularLocationEntryFilter.filterEntry(entry, allFields);
        assertNotNull(filterEntry);
        assertEquals(entry, filterEntry);
    }


    private SubcellularLocationEntry getCompleteSubcellularLocationEntry(boolean hasChild) {
        SubcellularLocationEntryImpl entry = new SubcellularLocationEntryImpl();
        entry.setAccession("accession");
        entry.setContent("content");
        entry.setDefinition("definition");
        entry.setGeneOntologies(Collections.singletonList(new GeneOntologyImpl("goId", "goTerm")));
        entry.setId("id");
        entry.setKeyword(new KeywordImpl("keywordId", "keywordAccession"));
        entry.setLinks(Collections.singletonList("link"));
        entry.setNote("note");
        entry.setReferences(Collections.singletonList("synonym"));
        entry.setStatistics(new SubcellularLocationStatisticsImpl(10L, 20L));
        entry.setSynonyms(Collections.singletonList("synonym"));
        entry.setCategory(SubcellLocationCategory.LOCATION);
        if (hasChild) {
            entry.setIsA(Collections.singletonList(getCompleteSubcellularLocationEntry(false)));
            entry.setPartOf(Collections.singletonList(getCompleteSubcellularLocationEntry(false)));
        }
        return entry;
    }

}