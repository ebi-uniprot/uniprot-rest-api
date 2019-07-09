package uk.ac.ebi.uniprot.api.literature.output;

import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.domain.citation.impl.AuthorImpl;
import uk.ac.ebi.uniprot.domain.citation.impl.PublicationDateImpl;
import uk.ac.ebi.uniprot.domain.literature.LiteratureEntry;
import uk.ac.ebi.uniprot.domain.literature.LiteratureMappedReference;
import uk.ac.ebi.uniprot.domain.literature.LiteratureStatistics;
import uk.ac.ebi.uniprot.domain.literature.builder.LiteratureEntryBuilder;
import uk.ac.ebi.uniprot.domain.literature.builder.LiteratureMappedReferenceBuilder;
import uk.ac.ebi.uniprot.domain.literature.builder.LiteratureStatisticsBuilder;
import uk.ac.ebi.uniprot.domain.uniprot.impl.UniProtAccessionImpl;
import uk.ac.ebi.uniprot.search.field.LiteratureField;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

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
        LiteratureEntry entry = getCompleteLiteratureEntry();
        LiteratureEntry filterEntry = LiteratureEntryFilter.filterEntry(entry, null);
        assertNotNull(filterEntry);
        assertEquals(entry, filterEntry);
    }

    @Test
    void filterWithAllFieldsEntry() {
        LiteratureEntry entry = getCompleteLiteratureEntry();
        List<String> allFields = Arrays.stream(LiteratureField.ResultFields.values())
                .map(LiteratureField.ResultFields::name)
                .collect(Collectors.toList());

        LiteratureEntry filterEntry = LiteratureEntryFilter.filterEntry(entry, allFields);
        assertNotNull(filterEntry);
        assertEquals(entry, filterEntry);
    }

    @Test
    void filterWithIdAndTitleFieldEntry() {
        LiteratureEntry entry = getCompleteLiteratureEntry();
        List<String> allFields = Arrays.asList("id", "title");

        LiteratureEntry filterEntry = LiteratureEntryFilter.filterEntry(entry, allFields);
        assertNotNull(filterEntry);
        assertEquals(entry.getPubmedId(), filterEntry.getPubmedId());
        assertEquals(entry.getTitle(), filterEntry.getTitle());
        assertEquals(filterEntry.getDoiId(), "");
        assertEquals(filterEntry.getFirstPage(), "");
        assertEquals(filterEntry.getLastPage(), "");
        assertEquals(filterEntry.getVolume(), "");
        assertNull(filterEntry.getPublicationDate());
        assertNull(filterEntry.getStatistics());
        assertNull(filterEntry.getJournal());
        assertTrue(filterEntry.getAuthors().isEmpty());
        assertTrue(filterEntry.getAuthoringGroup().isEmpty());
        assertTrue(filterEntry.getLiteratureMappedReferences().isEmpty());

    }

    private LiteratureEntry getCompleteLiteratureEntry() {
        return new LiteratureEntryBuilder()
                .doiId("doi Id")
                .pubmedId(100L)
                .firstPage("first Page")
                .journal("journal Name")
                .volume("volume")
                .lastPage("last Page")
                .literatureAbstract("literature Abstract")
                .publicationDate(new PublicationDateImpl("21-06-2019"))
                .statistics(createCompleteLiteratureStatistics())
                .addAuthor(new AuthorImpl("author name"))
                .addAuthoringGroup("authoring group")
                .addLiteratureMappedReference(createCompleteLiteratureMappedReference())
                .title("title")
                .completeAuthorList(false)
                .build();
    }

    private LiteratureStatistics createCompleteLiteratureStatistics() {
        return new LiteratureStatisticsBuilder()
                .reviewedProteinCount(10)
                .unreviewedProteinCount(20)
                .mappedProteinCount(30)
                .build();
    }

    private LiteratureMappedReference createCompleteLiteratureMappedReference() {
        return new LiteratureMappedReferenceBuilder()
                .annotation("annotation value")
                .source("source value")
                .sourceId("source Id")
                .addSourceCategory("source category")
                .uniprotAccession(new UniProtAccessionImpl("P12345"))
                .build();
    }

}