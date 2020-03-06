package org.uniprot.api.literature.output;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.uniprot.core.CrossReference;
import org.uniprot.core.builder.CrossReferenceBuilder;
import org.uniprot.core.citation.Citation;
import org.uniprot.core.citation.CitationDatabase;
import org.uniprot.core.citation.Literature;
import org.uniprot.core.citation.builder.LiteratureBuilder;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.literature.LiteratureStatistics;
import org.uniprot.core.literature.builder.LiteratureEntryBuilder;
import org.uniprot.core.literature.builder.LiteratureStatisticsBuilder;
import org.uniprot.store.search.field.LiteratureField;

/**
 * @author lgonzales
 * @since 2019-07-05
 */
class LiteratureEntryFilterTest {

    @Test
    void parseWithoutFieldsReturnDefaultFields() {
        List<String> fieldList = LiteratureEntryFilter.parse(null);
        assertNotNull(fieldList);
        assertFalse(fieldList.isEmpty());
        assertEquals(fieldList.size(), 4);

        assertTrue(fieldList.contains("id"));
        assertTrue(fieldList.contains("reference"));
        assertTrue(fieldList.contains("title"));
        assertTrue(fieldList.contains("lit_abstract"));
    }

    @Test
    void parseFieldsReturnFieldList() {
        List<String> fieldList = LiteratureEntryFilter.parse("id,title");
        assertNotNull(fieldList);
        assertFalse(fieldList.isEmpty());
        assertEquals(fieldList.size(), 2);

        assertTrue(fieldList.contains("id"));
        assertTrue(fieldList.contains("title"));
    }

    @Test
    void filterWithoutFieldsEntry() {
        LiteratureEntry entry = createCompleteLiteratureEntry();
        LiteratureEntry filterEntry = LiteratureEntryFilter.filterEntry(entry, null);
        assertNotNull(filterEntry);
        assertEquals(entry, filterEntry);
    }

    @Test
    void filterWithAllFieldsEntry() {
        LiteratureEntry entry = createCompleteLiteratureEntry();
        List<String> allFields =
                Arrays.stream(LiteratureField.ResultFields.values())
                        .map(LiteratureField.ResultFields::name)
                        .collect(Collectors.toList());

        LiteratureEntry filterEntry = LiteratureEntryFilter.filterEntry(entry, allFields);
        assertNotNull(filterEntry);
        assertEquals(entry, filterEntry);
    }

    @Test
    void filterWithIdAndTitleFieldEntry() {
        LiteratureEntry entry = createCompleteLiteratureEntry();
        Literature literature = (Literature) entry.getCitation();
        List<String> allFields = Arrays.asList("id", "title");

        LiteratureEntry filterEntry = LiteratureEntryFilter.filterEntry(entry, allFields);
        assertNotNull(filterEntry);
        assertNotNull(filterEntry.getCitation());
        Literature filteredLiterature = (Literature) filterEntry.getCitation();

        assertEquals(literature.getPubmedId(), filteredLiterature.getPubmedId());
        assertEquals(literature.getTitle(), filteredLiterature.getTitle());
        assertEquals("", filteredLiterature.getDoiId());
        assertEquals("", filteredLiterature.getFirstPage());
        assertEquals("", filteredLiterature.getLastPage());
        assertEquals("", filteredLiterature.getVolume());
        assertNull(filteredLiterature.getPublicationDate());
        assertNull(filterEntry.getStatistics());
        assertNull(filteredLiterature.getJournal());
        assertTrue(filteredLiterature.getAuthors().isEmpty());
        assertTrue(filteredLiterature.getAuthoringGroups().isEmpty());
    }

    private LiteratureEntry createCompleteLiteratureEntry() {
        return new LiteratureEntryBuilder()
                .citation(createCompleteLiteratureCitation())
                .statistics(createCompleteLiteratureStatistics())
                .build();
    }

    private Citation createCompleteLiteratureCitation() {
        CrossReference<CitationDatabase> pubmed =
                new CrossReferenceBuilder<CitationDatabase>()
                        .database(CitationDatabase.PUBMED)
                        .id("12345")
                        .build();

        CrossReference<CitationDatabase> doi =
                new CrossReferenceBuilder<CitationDatabase>()
                        .database(CitationDatabase.DOI)
                        .id("doiId")
                        .build();

        return new LiteratureBuilder()
                .literatureAbstract("literature abstract")
                .completeAuthorList(true)
                .firstPage("first page")
                .lastPage("last page")
                .volume("the volume")
                .journalName("The journal name")
                .authorsAdd("John")
                .authoringGroupsAdd("the author group")
                .citationCrossReferencesAdd(pubmed)
                .citationCrossReferencesAdd(doi)
                .publicationDate("2015-MAY")
                .title("the big title")
                .build();
    }

    private LiteratureStatistics createCompleteLiteratureStatistics() {
        return new LiteratureStatisticsBuilder()
                .reviewedProteinCount(10)
                .unreviewedProteinCount(20)
                .mappedProteinCount(30)
                .build();
    }
}
