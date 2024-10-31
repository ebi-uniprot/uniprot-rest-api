package org.uniprot.api.uniprotkb.common.service.publication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.uniprotkb.common.service.publication.PublicationConverter.extractObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.api.uniprotkb.common.repository.model.PublicationEntry;
import org.uniprot.core.CrossReference;
import org.uniprot.core.citation.CitationDatabase;
import org.uniprot.core.citation.JournalArticle;
import org.uniprot.core.citation.Submission;
import org.uniprot.core.citation.impl.JournalArticleBuilder;
import org.uniprot.core.citation.impl.SubmissionBuilder;
import org.uniprot.core.impl.CrossReferenceBuilder;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.literature.impl.LiteratureEntryBuilder;
import org.uniprot.core.literature.impl.LiteratureStatisticsBuilder;
import org.uniprot.core.publication.MappedPublications;
import org.uniprot.core.publication.MappedReference;
import org.uniprot.core.publication.MappedSource;
import org.uniprot.store.indexer.uniprot.mockers.PublicationDocumentMocker;
import org.uniprot.store.search.document.publication.PublicationDocument;

class PublicationConverterTest {
    private PublicationConverter converter;

    @BeforeEach
    void setUp() {
        this.converter = new PublicationConverter();
    }

    @Test
    void convertPublicationDocumentThatHasPubmedIdAsCitationId() {
        PublicationDocument document = PublicationDocumentMocker.create(1, "1");
        Map<String, LiteratureEntry> pubmedLiteratureEntryMap = new HashMap<>();
        CrossReference<CitationDatabase> pubmedXref =
                new CrossReferenceBuilder<CitationDatabase>()
                        .id("1")
                        .database(CitationDatabase.PUBMED)
                        .build();
        JournalArticle journalArticle =
                new JournalArticleBuilder().citationCrossReferencesAdd(pubmedXref).build();
        LiteratureEntry literatureEntry =
                new LiteratureEntryBuilder()
                        .citation(journalArticle)
                        .statistics(
                                new LiteratureStatisticsBuilder().reviewedProteinCount(1L).build())
                        .build();
        pubmedLiteratureEntryMap.put("1", literatureEntry);
        PublicationEntry entry = converter.apply(document, pubmedLiteratureEntryMap);

        // check the journal object is the one from the map
        assertSame(entry.getCitation(), journalArticle);

        assertThat(entry.getStatistics(), is(notNullValue()));
        assertTrue(entry.getStatistics().hasReviewedProteinCount());
        assertThat(entry.getStatistics().getReviewedProteinCount(), is(1L));
        assertFalse(entry.getStatistics().hasUnreviewedProteinCount());
        assertFalse(entry.getStatistics().hasComputationallyMappedProteinCount());
        assertFalse(entry.getStatistics().hasCommunityMappedProteinCount());

        assertThat(entry.getReferences(), hasSize(3));

        Set<String> sources =
                entry.getReferences().stream()
                        .map(MappedReference::getSource)
                        .map(MappedSource::getName)
                        .collect(Collectors.toSet());

        assertThat(sources, contains("source P00001"));
    }

    @Test
    void convertPublicationDocumentWithCitationId() {
        String citationId = "CI-1234567890";
        PublicationDocument document = PublicationDocumentMocker.create(1, citationId);
        Map<String, LiteratureEntry> emptyMap = new HashMap<>();
        Submission submission = new SubmissionBuilder().title("Submission").build();
        LiteratureEntry literatureEntry =
                new LiteratureEntryBuilder()
                        .citation(submission)
                        .statistics(
                                new LiteratureStatisticsBuilder().reviewedProteinCount(1L).build())
                        .build();
        emptyMap.put(citationId, literatureEntry);
        PublicationEntry entry = converter.apply(document, emptyMap);

        // check the citation is a submission
        assertThat(entry.getCitation(), is(notNullValue()));
        assertThat(entry.getCitation(), instanceOf(Submission.class));
        assertThat(entry.getCitation().getTitle(), is("Submission"));

        assertThat(entry.getStatistics(), is(notNullValue()));
        assertTrue(entry.getStatistics().hasReviewedProteinCount());
        assertThat(entry.getStatistics().getReviewedProteinCount(), is(1L));
        assertFalse(entry.getStatistics().hasUnreviewedProteinCount());
        assertFalse(entry.getStatistics().hasComputationallyMappedProteinCount());
        assertFalse(entry.getStatistics().hasCommunityMappedProteinCount());
        assertThat(entry.getReferences(), hasSize(3));

        Set<String> sources =
                entry.getReferences().stream()
                        .map(MappedReference::getSource)
                        .map(MappedSource::getName)
                        .collect(Collectors.toSet());

        assertThat(sources, contains("source P00001"));
    }

    @Test
    void validObjectIsDeserialised() {
        PublicationDocument document = PublicationDocumentMocker.create(1, "1");
        Optional<MappedPublications> mappedPublications = extractObject(document);

        assertDoesNotThrow(() -> mappedPublications.orElseThrow(IllegalStateException::new));
    }

    @Test
    void invalidObjectDeserialisationReturnsEmpty() {
        PublicationDocument document =
                PublicationDocument.builder().publicationMappedReferences(new byte[1]).build();

        Optional<MappedPublications> mappedPublications = extractObject(document);

        assertThat(mappedPublications, is(Optional.empty()));
    }
}
